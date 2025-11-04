package org.niis.harmony.buildlogic.internal

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object Mappers {

  val json: ObjectMapper by lazy {
    ObjectMapper(JsonFactory()).registerKotlinModule()
  }

  val yaml: ObjectMapper by lazy {
    ObjectMapper(YAMLFactory()).registerKotlinModule()
  }
}
