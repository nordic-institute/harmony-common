package org.niis.harmony.buildlogic.wiring.workflows

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.niis.harmony.buildlogic.internal.utils.BuildOutputPaths
import org.niis.harmony.buildlogic.internal.utils.toTaskName
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.tasks.CompileMavenComponentTask

internal fun Project.registerCompileWorkflow(
  component: ComponentConfig,
  cacheRestoreOnly: Provider<Boolean>
): TaskProvider<CompileMavenComponentTask> {
  val taskName = "compileJava${component.name.toTaskName()}"
  return tasks.register(taskName, CompileMavenComponentTask::class.java) {
    group = "build"
    description = "Compiles the Maven repository for component '${component.name}'."

    this.component.set(component.name)
    this.version.set(component.version)
    this.repoDir.set(component.compile.repoDir)
    this.mavenProfiles.set(component.compile.mavenProfiles)
    this.mavenGoals.set(component.compile.mavenGoals)
    this.skipTests.set(component.compile.skipTests)
    this.javaVersion.set(component.compile.javaVersion)
    this.sourceDateEpoch.set(component.buildInfo.epoch)

    this.sources.from(
      providers.provider {
        fileTree(component.compile.repoDir.get().asFile) {
          include("**/src/**")
          exclude("**/target/**", ".git/**", ".idea/**", "**/*.iml")
        }
      }
    )
    this.poms.from(
      providers.provider {
        fileTree(component.compile.repoDir.get().asFile) { include("**/pom.xml") }
      }
    )
    this.wrapper.from(
      component.compile.repoDir.map { it.dir(".mvn") },
      component.compile.repoDir.map { it.file("mvnw") },
      component.compile.repoDir.map { it.file("mvnw.cmd") }
    )

    this.markerFile.set(BuildOutputPaths.compileMarker(project, component.name, component.version))

    component.compile.artifacts.get().values.forEach { artifactProvider ->
      this.artifacts.from(artifactProvider)
    }

    this.cacheRestoreOnly.set(cacheRestoreOnly)
  }
}
