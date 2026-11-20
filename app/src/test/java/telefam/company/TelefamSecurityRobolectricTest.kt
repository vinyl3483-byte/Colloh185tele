package telefam.company

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import telefam.company.data.model.CountryCatalog
import telefam.company.data.network.TelefamApiService
import telefam.company.security.EncryptedTokenVault
import telefam.company.security.SecurityIntegrityGuard

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TelefamSecurityRobolectricTest {

  @Test
  fun `encrypted token vault uses v2 AAD and rejects wrong AAD`() {
    val secretToken = "tf_live_access_token_sample_9911"
    val encrypted = EncryptedTokenVault.encrypt(secretToken)
    assertTrue(encrypted.startsWith("v2:"))

    val decrypted = EncryptedTokenVault.decrypt(encrypted)
    assertEquals(secretToken, decrypted)

    val wrongAadResult = EncryptedTokenVault.decrypt(encrypted, aad = "tampered:aad")
    assertEquals("", wrongAadResult)
  }

  @Test
  fun `base url policy enforces https for non-loopback in release mode`() {
    assertTrue(TelefamApiService.isAllowedBaseUrl("https://api.example.com", enforceHttps = true))
    assertTrue(TelefamApiService.isAllowedBaseUrl("http://127.0.0.1:8080", enforceHttps = true))
    assertTrue(TelefamApiService.isAllowedBaseUrl("http://localhost:8080", enforceHttps = true))
    assertFalse(TelefamApiService.isAllowedBaseUrl("http://api.example.com", enforceHttps = true))
    assertFalse(TelefamApiService.isAllowedBaseUrl("ftp://api.example.com", enforceHttps = true))
    assertFalse(TelefamApiService.isAllowedBaseUrl("", enforceHttps = true))

    assertNull(TelefamApiService.create("http://api.example.com", enforceHttps = true))
    assertNotNull(TelefamApiService.create("https://api.example.com", enforceHttps = true))
  }

  @Test
  fun `security integrity guard recognizes robolectric runtime cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info = SecurityIntegrityGuard.inspect(context)
    assertTrue(SecurityIntegrityGuard.isRobolectricRuntime())
    assertFalse(info.isTampered)
    assertFalse(info.isDebuggerAttached)
    assertFalse(info.hasHookFramework)
    assertTrue(info.signatureDigest.isNotBlank())
  }

  @Test
  fun `country catalog loads dynamically without empty list`() {
    assertTrue(CountryCatalog.allCountries.size > 150)
    assertTrue(CountryCatalog.allCountries.any { it.iso == "US" && it.dialCode == "+1" })
    assertTrue(CountryCatalog.allCountries.any { it.iso == "KE" && it.dialCode == "+254" })
  }
}
