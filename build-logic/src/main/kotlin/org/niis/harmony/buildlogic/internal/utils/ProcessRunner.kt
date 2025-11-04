package org.niis.harmony.buildlogic.internal.utils

import org.niis.harmony.buildlogic.internal.Constants
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object ProcessRunner {

  data class Result(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false
  ) {
    val isSuccess: Boolean get() = exitCode == 0 && !timedOut
  }

  fun execute(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    timeoutSeconds: Long = Constants.ProcessExecution.DEFAULT_TIMEOUT_SECONDS
  ): Result {
    val processBuilder = ProcessBuilder(command).apply {
      workingDir?.let { directory(it) }
      if (environment.isNotEmpty()) environment().putAll(environment)
    }

    val process = processBuilder.start().also { it.outputStream.close() }
    val stdoutBuf = ByteArrayOutputStream()
    val stderrBuf = ByteArrayOutputStream()

    val threadOut = thread(start = true, isDaemon = true) {
      process.inputStream.use { it.copyTo(stdoutBuf) }
    }
    val threadError = thread(start = true, isDaemon = true) {
      process.errorStream.use { it.copyTo(stderrBuf) }
    }

    val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

    if (!finished) {
      process.destroyForcibly()
      process.waitFor(5, TimeUnit.SECONDS)
    }

    threadOut.join()
    threadError.join()

    val stdout = truncate(stdoutBuf)
    val stderr = truncate(stderrBuf)

    return if (!finished) {
      Result(exitCode = 124, stdout = stdout, stderr = stderr.ifBlank { "Process timeout" }, timedOut = true)
    } else {
      Result(exitCode = process.exitValue(), stdout = stdout, stderr = stderr, timedOut = false)
    }
  }

  private fun truncate(buffer: ByteArrayOutputStream): String {
    val content = buffer.toString(Charsets.UTF_8.name())
    val maxChars = Constants.ProcessExecution.MAX_CAPTURED_OUTPUT_CHARS
    if (content.length <= maxChars) return content
    val omitted = content.length - maxChars
    return buildString(maxChars + 64) {
      append(content, 0, maxChars)
      append("\n...output truncated (")
      append(omitted)
      append(" chars omitted)")
    }
  }
}
