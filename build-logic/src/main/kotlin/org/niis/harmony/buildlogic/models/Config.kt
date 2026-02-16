package org.niis.harmony.buildlogic.models

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

data class BuildConfig(
  val components: Provider<List<String>>,
  val debBuilderImage: Provider<String>,
  val debBuilderTag: Provider<String>,
  val debBuilderPullPolicy: Provider<PullPolicy>,
  val cache: CacheConfig
)

data class CacheConfig(
  val restoreOnly: Provider<Boolean>
)

data class ComponentConfig(
  val name: String,
  val version: Provider<String>,
  val compile: CompileConfig,
  val staging: StagingConfig,
  val deb: DebConfig,
  val docker: DockerTargetConfig,
  val buildInfo: BuildInfoConfig,
  val vendor: VendorConfig
)

data class CompileConfig(
  val skipTests: Provider<Boolean>,
  val repoDir: Provider<Directory>,
  val mavenProfiles: Provider<List<String>>,
  val mavenGoals: Provider<List<String>>,
  val mavenLocalRepo: Provider<String>,
  val javaVersion: Provider<Int>,
  val artifacts: Provider<Map<String, Provider<RegularFile>>>
)

data class StagingConfig(
  val manifestFile: File
)

data class DebConfig(
  val distros: Provider<List<String>>,
  val sign: Provider<Boolean>,
  val keyId: Provider<String>,
  val packageName: Provider<String>,
  val gpgHome: Provider<String>
)

data class DockerTargetConfig(
  val imageName: Provider<String>,
  val tags: Provider<List<String>>,
  val platforms: Provider<String>,
  val outputMode: Provider<DockerOutputMode>,
  val trackBase: Provider<Boolean>,
  val pullPolicy: Provider<PullPolicy>,
  val provenanceEnabled: Provider<Boolean>,
  val baseImageDigests: Provider<Map<String, String?>>
)

data class BuildInfoConfig(
  val epoch: Provider<Long>,
  val revision: Provider<String>,
  val buildNumber: Provider<Int>
)

data class VendorConfig(
  val resolveVersion: (dependency: String) -> Provider<String>,
  val resolveNotation: (dependency: String, version: String, classifier: String?) -> String
)
