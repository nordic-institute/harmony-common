package org.niis.harmony.buildlogic.wiring

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.niis.harmony.buildlogic.internal.Constants
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.OciDigest
import org.niis.harmony.buildlogic.internal.utils.parseCsvList
import org.niis.harmony.buildlogic.internal.utils.sanitize
import org.niis.harmony.buildlogic.models.BuildConfig
import org.niis.harmony.buildlogic.models.BuildInfoConfig
import org.niis.harmony.buildlogic.models.CacheConfig
import org.niis.harmony.buildlogic.models.CompileConfig
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.models.DebConfig
import org.niis.harmony.buildlogic.models.DockerOutputMode
import org.niis.harmony.buildlogic.models.DockerTargetConfig
import org.niis.harmony.buildlogic.models.PullPolicy
import org.niis.harmony.buildlogic.models.StagingConfig
import org.niis.harmony.buildlogic.models.VendorConfig
import org.niis.harmony.buildlogic.providers.SourceDateEpochValueSource
import org.niis.harmony.buildlogic.providers.VcsRevisionValueSource
import tools.jackson.module.kotlin.readValue
import java.io.File

object ConfigFactory {

  // ---------------------------------------------------------------------------
  // Public factory methods
  // ---------------------------------------------------------------------------

  fun createBuildConfig(project: Project): BuildConfig {
    val resolver = PropertyResolver(project, component = null)

    return BuildConfig(
      components = resolver.stringList("components")
        .orElse(Constants.BuildDefaults.DEFAULT_COMPONENTS),
      debBuilderImage = resolver.string("deb.builder.image")
        .orElse(Constants.BuildDefaults.DEFAULT_DEB_BUILDER_IMAGE),
      debBuilderTag = resolver.string("deb.builder.tag")
        .orElse(Constants.BuildDefaults.DEFAULT_DEB_BUILDER_TAG),
      debBuilderPullPolicy = resolver.string("deb.builder.pullPolicy")
        .orElse(Constants.BuildDefaults.DEFAULT_DEB_BUILDER_PULL_POLICY)
        .map { PullPolicy.fromString(it) },
      cache = CacheConfig(
        restoreOnly = resolver.boolean("cache.restoreOnly")
          .orElse(Constants.BuildDefaults.DEFAULT_CACHE_RESTORE_ONLY)
      )
    )
  }

  fun createComponentConfig(
    project: Project,
    component: String,
    tools: PluginWiring.ToolProviders
  ): ComponentConfig {
    val resolver = PropertyResolver(project, component)

    val version: Provider<String> = resolver.string("version")
      .orElse(project.provider {
        throw GradleException(
          "Version is missing for component '$component'. Define 'harmony.$component.version'."
        )
      })

    val compileConfig = createCompileConfig(project, component, version, resolver)
    val stagingConfig = createStagingConfig(project, component)
    val buildInfoConfig = createBuildInfoConfig(project, compileConfig.repoDir, resolver, tools)
    val debConfig = createDebConfig(project, resolver, component)
    val dockerConfig = createDockerTargetConfig(resolver, component, version)
    val vendorConfig = createVendorConfig(project, component)

    return ComponentConfig(
      name = component,
      version = version,
      compile = compileConfig,
      staging = stagingConfig,
      deb = debConfig,
      docker = dockerConfig,
      buildInfo = buildInfoConfig,
      vendor = vendorConfig
    )
  }

  // ---------------------------------------------------------------------------
  // Sub-config factories
  // ---------------------------------------------------------------------------

