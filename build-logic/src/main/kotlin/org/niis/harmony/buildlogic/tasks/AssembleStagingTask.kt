package org.niis.harmony.buildlogic.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.niis.harmony.buildlogic.internal.utils.ManifestParser
import org.niis.harmony.buildlogic.models.ArtifactReference
import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.ManifestReference
import org.niis.harmony.buildlogic.models.ProjectReference
import org.niis.harmony.buildlogic.models.Scope
import org.niis.harmony.buildlogic.models.VendorReference
import org.niis.harmony.buildlogic.tasks.inputs.AliasedClasspathInput
import org.niis.harmony.buildlogic.tasks.inputs.AliasedFileInput
import org.niis.harmony.buildlogic.tasks.inputs.AliasedPathInput
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import javax.inject.Inject

@CacheableTask
abstract class AssembleStagingTask @Inject constructor(
  private val layout: ProjectLayout,
  private val fileSystemOperations: FileSystemOperations,
  private val archiveOperations: ArchiveOperations,
) : DefaultTask() {

  @get:Internal
  abstract val manifestFile: RegularFileProperty

  @get:Internal
  abstract val component: Property<String>

  @get:Internal
  abstract val cacheRestoreOnly: Property<Boolean>

  @get:Input
  abstract val scope: Property<Scope>

  @get:Input
  abstract val sourceDateEpoch: Property<Long>

  @get:Input
  abstract val manifestFingerprint: Property<String>

  @get:Nested
  abstract val artifactClasspath: ListProperty<AliasedClasspathInput>

  @get:Nested
  abstract val vendorFiles: ListProperty<AliasedFileInput>

  @get:Nested
  abstract val projectPaths: ListProperty<AliasedPathInput>

  @get:Input
  @get:Optional
  abstract val distro: Property<String>

  @get:OutputDirectory
  abstract val stagingDir: DirectoryProperty

  private data class SourceIndexes(
    val artifacts: Map<String, File>,
    val vendors: Map<String, File>,
    val projectPaths: Map<String, List<File>>
  )

  @TaskAction
  fun execute() {
    check(!(cacheRestoreOnly.getOrElse(false))) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${project.providers.gradleProperty("harmony.${component.get()}.build.number").orNull ?: "unknown"}
      Component:    ${component.get()}
      Scope:        ${scope.get().name.lowercase()}
      Distro:       ${distro.orNull ?: "N/A"}
      """.trimIndent()
    }

    require(manifestFingerprint.orNull?.isNotBlank() == true) {
      "Manifest fingerprint is blank. Caching may be ineffective. " +
      "(component='${component.get()}', scope='${scope.get()}', distro='${distro.orNull}')."
    }

    val outputDirectory = stagingDir.get().asFile.toPath()
    prepareOutputDirectory(outputDirectory)

    val manifest = ManifestParser.parse(manifestFile.get().asFile)
    val sourceIndexes = buildSourceIndexes()
    val epochMillis = sourceDateEpoch.get()

    logger.lifecycle(
      "Assembling staging for component='{}', scope='{}'{} output='{}'",
      component.get(),
      scope.get(),
      distro.orNull?.let { ", distro='$it'," } ?: ",",
      outputDirectory.toAbsolutePath()
    )

    processManifestSteps(manifest, sourceIndexes, outputDirectory, epochMillis)
  }

  private fun prepareOutputDirectory(dir: Path) {
    if (Files.exists(dir)) {
      dir.toFile().deleteRecursively()
    }
    Files.createDirectories(dir)
  }

  private fun processManifestSteps(
    manifest: Manifest,
    sourceIndexes: SourceIndexes,
    outputDirectory: Path,
    epochMillis: Long
  ) {
    manifest.inputs.forEach { step ->
      if (!ManifestParser.applies(step.`when`, scope.get(), distro.orNull)) {
        return@forEach
      }

      val sourceFiles = resolveSourceFiles(step.from, sourceIndexes)
      val destinationPath = outputDirectory.resolve(normalizeAndValidatePath(step.into))

      when (step) {
        is Manifest.Copy -> handleCopy(step, sourceFiles, destinationPath, epochMillis)
        is Manifest.Unpack -> handleUnpack(step, sourceFiles, destinationPath, epochMillis)
      }
    }
  }

  private fun handleCopy(step: Manifest.Copy, sources: List<File>, destination: Path, epochMillis: Long) {
    logger.info("COPY from='{}' into='{}' preserveTop={}", step.from, step.into, step.preserveTop == true)

    val preserveTop = step.preserveTop == true
    val destinationIsDirectory = step.into.isDirectoryHint()
    val timestampTargets = LinkedHashSet<Path>()

    sources.forEach { source ->
      if (source.isDirectory) {
        val finalDestDir = if (preserveTop) destination.resolve(source.name) else destination
        fileSystemOperations.copy {
          from(source)
          into(finalDestDir.toFile())
        }
        timestampTargets.add(finalDestDir)
      } else {
        val finalDestFile = if (destinationIsDirectory) destination.resolve(source.name) else destination
        fileSystemOperations.copy {
          from(source)
          into(finalDestFile.parent.toFile())
          rename { finalDestFile.fileName.toString() }
        }
        timestampTargets.add(finalDestFile)
        timestampTargets.add(finalDestFile.parent)
      }
    }

    timestampTargets.forEach { setTimestamps(it, epochMillis) }
  }

  private fun handleUnpack(step: Manifest.Unpack, sources: List<File>, destination: Path, epochMillis: Long) {
    val stripComponents = step.strip ?: 0
    val (includes, excludes) = step.getFilters()

    val routes = step.routes.orEmpty()
    val hasRoutes = routes.isNotEmpty()
    val baseDestination = if (hasRoutes) Files.createTempDirectory("unpack-routed") else destination

    logger.info(
      "UNPACK from='{}' into='{}' strip={} includes={} excludes={} routed={}",
      step.from, step.into, stripComponents, includes.size, excludes.size, hasRoutes
    )

    sources.forEach { sourceFile ->
      val fileTree = createFileTreeFromArchive(sourceFile)

      fileSystemOperations.copy {
        from(fileTree)
        applyFilters(this, includes, excludes)
        applyStripping(this, stripComponents)
        into(baseDestination.toFile())
        includeEmptyDirs = false
      }
    }

    setTimestamps(baseDestination, epochMillis)

    if (hasRoutes) {
      try {
        routes.forEach { route ->
          val routeDest = destination.resolve(normalizeAndValidatePath(route.into))
          copyRoutedContent(baseDestination, routeDest, route, epochMillis)
        }
      } finally {
        baseDestination.toFile().deleteRecursively()
      }
    }
  }

  private fun copyRoutedContent(
    sourceDir: Path,
    destinationDir: Path,
    route: Manifest.Route,
    epochMillis: Long
  ) {
    val (includes, excludes) = route.getFilters()
    val commonPrefixSegments = computeCommonPrefixSegments(includes)

    fileSystemOperations.copy {
      from(sourceDir.toFile())
      applyFilters(this, includes, excludes)
      if (!commonPrefixSegments.isNullOrEmpty()) {
        applyPrefixStripping(this, commonPrefixSegments)
      }
      into(destinationDir.toFile())
      includeEmptyDirs = false
    }

    setTimestamps(destinationDir, epochMillis)
  }

  private fun buildSourceIndexes(): SourceIndexes {
    val artifacts = artifactClasspath.get().associate { it.alias.get() to it.file.get().asFile }
    val vendors = vendorFiles.get().associate { it.alias.get() to it.file.get().asFile }
    val projectPaths = projectPaths.get().associate { bean ->
      val base = layout.projectDirectory.file(bean.relBasePath.get()).asFile
      bean.alias.get() to listOf(base)
    }

    return SourceIndexes(artifacts, vendors, projectPaths)
  }

  private fun availableList(map: Map<String, *>): String =
    map.keys.sorted().joinToString(", ").ifBlank { "(none)" }

  private fun resolveSourceFiles(from: String, indexes: SourceIndexes): List<File> {
    return when (val reference = ManifestReference.fromString(from)) {
      is ArtifactReference -> {
        val file = indexes.artifacts[reference.alias]
          ?: error("Unknown artifact alias '${reference.alias}'. " +
                   "Available aliases: ${availableList(indexes.artifacts)}")
        listOf(file)
      }
      is VendorReference -> {
        val file = indexes.vendors[reference.alias]
          ?: error("Unknown vendor alias '${reference.alias}'. " +
                   "Available aliases: ${availableList(indexes.vendors)}")
        listOf(file)
      }
      is ProjectReference -> {
        val files = indexes.projectPaths[reference.path]
          ?: error("Unknown project path '${reference.path}'. " +
                   "Available project paths: ${availableList(indexes.projectPaths)}")
        require(files.all { it.exists() }) {
          "Project path '${reference.path}' not found (required for this step/scope)."
        }
        files
      }
    }
  }

  private fun normalizeAndValidatePath(path: String): Path {
    val raw = path.trim().replace('\\', '/')
    val normalized = raw.removePrefix("/").also {
      require(!it.contains("..")) { "Destination 'into' must not contain '..': $path" }
    }

    return Paths.get(normalized.ifBlank { "." })
  }

  private fun Manifest.Unpack.getFilters(): Pair<List<String>, List<String>> = filterPair(include, exclude)

  private fun Manifest.Route.getFilters(): Pair<List<String>, List<String>> = filterPair(include, exclude)

  private fun filterPair(include: List<String>?, exclude: List<String>?): Pair<List<String>, List<String>> {
    val includesFiltered = include?.filter { it.isNotBlank() }.orEmpty()
    val excludesFiltered = exclude?.filter { it.isNotBlank() }.orEmpty()
    return includesFiltered to excludesFiltered
  }

  private fun computeCommonPrefixSegments(patterns: List<String>): List<String>? {
    if (patterns.isEmpty()) return null

    val prefixes = patterns.mapNotNull { pattern ->
      val normalized = pattern.trim().replace('\\', '/')
      val wildcardIndex = normalized.indexOfAny(charArrayOf('*', '?'))

      val pathBeforeWildcard = if (wildcardIndex < 0) normalized else normalized.take(wildcardIndex)
      val lastSlash = pathBeforeWildcard.lastIndexOf('/')

      if (lastSlash > 0) {
        pathBeforeWildcard.take(lastSlash)
          .split('/')
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .takeIf { it.isNotEmpty() }
      } else {
        null
      }
    }

    if (prefixes.isEmpty()) return null

    val first = prefixes.first()
    return if (prefixes.all { it == first }) first else null
  }

  private fun createFileTreeFromArchive(archive: File): FileTree {
    val name = archive.name.lowercase()
    return when {
      name.endsWith(".tar.gz") || name.endsWith(".tgz") ->
        archiveOperations.tarTree(archive)
      name.endsWith(".tar") ->
        archiveOperations.tarTree(archive)
      name.endsWith(".zip") || name.endsWith(".war") || name.endsWith(".jar") ->
        archiveOperations.zipTree(archive)
      else -> error("Unsupported archive format: ${archive.name}")
    }
  }

  private fun applyFilters(
    spec: CopySpec,
    includes: List<String>,
    excludes: List<String>
  ) {
    if (includes.isNotEmpty()) {
      spec.include(includes)
    }
    if (excludes.isNotEmpty()) {
      spec.exclude(excludes)
    }
  }

  private fun applyStripping(spec: CopySpec, stripComponents: Int) {
    if (stripComponents <= 0) return

    spec.eachFile(object : Action<FileCopyDetails> {
      override fun execute(details: FileCopyDetails) {
        val segments = details.relativePath.segments
        if (segments.size > stripComponents) {
          val newSegments = segments.drop(stripComponents).toTypedArray()
          details.relativePath = RelativePath(true, *newSegments)
        } else {
          details.exclude()
        }
      }
    })
  }

  private fun applyPrefixStripping(spec: CopySpec, prefixSegments: List<String>) {
    if (prefixSegments.isEmpty()) return

    spec.eachFile(object : Action<FileCopyDetails> {
      override fun execute(details: FileCopyDetails) {
        val segments = details.relativePath.segments.toList()
        if (segments.size >= prefixSegments.size &&
          segments.take(prefixSegments.size) == prefixSegments
        ) {
          val newSegments = segments.drop(prefixSegments.size)
          if (newSegments.isEmpty()) {
            details.exclude()
          } else {
            details.relativePath = RelativePath(true, *newSegments.toTypedArray())
          }
        }
      }
    })
  }

  private fun setTimestamps(path: Path, epochMillis: Long) {
    if (!Files.exists(path)) return

    val timestamp = FileTime.fromMillis(epochMillis)

    runSafely(path,
      action = {
        if (Files.isDirectory(path)) {
          Files.walk(path).use { stream ->
            stream.forEach { file -> setTimestampOnPath(file, timestamp) }
          }
        } else {
          setTimestampOnPath(path, timestamp)
        }
      },
      onAccessDeniedMsg = "Insufficient permissions to enumerate for timestamps on: {}",
      onIoMsg = "I/O error while enumerating for timestamps on: {} - {}",
      onUnexpectedMsg = "Unexpected error while enumerating for timestamps on: {}"
    )
  }

  private fun setTimestampOnPath(file: Path, timestamp: FileTime) {
    runSafely(
      file,
      action = { Files.setLastModifiedTime(file, timestamp) },
      onAccessDeniedMsg = "Insufficient permissions to set timestamp on: {}",
      onIoMsg = "I/O error setting timestamp on: {} - {}",
      onUnexpectedMsg = "Unexpected error setting timestamp on: {}"
    )
  }

  private inline fun <T> runSafely(
    path: Path,
    action: () -> T,
    onAccessDeniedMsg: String,
    onIoMsg: String,
    onUnexpectedMsg: String
  ): T? {
    try {
      return action()
    } catch (_: AccessDeniedException) {
      logger.warn(onAccessDeniedMsg, path)
    } catch (e: IOException) {
      logger.warn(onIoMsg, path, e.message)
    } catch (e: Exception) {
      logger.warn(onUnexpectedMsg, path, e)
    }
    return null
  }

  private fun String.isDirectoryHint(): Boolean =
    endsWith("/") || endsWith("\\") || this == "." || this == "./" || this == "/"
}
