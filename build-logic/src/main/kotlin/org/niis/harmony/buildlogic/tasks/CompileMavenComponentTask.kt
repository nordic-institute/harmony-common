package org.niis.harmony.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import org.niis.harmony.buildlogic.internal.Constants
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.env.HostInfoService
import org.niis.harmony.buildlogic.internal.env.HostOs
import org.niis.harmony.buildlogic.models.CompileMarker
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class CompileMavenComponentTask @Inject constructor(
  private val execOps: ExecOperations,
  private val toolchains: JavaToolchainService,
  private val layout: ProjectLayout
) : DefaultTask() {

  @get:Internal
  abstract val component: Property<String>

  @get:Internal
  abstract val version: Property<String>

  @get:Internal
  abstract val repoDir: DirectoryProperty

  @get:Internal
  abstract val cacheRestoreOnly: Property<Boolean>

  @get:Input
  abstract val sourceDateEpoch: Property<Long>

  @get:Input
  abstract val mavenProfiles: ListProperty<String>

  @get:Input
  abstract val mavenGoals: ListProperty<String>

  @get:Input
  abstract val skipTests: Property<Boolean>

  @get:Input
  abstract val javaVersion: Property<Int>

  @get:Input
  val javaRuntimeId: String
    get() {
      val launcherMetadata = toolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.get()))
      }.get().metadata
      return "${launcherMetadata.vendor}:${launcherMetadata.javaRuntimeVersion}"
    }

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sources: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val poms: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val wrapper: ConfigurableFileCollection

  @get:OutputFile
  abstract val markerFile: RegularFileProperty

  @get:OutputFiles
  abstract val artifacts: ConfigurableFileCollection

  @TaskAction
  fun execute() {
    check(!(cacheRestoreOnly.getOrElse(false))) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${project.providers.gradleProperty("harmony.${component.get()}.build.number").orNull ?: "unknown"}
      Component:    ${component.get()}
      Version:      ${version.get()}
      """.trimIndent()
    }

    val repositoryDir = repoDir.get().asFile.also {
      require(it.isDirectory) { "Maven repository directory does not exist: ${it.absolutePath}" }
    }

    logger.lifecycle(
      "Compiling Maven component '{}' with JDK {} and goals {}",
      repositoryDir.name, javaVersion.get(), mavenGoals.get().joinToString(" ")
    )

    val mavenWrapperExecutable = findMavenWrapper(repositoryDir)
    val arguments = buildMavenArguments()
    val environment = buildMavenEnvironment()

    logger.info("  Executing command: {} {}", mavenWrapperExecutable.path, arguments.joinToString(" "))

    execOps.exec {
      commandLine(mavenWrapperExecutable.absolutePath, *arguments.toTypedArray())
      workingDir = repositoryDir
      if (environment.isNotEmpty()) environment(environment)
      isIgnoreExitValue = false
    }

    validateArtifacts()
    writeDeterministicMarker()
    logger.lifecycle("Successfully compiled {}. Marker file at: {}", repositoryDir.name, markerFile.get().asFile)
  }

  private fun findMavenWrapper(repositoryDir: File): File {
    val os = HostInfoService.detectOs()
    val wrapperName = if (os == HostOs.WINDOWS) Constants.MavenBuild.WRAPPER_WINDOWS else Constants.MavenBuild.WRAPPER_UNIX

    return repositoryDir.resolve(wrapperName).also {
      require(it.isFile) { "Maven wrapper not found at: ${it.absolutePath}" }

      if (os != HostOs.WINDOWS && !it.canExecute()) {
        logger.info("Making wrapper executable: {}", it.path)
        it.setExecutable(true)
      }
    }
  }

  private fun buildMavenArguments(): List<String> {
    return buildList {
      addAll(Constants.MavenBuild.DEFAULT_ARGS)
      add("${Constants.MavenBuild.PROP_LOCAL_REPO}=${temporaryDir.resolve("m2").absolutePath}")

      val profiles = mavenProfiles.get().filter { it.isNotBlank() }
      if (profiles.isNotEmpty()) {
        add("-P" + profiles.joinToString(","))
      }

      if (skipTests.getOrElse(false)) {
        logger.info("Maven tests are skipped for this compilation.")
        addAll(Constants.MavenBuild.SKIP_TESTS_ARGS)
      }

      addAll(mavenGoals.get())
    }
  }

  private fun buildMavenEnvironment(): Map<String, String> {
    val javaLauncher = toolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(javaVersion.get()))
    }.get()

    val javaHome = javaLauncher.metadata.installationPath.asFile
    val pathWithJava = "${javaHome.resolve("bin")}${File.pathSeparator}${System.getenv("PATH")}"

    return mapOf(
      "SOURCE_DATE_EPOCH" to sourceDateEpoch.get().toString(),
      "JAVA_HOME" to javaHome.absolutePath,
      "PATH" to pathWithJava,
      "MAVEN_OPTS" to "" // Clear to ensure consistent builds
    )
  }

  private fun validateArtifacts() {
    val missingArtifacts = artifacts.files.filterNot(File::exists)
    check(missingArtifacts.isEmpty()) {
      "Maven compilation completed but expected artifacts were not generated:\n" +
      missingArtifacts.joinToString("\n") { "  - ${it.absolutePath}" }
    }
  }

  private fun writeDeterministicMarker() {
    val projectRoot = layout.projectDirectory.asFile.toPath()

    val artifactPaths = artifacts.files
      .sortedBy { it.absolutePath }
      .map { file ->
        runCatching { projectRoot.relativize(file.toPath()).toString() }
          .getOrElse { file.absolutePath }
      }

    val marker = CompileMarker(
      component = component.get(),
      version = version.get(),
      componentRepo = repoDir.get().asFile.name,
      javaVersion = javaVersion.get(),
      javaRuntimeId = javaRuntimeId,
      mavenGoals = mavenGoals.get(),
      mavenProfiles = mavenProfiles.get(),
      sourceDateEpoch = sourceDateEpoch.get(),
      artifacts = artifactPaths,
      timestamp = System.currentTimeMillis()
    )

    val out = markerFile.get().asFile
    out.parentFile.mkdirs()
    out.writeText(Mappers.json.writerWithDefaultPrettyPrinter().writeValueAsString(marker))
    logger.lifecycle("Successfully wrote compile marker to: {}", out.absolutePath)
  }
}
