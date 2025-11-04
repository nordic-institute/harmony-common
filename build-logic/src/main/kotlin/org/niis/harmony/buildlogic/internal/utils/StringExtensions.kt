package org.niis.harmony.buildlogic.internal.utils

import java.util.Locale

fun String.titleCase(): String =
  this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
  }

fun String.sanitize(): String =
  this.replace(Regex("[^A-Za-z0-9._-]"), "_")

fun String.toTaskName(): String =
  this.sanitize().titleCase()

fun String.parseCsvList(): List<String> =
  this.split(Regex("[,\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
