package org.niis.harmony.buildlogic.internal.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtensionsTest {

  @Test
  fun `titleCase capitalizes first character`() {
    assertEquals("Hello", "hello".titleCase())
    assertEquals("World", "world".titleCase())
    assertEquals("A", "a".titleCase())
  }

  @Test
  fun `titleCase preserves already capitalized strings`() {
    assertEquals("Hello", "Hello".titleCase())
    assertEquals("WORLD", "WORLD".titleCase())
  }

  @Test
  fun `titleCase handles empty and single character strings`() {
    assertEquals("", "".titleCase())
    assertEquals("A", "a".titleCase())
    assertEquals("Z", "z".titleCase())
  }

  @Test
  fun `sanitize replaces invalid characters with underscores`() {
    assertEquals("hello_world", "hello world".sanitize())
    assertEquals("hello_world", "hello@world".sanitize())
    assertEquals("hello_world", "hello/world".sanitize())
    assertEquals("hello_world", "hello\\world".sanitize())
  }

  @Test
  fun `sanitize preserves valid characters`() {
    assertEquals("hello-world", "hello-world".sanitize())
    assertEquals("hello_world", "hello_world".sanitize())
    assertEquals("hello.world", "hello.world".sanitize())
    assertEquals("hello123", "hello123".sanitize())
    assertEquals("HELLO", "HELLO".sanitize())
  }

  @Test
  fun `sanitize handles multiple consecutive invalid characters`() {
    assertEquals("hello___world", "hello   world".sanitize())
    assertEquals("hello___world", "hello!!!world".sanitize())
  }

  @Test
  fun `toTaskName combines sanitize and titleCase`() {
    assertEquals("Hello_world", "hello world".toTaskName())
    assertEquals("My_task_name", "my@task@name".toTaskName())
    assertEquals("Foo_bar", "foo/bar".toTaskName())
  }

  @Test
  fun `parseCsvList splits by comma`() {
    assertEquals(listOf("a", "b", "c"), "a,b,c".parseCsvList())
    assertEquals(listOf("foo", "bar", "baz"), "foo, bar, baz".parseCsvList())
  }

  @Test
  fun `parseCsvList splits by whitespace`() {
    assertEquals(listOf("a", "b", "c"), "a b c".parseCsvList())
    assertEquals(listOf("foo", "bar", "baz"), "foo  bar  baz".parseCsvList())
  }

  @Test
  fun `parseCsvList handles mixed comma and whitespace`() {
    assertEquals(listOf("a", "b", "c"), "a, b, c".parseCsvList())
    assertEquals(listOf("foo", "bar", "baz"), "foo , bar , baz".parseCsvList())
    assertEquals(listOf("x", "y", "z"), "x  ,  y  ,  z".parseCsvList())
  }

  @Test
  fun `parseCsvList filters empty strings`() {
    assertEquals(emptyList(), "".parseCsvList())
    assertEquals(emptyList(), "   ".parseCsvList())
    assertEquals(emptyList(), ", , ,".parseCsvList())
    assertEquals(listOf("a", "b"), "a, , b".parseCsvList())
  }

  @Test
  fun `parseCsvList trims individual values`() {
    assertEquals(listOf("a", "b", "c"), "  a  ,  b  ,  c  ".parseCsvList())
    assertEquals(listOf("foo", "bar"), " foo   bar ".parseCsvList())
  }
}
