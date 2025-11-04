package org.niis.harmony.buildlogic.providers

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import org.slf4j.LoggerFactory
import java.io.File

abstract class VcsRevisionValueSource : ValueSource<String, VcsRevisionValueSource.Params> {

  interface Params : ValueSourceParameters {
    val gitExecutable: Property<String>
    val repoDir: DirectoryProperty
    val execTimeoutSeconds: Property<Long>
  }

  override fun obtain(): String {
    val gitExecutable = parameters.gitExecutable.orNull
    if (gitExecutable != null) {
      val repoDir = parameters.repoDir.orNull?.asFile ?: File(System.getProperty("user.dir"))
      val command = listOf(gitExecutable, "rev-parse", "--short", "HEAD")
      val result = ProcessRunner.execute(command, workingDir = repoDir, timeoutSeconds = parameters.execTimeoutSeconds.get())

      if (result.isSuccess && result.stdout.isNotBlank()) {
        return result.stdout.trim()
      }
    }

    logger.warn("Could not determine VCS revision. Falling back to 'UNKNOWN'.")
    return "UNKNOWN"
  }

  private companion object {
    private val logger = LoggerFactory.getLogger(VcsRevisionValueSource::class.java)
  }
}
