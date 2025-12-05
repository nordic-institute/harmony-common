package org.niis.harmony.buildlogic.providers

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaseImageDigestsValueSourceTest {

  @Test
  fun `returns empty map when tracking is disabled`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply { writeText("FROM scratch") }

    val result = obtain(project) {
      enabled.set(false)
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
    }

    assertEquals(emptyMap(), result)
  }

  @Test
  fun `fails when dockerfile is missing`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile")

    val exception = assertFailsWith<IllegalArgumentException> {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
        dockerExecutable.set("/usr/bin/docker")
      }
    }

    assertTrue(
      exception.message?.contains("Dockerfile not found") ?: false,
      "Expected error about missing Dockerfile, got: ${exception.message}"
    )
  }

  @Test
  fun `fails when dockerfile has no FROM instructions`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText(
        """
        # syntax=docker/dockerfile:1
        RUN echo "no from here"
        """.trimIndent()
      )
    }

    val exception = assertFailsWith<IllegalArgumentException> {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
        dockerExecutable.set("/usr/bin/docker")
      }
    }

    assertTrue(
      exception.message?.contains("contains no FROM instructions") ?: false,
      "Expected error about missing FROM instructions, got: ${exception.message}"
    )
  }

  @Test
  fun `fails when docker executable is missing`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText("FROM scratch")
    }

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      }
    }

    assertTrue(
      exception.message?.contains("Docker executable path is required") ?: false,
      "Expected error about missing docker executable, got: ${exception.message}"
    )
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
    val script = File(project.projectDir, "fake-docker.sh").apply {
      writeText(
        """
        #!/bin/sh
        digest="$digest"
        if [ "${'$'}4" = "ubuntu:24.04" ]; then
          printf 'Digest: %s\n' "${'$'}digest"
          exit 0
        fi
        exit 1
        """.trimIndent()
      )
      setExecutable(true)
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set(script.absolutePath)
    }

    assertEquals(2, result.size)
    assertEquals(digest, result["ubuntu:24.04"])
    assertTrue(result.containsKey("scratch"))
    assertNull(result["scratch"], "scratch should have null digest")
  }

  @Test
  fun `expands both arg syntaxes in FROM`() {
    val project = newProject()
    val pinnedDigest = "sha256:" + "c".repeat(64)
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText(
        """
        ARG IMAGE_NAME=alpine
        ARG IMAGE_TAG=3.18
        FROM ${'$'}{IMAGE_NAME}:${'$'}IMAGE_TAG@$pinnedDigest
        """.trimIndent()
      )
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals(mapOf("alpine:3.18" to pinnedDigest), result)
  }

  @Test
  fun `supports line continuations and AS in FROM`() {
    val project = newProject()
    val pinnedDigest = "sha256:" + "d".repeat(64)
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText(
        """
        ARG VERSION=3.18
        FROM alpine:${'$'}VERSION@$pinnedDigest \
          AS builder
        """.trimIndent()
      )
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals(mapOf("alpine:3.18" to pinnedDigest), result)
  }

  @Test
  fun `returns null digest for scratch-only dockerfile`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText("FROM scratch")
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals(mapOf("scratch" to null), result)
  }

  @Test
  fun `preserves digest for already pinned images`() {
    val project = newProject()
    val pinnedDigest = "sha256:" + "b".repeat(64)
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText("FROM alpine@$pinnedDigest")
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals(mapOf("alpine" to pinnedDigest), result)
  }

  @Test
  fun `deduplicates repeated FROM instructions for same base image`() {
    val project = newProject()
    val pinnedDigest = "sha256:" + "e".repeat(64)
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText(
        """
        FROM scratch
        FROM alpine@$pinnedDigest
        FROM scratch
        FROM alpine@$pinnedDigest
        """.trimIndent()
      )
    }

    val result = obtain(project) {
      dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
      dockerExecutable.set("/usr/bin/docker")
    }

    assertEquals(2, result.size)
    assertEquals(null, result["scratch"])
    assertEquals(pinnedDigest, result["alpine"])
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

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
        dockerExecutable.set(script.absolutePath)
      }
    }

    assertTrue(
      exception.message?.contains("Failed to resolve digest for base image") ?: false,
      "Expected error message about failing to resolve digest, got: ${exception.message}"
    )
  }

  @Test
  fun `fails when digest line is missing in docker output`() {
    val project = newProject()
    val dockerfilePath = File(project.projectDir, "Dockerfile").apply {
      writeText("FROM ubuntu:24.04")
    }

    val script = File(project.projectDir, "docker-no-digest.sh").apply {
      writeText(
        """
        #!/bin/sh
        if [ "${'$'}4" = "ubuntu:24.04" ]; then
          echo "Some other output"
          exit 0
        fi
        exit 1
        """.trimIndent()
      )
      setExecutable(true)
    }

    val exception = assertFailsWith<GradleException> {
      obtain(project) {
        dockerfile.set(project.layout.file(project.provider { dockerfilePath }))
        dockerExecutable.set(script.absolutePath)
      }
    }

    assertTrue(
      exception.message?.contains("Could not find digest in Docker output") ?: false,
      "Expected error about missing digest line, got: ${exception.message}"
    )
  }

  private fun newProject(): Project {
    val projectDir = Files.createTempDirectory("base-image-digests").toFile()
    return ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  private fun obtain(
    project: Project,
    configure: BaseImageDigestsValueSource.Params.() -> Unit
  ): Map<String, String?> {
    return project.providers.of(BaseImageDigestsValueSource::class.java) {
      parameters.execTimeoutSeconds.set(5)
      parameters.enabled.convention(true)
      configure(parameters)
    }.get()
  }
}