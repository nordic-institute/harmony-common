package org.niis.harmony.buildlogic.models

enum class DockerOutputMode {
  LOAD,
  PUSH,
  TAR,
  OCI;

  companion object {
    fun fromString(value: String): DockerOutputMode {
      return when (value.lowercase().trim()) {
        "load" -> LOAD
        "push" -> PUSH
        "tar"  -> TAR
        "oci"  -> OCI
        else -> throw IllegalArgumentException(
          "Invalid docker output mode '$value'. Valid values: load, push, tar, oci"
        )
      }
    }
  }

  val producesFileOutput: Boolean
    get() = this == TAR || this == OCI
}
