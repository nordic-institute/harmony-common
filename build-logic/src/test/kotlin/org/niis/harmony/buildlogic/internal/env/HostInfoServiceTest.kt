package org.niis.harmony.buildlogic.internal.env

import kotlin.test.Test
import kotlin.test.assertEquals

class HostInfoServiceTest {

  @Test
  fun `detects macOS from various OS names`() {
    assertEquals(HostOs.MAC, HostInfoService.detectOs("Mac OS X"))
    assertEquals(HostOs.MAC, HostInfoService.detectOs("macOS"))
    assertEquals(HostOs.MAC, HostInfoService.detectOs("darwin"))
    assertEquals(HostOs.MAC, HostInfoService.detectOs("Darwin"))
  }

  @Test
  fun `detects Windows from various OS names`() {
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("Windows 10"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("Windows 11"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("win"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("MSYS_NT-10.0"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("MINGW64_NT-10.0"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("CYGWIN_NT-10.0"))
  }

  @Test
  fun `detects Linux from various OS names`() {
    assertEquals(HostOs.LINUX, HostInfoService.detectOs("Linux"))
    assertEquals(HostOs.LINUX, HostInfoService.detectOs("linux"))
    assertEquals(HostOs.LINUX, HostInfoService.detectOs("GNU/Linux"))
  }

  @Test
  fun `returns UNKNOWN for unrecognized OS names`() {
    assertEquals(HostOs.UNKNOWN, HostInfoService.detectOs("FreeBSD"))
    assertEquals(HostOs.UNKNOWN, HostInfoService.detectOs("SunOS"))
    assertEquals(HostOs.UNKNOWN, HostInfoService.detectOs(""))
    assertEquals(HostOs.UNKNOWN, HostInfoService.detectOs("some-weird-os"))
  }

  @Test
  fun `detection is case insensitive`() {
    assertEquals(HostOs.MAC, HostInfoService.detectOs("DARWIN"))
    assertEquals(HostOs.WINDOWS, HostInfoService.detectOs("WINDOWS"))
    assertEquals(HostOs.LINUX, HostInfoService.detectOs("LINUX"))
  }
}