  private fun createCompileConfig(
    project: Project,
    component: String,
    version: Provider<String>,
    resolver: PropertyResolver
  ): CompileConfig {
    val repoDir: Provider<Directory> = resolver.string("compile.repo")
      .map { path -> project.layout.projectDirectory.dir(path) }
      .orElse(project.provider {
        throw GradleException(
          "Mandatory property 'harmony.$component.compile.repo' is not defined in gradle.properties."
        )
      })

    val artifactPrefix = "harmony.$component.compile.artifact."
    val artifactProperties = project.providers.gradlePropertiesPrefixedBy(artifactPrefix)

    val artifacts = artifactProperties.flatMap { propsMap ->
      version.map { ver ->
        propsMap
          .mapKeys { (key, _) -> key.removePrefix(artifactPrefix) }
          .mapValues { (_, pathTemplate) ->
            val expandedPath = pathTemplate.replace("\${harmony.$component.version}", ver)
            repoDir.map { dir -> dir.file(expandedPath) }
          }
      }
    }

    return CompileConfig(
      skipTests = resolver.boolean("compile.skipTests")
        .orElse(Constants.BuildDefaults.DEFAULT_COMPILE_SKIP_TESTS),
      repoDir = repoDir,
      mavenProfiles = resolver.stringList("compile.maven.profiles")
        .orElse(Constants.BuildDefaults.DEFAULT_COMPILE_MAVEN_PROFILES),
      mavenGoals = resolver.stringList("compile.maven.goals")
        .orElse(Constants.BuildDefaults.DEFAULT_COMPILE_MAVEN_GOALS),
      mavenLocalRepo = resolver.string("compile.maven.localRepo")
        .orElse(project.provider { "" }),
      javaVersion = resolver.int("compile.javaVersion")
        .orElse(Constants.BuildDefaults.DEFAULT_COMPILE_JAVA_VERSION),
      artifacts = artifacts
    )
  }

  private fun createStagingConfig(
    project: Project,
    component: String
  ): StagingConfig =
    StagingConfig(
      manifestFile = project.layout.projectDirectory
        .file(stagingManifestPath(component))
        .asFile
    )

  private fun createDebConfig(
    project: Project,
    resolver: PropertyResolver,
    component: String
  ): DebConfig {
    return DebConfig(
      distros = resolver.stringList("deb.distros")
        .orElse(Constants.BuildDefaults.DEFAULT_DEB_DISTROS),
      sign = resolver.boolean("deb.sign")
        .orElse(Constants.BuildDefaults.DEFAULT_DEB_SIGN_ENABLED),
      keyId = resolver.string("deb.keyId"),
      packageName = resolver.string("deb.packageName")
        .orElse(debPackageName(component)),
      gpgHome = resolver.string("deb.gpgHome")
        .orElse(project.provider { defaultGpgHome() })
    )
  }

  private fun createDockerTargetConfig(
    resolver: PropertyResolver,
    component: String,
    version: Provider<String>
  ): DockerTargetConfig {
    val imageName = resolver.string("docker.imageName")
      .orElse(dockerImageNameForComponent(component))

    val tags = resolver.stringList("docker.tags")
      .orElse(version.map { listOf(it.sanitize()) })

    val platforms = resolver.string("docker.platforms")
      .orElse(Constants.BuildDefaults.DEFAULT_DOCKER_PLATFORMS)

    val outputMode = resolver.string("docker.outputMode")
      .orElse(Constants.BuildDefaults.DEFAULT_DOCKER_OUTPUT_MODE)
      .map { DockerOutputMode.fromString(it) }

    val trackBase = resolver.boolean("docker.trackBase")
      .orElse(Constants.BuildDefaults.DEFAULT_DOCKER_TRACK_BASE_ENABLED)

    val pullPolicy = resolver.string("docker.pullPolicy")
      .orElse(Constants.BuildDefaults.DEFAULT_DOCKER_PULL_POLICY)
      .map { PullPolicy.fromString(it) }

    val provenanceEnabled = resolver.boolean("docker.provenanceEnabled")
      .orElse(Constants.BuildDefaults.DEFAULT_DOCKER_PROVENANCE_ENABLED)

    val baseImageDigests = resolver.string("docker.baseImageDigests")
      .map { json ->
        val digests: Map<String, String?> = Mappers.json.readValue(json)
        digests.forEach { (image, digest) ->
          if (digest != null) {
            OciDigest.requireValid(digest, image)
          }
        }
        digests
      }

    return DockerTargetConfig(
      imageName = imageName,
      tags = tags,
      platforms = platforms,
      outputMode = outputMode,
      trackBase = trackBase,
      pullPolicy = pullPolicy,
      provenanceEnabled = provenanceEnabled,
      baseImageDigests = baseImageDigests
    )
  }

