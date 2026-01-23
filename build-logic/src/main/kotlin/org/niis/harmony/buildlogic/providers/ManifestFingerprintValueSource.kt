package org.niis.harmony.buildlogic.providers

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.ManifestParser
import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.Scope
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.security.MessageDigest

abstract class ManifestFingerprintValueSource : ValueSource<String, ManifestFingerprintValueSource.Params> {

  interface Params : ValueSourceParameters {
    val manifestFile: RegularFileProperty
    val scope: Property<Scope>
    val distro: Property<String>
  }

  private val yamlMapper: ObjectMapper = Mappers.yaml
  private val jsonMapper: ObjectMapper = Mappers.json

  override fun obtain(): String {
    val manifest = parameters.manifestFile.asFile.get()
    if (!manifest.isFile) {
      throw GradleException(
        "Cannot determine manifest fingerprint: manifest file '${manifest.absolutePath}' does not exist. " +
        "Ensure the manifest is generated or configure the correct path."
      )
    }

    val rootNode = yamlMapper.readTree(manifest)
    val inputsNode = rootNode.get("inputs") as? ArrayNode
      ?: throw GradleException(
        "Cannot determine manifest fingerprint: manifest '${manifest.absolutePath}' has no 'inputs' array. " +
        "Check the manifest format or adjust the fingerprinting logic."
      )

    val scope = parameters.scope.get()
    val rawDistro = parameters.distro.orNull

    val effectiveDistro = when (scope) {
      Scope.DOCKER -> rawDistro ?: "docker"
      else -> rawDistro ?: throw GradleException(
        "Cannot determine manifest fingerprint: distro was not configured for scope '$scope'. " +
        "This is an internal configuration error. Ensure 'distro' is set when wiring ManifestFingerprintValueSource."
      )
    }

    val applicableSteps = filterAndCanonicalizeSteps(inputsNode, scope, effectiveDistro)

    if (applicableSteps.isEmpty) {
      return sha256("NO_MATCHING_STEPS/$scope/$effectiveDistro")
    }

    val payload = buildString {
      appendLine("--SCOPE=${scope.name}")
      appendLine("--DISTRO=$effectiveDistro")
      append(jsonMapper.writeValueAsString(applicableSteps))
    }
    return sha256(payload)
  }

  private fun filterAndCanonicalizeSteps(
    allSteps: ArrayNode,
    scope: Scope,
    distro: String?
  ): ArrayNode {
    val filteredSteps = jsonMapper.createArrayNode()
    allSteps.forEach { node ->
      if (node !is ObjectNode) return@forEach

      val whenNode = node.get("when")
      val whenCondition = if (whenNode == null || whenNode.isNull) {
        null
      } else {
        yamlMapper.treeToValue(whenNode, Manifest.When::class.java)
      }

      if (ManifestParser.applies(whenCondition, scope, distro)) {
        filteredSteps.add(canonicalizeNode(node))
      }
    }
    return filteredSteps
  }

  private fun canonicalizeNode(node: JsonNode): JsonNode = when {
    node.isObject -> canonicalizeObject(node as ObjectNode)
    node.isArray -> {
      if (isSortableStringArray(node)) {
        sortStringArray(node as ArrayNode)
      } else {
        val newArray = jsonMapper.createArrayNode()
        node.forEach { element -> newArray.add(canonicalizeNode(element)) }
        newArray
      }
    }
    else -> node
  }

  private fun canonicalizeObject(obj: ObjectNode): ObjectNode {
    val sorted = jsonMapper.createObjectNode()

    obj.propertyNames().toList().sorted().forEach { key ->
      val value = obj.get(key)
      sorted.set(key, canonicalizeNode(value))
    }

    return sorted
  }

  private fun isSortableStringArray(node: JsonNode): Boolean =
    node.isArray && node.all { it.isString }

  private fun sortStringArray(array: ArrayNode): ArrayNode {
    val sortedValues = array.asSequence()
      .map { it.asString("") }
      .sorted()

    val newArray = jsonMapper.createArrayNode()
    sortedValues.forEach { newArray.add(it) }

    return newArray
  }

  private fun sha256(s: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) { digest.forEach { append("%02x".format(it)) } }
  }
}
