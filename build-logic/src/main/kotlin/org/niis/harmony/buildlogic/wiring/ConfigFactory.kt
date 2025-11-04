package org.niis.harmony.buildlogic.wiring

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.niis.harmony.buildlogic.internal.Constants
import org.niis.harmony.buildlogic.internal.utils.parseCsvList
import org.niis.harmony.buildlogic.internal.utils.sanitize
import org.niis.harmony.buildlogic.models.BuildConfig
import org.niis.harmony.buildlogic.models.BuildInfoConfig
import org.niis.harmony.buildlogic.models.CompileConfig
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.models.DebConfig
import org.niis.harmony.buildlogic.models.DockerOutputMode
import org.niis.harmony.buildlogic.models.DockerTargetConfig
import org.niis.harmony.buildlogic.models.StagingConfig
import org.niis.harmony.buildlogic.models.VendorConfig
import org.niis.harmony.buildlogic.providers.SourceDateEpochValueSource
import org.niis.harmony.buildlogic.providers.VcsRevisionValueSource
import java.io.File

object ConfigFactory {

  fun createBuildConfig(project: Project): BuildConfig {
    val resolver = PropertyResolver(project, null)
    return BuildConfig(
      components = resolver.list("components").orElse(Constants.BuildDefaults.COMPONENTS),
      debBuilderImage = resolver.string("deb.builder.image").orElse(Constants.BuildDefaults.DEB_BUILDER_IMAGE),
      debBuilderTag = resolver.string("deb.builder.tag").orElse(Constants.BuildDefaults.DEB_BUILDER_TAG)
    )
  }

  fun createComponentConfig(project: Project, component: String, tools: PluginWiring.ToolProviders): ComponentConfig {
    val resolver = PropertyResolver(project, component)

    val version = resolver.string("version")
      .orElse(project.provider {
        error("Version is missing for component '$component'. Define 'harmony.$component.version'.")
      })

    val compile = createCompileConfig(project, component, version, resolver)
    val staging = createStagingConfig(project, component)
    val buildInfo = createBuildInfoConfig(project, compile.repoDir, resolver, tools)

    return ComponentConfig(
      name = component,
      version = version,
      compile = compile,
      staging = staging,
      deb = createDebConfig(project, resolver, component),
      docker = createDockerTargetForComponent(resolver, component, version),
      buildInfo = buildInfo,
      vendor = createVendorConfig(project, component)
    )
  }

  private fun createCompileConfig(
    project: Project, component: String, version: Provider<String>, resolver: PropertyResolver
  ): CompileConfig {
    val repoDir = resolver.string("compile.repo").map { project.layout.projectDirectory.dir(it)
    }.orElse(project.provider {
      error("Mandatory property 'harmony.$component.compile.repo' is not defined in gradle.properties.")
    })

    val artifactPrefix = "harmony.$component.compile.artifact."
    val artifactProperties = project.providers.gradlePropertiesPrefixedBy(artifactPrefix)

    val artifacts = artifactProperties.flatMap { propsMap ->
      version.map { ver ->
        propsMap.mapKeys { (k, _) -> k.removePrefix(artifactPrefix) }
          .mapValues { (_, pathTemplate) ->
            val expandedPath = pathTemplate.replace("\${harmony.$component.version}", ver)
            repoDir.map { dir -> dir.file(expandedPath) }
          }
      }
    }

    return CompileConfig(
      skipTests = resolver.boolean("compile.skipTests", Constants.BuildDefaults.COMPILE_SKIP_TESTS),
      repoDir = repoDir,
      mavenProfiles = resolver.list("compile.maven.profiles").orElse(Constants.BuildDefaults.COMPILE_MAVEN_PROFILES),
      mavenGoals = resolver.list("compile.maven.goals").orElse(Constants.BuildDefaults.COMPILE_MAVEN_GOALS),
      javaVersion = resolver.int("compile.javaVersion", Constants.BuildDefaults.COMPILE_JAVA_VERSION),
      artifacts = artifacts
    )
  }

  private fun createStagingConfig(project: Project, component: String) = StagingConfig(
    manifestFile = project.layout.projectDirectory.file(stagingManifestPath(component)).asFile
  )

  private fun createDebConfig(project: Project, resolver: PropertyResolver, component: String) = DebConfig(
    distros = resolver.list("deb.distros").orElse(Constants.BuildDefaults.DEB_DISTROS),
    sign = resolver.boolean("deb.sign", Constants.BuildDefaults.DEB_SIGN),
    keyId = resolver.string("deb.keyId"),
    packageName = resolver.string("deb.packageName").orElse(debPackageName(component)),
    gpgHome = resolver.string("deb.gpgHome").orElse(project.provider { defaultGpgHome() })
  )

