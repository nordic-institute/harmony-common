package org.niis.harmony.buildlogic.internal

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

object Mappers {

  val json: ObjectMapper by lazy {
    JsonMapper.builder()
      .addModule(kotlinModule())
      .build()
  }

  val yaml: ObjectMapper by lazy {
    YAMLMapper.builder()
      .addModule(kotlinModule())
      .build()
  }
}
