package org.niis.harmony.buildlogic.internal.utils

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

object BuildOutputPaths {
  fun compileMarker(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/compile/$component/$it.json") }

  fun dockerMarker(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/docker/$component/$it.json") }

  fun debMarker(project: Project, component: String, version: Provider<String>, distro: String): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("metadata/deb/$component/$it-$distro.json") }

  fun compileMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/compile/$component")

  fun dockerMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/docker/$component")

  fun debMarkerDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("metadata/deb/$component")

  fun dockerStagingDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("staging/$component/docker")

  fun debStagingDir(project: Project, component: String): Provider<Directory> =
    project.layout.buildDirectory.dir("staging/$component/deb")

  fun debStagingDir(project: Project, component: String, distro: String): Provider<Directory> =
    debStagingDir(project, component).map { it.dir(distro) }

  fun debOutputDir(project: Project, component: String, version: Provider<String>, distro: String): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("deb/$component/$it/$distro") }

  fun debArtifactsRoot(project: Project, component: String, version: Provider<String>): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("deb/$component/$it") }

  fun dockerOutputTar(project: Project, component: String, version: Provider<String>): Provider<RegularFile> =
    version.flatMap { project.layout.buildDirectory.file("docker/$component/$it/image.tar") }

  fun dockerOutputOci(project: Project, component: String, version: Provider<String>): Provider<Directory> =
    version.flatMap { project.layout.buildDirectory.dir("docker/$component/$it/image-oci") }
}
