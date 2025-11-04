package org.niis.harmony.buildlogic.providers

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.niis.harmony.buildlogic.models.Scope
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestFingerprintValueSourceTest {

  @Test
  fun `missing manifest returns sentinel digest`() {
    val project = newProject()
    val missing = File(project.projectDir, "missing.yml")

    val fingerprint = fingerprint(project, missing, Scope.DOCKER, "jammy")
    val expected = sha256("MISSING_MANIFEST/DOCKER/jammy")

    assertEquals(expected, fingerprint)
  }

  @Test
  fun `equivalent manifests produce identical fingerprints`() {
    val project = newProject()
    val manifestA = File(project.projectDir, "manifest-a.yml").apply {
      writeText(
        """
        inputs:
          - do: copy
            from: artifacts/app.war
            into: /opt/app
            when:
              distros: [jammy, noble]
              scopes: [docker]
          - do: copy
            from: artifacts/ignored.zip
            into: /opt/app
            when:
              scopes: [deb]
          - do: unpack
            from: vendor:s6overlay
            include: [bin, etc]
            exclude:
              - var
              - tmp
            routes:
              - include: [a, b]
                into: /opt/app
            into: /opt/app
            when:
              scopes: [docker]
              distros: [jammy]
        """.trimIndent()
      )
    }

    val manifestB = File(project.projectDir, "manifest-b.yml").apply {
      writeText(
        """
        inputs:
          - into: /opt/app
            when:
              distros: [noble, jammy]
              scopes: [docker]
            from: artifacts/app.war
            do: copy
          - do: copy
            from: artifacts/ignored.zip
            into: /opt/app
            when:
              scopes: [deb]
          - do: unpack
            from: vendor:s6overlay
            into: /opt/app
            routes:
              - into: /opt/app
                include: [b, a]
            exclude: [tmp, var]
            include:
              - etc
              - bin
            when:
              scopes: [docker]
              distros: [jammy]
        """.trimIndent()
      )
    }

    val scope = Scope.DOCKER
    val distro = "jammy"

    val fingerprintA = fingerprint(project, manifestA, scope, distro)
    val fingerprintB = fingerprint(project, manifestB, scope, distro)

    assertEquals(fingerprintA, fingerprintB, "fingerprintA=$fingerprintA fingerprintB=$fingerprintB")
  }

  @Test
  fun `no matching steps returns sentinel digest`() {
    val project = newProject()
    val manifest = File(project.projectDir, "manifest.yml").apply {
      writeText(
        """
        inputs:
          - do: copy
            from: artifacts/app.war
            into: /opt/app
            when:
              scopes: [deb]
              distros: [jammy]
        """.trimIndent()
      )
    }

    val fingerprint = fingerprint(project, manifest, Scope.DOCKER, "jammy")
    val expected = sha256("NO_MATCHING_STEPS/DOCKER/jammy")

    assertEquals(expected, fingerprint)
  }

  private fun newProject(): Project {
    val projectDir = Files.createTempDirectory("manifest-fingerprint").toFile()
    return ProjectBuilder.builder().withProjectDir(projectDir).build()
  }

  private fun fingerprint(
    project: Project,
    manifest: File,
    scope: Scope,
    distro: String?
  ): String {
    return project.providers.of(ManifestFingerprintValueSource::class.java) {
      parameters.manifestFile.set(project.layout.file(project.provider { manifest }))
      parameters.scope.set(scope)
      distro?.let { value -> parameters.distro.set(value) }
    }.get()
  }

  private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) { digest.forEach { append("%02x".format(it)) } }
  }
}
