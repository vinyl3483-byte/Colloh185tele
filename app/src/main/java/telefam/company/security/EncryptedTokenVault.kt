package telefam.company.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM vault for encrypting
 * short-lived access tokens and long-lived rotating refresh tokens at rest.
 */
object EncryptedTokenVault {
  private const val KEY_ALIAS = "telefam_token_aes_gcm_v1"
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_BITS = 128
  const val DEFAULT_VAULT_AAD = "com.telefam.company.app:token_vault:v2"

  fun encrypt(plaintext: String, aad: String = DEFAULT_VAULT_AAD): String {
    if (plaintext.isBlank()) return ""
    return runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
      cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
      val iv = cipher.iv
      val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
      val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
      val ctB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
      "v2:$ivB64:$ctB64"
    }.getOrElse { "" }
  }

  fun decrypt(encoded: String?, aad: String = DEFAULT_VAULT_AAD): String {
    if (encoded.isNullOrBlank()) return ""
    val isV2 = encoded.startsWith("v2:")
    val isV1 = encoded.startsWith("v1:")
    if (!isV2 && !isV1) return ""
    return runCatching {
      val parts = encoded.split(":", limit = 3)
      if (parts.size != 3) return ""
      val iv = Base64.decode(parts[1], Base64.NO_WRAP)
      val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
      if (isV2) {
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
      }
      String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrElse { "" }
  }

  fun sha256Hex(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  @Volatile
  private var jvmTestFallbackKey: SecretKey? = null

  private fun getOrCreateSecretKey(): SecretKey {
    return runCatching {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
      if (existing != null) return@runCatching existing

      val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val spec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setRandomizedEncryptionRequired(true)
        .build()
      keyGen.init(spec)
      keyGen.generateKey()
    }.getOrElse {
      jvmTestFallbackKey ?: synchronized(this) {
        jvmTestFallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
          .also { jvmTestFallbackKey = it }
      }
    }
  }
}
