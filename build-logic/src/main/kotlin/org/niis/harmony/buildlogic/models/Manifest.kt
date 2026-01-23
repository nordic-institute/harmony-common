package org.niis.harmony.buildlogic.models

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class Manifest(
  val inputs: List<Input> = emptyList()
) {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "do")
  @JsonSubTypes(
    JsonSubTypes.Type(value = Copy::class, name = "copy"),
    JsonSubTypes.Type(value = Unpack::class, name = "unpack")
  )
  sealed interface Input {
    val from: String
    val into: String
    val `when`: When?
  }

  data class Copy(
    override val from: String,
    val preserveTop: Boolean? = null,
    override val into: String,
    override val `when`: When? = null,
  ) : Input

  data class Unpack(
    override val from: String,
    val strip: Int? = null,
    val include: List<String>? = null,
    val exclude: List<String>? = null,
    val routes: List<Route>? = null,
    override val into: String,
    override val `when`: When? = null,
  ) : Input

  data class Route(
    val include: List<String> = emptyList(),
    val exclude: List<String> = emptyList(),
    val into: String
  )

  data class When(
    val scopes: List<String> = emptyList(),
    val distros: List<String> = emptyList()
  )
}
