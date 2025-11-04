package org.niis.harmony.buildlogic.wiring

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.niis.harmony.buildlogic.internal.utils.toTaskName
import org.niis.harmony.buildlogic.models.BuildConfig
import org.niis.harmony.buildlogic.models.ComponentConfig
import org.niis.harmony.buildlogic.wiring.workflows.registerCleanWorkflows
import org.niis.harmony.buildlogic.wiring.workflows.registerCompileWorkflow
import org.niis.harmony.buildlogic.wiring.workflows.registerDebWorkflows
import org.niis.harmony.buildlogic.wiring.workflows.registerDockerWorkflowForTarget
import org.niis.harmony.buildlogic.wiring.workflows.registerPrintDockerBaseDigestsTask
import org.niis.harmony.buildlogic.wiring.workflows.registerStagingWorkflows

object PluginWiring {

  internal data class BuildContext(
    val root: Project,
    val build: BuildConfig,
    val components: Map<String, ComponentConfig>,
    val tools: ToolProviders,
    val vendorResolver: (component: String, dependency: String, classifier: String?) -> Provider<RegularFile>
  )

  data class ToolProviders(
    val docker: Provider<String>,
    val git: Provider<String>,
    val execTimeoutSeconds: Provider<Long>
  )

  fun applyTo(root: Project) {
    configureVendorRepositories(root)

    val context = createBuildContext(root)

    root.registerCleanWorkflows(context.components.values)

    context.components.values.forEach { componentConfig ->
      registerComponentWorkflows(componentConfig, context)
    }
  }

  private fun createBuildContext(root: Project): BuildContext {
    val buildConfig = ConfigFactory.createBuildConfig(root)

    val tools = createToolProviders(root)
    val components = buildConfig.components.get()
      .associateWith { name -> ConfigFactory.createComponentConfig(root, name, tools) }

    val vendorResolver = { componentName: String, dependency: String, classifier: String? ->
      val componentConfig = components.getValue(componentName)
      resolveVendorFile(root, componentConfig, dependency, classifier)
    }

    return BuildContext(root, buildConfig, components, tools, vendorResolver)
  }

  private fun createToolProviders(root: Project): ToolProviders = ToolProviders(
    docker = root.providers.gradleProperty("harmony.exec.docker").orElse("docker"),
    git = root.providers.gradleProperty("harmony.exec.git").orElse("git"),
    execTimeoutSeconds = root.providers.gradleProperty("harmony.exec.timeoutSec")
      .map { it.toLong() }
      .orElse(120L)
  )

  private fun registerComponentWorkflows(component: ComponentConfig, context: BuildContext) {
    val compileTask = context.root.registerCompileWorkflow(component)
    val stagingTasks = context.root.registerStagingWorkflows(component, context, dependsOn = compileTask)

    val dockerfileProvider = stagingTasks.docker.flatMap { it.stagingDir.file("Dockerfile") }

    context.root.registerDockerWorkflowForTarget(
      nameForTask = component.name,
      target = component.docker,
      tools = context.tools,
      version = component.version,
      epoch = component.buildInfo.epoch,
      revision = component.buildInfo.revision,
      buildId = component.buildInfo.buildId,
      dependsOnTask = stagingTasks.docker,
      contextDirProvider = stagingTasks.docker.flatMap { it.stagingDir },
      dockerfileProvider = dockerfileProvider
    )

    context.root.registerPrintDockerBaseDigestsTask(
      nameForTask = component.name,
      tools = context.tools,
      dockerfileProvider = dockerfileProvider
    )

    val debTasksByDistro = context.root.registerDebWorkflows(component, context, stagingTasks.debByDistro)

    context.root.tasks.register("buildDeb${component.name.toTaskName()}") {
      group = "packaging"
      description = "Builds Debian packages for '${component.name}' for all configured distros."
      dependsOn(debTasksByDistro.values)
    }
  }

  private fun resolveVendorFile(
    project: Project,
    componentConfig: ComponentConfig,
    dependency: String,
    classifier: String?
  ): Provider<RegularFile> {
    return componentConfig.vendor.resolveVersion(dependency).flatMap { version ->
      val notation = componentConfig.vendor.resolveNotation(dependency, version, classifier)

      val dep = project.dependencies.create(notation)
      val config = project.configurations.detachedConfiguration(dep).apply {
        isTransitive = false
        attributes.attribute(
          Usage.USAGE_ATTRIBUTE,
          project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME)
        )
      }
      project.layout.file(project.provider { config.singleFile })
    }
  }

  private fun configureVendorRepositories(root: Project) {
    root.repositories.apply {
      ivy {
        name = "S6OverlayReleases"
        url = root.uri("https://github.com/just-containers/s6-overlay/releases/download")
        patternLayout { artifact("v[revision]/[artifact]-[classifier].[ext]") }
        metadataSources { artifact() }
        content { includeModule("s6-overlay", "s6-overlay") }
      }
      ivy {
        name = "LiquibaseReleases"
        url = root.uri("https://github.com/liquibase/liquibase/releases/download")
        patternLayout { artifact("v[revision]/liquibase-[revision].[ext]") }
        metadataSources { artifact() }
        content { includeModule("liquibase", "liquibase") }
      }
      ivy {
        name = "ApacheTomcatArchives"
        url = root.uri("https://archive.apache.org/dist/tomcat/tomcat-9")
        patternLayout { artifact("v[revision]/bin/apache-tomcat-[revision].[ext]") }
        metadataSources { artifact() }
        content { includeModule("apache", "tomcat") }
      }
      mavenCentral()
    }
  }
}
