package org.niis.harmony.buildlogic.models

enum class DockerOutputMode(val rawValue: String) {
  LOAD("load"),
  PUSH("push"),
  TAR("tar"),
  OCI_DIR("oci-dir"),
  OCI_TAR("oci-tar");

  companion object {
    private fun normalize(input: String) = input.lowercase()
      .trim()
      .replace("_", "")
      .replace("-", "")

    private val byNormalized = entries.associateBy { normalize(it.rawValue) }

    fun fromString(input: String): DockerOutputMode = byNormalized[normalize(input)]
      ?: throw IllegalArgumentException(
        "Invalid docker output mode '$input'. Valid values: ${entries.joinToString { it.rawValue }}"
      )
  }
}
