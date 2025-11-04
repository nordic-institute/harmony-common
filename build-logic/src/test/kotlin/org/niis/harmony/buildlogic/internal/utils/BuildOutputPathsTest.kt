package org.niis.harmony.buildlogic.internal.utils

import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import kotlin.io.path.relativeTo
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildOutputPathsTest {

  private val project = ProjectBuilder.builder()
    .withProjectDir(Files.createTempDirectory("build-output-paths").toFile())
    .build()

  @Test
  fun `provides compile marker path`() {
    val marker = BuildOutputPaths.compileMarker(project, "ap", project.provider { "2.6.0" }).get().asFile.toPath()
    val relative = marker.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("metadata/compile/ap/2.6.0.json", relative)
  }

  @Test
  fun `provides docker marker path`() {
    val marker = BuildOutputPaths.dockerMarker(project, "ap", project.provider { "2.6.0" }).get().asFile.toPath()
    val relative = marker.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("metadata/docker/ap/2.6.0.json", relative)
  }

  @Test
  fun `provides deb marker path`() {
    val marker = BuildOutputPaths.debMarker(project, "ap", project.provider { "2.6.0" }, "jammy").get().asFile.toPath()
    val relative = marker.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("metadata/deb/ap/2.6.0-jammy.json", relative)
  }

  @Test
  fun `provides docker staging dir`() {
    val dir = BuildOutputPaths.dockerStagingDir(project, "ap").get().asFile.toPath()
    val relative = dir.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("staging/ap/docker", relative)
  }

  @Test
  fun `provides deb distro specific path`() {
    val dir = BuildOutputPaths.debStagingDir(project, "ap", "jammy").get().asFile.toPath()
    val relative = dir.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("staging/ap/deb/jammy", relative)
  }

  @Test
  fun `provides deb artifacts root`() {
    val root = BuildOutputPaths.debArtifactsRoot(project, "ap", project.provider { "1.2.3" }).get().asFile.toPath()
    val relative = root.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("deb/ap/1.2.3", relative)
  }

  @Test
  fun `provides docker tar output path`() {
    val tar = BuildOutputPaths.dockerOutputTar(project, "ap", project.provider { "2.6.0" }).get().asFile.toPath()
    val relative = tar.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("docker/ap/2.6.0/image.tar", relative)
  }

  @Test
  fun `provides docker oci output path`() {
    val oci = BuildOutputPaths.dockerOutputOci(project, "ap", project.provider { "2.6.0" }).get().asFile.toPath()
    val relative = oci.relativeTo(project.buildDir.toPath()).toString().replace('\\', '/')
    assertEquals("docker/ap/2.6.0/image-oci", relative)
  }
}
