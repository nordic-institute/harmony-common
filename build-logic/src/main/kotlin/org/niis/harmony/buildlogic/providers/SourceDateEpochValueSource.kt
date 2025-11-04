package org.niis.harmony.buildlogic.providers

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import java.io.File
import java.time.Instant

abstract class SourceDateEpochValueSource : ValueSource<Long, SourceDateEpochValueSource.Params> {

  interface Params : ValueSourceParameters {
    val gitExecutable: Property<String>
    val repoDir: DirectoryProperty
    val execTimeoutSeconds: Property<Long>
  }

  override fun obtain(): Long {
    val gitExecutable = parameters.gitExecutable.orNull
    if (gitExecutable != null) {
      val repoDir = parameters.repoDir.orNull?.asFile ?: File(System.getProperty("user.dir"))
      val command = listOf(gitExecutable, "log", "-1", "--format=%ct")
      val result = ProcessRunner.execute(command, workingDir = repoDir, timeoutSeconds = parameters.execTimeoutSeconds.get())

      val timestampStr = result.stdout.trim()
      if (result.isSuccess && timestampStr.matches(EPOCH_REGEX)) {
        timestampStr.toLongOrNull()?.let { return it }
      }
    }

    return Instant.now().epochSecond
  }

  private companion object {
    private val EPOCH_REGEX = Regex("\\d+")
  }
}
