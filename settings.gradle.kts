rootProject.name = "harmony-common"

pluginManagement {
  includeBuild("build-logic")
}

buildCache {
  local {
    isEnabled = true
  }

  remote<HttpBuildCache> {
    val cacheUrl = providers.gradleProperty("harmony.cache.url").orNull
    url = cacheUrl?.let { uri(it) }
    isEnabled = cacheUrl != null

    isPush = providers.gradleProperty("harmony.cache.push")
      .map { it.toBoolean() }
      .getOrElse(false)

    credentials {
      username = providers.gradleProperty("harmony.cache.username").orNull
      password = providers.gradleProperty("harmony.cache.password").orNull
    }
  }
}
