package org.niis.harmony.buildlogic.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ManifestReferenceTest {

  @Test
  fun `parses artifact references`() {
    val reference = ManifestReference.fromString("artifact:war-repacked")
    val artifact = assertIs<ArtifactReference>(reference)
    assertEquals("war-repacked", artifact.alias)
  }

  @Test
  fun `parses project references`() {
    val reference = ManifestReference.fromString("project:components/ap/overlays")
    val projectRef = assertIs<ProjectReference>(reference)
    assertEquals("components/ap/overlays", projectRef.path)
  }

  @Test
  fun `parses vendor references with classifier`() {
    val reference = ManifestReference.fromString("vendor:s6overlay:amd64")
    val vendor = assertIs<VendorReference>(reference)
    assertEquals("s6overlay", vendor.dependency)
    assertEquals("amd64", vendor.classifier)
    assertEquals("s6overlay_amd64", vendor.alias)
  }

  @Test
  fun `fails for unsupported schemes`() {
    assertFailsWith<IllegalArgumentException> {
      ManifestReference.fromString("file:/tmp/foo")
    }
  }
}
