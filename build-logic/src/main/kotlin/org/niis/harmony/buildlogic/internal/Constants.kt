package org.niis.harmony.buildlogic.internal

internal object Constants {

  object BuildDefaults {
    // General
    val COMPONENTS: List<String> = listOf("ap", "smp")
    const val BUILD_NUMBER: Int = 0

    // Cache
    const val CACHE_RESTORE_ONLY: Boolean = false

    // Compile
    const val COMPILE_SKIP_TESTS: Boolean = false
    val COMPILE_MAVEN_PROFILES: List<String> = emptyList()
    val COMPILE_MAVEN_GOALS: List<String> = listOf("clean", "package")
    const val COMPILE_JAVA_VERSION: Int = 8

    // Deb
    val DEB_DISTROS: List<String> = listOf("jammy", "noble")
    const val DEB_SIGN: Boolean = false
    const val DEB_BUILDER_IMAGE: String = "ghcr.io/nordic-institute/harmony-deb-builder"
    const val DEB_BUILDER_TAG: String = "1.0.0"

    // Docker
    const val DOCKER_PLATFORMS: String = ""
    const val DOCKER_OUTPUT_MODE: String = "load"
    const val DOCKER_TRACK_BASE: Boolean = true
    const val DOCKER_PULL_ALWAYS: Boolean = true
    const val DOCKER_PROVENANCE_DISABLED: Boolean = true
  }

  object MavenBuild {
    val DEFAULT_ARGS = listOf("-B", "--no-transfer-progress", "--fail-at-end")
    const val PROP_LOCAL_REPO = "-Dmaven.repo.local"
    val SKIP_TESTS_ARGS = listOf("-DskipTests", "-DskipITs")
    const val WRAPPER_UNIX = "mvnw"
    const val WRAPPER_WINDOWS = "mvnw.cmd"
  }

  object DebianPackaging {
    const val FLAG_BINARY_ONLY = "-b"
    const val FLAG_FAKEROOT = "-rfakeroot"
    const val FLAG_SIGN_KEY = "-k"
    val FLAGS_UNSIGNED = listOf("-us", "-uc")
  }

  object ProcessExecution {
    const val DEFAULT_TIMEOUT_SECONDS = 120L
    const val MAX_CAPTURED_OUTPUT_CHARS = 1_000
  }
}
