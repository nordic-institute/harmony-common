package org.niis.harmony.buildlogic.models

enum class PullPolicy(val rawValue: String) {
  ALWAYS("always"),
  IF_NOT_PRESENT("if-not-present"),
  NEVER("never");

  companion object {
    private fun normalize(input: String) = input.lowercase()
      .trim()
      .replace("_", "")
      .replace("-", "")

    private val byNormalized = entries.associateBy { normalize(it.rawValue) }

    fun fromString(input: String): PullPolicy = byNormalized[normalize(input)]
      ?: throw IllegalArgumentException(
        "Invalid pull policy '$input'. Valid values: ${entries.joinToString { it.rawValue }}"
      )
  }
}
