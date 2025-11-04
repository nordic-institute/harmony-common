package org.niis.harmony.buildlogic.providers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.niis.harmony.buildlogic.internal.Mappers
import org.niis.harmony.buildlogic.internal.utils.ManifestParser
import org.niis.harmony.buildlogic.models.Manifest
import org.niis.harmony.buildlogic.models.Scope
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
      return sha256("MISSING_MANIFEST/${parameters.scope.get()}/${parameters.distro.orNull.orEmpty()}")
    }

    val rootNode = yamlMapper.readTree(manifest)
    val inputsNode = rootNode.get("inputs") as? ArrayNode
      ?: return sha256("NO_INPUTS/${parameters.scope.get()}/${parameters.distro.orNull.orEmpty()}")

    val currentScope = parameters.scope.get()
    val currentDistro = parameters.distro.orNull

    val applicableSteps = filterAndCanonicalizeSteps(inputsNode, currentScope, currentDistro)

    if (applicableSteps.isEmpty) {
      return sha256("NO_MATCHING_STEPS/${currentScope}/${currentDistro.orEmpty()}")
    }

    val payload = buildString {
      appendLine("--SCOPE=${currentScope.name}")
      appendLine("--DISTRO=${currentDistro.orEmpty()}")
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

    obj.fieldNames().asSequence().toList().sorted().forEach { key ->
      val value = obj.get(key)
      sorted.set<JsonNode>(key, canonicalizeNode(value))
    }

    return sorted
  }

  private fun isSortableStringArray(node: JsonNode): Boolean =
    node.isArray && node.elements().asSequence().all { it.isTextual }

  private fun sortStringArray(array: ArrayNode): ArrayNode {
    val sortedValues = array.elements().asSequence()
      .map { it.asText("") }
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
