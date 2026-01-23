package org.niis.harmony.buildlogic.internal.utils

import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.Scope
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ManifestParserTest {

  @Test
  fun `applies returns true when no constraints defined`() {
    assertTrue(ManifestParser.applies(null, Scope.DOCKER, null))
  }

  @Test
  fun `applies respects scope filter`() {
    val onlyDeb = Manifest.When(scopes = listOf("deb"))
    assertTrue(ManifestParser.applies(onlyDeb, Scope.DEB, null))
    assertFalse(ManifestParser.applies(onlyDeb, Scope.DOCKER, null))
  }

  @Test
  fun `applies respects distro filter with case insensitivity`() {
    val jammyOnly = Manifest.When(distros = listOf("JAMMY"))
    assertTrue(ManifestParser.applies(jammyOnly, Scope.DEB, "jammy"))
    assertFalse(ManifestParser.applies(jammyOnly, Scope.DEB, "noble"))
    assertFalse(ManifestParser.applies(jammyOnly, Scope.DOCKER, null))
  }

  @Test
  fun `applies combines scope and distro filters`() {
    val whenBlock = Manifest.When(scopes = listOf("docker"), distros = listOf("noble"))

    assertFalse(ManifestParser.applies(whenBlock, Scope.DEB, "noble"))
    assertFalse(ManifestParser.applies(whenBlock, Scope.DOCKER, "jammy"))
    assertTrue(ManifestParser.applies(whenBlock, Scope.DOCKER, "noble"))
  }

  @Test
  fun `parse reads a valid manifest with copy inputs`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: copy
          from: artifact:app.war
          into: /opt/app
      """.trimIndent()
    )

    val manifest = ManifestParser.parse(manifestFile)

    assertEquals(1, manifest.inputs.size)
    val input = assertIs<Manifest.Copy>(manifest.inputs[0])
    assertEquals("artifact:app.war", input.from)
    assertEquals("/opt/app", input.into)
  }

  @Test
  fun `parse reads a valid manifest with unpack inputs`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: unpack
          from: vendor:s6overlay
          into: /opt/overlay
          strip: 1
          include: [bin, etc]
          exclude: [tmp]
      """.trimIndent()
    )

    val manifest = ManifestParser.parse(manifestFile)

    assertEquals(1, manifest.inputs.size)
    val input = assertIs<Manifest.Unpack>(manifest.inputs[0])
    assertEquals("vendor:s6overlay", input.from)
    assertEquals("/opt/overlay", input.into)
    assertEquals(1, input.strip)
    assertEquals(listOf("bin", "etc"), input.include)
    assertEquals(listOf("tmp"), input.exclude)
  }

  @Test
  fun `parse handles manifest with when constraints`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: copy
          from: artifact:app.war
          into: /opt/app
          when:
            scopes: [docker]
            distros: [jammy, noble]
      """.trimIndent()
    )

    val manifest = ManifestParser.parse(manifestFile)

    val input = assertIs<Manifest.Copy>(manifest.inputs[0])
    assertEquals(listOf("docker"), input.`when`?.scopes)
    assertEquals(listOf("jammy", "noble"), input.`when`?.distros)
  }

  @Test
  fun `parse handles empty inputs list`() {
    val manifestFile = createTempFile(
      """
      inputs: []
      """.trimIndent()
    )

    val manifest = ManifestParser.parse(manifestFile)

    assertTrue(manifest.inputs.isEmpty())
  }

  @Test
  fun `parse handles manifest with routes`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: unpack
          from: vendor:dependency
          into: /opt/base
          routes:
            - include: [config]
              exclude: [tmp]
              into: /etc/app
      """.trimIndent()
    )

    val manifest = ManifestParser.parse(manifestFile)

    val input = assertIs<Manifest.Unpack>(manifest.inputs[0])
    assertEquals(1, input.routes?.size)
    assertEquals(listOf("config"), input.routes?.get(0)?.include)
    assertEquals(listOf("tmp"), input.routes?.get(0)?.exclude)
    assertEquals("/etc/app", input.routes?.get(0)?.into)
  }

  @Test
  fun `parse fails when file does not exist`() {
    val nonExistentFile = File("/tmp/nonexistent-manifest-${System.currentTimeMillis()}.yml")

    val exception = assertFailsWith<IllegalArgumentException> {
      ManifestParser.parse(nonExistentFile)
    }

    assertTrue(exception.message?.contains("Manifest file not found") == true)
  }

  @Test
  fun `parse fails when file is not readable`() {
    val manifestFile = createTempFile("inputs: []")
    manifestFile.setReadable(false)

    val exception = assertFailsWith<IllegalArgumentException> {
      ManifestParser.parse(manifestFile)
    }

    assertTrue(
      exception.message?.contains("not readable") == true ||
      exception.message?.contains("Failed to parse") == true
    )
  }

  @Test
  fun `parse fails with invalid YAML syntax`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: copy
          from: artifact:app
          invalid yaml: [unclosed bracket
      """.trimIndent()
    )

    val exception = assertFailsWith<IllegalArgumentException> {
      ManifestParser.parse(manifestFile)
    }

    assertTrue(exception.message?.contains("Failed to parse manifest") == true)
  }

  @Test
  fun `parse fails with unsupported input type`() {
    val manifestFile = createTempFile(
      """
      inputs:
        - do: unsupported-action
          from: artifact:app
          into: /opt/app
      """.trimIndent()
    )

    val exception = assertFailsWith<IllegalArgumentException> {
      ManifestParser.parse(manifestFile)
    }

    assertTrue(exception.message?.contains("Failed to parse manifest") == true)
  }

  private fun createTempFile(content: String): File {
    val tempFile = Files.createTempFile("manifest-", ".yml").toFile()
    tempFile.deleteOnExit()
    tempFile.writeText(content)
    return tempFile
  }
}
