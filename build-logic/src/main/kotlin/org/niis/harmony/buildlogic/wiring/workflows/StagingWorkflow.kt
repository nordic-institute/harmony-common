package org.niis.harmony.buildlogic.wiring.workflows

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.BuildOutputPaths
import org.niis.harmony.buildlogic.internal.utils.ManifestParser
import org.niis.harmony.buildlogic.internal.utils.toTaskName
import org.niis.harmony.buildlogic.models.ArtifactReference
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.ManifestReference
import org.niis.harmony.buildlogic.models.ProjectReference
import org.niis.harmony.buildlogic.models.Scope
import org.niis.harmony.buildlogic.models.VendorReference
import org.niis.harmony.buildlogic.providers.ManifestFingerprintValueSource
import org.niis.harmony.buildlogic.tasks.AssembleStagingTask
import org.niis.harmony.buildlogic.tasks.inputs.AliasedClasspathInput
import org.niis.harmony.buildlogic.tasks.inputs.AliasedFileInput
import org.niis.harmony.buildlogic.tasks.inputs.AliasedPathInput
import org.niis.harmony.buildlogic.wiring.PluginWiring

internal data class ContentProviders(
  val artifacts: Provider<List<AliasedClasspathInput>>,
  val vendors: Provider<List<AliasedFileInput>>,
  val projectPaths: Provider<List<AliasedPathInput>>
)

private data class ManifestInputs(
  val docker: ContentProviders,
  val debByDistro: Map<String, ContentProviders>
)

internal data class StagingTasks(
  val debByDistro: Map<String, TaskProvider<AssembleStagingTask>>,
  val docker: TaskProvider<AssembleStagingTask>
)

internal fun Project.registerStagingWorkflows(
  component: ComponentConfig,
  context: PluginWiring.BuildContext,
  dependsOn: TaskProvider<*>
): StagingTasks {
  val allInputs = resolveManifestInputs(this, context, component)

  val dockerTask = registerAssembleStagingTask(
    component = component,
    scope = Scope.DOCKER,
    contentProviders = allInputs.docker,
    taskNameSuffix = "Docker",
    outputDir = BuildOutputPaths.dockerStagingDir(project, component.name),
    cacheRestoreOnly = context.build.cache.restoreOnly
  ).also { it.configure { dependsOn(dependsOn) } }

  val debTasks = component.deb.distros.get().associateWith { distro ->
    registerAssembleStagingTask(
      component = component,
      scope = Scope.DEB,
      distro = distro,
      contentProviders = allInputs.debByDistro.getValue(distro),
      taskNameSuffix = "Deb${distro.toTaskName()}",
      outputDir = BuildOutputPaths.debStagingDir(project, component.name, distro),
      cacheRestoreOnly = context.build.cache.restoreOnly
    ).also { it.configure { dependsOn(dependsOn) } }
  }

  return StagingTasks(debByDistro = debTasks, docker = dockerTask)
}

private fun resolveManifestInputs(
  root: Project,
  ctx: PluginWiring.BuildContext,
  component: ComponentConfig
): ManifestInputs {
  val manifestText: Provider<String> = root.providers
    .fileContents(root.layout.file(root.provider { component.staging.manifestFile }))
    .asText

  fun applicableRefs(scope: Scope, distro: String?): Provider<List<ManifestReference>> =
    manifestText.map { text ->
      val manifest = Mappers.yaml.readValue(text, Manifest::class.java)
      manifest.inputs
        .filter { ManifestParser.applies(it.`when`, scope, distro) }
        .map { ManifestReference.fromString(it.from) }
    }

  fun buildContentProviders(refsProvider: Provider<List<ManifestReference>>): ContentProviders =
    ContentProviders(
      artifacts = resolveArtifactInputs(root, component, refsProvider),
      vendors = resolveVendorInputs(root, ctx, component, refsProvider),
      projectPaths = resolveProjectInputs(root, component, refsProvider)
    )

  val dockerContent = buildContentProviders(applicableRefs(Scope.DOCKER, null))
  val debContentByDistro = component.deb.distros.get().associateWith { distro ->
    buildContentProviders(applicableRefs(Scope.DEB, distro))
  }

  return ManifestInputs(docker = dockerContent, debByDistro = debContentByDistro)
}

