package org.niis.harmony.buildlogic.internal

internal object Constants {

  object BuildDefaults {
    // General
    val DEFAULT_COMPONENTS: List<String> = listOf("ap", "smp")
    const val DEFAULT_BUILD_NUMBER: Int = 0

    // Cache
    const val DEFAULT_CACHE_RESTORE_ONLY: Boolean = false

    // Compile
    const val DEFAULT_COMPILE_SKIP_TESTS: Boolean = false
    val DEFAULT_COMPILE_MAVEN_PROFILES: List<String> = emptyList()
    val DEFAULT_COMPILE_MAVEN_GOALS: List<String> = listOf("clean", "package")
    const val DEFAULT_COMPILE_JAVA_VERSION: Int = 21

    // Debian packages
    val DEFAULT_DEB_DISTROS: List<String> = listOf("jammy", "noble")
    const val DEFAULT_DEB_SIGN_ENABLED: Boolean = false
    const val DEFAULT_DEB_BUILDER_IMAGE: String =
      "artifactory.niis.org/harmony-release-docker/niis/harmony-deb-builder"
    const val DEFAULT_DEB_BUILDER_TAG: String = "1.0.0"
    const val DEFAULT_DEB_BUILDER_PULL_POLICY: String = "if-not-present"

    // Docker
    const val DEFAULT_DOCKER_PLATFORMS: String = ""
    const val DEFAULT_DOCKER_OUTPUT_MODE: String = "load"
    const val DEFAULT_DOCKER_TRACK_BASE_ENABLED: Boolean = true
    const val DEFAULT_DOCKER_PULL_POLICY: String = "if-not-present"
    const val DEFAULT_DOCKER_PROVENANCE_ENABLED: Boolean = false
  }

  object MavenBuild {
    // Default common Maven arguments
    val DEFAULT_ARGS: List<String> =
      listOf("-B", "--no-transfer-progress", "--fail-at-end")

    // Property used to override local Maven repository
    const val LOCAL_REPO_PROPERTY: String = "-Dmaven.repo.local"

    // Additional arguments to skip tests
    val SKIP_TESTS_PROPERTIES: List<String> =
      listOf("-DskipTests", "-DskipITs")

    // Maven wrapper scripts
    const val WRAPPER_SCRIPT_UNIX: String = "mvnw"
    const val WRAPPER_SCRIPT_WINDOWS: String = "mvnw.cmd"
  }

  object DebianPackaging {
    // dpkg-buildpackage flags
    const val FLAG_BINARY_ONLY: String = "-b"
    const val FLAG_FAKEROOT: String = "-rfakeroot"
    const val FLAG_SIGN_KEY: String = "-k"

    // Flags used when building unsigned packages
    val UNSIGNED_FLAGS: List<String> = listOf("-us", "-uc")
  }

  object ProcessExecution {
    const val DEFAULT_TIMEOUT_SECONDS: Long = 120L
    const val MAX_CAPTURED_OUTPUT_CHARS: Int = 1_000
  }
}
