package org.niis.harmony.buildlogic.tasks

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
import org.niis.harmony.buildlogic.models.PullPolicy
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

  @get:Internal
  abstract val buildNumber: Property<Int>

  @get:Internal
  abstract val dockerExecutable: Property<String>

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
  abstract val gpgHome: Property<String>

  @get:Input
  abstract val builderImage: Property<String>

  @get:Input
  abstract val builderImageTag: Property<String>

  @get:Input
  abstract val builderPullPolicy: Property<PullPolicy>

  @get:OutputDirectory
  abstract val debOutDir: DirectoryProperty

  @get:OutputFile
  abstract val markerFile: RegularFileProperty

  init {
    outputs.cacheIf("Signing makes artifacts host-dependent") {
      !debSign.get()
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
    check(!(cacheRestoreOnly.get())) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${buildNumber.orNull ?: "unknown"}
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
      debSign.get(),
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
      "Debian control directory is missing at '${debSource.absolutePath}' for component='${component.get()}', distro='${distro.get()}'. " +
      "Ensure the manifest copies control files into '/deb' for scope=deb and the current distro."
    }

    fileSystemOperations.copy {
      from(debSource)
      into(workspace.debianDir)
    }

    val serviceNamePlaceholder = workspace.debianDir.resolve("service")
    if (serviceNamePlaceholder.isFile) {
      val finalServiceFile = workspace.debianDir.resolve("${packageName.get()}.service")
      check(serviceNamePlaceholder.renameTo(finalServiceFile)) {
        "Failed to rename service placeholder '${serviceNamePlaceholder.name}' to '${finalServiceFile.name}'."
      }
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
    if (debSign.get()) {
      val key = requireSigningKeyId()
      command.add("${Constants.DebianPackaging.FLAG_SIGN_KEY}$key")
    } else {
      command.addAll(Constants.DebianPackaging.UNSIGNED_FLAGS)
    }

    return command
  }

  private fun prepareSigningSetup(workspace: Workspace): SigningSetup? {
    if (!debSign.get()) {
      return null
    }

    val keyId = requireSigningKeyId()
    val configuredHome = gpgHome.orNull?.takeIf { it.isNotBlank() }
      ?: throw GradleException(
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
      ?: throw GradleException("Debian builder image repository is required (set harmony.deb.builder.image).")
    val tag = builderImageTag.get().takeIf { it.isNotBlank() }
      ?: throw GradleException("Debian builder image tag is required (set harmony.deb.builder.tag).")
    val builderImageRef = "$image:$tag"

    ensureBuilderImage(builderImageRef)

    val command = buildList {
      add(dockerExecutable.get())
      addAll(listOf("run", "--rm"))
      add("--network"); add("none")
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

  private fun ensureBuilderImage(builderImageRef: String) {
    val policy = builderPullPolicy.get()

    when (policy) {
      PullPolicy.ALWAYS -> {
        val result = ProcessRunner.execute(listOf(dockerExecutable.get(), "pull", builderImageRef))
        if (!result.isSuccess) {
          throw GradleException(
            "Failed to pull builder image '$builderImageRef' (pullPolicy=always). " +
            "Exit code=${result.exitCode}, stderr=${result.stderr}"
          )
        }
        logger.lifecycle(
          "Pulled builder image '{}'. Digest: {}",
          builderImageRef,
          result.stdout.lineSequence().lastOrNull()
        )
      }

      PullPolicy.IF_NOT_PRESENT -> {
        val inspectResult = ProcessRunner.execute(
          listOf(dockerExecutable.get(), "image", "inspect", builderImageRef)
        )

        if (inspectResult.isSuccess) {
          logger.lifecycle("Using existing local builder image '{}'", builderImageRef)
          return
        }

        val pullResult = ProcessRunner.execute(listOf(dockerExecutable.get(), "pull", builderImageRef))
        if (!pullResult.isSuccess) {
          throw GradleException(
            "Builder image '$builderImageRef' not found locally and pull failed (pullPolicy=${PullPolicy.IF_NOT_PRESENT.rawValue}). " +
            "Exit code=${pullResult.exitCode}, stderr=${pullResult.stderr}"
          )
        }
        logger.lifecycle(
          "Pulled builder image '{}'. Digest: {}",
          builderImageRef,
          pullResult.stdout.lineSequence().lastOrNull()
        )
      }

      PullPolicy.NEVER -> {
        val inspectResult = ProcessRunner.execute(
          listOf(dockerExecutable.get(), "image", "inspect", builderImageRef)
        )

        if (!inspectResult.isSuccess) {
          throw GradleException(
            "Builder image '$builderImageRef' not found locally (pullPolicy=never). " +
            "The image must be available locally when using pullPolicy=never."
          )
        }
        logger.lifecycle("Using local builder image '{}' (pullPolicy=never)", builderImageRef)
      }
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
      ?: throw GradleException(
        "Signing is enabled for component='${component.get()}' but 'harmony.${component.get()}.deb.keyId' was not provided."
      )
  }

  private fun resolveUidGid(): String {
    return try {
      val uidResult = ProcessRunner.execute(listOf("id", "-u"))
      val gidResult = ProcessRunner.execute(listOf("id", "-g"))

      val uid = uidResult.stdout.trim()
      val gid = gidResult.stdout.trim()

      require(uid.isNotBlank() && gid.isNotBlank()) {
        "Could not determine local UID/GID: 'id' returned blank values (uid='$uid', gid='$gid'). " +
        "Make sure the 'id' command is available and working."
      }

      require(uid.all { it.isDigit() } && gid.all { it.isDigit() }) {
        "Could not determine local UID/GID: 'id' returned invalid values (uid='$uid', gid='$gid'). " +
        "Make sure the 'id' command is available and working."
      }

      "$uid:$gid"
    } catch (e: Exception) {
      throw GradleException(
        "Failed to resolve local UID/GID to run the Debian builder container. " +
        "Ensure the 'id' command is available in PATH and functional.",
        e
      )
    }
  }

  private fun writeDeterministicMarker(publishedArtifacts: List<File>) {
    val projectRoot = layout.projectDirectory.asFile.toPath()

    val artifacts = publishedArtifacts
      .sortedBy { it.absolutePath }
      .associate { file ->
        val relPath = runCatching { projectRoot.relativize(file.toPath()).toString() }
          .getOrElse { file.absolutePath }
        val sha = file.inputStream().use { DigestUtils.sha256Hex(it) }
        relPath to "sha256:${sha}"
      }

    val marker = DebBuildMarker(
      component = component.get(),
      version = version.get(),
      distro = distro.get(),
      packageName = packageName.get(),
      signed = debSign.get(),
      keyId = if (debSign.get()) debKeyId.orNull else null,
      builderImage = builderImage.get(),
      builderImageTag = builderImageTag.get(),
      builderPullPolicy = builderPullPolicy.get().rawValue,
      sourceDateEpoch = sourceDateEpoch.get(),
      artifacts = artifacts,
      timestamp = System.currentTimeMillis()
    )

    val out = markerFile.get().asFile
    out.parentFile.mkdirs()
    out.writeText(Mappers.json.writerWithDefaultPrettyPrinter().writeValueAsString(marker))
    logger.lifecycle("Successfully wrote Debian marker to: {}", out.absolutePath)
  }
}
