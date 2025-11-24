package org.niis.harmony.buildlogic.wiring.workflows

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.BuildOutputPaths
import org.niis.harmony.buildlogic.internal.utils.toTaskName
import org.niis.harmony.buildlogic.models.DockerOutputMode
import org.niis.harmony.buildlogic.models.DockerTargetConfig
import org.niis.harmony.buildlogic.providers.BaseImageDigestsValueSource
import org.niis.harmony.buildlogic.tasks.BuildDockerTask
import org.niis.harmony.buildlogic.wiring.PluginWiring

internal fun Project.registerDockerWorkflowForTarget(
  nameForTask: String,
  target: DockerTargetConfig,
  tools: PluginWiring.ToolProviders,
  version: Provider<String>,
  epoch: Provider<Long>,
  revision: Provider<String>,
  buildNumber: Provider<Int>,
  dependsOnTask: TaskProvider<*>,
  contextDirProvider: Provider<Directory>,
  dockerfileProvider: Provider<RegularFile>,
  cacheRestoreOnly: Provider<Boolean>
): TaskProvider<BuildDockerTask> {
  val taskName = "buildDocker${nameForTask.toTaskName()}"
  return tasks.register(taskName, BuildDockerTask::class.java) {
    group = "packaging"
    description = "Builds the Docker image for '$nameForTask'."

    dependsOn(dependsOnTask)

    this.component.set(nameForTask)
    this.imageName.set(target.imageName)
    this.tags.set(target.tags)
    this.platforms.set(target.platforms)
    this.dockerExecutable.set(tools.docker)

    this.outputMode.set(target.outputMode)
    this.pullAlways.set(target.pullAlways)
    this.provenanceDisabled.set(target.provenanceDisabled)

    this.sourceDateEpoch.set(epoch)
    this.version.set(version)
    this.vcsRevision.set(revision)
    this.dockerfile.set(dockerfileProvider)
    this.dockerContext.set(contextDirProvider)
    this.buildNumber.set(buildNumber)
    this.cacheRestoreOnly.set(cacheRestoreOnly)

    this.baseImageDigests.set(
      target.baseImageDigests.orElse(
        providers.of(BaseImageDigestsValueSource::class.java) {
          parameters.dockerExecutable.set(tools.docker)
          parameters.dockerfile.set(dockerfileProvider)
          parameters.enabled.set(target.trackBase)
          parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
        }
      )
    )

    this.markerFile.set(BuildOutputPaths.dockerMarker(project, nameForTask, version))

    target.outputMode.map { mode ->
      when (mode) {
        DockerOutputMode.TAR -> this.tarOutput.set(BuildOutputPaths.dockerOutputTar(project, nameForTask, version))
        DockerOutputMode.OCI -> this.ociOutput.set(BuildOutputPaths.dockerOutputOci(project, nameForTask, version))
        else -> { /* LOAD and PUSH don't produce file outputs */ }
      }
    }.get()
  }
}

internal fun Project.registerPrintDockerBaseDigestsTask(
  nameForTask: String,
  tools: PluginWiring.ToolProviders,
  dockerfileProvider: Provider<RegularFile>
) {
  val taskName = "printDockerBaseDigests${nameForTask.toTaskName()}"
  tasks.register(taskName) {
    group = "help"
    description = "Prints Docker base image digests for '$nameForTask' (for security monitoring) as JSON."

    doLast {
      val digests = providers.of(BaseImageDigestsValueSource::class.java) {
        parameters.dockerExecutable.set(tools.docker)
        parameters.dockerfile.set(dockerfileProvider)
        parameters.enabled.set(true)
        parameters.execTimeoutSeconds.set(tools.execTimeoutSeconds)
      }.get()

      val structuredMap = BaseImageDigestsValueSource.toStructuredMap(digests)

      println(Mappers.json.writeValueAsString(structuredMap))
    }
  }
}
