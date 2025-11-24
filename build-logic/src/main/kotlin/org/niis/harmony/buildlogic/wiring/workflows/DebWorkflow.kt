package org.niis.harmony.buildlogic.wiring.workflows

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.niis.harmony.buildlogic.internal.utils.BuildOutputPaths
import org.niis.harmony.buildlogic.internal.utils.toTaskName
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.tasks.AssembleStagingTask
import org.niis.harmony.buildlogic.tasks.BuildDebTask
import org.niis.harmony.buildlogic.wiring.PluginWiring

internal fun Project.registerDebWorkflows(
  component: ComponentConfig,
  ctx: PluginWiring.BuildContext,
  assembleDebTasksByDistro: Map<String, TaskProvider<AssembleStagingTask>>
): Map<String, TaskProvider<BuildDebTask>> {

  return component.deb.distros.get().associateWith { distro ->
    val taskName = "buildDeb${component.name.toTaskName()}${distro.toTaskName()}"
    tasks.register(taskName, BuildDebTask::class.java) {
      group = "packaging"
      description = "Builds Debian package for '${component.name}' (distro=$distro)."

      dependsOn(assembleDebTasksByDistro.getValue(distro))

      this.component.set(component.name)
      this.version.set(component.version)
      this.distro.set(distro)
      this.packageName.set(component.deb.packageName)
      this.serviceName.set(component.deb.packageName)
      this.stagingDeb.set(assembleDebTasksByDistro.getValue(distro).flatMap { it.stagingDir })

      this.debSign.set(component.deb.sign)
      this.debKeyId.set(component.deb.keyId)
      this.gpgHome.set(component.deb.gpgHome)
      this.sourceDateEpoch.set(component.buildInfo.epoch)

      this.dockerExecutable.set(ctx.tools.docker)
      this.builderImage.set(ctx.build.debBuilderImage)
      this.builderImageTag.set(ctx.build.debBuilderTag)
      this.cacheRestoreOnly.set(ctx.build.cache.restoreOnly)

      this.debOutDir.set(
        BuildOutputPaths.debOutputDir(project, component.name, component.version, distro)
      )
      this.markerFile.set(
        BuildOutputPaths.debMarker(project, component.name, component.version, distro)
      )
    }
  }
}
