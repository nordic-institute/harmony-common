package org.niis.harmony.buildlogic.tasks.inputs

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import javax.inject.Inject

abstract class AliasedFileInput @Inject constructor() {
  @get:Input
  abstract val alias: Property<String>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val file: RegularFileProperty
}
