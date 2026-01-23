package org.niis.harmony.buildlogic.internal.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessRunnerTest {

  @Test
  fun `captures stdout from successful process`() {
    val result = ProcessRunner.execute(listOf("bash", "-lc", "printf 'hello'"))

    assertTrue(result.isSuccess)
    assertEquals("hello", result.stdout.trim())
    assertEquals("", result.stderr)
    assertEquals(0, result.exitCode)
    assertFalse(result.timedOut)
  }

  @Test
  fun `propagates non-zero exit codes`() {
    val result = ProcessRunner.execute(listOf("bash", "-lc", "echo error >&2; exit 42"))

    assertFalse(result.isSuccess)
    assertEquals(42, result.exitCode)
    assertTrue(result.stderr.contains("error"))
    assertFalse(result.timedOut)
  }

  @Test
  fun `marks process as timed out`() {
    val result = ProcessRunner.execute(
      command = listOf("bash", "-lc", "sleep 2"),
      timeoutSeconds = 1
    )

    assertTrue(result.timedOut)
    assertEquals(124, result.exitCode)
    assertFalse(result.isSuccess)
  }
}
