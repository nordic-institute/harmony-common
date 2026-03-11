package org.niis.harmony.buildlogic.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
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
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

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

  @get:Internal
  abstract val buildNumber: Property<Int>

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
  abstract val vars: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val distro: Property<String>

  @get:OutputFile
  abstract val stagingFingerprint: RegularFileProperty

  @get:OutputDirectory
  abstract val stagingDir: DirectoryProperty

  private data class SourceIndexes(
    val artifacts: Map<String, File>,
    val vendors: Map<String, File>,
    val projectPaths: Map<String, List<File>>
  )

  @TaskAction
  fun execute() {
    check(!(cacheRestoreOnly.get())) {
      """
      Build cache miss: This task requires cached artifacts but none were found.

      Build number: #${buildNumber.orNull ?: "unknown"}
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
    val epochSeconds = sourceDateEpoch.get()

    logger.lifecycle(
      "Assembling staging for component='{}', scope='{}'{} output='{}'",
      component.get(),
      scope.get(),
      distro.orNull?.let { ", distro='$it'," } ?: ",",
      outputDirectory.toAbsolutePath()
    )

    processManifestSteps(manifest, sourceIndexes, outputDirectory, epochSeconds)

    writeStagingFingerprint(outputDirectory, stagingFingerprint.get().asFile.toPath())
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
    epochSeconds: Long
  ) {
    manifest.inputs.forEach { step ->
      if (!ManifestParser.applies(step.`when`, scope.get(), distro.orNull)) {
        return@forEach
      }

      val sourceFiles = resolveSourceFiles(step.from, sourceIndexes)
      val destinationPath = outputDirectory.resolve(normalizeAndValidatePath(step.into))

      when (step) {
        is Manifest.Copy -> handleCopy(step, sourceFiles, destinationPath, epochSeconds)
        is Manifest.Unpack -> handleUnpack(step, sourceFiles, destinationPath, epochSeconds)
      }
    }
  }

  private fun handleCopy(step: Manifest.Copy, sources: List<File>, destination: Path, epochSeconds: Long) {
    logger.info("COPY from='{}' into='{}' preserveTop={}", step.from, step.into, step.preserveTop == true)

    val preserveTop = step.preserveTop == true
    val destinationIsDirectory = step.into.isDirectoryHint()
    val timestampTargets = LinkedHashSet<Path>()
    val activeVars = vars.get()

    sources.forEach { source ->
      if (source.isDirectory) {
        val finalDestDir = if (preserveTop) destination.resolve(source.name) else destination
        fileSystemOperations.copy {
          from(source)
          into(finalDestDir.toFile())
          applyVarsToRelativePath(this, activeVars)
          includeEmptyDirs = false
        }
        timestampTargets.add(finalDestDir)
      } else {
        val finalDestFile = if (destinationIsDirectory) destination.resolve(source.name) else destination
        fileSystemOperations.copy {
          from(source)
          into(finalDestFile.parent.toFile())
          rename { finalDestFile.fileName.toString() }
          includeEmptyDirs = false
        }
        timestampTargets.add(finalDestFile)
        timestampTargets.add(finalDestFile.parent)
      }
    }

    timestampTargets.forEach { setTimestamps(it, epochSeconds) }
  }

  private fun handleUnpack(step: Manifest.Unpack, sources: List<File>, destination: Path, epochSeconds: Long) {
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
        applyVarsToRelativePath(this, vars.get())
        into(baseDestination.toFile())
        includeEmptyDirs = false
      }
    }

    setTimestamps(baseDestination, epochSeconds)

    if (hasRoutes) {
      try {
        routes.forEach { route ->
          val routeDest = destination.resolve(normalizeAndValidatePath(route.into))
          copyRoutedContent(baseDestination, routeDest, route, epochSeconds)
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
    epochSeconds: Long
  ) {
    val (includes, excludes) = route.getFilters()
    val commonPrefixSegments = computeCommonPrefixSegments(includes)

    fileSystemOperations.copy {
      from(sourceDir.toFile())
      applyFilters(this, includes, excludes)
      if (!commonPrefixSegments.isNullOrEmpty()) {
        applyPrefixStripping(this, commonPrefixSegments)
      }
      applyVarsToRelativePath(this, vars.get())
      into(destinationDir.toFile())
      includeEmptyDirs = false
    }

    setTimestamps(destinationDir, epochSeconds)
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
          ?: throw GradleException("Unknown artifact alias '${reference.alias}'. " +
                   "Available aliases: ${availableList(indexes.artifacts)}")
        listOf(file)
      }
      is VendorReference -> {
        val file = indexes.vendors[reference.alias]
          ?: throw GradleException("Unknown vendor alias '${reference.alias}'. " +
                   "Available aliases: ${availableList(indexes.vendors)}")
        listOf(file)
      }
      is ProjectReference -> {
        val files = indexes.projectPaths[reference.path]
          ?: throw GradleException("Unknown project path '${reference.path}'. " +
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
      else -> throw GradleException("Unsupported archive format: ${archive.name}")
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

  private fun setTimestamps(path: Path, epochSeconds: Long) {
    if (!Files.exists(path)) return

    val timestamp = FileTime.from(epochSeconds, TimeUnit.SECONDS)

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

  private fun applyVarsToRelativePath(spec: CopySpec, vars: Map<String, String>) {
    if (vars.isEmpty()) return

    spec.eachFile(object : Action<FileCopyDetails> {
      override fun execute(details: FileCopyDetails) {
        val segments = details.relativePath.segments
        var changed = false
        val newSegments = segments.map { segment ->
          val replacement = vars[segment]
          if (replacement != null) {
            changed = true
            replacement
          } else {
            segment
          }
        }.toTypedArray()
        if (changed) {
          details.relativePath = RelativePath(details.relativePath.isFile, *newSegments)
        }
      }
    })
  }

  private fun String.isDirectoryHint(): Boolean =
    endsWith("/") || endsWith("\\") || this == "." || this == "./" || this == "/"

  private fun writeStagingFingerprint(stagingRoot: Path, outFile: Path) {
    val parentDir = outFile.parent
      ?: throw GradleException("Fingerprint output path has no parent directory: $outFile")

    try {
      parentDir.createDirectories()
      if (!Files.isDirectory(parentDir)) {
        throw GradleException("Fingerprint output parent is not a directory: $parentDir")
      }
    } catch (e: Exception) {
      throw GradleException("Failed to create staging fingerprint directory: $parentDir", e)
    }

    val sha256 = MessageDigest.getInstance("SHA-256")
    val chunkBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val longBuffer = ByteArray(8)

    fun updateWithString(value: String) {
      sha256.update(value.toByteArray(Charsets.UTF_8))
      sha256.update(0)
    }

    fun updateWithLong(value: Long) {
      for (i in 7 downTo 0) {
        longBuffer[7 - i] = ((value ushr (i * 8)) and 0xFF).toByte()
      }
      sha256.update(longBuffer)
      sha256.update(0)
    }

    try {
      val entries = ArrayList<Pair<String, Path>>(1024)

      Files.walk(stagingRoot).use { stream ->
        stream.forEach { path ->
          if (path.isSymbolicLink()) {
            val rel = stagingRoot.relativize(path).invariantSeparatorsPathString
            throw GradleException("Symlink detected in staging directory (not allowed): $rel")
          }
          if (path.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
            val rel = stagingRoot.relativize(path).invariantSeparatorsPathString
            entries.add(rel to path)
          }
        }
      }

      entries.sortBy { it.first }

      for ((rel, file) in entries) {
        updateWithString(rel)

        updateWithLong(Files.size(file))

        Files.newInputStream(file).use { input: InputStream ->
          while (true) {
            val read = input.read(chunkBuffer)
            if (read < 0) break
            if (read > 0) sha256.update(chunkBuffer, 0, read)
          }
        }

        sha256.update(0)
      }
    } catch (e: Exception) {
      throw GradleException("Failed to compute staging fingerprint for: $stagingRoot", e)
    }

    val fingerprintHex = sha256.digest().joinToString("") { "%02x".format(it) }
    try {
      val tmp = Files.createTempFile(parentDir, "fingerprint-", ".tmp")
      Files.writeString(tmp, "$fingerprintHex\n")
      Files.move(tmp, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (e: IOException) {
      throw GradleException("Failed to write staging fingerprint to: $outFile", e)
    }
  }
}
