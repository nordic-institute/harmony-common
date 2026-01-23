package org.niis.harmony.buildlogic.internal.env

enum class HostOs { MAC, WINDOWS, LINUX, UNKNOWN }

object HostInfoService {

  @Volatile
  private var cachedHostOs: HostOs? = null

  fun detectOs(osNameRaw: String? = null): HostOs {
    if (osNameRaw == null) {
      cachedHostOs?.let { return it }
    }

    val osName = (osNameRaw ?: System.getProperty("os.name"))?.lowercase().orEmpty()
    val detected = detectFromOsName(osName)

    if (osNameRaw == null) {
      cachedHostOs = detected
    }

    return detected
  }

  private fun detectFromOsName(osName: String): HostOs {
    val macIndicators = listOf("mac", "darwin")
    val windowsIndicators = listOf("win", "msys", "mingw", "cygwin")
    val linuxIndicators = listOf("nux", "linux")

    return when {
      macIndicators.any { osName.contains(it) } -> HostOs.MAC
      windowsIndicators.any { osName.contains(it) } -> HostOs.WINDOWS
      linuxIndicators.any { osName.contains(it) } -> HostOs.LINUX
      else -> HostOs.UNKNOWN
    }
  }
}
