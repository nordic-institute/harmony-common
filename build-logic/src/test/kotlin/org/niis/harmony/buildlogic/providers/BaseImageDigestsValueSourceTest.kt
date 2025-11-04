package org.niis.harmony.buildlogic.providers

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseImageDigestsValueSourceTest {

  @Test
  fun `returns disabled when flag is false`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply { writeText("FROM scratch") }

    val result = obtain(project) {
      enabled.set(false)
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
    }

    assertEquals("DISABLED", result)
  }

  @Test
  fun `returns no dockerfile when file is missing`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile")

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals("NO_DOCKERFILE", result)
  }

  @Test
  fun `resolves digests for base images`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText(
        """
        # syntax=docker/dockerfile:1
        ARG BASE_VERSION=24.04
        FROM ubuntu:${'$'}{BASE_VERSION} AS builder
        FROM scratch
        """.trimIndent()
      )
    }

    val digest = "sha256:" + "a".repeat(64)
    val otherDigest = "sha256:" + "b".repeat(64)
    val script = File(project.projectDir, "fake-docker.sh").apply {
      writeText(
        """
        #!/bin/sh
        digest="$digest"
        otherDigest="$otherDigest"
        if [ "${'$'}4" = "ubuntu:24.04" ]; then
          printf 'Digest: %s\n' "${'$'}digest"
          exit 0
        fi
        printf 'Digest: %s\n' "${'$'}otherDigest"
        """.trimIndent()
      )
      setExecutable(true)
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set(script.absolutePath)
    }

    assertEquals("ubuntu@$digest|scratch", result)
  }

  @Test
  fun `fails when digest cannot be resolved`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText("FROM ubuntu:24.04")
    }

    val script = File(project.projectDir, "failing-docker.sh").apply {
      writeText(
        """
        #!/bin/sh
        echo "Failed to inspect ${'$'}4" >&2
        exit 1
        """.trimIndent()
      )
      setExecutable(true)
    }

    val exception = kotlin.test.assertFails {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
        dockerExecutable.set(script.absolutePath)
      }
    }

    kotlin.test.assertTrue(
      exception.message?.contains("Failed to resolve digest for base image") ?: false,
      "Expected error message about failing to resolve digest, got: ${exception.message}"
    )
  }

  private fun newProject(): Project {
    val projectDir = Files.createTempDirectory("base-image-digests").toFile()
    return ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  private fun obtain(
    project: Project,
    configure: BaseImageDigestsValueSource.Params.() -> Unit
  ): String {
    return project.providers.of(BaseImageDigestsValueSource::class.java) {
      parameters.execTimeoutSeconds.set(5)
      parameters.enabled.convention(true)
      configure(parameters)
    }.get()
  }
}