  private fun createDockerTargetForComponent(
    resolver: PropertyResolver, component: String, version: Provider<String>
  ) = DockerTargetConfig(
    imageName = resolver.string("docker.imageName").orElse(dockerImageNameForComponent(component)),
    tags = resolver.list("docker.tags").orElse(version.map { listOf(it.sanitize()) }),
    platforms = resolver.string("docker.platforms").orElse(Constants.BuildDefaults.DOCKER_PLATFORMS),
    outputMode = resolver.string("docker.outputMode")
      .orElse(Constants.BuildDefaults.DOCKER_OUTPUT_MODE)
      .map { DockerOutputMode.fromString(it) },
    trackBase = resolver.boolean("docker.trackBase", Constants.BuildDefaults.DOCKER_TRACK_BASE),
    pullAlways = resolver.boolean("docker.pullAlways", Constants.BuildDefaults.DOCKER_PULL_ALWAYS),
    provenanceDisabled = resolver.boolean("docker.provenanceDisabled", Constants.BuildDefaults.DOCKER_PROVENANCE_DISABLED)
  )

  private fun createBuildInfoConfig(
    project: Project, repoDir: Provider<Directory>, resolver: PropertyResolver, tools: PluginWiring.ToolProviders
  ): BuildInfoConfig {
    val epoch = resolver.string("build.epoch").map { it.toLong() }
      .orElse(project.providers.of(SourceDateEpochValueSource::class.java) {
        parameters.gitExecutable.set(tools.git)
        parameters.repoDir.set(repoDir)
        parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
      })

    val revision = resolver.string("build.revision")
      .orElse(project.providers.of(VcsRevisionValueSource::class.java) {
        parameters.gitExecutable.set(tools.git)
        parameters.repoDir.set(repoDir)
        parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
      })

    return BuildInfoConfig(
      epoch = epoch,
      revision = revision,
      buildId = resolver.string("build.id").orElse(Constants.BuildDefaults.BUILD_ID)
    )
  }

  private fun createVendorConfig(project: Project, component: String): VendorConfig {
    val resolver = PropertyResolver(project, component)

    return VendorConfig(
      resolveVersion = { dependency ->
        resolver.vendorMetadata(dependency, "version")
          .orElse(project.provider {
            error(
              "Missing version for vendor dependency '$dependency' (required by component '$component')."
            )
          })
      },
      resolveNotation = { dependency, version, classifier ->
        resolveVendorNotation(resolver, dependency, version, classifier)
      }
    )
  }

  private fun resolveVendorNotation(
    resolver: PropertyResolver,
    dependency: String,
    version: String,
    classifier: String?
  ): String {
    val group = resolver.vendorMetadataOrNull(dependency, "group") ?: dependency
    val name = resolver.vendorMetadataOrNull(dependency, "name") ?: dependency
    val extension = resolver.vendorMetadataOrNull(dependency, "extension")

    val mappedClassifier = when {
      classifier == null -> null
      resolver.vendorMetadata(dependency, "classifier.$classifier").isPresent ->
        resolver.vendorMetadata(dependency, "classifier.$classifier").get()
      else -> classifier
    }

    return buildString {
      append("$group:$name:$version")
      if (mappedClassifier != null) append(":$mappedClassifier")
      if (extension != null) append("@$extension")
    }
  }

  private fun defaultGpgHome(): String {
    val envOverride = System.getenv("GNUPGHOME")?.takeIf { it.isNotBlank() }
    if (envOverride != null) return envOverride

    val userHome = System.getProperty("user.home")
      ?: error("System property 'user.home' is not set. Configure 'harmony.deb.gpgHome'.")
    return File(userHome, ".gnupg").absolutePath
  }

  private class PropertyResolver(private val project: Project, private val component: String?) {
    private val listCache = mutableMapOf<String, Provider<List<String>>>()

    private fun resolve(key: String): Provider<String> {
      return if (component != null) {
        project.providers.gradleProperty("harmony.$component.$key")
          .orElse(project.providers.gradleProperty("harmony.$key"))
      } else {
        project.providers.gradleProperty("harmony.$key")
      }
    }

    fun string(key: String) = resolve(key)
    fun list(key: String) = listCache.getOrPut(key) { resolve(key).map { it.parseCsvList() } }
    fun boolean(key: String, default: Boolean) = resolve(key).map { it.toBoolean() }.orElse(default)
    fun int(key: String, default: Int) = resolve(key).map { it.toInt() }.orElse(default)

    fun vendorMetadata(dependency: String, key: String): Provider<String> {
      return resolve("vendor.$dependency.$key")
    }

    fun vendorMetadataOrNull(dependency: String, key: String): String? {
      return vendorMetadata(dependency, key).orNull
    }
  }

  private fun stagingManifestPath(component: String) = "components/$component/manifest.yml"
  private fun debPackageName(component: String) = "harmony-$component"
  private fun dockerImageNameForComponent(component: String) = "niis/harmony-${component.sanitize()}"
}
