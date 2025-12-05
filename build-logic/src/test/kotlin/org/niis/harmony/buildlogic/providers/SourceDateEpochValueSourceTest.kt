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

class SourceDateEpochValueSourceTest {

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `throws exception when git executable is not set`() {
    val project = newProject()

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        repoDirs.from(project.projectDir)
      }
    }

    assertTrue(exception.message!!.contains("git executable not found"))
    assertTrue(exception.message!!.contains("harmony.build.epoch"))
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
      repoDirs.from(project.projectDir)
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
  fun `throws exception when git command fails for all repos`() {
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
        repoDirs.from(project.projectDir)
      }
    }

    assertTrue(exception.message!!.contains("failed to get git timestamp"))
    assertTrue(exception.message!!.contains("harmony.build.epoch"))
  }

  @Test
  fun `throws exception when git output is not a valid timestamp`() {
    val project = newProject()

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(any(), any(), any(), any())
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "not-a-number\n",
      stderr = ""
    )

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
        repoDirs.from(project.projectDir)
      }
    }

    assertTrue(exception.message!!.contains("failed to get git timestamp"))
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
      repoDirs.from(project.projectDir)
    }

    assertEquals(expectedTimestamp, result)
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
      repoDirs.from(customRepoDir)
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

  @Test
  fun `returns maximum timestamp from multiple repos`() {
    val project = newProject()
    val repoDir1 = Files.createTempDirectory("repo1").toFile()
    val repoDir2 = Files.createTempDirectory("repo2").toFile()

    val olderTimestamp = 1000000000L
    val newerTimestamp = 2000000000L

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(
        command = any(),
        workingDir = repoDir1,
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "$olderTimestamp\n",
      stderr = ""
    )
    every {
      ProcessRunner.execute(
        command = any(),
        workingDir = repoDir2,
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "$newerTimestamp\n",
      stderr = ""
    )

    val result = obtain(project) {
      gitExecutable.set("/usr/bin/git")
      repoDirs.from(repoDir1, repoDir2)
    }

    assertEquals(newerTimestamp, result)
  }

  @Test
  fun `throws exception when one repo fails`() {
    val project = newProject()
    val validRepoDir = Files.createTempDirectory("valid-repo").toFile()
    val invalidRepoDir = Files.createTempDirectory("invalid-repo").toFile()

    val validTimestamp = 1234567890L

    mockkObject(ProcessRunner)
    every {
      ProcessRunner.execute(
        command = any(),
        workingDir = validRepoDir,
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 0,
      stdout = "$validTimestamp\n",
      stderr = ""
    )
    every {
      ProcessRunner.execute(
        command = any(),
        workingDir = invalidRepoDir,
        timeoutSeconds = any(),
        environment = any()
      )
    } returns ProcessRunner.Result(
      exitCode = 128,
      stdout = "",
      stderr = "fatal: not a git repository"
    )

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
        repoDirs.from(validRepoDir, invalidRepoDir)
      }
    }

    assertTrue(exception.message!!.contains("some repositories failed"))
    assertTrue(exception.message!!.contains(validRepoDir.absolutePath))
    assertTrue(exception.message!!.contains(invalidRepoDir.absolutePath))
  }

  @Test
  fun `throws exception when no repoDirs specified`() {
    val project = newProject()

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        gitExecutable.set("/usr/bin/git")
      }
    }

    assertTrue(exception.message!!.contains("no repositories were provided"))
    assertTrue(exception.message!!.contains("harmony.build.epoch"))
  }

  @Test
  fun `error message includes checked directories`() {
    val project = newProject()
    val repoDir = Files.createTempDirectory("test-repo").toFile()

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
        repoDirs.from(repoDir)
      }
    }

    assertTrue(exception.message!!.contains(repoDir.absolutePath))
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
