package org.niis.harmony.buildlogic.internal.utils

object OciDigest {

  private val DIGEST_PATTERN = Regex("""^[a-z0-9]+([+._-][a-z0-9]+)*:[a-zA-Z0-9=_-]+$""")

  private val KNOWN_ALGORITHMS = mapOf(
    "sha256" to Regex("""^[a-f0-9]{64}$"""),
    "sha512" to Regex("""^[a-f0-9]{128}$"""),
    "blake3" to Regex("""^[a-f0-9]{64}$""")
  )

  fun isValid(digest: String): Boolean {
    return DIGEST_PATTERN.matches(digest)
  }

  fun requireValid(digest: String, context: String? = null) {
    require(isValid(digest)) {
      val prefix = context?.let { "Invalid digest for '$it': " } ?: "Invalid OCI digest format: "
      "$prefix'$digest'. Expected format: algorithm:encoded (e.g., sha256:abc123...)"
    }
  }

  fun isValidStrict(digest: String): Boolean {
    if (!DIGEST_PATTERN.matches(digest)) return false

    val colonIndex = digest.indexOf(':')
    if (colonIndex == -1) return false

    val algorithm = digest.substring(0, colonIndex)
    val encoded = digest.substring(colonIndex + 1)

    val knownPattern = KNOWN_ALGORITHMS[algorithm]
    return knownPattern?.matches(encoded) ?: true
  }
}
