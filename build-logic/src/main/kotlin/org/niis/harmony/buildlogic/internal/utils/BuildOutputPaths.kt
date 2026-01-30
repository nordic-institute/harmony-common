package org.niis.harmony.buildlogic.internal.utils

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.niis.harmony.buildlogic.models.Scope

object BuildOutputPaths {
  fun compileMarker(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/compile/$component/$it.json") }

  fun compileMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/compile/$component")

  fun stagingFingerprint(project: Project, component: String, scope: Scope, distro: String? = null): Provider<RegularFile> {
    val scopeSegment = scope.name.lowercase()
    val path = if (scope == Scope.DEB && distro != null) {
      "metadata/staging/$component/deb/$distro/fingerprint.sha256"
    } else {
      "metadata/staging/$component/$scopeSegment/fingerprint.sha256"
    }
    return project.layout.buildDirectory.file(path)
  }

  fun debStagingDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("staging/$component/deb")

  fun debStagingDir(project: Project, component: String, distro: String): Provider<Directory> =
    debStagingDir(project, component).map { it.dir(distro) }

  fun debMarker(project: Project, component: String, version: Provider<String>, distro: String): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/deb/$component/$distro/$it.json") }

  fun debMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/deb/$component")

  fun debArtifactsRoot(project: Project, component: String, version: Provider<String>): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("deb/$component/$it") }

  fun debOutputDir(project: Project, component: String, version: Provider<String>, distro: String): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("deb/$component/$it/$distro") }

  fun dockerStagingDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("staging/$component/docker")

  fun dockerMarker(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/docker/$component/$it.json") }

  fun dockerMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/docker/$component")

  fun dockerOutputTar(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("docker/$component/$it/image.tar") }

  fun dockerOutputOciDir(project: Project, component: String, version: Provider<String>): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("docker/$component/$it/image-oci") }

  fun dockerOutputOciTar(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("docker/$component/$it/image-oci.tar") }
}
