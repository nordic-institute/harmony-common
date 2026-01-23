package org.niis.harmony.buildlogic.providers

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner

abstract class VcsRevisionValueSource : ValueSource<String, VcsRevisionValueSource.Params> {

  interface Params : ValueSourceParameters {
    val gitExecutable: Property<String>
    val repoDir: DirectoryProperty
    val execTimeoutSeconds: Property<Long>
  }

  override fun obtain(): String {
    val gitExecutable = parameters.gitExecutable.orNull
      ?: throw GradleException(
        "Cannot determine VCS revision: git executable not found. " +
        "Set 'harmony.build.revision' explicitly or ensure git is available."
      )

    val repoDirFile = parameters.repoDir.orNull?.asFile
      ?: throw GradleException(
        "Cannot determine VCS revision: repository directory was not configured. " +
        "This is an internal configuration error. Ensure 'repoDir' is set when wiring VcsRevisionValueSource."
      )

    val command = listOf(gitExecutable, "rev-parse", "--short", "HEAD")
    val result = ProcessRunner.execute(
      command,
      workingDir = repoDirFile,
      timeoutSeconds = parameters.execTimeoutSeconds.get()
    )

    val stdout = result.stdout.trim()
    if (!result.isSuccess || stdout.isBlank()) {
      throw GradleException(
        "Cannot determine VCS revision from repository '${repoDirFile.absolutePath}'.\n" +
        "Git command: ${command.joinToString(" ")}\n" +
        "Exit code: ${result.exitCode}\n" +
        "Stderr: ${result.stderr.ifBlank { "<empty>" }}\n" +
        "Set 'harmony.build.revision' explicitly or ensure this directory is a valid git repository."
      )
    }

    return stdout
  }
}