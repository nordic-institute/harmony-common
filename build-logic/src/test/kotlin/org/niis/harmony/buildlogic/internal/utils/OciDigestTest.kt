package org.niis.harmony.buildlogic.internal.utils

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OciDigestTest {

  @Test
  fun `validates sha256 digest`() {
    val digest = "sha256:" + "a".repeat(64)
    assertTrue(OciDigest.isValid(digest))
    assertTrue(OciDigest.isValidStrict(digest))
  }

  @Test
  fun `validates sha512 digest`() {
    val digest = "sha512:" + "b".repeat(128)
    assertTrue(OciDigest.isValid(digest))
    assertTrue(OciDigest.isValidStrict(digest))
  }

  @Test
  fun `validates blake3 digest`() {
    val digest = "blake3:" + "c".repeat(64)
    assertTrue(OciDigest.isValid(digest))
    assertTrue(OciDigest.isValidStrict(digest))
  }

  @Test
  fun `validates unknown algorithm with valid format`() {
    val digest = "sha3-256:" + "d".repeat(64)
    assertTrue(OciDigest.isValid(digest))
    assertTrue(OciDigest.isValidStrict(digest))
  }

  @Test
  fun `validates algorithm with separators`() {
    val digest = "multihash+base58:QmRZxt2b1FVZPNqd8hsiykDL3TdBDeTSPX9Kv46HmX4Gx8"
    assertTrue(OciDigest.isValid(digest))
  }

  @Test
  fun `rejects digest without colon`() {
    assertFalse(OciDigest.isValid("sha256abcdef"))
  }

  @Test
  fun `rejects digest with uppercase algorithm`() {
    val digest = "SHA256:" + "a".repeat(64)
    assertFalse(OciDigest.isValid(digest))
  }

  @Test
  fun `rejects empty digest`() {
    assertFalse(OciDigest.isValid(""))
  }

  @Test
  fun `rejects digest with empty algorithm`() {
    assertFalse(OciDigest.isValid(":abcdef"))
  }

  @Test
  fun `rejects digest with empty encoded part`() {
    assertFalse(OciDigest.isValid("sha256:"))
  }

  @Test
  fun `strict validation rejects sha256 with wrong length`() {
    val shortDigest = "sha256:" + "a".repeat(32)
    assertTrue(OciDigest.isValid(shortDigest))
    assertFalse(OciDigest.isValidStrict(shortDigest))
  }

  @Test
  fun `strict validation rejects sha512 with wrong length`() {
    val shortDigest = "sha512:" + "b".repeat(64)
    assertTrue(OciDigest.isValid(shortDigest))
    assertFalse(OciDigest.isValidStrict(shortDigest))
  }

  @Test
  fun `strict validation rejects sha256 with uppercase hex`() {
    val upperDigest = "sha256:" + "A".repeat(64)
    assertTrue(OciDigest.isValid(upperDigest))
    assertFalse(OciDigest.isValidStrict(upperDigest))
  }

  @Test
  fun `requireValid throws for invalid digest`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      OciDigest.requireValid("invalid")
    }
    assertTrue(exception.message?.contains("Invalid OCI digest format") ?: false)
  }

  @Test
  fun `requireValid includes context in error message`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      OciDigest.requireValid("invalid", "ubuntu:24.04")
    }
    assertTrue(exception.message?.contains("ubuntu:24.04") ?: false)
  }

  @Test
  fun `requireValid does not throw for valid digest`() {
    val digest = "sha256:" + "a".repeat(64)
    OciDigest.requireValid(digest)
  }

  @Test
  fun `validates real-world sha256 digest`() {
    val realDigest = "sha256:2d4e5e3f3e4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c"
    assertTrue(OciDigest.isValid(realDigest))
    assertTrue(OciDigest.isValidStrict(realDigest))
  }
}
