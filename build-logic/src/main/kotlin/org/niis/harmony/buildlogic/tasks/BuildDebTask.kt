package org.niis.harmony.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.niis.harmony.buildlogic.internal.Constants
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import org.niis.harmony.buildlogic.models.DebBuildMarker
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

@CacheableTask
abstract class BuildDebTask @Inject constructor(
  private val execOps: ExecOperations,
  private val fileSystemOperations: FileSystemOperations,
  private val layout: ProjectLayout
) : DefaultTask() {

  @get:Internal
  abstract val component: Property<String>

  @get:Internal
  abstract val cacheRestoreOnly: Property<Boolean>

  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val debSign: Property<Boolean>

  @get:Input
  abstract val sourceDateEpoch: Property<Long>

  @get:Input
  abstract val packageName: Property<String>

  @get:Input
  abstract val distro: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val stagingDeb: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val debKeyId: Property<String>

  @get:Input
  @get:Optional
  abstract val serviceName: Property<String>

  @get:Input
  @get:Optional
  abstract val gpgHome: Property<String>

  @get:Input
  abstract val dockerExecutable: Property<String>

  @get:Input
  abstract val builderImage: Property<String>

  @get:Input
  abstract val builderImageTag: Property<String>

  @get:OutputDirectory
  abstract val debOutDir: DirectoryProperty

  @get:OutputFile
  abstract val markerFile: RegularFileProperty

  init {
    outputs.cacheIf("Signing makes artifacts host-dependent") {
      !debSign.getOrElse(false)
    }
  }

  private data class Workspace(
    val rootDir: File,
    val sourceDir: File,
    val debianDir: File
  )

  private data class SigningSetup(
    val gpgHomeCopy: File,
    val env: Map<String, String>
  )

  @TaskAction
  fun execute() {
    check(!(cacheRestoreOnly.getOrElse(false))) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${project.providers.gradleProperty("harmony.${component.get()}.build.number").orNull ?: "unknown"}
      Component:    ${component.get()}
      Version:      ${version.get()}
      Distro:       ${distro.get()}
      """.trimIndent()
    }

    val workspace = setupWorkspace()
    prepareDebianDirectoryFromStaging(workspace)
    val signingSetup = prepareSigningSetup(workspace)

    logger.lifecycle(
      "Building Debian package for component='{}', distro='{}', sign={}, dockerImage='{}:{}'",
      component.get(),
      distro.get(),
      debSign.getOrElse(false),
      builderImage.get(),
      builderImageTag.get()
    )

    try {
      val dpkgArgs = buildDpkgCommandArgs()
      executeInDocker(workspace, dpkgArgs, signingSetup)

      val publishedArtifacts = findAndPublishArtifacts(workspace.rootDir, debOutDir.get().asFile)
      writeDeterministicMarker(publishedArtifacts)
    } finally {
      signingSetup?.gpgHomeCopy?.deleteRecursively()
    }
  }

  private fun setupWorkspace(): Workspace {
    val rootDir = temporaryDir.resolve("deb-build-root").apply {
      deleteRecursively()
      mkdirs()
    }
    val sourceDir = rootDir.resolve("source-package").apply { mkdirs() }
    val debianDir = sourceDir.resolve("debian").apply { mkdirs() }

    return Workspace(rootDir, sourceDir, debianDir)
  }

  private fun prepareDebianDirectoryFromStaging(workspace: Workspace) {
    val stagingRoot = stagingDeb.get().asFile
    val debSource = stagingRoot.resolve("deb")

    require(debSource.isDirectory) {
      "Debian control directory is missing at '${debSource.absolutePath}' for component='${component.get()}', distro='${distro.get()}'" +
      "Ensure the manifest copies control files into '/deb' for scope=deb and the current distro."
    }

    fileSystemOperations.copy {
      from(debSource)
      into(workspace.debianDir)
    }

    val serviceNamePlaceholder = workspace.debianDir.resolve("service")
    if (serviceNamePlaceholder.isFile) {
      val finalServiceName = serviceName.orNull?.takeIf { it.isNotBlank() } ?: packageName.get()
      val finalServiceFile = workspace.debianDir.resolve("$finalServiceName.service")
      serviceNamePlaceholder.renameTo(finalServiceFile)
    }

    validateInstallFile(workspace, stagingRoot)
  }

  private fun validateInstallFile(workspace: Workspace, stagingRoot: File) {
    val installFile = workspace.debianDir.resolve("install")

    check(installFile.exists()) {
      """
      debian/install file is missing for component='${component.get()}', distro='${distro.get()}'.
      This file is required to specify which directories from staging should be packaged.
      Create it at: components/${component.get()}/packaging/deb/generic/install
      """.trimIndent()
    }

    val declaredPaths = installFile.readLines()
      .filter { it.isNotBlank() && !it.startsWith("#") }
      .map { line ->
        line.trim().split(Regex("\\s+")).first().removePrefix("/")
      }
      .filter { it.isNotBlank() }

    val missingPaths = declaredPaths.filter { path ->
      !stagingRoot.resolve(path).exists()
    }

    check(missingPaths.isEmpty()) {
      """
      debian/install references paths that don't exist in staging for component='${component.get()}':
      ${missingPaths.joinToString("\n") { "  - /$it" }}
      Ensure manifest.yml generates these paths or update debian/install
      """.trimIndent()
    }

    val stagingDirectories = stagingRoot.listFiles()
      ?.filter { it.isDirectory && it.name != "deb" }
      ?.map { it.name }
      ?.toSet()
      ?: emptySet()

    val undeclaredPaths = stagingDirectories - declaredPaths.toSet()

    if (undeclaredPaths.isNotEmpty()) {
      logger.warn(
        """
        Staging directories not declared in debian/install for component='${component.get()}':
        ${undeclaredPaths.sorted().joinToString("\n") { "  - /$it" }}
        These directories will NOT be included in the .deb package.
        """.trimIndent()
      )
    }

    logger.info(
      "Validated debian/install for component='${component.get()}': ${declaredPaths.size} paths declared, ${missingPaths.size} missing, ${undeclaredPaths.size} undeclared"
    )
  }

  private fun buildDpkgCommandArgs(): List<String> {
    val command = mutableListOf(
      Constants.DebianPackaging.FLAG_BINARY_ONLY,
      Constants.DebianPackaging.FLAG_FAKEROOT
    )
    if (debSign.getOrElse(false)) {
      val key = requireSigningKeyId()
      command.add("${Constants.DebianPackaging.FLAG_SIGN_KEY}$key")
    } else {
      command.addAll(Constants.DebianPackaging.FLAGS_UNSIGNED)
    }

    return command
  }

  private fun prepareSigningSetup(workspace: Workspace): SigningSetup? {
    if (!debSign.getOrElse(false)) {
      return null
    }

    val keyId = requireSigningKeyId()
    val configuredHome = gpgHome.orNull?.takeIf { it.isNotBlank() }
      ?: error(
        "Signing is enabled for component='${component.get()}' but no GnuPG home was configured. " +
          "Set 'harmony.${component.get()}.deb.gpgHome' or 'harmony.deb.gpgHome'."
      )

    val hostHome = project.file(configuredHome).absoluteFile
    require(hostHome.isDirectory) {
      "Configured GnuPG home '${hostHome.absolutePath}' does not exist or is not a directory."
    }

    val copyTarget = workspace.rootDir.resolve("gnupg-home").apply {
      deleteRecursively()
      mkdirs()
    }
    fileSystemOperations.copy {
      from(hostHome)
      into(copyTarget)
    }
    copyTarget.setReadable(true, true)
    copyTarget.setWritable(true, true)
    copyTarget.setExecutable(true, true)

    logger.info(
      "Using GnuPG home '{}' to sign component='{}' with key '{}'",
      hostHome.absolutePath,
      component.get(),
      keyId
    )

    val containerHome = "/build/gnupg-home"
    return SigningSetup(
      gpgHomeCopy = copyTarget,
      env = mapOf(
        "GNUPGHOME" to containerHome,
        "DEBSIGN_KEYID" to keyId,
        "GPG_TTY" to "/dev/tty"
      )
    )
  }

  private fun executeInDocker(workspace: Workspace, dpkgArgs: List<String>, signing: SigningSetup?) {
    val image = builderImage.get().takeIf { it.isNotBlank() }
      ?: error("Debian builder image repository is required (set harmony.deb.builder.image).")
    val tag = builderImageTag.get().takeIf { it.isNotBlank() }
      ?: error("Debian builder image tag is required (set harmony.deb.builder.tag).")
    val builderImageRef = "$image:$tag"

    refreshBuilderImage(builderImageRef)

    val command = buildList {
      add(dockerExecutable.get())
      addAll(listOf("run", "--rm"))
      add("--user"); add(resolveUidGid())
      add("--env"); add("TZ=UTC")
      add("--env"); add("SOURCE_DATE_EPOCH=${sourceDateEpoch.get()}")
      add("--env"); add("STAGING_DIR=/staging")
      signing?.env?.forEach { (key, value) ->
        add("--env"); add("$key=$value")
      }
      add("--volume"); add("${workspace.rootDir.absolutePath}:/build")
      add("--volume"); add("${stagingDeb.get().asFile.absolutePath}:/staging:ro")
      add("--workdir"); add("/build/source-package")
      add(builderImageRef)
      add("dpkg-buildpackage")
      addAll(dpkgArgs)
    }

    execOps.exec {
      commandLine(command)
      isIgnoreExitValue = false
    }
  }

  private fun refreshBuilderImage(builderImageRef: String) {
    val result = ProcessRunner.execute(listOf(dockerExecutable.get(), "pull", builderImageRef))
    if (!result.isSuccess) {
      logger.warn(
        "Could not pull builder image '{}'. Assuming it is available locally. Exit code={}, stderr={}",
        builderImageRef,
        result.exitCode,
        result.stderr
      )
    } else {
      logger.lifecycle("Using builder image '{}'. Digest: {}", builderImageRef, result.stdout.lineSequence().lastOrNull())
    }
  }

  private fun findAndPublishArtifacts(buildRootDir: File, outputDir: File): List<File> {
    outputDir.mkdirs()

    val changesFile = buildRootDir.walk()
      .filter { it.isFile && it.name.endsWith(".changes") }
      .maxByOrNull { it.lastModified() }

    val artifactsToPublish = if (changesFile != null) {
      logger.info("Found .changes file, parsing artifacts from: ${changesFile.name}")
      parseArtifactsFromChangesFile(changesFile, buildRootDir)
    } else {
      logger.warn("No .changes file found in {}. Falling back to finding all .deb, .buildinfo files.", buildRootDir)
      buildRootDir.walk()
        .filter { it.isFile && (it.name.endsWith(".deb") || it.name.endsWith(".buildinfo")) }
        .toList()
    }

    require(artifactsToPublish.isNotEmpty()) {
      "Build succeeded but no Debian artifacts (.deb, .buildinfo) were found in ${buildRootDir.absolutePath}"
    }

    val publishedFiles = mutableListOf<File>()
    artifactsToPublish.forEach { sourceFile ->
      val destinationFile = outputDir.resolve(sourceFile.name)
      Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
      publishedFiles.add(destinationFile)
    }

    logger.lifecycle(
      "Published {} Debian artifacts to {}:\n{}",
      publishedFiles.size,
      outputDir.absolutePath,
      publishedFiles.joinToString("\n") { " - ${it.name}" }
    )

    return publishedFiles
  }

  private fun parseArtifactsFromChangesFile(changesFile: File, baseDir: File): List<File> {
    return changesFile.readLines()
      .dropWhile { !it.trim().equals("Files:", ignoreCase = true) }
      .drop(1)
      .mapNotNull { line -> line.trim().split(Regex("\\s+")).lastOrNull() }
      .map { baseDir.resolve(it) }
      .filter { it.exists() }
      .plus(changesFile)
      .distinct()
  }

  private fun requireSigningKeyId(): String {
    return debKeyId.orNull?.takeIf { it.isNotBlank() }
      ?: error(
        "Signing is enabled for component='${component.get()}' but 'harmony.${component.get()}.deb.keyId' was not provided."
      )
  }

  private fun resolveUidGid(): String {
    return try {
      val uid = ProcessRunner.execute(listOf("id", "-u")).stdout.trim()
      val gid = ProcessRunner.execute(listOf("id", "-g")).stdout.trim()
      if (uid.isNotBlank() && gid.isNotBlank()) "$uid:$gid" else "0:0"
    } catch (e: Exception) {
      logger.debug("Could not resolve local UID/GID using 'id' command, falling back to '0:0'. Error: ${e.message}")
      "0:0"
    }
  }

  private fun writeDeterministicMarker(publishedArtifacts: List<File>) {
    val projectRoot = layout.projectDirectory.asFile.toPath()

    val artifactPaths = publishedArtifacts
      .sortedBy { it.absolutePath }
      .map { file ->
        runCatching { projectRoot.relativize(file.toPath()).toString() }
          .getOrElse { file.absolutePath }
      }

    val marker = DebBuildMarker(
      component = component.get(),
      version = version.get(),
      distro = distro.get(),
      packageName = packageName.get(),
      signed = debSign.getOrElse(false),
      keyId = if (debSign.getOrElse(false)) debKeyId.orNull else null,
      builderImage = builderImage.get(),
      builderImageTag = builderImageTag.get(),
      sourceDateEpoch = sourceDateEpoch.get(),
      artifacts = artifactPaths,
      timestamp = System.currentTimeMillis()
    )

    val out = markerFile.get().asFile
    out.parentFile.mkdirs()
    out.writeText(Mappers.json.writerWithDefaultPrettyPrinter().writeValueAsString(marker))
    logger.lifecycle("Successfully wrote Debian marker to: {}", out.absolutePath)
  }
}
