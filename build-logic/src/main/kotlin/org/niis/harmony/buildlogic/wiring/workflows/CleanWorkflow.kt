package org.niis.harmony.buildlogic.wiring.workflows

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.niis.harmony.buildlogic.internal.utils.BuildOutputPaths
import org.niis.harmony.buildlogic.internal.utils.titleCase
import org.niis.harmony.buildlogic.models.ComponentConfig

internal fun Project.registerCleanWorkflows(
  components: Collection<ComponentConfig>
): TaskProvider<Task> {
  val cleanRootBuild = tasks.register("clean", Delete::class.java) {
    group = "build"
    description = "Deletes the root build/ directory."
    delete(layout.buildDirectory)
  }

  val cleanAll = tasks.register("cleanAll") {
    group = "cleanup"
    description = "Runs all clean tasks: deletes build/, and output artifacts."
    dependsOn(cleanRootBuild)
  }

  components.forEach { component ->
    val perComponentCleanTasks = registerComponentCleanTasks(component)
    cleanAll.configure { dependsOn(perComponentCleanTasks) }
  }

  return cleanAll
}

internal fun Project.registerComponentCleanTasks(
  component: ComponentConfig
): TaskProvider<Delete> {
  val name = component.name.titleCase()

  val cleanStagingDocker = tasks.register("cleanStaging${name}Docker", Delete::class.java) {
    group = "cleanup"
    description = "Deletes the Docker staging directory for '${component.name}'."
    delete(BuildOutputPaths.dockerStagingDir(project, component.name))
  }

  val cleanStagingDeb = tasks.register("cleanStaging${name}DebAll", Delete::class.java) {
    group = "cleanup"
    description = "Deletes all Debian staging directories for '${component.name}'."
    delete(BuildOutputPaths.debStagingDir(project, component.name))
  }

  val cleanCompileMetadata = tasks.register("cleanCompileMetadata${name}", Delete::class.java) {
    group = "cleanup"
    description = "Deletes compile metadata for '${component.name}'."
    delete(BuildOutputPaths.compileMarkerDir(project, component.name))
  }

  val cleanDockerMetadata = tasks.register("cleanDockerMetadata${name}", Delete::class.java) {
    group = "cleanup"
    description = "Deletes Docker build metadata for '${component.name}'."
    delete(BuildOutputPaths.dockerMarkerDir(project, component.name))
  }

  val cleanDebMetadata = tasks.register("cleanDebMetadata${name}", Delete::class.java) {
    group = "cleanup"
    description = "Deletes Debian build metadata for '${component.name}'."
    delete(BuildOutputPaths.debMarkerDir(project, component.name))
  }

  val cleanDebArtifacts = tasks.register("cleanDebArtifacts${name}", Delete::class.java) {
    group = "cleanup"
    description = "Deletes all built .deb packages for '${component.name}'."
    delete(BuildOutputPaths.debArtifactsRoot(project, component.name, component.version))
  }

  val cleanJavaTargets = tasks.register("cleanJavaTargets${name}", Delete::class.java) {
    group = "cleanup"
    description = "Deletes all 'target/' directories from the '${component.name}' Maven project."

    val targetDirs = provider {
      fileTree(component.compile.repoDir) { include("**/pom.xml") }
        .files
        .map { it.parentFile.resolve("target") }
        .filter { it.isDirectory }
    }

    delete(targetDirs)
  }

  return tasks.register("clean${name}", Delete::class.java) {
    group = "cleanup"
    description = "Cleans all build outputs for component '${component.name}'."
    dependsOn(
      cleanStagingDocker,
      cleanStagingDeb,
      cleanCompileMetadata,
      cleanDockerMetadata,
      cleanDebMetadata,
      cleanDebArtifacts,
      cleanJavaTargets
    )
  }
}
