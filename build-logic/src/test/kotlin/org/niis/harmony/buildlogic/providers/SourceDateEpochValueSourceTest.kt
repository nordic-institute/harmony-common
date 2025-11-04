package org.niis.harmony.buildlogic.providers

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.niis.harmony.buildlogic.internal.utils.ProcessRunner
import java.nio.file.Files
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceDateEpochValueSourceTest {

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `returns current time when git executable is not set`() {
    val project = newProject()
    val before = Instant.now().epochSecond

    val result = obtain(project) {
      gitExecutable.set(null as String?)
    }

    val after = Instant.now().epochSecond

    assertTrue(result >= before && result <= after + 1, "Expected $result to be between $before and $after")
  }

  @Test
  fun `returns timestamp from git log when successful`() {
    val project = newProject()
    val expectedTimestamp = 1234567890L

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(
        command = listOf("/usr/bin/git", "log", "-1", "--format=%ct"),
        workingDir = any(),
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "$expectedTimestamp\n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals(expectedTimestamp, result)

    verify {
      ProcessRunner.execute(
        command = listOf("/usr/bin/git", "log", "-1", "--format=%ct"),
        workingDir = project.projectDir,
        timeoutSeconds = 5,
        environment = any()
      )
    }
  }

  @Test
  fun `returns current time when git command fails`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 128,
      stdout = "",
      stderr = "fatal: not a git repository"
    )

    val before = Instant.now().epochSecond
    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }
    val after = Instant.now().epochSecond

    assertTrue(result >= before && result <= after + 1, "Expected $result to be between $before and $after")
  }

  @Test
  fun `returns current time when git output is not a valid timestamp`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "not-a-number\n",
      stderr = ""
    )

    val before = Instant.now().epochSecond
    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }
    val after = Instant.now().epochSecond

    assertTrue(result >= before && result <= after + 1, "Expected $result to be between $before and $after")
  }

  @Test
  fun `handles timestamp with whitespace`() {
    val project = newProject()
    val expectedTimestamp = 1234567890L

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "  $expectedTimestamp  \n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }

    assertEquals(expectedTimestamp, result)
  }

  @Test
  fun `returns current time when timestamp is invalid after parsing`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "123abc456\n",
      stderr = ""
    )

    val before = Instant.now().epochSecond
    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(project.projectDir)
    }
    val after = Instant.now().epochSecond

    assertTrue(result >= before && result <= after + 1, "Expected $result to be between $before and $after")
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
      stdout = "1234567890\n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDir.set(customRepoDir)
    }

    assertEquals(1234567890L, result)

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
    val projectDir = Files.createTempDirectory("source-date-epoch").toFile()
    return ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  private fun obtain(
    project: Project,
    configure: SourceDateEpochValueSource.Params.() -> Unit
  ): Long {
    return project.providers.of(SourceDateEpochValueSource::class.java) {
      parameters.execTimeoutSeconds.set(5)
      configure(parameters)
    }.get()
  }
}
