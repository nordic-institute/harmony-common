package org.niis.harmony.buildlogic.providers

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VcsRevisionValueSourceTest {

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `returns UNKNOWN when git executable is not set`() {
    val project = newProject()

    val result = obtain(project) {
      gitExecutable.set(null as String?)
    }

    assertEquals("UNKNOWN", result)
  }

  @Test
  fun `returns short revision hash when git succeeds`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(
        command = listOf("/usr/bin/git", "rev-parse", "--short", "HEAD"),
        workingDir = any(),
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "abc1234\n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals("abc1234", result)

    verify {
      ProcessRunner.execute(
        command = listOf("/usr/bin/git", "rev-parse", "--short", "HEAD"),
        workingDir = project.projectDir,
        timeoutSeconds = 5,
        environment = any()
      )
    }
  }

  @Test
  fun `returns UNKNOWN when git command fails`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 128,
      stdout = "",
      stderr = "fatal: not a git repository"
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals("UNKNOWN", result)
  }

  @Test
  fun `returns UNKNOWN when git output is blank`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "   \n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals("UNKNOWN", result)
  }

  @Test
  fun `trims whitespace from git output`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "  def5678  \n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals("def5678", result)
  }

  @Test
  fun `uses provided repoDir for git command`() {
    val project = newProject()
    val customRepoDir = Files.createTempDirectory("custom-repo").toFile()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(
        command = any(),
        workingDir = customRepoDir,
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "correct-dir\n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(customRepoDir)
    }

    assertEquals("correct-dir", result)

    verify {
      ProcessRunner.execute(
        command = any(),
        workingDir = customRepoDir,
        timeoutSeconds = any(),
        environment = any()
      )
    }
  }

  private fun newProject(): Project {
    val projectDir = Files.createTempDirectory("vcs-revision").toFile()
    return ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  private fun obtain(
    project: Project,
    configure: VcsRevisionValueSource.Params.() -> Unit
  ): String {
    return project.providers.of(VcsRevisionValueSource::class.java) {
      parameters.execTimeoutSeconds.set(5)
      configure(parameters)
    }.get()
  }
}
