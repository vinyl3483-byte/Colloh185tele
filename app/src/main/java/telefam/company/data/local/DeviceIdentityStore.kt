package telefam.company.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import telefam.company.security.EncryptedTokenVault
import telefam.company.ui.theme.ThemeMode
import java.util.UUID

private val Context.telefamPrefs by preferencesDataStore(name = "telefam_company_prefs")

data class StoredSession(
  val userId: String?,
  val accessToken: String?,
  val refreshToken: String?,
  val accessExpiresAtMs: Long,
  val refreshExpiresAtMs: Long,
  val email: String?,
  val pendingOtpEmail: String?,
  val profileCompleted: Boolean,
  val themeMode: ThemeMode,
)

class DeviceIdentityStore(private val context: Context) {
  private val keyInstallId = stringPreferencesKey("install_id")
  private val keyUserId = stringPreferencesKey("user_id")
  private val keyAccessTokenEnc = stringPreferencesKey("access_token_enc")
  private val keyRefreshTokenEnc = stringPreferencesKey("refresh_token_enc")
  private val keyAccessExpiresAtMs = longPreferencesKey("access_expires_at_ms")
  private val keyRefreshExpiresAtMs = longPreferencesKey("refresh_expires_at_ms")
  private val keySessionEmail = stringPreferencesKey("session_email")
  private val keyPendingOtpEmail = stringPreferencesKey("pending_otp_email")
  private val keyProfileCompleted = booleanPreferencesKey("profile_completed")
  private val keyThemeMode = stringPreferencesKey("theme_mode")
  private val keyPendingDeviceCtxEnc = stringPreferencesKey("pending_device_ctx_enc")
  private val keyLastSyncedCountryIso = stringPreferencesKey("last_synced_country_iso")

  suspend fun getOrCreateInstallId(): String {
    val existing = context.telefamPrefs.data.first()[keyInstallId]
    if (!existing.isNullOrBlank()) return existing
    val generated = UUID.randomUUID().toString()
    context.telefamPrefs.edit { prefs ->
      prefs[keyInstallId] = generated
    }
    return generated
  }

  val sessionFlow: Flow<StoredSession> = context.telefamPrefs.data.map { prefs ->
    val modeName = prefs[keyThemeMode] ?: ThemeMode.SYSTEM.name
    val theme = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.SYSTEM)
    StoredSession(
      userId = prefs[keyUserId],
      accessToken = EncryptedTokenVault.decrypt(prefs[keyAccessTokenEnc]).ifBlank { null },
      refreshToken = EncryptedTokenVault.decrypt(prefs[keyRefreshTokenEnc]).ifBlank { null },
      accessExpiresAtMs = prefs[keyAccessExpiresAtMs] ?: 0L,
      refreshExpiresAtMs = prefs[keyRefreshExpiresAtMs] ?: 0L,
      email = prefs[keySessionEmail],
      pendingOtpEmail = prefs[keyPendingOtpEmail],
      profileCompleted = prefs[keyProfileCompleted] ?: false,
      themeMode = theme,
    )
  }

  suspend fun savePendingOtpEmail(email: String?) {
    context.telefamPrefs.edit { prefs ->
      if (email.isNullOrBlank()) {
        prefs.remove(keyPendingOtpEmail)
      } else {
        prefs[keyPendingOtpEmail] = email
      }
    }
  }

  suspend fun savePendingDeviceContextJson(json: String?) {
    context.telefamPrefs.edit { prefs ->
      if (json.isNullOrBlank()) {
        prefs.remove(keyPendingDeviceCtxEnc)
      } else {
        prefs[keyPendingDeviceCtxEnc] = EncryptedTokenVault.encrypt(json)
      }
    }
  }

  suspend fun getPendingDeviceContextJson(): String? {
    val enc = context.telefamPrefs.data.first()[keyPendingDeviceCtxEnc] ?: return null
    return EncryptedTokenVault.decrypt(enc).ifBlank { null }
  }

  suspend fun saveLastSyncedCountryIso(countryIso: String) {
    val clean = countryIso.trim().uppercase()
    if (clean.length != 2) return
    context.telefamPrefs.edit { prefs ->
      prefs[keyLastSyncedCountryIso] = clean
    }
  }

  suspend fun getLastSyncedCountryIso(): String {
    return context.telefamPrefs.data.first()[keyLastSyncedCountryIso].orEmpty()
  }

  suspend fun saveEncryptedSyncPrefsJson(userId: String, json: String) {
    val cleanUid = userId.trim().ifBlank { return }
    val key = stringPreferencesKey("sync_prefs_enc_$cleanUid")
    val enc = EncryptedTokenVault.encrypt(json)
    context.telefamPrefs.edit { prefs ->
      prefs[key] = enc
    }
  }

  suspend fun getEncryptedSyncPrefsJson(userId: String): String? {
    val cleanUid = userId.trim().ifBlank { return null }
    val key = stringPreferencesKey("sync_prefs_enc_$cleanUid")
    val enc = context.telefamPrefs.data.first()[key] ?: return null
    return EncryptedTokenVault.decrypt(enc).ifBlank { null }
  }

  suspend fun saveEncryptedSession(
    userId: String,
    email: String,
    accessToken: String,
    refreshToken: String,
    accessExpiresInSec: Long,
    refreshExpiresInSec: Long,
    profileCompleted: Boolean,
  ) {
    val nowMs = System.currentTimeMillis()
    val encAccess = EncryptedTokenVault.encrypt(accessToken)
    val encRefresh = EncryptedTokenVault.encrypt(refreshToken)
    context.telefamPrefs.edit { prefs ->
      prefs[keyUserId] = userId
      prefs[keySessionEmail] = email
      prefs[keyAccessTokenEnc] = encAccess
      prefs[keyRefreshTokenEnc] = encRefresh
      prefs[keyAccessExpiresAtMs] = nowMs + (accessExpiresInSec * 1000L)
      prefs[keyRefreshExpiresAtMs] = nowMs + (refreshExpiresInSec * 1000L)
      prefs[keyProfileCompleted] = profileCompleted
      prefs.remove(keyPendingOtpEmail)
    }
  }

  suspend fun markProfileCompleted(completed: Boolean) {
    context.telefamPrefs.edit { prefs ->
      prefs[keyProfileCompleted] = completed
    }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.telefamPrefs.edit { prefs ->
      prefs[keyThemeMode] = mode.name
    }
  }

  suspend fun clearSession() {
    context.telefamPrefs.edit { prefs ->
      prefs.remove(keyUserId)
      prefs.remove(keyAccessTokenEnc)
      prefs.remove(keyRefreshTokenEnc)
      prefs.remove(keyAccessExpiresAtMs)
      prefs.remove(keyRefreshExpiresAtMs)
      prefs.remove(keySessionEmail)
      prefs.remove(keyPendingOtpEmail)
      prefs.remove(keyProfileCompleted)
    }
  }

  suspend fun clearAll() {
    context.telefamPrefs.edit { prefs ->
      prefs.clear()
    }
  }
}
