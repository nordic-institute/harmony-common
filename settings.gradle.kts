rootProject.name = "harmony-common"

pluginManagement {
  includeBuild("build-logic")
}

buildCache {
  local {
    isEnabled = true
    directory = file(".gradle/build-cache")
  }

  remote<HttpBuildCache> {
    val cacheUrl = providers.gradleProperty("harmony.cache.url").orNull
    val component = providers.gradleProperty("harmony.cache.component").orNull
    val version = providers.gradleProperty("harmony.cache.version").orNull

    url = when {
      cacheUrl != null && component != null && version != null ->
        uri("$cacheUrl$component/$version/")
      cacheUrl != null ->
        uri(cacheUrl)
      else -> null
    }
    isEnabled = url != null

    isPush = providers.gradleProperty("harmony.cache.push")
      .map { it.toBoolean() }
      .getOrElse(false)

    credentials {
      username = providers.gradleProperty("harmony.cache.username").orNull
      password = providers.gradleProperty("harmony.cache.password").orNull
    }
  }
}
