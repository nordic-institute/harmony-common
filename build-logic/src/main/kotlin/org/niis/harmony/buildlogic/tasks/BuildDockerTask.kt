package org.niis.harmony.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.models.DockerBuildMarker
import org.niis.harmony.buildlogic.models.DockerOutputMode
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class BuildDockerTask @Inject constructor(
  private val execOps: ExecOperations,
  private val layout: ProjectLayout
) : DefaultTask() {

  @get:Internal
  abstract val component: Property<String>

  @get:Internal
  abstract val cacheRestoreOnly: Property<Boolean>

  @get:Internal
  lateinit var baseImageDigestsForMarker: Provider<Map<String, String?>>

  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val sourceDateEpoch: Property<Long>

  @get:Input
  abstract val buildNumber: Property<Int>

  @get:Input
  abstract val imageName: Property<String>

  @get:Input
  abstract val dockerExecutable: Property<String>

  @get:Input
  abstract val tags: ListProperty<String>

  @get:Input
  abstract val platforms: Property<String>

  @get:Input
  abstract val outputMode: Property<DockerOutputMode>

  @get:Input
  abstract val provenanceDisabled: Property<Boolean>

  @get:Input
  abstract val pullAlways: Property<Boolean>

  @get:Input
  abstract val vcsRevision: Property<String>

  @get:Input
  abstract val baseImageDigests: MapProperty<String, String>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val dockerfile: RegularFileProperty

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val dockerContext: DirectoryProperty

  @get:OutputFile
  abstract val markerFile: RegularFileProperty

  @get:OutputFile
  @get:Optional
  abstract val tarOutput: RegularFileProperty

  @get:OutputDirectory
  @get:Optional
  abstract val ociDirOutput: DirectoryProperty

  @get:OutputFile
  @get:Optional
  abstract val ociTarOutput: RegularFileProperty

  private data class DockerBuildSpec(
    val dockerExecutable: String,
    val contextDir: File,
    val dockerfile: File,
    val tags: List<String>,
    val platformsCsv: String?,
    val outputMode: DockerOutputMode,
    val outputPath: File?,
    val metadataFile: File,
    val buildArgs: Map<String, String>,
    val sourceDateEpoch: Long,
    val provenanceDisabled: Boolean,
    val pullAlways: Boolean
  )

  private data class DockerBuildResult(
    val primaryRef: String?,
    val imageDigest: String?
  )

  @TaskAction
  fun execute() {
    check(!(cacheRestoreOnly.getOrElse(false))) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${project.providers.gradleProperty("harmony.${component.get()}.build.number").orNull ?: "unknown"}
      Component:    ${component.get()}
      Version:      ${version.get()}
      Output mode:  ${outputMode.get().name.lowercase()}
      """.trimIndent()
    }

    validateDockerfileCopies()

    val buildSpec = createBuildSpec()

    logger.lifecycle(
      "Building Docker image '{}' with tags={} for platforms='{}'",
      buildSpec.tags.first(), buildSpec.tags.map { it.substringAfterLast(":") }, buildSpec.platformsCsv
    )

    val result = dockerBuild(buildSpec)

    writeDeterministicMarker(result)
    logger.lifecycle("Successfully built Docker image. Marker at: {}", markerFile.get().asFile.absolutePath)
  }

  private fun validateDockerfileCopies() {
    val dockerfileFile = dockerfile.get().asFile
    val stagingRoot = dockerContext.get().asFile

    val copyStatements = dockerfileFile.readLines()
      .filter { it.trim().startsWith("COPY") && !it.contains("--from=") }
      .mapNotNull { line ->
        val parts = line.trim().split(Regex("\\s+"))
          .filter { !it.startsWith("--") && it != "COPY" }

        if (parts.size >= 2) {
          val source = parts[0].removeSuffix("/").split("*").first()
          if (source.isNotBlank() && !source.startsWith("\$") && !source.startsWith("/")) {
            source
          } else null
        } else null
      }

    val missingPaths = copyStatements.filter { path ->
      !stagingRoot.resolve(path).exists()
    }

    check(missingPaths.isEmpty()) {
      """
      Dockerfile COPY statements reference paths that don't exist in staging:
      ${missingPaths.joinToString("\n") { "  - $it" }}
      Ensure manifest.yml generates these paths or update Dockerfile
      """.trimIndent()
    }

    val stagingDirectories = stagingRoot.listFiles()
      ?.filter { it.isDirectory }
      ?.map { it.name }
      ?.filter { it != "Dockerfile" && !it.startsWith(".") }
      ?.toSet()
      ?: emptySet()

    val declaredPaths = copyStatements.toSet()
    val undeclaredPaths = stagingDirectories - declaredPaths

    if (undeclaredPaths.isNotEmpty()) {
      logger.warn(
        """
        Staging directories not declared in Dockerfile COPY statements:
        ${undeclaredPaths.sorted().joinToString("\n") { "  - $it" }}
        These directories will NOT be included in the Docker image.
        """.trimIndent()
      )
    }

    logger.info(
      "Validated Dockerfile COPY statements: ${copyStatements.size} paths declared, ${missingPaths.size} missing, ${undeclaredPaths.size} undeclared"
    )
  }

  private fun dockerBuild(spec: DockerBuildSpec): DockerBuildResult {
    require(spec.contextDir.isDirectory) {
      "Docker context does not exist or is not a directory: ${spec.contextDir}"
    }
    require(spec.dockerfile.isFile && spec.dockerfile.canRead()) {
      "Dockerfile not found or unreadable: ${spec.dockerfile}"
    }

    spec.metadataFile.parentFile.mkdirs()
    spec.metadataFile.delete()

    val command = buildBuildxCommand(spec)

    execOps.exec {
      commandLine(command)
      environment("SOURCE_DATE_EPOCH", spec.sourceDateEpoch.toString())
      isIgnoreExitValue = false
    }

    val imageDigest = try {
      readImageDigestFromMetadata(spec.metadataFile)
    } finally {
      spec.metadataFile.delete()
    }

    return DockerBuildResult(
      primaryRef = spec.tags.firstOrNull(),
      imageDigest = imageDigest
    )
  }

  private fun buildBuildxCommand(spec: DockerBuildSpec): List<String> {
    return buildList {
      add(spec.dockerExecutable)
      addAll(listOf("buildx", "build"))
      spec.platformsCsv?.trim()?.takeIf { it.isNotEmpty() }?.let {
        addAll(listOf("--platform", it))
      }
      if (spec.provenanceDisabled) add("--provenance=false")
      if (spec.pullAlways) add("--pull")

      when (spec.outputMode) {
        DockerOutputMode.LOAD -> add("--load")
        DockerOutputMode.PUSH -> add("--push")
        DockerOutputMode.TAR -> {
          require(spec.outputPath != null) { "TAR output mode requires output path" }
          addAll(listOf("--output", "type=tar,dest=${spec.outputPath.absolutePath}"))
        }
        DockerOutputMode.OCI_DIR -> {
          require(spec.outputPath != null) { "OCI_DIR output mode requires output path" }
          addAll(listOf("--output", "type=oci,tar=false,dest=${spec.outputPath.absolutePath}"))
        }
        DockerOutputMode.OCI_TAR -> {
          require(spec.outputPath != null) { "OCI_TAR output mode requires output path" }
          addAll(listOf("--output", "type=oci,dest=${spec.outputPath.absolutePath}"))
        }
      }

      addAll(listOf("--metadata-file", spec.metadataFile.absolutePath))

      spec.tags.forEach { tag -> addAll(listOf("-t", tag)) }
      spec.buildArgs.forEach { (key, value) -> addAll(listOf("--build-arg", "$key=$value")) }
      addAll(listOf("-f", spec.dockerfile.absolutePath))
      add(spec.contextDir.absolutePath)
    }
  }

  private fun createBuildSpec(): DockerBuildSpec {
    val contextDir = dockerContext.get().asFile.also {
      require(it.isDirectory) { "Docker context directory must exist: ${it.absolutePath}" }
    }
    val dockerfileFile = dockerfile.get().asFile.also {
      require(it.isFile) { "Dockerfile not found or not readable: ${it.absolutePath}" }
    }
    val imageTags = tags.get().map { it.trim() }.filter { it.isNotEmpty() }.also {
      require(it.isNotEmpty()) { "At least one image tag must be provided." }
    }

    val baseName = imageName.get()
    val fullTags = imageTags.map { t -> if (':' in t) t else "$baseName:$t" }

    val metadataFile = File(temporaryDir, "docker-metadata.json")

    val mode = outputMode.get()
    val outputPath = when (mode) {
      DockerOutputMode.TAR -> tarOutput.get().asFile.also { it.parentFile.mkdirs() }
      DockerOutputMode.OCI_DIR -> ociDirOutput.get().asFile.also { it.mkdirs() }
      DockerOutputMode.OCI_TAR -> ociTarOutput.get().asFile.also { it.parentFile.mkdirs() }
      else -> null
    }

    return DockerBuildSpec(
      dockerExecutable = dockerExecutable.get(),
      contextDir = contextDir,
      dockerfile = dockerfileFile,
      tags = fullTags,
      platformsCsv = platforms.get(),
      outputMode = mode,
      outputPath = outputPath,
      metadataFile = metadataFile,
      buildArgs = mapOf(
        "VERSION" to version.get(),
        "VCS_REVISION" to vcsRevision.get(),
        "BUILD_NUMBER" to buildNumber.get().toString()
      ),
      sourceDateEpoch = sourceDateEpoch.get(),
      provenanceDisabled = provenanceDisabled.getOrElse(true),
      pullAlways = pullAlways.getOrElse(true)
    )
  }

  private fun readImageDigestFromMetadata(file: File): String? {
    if (!file.isFile) {
      logger.warn("Buildx metadata file not found at {}", file.absolutePath)
      return null
    }

    return runCatching {
      val root = Mappers.json.readTree(file)
      val digest = root.path("containerimage.digest").asString()
      digest.takeIf { it.isNotBlank() }
    }.onFailure { ex ->
      logger.warn("Failed to parse buildx metadata file at ${file.absolutePath}", ex)
    }.getOrNull()
  }

  private fun writeDeterministicMarker(result: DockerBuildResult) {
    val projectRoot = layout.projectDirectory.asFile.toPath()

    val dockerfileRel = runCatching {
      projectRoot.relativize(dockerfile.get().asFile.toPath()).toString()
    }.getOrElse {
      dockerfile.get().asFile.absolutePath
    }

    val contextRel = runCatching {
      projectRoot.relativize(dockerContext.get().asFile.toPath()).toString()
    }.getOrElse {
      dockerContext.get().asFile.absolutePath
    }

    val marker = DockerBuildMarker(
      component = component.get(),
      version = version.get(),
      imageName = imageName.get(),
      tags = tags.get(),
      platforms = platforms.get(),
      vcsRevision = vcsRevision.get(),
      buildNumber = buildNumber.get(),
      baseImageDigests = baseImageDigestsForMarker.get(),
      dockerfile = dockerfileRel,
      contextRel = contextRel,
      imageDigest = result.imageDigest ?: "",
      primaryTag = result.primaryRef,
      provenanceDisabled = provenanceDisabled.getOrElse(true),
      pullAlways = pullAlways.getOrElse(true),
      outputMode = outputMode.get().name.lowercase(),
      sourceDateEpoch = sourceDateEpoch.get(),
      archiveDigest = archiveDigest(outputMode.get()),
      timestamp = System.currentTimeMillis()
    )

    val out = markerFile.get().asFile
    out.parentFile.mkdirs()
    out.writeText(Mappers.json.writerWithDefaultPrettyPrinter().writeValueAsString(marker))
    logger.lifecycle("Successfully wrote Docker marker to: {}", out.absolutePath)
  }

  private fun archiveDigest(mode: DockerOutputMode): String? {
    val file = when (mode) {
      DockerOutputMode.TAR -> tarOutput.orNull?.asFile
      DockerOutputMode.OCI_TAR -> ociTarOutput.orNull?.asFile
      else -> null
    } ?: return null

    if (!file.isFile) {
      logger.warn("Expected Docker archive not found for checksum: {}", file.absolutePath)
      return null
    }

    val sha = file.inputStream().use { DigestUtils.sha256Hex(it) }
    return "sha256:$sha"
  }
}
