package org.niis.harmony.buildlogic.models

enum class PullPolicy(val value: String) {
  ALWAYS("always"),
  IF_NOT_PRESENT("ifNotPresent"),
  NEVER("never");

  companion object {
    fun fromString(input: String): PullPolicy {
      val normalized = input.lowercase().trim().replace("_", "").replace("-", "")
      return entries.find { it.value.lowercase() == normalized }
        ?: throw IllegalArgumentException(
          "Invalid pull policy '$input'. Valid values: ${entries.joinToString { it.value }}"
        )
    }
  }
}
