package org.niis.harmony.buildlogic.providers

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import org.slf4j.LoggerFactory
import java.io.File
import java.util.regex.Pattern

abstract class BaseImageDigestsValueSource : ValueSource<String, BaseImageDigestsValueSource.Params> {

  interface Params : ValueSourceParameters {
    val dockerExecutable: Property<String>
    val dockerfile: RegularFileProperty
    val enabled: Property<Boolean>
    val execTimeoutSeconds: Property<Long>
  }

  override fun obtain(): String {
    if (parameters.enabled.orNull == false) {
      return "DISABLED"
    }

    val dockerfile = parameters.dockerfile.asFile.orNull
    require(dockerfile != null && dockerfile.isFile) {
      "Dockerfile not found at: ${parameters.dockerfile.orNull}. " +
      "Base image digest tracking requires a valid Dockerfile."
    }

    val baseImageRefs = parseBaseImageRefs(dockerfile)
    require(baseImageRefs.isNotEmpty()) {
      "Dockerfile at ${dockerfile.absolutePath} contains no FROM instructions. " +
      "Cannot track base image digests without base images."
    }

    val dockerExecutable = parameters.dockerExecutable.orNull
      ?: throw GradleException("Docker executable path is required but was not set.")

    val resolvedDigests = baseImageRefs.map { ref ->
      resolveDigest(dockerExecutable, ref) ?: error(
        "Failed to resolve digest for base image '$ref'. " +
        "Cannot proceed without immutable reference. " +
        "Ensure image exists and is accessible from the build environment."
      )
    }

    return resolvedDigests.joinToString("|")
  }

  private fun parseBaseImageRefs(file: File): List<String> {
    val contentWithMergedLines = mergeContinuations(file.readText())
    val contentWithoutComments = stripComments(contentWithMergedLines)
    val lines = contentWithoutComments.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }

    val args = mutableMapOf<String, String>()
    val fromRefs = mutableListOf<String>()

    lines.forEach { line ->
      when {
        line.startsWith("ARG ", ignoreCase = true) -> {
          val payload = line.substring(4).trim()
          val name = payload.substringBefore('=', "").trim()
          val default = payload.substringAfter('=', "").trim()
          if (name.isNotEmpty()) {
            args.putIfAbsent(name, default)
          }
        }

        line.startsWith("FROM ", ignoreCase = true) -> {
          var body = line.substring(5).trim()
          while (body.startsWith("--")) {
            body = body.substringAfter(' ', "").trim()
          }
          val imageRefRaw = FROM_AS_REGEX.split(body, limit = 2).first().trim()

          if (imageRefRaw.isNotEmpty()) {
            val expandedRef = expandArgs(imageRefRaw, args)
            fromRefs.add(expandedRef)
          }
        }
      }
    }
    return fromRefs.distinct()
  }

  private fun mergeContinuations(text: String): String {
    return text.replace(Regex("""\\\s*\n\s*"""), " ")
  }

  private fun stripComments(text: String): String {
    return text.lines().joinToString("\n") { line ->
      if (line.trimStart().startsWith("#")) "" else line
    }
  }

  private fun expandArgs(imageRef: String, args: Map<String, String>): String {
    var result = imageRef

    result = Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)}""").replace(result) { matchResult ->
      args[matchResult.groupValues[1]] ?: matchResult.value
    }
    result = Regex("""\$([A-Za-z_][A-Za-z0-9_]*)""").replace(result) { matchResult ->
      args[matchResult.groupValues[1]] ?: matchResult.value
    }

    return result
  }

  private fun resolveDigest(dockerExecutable: String, imageRef: String): String? {
    if (imageRef.equals("scratch", ignoreCase = true) || imageRef.contains("@sha256:")) {
      return imageRef
    }

    val command = listOf(dockerExecutable, "buildx", "imagetools", "inspect", imageRef)

    val timeout = parameters.execTimeoutSeconds.get()
    val processResult = runCatching { ProcessRunner.execute(command, timeoutSeconds = timeout) }
      .getOrElse {
        logger.debug("First attempt to inspect image '{}' failed, retrying...", imageRef)
        ProcessRunner.execute(command, timeoutSeconds = timeout)
      }

    if (!processResult.isSuccess) {
      throw GradleException(
        "Failed to resolve digest for base image '$imageRef'. " +
        "Stderr: ${processResult.stderr.trim()}. " +
        "Ensure image exists and is accessible from the build environment."
      )
    }

    val matcher = DIGEST_LINE_PATTERN.matcher(processResult.stdout)
    return if (matcher.find()) {
      val digest = matcher.group(1)
      val baseName = imageRef.substringBeforeLast(':')
      "$baseName@$digest"
    } else {
      logger.warn("Could not find digest in output for image '{}'", imageRef)
      null
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(BaseImageDigestsValueSource::class.java)
    private val FROM_AS_REGEX = Regex("""\s+AS\s+""", RegexOption.IGNORE_CASE)
    private val DIGEST_LINE_PATTERN: Pattern = Pattern.compile("(?m)^\\s*Digest:\\s*(sha256:[a-f0-9]{64})\\s*$")

    @JvmStatic
    fun toStructuredMap(digestString: String): Map<String, String> {
      if (digestString.isBlank() || digestString in setOf("DISABLED", "NO_DOCKERFILE", "NO_FROM")) {
        return emptyMap()
      }

      return digestString.split('|')
        .mapNotNull { imageRef ->
          val trimmed = imageRef.trim()
          if (trimmed.isBlank()) return@mapNotNull null

          val atIndex = trimmed.indexOf('@')
          if (atIndex == -1) {
            logger.warn("Image reference without digest: {}", trimmed)
            return@mapNotNull null
          }

          val imageTag = trimmed.take(atIndex)
          val digest = trimmed.drop(atIndex + 1)
          imageTag to digest
        }
        .toMap()
    }
  }
}
