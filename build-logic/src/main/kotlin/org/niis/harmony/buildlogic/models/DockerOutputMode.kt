package org.niis.harmony.buildlogic.models

enum class DockerOutputMode {
  LOAD,
  PUSH,
  TAR,
  OCI_DIR,
  OCI_TAR;

  companion object {
    fun fromString(value: String): DockerOutputMode {
      return when (value.lowercase().trim()) {
        "load" -> LOAD
        "push" -> PUSH
        "tar"  -> TAR
        "oci-dir"  -> OCI_DIR
        "oci-tar"  -> OCI_TAR
        else -> throw IllegalArgumentException(
          "Invalid docker output mode '$value'. Valid values: load, push, tar, oci-dir, oci-tar"
        )
      }
    }
  }

  val producesFileOutput: Boolean
    get() = this == TAR || this == OCI_DIR || this == OCI_TAR
}
