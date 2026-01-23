package org.niis.harmony.buildlogic.providers

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VcsRevisionValueSourceTest {

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `throws exception when git executable is not set`() {
    val project = newProject()

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        repoDir.set(project.projectDir)
      }
    }

    assertTrue(exception.message!!.contains("git executable not found"))
    assertTrue(exception.message!!.contains("harmony.build.revision"))
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
  fun `throws exception when git command fails`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 128,
      stdout = "",
      stderr = "fatal: not a git repository"
    )

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
        repoDir.set(project.projectDir)
      }
    }

    val message = exception.message ?: error("Exception message should not be null")

    assertTrue(message.contains("Cannot determine VCS revision"))
    assertTrue(message.contains("harmony.build.revision"))
    assertTrue(message.contains("git rev-parse --short HEAD"), "Expected git command in error message")
    assertTrue(message.contains("Exit code: 128"), "Expected exit code in error message")
    assertTrue(message.contains("fatal: not a git repository"), "Expected stderr in error message")
  }

  @Test
  fun `throws exception when git output is blank`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "   \n",
      stderr = ""
    )

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
        repoDir.set(project.projectDir)
      }
    }

    assertTrue(exception.message!!.contains("Cannot determine VCS revision"))
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

  @Test
  fun `throws exception when repoDir is not set`() {
    val project = newProject()

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
      }
    }

    assertTrue(exception.message!!.contains("repository directory was not configured"))
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
