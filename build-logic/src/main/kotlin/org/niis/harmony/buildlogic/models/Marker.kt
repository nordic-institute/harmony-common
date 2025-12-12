package org.niis.harmony.buildlogic.models

data class CompileMarker(
  val component: String,
  val version: String,
  val componentRepo: String,
  val javaVersion: Int,
  val javaRuntimeId: String,
  val mavenGoals: List<String>,
  val mavenProfiles: List<String>,
  val skipTests: Boolean,
  val sourceDateEpoch: Long,
  val artifacts: List<String>,
  val timestamp: Long
)

data class DockerBuildMarker(
  val component: String,
  val version: String,
  val imageName: String,
  val tags: List<String>,
  val platforms: String,
  val vcsRevision: String,
  val buildNumber: Int,
  val baseImageDigests: Map<String, String?>,
  val dockerfile: String,
  val contextRel: String,
  val imageDigest: String,
  val primaryTag: String?,
  val provenanceEnabled: Boolean,
  val pullPolicy: String,
  val outputMode: String,
  val sourceDateEpoch: Long,
  val archiveDigest: String?,
  val timestamp: Long
)

data class DebBuildMarker(
  val component: String,
  val version: String,
  val distro: String,
  val packageName: String,
  val signed: Boolean,
  val keyId: String?,
  val builderImage: String,
  val builderImageTag: String,
  val builderPullPolicy: String,
  val sourceDateEpoch: Long,
  val artifacts: Map<String, String>,
  val timestamp: Long
)