  private fun createBuildInfoConfig(
    project: Project,
    repoDir: Provider<Directory>,
    resolver: PropertyResolver,
    tools: PluginWiring.ToolProviders
  ): BuildInfoConfig {
    val epoch = resolver.string("build.epoch")
      .map { it.toLong() }
      .orElse(
        project.providers.of(SourceDateEpochValueSource::class.java) {
          parameters.gitExecutable.set(tools.git)
          parameters.repoDirs.from(repoDir, project.layout.projectDirectory)
          parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
        }
      )

    val revision = resolver.string("build.revision")
      .orElse(
        project.providers.of(VcsRevisionValueSource::class.java) {
          parameters.gitExecutable.set(tools.git)
          parameters.repoDir.set(repoDir)
          parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
        }
      )

    return BuildInfoConfig(
      epoch = epoch,
      revision = revision,
      buildNumber = resolver.int("build.number")
        .orElse(Constants.BuildDefaults.DEFAULT_BUILD_NUMBER)
    )
  }

  private fun createVendorConfig(
    project: Project,
    component: String
  ): VendorConfig {
    val resolver = PropertyResolver(project, component)

    return VendorConfig(
      resolveVersion = { dependency ->
        resolver.vendorMetadata(dependency, "version")
          .orElse(project.provider {
            throw GradleException(
              "Missing version for vendor dependency '$dependency' (required by component '$component')."
            )
          })
      },
      resolveNotation = { dependency, version, classifier ->
        resolveVendorNotation(resolver, dependency, version, classifier)
      }
    )
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

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
    val envOverride = System.getenv("GNUPGHOME")
      ?.takeIf { it.isNotBlank() }

    if (envOverride != null) return envOverride

    val userHome = System.getProperty("user.home")
      ?: throw GradleException(
        "System property 'user.home' is not set. Configure 'harmony.deb.gpgHome'."
      )

    return File(userHome, ".gnupg").absolutePath
  }

  // ---------------------------------------------------------------------------
  // Path helpers
  // ---------------------------------------------------------------------------

  private fun stagingManifestPath(component: String): String =
    "components/$component/manifest.yml"

  private fun debPackageName(component: String): String =
    "harmony-$component"

  private fun dockerImageNameForComponent(component: String): String =
    "niis/harmony-${component.sanitize()}"

  // ---------------------------------------------------------------------------
  // Property resolver
  // ---------------------------------------------------------------------------

  private class PropertyResolver(
    private val project: Project,
    private val component: String?
  ) {
    private val listCache = mutableMapOf<String, Provider<List<String>>>()

    private fun resolve(key: String): Provider<String> {
      return if (component != null) {
        project.providers.gradleProperty("harmony.$component.$key")
          .orElse(project.providers.gradleProperty("harmony.$key"))
      } else {
        project.providers.gradleProperty("harmony.$key")
      }
    }

    fun string(key: String): Provider<String> =
      resolve(key)

    fun stringList(key: String): Provider<List<String>> =
      listCache.getOrPut(key) {
        resolve(key).map { it.parseCsvList() }
      }

    fun boolean(key: String): Provider<Boolean> =
      resolve(key).map { it.toBoolean() }

    fun int(key: String): Provider<Int> =
      resolve(key).map { it.toInt() }

    fun vendorMetadata(dependency: String, key: String): Provider<String> =
      resolve("vendor.$dependency.$key")

    fun vendorMetadataOrNull(dependency: String, key: String): String? =
      vendorMetadata(dependency, key).orNull
  }
}
