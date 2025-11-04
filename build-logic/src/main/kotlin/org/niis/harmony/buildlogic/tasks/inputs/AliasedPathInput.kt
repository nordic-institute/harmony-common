package org.niis.harmony.buildlogic.tasks.inputs

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import javax.inject.Inject

abstract class AliasedPathInput @Inject constructor() {
  @get:Input
  abstract val alias: Property<String>

  @get:Input
  abstract val relBasePath: Property<String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputs: ConfigurableFileCollection
}
