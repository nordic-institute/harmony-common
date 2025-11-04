package org.niis.harmony.buildlogic.tasks.inputs

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class AliasedClasspathInput @Inject constructor() {
  @get:Input
  abstract val alias: Property<String>

  @get:Classpath
  abstract val file: RegularFileProperty
}
