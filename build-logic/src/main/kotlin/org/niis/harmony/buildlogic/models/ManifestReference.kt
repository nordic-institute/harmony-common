package org.niis.harmony.buildlogic.models

sealed interface ManifestReference {
  val originalString: String

  companion object {
    fun fromString(manifestString: String): ManifestReference {
      val artifactPrefix = "artifact:"
      val vendorPrefix = "vendor:"
      val projectPrefix = "project:"

      return when {
        manifestString.startsWith(artifactPrefix) -> ArtifactReference(
          originalString = manifestString,
          alias = manifestString.removePrefix(artifactPrefix)
        )
        manifestString.startsWith(projectPrefix) -> ProjectReference(
          originalString = manifestString,
          path = manifestString.removePrefix(projectPrefix).trim()
        )
        manifestString.startsWith(vendorPrefix) -> {
          val payload = manifestString.removePrefix(vendorPrefix)
          val dep = payload.substringBefore(':')
          val sub = payload.substringAfter(':', "").ifBlank { null }

          VendorReference(
            originalString = manifestString,
            dependency = dep,
            classifier = sub,
            alias = if (sub == null) dep else "${dep}_${sub}"
          )
        }
        else -> throw IllegalArgumentException("Unsupported 'from' protocol: '$manifestString'")
      }
    }
  }
}

@ConsistentCopyVisibility
data class ArtifactReference internal constructor(
  override val originalString: String,
  val alias: String
) : ManifestReference

@ConsistentCopyVisibility
data class ProjectReference internal constructor(
  override val originalString: String,
  val path: String
) : ManifestReference

@ConsistentCopyVisibility
data class VendorReference internal constructor(
  override val originalString: String,
  val dependency: String,
  val classifier: String?,
  val alias: String
) : ManifestReference
