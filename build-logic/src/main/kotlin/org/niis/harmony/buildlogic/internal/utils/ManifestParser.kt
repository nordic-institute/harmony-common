package org.niis.harmony.buildlogic.internal.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.Scope
import java.io.File
import java.util.Locale

object ManifestParser {

  private val yamlMapper: ObjectMapper = Mappers.yaml

  fun applies(whenBlock: Manifest.When?, scope: Scope, distro: String?): Boolean {
    if (whenBlock == null) return true

    val scopes  = whenBlock.scopes.map { it.lowercase(Locale.ROOT) }
    val distros = whenBlock.distros.map { it.lowercase(Locale.ROOT) }

    val scopeOk = scopes.isEmpty() || when (scope) {
      Scope.DEB    -> "deb" in scopes
      Scope.DOCKER -> "docker" in scopes
    }
    val distroOk = distros.isEmpty() || (distro != null && distros.contains(distro.lowercase(Locale.ROOT)))

    return scopeOk && distroOk
  }

  fun parse(file: File): Manifest {
    require(file.isFile) { "Manifest file not found: ${file.absolutePath}" }
    require(file.canRead()) { "Manifest file is not readable: ${file.absolutePath}" }
    return runCatching {
      yamlMapper.readValue(file, Manifest::class.java)
    }.getOrElse { e ->
      throw IllegalArgumentException("Failed to parse manifest: ${file.absolutePath}", e)
    }
  }
}
