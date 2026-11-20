package telefam.company

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import telefam.company.core.NetworkTier
import telefam.company.core.TelefamCoreEngine

class TelefamCoreEngineTest {
  @Test
  fun `escalating cooldown follows 1m x3 then 15m then 1h then 1d`() {
    assertEquals(60L, TelefamCoreEngine.cooldownSecondsForSendCount(1))
    assertEquals(60L, TelefamCoreEngine.cooldownSecondsForSendCount(2))
    assertEquals(60L, TelefamCoreEngine.cooldownSecondsForSendCount(3))
    assertEquals(900L, TelefamCoreEngine.cooldownSecondsForSendCount(4))
    assertEquals(3600L, TelefamCoreEngine.cooldownSecondsForSendCount(5))
    assertEquals(86400L, TelefamCoreEngine.cooldownSecondsForSendCount(6))
    assertEquals(86400L, TelefamCoreEngine.cooldownSecondsForSendCount(7))
  }

  @Test
  fun `device and ip fingerprints are deterministic sha256 hashes`() {
    val fp1 = TelefamCoreEngine.computeDeviceFingerprint("android", "Pixel 9", "16", "install-abc")
    val fp2 = TelefamCoreEngine.computeDeviceFingerprint("android", "Pixel 9", "16", "install-abc")
    val fp3 = TelefamCoreEngine.computeDeviceFingerprint("android", "Pixel 9", "16", "install-xyz")
    assertEquals(64, fp1.length)
    assertEquals(fp1, fp2)
    assertNotEquals(fp1, fp3)

    val ip1 = TelefamCoreEngine.computeIpFingerprint("192.168.1.42")
    val ip2 = TelefamCoreEngine.computeIpFingerprint("10.0.0.5")
    assertEquals(64, ip1.length)
    assertNotEquals(ip1, ip2)
  }

  @Test
  fun `server error sanitizer hides internal details`() {
    val msg500 = TelefamCoreEngine.sanitizeServerError(500, "SQLITE_CONSTRAINT_UNIQUE rate_limit_hits")
    val msg429 = TelefamCoreEngine.sanitizeServerError(429, "IP fingerprint quota exceeded")
    assertFalse(msg500.contains("SQLITE"))
    assertFalse(msg429.contains("IP"))
    assertFalse(msg429.contains("fingerprint"))
    assertEquals("An error has occurred. Please try again.", msg500)
    assertEquals("An error has occurred. Please try again later.", msg429)
  }

  @Test
  fun `weak network policy limits batch size and increases backoff`() {
    assertEquals(0, TelefamCoreEngine.maxBatchSizeForNetwork(NetworkTier.OFFLINE))
    assertEquals(3, TelefamCoreEngine.maxBatchSizeForNetwork(NetworkTier.WEAK))
    assertEquals(20, TelefamCoreEngine.maxBatchSizeForNetwork(NetworkTier.GOOD))
    assertEquals(5000L, TelefamCoreEngine.computeBackoffMs(1, NetworkTier.WEAK))
    assertEquals(2000L, TelefamCoreEngine.computeBackoffMs(1, NetworkTier.GOOD))
  }
}
