package org.niis.harmony.buildlogic.internal.utils

import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildOutputPathsTest {

  private val project = ProjectBuilder.builder()
    .withProjectDir(Files.createTempDirectory("build-output-paths").toFile())
    .build()

  private val buildDir: Path
    get() = project.layout.buildDirectory.get().asFile.toPath()

  @Test
  fun `provides compile marker path`() {
    val marker = BuildOutputPaths.compileMarker(project, "ap", project.provider { "1.0.0" }).get().asFile.toPath()
    val relative = marker.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("metadata/compile/ap/1.0.0.json", relative)
  }

  @Test
  fun `provides docker marker path`() {
    val marker = BuildOutputPaths.dockerMarker(project, "ap", project.provider { "1.0.0" }).get().asFile.toPath()
    val relative = marker.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("metadata/docker/ap/1.0.0.json", relative)
  }

  @Test
  fun `provides deb marker path`() {
    val marker = BuildOutputPaths.debMarker(project, "ap", project.provider { "1.0.0" }, "jammy").get().asFile.toPath()
    val relative = marker.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("metadata/deb/ap/1.0.0-jammy.json", relative)
  }

  @Test
  fun `provides docker staging dir`() {
    val dir = BuildOutputPaths.dockerStagingDir(project, "ap").get().asFile.toPath()
    val relative = dir.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("staging/ap/docker", relative)
  }

  @Test
  fun `provides deb distro specific path`() {
    val dir = BuildOutputPaths.debStagingDir(project, "ap", "jammy").get().asFile.toPath()
    val relative = dir.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("staging/ap/deb/jammy", relative)
  }

  @Test
  fun `provides deb artifacts root`() {
    val root = BuildOutputPaths.debArtifactsRoot(project, "ap", project.provider { "1.2.3" }).get().asFile.toPath()
    val relative = root.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("deb/ap/1.2.3", relative)
  }

  @Test
  fun `provides docker tar output path`() {
    val tar = BuildOutputPaths.dockerOutputTar(project, "ap", project.provider { "1.0.0" }).get().asFile.toPath()
    val relative = tar.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("docker/ap/1.0.0/image.tar", relative)
  }

  @Test
  fun `provides docker oci dir output path`() {
    val ociDir = BuildOutputPaths.dockerOutputOciDir(project, "ap", project.provider { "1.0.0" }).get().asFile.toPath()
    val relative = ociDir.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("docker/ap/1.0.0/image-oci", relative)
  }

  @Test
  fun `provides docker oci tar output path`() {
    val ociTar = BuildOutputPaths.dockerOutputOciTar(project, "ap", project.provider { "1.0.0" }).get().asFile.toPath()
    val relative = ociTar.relativeTo(buildDir).toString().replace('\\', '/')
    assertEquals("docker/ap/1.0.0/image-oci.tar", relative)
  }
}