private fun resolveArtifactInputs(
  root: Project,
  component: ComponentConfig,
  refsProvider: Provider<List<ManifestReference>>
): Provider<List<AliasedClasspathInput>> = refsProvider.map { refs ->
  val available = component.compile.artifacts.get()
  refs.filterIsInstance<ArtifactReference>().map { ref ->
    root.objects.newInstance(AliasedClasspathInput::class.java).apply {
      alias.set(ref.alias)
      file.set(
        available[ref.alias]
          ?: error("Unknown artifact alias '${ref.alias}' in manifest for component '${component.name}'. " +
                   "Available: ${available.keys.sorted().joinToString(", ")}")
      )
    }
  }
}

private fun resolveVendorInputs(
  root: Project,
  ctx: PluginWiring.BuildContext,
  component: ComponentConfig,
  refsProvider: Provider<List<ManifestReference>>
): Provider<List<AliasedFileInput>> = refsProvider.map { refs ->
  refs.filterIsInstance<VendorReference>().map { ref ->
    root.objects.newInstance(AliasedFileInput::class.java).apply {
      alias.set(ref.alias)
      file.set(ctx.vendorResolver(component.name, ref.dependency, ref.classifier))
    }
  }
}

private fun resolveProjectInputs(
  root: Project,
  component: ComponentConfig,
  refsProvider: Provider<List<ManifestReference>>
): Provider<List<AliasedPathInput>> = refsProvider.map { refs ->
  refs.filterIsInstance<ProjectReference>().map { ref ->
    root.objects.newInstance(AliasedPathInput::class.java).apply {
      alias.set(ref.path)
      relBasePath.set(ref.path)
      val dirProv  = root.layout.projectDirectory.dir(ref.path)
      val fileProv = root.layout.projectDirectory.file(ref.path)
      inputs.from(
        root.providers.provider {
          val dir = dirProv.asFile
          val file = fileProv.asFile
          when {
            dir.isDirectory -> dirProv.asFileTree
            file.isFile     -> root.objects.fileCollection().from(file)
            else -> error("Project path '${ref.path}' not found for component '${component.name}'.")
          }
        }
      )
    }
  }
}

private fun Project.registerAssembleStagingTask(
  component: ComponentConfig,
  scope: Scope,
  distro: String? = null,
  contentProviders: ContentProviders,
  taskNameSuffix: String,
  outputDir: Provider<Directory>,
  cacheRestoreOnly: Provider<Boolean>
): TaskProvider<AssembleStagingTask> {
  val taskName = "assembleStaging${component.name.toTaskName()}$taskNameSuffix"
  return tasks.register(taskName, AssembleStagingTask::class.java) {
    group = "staging"
    description = "Assembles the staging tree for '${component.name}' (${scope.name.lowercase()}${distro?.let { "/$it" } ?: ""})."

    this.component.set(component.name)
    this.scope.set(scope)
    this.distro.set(distro)
    this.sourceDateEpoch.set(component.buildInfo.epoch)
    this.manifestFile.set(component.staging.manifestFile)
    this.manifestFingerprint.set(providers.of(ManifestFingerprintValueSource::class.java) {
      parameters.manifestFile.set(component.staging.manifestFile)
      parameters.scope.set(scope)
      parameters.distro.set(distro ?: "")
    })

    this.artifactClasspath.set(contentProviders.artifacts)
    this.vendorFiles.set(contentProviders.vendors)
    this.projectPaths.set(contentProviders.projectPaths)

    this.stagingDir.set(outputDir)
    this.cacheRestoreOnly.set(cacheRestoreOnly)
  }
}
