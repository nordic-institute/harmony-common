package org.niis.harmony.buildlogic.providers

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import java.io.File

abstract class SourceDateEpochValueSource : ValueSource<Long, SourceDateEpochValueSource.Params> {

  interface Params : ValueSourceParameters {
    val gitExecutable: Property<String>
    val repoDirs: ConfigurableFileCollection
    val execTimeoutSeconds: Property<Long>
  }

  override fun obtain(): Long {
    val gitExecutable = parameters.gitExecutable.orNull
      ?: throw GradleException(
        "Cannot determine SOURCE_DATE_EPOCH: git executable not found. " +
        "Set 'harmony.build.epoch' explicitly or ensure git is available."
      )

    if (parameters.repoDirs.isEmpty) {
      throw GradleException(
        "Cannot determine SOURCE_DATE_EPOCH: no repositories were provided. " +
        "Set 'harmony.build.epoch' explicitly or configure valid repositories."
      )
    }

    val results: Map<File, Long?> = parameters.repoDirs.associateWith { dir ->
      getGitTimestamp(gitExecutable, dir)
    }

    val successes: Map<File, Long> = results
      .filterValues { it != null }
      .mapValues { (_, value) -> value!! }

    val failures: Set<File> = results
      .filterValues { it == null }
      .keys

    if (successes.isEmpty()) {
      val failuresList = results.keys.joinToString("\n") { "  - ${it.absolutePath}" }
      throw GradleException(
        "Cannot determine SOURCE_DATE_EPOCH: failed to get git timestamp from any repository.\n" +
        "Directories checked:\n$failuresList\n" +
        "Set 'harmony.build.epoch' explicitly or ensure the directories are valid git repositories."
      )
    }

    if (failures.isNotEmpty()) {
      val successesList = successes.keys.joinToString("\n") { "  - ${it.absolutePath}" }
      val failuresList = failures.joinToString("\n") { "  - ${it.absolutePath}" }
      throw GradleException(
        "Cannot determine SOURCE_DATE_EPOCH: some repositories failed to provide a git timestamp.\n" +
        "Repositories with successful timestamps:\n$successesList\n" +
        "Repositories with errors or no timestamp:\n$failuresList\n" +
        "Set 'harmony.build.epoch' explicitly or ensure all directories are valid git repositories."
      )
    }

    return successes.values.maxOrNull()
      ?: throw GradleException(
        "Cannot determine SOURCE_DATE_EPOCH: no valid timestamps found after filtering successes. " +
        "This indicates an internal configuration error."
      )
  }

  private fun getGitTimestamp(gitExecutable: String, repoDir: File): Long? {
    val command = listOf(gitExecutable, "log", "-1", "--format=%ct")
    val result = ProcessRunner.execute(
      command,
      workingDir = repoDir,
      timeoutSeconds = parameters.execTimeoutSeconds.get()
    )

    val timestampStr = result.stdout.trim()
    return if (result.isSuccess && timestampStr.matches(EPOCH_REGEX)) {
      timestampStr.toLongOrNull()
    } else {
      null
    }
  }

  private companion object {
    private val EPOCH_REGEX = Regex("\\d+")
  }
}
