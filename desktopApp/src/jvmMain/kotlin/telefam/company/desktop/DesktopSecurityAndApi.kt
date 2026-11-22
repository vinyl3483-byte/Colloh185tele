package telefam.company.desktop

import telefam.company.shared.ContactFilterTab
import telefam.company.shared.SharedFeedAction
import telefam.company.shared.SharedFeedTab
import telefam.company.shared.SharedHashtagItem
import telefam.company.shared.SharedPostFont
import telefam.company.shared.SharedPostMode
import telefam.company.shared.SharedSongTrack
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object DesktopSecurityGuard {
  fun isDebuggerAttached(): Boolean {
    return runCatching {
      val args = ManagementFactory.getRuntimeMXBean().inputArguments
      args.any { arg ->
        arg.contains("jdwp", ignoreCase = true) ||
          arg.contains("-Xdebug", ignoreCase = true) ||
          arg.contains("yjpagent", ignoreCase = true)
      }
    }.getOrDefault(false)
  }

  fun isAllowedServerUrl(rawUrl: String, enforceHttpsForRemote: Boolean = true): Boolean {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return false
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    val host = uri.host?.lowercase() ?: return false
    if (scheme == "https") return true
    if (scheme == "http") {
      val isLoopback = host == "127.0.0.1" || host == "localhost" || host == "::1"
      return !enforceHttpsForRemote || isLoopback
    }
    return false
  }

  fun sha256Hex(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun sha256HexBytes(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }
}

data class DesktopCountryDialOption(
  val iso: String,
  val name: String,
  val dialCode: String,
) {
  val flagEmoji: String
    get() = iso.uppercase().map { ch ->
      val codePoint = 0x1F1E6 + (ch.code - 'A'.code)
      String(Character.toChars(codePoint))
    }.joinToString("")
}

object DesktopCountryCatalog {
  val defaultCountry = DesktopCountryDialOption("KE", "Kenya", "+254")
  val allCountries: List<DesktopCountryDialOption> = listOf(
    DesktopCountryDialOption("AF", "Afghanistan", "+93"),
    DesktopCountryDialOption("AL", "Albania", "+355"),
    DesktopCountryDialOption("DZ", "Algeria", "+213"),
    DesktopCountryDialOption("AR", "Argentina", "+54"),
    DesktopCountryDialOption("AU", "Australia", "+61"),
    DesktopCountryDialOption("AT", "Austria", "+43"),
    DesktopCountryDialOption("BD", "Bangladesh", "+880"),
    DesktopCountryDialOption("BE", "Belgium", "+32"),
    DesktopCountryDialOption("BJ", "Benin", "+229"),
    DesktopCountryDialOption("BR", "Brazil", "+55"),
    DesktopCountryDialOption("BF", "Burkina Faso", "+226"),
    DesktopCountryDialOption("BI", "Burundi", "+257"),
    DesktopCountryDialOption("CM", "Cameroon", "+237"),
    DesktopCountryDialOption("CA", "Canada", "+1"),
    DesktopCountryDialOption("TD", "Chad", "+235"),
    DesktopCountryDialOption("CL", "Chile", "+56"),
    DesktopCountryDialOption("CN", "China", "+86"),
    DesktopCountryDialOption("CO", "Colombia", "+57"),
    DesktopCountryDialOption("CG", "Congo", "+242"),
    DesktopCountryDialOption("CD", "Congo (DRC)", "+243"),
    DesktopCountryDialOption("CI", "Cote d'Ivoire", "+225"),
    DesktopCountryDialOption("DK", "Denmark", "+45"),
    DesktopCountryDialOption("DJ", "Djibouti", "+253"),
    DesktopCountryDialOption("EG", "Egypt", "+20"),
    DesktopCountryDialOption("ET", "Ethiopia", "+251"),
    DesktopCountryDialOption("FI", "Finland", "+358"),
    DesktopCountryDialOption("FR", "France", "+33"),
    DesktopCountryDialOption("GA", "Gabon", "+241"),
    DesktopCountryDialOption("GM", "Gambia", "+220"),
    DesktopCountryDialOption("DE", "Germany", "+49"),
    DesktopCountryDialOption("GH", "Ghana", "+233"),
    DesktopCountryDialOption("GR", "Greece", "+30"),
    DesktopCountryDialOption("GN", "Guinea", "+224"),
    DesktopCountryDialOption("HT", "Haiti", "+509"),
    DesktopCountryDialOption("IN", "India", "+91"),
    DesktopCountryDialOption("ID", "Indonesia", "+62"),
    DesktopCountryDialOption("IE", "Ireland", "+353"),
    DesktopCountryDialOption("IL", "Israel", "+972"),
    DesktopCountryDialOption("IT", "Italy", "+39"),
    DesktopCountryDialOption("JM", "Jamaica", "+1876"),
    DesktopCountryDialOption("JP", "Japan", "+81"),
    DesktopCountryDialOption("JO", "Jordan", "+962"),
    DesktopCountryDialOption("KE", "Kenya", "+254"),
    DesktopCountryDialOption("KW", "Kuwait", "+965"),
    DesktopCountryDialOption("LB", "Lebanon", "+961"),
    DesktopCountryDialOption("LR", "Liberia", "+231"),
    DesktopCountryDialOption("LY", "Libya", "+218"),
    DesktopCountryDialOption("MG", "Madagascar", "+261"),
    DesktopCountryDialOption("MW", "Malawi", "+265"),
    DesktopCountryDialOption("MY", "Malaysia", "+60"),
    DesktopCountryDialOption("ML", "Mali", "+223"),
    DesktopCountryDialOption("MX", "Mexico", "+52"),
    DesktopCountryDialOption("MA", "Morocco", "+212"),
    DesktopCountryDialOption("MZ", "Mozambique", "+258"),
    DesktopCountryDialOption("NA", "Namibia", "+264"),
    DesktopCountryDialOption("NL", "Netherlands", "+31"),
    DesktopCountryDialOption("NZ", "New Zealand", "+64"),
    DesktopCountryDialOption("NE", "Niger", "+227"),
    DesktopCountryDialOption("NG", "Nigeria", "+234"),
    DesktopCountryDialOption("NO", "Norway", "+47"),
    DesktopCountryDialOption("PK", "Pakistan", "+92"),
    DesktopCountryDialOption("PE", "Peru", "+51"),
    DesktopCountryDialOption("PH", "Philippines", "+63"),
    DesktopCountryDialOption("PL", "Poland", "+48"),
    DesktopCountryDialOption("PT", "Portugal", "+351"),
    DesktopCountryDialOption("QA", "Qatar", "+974"),
    DesktopCountryDialOption("RO", "Romania", "+40"),
    DesktopCountryDialOption("RW", "Rwanda", "+250"),
    DesktopCountryDialOption("SA", "Saudi Arabia", "+966"),
    DesktopCountryDialOption("SN", "Senegal", "+221"),
    DesktopCountryDialOption("SL", "Sierra Leone", "+232"),
    DesktopCountryDialOption("SG", "Singapore", "+65"),
    DesktopCountryDialOption("SO", "Somalia", "+252"),
    DesktopCountryDialOption("ZA", "South Africa", "+27"),
    DesktopCountryDialOption("KR", "South Korea", "+82"),
    DesktopCountryDialOption("SS", "South Sudan", "+211"),
    DesktopCountryDialOption("ES", "Spain", "+34"),
    DesktopCountryDialOption("SD", "Sudan", "+249"),
    DesktopCountryDialOption("SE", "Sweden", "+46"),
    DesktopCountryDialOption("CH", "Switzerland", "+41"),
    DesktopCountryDialOption("TZ", "Tanzania", "+255"),
    DesktopCountryDialOption("TH", "Thailand", "+66"),
    DesktopCountryDialOption("TG", "Togo", "+228"),
    DesktopCountryDialOption("TN", "Tunisia", "+216"),
    DesktopCountryDialOption("TR", "Turkey", "+90"),
    DesktopCountryDialOption("UG", "Uganda", "+256"),
    DesktopCountryDialOption("UA", "Ukraine", "+380"),
    DesktopCountryDialOption("AE", "United Arab Emirates", "+971"),
    DesktopCountryDialOption("GB", "United Kingdom", "+44"),
    DesktopCountryDialOption("US", "United States", "+1"),
    DesktopCountryDialOption("ZM", "Zambia", "+260"),
    DesktopCountryDialOption("ZW", "Zimbabwe", "+263"),
  )

  fun isAtLeast13(isoDob: String): Boolean {
    val parsed = runCatching { LocalDate.parse(isoDob.trim()) }.getOrNull() ?: return false
    val today = LocalDate.now()
    if (parsed.isAfter(today)) return false
    return Period.between(parsed, today).years >= 13
  }
}

data class DesktopStoredSession(
  val installId: String,
  val serverBaseUrl: String,
  val userId: String = "",
  val accessToken: String = "",
  val refreshToken: String = "",
  val email: String = "",
  val suggestedFullName: String = "",
  val suggestedUsername: String = "",
  val profileCompleted: Boolean = false,
  val fullName: String = "",
  val username: String = "",
  val bio: String = "",
  val location: String = "",
  val themeMode: DesktopThemeMode = DesktopThemeMode.SYSTEM,
) {
  val isAuthenticated: Boolean
    get() = accessToken.isNotBlank() && !DesktopSecurityGuard.isDebuggerAttached()
}

class DesktopEncryptedSessionVault {
  private val homeDir = File(System.getProperty("user.home", "."), ".telefam").apply { mkdirs() }
  private val sessionFile = File(homeDir, "session.enc")
  private val keyFile = File(homeDir, "vault.key")
  private val aad = "telefam.company.desktop:vault:v2".toByteArray(StandardCharsets.UTF_8)

  private val deviceIdFile = File(homeDir, "device.id")

  fun getOrCreatePersistentInstallId(): String {
    val existing = if (deviceIdFile.exists()) {
      runCatching { deviceIdFile.readText(StandardCharsets.UTF_8).trim() }.getOrDefault("")
    } else ""
    if (existing.isNotBlank()) return existing
    val created = UUID.randomUUID().toString()
    runCatching {
      deviceIdFile.writeText(created, StandardCharsets.UTF_8)
      setOwnerOnlyPermissions(deviceIdFile)
    }
    return created
  }

  private fun setOwnerOnlyPermissions(file: File) {
    runCatching {
      Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
    }
  }

  private fun getOrCreateKey(): SecretKey {
    val keyBytes = if (keyFile.exists() && keyFile.length() == 32L) {
      keyFile.readBytes()
    } else {
      ByteArray(32).also { buf ->
        SecureRandom().nextBytes(buf)
        keyFile.writeBytes(buf)
        setOwnerOnlyPermissions(keyFile)
      }
    }
    val derived = MessageDigest.getInstance("SHA-256").digest(
      keyBytes + (System.getProperty("user.name", "desktop") + ":telefam").toByteArray(StandardCharsets.UTF_8),
    )
    return SecretKeySpec(derived, "AES")
  }

  fun load(): DesktopStoredSession {
    val envUrl = (System.getProperty("TELEFAM_API_BASE_URL")
      ?: System.getenv("TELEFAM_API_BASE_URL")
      ?: "")
      .trim()
    val persistentInstallId = getOrCreatePersistentInstallId()

    if (!sessionFile.exists() || DesktopSecurityGuard.isDebuggerAttached()) {
      return DesktopStoredSession(
        installId = persistentInstallId,
        serverBaseUrl = envUrl,
      )
    }

    return runCatching {
      val raw = sessionFile.readText(StandardCharsets.UTF_8).trim()
      val parts = raw.split(":", limit = 3)
      require(parts.size == 3 && parts[0] == "v2")
      val iv = Base64.getDecoder().decode(parts[1])
      val ct = Base64.getDecoder().decode(parts[2])
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
      cipher.updateAAD(aad)
      val json = String(cipher.doFinal(ct), StandardCharsets.UTF_8)
      val map = DesktopJson.parseObject(json)
      val savedUrl = DesktopJson.str(map, "server_base_url").ifBlank { envUrl }
      val mode = runCatching {
        DesktopThemeMode.valueOf(DesktopJson.str(map, "theme_mode").ifBlank { "SYSTEM" })
      }.getOrDefault(DesktopThemeMode.SYSTEM)
      val resolvedInstallId = DesktopJson.str(map, "install_id").ifBlank { persistentInstallId }
      if (!deviceIdFile.exists() && resolvedInstallId.isNotBlank()) {
        runCatching {
          deviceIdFile.writeText(resolvedInstallId, StandardCharsets.UTF_8)
          setOwnerOnlyPermissions(deviceIdFile)
        }
      }

      DesktopStoredSession(
        installId = resolvedInstallId,
        serverBaseUrl = if (envUrl.isNotBlank()) envUrl else savedUrl,
        userId = DesktopJson.str(map, "user_id"),
        accessToken = DesktopJson.str(map, "access_token"),
        refreshToken = DesktopJson.str(map, "refresh_token"),
        email = DesktopJson.str(map, "email"),
        suggestedFullName = DesktopJson.str(map, "suggested_full_name"),
        suggestedUsername = DesktopJson.str(map, "suggested_username"),
        profileCompleted = DesktopJson.bool(map, "profile_completed"),
        fullName = DesktopJson.str(map, "full_name"),
        username = DesktopJson.str(map, "username"),
        bio = DesktopJson.str(map, "bio"),
        location = DesktopJson.str(map, "location"),
        themeMode = mode,
      )
    }.getOrElse {
      DesktopStoredSession(
        installId = persistentInstallId,
        serverBaseUrl = envUrl,
      )
    }
  }

  fun save(session: DesktopStoredSession) {
    runCatching {
      val obj = buildString {
        append('{')
        append("\"install_id\":\"").append(DesktopJson.escape(session.installId)).append("\",")
        append("\"server_base_url\":\"").append(DesktopJson.escape(session.serverBaseUrl)).append("\",")
        append("\"user_id\":\"").append(DesktopJson.escape(session.userId)).append("\",")
        append("\"access_token\":\"").append(DesktopJson.escape(session.accessToken)).append("\",")
        append("\"refresh_token\":\"").append(DesktopJson.escape(session.refreshToken)).append("\",")
        append("\"email\":\"").append(DesktopJson.escape(session.email)).append("\",")
        append("\"suggested_full_name\":\"").append(DesktopJson.escape(session.suggestedFullName)).append("\",")
        append("\"suggested_username\":\"").append(DesktopJson.escape(session.suggestedUsername)).append("\",")
        append("\"profile_completed\":").append(session.profileCompleted).append(',')
        append("\"full_name\":\"").append(DesktopJson.escape(session.fullName)).append("\",")
        append("\"username\":\"").append(DesktopJson.escape(session.username)).append("\",")
        append("\"bio\":\"").append(DesktopJson.escape(session.bio)).append("\",")
        append("\"location\":\"").append(DesktopJson.escape(session.location)).append("\",")
        append("\"theme_mode\":\"").append(session.themeMode.name).append('"')
        append('}')
      }
      val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
      cipher.updateAAD(aad)
      val encrypted = cipher.doFinal(obj.toByteArray(StandardCharsets.UTF_8))
      val payload = "v2:${Base64.getEncoder().encodeToString(iv)}:${Base64.getEncoder().encodeToString(encrypted)}"
      sessionFile.writeText(payload, StandardCharsets.UTF_8)
      setOwnerOnlyPermissions(sessionFile)
    }
  }
}

data class DesktopEmailCheckResult(
  val exists: Boolean,
  val profileCompleted: Boolean,
  val requiresDeviceApproval: Boolean,
)

sealed interface DesktopApiOutcome<out T> {
  data class Ok<T>(val data: T) : DesktopApiOutcome<T>
  data class Fail(val statusCode: Int, val message: String) : DesktopApiOutcome<Nothing>
}

data class DesktopOtpRequestResult(
  val cooldownSeconds: Long,
  val requiresDeviceApproval: Boolean = false,
  val requestId: String = "",
  val pollToken: String = "",
  val targetDeviceName: String = "",
  val targetLocation: String = "",
  val requestTimeMs: Long = 0L,
  val expiresInSeconds: Long = 60L,
)

data class DesktopLoginRequestPollResult(
  val status: String,
  val blocked: Boolean,
  val remainingSeconds: Long,
  val targetDeviceName: String,
  val targetLocation: String,
  val authenticatedSession: DesktopStoredSession? = null,
)

data class DesktopMediaUploadResult(
  val mediaId: String,
  val contentId: String,
  val thumbnailMediaId: String,
  val perFrameHashes: List<String>,
)

data class DesktopContactsPage(
  val items: List<DesktopContactItem>,
  val page: Int,
  val hasMore: Boolean,
)

data class DesktopFeedPage(
  val items: List<DesktopFeedPost>,
  val page: Int,
  val hasMore: Boolean,
)

data class DesktopSearchShortcut(
  val key: String,
  val label: String,
  val count: Long,
)

data class DesktopFeedSearchPage(
  val shortcuts: List<DesktopSearchShortcut>,
  val items: List<DesktopFeedPost>,
  val hasMore: Boolean,
)

data class DesktopProfileBackgroundItem(
  val mediaId: String,
  val url: String,
  val sha256Fingerprint: String = "",
  val dHashFingerprint: String = "",
  val width: Int = 0,
  val height: Int = 0,
  val byteSize: Int = 0,
  val createdAtMs: Long = 0L,
)

data class DesktopUserProfile(
  val userId: String,
  val fullName: String,
  val username: String,
  val avatarUrl: String,
  val bio: String,
  val location: String,
  val countryIso: String,
  val gender: String = "",
  val dateOfBirth: String = "",
  val phoneNumber: String = "",
  val usernameChangedAtMs: Long = 0L,
  val nextUsernameChangeMs: Long = 0L,
  val backgrounds: List<DesktopProfileBackgroundItem> = emptyList(),
  val followingCount: Long,
  val followersCount: Long,
  val likesCount: Long,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val isOwner: Boolean,
  val isBlocked: Boolean,
  val isMuted: Boolean = false,
  val isPrivateRestricted: Boolean = false,
  val likesVisibility: String = "everyone",
  val followingVisibility: String = "everyone",
  val showActivityStatus: Boolean = true,
  val riskScore: Double,
  val posts: List<DesktopFeedPost>,
  val savedPosts: List<DesktopFeedPost> = emptyList(),
  val repostedPosts: List<DesktopFeedPost> = emptyList(),
  val lockedPosts: List<DesktopFeedPost> = emptyList(),
) {
  val relationship: telefam.company.shared.ContactRelationshipState
    get() = telefam.company.shared.ContactRelationshipState.compute(isFollowing, followsYou)
}

data class DesktopUserConnectionsPage(
  val items: List<DesktopContactItem>,
  val page: Int,
  val hasMore: Boolean,
)

data class DesktopLinkPreview(
  val normalizedUrl: String,
  val domain: String,
  val title: String = "",
  val description: String = "",
  val imageUrl: String = "",
  val faviconUrl: String = "",
  val status: String = "ready", // loading, ready, fallback, failed
  val fetchedAtMs: Long = 0L,
)

data class DesktopSavedLink(
  val id: String,
  val url: String,
  val normalizedUrl: String,
  val domain: String,
  val title: String = "",
  val description: String = "",
  val previewImageUrl: String = "",
  val faviconUrl: String = "",
  val source: String = "browser",
  val savedAtMs: Long = 0L,
)

class DesktopApiClient(
  private val vault: DesktopEncryptedSessionVault,
  initialSession: DesktopStoredSession,
  private val onSessionUpdated: (DesktopStoredSession) -> Unit,
) {
  @Volatile
  var session: DesktopStoredSession = initialSession
    private set

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(12))
    .build()

  val isConfigured: Boolean
    get() = DesktopSecurityGuard.isAllowedServerUrl(session.serverBaseUrl)

  val deviceFingerprint: String
    get() {
      val os = System.getProperty("os.name", "desktop")
      val arch = System.getProperty("os.arch", "x64")
      val dbg = if (DesktopSecurityGuard.isDebuggerAttached()) "1" else "0"
      return DesktopSecurityGuard.sha256Hex("desktop|$os|$arch|$dbg|${session.installId}")
    }

  val ipHint: String
    get() = DesktopSecurityGuard.sha256Hex("desktop_client|${session.installId}")

  private val cacheDir = File(System.getProperty("user.home", "."), ".telefam/cache").apply { mkdirs() }

  private fun cacheKeyFile(key: String): File {
    val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return File(cacheDir, safe)
  }

  private fun saveCachedText(key: String, raw: String) {
    runCatching {
      val f = cacheKeyFile(key)
      f.writeText(raw, StandardCharsets.UTF_8)
      Files.setPosixFilePermissions(f.toPath(), PosixFilePermissions.fromString("rw-------"))
    }
  }

  private fun loadCachedText(key: String): String? {
    val f = cacheKeyFile(key)
    return if (f.exists()) runCatching { f.readText(StandardCharsets.UTF_8) }.getOrNull() else null
  }

  fun resolveMediaUrl(pathOrUrl: String): String {
    val clean = pathOrUrl.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
    val base = baseUrl() ?: return clean
    return if (clean.startsWith("/")) "$base$clean" else "$base/$clean"
  }

  fun fetchCachedMediaFile(pathOrUrl: String): File? {
    val resolved = resolveMediaUrl(pathOrUrl)
    if (resolved.isBlank()) return null
    val key = "media_${DesktopSecurityGuard.sha256Hex(resolved)}.bin"
    val cachedFile = cacheKeyFile(key)
    if (cachedFile.exists() && cachedFile.length() > 0L) {
      return cachedFile
    }
    val uri = runCatching { URI(resolved) }.getOrNull() ?: return null
    val isTelefamHost = baseUrl()?.let { b ->
      val bh = runCatching { URI(b).host }.getOrNull()
      !bh.isNullOrBlank() && bh.equals(uri.host, ignoreCase = true)
    } ?: false
    val fetched = runCatching {
      val reqBuilder = HttpRequest.newBuilder(uri)
        .GET()
        .timeout(Duration.ofSeconds(25))
      if (isTelefamHost && session.accessToken.isNotBlank()) {
        reqBuilder.header("Authorization", "Bearer ${session.accessToken}")
        reqBuilder.header("X-Telefam-Device-FP", deviceFingerprint)
      }
      val res = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
      if (res.statusCode() in 200..299 && res.body().isNotEmpty()) {
        cachedFile.writeBytes(res.body())
        cachedFile
      } else null
    }.getOrNull()
    return fetched ?: cachedFile.takeIf { it.exists() && it.length() > 0L }
  }

  fun fetchCachedMediaBytes(pathOrUrl: String): ByteArray? =
    fetchCachedMediaFile(pathOrUrl)?.let { f -> runCatching { f.readBytes() }.getOrNull() }

  fun shareUrlForPost(postId: String): String {
    val clean = session.serverBaseUrl.trim().trimEnd('/')
    return if (DesktopSecurityGuard.isAllowedServerUrl(clean)) {
      "$clean/post/$postId"
    } else {
      "telefam://post/$postId"
    }
  }

  fun shareUrlForProfile(usernameOrId: String): String {
    val clean = session.serverBaseUrl.trim().trimEnd('/')
    val handle = usernameOrId.ifBlank { "member" }
    return if (DesktopSecurityGuard.isAllowedServerUrl(clean)) {
      "$clean/u/$handle"
    } else {
      "telefam://u/$handle"
    }
  }

  fun updateServerBaseUrl(newUrl: String) {
    val updated = session.copy(serverBaseUrl = newUrl.trim())
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
  }

  fun updateThemeMode(mode: DesktopThemeMode) {
    val updated = session.copy(themeMode = mode)
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
  }

  fun signOut(allDevices: Boolean = false) {
    val curToken = session.accessToken
    val prevUserId = session.userId
    if (curToken.isNotBlank()) {
      runCatching { unregisterPushToken(allDevices) }
      runCatching {
        executeRaw("POST", "/v1/auth/logout", "{\"all_devices\":$allDevices}".toByteArray(StandardCharsets.UTF_8), auth = true, allowRefreshRetry = false)
      }
    }
    runCatching {
      if (prevUserId.isNotBlank()) {
        cacheKeyFile("settings_$prevUserId.json").delete()
        cacheKeyFile("settings_outbox_$prevUserId.json").delete()
        cacheKeyFile("tickets_$prevUserId.json").delete()
        cacheKeyFile("restricted_blocked_$prevUserId.json").delete()
        cacheKeyFile("restricted_muted_$prevUserId.json").delete()
        cacheKeyFile("support_status_$prevUserId.json").delete()
        cacheKeyFile("saved_links_$prevUserId.json").delete()
        cacheKeyFile("notifications_$prevUserId.json").delete()
      }
      val voicesDir = File(System.getProperty("user.home", "."), ".telefam/voices")
      voicesDir.listFiles()?.forEach { f ->
        if (f.name.startsWith("dl_")) f.delete()
      }
    }
    val updated = session.copy(
      userId = "",
      accessToken = "",
      refreshToken = "",
      profileCompleted = false,
      suggestedFullName = "",
      suggestedUsername = "",
      fullName = "",
      username = "",
      bio = "",
      location = "",
    )
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
  }

  private fun baseUrl(): String? {
    val trimmed = session.serverBaseUrl.trim().trimEnd('/')
    return if (DesktopSecurityGuard.isAllowedServerUrl(trimmed)) trimmed else null
  }

  private fun applyAuthSessionResponse(map: Map<String, Any?>, fallbackEmail: String) {
    val email = DesktopJson.str(map, "email").ifBlank { fallbackEmail }
    val profileCompleted = DesktopJson.bool(map, "profile_completed")
    val uid = DesktopJson.str(map, "user_id").ifBlank { session.userId }
    val updated = session.copy(
      userId = uid,
      accessToken = DesktopJson.str(map, "session_token"),
      refreshToken = DesktopJson.str(map, "refresh_token"),
      email = email,
      suggestedFullName = DesktopJson.str(map, "suggested_full_name"),
      suggestedUsername = DesktopJson.str(map, "suggested_username"),
      profileCompleted = profileCompleted,
    )
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
    if (profileCompleted && uid.isNotBlank()) {
      hydrateExistingProfileAfterSignIn(uid)
    }
    captureAndSyncDeviceContext()
  }

  private fun hydrateExistingProfileAfterSignIn(userId: String) {
    val outcome = getUserProfile(userId)
    if (outcome is DesktopApiOutcome.Ok) {
      val prof = outcome.data
      val refreshed = session.copy(
        profileCompleted = true,
        fullName = prof.fullName,
        username = prof.username,
        bio = prof.bio,
        location = prof.location,
      )
      session = refreshed
      vault.save(refreshed)
      onSessionUpdated(refreshed)
    }
  }

  fun checkEmail(email: String): DesktopEmailCheckResult? {
    val clean = email.trim().lowercase()
    if (clean.isBlank()) return null
    val enc = URLEncoder.encode(clean, StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/auth/check-email?email=$enc", null, auth = false) ?: return null
    if (resp.first !in 200..299) return null
    val map = DesktopJson.parseObject(resp.second)
    if (!DesktopJson.bool(map, "success")) return null
    return DesktopEmailCheckResult(
      exists = DesktopJson.bool(map, "exists"),
      profileCompleted = DesktopJson.bool(map, "profile_completed"),
      requiresDeviceApproval = DesktopJson.bool(map, "requires_device_approval"),
    )
  }

  fun requestOtp(email: String): DesktopApiOutcome<DesktopOtpRequestResult> {
    val body = "{\"email\":\"${DesktopJson.escape(email.trim())}\"}"
    val resp = executeRaw("POST", "/v1/auth/otp/request", body.toByteArray(StandardCharsets.UTF_8), auth = false)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first == 403) {
      val msg = parseError(resp.first, resp.second).ifBlank {
        "This device was denied access for this account."
      }
      return DesktopApiOutcome.Fail(403, msg)
    }
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val requiresApproval = DesktopJson.bool(map, "requires_device_approval")
    return DesktopApiOutcome.Ok(
      DesktopOtpRequestResult(
        cooldownSeconds = DesktopJson.long(map, "cooldown_seconds", 60L),
        requiresDeviceApproval = requiresApproval,
        requestId = DesktopJson.str(map, "request_id"),
        pollToken = DesktopJson.str(map, "poll_token"),
        targetDeviceName = DesktopJson.str(map, "target_device_name"),
        targetLocation = DesktopJson.str(map, "target_location"),
        requestTimeMs = DesktopJson.long(map, "request_time_ms", System.currentTimeMillis()),
        expiresInSeconds = DesktopJson.long(map, "expires_in_seconds", 60L),
      ),
    )
  }

  fun pollLoginRequestStatus(requestId: String, pollToken: String): DesktopApiOutcome<DesktopLoginRequestPollResult> {
    val encId = URLEncoder.encode(requestId.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw(
      method = "GET",
      path = "/v1/auth/login-requests/$encId/status",
      body = null,
      auth = false,
      extraHeaders = mapOf("X-Telefam-Poll-Token" to pollToken),
    ) ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val status = DesktopJson.str(map, "status").ifBlank { "pending" }
    val blocked = DesktopJson.bool(map, "blocked")
    val sessionTok = DesktopJson.str(map, "session_token")
    if (status == "approved" && sessionTok.isNotBlank()) {
      applyAuthSessionResponse(map, DesktopJson.str(map, "email"))
    }
    return DesktopApiOutcome.Ok(
      DesktopLoginRequestPollResult(
        status = status,
        blocked = blocked,
        remainingSeconds = DesktopJson.long(map, "remaining_seconds", 0L),
        targetDeviceName = DesktopJson.str(map, "target_device_name"),
        targetLocation = DesktopJson.str(map, "target_location"),
        authenticatedSession = if (status == "approved" && sessionTok.isNotBlank()) session else null,
      ),
    )
  }

  fun getPendingLoginRequests(): DesktopApiOutcome<List<DesktopIncomingLoginRequestItem>> {
    if (!session.isAuthenticated) return DesktopApiOutcome.Fail(401, "Unauthorized")
    val resp = executeRaw("GET", "/v1/auth/login-requests/pending", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val list = DesktopJson.objList(map, "requests").mapNotNull { item ->
      val rid = DesktopJson.str(item, "request_id")
      if (rid.isBlank()) return@mapNotNull null
      DesktopIncomingLoginRequestItem(
        requestId = rid,
        deviceName = DesktopJson.str(item, "device_name"),
        location = DesktopJson.str(item, "location"),
        ipAddressMasked = DesktopJson.str(item, "ip_address_masked"),
        requestedAtMs = DesktopJson.long(item, "requested_at_ms", System.currentTimeMillis()),
        remainingSeconds = DesktopJson.long(item, "remaining_seconds", 60L),
      )
    }
    return DesktopApiOutcome.Ok(list)
  }

  fun respondToLoginRequest(requestId: String, accept: Boolean): DesktopApiOutcome<String> {
    val encId = URLEncoder.encode(requestId.trim(), StandardCharsets.UTF_8)
    val action = if (accept) "accept" else "deny"
    val body = "{\"action\":\"$action\"}"
    val resp = executeRaw("POST", "/v1/auth/login-requests/$encId/respond", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(DesktopJson.str(map, "status").ifBlank { if (accept) "approved" else "denied" })
  }

  fun captureAndSyncDeviceContext() {
    val snap = DesktopDeviceContextCollector.collectSnapshot()
    val payload = buildString {
      append('{')
      append("\"device_name\":\"").append(DesktopJson.escape(snap.deviceName)).append("\",")
      append("\"platform\":\"").append(DesktopJson.escape(snap.platform)).append("\",")
      append("\"hardware_model\":\"").append(DesktopJson.escape(snap.osVersion)).append("\",")
      append("\"carrier_country_iso\":\"\",")
      append("\"sim_country_iso\":\"\",")
      append("\"gps_country_iso\":\"\",")
      append("\"gps_city\":\"\",")
      append("\"locale_country_iso\":\"").append(DesktopJson.escape(snap.localeCountryIso)).append("\",")
      append("\"timezone\":\"").append(DesktopJson.escape(snap.timezoneId)).append("\",")
      append("\"lat_bucket\":0,\"lon_bucket\":0,")
      append("\"signal_source\":\"desktop_locale\",")
      append("\"captured_at_ms\":").append(snap.capturedAtMs)
      append('}')
    }
    saveCachedText("device_ctx_pending.json", payload)
    if (session.isAuthenticated) {
      flushPendingDeviceContext()
    }
  }

  fun flushPendingDeviceContext() {
    if (!session.isAuthenticated) return
    val pending = loadCachedText("device_ctx_pending.json") ?: return
    val resp = executeRaw("POST", "/v1/telemetry/device-context", pending.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      runCatching { cacheKeyFile("device_ctx_pending.json").delete() }
    }
  }

  fun verifyOtp(email: String, otp: String): DesktopApiOutcome<DesktopStoredSession> {
    val body = "{\"email\":\"${DesktopJson.escape(email.trim())}\",\"otp\":\"${DesktopJson.escape(otp.trim())}\"}"
    val resp = executeRaw("POST", "/v1/auth/otp/verify", body.toByteArray(StandardCharsets.UTF_8), auth = false)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    applyAuthSessionResponse(map, email.trim())
    return DesktopApiOutcome.Ok(session)
  }

  fun verifyLinkToken(token: String): DesktopApiOutcome<DesktopStoredSession> {
    val body = "{\"token\":\"${DesktopJson.escape(token.trim())}\"}"
    val resp = executeRaw("POST", "/v1/auth/verify-link", body.toByteArray(StandardCharsets.UTF_8), auth = false)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    applyAuthSessionResponse(map, session.email)
    return DesktopApiOutcome.Ok(session)
  }

  fun verifyGoogleOAuth(
    identityToken: String,
    expectedEmail: String = "",
    approvalRequestId: String = "",
  ): DesktopApiOutcome<DesktopStoredSession> {
    val body = buildString {
      append("{\"provider\":\"google\",\"identity_token\":\"").append(DesktopJson.escape(identityToken.trim())).append('"')
      if (expectedEmail.isNotBlank()) {
        append(",\"expected_email\":\"").append(DesktopJson.escape(expectedEmail.trim().lowercase())).append('"')
      }
      if (approvalRequestId.isNotBlank()) {
        append(",\"approval_request_id\":\"").append(DesktopJson.escape(approvalRequestId.trim())).append('"')
      }
      append('}')
    }
    val resp = executeRaw("POST", "/v1/auth/oauth/google", body.toByteArray(StandardCharsets.UTF_8), auth = false)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    applyAuthSessionResponse(map, expectedEmail.ifBlank { session.email })
    return DesktopApiOutcome.Ok(session)
  }

  fun checkUsernameAvailable(username: String): Boolean? {
    val clean = URLEncoder.encode(username.trim().lowercase(), StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/profile/check-username?u=$clean", null, auth = false) ?: return null
    if (resp.first !in 200..299) return null
    val map = DesktopJson.parseObject(resp.second)
    return DesktopJson.bool(map, "available")
  }

  fun uploadAvatar(bytes: ByteArray, cx: Float, cy: Float, scale: Float): DesktopApiOutcome<DesktopMediaUploadResult> {
    val path = "/v1/media/avatar?cx=$cx&cy=$cy&scale=$scale"
    val resp = executeRaw("POST", path, bytes, auth = true, contentType = "image/jpeg")
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(
      DesktopMediaUploadResult(
        mediaId = DesktopJson.str(map, "media_id"),
        contentId = DesktopJson.str(map, "content_id"),
        thumbnailMediaId = DesktopJson.str(map, "thumbnail_media_id"),
        perFrameHashes = DesktopJson.stringList(map, "per_frame_hashes"),
      ),
    )
  }

  fun setupProfile(
    fullName: String,
    username: String,
    phoneNumber: String,
    countryIso: String,
    gender: String,
    dateOfBirth: String,
    location: String,
    bio: String,
    avatarMediaId: String?,
  ): DesktopApiOutcome<DesktopStoredSession> {
    val body = buildString {
      append('{')
      append("\"full_name\":\"").append(DesktopJson.escape(fullName)).append("\",")
      append("\"username\":\"").append(DesktopJson.escape(username)).append("\",")
      append("\"phone_number\":\"").append(DesktopJson.escape(phoneNumber)).append("\",")
      append("\"country_iso\":\"").append(DesktopJson.escape(countryIso)).append("\",")
      append("\"gender\":\"").append(DesktopJson.escape(gender)).append("\",")
      append("\"date_of_birth\":\"").append(DesktopJson.escape(dateOfBirth)).append("\",")
      append("\"location\":\"").append(DesktopJson.escape(location)).append("\",")
      append("\"bio\":\"").append(DesktopJson.escape(bio)).append('"')
      if (!avatarMediaId.isNullOrBlank()) {
        append(",\"avatar_media_id\":\"").append(DesktopJson.escape(avatarMediaId)).append('"')
      }
      append('}')
    }
    val resp = executeRaw("POST", "/v1/profile/setup", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val updated = session.copy(
      profileCompleted = true,
      fullName = fullName.trim(),
      username = username.trim().lowercase(),
      bio = bio.trim(),
      location = location.trim(),
    )
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
    return DesktopApiOutcome.Ok(updated)
  }

  fun uploadProfileBackground(bytes: ByteArray): DesktopApiOutcome<List<DesktopProfileBackgroundItem>> {
    val fpHex = DesktopSecurityGuard.sha256HexBytes(bytes)
    val resp = executeRaw(
      method = "POST",
      path = "/v1/media/background",
      body = bytes,
      auth = true,
      contentType = "image/jpeg",
      extraHeaders = mapOf("X-Telefam-Content-SHA256" to fpHex),
    ) ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val parsedList = DesktopJson.objList(map, "backgrounds").mapNotNull(::parseBackgroundItem)
    return DesktopApiOutcome.Ok(parsedList)
  }

  fun deleteProfileBackground(mediaId: String): DesktopApiOutcome<List<DesktopProfileBackgroundItem>> {
    val encId = URLEncoder.encode(mediaId.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("DELETE", "/v1/profile/backgrounds/$encId", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(DesktopJson.objList(map, "backgrounds").mapNotNull(::parseBackgroundItem))
  }

  fun reorderProfileBackgrounds(mediaIds: List<String>): DesktopApiOutcome<List<DesktopProfileBackgroundItem>> {
    val body = "{\"media_ids\":[${mediaIds.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }}]}"
    val resp = executeRaw("PUT", "/v1/profile/backgrounds", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(DesktopJson.objList(map, "backgrounds").mapNotNull(::parseBackgroundItem))
  }

  fun updateProfile(
    fullName: String,
    username: String,
    phoneNumber: String,
    countryIso: String,
    gender: String,
    dateOfBirth: String,
    location: String,
    bio: String,
    avatarMediaId: String?,
  ): DesktopApiOutcome<Pair<Long, Long>> {
    val body = buildString {
      append('{')
      append("\"full_name\":\"").append(DesktopJson.escape(fullName.trim())).append("\",")
      append("\"username\":\"").append(DesktopJson.escape(username.trim().lowercase())).append("\",")
      append("\"phone_number\":\"").append(DesktopJson.escape(phoneNumber.trim())).append("\",")
      append("\"country_iso\":\"").append(DesktopJson.escape(countryIso.trim().uppercase())).append("\",")
      append("\"gender\":\"").append(DesktopJson.escape(gender.trim())).append("\",")
      append("\"date_of_birth\":\"").append(DesktopJson.escape(dateOfBirth.trim())).append("\",")
      append("\"location\":\"").append(DesktopJson.escape(location.trim())).append("\",")
      append("\"bio\":\"").append(DesktopJson.escape(bio.trim())).append('"')
      if (!avatarMediaId.isNullOrBlank()) {
        append(",\"avatar_media_id\":\"").append(DesktopJson.escape(avatarMediaId)).append('"')
      }
      append('}')
    }
    val resp = executeRaw("PATCH", "/v1/profile", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val savedUsername = DesktopJson.str(map, "username").ifBlank { username.trim().lowercase() }
    val savedFullName = DesktopJson.str(map, "full_name").ifBlank { fullName.trim() }
    val changedAtMs = DesktopJson.long(map, "username_changed_at_ms", 0L)
    val nextAllowedMs = DesktopJson.long(map, "next_username_change_ms", 0L)
    val updatedSession = session.copy(
      fullName = savedFullName,
      username = savedUsername,
      bio = bio.trim(),
      location = location.trim(),
    )
    session = updatedSession
    vault.save(updatedSession)
    onSessionUpdated(updatedSession)
    return DesktopApiOutcome.Ok(changedAtMs to nextAllowedMs)
  }

  fun listContacts(filter: ContactFilterTab, query: String, page: Int, pageSize: Int = 20): DesktopApiOutcome<DesktopContactsPage> {
    val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val path = "/v1/contacts?filter=${filter.apiParam}&q=$q&page=$page&page_size=$pageSize"
    val resp = executeRaw("GET", path, null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val rawItems = DesktopJson.objList(map, "items")
    val parsed = rawItems.mapNotNull { item ->
      val uid = DesktopJson.str(item, "user_id")
      if (uid.isBlank()) return@mapNotNull null
      DesktopContactItem(
        userId = uid,
        fullName = DesktopJson.str(item, "full_name"),
        username = DesktopJson.str(item, "username"),
        countryDisplay = DesktopJson.str(item, "location").ifBlank { DesktopJson.str(item, "country_iso") },
        isFollowing = DesktopJson.bool(item, "is_following"),
        followsYou = DesktopJson.bool(item, "follows_you"),
        riskScore = DesktopJson.double(item, "risk_score"),
      )
    }
    return DesktopApiOutcome.Ok(
      DesktopContactsPage(
        items = parsed,
        page = DesktopJson.long(map, "page", page.toLong()).toInt(),
        hasMore = DesktopJson.bool(map, "has_more"),
      ),
    )
  }

  fun setFollowContact(targetUserId: String, follow: Boolean): DesktopApiOutcome<Boolean> {
    val path = if (follow) "/v1/contacts/follow" else "/v1/contacts/unfollow"
    val body = "{\"target_user_id\":\"${DesktopJson.escape(targetUserId)}\"}"
    val resp = executeRaw("POST", path, body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first == 429) {
      return DesktopApiOutcome.Fail(429, "Try again later")
    }
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(DesktopJson.bool(map, "is_following"))
  }

  fun searchSongs(query: String): DesktopApiOutcome<List<SharedSongTrack>> {
    val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/songs/search?q=$q", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val tracks = DesktopJson.objList(map, "items").mapNotNull { item ->
      val title = DesktopJson.str(item, "title")
      val artist = DesktopJson.str(item, "artist")
      if (title.isBlank()) return@mapNotNull null
      SharedSongTrack(
        trackId = DesktopJson.str(item, "track_id"),
        title = title,
        artist = artist,
        album = DesktopJson.str(item, "album"),
        artworkUrl = DesktopJson.str(item, "artwork_url"),
        previewUrl = DesktopJson.str(item, "preview_url"),
        durationMs = DesktopJson.long(item, "duration_ms", 0L),
      )
    }
    return DesktopApiOutcome.Ok(tracks)
  }

  fun searchHashtags(query: String): DesktopApiOutcome<List<SharedHashtagItem>> {
    val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/hashtags/search?q=$q", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val tags = DesktopJson.objList(map, "items").mapNotNull { item ->
      val tag = DesktopJson.str(item, "tag")
      if (tag.isBlank()) return@mapNotNull null
      SharedHashtagItem(tag = tag, postCount = DesktopJson.long(item, "post_count", 0L))
    }
    return DesktopApiOutcome.Ok(tags)
  }

  fun uploadPostPhoto(bytes: ByteArray): DesktopApiOutcome<DesktopMediaUploadResult> {
    val resp = executeRaw("POST", "/v1/media/photo", bytes, auth = true, contentType = "image/jpeg")
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(
      DesktopMediaUploadResult(
        mediaId = DesktopJson.str(map, "media_id"),
        contentId = DesktopJson.str(map, "content_id"),
        thumbnailMediaId = "",
        perFrameHashes = emptyList(),
      ),
    )
  }

  fun uploadPostVideo(
    videoBytes: ByteArray,
    frameHashesCsv: String,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    width: Int,
    height: Int,
    thumbnailBytes: ByteArray?,
  ): DesktopApiOutcome<DesktopMediaUploadResult> {
    val boundary = "----TelefamDesktop${UUID.randomUUID().toString().replace("-", "")}"
    val baos = ByteArrayOutputStream()
    fun writeStr(s: String) = baos.write(s.toByteArray(StandardCharsets.UTF_8))

    writeStr("--$boundary\r\n")
    writeStr("Content-Disposition: form-data; name=\"video\"; filename=\"${UUID.randomUUID()}.mp4\"\r\n")
    writeStr("Content-Type: video/mp4\r\n\r\n")
    baos.write(videoBytes)
    writeStr("\r\n")
    if (thumbnailBytes != null && thumbnailBytes.isNotEmpty()) {
      writeStr("--$boundary\r\n")
      writeStr("Content-Disposition: form-data; name=\"thumbnail\"; filename=\"${UUID.randomUUID()}.jpg\"\r\n")
      writeStr("Content-Type: image/jpeg\r\n\r\n")
      baos.write(thumbnailBytes)
      writeStr("\r\n")
    }
    writeStr("--$boundary--\r\n")

    val path = "/v1/media/video?duration_ms=$durationMs&trim_start_ms=$trimStartMs&trim_end_ms=$trimEndMs&width=$width&height=$height"
    val resp = executeRaw(
      method = "POST",
      path = path,
      body = baos.toByteArray(),
      auth = true,
      contentType = "multipart/form-data; boundary=$boundary",
      extraHeaders = mapOf("X-Telefam-Frame-Hashes" to frameHashesCsv),
    ) ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(
      DesktopMediaUploadResult(
        mediaId = DesktopJson.str(map, "media_id"),
        contentId = DesktopJson.str(map, "content_id"),
        thumbnailMediaId = DesktopJson.str(map, "thumbnail_media_id"),
        perFrameHashes = DesktopJson.stringList(map, "per_frame_hashes"),
      ),
    )
  }

  fun createPost(
    clientOpId: String,
    mode: SharedPostMode,
    textContent: String,
    caption: String,
    themeId: String,
    fontId: String,
    song: SharedSongTrack?,
    taggedUserIds: List<String>,
    hashtags: List<String>,
    photoMediaIds: List<String>,
    videoMediaId: String,
    thumbnailMediaId: String,
    trimStartMs: Long,
    trimEndMs: Long,
    muteOriginalAudio: Boolean,
    contentId: String,
  ): DesktopApiOutcome<String> {
    val body = buildString {
      append('{')
      append("\"client_op_id\":\"").append(DesktopJson.escape(clientOpId)).append("\",")
      append("\"mode\":\"").append(mode.apiValue).append("\",")
      append("\"text_content\":\"").append(DesktopJson.escape(textContent)).append("\",")
      append("\"caption\":\"").append(DesktopJson.escape(caption)).append("\",")
      append("\"theme_id\":\"").append(DesktopJson.escape(themeId)).append("\",")
      append("\"font_id\":\"").append(DesktopJson.escape(fontId)).append("\",")
      if (song != null) {
        append("\"song\":{")
        append("\"track_id\":\"").append(DesktopJson.escape(song.trackId)).append("\",")
        append("\"title\":\"").append(DesktopJson.escape(song.title)).append("\",")
        append("\"artist\":\"").append(DesktopJson.escape(song.artist)).append("\",")
        append("\"album\":\"").append(DesktopJson.escape(song.album)).append("\",")
        append("\"artwork_url\":\"").append(DesktopJson.escape(song.artworkUrl)).append("\",")
        append("\"preview_url\":\"").append(DesktopJson.escape(song.previewUrl)).append("\",")
        append("\"duration_ms\":").append(song.durationMs)
        append("},")
      }
      append("\"tagged_user_ids\":[").append(taggedUserIds.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }).append("],")
      append("\"hashtags\":[").append(hashtags.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }).append("],")
      append("\"photo_media_ids\":[").append(photoMediaIds.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }).append("],")
      append("\"video_media_id\":\"").append(DesktopJson.escape(videoMediaId)).append("\",")
      append("\"thumbnail_media_id\":\"").append(DesktopJson.escape(thumbnailMediaId)).append("\",")
      append("\"trim_start_ms\":").append(trimStartMs).append(',')
      append("\"trim_end_ms\":").append(trimEndMs).append(',')
      append("\"mute_original_audio\":").append(muteOriginalAudio).append(',')
      append("\"content_id\":\"").append(DesktopJson.escape(contentId)).append('"')
      append('}')
    }
    val resp = executeRaw("POST", "/v1/posts", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val postObj = DesktopJson.obj(map, "post")
    return DesktopApiOutcome.Ok(DesktopJson.str(postObj, "post_id"))
  }

  fun updatePost(
    postId: String,
    textContent: String,
    caption: String,
    themeId: String,
    fontId: String,
    song: SharedSongTrack?,
    hashtags: List<String>,
    muteOriginalAudio: Boolean,
  ): DesktopApiOutcome<Boolean> {
    val body = buildString {
      append('{')
      append("\"text_content\":\"").append(DesktopJson.escape(textContent)).append("\",")
      append("\"caption\":\"").append(DesktopJson.escape(caption)).append("\",")
      append("\"theme_id\":\"").append(DesktopJson.escape(themeId)).append("\",")
      append("\"font_id\":\"").append(DesktopJson.escape(fontId)).append("\",")
      if (song != null) {
        append("\"song\":{")
        append("\"track_id\":\"").append(DesktopJson.escape(song.trackId)).append("\",")
        append("\"title\":\"").append(DesktopJson.escape(song.title)).append("\",")
        append("\"artist\":\"").append(DesktopJson.escape(song.artist)).append("\",")
        append("\"album\":\"").append(DesktopJson.escape(song.album)).append("\",")
        append("\"artwork_url\":\"").append(DesktopJson.escape(song.artworkUrl)).append("\",")
        append("\"preview_url\":\"").append(DesktopJson.escape(song.previewUrl)).append("\",")
        append("\"duration_ms\":").append(song.durationMs)
        append("},")
      }
      append("\"hashtags\":[").append(hashtags.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }).append("],")
      append("\"mute_original_audio\":").append(muteOriginalAudio)
      append('}')
    }
    val resp = executeRaw("PATCH", "/v1/posts/${URLEncoder.encode(postId, StandardCharsets.UTF_8)}", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun updatePostPrivacy(postId: String, privacy: String): DesktopApiOutcome<Boolean> {
    val body = "{\"privacy\":\"${DesktopJson.escape(privacy.trim().lowercase())}\"}"
    val resp = executeRaw("PATCH", "/v1/posts/${URLEncoder.encode(postId, StandardCharsets.UTF_8)}", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun deletePost(postId: String): DesktopApiOutcome<Boolean> {
    val resp = executeRaw("DELETE", "/v1/posts/${URLEncoder.encode(postId, StandardCharsets.UTF_8)}", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun getFeeds(tab: SharedFeedTab, page: Int, pageSize: Int = 15): DesktopApiOutcome<DesktopFeedPage> {
    val resp = executeRaw("GET", "/v1/feeds?tab=${tab.apiParam}&page=$page&page_size=$pageSize", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    return DesktopApiOutcome.Ok(
      DesktopFeedPage(
        items = DesktopJson.objList(map, "items").map(::parseFeedPost),
        page = DesktopJson.long(map, "page", page.toLong()).toInt(),
        hasMore = DesktopJson.bool(map, "has_more"),
      ),
    )
  }

  fun searchFeeds(query: String, shortcut: String, page: Int, pageSize: Int = 24): DesktopApiOutcome<DesktopFeedSearchPage> {
    val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val sc = URLEncoder.encode(shortcut.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/feeds/search?q=$q&shortcut=$sc&page=$page&page_size=$pageSize", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    val map = DesktopJson.parseObject(resp.second)
    val shortcuts = DesktopJson.objList(map, "shortcuts").map { s ->
      DesktopSearchShortcut(
        key = DesktopJson.str(s, "key"),
        label = DesktopJson.str(s, "label"),
        count = DesktopJson.long(s, "count", 0L),
      )
    }
    return DesktopApiOutcome.Ok(
      DesktopFeedSearchPage(
        shortcuts = shortcuts,
        items = DesktopJson.objList(map, "items").map(::parseFeedPost),
        hasMore = DesktopJson.bool(map, "has_more"),
      ),
    )
  }

  fun interactFeed(
    postId: String,
    action: SharedFeedAction,
    reason: String = "",
    watchMs: Long = 0L,
    durationMs: Long = 0L,
  ): DesktopApiOutcome<Boolean> {
    val body = buildString {
      append('{')
      append("\"client_op_id\":\"").append(UUID.randomUUID().toString()).append("\",")
      append("\"post_id\":\"").append(DesktopJson.escape(postId)).append("\",")
      append("\"action\":\"").append(action.apiValue).append("\",")
      append("\"watch_ms\":").append(watchMs).append(',')
      append("\"duration_ms\":").append(durationMs).append(',')
      append("\"reason\":\"").append(DesktopJson.escape(reason)).append('"')
      append('}')
    }
    val resp = executeRaw("POST", "/v1/feeds/interact", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun downloadFeedVideo(postId: String, destinationFile: File): DesktopApiOutcome<File> {
    val base = baseUrl() ?: return DesktopApiOutcome.Fail(0, "Server connection unavailable")
    return runCatching {
      val req = HttpRequest.newBuilder(URI("$base/v1/feeds/${URLEncoder.encode(postId, StandardCharsets.UTF_8)}/download"))
        .header("Authorization", "Bearer ${session.accessToken}")
        .header("X-Telefam-Device-FP", deviceFingerprint)
        .GET()
        .timeout(Duration.ofSeconds(60))
        .build()
      val res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
      if (res.statusCode() !in 200..299) {
        DesktopApiOutcome.Fail(res.statusCode(), "Download unavailable")
      } else {
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeBytes(res.body())
        DesktopApiOutcome.Ok(destinationFile)
      }
    }.getOrElse { DesktopApiOutcome.Fail(0, "Network error") }
  }

  fun recordAuditEvent(action: String, metadata: Map<String, String> = emptyMap()) {
    val metaJson = metadata.entries.joinToString(",") {
      "\"${DesktopJson.escape(it.key)}\":\"${DesktopJson.escape(it.value)}\""
    }
    val body = "{\"action\":\"${DesktopJson.escape(action)}\",\"metadata\":{$metaJson}}"
    executeRaw("POST", "/v1/audit/event", body.toByteArray(StandardCharsets.UTF_8), auth = true)
  }

  fun getUserProfile(userId: String): DesktopApiOutcome<DesktopUserProfile> {
    val cleanId = userId.trim().ifBlank { "me" }
    val encId = URLEncoder.encode(cleanId, StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/users/$encId/profile", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      if (DesktopJson.bool(map, "success")) {
        val parsed = parseUserProfile(map, cleanId)
        saveCachedText("profile_$cleanId.json", resp.second)
        if (parsed.userId.isNotBlank()) {
          saveCachedText("profile_${parsed.userId}.json", resp.second)
        }
        return DesktopApiOutcome.Ok(parsed)
      }
    }
    val cachedRaw = loadCachedText("profile_$cleanId.json")
    if (cachedRaw != null) {
      val map = DesktopJson.parseObject(cachedRaw)
      if (map.isNotEmpty()) {
        return DesktopApiOutcome.Ok(parseUserProfile(map, cleanId))
      }
    }
    if (cleanId == "me" || (session.userId.isNotBlank() && cleanId == session.userId)) {
      return DesktopApiOutcome.Ok(
        DesktopUserProfile(
          userId = session.userId.ifBlank { "me" },
          fullName = session.fullName.ifBlank { session.email },
          username = session.username,
          avatarUrl = "",
          bio = session.bio,
          location = session.location,
          countryIso = "",
          followingCount = 0L,
          followersCount = 0L,
          likesCount = 0L,
          isFollowing = false,
          followsYou = false,
          isOwner = true,
          isBlocked = false,
          riskScore = 0.0,
          posts = emptyList(),
        ),
      )
    }
    return if (resp == null) {
      DesktopApiOutcome.Fail(0, "Offline")
    } else {
      DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
  }

  fun getUserConnections(
    userId: String,
    isFollowers: Boolean,
    page: Int = 1,
    pageSize: Int = 30,
  ): DesktopApiOutcome<DesktopUserConnectionsPage> {
    val cleanId = userId.trim().ifBlank { "me" }
    val encId = URLEncoder.encode(cleanId, StandardCharsets.UTF_8)
    val endpoint = if (isFollowers) "followers" else "following"
    val cacheKey = "conn_${cleanId}_$endpoint.json"
    val resp = executeRaw("GET", "/v1/users/$encId/$endpoint?page=$page&page_size=$pageSize", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      if (page == 1) saveCachedText(cacheKey, resp.second)
      return DesktopApiOutcome.Ok(parseConnectionsPage(DesktopJson.parseObject(resp.second), page))
    }
    if (page == 1) {
      loadCachedText(cacheKey)?.let { raw ->
        val parsedMap = DesktopJson.parseObject(raw)
        if (parsedMap.isNotEmpty()) {
          return DesktopApiOutcome.Ok(parseConnectionsPage(parsedMap, 1))
        }
      }
    }
    return if (resp == null) {
      DesktopApiOutcome.Fail(0, "Offline")
    } else {
      DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
  }

  fun blockUser(userId: String): DesktopApiOutcome<Boolean> {
    val encId = URLEncoder.encode(userId.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("POST", "/v1/users/$encId/block", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun unblockUser(userId: String): DesktopApiOutcome<Boolean> {
    val encId = URLEncoder.encode(userId.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("POST", "/v1/users/$encId/unblock", null, auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(false)
  }

  fun reportUser(userId: String, reason: String, details: String): DesktopApiOutcome<Boolean> {
    val encId = URLEncoder.encode(userId.trim(), StandardCharsets.UTF_8)
    val body = "{\"reason\":\"${DesktopJson.escape(reason)}\",\"details\":\"${DesktopJson.escape(details)}\"}"
    val resp = executeRaw("POST", "/v1/users/$encId/report", body.toByteArray(StandardCharsets.UTF_8), auth = true)
      ?: return DesktopApiOutcome.Fail(0, "Offline")
    if (resp.first !in 200..299) {
      return DesktopApiOutcome.Fail(resp.first, parseError(resp.first, resp.second))
    }
    return DesktopApiOutcome.Ok(true)
  }

  fun muteUser(userId: String): DesktopApiOutcome<Boolean> {
    val cleanId = userId.trim()
    val encId = URLEncoder.encode(cleanId, StandardCharsets.UTF_8)
    val resp = executeRaw("POST", "/v1/users/$encId/mute", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      return DesktopApiOutcome.Ok(true)
    }
    enqueueSettingsOp("mute", "{\"user_id\":\"${DesktopJson.escape(cleanId)}\"}")
    return DesktopApiOutcome.Ok(true)
  }

  fun unmuteUser(userId: String): DesktopApiOutcome<Boolean> {
    val cleanId = userId.trim()
    val encId = URLEncoder.encode(cleanId, StandardCharsets.UTF_8)
    val resp = executeRaw("POST", "/v1/users/$encId/unmute", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      return DesktopApiOutcome.Ok(false)
    }
    enqueueSettingsOp("unmute", "{\"user_id\":\"${DesktopJson.escape(cleanId)}\"}")
    return DesktopApiOutcome.Ok(false)
  }

  fun loadCachedSettingsBundle(): DesktopSettingsBundle {
    val uid = session.userId.ifBlank { "default" }
    val raw = loadCachedText("settings_$uid.json") ?: return DesktopSettingsBundle()
    val map = DesktopJson.parseObject(raw)
    return if (map.isEmpty()) DesktopSettingsBundle() else parseSettingsBundle(map)
  }

  private fun saveCachedSettingsBundle(bundle: DesktopSettingsBundle) {
    val uid = session.userId.ifBlank { "default" }
    val json = buildString {
      append('{')
      append("\"privacy\":").append(encodePrivacyJson(bundle.privacy)).append(',')
      append("\"notifications\":").append(encodeNotificationsJson(bundle.notifications)).append(',')
      append("\"content_preferences\":").append(encodeContentPrefsJson(bundle.contentPreferences, resetSuggestions = false)).append(',')
      append("\"chat_privacy\":").append(encodeChatPrivacyJson(bundle.chatPrivacy)).append(',')
      append("\"sync_preferences\":").append(encodeSyncPrefsJson(bundle.syncPreferences))
      append('}')
    }
    saveCachedText("settings_$uid.json", json)
  }

  private fun loadPendingSettingsOps(): MutableList<DesktopPendingSettingsOp> {
    val uid = session.userId.ifBlank { "default" }
    val raw = loadCachedText("settings_outbox_$uid.json") ?: return mutableListOf()
    val map = DesktopJson.parseObject(raw)
    return DesktopJson.objList(map, "ops").mapNotNull { item ->
      val id = DesktopJson.str(item, "op_id")
      val kind = DesktopJson.str(item, "op_kind")
      val payload = DesktopJson.str(item, "payload_json")
      if (id.isBlank() || kind.isBlank()) null
      else DesktopPendingSettingsOp(id, kind, payload, DesktopJson.long(item, "created_at_ms", 0L))
    }.toMutableList()
  }

  private fun savePendingSettingsOps(ops: List<DesktopPendingSettingsOp>) {
    val uid = session.userId.ifBlank { "default" }
    val json = buildString {
      append("{\"ops\":[")
      ops.forEachIndexed { idx, op ->
        if (idx > 0) append(',')
        append('{')
        append("\"op_id\":\"").append(DesktopJson.escape(op.opId)).append("\",")
        append("\"op_kind\":\"").append(DesktopJson.escape(op.opKind)).append("\",")
        append("\"payload_json\":\"").append(DesktopJson.escape(op.payloadJson)).append("\",")
        append("\"created_at_ms\":").append(op.createdAtMs)
        append('}')
      }
      append("]}")
    }
    saveCachedText("settings_outbox_$uid.json", json)
  }

  fun pendingSettingsOutboxCount(): Int = loadPendingSettingsOps().size

  private fun enqueueSettingsOp(kind: String, payloadJson: String) {
    val list = loadPendingSettingsOps()
    if (kind == "privacy" || kind == "notifications" || kind == "content_prefs" || kind == "chat_privacy" || kind == "chat_theme" || kind == "sync_prefs") {
      list.removeAll { it.opKind == kind }
    }
    list.add(
      DesktopPendingSettingsOp(
        opId = "${kind}_${UUID.randomUUID()}",
        opKind = kind,
        payloadJson = payloadJson,
        createdAtMs = System.currentTimeMillis(),
      ),
    )
    savePendingSettingsOps(list)
  }

  fun flushSettingsOutbox() {
    if (session.accessToken.isBlank()) return
    val ops = loadPendingSettingsOps()
    if (ops.isEmpty()) return
    val remaining = mutableListOf<DesktopPendingSettingsOp>()
    for (op in ops) {
      val ok = when (op.opKind) {
        "privacy" -> {
          val r = executeRaw("PUT", "/v1/settings/privacy", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "notifications" -> {
          val r = executeRaw("PUT", "/v1/settings/notifications", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "content_prefs" -> {
          val r = executeRaw("PUT", "/v1/settings/content-preferences", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "chat_privacy" -> {
          val r = executeRaw("PUT", "/v1/chat/privacy", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "chat_theme" -> {
          val r = executeRaw("PUT", "/v1/chat/theme", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "sync_prefs" -> {
          val r = executeRaw("PUT", "/v1/settings/sync", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        "mute" -> {
          val uid = DesktopJson.str(DesktopJson.parseObject(op.payloadJson), "user_id")
          if (uid.isBlank()) true else {
            val r = executeRaw("POST", "/v1/users/${URLEncoder.encode(uid, StandardCharsets.UTF_8)}/mute", null, auth = true)
            r != null && r.first in 200..299
          }
        }
        "unmute" -> {
          val uid = DesktopJson.str(DesktopJson.parseObject(op.payloadJson), "user_id")
          if (uid.isBlank()) true else {
            val r = executeRaw("POST", "/v1/users/${URLEncoder.encode(uid, StandardCharsets.UTF_8)}/unmute", null, auth = true)
            r != null && r.first in 200..299
          }
        }
        "unblock" -> {
          val uid = DesktopJson.str(DesktopJson.parseObject(op.payloadJson), "user_id")
          if (uid.isBlank()) true else {
            val r = executeRaw("POST", "/v1/users/${URLEncoder.encode(uid, StandardCharsets.UTF_8)}/unblock", null, auth = true)
            r != null && r.first in 200..299
          }
        }
        "report_problem" -> {
          val r = executeRaw("POST", "/v1/support/reports", op.payloadJson.toByteArray(StandardCharsets.UTF_8), auth = true)
          r != null && r.first in 200..299
        }
        else -> true
      }
      if (!ok) remaining.add(op)
    }
    savePendingSettingsOps(remaining)
  }

  fun getSettings(): DesktopSettingsBundle {
    flushSettingsOutbox()
    val resp = executeRaw("GET", "/v1/settings", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      if (DesktopJson.bool(map, "success")) {
        val parsed = parseSettingsBundle(map)
        saveCachedSettingsBundle(parsed)
        return parsed
      }
    }
    return loadCachedSettingsBundle()
  }

  fun updatePrivacySettings(next: DesktopPrivacySettings): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val updated = cur.copy(privacy = stamped)
    saveCachedSettingsBundle(updated)
    val payload = encodePrivacyJson(stamped)
    val resp = executeRaw("PUT", "/v1/settings/privacy", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val parsed = parseSettingsBundle(DesktopJson.parseObject(resp.second))
      saveCachedSettingsBundle(parsed)
      return parsed
    }
    enqueueSettingsOp("privacy", payload)
    return updated
  }

  fun updateNotificationSettings(next: DesktopNotificationSettings): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val updated = cur.copy(notifications = stamped)
    saveCachedSettingsBundle(updated)
    val payload = encodeNotificationsJson(stamped)
    val resp = executeRaw("PUT", "/v1/settings/notifications", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val parsed = parseSettingsBundle(DesktopJson.parseObject(resp.second))
      saveCachedSettingsBundle(parsed)
      return parsed
    }
    enqueueSettingsOp("notifications", payload)
    return updated
  }

  fun updateContentPreferences(next: DesktopContentPreferences, resetSuggestions: Boolean = false): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val updated = cur.copy(contentPreferences = stamped)
    saveCachedSettingsBundle(updated)
    val payload = encodeContentPrefsJson(stamped, resetSuggestions)
    val resp = executeRaw("PUT", "/v1/settings/content-preferences", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val parsed = parseSettingsBundle(DesktopJson.parseObject(resp.second))
      saveCachedSettingsBundle(parsed)
      return parsed
    }
    enqueueSettingsOp("content_prefs", payload)
    return updated
  }

  fun updateChatPrivacySettings(next: DesktopChatPrivacySettings): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val stamped = next.copy(
      chatThemeId = DesktopChatPrivacyCatalog.themeById(next.chatThemeId).themeId,
      bubbleColorId = DesktopChatPrivacyCatalog.bubbleColorById(next.bubbleColorId).colorId,
      updatedAtMs = System.currentTimeMillis(),
    )
    val updated = cur.copy(chatPrivacy = stamped)
    saveCachedSettingsBundle(updated)
    val payload = encodeChatPrivacyJson(stamped)
    val resp = executeRaw("PUT", "/v1/chat/privacy", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val cpMap = DesktopJson.objOrNull(map, "chat_privacy") ?: map
      val synced = updated.copy(chatPrivacy = parseChatPrivacy(cpMap))
      saveCachedSettingsBundle(synced)
      return synced
    }
    enqueueSettingsOp("chat_privacy", payload)
    return updated
  }

  fun updateChatThemeAndBubble(themeId: String? = null, bubbleColorId: String? = null): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val prev = cur.chatPrivacy
    val nextTheme = themeId?.let { DesktopChatPrivacyCatalog.themeById(it).themeId } ?: prev.chatThemeId
    val nextBubble = bubbleColorId?.let { DesktopChatPrivacyCatalog.bubbleColorById(it).colorId } ?: prev.bubbleColorId
    val stamped = prev.copy(
      chatThemeId = nextTheme,
      bubbleColorId = nextBubble,
      updatedAtMs = System.currentTimeMillis(),
    )
    val updated = cur.copy(chatPrivacy = stamped)
    saveCachedSettingsBundle(updated)
    val payload = buildString {
      append('{')
      append("\"chat_theme_id\":\"").append(DesktopJson.escape(nextTheme)).append("\",")
      append("\"bubble_color_id\":\"").append(DesktopJson.escape(nextBubble)).append('"')
      append('}')
    }
    val resp = executeRaw("PUT", "/v1/chat/theme", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val cpMap = DesktopJson.objOrNull(map, "chat_privacy") ?: map
      val synced = updated.copy(chatPrivacy = parseChatPrivacy(cpMap))
      saveCachedSettingsBundle(synced)
      return synced
    }
    enqueueSettingsOp("chat_theme", payload)
    return updated
  }

  fun updateSyncPreferences(next: DesktopSyncPreferences): DesktopSettingsBundle {
    val cur = loadCachedSettingsBundle()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val updated = cur.copy(syncPreferences = stamped)
    saveCachedSettingsBundle(updated)
    val payload = encodeSyncPrefsJson(stamped)
    val resp = executeRaw("PUT", "/v1/settings/sync", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val spMap = DesktopJson.objOrNull(map, "sync_preferences")
      val synced = if (spMap != null) updated.copy(syncPreferences = parseSyncPreferences(spMap)) else parseSettingsBundle(map)
      saveCachedSettingsBundle(synced)
      return synced
    }
    enqueueSettingsOp("sync_prefs", payload)
    return updated
  }

  fun syncNowToServer(): DesktopSyncRunResult {
    flushSettingsOutbox()
    val cur = loadCachedSettingsBundle()
    val payload = buildString {
      append('{')
      append("\"client_sync_id\":\"").append(DesktopJson.escape(UUID.randomUUID().toString())).append("\",")
      append("\"privacy\":").append(encodePrivacyJson(cur.privacy)).append(',')
      append("\"notifications\":").append(encodeNotificationsJson(cur.notifications)).append(',')
      append("\"content_preferences\":").append(encodeContentPrefsJson(cur.contentPreferences, resetSuggestions = false)).append(',')
      append("\"chat_privacy\":").append(encodeChatPrivacyJson(cur.chatPrivacy)).append(',')
      append("\"sync_preferences\":").append(encodeSyncPrefsJson(cur.syncPreferences))
      append('}')
    }
    val resp = executeRaw("POST", "/v1/sync/now", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      if (DesktopJson.bool(map, "success")) {
        val synced = parseSettingsBundle(map)
        saveCachedSettingsBundle(synced)
        val count = DesktopJson.long(map, "synced_ops_count", 0L).toInt()
        val rem = pendingSettingsOutboxCount()
        val msg = if (rem == 0) "All data synced with server" else "Synced $count section(s), $rem pending"
        return DesktopSyncRunResult(
          success = true,
          syncedOpsCount = count,
          pendingRemaining = rem,
          message = msg,
          bundle = synced,
        )
      }
    }
    val rem = pendingSettingsOutboxCount()
    val queuedBundle = cur.copy(
      syncPreferences = cur.syncPreferences.copy(
        lastSyncStatus = if (rem > 0) "queued" else cur.syncPreferences.lastSyncStatus,
        updatedAtMs = System.currentTimeMillis(),
      ),
    )
    saveCachedSettingsBundle(queuedBundle)
    return DesktopSyncRunResult(
      success = false,
      syncedOpsCount = 0,
      pendingRemaining = rem,
      message = "Offline or unreachable — changes remain safely queued",
      bundle = queuedBundle,
    )
  }

  fun listBlockedAccounts(query: String): List<DesktopRestrictedAccount> =
    listRestrictedAccounts(isMuted = false, query = query)

  fun listMutedAccounts(query: String): List<DesktopRestrictedAccount> =
    listRestrictedAccounts(isMuted = true, query = query)

  private fun listRestrictedAccounts(isMuted: Boolean, query: String): List<DesktopRestrictedAccount> {
    val uid = session.userId.ifBlank { "default" }
    val kind = if (isMuted) "muted" else "blocked"
    val qEnc = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val path = if (isMuted) "/v1/settings/muted-accounts?q=$qEnc" else "/v1/settings/blocked-accounts?q=$qEnc"
    val resp = executeRaw("GET", path, null, auth = true)
    val items = if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      if (query.isBlank()) saveCachedText("restricted_${kind}_$uid.json", resp.second)
      DesktopJson.objList(map, "items").mapNotNull { m -> parseRestrictedAccount(m, isMuted) }
    } else {
      val cached = loadCachedText("restricted_${kind}_$uid.json")?.let { DesktopJson.parseObject(it) }
      cached?.let { c -> DesktopJson.objList(c, "items").mapNotNull { m -> parseRestrictedAccount(m, isMuted) } } ?: emptyList()
    }
    val qLow = query.trim().lowercase()
    return if (qLow.isEmpty()) items else items.filter {
      it.fullName.lowercase().contains(qLow) || it.username.lowercase().contains(qLow)
    }
  }

  fun submitProblemReport(
    category: String,
    description: String,
    screenshotBytes: ByteArray?,
  ): DesktopSupportTicket {
    val opId = UUID.randomUUID().toString()
    val nowMs = System.currentTimeMillis()
    val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(nowMs))
    val localRef = "TF-$dateStr-${opId.take(6).uppercase()}"
    var mediaId = ""
    var sha256 = if (screenshotBytes != null && screenshotBytes.isNotEmpty()) desktopBytesSha256(screenshotBytes) else ""

    if (screenshotBytes != null && screenshotBytes.isNotEmpty()) {
      val up = uploadPostPhoto(screenshotBytes)
      if (up is DesktopApiOutcome.Ok) {
        mediaId = up.data.mediaId
      }
    }

    val payload = buildString {
      append('{')
      append("\"client_op_id\":\"").append(DesktopJson.escape(opId)).append("\",")
      append("\"category\":\"").append(DesktopJson.escape(category.trim())).append("\",")
      append("\"description\":\"").append(DesktopJson.escape(description.trim())).append("\",")
      append("\"screenshot_media_id\":\"").append(DesktopJson.escape(mediaId)).append("\",")
      append("\"screenshot_sha256\":\"").append(DesktopJson.escape(sha256)).append('"')
      append('}')
    }
    val resp = executeRaw("POST", "/v1/support/reports", payload.toByteArray(StandardCharsets.UTF_8), auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val srv = DesktopJson.objOrNull(map, "ticket")
      val ticket = DesktopSupportTicket(
        ticketId = srv?.let { DesktopJson.str(it, "ticket_id") }?.ifBlank { opId } ?: opId,
        referenceNumber = DesktopJson.str(map, "reference_number").ifBlank { localRef },
        category = srv?.let { DesktopJson.str(it, "category") }?.ifBlank { category } ?: category,
        description = srv?.let { DesktopJson.str(it, "description") }?.ifBlank { description } ?: description,
        screenshotSha256 = sha256,
        status = srv?.let { DesktopJson.str(it, "status") }?.ifBlank { "open" } ?: "open",
        resolutionNote = srv?.let { DesktopJson.str(it, "resolution_note") } ?: "Submitted to Trust & Support",
        isSynced = true,
        createdAtMs = srv?.let { DesktopJson.long(it, "created_at_ms", nowMs) } ?: nowMs,
      )
      appendCachedTicket(ticket)
      return ticket
    }

    enqueueSettingsOp("report_problem", payload)
    val offlineTicket = DesktopSupportTicket(
      ticketId = opId,
      referenceNumber = localRef,
      category = category.trim(),
      description = description.trim(),
      screenshotSha256 = sha256,
      status = "queued_offline",
      resolutionNote = "Saved offline. Will sync automatically on reconnect.",
      isSynced = false,
      createdAtMs = nowMs,
    )
    appendCachedTicket(offlineTicket)
    return offlineTicket
  }

  private fun appendCachedTicket(ticket: DesktopSupportTicket) {
    val uid = session.userId.ifBlank { "default" }
    val cur = loadCachedTickets().filterNot { it.ticketId == ticket.ticketId }
    saveCachedTickets(listOf(ticket) + cur)
    val _u = uid
  }

  private fun saveCachedTickets(list: List<DesktopSupportTicket>) {
    val uid = session.userId.ifBlank { "default" }
    val json = buildString {
      append("{\"items\":[")
      list.forEachIndexed { idx, t ->
        if (idx > 0) append(',')
        append('{')
        append("\"ticket_id\":\"").append(DesktopJson.escape(t.ticketId)).append("\",")
        append("\"reference_number\":\"").append(DesktopJson.escape(t.referenceNumber)).append("\",")
        append("\"category\":\"").append(DesktopJson.escape(t.category)).append("\",")
        append("\"description\":\"").append(DesktopJson.escape(t.description)).append("\",")
        append("\"screenshot_sha256\":\"").append(DesktopJson.escape(t.screenshotSha256)).append("\",")
        append("\"status\":\"").append(DesktopJson.escape(t.status)).append("\",")
        append("\"resolution_note\":\"").append(DesktopJson.escape(t.resolutionNote)).append("\",")
        append("\"created_at_ms\":").append(t.createdAtMs)
        append('}')
      }
      append("]}")
    }
    saveCachedText("tickets_$uid.json", json)
  }

  private fun loadCachedTickets(): List<DesktopSupportTicket> {
    val uid = session.userId.ifBlank { "default" }
    val raw = loadCachedText("tickets_$uid.json") ?: return emptyList()
    val map = DesktopJson.parseObject(raw)
    return DesktopJson.objList(map, "items").map { parseTicket(it, synced = false) }
  }

  fun listSupportTickets(): List<DesktopSupportTicket> {
    flushSettingsOutbox()
    val resp = executeRaw("GET", "/v1/support/tickets", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val serverItems = DesktopJson.objList(map, "items").map { parseTicket(it, synced = true) }
      val localQueued = loadCachedTickets().filter { !it.isSynced && serverItems.none { s -> s.ticketId == it.ticketId } }
      val merged = (localQueued + serverItems).sortedByDescending { it.createdAtMs }
      saveCachedTickets(merged)
      return merged
    }
    return loadCachedTickets()
  }

  fun searchFaqs(query: String): List<DesktopFaqEntry> {
    val qEnc = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
    val resp = executeRaw("GET", "/v1/support/faqs?q=$qEnc", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val remote = DesktopJson.objList(map, "items").mapNotNull { m ->
        val id = DesktopJson.str(m, "id")
        val q = DesktopJson.str(m, "question")
        if (q.isBlank()) null else DesktopFaqEntry(
          id = id.ifBlank { q },
          category = DesktopJson.str(m, "category").ifBlank { "General" },
          question = q,
          answer = DesktopJson.str(m, "answer"),
        )
      }
      if (remote.isNotEmpty()) return remote
    }
    val low = query.trim().lowercase()
    return DesktopSettingsCatalog.defaultFaqs.filter {
      low.isEmpty() || it.question.lowercase().contains(low) || it.answer.lowercase().contains(low) || it.category.lowercase().contains(low)
    }
  }

  fun getSystemStatus(): List<DesktopSystemServiceStatus> {
    val resp = executeRaw("GET", "/v1/settings/system-status", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val services = DesktopJson.objList(map, "services").mapNotNull { m ->
        val name = DesktopJson.str(m, "service_name")
        if (name.isBlank()) null else DesktopSystemServiceStatus(
          serviceName = name,
          status = DesktopJson.str(m, "status").ifBlank { "operational" },
          latencyMs = DesktopJson.long(m, "latency_ms", 0L).toInt(),
          summary = DesktopJson.str(m, "summary"),
        )
      }
      if (services.isNotEmpty()) return services
    }
    return listOf(
      DesktopSystemServiceStatus(
        serviceName = "Device Sync & Connectivity",
        status = "offline",
        latencyMs = 0,
        summary = "Offline or server unreachable. Encrypted local cache is active.",
      ),
    )
  }

  fun getLegalDocuments(): List<DesktopLegalDoc> {
    val resp = executeRaw("GET", "/v1/legal/documents", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      val docs = DesktopJson.objList(map, "items").mapNotNull { m ->
        val title = DesktopJson.str(m, "title")
        if (title.isBlank()) null else DesktopLegalDoc(
          id = DesktopJson.str(m, "id").ifBlank { title.lowercase() },
          title = title,
          lastUpdated = DesktopJson.str(m, "last_updated").ifBlank { "November 14, 2026" },
          sections = DesktopJson.stringList(m, "sections"),
        )
      }
      if (docs.isNotEmpty()) return docs
    }
    return DesktopSettingsCatalog.legalDocuments
  }

  fun checkAppVersion(): DesktopVersionCheckResult {
    val nowMs = System.currentTimeMillis()
    val curVer = DesktopSettingsCatalog.APP_VERSION
    val curBuild = DesktopSettingsCatalog.APP_BUILD
    val resp = executeRaw("GET", "/v1/app/version?version=$curVer", null, auth = true)
    if (resp != null && resp.first in 200..299) {
      val map = DesktopJson.parseObject(resp.second)
      if (DesktopJson.bool(map, "success")) {
        return DesktopVersionCheckResult(
          installedVersion = curVer,
          installedBuild = curBuild,
          latestVersion = DesktopJson.str(map, "latest_version").ifBlank { curVer },
          updateAvailable = DesktopJson.bool(map, "update_available"),
          releaseNotes = DesktopJson.str(map, "release_notes").ifBlank { "You are up to date." },
          checkedAtMs = nowMs,
        )
      }
    }
    return DesktopVersionCheckResult(
      installedVersion = curVer,
      installedBuild = curBuild,
      latestVersion = curVer,
      updateAvailable = false,
      releaseNotes = "Offline or unreachable. Running Telefam Desktop v$curVer.",
      checkedAtMs = nowMs,
    )
  }

  private fun parseConnectionsPage(map: Map<String, Any?>, fallbackPage: Int): DesktopUserConnectionsPage {
    val items = DesktopJson.objList(map, "items").mapNotNull { item ->
      val uid = DesktopJson.str(item, "user_id")
      if (uid.isBlank()) return@mapNotNull null
      DesktopContactItem(
        userId = uid,
        fullName = DesktopJson.str(item, "full_name"),
        username = DesktopJson.str(item, "username"),
        countryDisplay = DesktopJson.str(item, "location").ifBlank { DesktopJson.str(item, "country_iso") },
        isFollowing = DesktopJson.bool(item, "is_following"),
        followsYou = DesktopJson.bool(item, "follows_you"),
        riskScore = DesktopJson.double(item, "risk_score"),
      )
    }
    return DesktopUserConnectionsPage(
      items = items,
      page = DesktopJson.long(map, "page", fallbackPage.toLong()).toInt(),
      hasMore = DesktopJson.bool(map, "has_more"),
    )
  }

  private fun parseBackgroundItem(map: Map<String, Any?>): DesktopProfileBackgroundItem? {
    val id = DesktopJson.str(map, "media_id")
    val url = DesktopJson.str(map, "url")
    if (id.isBlank() && url.isBlank()) return null
    return DesktopProfileBackgroundItem(
      mediaId = id.ifBlank { url },
      url = url,
      sha256Fingerprint = DesktopJson.str(map, "sha256_fingerprint"),
      dHashFingerprint = DesktopJson.str(map, "dhash_fingerprint"),
      width = DesktopJson.long(map, "width", 0L).toInt(),
      height = DesktopJson.long(map, "height", 0L).toInt(),
      byteSize = DesktopJson.long(map, "byte_size", 0L).toInt(),
      createdAtMs = DesktopJson.long(map, "created_at_ms", 0L),
    )
  }

  private fun parseUserProfile(map: Map<String, Any?>, fallbackId: String): DesktopUserProfile {
    val parsedBgs = DesktopJson.objList(map, "backgrounds").mapNotNull(::parseBackgroundItem).ifEmpty {
      DesktopJson.stringList(map, "background_urls").mapIndexed { idx, rawUrl ->
        DesktopProfileBackgroundItem(mediaId = "bg_$idx", url = rawUrl)
      }
    }
    return DesktopUserProfile(
      userId = DesktopJson.str(map, "user_id").ifBlank { fallbackId },
      fullName = DesktopJson.str(map, "full_name"),
      username = DesktopJson.str(map, "username"),
      avatarUrl = DesktopJson.str(map, "avatar_url"),
      bio = DesktopJson.str(map, "bio"),
      location = DesktopJson.str(map, "location"),
      countryIso = DesktopJson.str(map, "country_iso"),
      gender = DesktopJson.str(map, "gender"),
      dateOfBirth = DesktopJson.str(map, "date_of_birth"),
      phoneNumber = DesktopJson.str(map, "phone_number"),
      usernameChangedAtMs = DesktopJson.long(map, "username_changed_at_ms", 0L),
      nextUsernameChangeMs = DesktopJson.long(map, "next_username_change_ms", 0L),
      backgrounds = parsedBgs,
      followingCount = DesktopJson.long(map, "following_count", 0L),
      followersCount = DesktopJson.long(map, "followers_count", 0L),
      likesCount = DesktopJson.long(map, "likes_count", 0L),
      isFollowing = DesktopJson.bool(map, "is_following"),
      followsYou = DesktopJson.bool(map, "follows_you"),
      isOwner = DesktopJson.bool(map, "is_owner"),
      isBlocked = DesktopJson.bool(map, "is_blocked"),
      isMuted = DesktopJson.bool(map, "is_muted"),
      isPrivateRestricted = DesktopJson.bool(map, "is_private_restricted"),
      likesVisibility = DesktopJson.str(map, "likes_visibility").ifBlank { "everyone" },
      followingVisibility = DesktopJson.str(map, "following_visibility").ifBlank { "everyone" },
      showActivityStatus = if (map.containsKey("show_activity_status")) DesktopJson.bool(map, "show_activity_status") else true,
      riskScore = DesktopJson.double(map, "risk_score", 0.0),
      posts = DesktopJson.objList(map, "posts").map(::parseFeedPost),
      savedPosts = DesktopJson.objList(map, "saved_posts").map(::parseFeedPost),
      repostedPosts = DesktopJson.objList(map, "reposted_posts").map(::parseFeedPost),
      lockedPosts = DesktopJson.objList(map, "locked_posts").map(::parseFeedPost),
    )
  }

  private fun parseSettingsBundle(map: Map<String, Any?>): DesktopSettingsBundle {
    val pMap = DesktopJson.objOrNull(map, "privacy")
    val nMap = DesktopJson.objOrNull(map, "notifications")
    val cMap = DesktopJson.objOrNull(map, "content_preferences")
    val cpMap = DesktopJson.objOrNull(map, "chat_privacy")
    val spMap = DesktopJson.objOrNull(map, "sync_preferences")
    return DesktopSettingsBundle(
      privacy = if (pMap == null) DesktopPrivacySettings() else DesktopPrivacySettings(
        isPrivateAccount = DesktopJson.bool(pMap, "is_private_account"),
        whoCanComment = DesktopAudienceOption.fromApi(DesktopJson.str(pMap, "who_can_comment")),
        whoCanMention = DesktopAudienceOption.fromApi(DesktopJson.str(pMap, "who_can_mention")),
        whoCanFindYou = DesktopDiscoverabilityOption.fromApi(DesktopJson.str(pMap, "who_can_find_you")),
        showActivityStatus = if (pMap.containsKey("show_activity_status")) DesktopJson.bool(pMap, "show_activity_status") else true,
        likesVisibility = DesktopVisibilityOption.fromApi(DesktopJson.str(pMap, "likes_visibility")),
        followingVisibility = DesktopVisibilityOption.fromApi(DesktopJson.str(pMap, "following_visibility")),
        dataSharingPersonalization = if (pMap.containsKey("data_sharing_personalization")) DesktopJson.bool(pMap, "data_sharing_personalization") else true,
        analyticsSharing = if (pMap.containsKey("analytics_sharing")) DesktopJson.bool(pMap, "analytics_sharing") else true,
        essentialCookies = true,
        personalizationCookies = if (pMap.containsKey("personalization_cookies")) DesktopJson.bool(pMap, "personalization_cookies") else true,
        updatedAtMs = DesktopJson.long(pMap, "updated_at_ms", 0L),
      ),
      notifications = if (nMap == null) DesktopNotificationSettings() else DesktopNotificationSettings(
        masterPause = DesktopJson.bool(nMap, "master_pause"),
        channelPush = if (nMap.containsKey("channel_push")) DesktopJson.bool(nMap, "channel_push") else true,
        channelEmail = if (nMap.containsKey("channel_email")) DesktopJson.bool(nMap, "channel_email") else true,
        channelSms = DesktopJson.bool(nMap, "channel_sms"),
        typeMessages = if (nMap.containsKey("type_messages")) DesktopJson.bool(nMap, "type_messages") else true,
        typeComments = if (nMap.containsKey("type_comments")) DesktopJson.bool(nMap, "type_comments") else true,
        typeFollowers = if (nMap.containsKey("type_followers")) DesktopJson.bool(nMap, "type_followers") else true,
        typeProductUpdates = DesktopJson.bool(nMap, "type_product_updates"),
        quietHoursEnabled = DesktopJson.bool(nMap, "quiet_hours_enabled"),
        quietStartMinutes = DesktopJson.long(nMap, "quiet_start_minutes", 22L * 60L).toInt(),
        quietEndMinutes = DesktopJson.long(nMap, "quiet_end_minutes", 7L * 60L).toInt(),
        updatedAtMs = DesktopJson.long(nMap, "updated_at_ms", 0L),
      ),
      contentPreferences = if (cMap == null) DesktopContentPreferences() else DesktopContentPreferences(
        suggestedContentEnabled = if (cMap.containsKey("suggested_content_enabled")) DesktopJson.bool(cMap, "suggested_content_enabled") else true,
        notInterestedTopics = DesktopJson.stringList(cMap, "not_interested_topics"),
        language = DesktopAppLanguageOption.fromCode(DesktopJson.str(cMap, "language_code")),
        autoplayVideos = if (cMap.containsKey("autoplay_videos")) DesktopJson.bool(cMap, "autoplay_videos") else true,
        videoQuality = DesktopVideoQualityOption.fromApi(DesktopJson.str(cMap, "video_quality")),
        dataSaver = DesktopJson.bool(cMap, "data_saver"),
        captionsEnabled = DesktopJson.bool(cMap, "captions_enabled"),
        reducedMotion = DesktopJson.bool(cMap, "reduced_motion"),
        updatedAtMs = DesktopJson.long(cMap, "updated_at_ms", 0L),
      ),
      chatPrivacy = if (cpMap == null) DesktopChatPrivacySettings() else parseChatPrivacy(cpMap),
      syncPreferences = if (spMap == null) DesktopSyncPreferences() else parseSyncPreferences(spMap),
    )
  }

  private fun parseSyncPreferences(m: Map<String, Any?>): DesktopSyncPreferences = DesktopSyncPreferences(
    autoSyncEnabled = if (m.containsKey("auto_sync_enabled")) DesktopJson.bool(m, "auto_sync_enabled") else true,
    syncOverWifiOnly = DesktopJson.bool(m, "sync_over_wifi_only"),
    backgroundSyncEnabled = if (m.containsKey("background_sync_enabled")) DesktopJson.bool(m, "background_sync_enabled") else true,
    cacheFeedsOffline = if (m.containsKey("cache_feeds_offline")) DesktopJson.bool(m, "cache_feeds_offline") else true,
    cacheChatMediaOffline = if (m.containsKey("cache_chat_media_offline")) DesktopJson.bool(m, "cache_chat_media_offline") else true,
    lastSyncedAtMs = DesktopJson.long(m, "last_synced_at_ms", 0L),
    lastSyncStatus = DesktopJson.str(m, "last_sync_status").ifBlank { "idle" },
    totalSyncedOps = DesktopJson.long(m, "total_synced_ops", 0L),
    updatedAtMs = DesktopJson.long(m, "updated_at_ms", 0L),
  )

  private fun encodeSyncPrefsJson(s: DesktopSyncPreferences): String = buildString {
    append('{')
    append("\"auto_sync_enabled\":").append(s.autoSyncEnabled).append(',')
    append("\"sync_over_wifi_only\":").append(s.syncOverWifiOnly).append(',')
    append("\"background_sync_enabled\":").append(s.backgroundSyncEnabled).append(',')
    append("\"cache_feeds_offline\":").append(s.cacheFeedsOffline).append(',')
    append("\"cache_chat_media_offline\":").append(s.cacheChatMediaOffline).append(',')
    append("\"last_synced_at_ms\":").append(s.lastSyncedAtMs).append(',')
    append("\"last_sync_status\":\"").append(DesktopJson.escape(s.lastSyncStatus)).append("\",")
    append("\"total_synced_ops\":").append(s.totalSyncedOps).append(',')
    append("\"updated_at_ms\":").append(s.updatedAtMs)
    append('}')
  }

  private fun parseChatPrivacy(m: Map<String, Any?>): DesktopChatPrivacySettings = DesktopChatPrivacySettings(
    messageRequests = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "message_requests"), allowContacts = true, fallback = DesktopChatAudienceOption.ANYONE),
    whoCanCall = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "who_can_call"), allowContacts = true, fallback = DesktopChatAudienceOption.ANYONE),
    whoCanScreenshot = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "who_can_screenshot"), allowContacts = false, fallback = DesktopChatAudienceOption.NOBODY),
    whoCanShareChats = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "who_can_share_chats"), allowContacts = false, fallback = DesktopChatAudienceOption.NOBODY),
    whoCanCopyMessages = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "who_can_copy_messages"), allowContacts = false, fallback = DesktopChatAudienceOption.NOBODY),
    whoCanDownloadMedia = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "who_can_download_media"), allowContacts = false, fallback = DesktopChatAudienceOption.ANYONE),
    chatThemeId = DesktopChatPrivacyCatalog.themeById(DesktopJson.str(m, "chat_theme_id")).themeId,
    bubbleColorId = DesktopChatPrivacyCatalog.bubbleColorById(DesktopJson.str(m, "bubble_color_id")).colorId,
    readReceipts = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "read_receipts"), allowContacts = false, fallback = DesktopChatAudienceOption.ANYONE),
    typingIndicators = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "typing_indicators"), allowContacts = false, fallback = DesktopChatAudienceOption.NOBODY),
    onlineIndicators = DesktopChatAudienceOption.fromApi(DesktopJson.str(m, "online_indicators"), allowContacts = false, fallback = DesktopChatAudienceOption.NOBODY),
    updatedAtMs = DesktopJson.long(m, "updated_at_ms", 0L),
  )

  private fun encodeChatPrivacyJson(s: DesktopChatPrivacySettings): String = buildString {
    append('{')
    append("\"message_requests\":\"").append(s.messageRequests.apiValue).append("\",")
    append("\"who_can_call\":\"").append(s.whoCanCall.apiValue).append("\",")
    append("\"who_can_screenshot\":\"").append(s.whoCanScreenshot.apiValue).append("\",")
    append("\"who_can_share_chats\":\"").append(s.whoCanShareChats.apiValue).append("\",")
    append("\"who_can_copy_messages\":\"").append(s.whoCanCopyMessages.apiValue).append("\",")
    append("\"who_can_download_media\":\"").append(s.whoCanDownloadMedia.apiValue).append("\",")
    append("\"chat_theme_id\":\"").append(DesktopJson.escape(s.chatThemeId)).append("\",")
    append("\"bubble_color_id\":\"").append(DesktopJson.escape(s.bubbleColorId)).append("\",")
    append("\"read_receipts\":\"").append(s.readReceipts.apiValue).append("\",")
    append("\"typing_indicators\":\"").append(s.typingIndicators.apiValue).append("\",")
    append("\"online_indicators\":\"").append(s.onlineIndicators.apiValue).append("\",")
    append("\"updated_at_ms\":").append(s.updatedAtMs)
    append('}')
  }

  private fun encodePrivacyJson(s: DesktopPrivacySettings): String = buildString {
    append('{')
    append("\"is_private_account\":").append(s.isPrivateAccount).append(',')
    append("\"who_can_comment\":\"").append(s.whoCanComment.apiValue).append("\",")
    append("\"who_can_mention\":\"").append(s.whoCanMention.apiValue).append("\",")
    append("\"who_can_find_you\":\"").append(s.whoCanFindYou.apiValue).append("\",")
    append("\"show_activity_status\":").append(s.showActivityStatus).append(',')
    append("\"likes_visibility\":\"").append(s.likesVisibility.apiValue).append("\",")
    append("\"following_visibility\":\"").append(s.followingVisibility.apiValue).append("\",")
    append("\"data_sharing_personalization\":").append(s.dataSharingPersonalization).append(',')
    append("\"analytics_sharing\":").append(s.analyticsSharing).append(',')
    append("\"essential_cookies\":true,")
    append("\"personalization_cookies\":").append(s.personalizationCookies).append(',')
    append("\"updated_at_ms\":").append(s.updatedAtMs)
    append('}')
  }

  private fun encodeNotificationsJson(s: DesktopNotificationSettings): String = buildString {
    append('{')
    append("\"master_pause\":").append(s.masterPause).append(',')
    append("\"channel_push\":").append(s.channelPush).append(',')
    append("\"channel_email\":").append(s.channelEmail).append(',')
    append("\"channel_sms\":").append(s.channelSms).append(',')
    append("\"type_messages\":").append(s.typeMessages).append(',')
    append("\"type_comments\":").append(s.typeComments).append(',')
    append("\"type_followers\":").append(s.typeFollowers).append(',')
    append("\"type_product_updates\":").append(s.typeProductUpdates).append(',')
    append("\"quiet_hours_enabled\":").append(s.quietHoursEnabled).append(',')
    append("\"quiet_start_minutes\":").append(s.quietStartMinutes).append(',')
    append("\"quiet_end_minutes\":").append(s.quietEndMinutes).append(',')
    append("\"updated_at_ms\":").append(s.updatedAtMs)
    append('}')
  }

  private fun encodeContentPrefsJson(s: DesktopContentPreferences, resetSuggestions: Boolean): String = buildString {
    append('{')
    append("\"suggested_content_enabled\":").append(s.suggestedContentEnabled).append(',')
    append("\"not_interested_topics\":[").append(s.notInterestedTopics.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }).append("],")
    append("\"language_code\":\"").append(s.language.code).append("\",")
    append("\"autoplay_videos\":").append(s.autoplayVideos).append(',')
    append("\"video_quality\":\"").append(s.videoQuality.apiValue).append("\",")
    append("\"data_saver\":").append(s.dataSaver).append(',')
    append("\"captions_enabled\":").append(s.captionsEnabled).append(',')
    append("\"reduced_motion\":").append(s.reducedMotion).append(',')
    append("\"reset_suggestions\":").append(resetSuggestions).append(',')
    append("\"updated_at_ms\":").append(s.updatedAtMs)
    append('}')
  }

  private fun parseRestrictedAccount(m: Map<String, Any?>, isMuted: Boolean): DesktopRestrictedAccount? {
    val uid = DesktopJson.str(m, "user_id")
    if (uid.isBlank()) return null
    return DesktopRestrictedAccount(
      userId = uid,
      fullName = DesktopJson.str(m, "full_name"),
      username = DesktopJson.str(m, "username"),
      avatarUrl = DesktopJson.str(m, "avatar_url"),
      isMuted = isMuted,
      createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
    )
  }

  private fun parseTicket(m: Map<String, Any?>, synced: Boolean): DesktopSupportTicket {
    val id = DesktopJson.str(m, "ticket_id").ifBlank { DesktopJson.str(m, "reference_number") }
    return DesktopSupportTicket(
      ticketId = id,
      referenceNumber = DesktopJson.str(m, "reference_number").ifBlank { id },
      category = DesktopJson.str(m, "category"),
      description = DesktopJson.str(m, "description"),
      screenshotSha256 = DesktopJson.str(m, "screenshot_sha256"),
      status = DesktopJson.str(m, "status").ifBlank { "open" },
      resolutionNote = DesktopJson.str(m, "resolution_note"),
      isSynced = synced,
      createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
    )
  }

  private fun parseFeedPost(map: Map<String, Any?>): DesktopFeedPost {
    val modeStr = DesktopJson.str(map, "mode").lowercase()
    val mode = SharedPostMode.entries.firstOrNull { it.apiValue == modeStr } ?: SharedPostMode.VIDEO
    val fontStr = DesktopJson.str(map, "font_id").lowercase()
    val font = SharedPostFont.entries.firstOrNull { it.id == fontStr } ?: SharedPostFont.CLASSIC
    val songObj = DesktopJson.objOrNull(map, "song")
    val song = songObj?.let { s ->
      val title = DesktopJson.str(s, "title")
      if (title.isBlank()) null
      else SharedSongTrack(
        trackId = DesktopJson.str(s, "track_id"),
        title = title,
        artist = DesktopJson.str(s, "artist"),
        album = DesktopJson.str(s, "album"),
        artworkUrl = DesktopJson.str(s, "artwork_url"),
        previewUrl = DesktopJson.str(s, "preview_url"),
        durationMs = DesktopJson.long(s, "duration_ms", 0L),
      )
    }
    val rawLabel = DesktopJson.str(map, "recommendation_label")
    return DesktopFeedPost(
      postId = DesktopJson.str(map, "post_id"),
      authorUserId = DesktopJson.str(map, "author_user_id"),
      authorUsername = DesktopJson.str(map, "author_username"),
      authorFullName = DesktopJson.str(map, "author_full_name"),
      authorAvatarUrl = DesktopJson.str(map, "author_avatar_url"),
      isFollowingAuthor = DesktopJson.bool(map, "is_following_author"),
      isOwner = DesktopJson.bool(map, "is_owner"),
      privacy = DesktopJson.str(map, "privacy").ifBlank { "public" },
      mode = mode,
      textContent = DesktopJson.str(map, "text_content"),
      caption = DesktopJson.str(map, "caption"),
      themeId = DesktopJson.str(map, "theme_id").ifBlank { "crimson" },
      fontOption = font,
      song = song,
      hashtags = DesktopJson.stringList(map, "hashtags"),
      photoUrls = DesktopJson.stringList(map, "photo_urls"),
      videoUrl = DesktopJson.str(map, "video_url"),
      thumbnailUrl = DesktopJson.str(map, "thumbnail_url"),
      mediaWidth = DesktopJson.long(map, "width", 1080L).toInt().coerceAtLeast(1),
      mediaHeight = DesktopJson.long(map, "height", 1920L).toInt().coerceAtLeast(1),
      likesCount = DesktopJson.long(map, "likes_count", 0L),
      commentsCount = DesktopJson.long(map, "comments_count", 0L),
      sharesCount = DesktopJson.long(map, "shares_count", 0L),
      favouritesCount = DesktopJson.long(map, "favourites_count", 0L),
      repostsCount = DesktopJson.long(map, "reposts_count", 0L),
      viewsCount = DesktopJson.long(map, "views_count", 0L),
      isLiked = DesktopJson.bool(map, "is_liked"),
      isFavourited = DesktopJson.bool(map, "is_favourited"),
      isReposted = DesktopJson.bool(map, "is_reposted"),
      repostedByUsername = DesktopJson.str(map, "reposted_by_username"),
      recommendationLabel = rawLabel,
      muteOriginalAudio = DesktopJson.bool(map, "mute_original_audio"),
      createdAtMs = DesktopJson.long(map, "created_at_ms", 0L),
    )
  }

  private fun parseError(status: Int, body: String): String {
    val map = runCatching { DesktopJson.parseObject(body) }.getOrNull()
    val err = map?.let { DesktopJson.str(it, "error") }.orEmpty()
    if (err.isNotBlank()) return err
    return if (status == 429) "Try again later" else "Unable to complete request ($status)"
  }

  fun currentSessionSnapshot(): DesktopStoredSession = session
  fun currentBaseUrl(): String? = baseUrl()
  fun currentDeviceFp(): String = deviceFingerprint
  fun currentHttpClient(): HttpClient = httpClient

  fun executeChatRaw(
    method: String,
    path: String,
    jsonBody: String? = null,
    extraHeaders: Map<String, String> = emptyMap(),
  ): Pair<Int, String>? = executeRaw(
    method = method,
    path = path,
    body = jsonBody?.toByteArray(StandardCharsets.UTF_8),
    auth = true,
    extraHeaders = extraHeaders,
  )

  fun registerPushToken(token: String = "desktop_${deviceFingerprint.take(24)}"): Boolean {
    if (session.accessToken.isBlank() || token.isBlank()) return false
    val body = "{\"platform\":\"desktop\",\"token\":\"${DesktopJson.escape(token)}\",\"app_version\":\"1.0\"}"
    val resp = executeRaw("POST", "/v1/notifications/push-token", body.toByteArray(StandardCharsets.UTF_8), auth = true)
    return resp != null && resp.first in 200..299
  }

  fun unregisterPushToken(allDevices: Boolean = false): Boolean {
    if (session.accessToken.isBlank()) return false
    val resp = executeRaw("DELETE", "/v1/notifications/push-token?all_devices=$allDevices", null, auth = true, allowRefreshRetry = false)
    return resp != null && resp.first in 200..299
  }

  fun listNotifications(limit: Int = 60): Pair<List<DesktopNotificationItem>, Int>? {
    if (session.accessToken.isBlank()) return null
    val resp = executeRaw("GET", "/v1/notifications?limit=$limit", null, auth = true) ?: return null
    if (resp.first !in 200..299) return null
    val root = DesktopJson.parseObject(resp.second)
    val unread = DesktopJson.long(root, "unread_count", 0L).toInt()
    val items = DesktopJson.objList(root, "items").map { m ->
      val isVo = DesktopJson.bool(m, "is_view_once")
      val rawBody = DesktopJson.str(m, "body")
      DesktopNotificationItem(
        id = DesktopJson.str(m, "id"),
        actorUserId = DesktopJson.str(m, "actor_user_id"),
        actorName = DesktopJson.str(m, "actor_name"),
        actorUsername = DesktopJson.str(m, "actor_username"),
        actorAvatarUrl = DesktopJson.str(m, "actor_avatar_url"),
        kind = DesktopJson.str(m, "kind"),
        title = DesktopJson.str(m, "title"),
        body = if (isVo) "Sent a view-once voice message" else rawBody,
        conversationId = DesktopJson.str(m, "conversation_id"),
        messageId = DesktopJson.str(m, "message_id"),
        postId = DesktopJson.str(m, "post_id"),
        targetUserId = DesktopJson.str(m, "target_user_id"),
        isViewOnce = isVo,
        isRead = DesktopJson.bool(m, "is_read"),
        createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
      )
    }
    return items to unread
  }

  fun markNotificationRead(notificationId: String = "", conversationId: String = ""): Boolean {
    if (session.accessToken.isBlank()) return false
    val body = "{\"notification_id\":\"${DesktopJson.escape(notificationId)}\",\"conversation_id\":\"${DesktopJson.escape(conversationId)}\"}"
    val resp = executeRaw("POST", "/v1/notifications/read", body.toByteArray(StandardCharsets.UTF_8), auth = true)
    return resp != null && resp.first in 200..299
  }

  fun markAllNotificationsRead(): Boolean {
    if (session.accessToken.isBlank()) return false
    val resp = executeRaw("POST", "/v1/notifications/read-all", "{}".toByteArray(StandardCharsets.UTF_8), auth = true)
    return resp != null && resp.first in 200..299
  }

  fun uploadVoiceMediaMultipart(
    conversationId: String,
    durationMs: Long,
    viewOnce: Boolean,
    audioBytes: ByteArray,
    filename: String,
  ): Pair<Int, String>? {
    val boundary = "TelefamVoiceBoundary${UUID.randomUUID().toString().replace("-", "")}"
    val baos = ByteArrayOutputStream()
    fun appendPart(name: String, value: String) {
      baos.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray(StandardCharsets.UTF_8))
    }
    appendPart("conversation_id", conversationId)
    appendPart("duration_ms", durationMs.toString())
    appendPart("view_once", viewOnce.toString())
    val mime = if (filename.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/mp4"
    baos.write(
      "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\nContent-Type: $mime\r\n\r\n"
        .toByteArray(StandardCharsets.UTF_8),
    )
    baos.write(audioBytes)
    baos.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
    return executeRaw(
      method = "POST",
      path = "/v1/chat/media/voice",
      body = baos.toByteArray(),
      auth = true,
      contentType = "multipart/form-data; boundary=$boundary",
    )
  }

  fun downloadMediaBytes(rawUrl: String): ByteArray? {
    val clean = rawUrl.trim()
    if (clean.isBlank()) return null
    val resolved = when {
      clean.startsWith("http://") || clean.startsWith("https://") -> clean
      clean.startsWith("/") -> "${baseUrl() ?: return null}$clean"
      else -> "${baseUrl() ?: return null}/$clean"
    }
    val reqBuilder = HttpRequest.newBuilder(URI(resolved))
      .timeout(Duration.ofSeconds(30))
      .GET()
    val base = baseUrl().orEmpty()
    if (base.isNotBlank() && resolved.startsWith(base) && session.accessToken.isNotBlank()) {
      reqBuilder.header("Authorization", "Bearer ${session.accessToken}")
      reqBuilder.header("X-Telefam-Device-FP", deviceFingerprint)
    }
    val res = runCatching {
      httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
    }.getOrNull() ?: return null
    return if (res.statusCode() in 200..299) res.body() else null
  }

  private val linkPreviewMemoryCache = linkedMapOf<String, DesktopLinkPreview>()

  fun fetchLinkPreview(rawUrl: String): DesktopLinkPreview {
    val safeUrl = telefam.company.shared.SmartTextDetector.normalizeSafeHttpUrl(rawUrl)
      ?: return DesktopLinkPreview(
        normalizedUrl = rawUrl.trim(),
        domain = telefam.company.shared.SmartTextDetector.extractCleanDomain(rawUrl),
        title = telefam.company.shared.SmartTextDetector.extractCleanDomain(rawUrl),
        status = "failed",
        fetchedAtMs = System.currentTimeMillis(),
      )
    val domain = telefam.company.shared.SmartTextDetector.extractCleanDomain(safeUrl)
    synchronized(linkPreviewMemoryCache) {
      val cached = linkPreviewMemoryCache[safeUrl]
      if (cached != null && (System.currentTimeMillis() - cached.fetchedAtMs) < 6 * 3600_000L) {
        return cached
      }
    }
    val diskKey = "preview_${DesktopSecurityGuard.sha256Hex(safeUrl)}.json"
    loadCachedText(diskKey)?.let { rawJson ->
      val parsed = parseLinkPreviewMap(DesktopJson.parseObject(rawJson), safeUrl, domain)
      if (parsed.status == "ready" && (System.currentTimeMillis() - parsed.fetchedAtMs) < 6 * 3600_000L) {
        synchronized(linkPreviewMemoryCache) { linkPreviewMemoryCache[safeUrl] = parsed }
        return parsed
      }
    }

    val encoded = URLEncoder.encode(safeUrl, StandardCharsets.UTF_8)
    val res = executeRaw("GET", "/v1/links/preview?url=$encoded", null, auth = true)
    val preview = if (res != null && res.first in 200..299) {
      val map = DesktopJson.parseObject(res.second)
      val inner = DesktopJson.objOrNull(map, "preview") ?: map
      parseLinkPreviewMap(inner, safeUrl, domain).copy(fetchedAtMs = System.currentTimeMillis())
    } else {
      DesktopLinkPreview(
        normalizedUrl = safeUrl,
        domain = domain,
        title = domain.ifBlank { safeUrl },
        description = "",
        status = "fallback",
        fetchedAtMs = System.currentTimeMillis(),
      )
    }

    synchronized(linkPreviewMemoryCache) {
      if (linkPreviewMemoryCache.size > 200) {
        val oldest = linkPreviewMemoryCache.keys.firstOrNull()
        if (oldest != null) linkPreviewMemoryCache.remove(oldest)
      }
      linkPreviewMemoryCache[safeUrl] = preview
    }
    if (preview.status == "ready") {
      val json = buildString {
        append("{")
        append("\"normalized_url\":\"${DesktopJson.escape(preview.normalizedUrl)}\",")
        append("\"domain\":\"${DesktopJson.escape(preview.domain)}\",")
        append("\"title\":\"${DesktopJson.escape(preview.title)}\",")
        append("\"description\":\"${DesktopJson.escape(preview.description)}\",")
        append("\"image_url\":\"${DesktopJson.escape(preview.imageUrl)}\",")
        append("\"favicon_url\":\"${DesktopJson.escape(preview.faviconUrl)}\",")
        append("\"status\":\"${DesktopJson.escape(preview.status)}\",")
        append("\"fetched_at_ms\":${preview.fetchedAtMs}")
        append("}")
      }
      saveCachedText(diskKey, json)
    }
    return preview
  }

  private fun parseLinkPreviewMap(map: Map<String, Any?>, fallbackUrl: String, fallbackDomain: String): DesktopLinkPreview {
    val norm = DesktopJson.str(map, "normalized_url").ifBlank { fallbackUrl }
    val dom = DesktopJson.str(map, "domain").ifBlank { fallbackDomain }
    val title = DesktopJson.str(map, "title").ifBlank { dom.ifBlank { norm } }
    val desc = DesktopJson.str(map, "description")
    val img = DesktopJson.str(map, "image_url")
    val fav = DesktopJson.str(map, "favicon_url")
    val st = DesktopJson.str(map, "status").ifBlank { "ready" }
    val ts = DesktopJson.long(map, "fetched_at_ms", System.currentTimeMillis())
    return DesktopLinkPreview(
      normalizedUrl = norm,
      domain = dom,
      title = title,
      description = desc,
      imageUrl = img,
      faviconUrl = fav,
      status = st,
      fetchedAtMs = ts,
    )
  }

  private fun savedLinksCacheKey(): String? {
    val uid = session.userId.trim()
    return if (uid.isBlank()) null else "saved_links_$uid.json"
  }

  fun loadCachedSavedLinks(): List<DesktopSavedLink> {
    val key = savedLinksCacheKey() ?: return emptyList()
    val raw = loadCachedText(key) ?: return emptyList()
    val root = DesktopJson.parseObject(raw)
    return DesktopJson.objList(root, "items").map { parseSavedLinkMap(it) }
  }

  private fun persistSavedLinksCache(items: List<DesktopSavedLink>) {
    val key = savedLinksCacheKey() ?: return
    val json = buildString {
      append("{\"items\":[")
      items.forEachIndexed { idx, item ->
        if (idx > 0) append(",")
        append("{")
        append("\"id\":\"${DesktopJson.escape(item.id)}\",")
        append("\"url\":\"${DesktopJson.escape(item.url)}\",")
        append("\"normalized_url\":\"${DesktopJson.escape(item.normalizedUrl)}\",")
        append("\"domain\":\"${DesktopJson.escape(item.domain)}\",")
        append("\"title\":\"${DesktopJson.escape(item.title)}\",")
        append("\"description\":\"${DesktopJson.escape(item.description)}\",")
        append("\"preview_image_url\":\"${DesktopJson.escape(item.previewImageUrl)}\",")
        append("\"favicon_url\":\"${DesktopJson.escape(item.faviconUrl)}\",")
        append("\"source\":\"${DesktopJson.escape(item.source)}\",")
        append("\"saved_at_ms\":${item.savedAtMs}")
        append("}")
      }
      append("]}")
    }
    saveCachedText(key, json)
  }

  private fun parseSavedLinkMap(m: Map<String, Any?>): DesktopSavedLink {
    val url = DesktopJson.str(m, "url").ifBlank { DesktopJson.str(m, "normalized_url") }
    val norm = DesktopJson.str(m, "normalized_url").ifBlank {
      telefam.company.shared.SmartTextDetector.normalizeSafeHttpUrl(url) ?: url
    }
    val dom = DesktopJson.str(m, "domain").ifBlank {
      telefam.company.shared.SmartTextDetector.extractCleanDomain(norm)
    }
    return DesktopSavedLink(
      id = DesktopJson.str(m, "id"),
      url = url,
      normalizedUrl = norm,
      domain = dom,
      title = DesktopJson.str(m, "title").ifBlank { dom.ifBlank { norm } },
      description = DesktopJson.str(m, "description"),
      previewImageUrl = DesktopJson.str(m, "preview_image_url"),
      faviconUrl = DesktopJson.str(m, "favicon_url"),
      source = DesktopJson.str(m, "source").ifBlank { "browser" },
      savedAtMs = DesktopJson.long(m, "saved_at_ms", System.currentTimeMillis()),
    )
  }

  fun listSavedLinks(query: String = ""): DesktopApiOutcome<List<DesktopSavedLink>> {
    val q = query.trim()
    val path = if (q.isNotBlank()) {
      "/v1/links/saved?q=${URLEncoder.encode(q, StandardCharsets.UTF_8)}&limit=150"
    } else {
      "/v1/links/saved?limit=150"
    }
    val res = executeRaw("GET", path, null, auth = true)
    if (res != null && res.first in 200..299) {
      val map = DesktopJson.parseObject(res.second)
      val items = DesktopJson.objList(map, "items").map { parseSavedLinkMap(it) }
      if (q.isEmpty()) persistSavedLinksCache(items)
      return DesktopApiOutcome.Ok(items)
    }
    val cached = loadCachedSavedLinks()
    val filtered = if (q.isEmpty()) cached else cached.filter {
      it.title.contains(q, ignoreCase = true) ||
        it.domain.contains(q, ignoreCase = true) ||
        it.url.contains(q, ignoreCase = true) ||
        it.source.contains(q, ignoreCase = true)
    }
    return DesktopApiOutcome.Ok(filtered)
  }

  fun saveLink(
    rawUrl: String,
    titleHint: String = "",
    source: String = "browser",
    preview: DesktopLinkPreview? = null,
  ): DesktopApiOutcome<DesktopSavedLink> {
    val safeUrl = telefam.company.shared.SmartTextDetector.normalizeSafeHttpUrl(rawUrl)
      ?: return DesktopApiOutcome.Fail("Blocked or invalid link URL")
    val resolvedPreview = preview ?: fetchLinkPreview(safeUrl)
    val finalTitle = titleHint.trim().ifBlank { resolvedPreview.title }.ifBlank { resolvedPreview.domain }
    val body = buildString {
      append("{")
      append("\"url\":\"${DesktopJson.escape(safeUrl)}\",")
      append("\"title\":\"${DesktopJson.escape(finalTitle)}\",")
      append("\"description\":\"${DesktopJson.escape(resolvedPreview.description)}\",")
      append("\"preview_image_url\":\"${DesktopJson.escape(resolvedPreview.imageUrl)}\",")
      append("\"favicon_url\":\"${DesktopJson.escape(resolvedPreview.faviconUrl)}\",")
      append("\"source\":\"${DesktopJson.escape(source.ifBlank { "browser" })}\"")
      append("}")
    }.toByteArray(StandardCharsets.UTF_8)
    val res = executeRaw("POST", "/v1/links/saved", body, auth = true)
    if (res != null && res.first in 200..299) {
      val map = DesktopJson.parseObject(res.second)
      val savedObj = DesktopJson.objOrNull(map, "item") ?: map
      val saved = parseSavedLinkMap(savedObj)
      val merged = (listOf(saved) + loadCachedSavedLinks().filterNot { it.normalizedUrl == saved.normalizedUrl })
      persistSavedLinksCache(merged)
      return DesktopApiOutcome.Ok(saved)
    }
    val errMsg = res?.second?.let { DesktopJson.str(DesktopJson.parseObject(it), "error") }.orEmpty()
      .ifBlank { "Unable to save link right now" }
    return DesktopApiOutcome.Fail(errMsg)
  }

  fun deleteSavedLink(linkIdOrUrl: String): DesktopApiOutcome<Unit> {
    val encoded = URLEncoder.encode(linkIdOrUrl, StandardCharsets.UTF_8)
    val res = executeRaw("DELETE", "/v1/links/saved/$encoded", null, auth = true)
    if (res != null && res.first in 200..299) {
      val remaining = loadCachedSavedLinks().filterNot {
        it.id == linkIdOrUrl || it.normalizedUrl == linkIdOrUrl
      }
      persistSavedLinksCache(remaining)
      return DesktopApiOutcome.Ok(Unit)
    }
    val errMsg = res?.second?.let { DesktopJson.str(DesktopJson.parseObject(it), "error") }.orEmpty()
      .ifBlank { "Unable to remove saved link" }
    return DesktopApiOutcome.Fail(errMsg)
  }

  private fun executeRaw(
    method: String,
    path: String,
    body: ByteArray?,
    auth: Boolean,
    contentType: String = "application/json",
    extraHeaders: Map<String, String> = emptyMap(),
    allowRefreshRetry: Boolean = true,
  ): Pair<Int, String>? {
    if (DesktopSecurityGuard.isDebuggerAttached()) {
      return 403 to "{\"error\":\"Security verification failed\"}"
    }
    val base = baseUrl() ?: return null
    val fullUrl = "$base$path"
    val builder = HttpRequest.newBuilder(URI(fullUrl))
      .timeout(Duration.ofSeconds(30))
      .header("Accept-Encoding", "gzip")
      .header("X-Telefam-Device-FP", deviceFingerprint)
      .header("X-Telefam-Client-IP-Hint", ipHint)
    DesktopDeviceContextCollector.toHeaders().forEach { (k, v) ->
      if (v.isNotBlank() && !extraHeaders.containsKey(k)) {
        builder.header(k, v)
      }
    }
    if (auth && session.accessToken.isNotBlank()) {
      builder.header("Authorization", "Bearer ${session.accessToken}")
    }
    extraHeaders.forEach { (k, v) -> builder.header(k, v) }
    when (method) {
      "GET" -> builder.GET()
      "DELETE" -> builder.DELETE()
      else -> {
        builder.header("Content-Type", contentType)
        val rawBytes = body ?: ByteArray(0)
        val shouldGzip = rawBytes.size >= 256 &&
          contentType.contains("json", ignoreCase = true) &&
          !extraHeaders.containsKey("Content-Encoding")
        val finalBytes = if (shouldGzip) {
          val baos = ByteArrayOutputStream()
          GZIPOutputStream(baos).use { gz -> gz.write(rawBytes) }
          val compressed = baos.toByteArray()
          if (compressed.size < rawBytes.size) {
            builder.header("Content-Encoding", "gzip")
            compressed
          } else {
            rawBytes
          }
        } else {
          rawBytes
        }
        val pub = HttpRequest.BodyPublishers.ofByteArray(finalBytes)
        builder.method(method, pub)
      }
    }
    val response = runCatching {
      httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
    }.getOrNull() ?: return null

    if (response.statusCode() == 401 && auth && allowRefreshRetry && session.refreshToken.isNotBlank()) {
      if (attemptRefreshToken()) {
        return executeRaw(method, path, body, auth, contentType, extraHeaders, allowRefreshRetry = false)
      }
    }
    val enc = response.headers().firstValue("Content-Encoding").orElse("").lowercase()
    val rawRespBytes = response.body() ?: ByteArray(0)
    val decodedBytes = if (enc.contains("gzip") && rawRespBytes.isNotEmpty()) {
      runCatching { GZIPInputStream(rawRespBytes.inputStream()).use { it.readBytes() } }.getOrDefault(rawRespBytes)
    } else {
      rawRespBytes
    }
    return response.statusCode() to String(decodedBytes, StandardCharsets.UTF_8)
  }

  private fun attemptRefreshToken(): Boolean {
    val base = baseUrl() ?: return false
    val rt = session.refreshToken
    if (rt.isBlank()) return false
    val body = "{\"refresh_token\":\"${DesktopJson.escape(rt)}\"}"
    val req = HttpRequest.newBuilder(URI("$base/v1/auth/token/refresh"))
      .timeout(Duration.ofSeconds(15))
      .header("Content-Type", "application/json")
      .header("X-Telefam-Device-FP", deviceFingerprint)
      .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
      .build()
    val res = runCatching {
      httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }.getOrNull() ?: return false
    if (res.statusCode() !in 200..299) {
      signOut()
      return false
    }
    val map = DesktopJson.parseObject(res.body())
    val nextAccess = DesktopJson.str(map, "session_token")
    val nextRefresh = DesktopJson.str(map, "refresh_token")
    if (nextAccess.isBlank() || nextRefresh.isBlank()) return false
    val updated = session.copy(accessToken = nextAccess, refreshToken = nextRefresh)
    session = updated
    vault.save(updated)
    onSessionUpdated(updated)
    return true
  }
}

/** Minimal zero-dependency RFC 8259 JSON parser & escaper for Desktop KMP JVM. */
object DesktopJson {
  fun escape(raw: String): String = buildString(raw.length + 8) {
    for (ch in raw) {
      when (ch) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        '\b' -> append("\\b")
        '\u000C' -> append("\\f")
        else -> append(ch)
      }
    }
  }

  fun str(map: Map<String, Any?>, key: String): String = (map[key] as? String).orEmpty()
  fun bool(map: Map<String, Any?>, key: String): Boolean = map[key] as? Boolean ?: false
  fun long(map: Map<String, Any?>, key: String, default: Long = 0L): Long =
    (map[key] as? Number)?.toLong() ?: default
  fun double(map: Map<String, Any?>, key: String, default: Double = 0.0): Double =
    (map[key] as? Number)?.toDouble() ?: default

  @Suppress("UNCHECKED_CAST")
  fun obj(map: Map<String, Any?>, key: String): Map<String, Any?> =
    (map[key] as? Map<String, Any?>).orEmpty()

  @Suppress("UNCHECKED_CAST")
  fun objOrNull(map: Map<String, Any?>, key: String): Map<String, Any?>? =
    map[key] as? Map<String, Any?>

  @Suppress("UNCHECKED_CAST")
  fun objList(map: Map<String, Any?>, key: String): List<Map<String, Any?>> =
    (map[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty()

  fun stringList(map: Map<String, Any?>, key: String): List<String> =
    (map[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

  @Suppress("UNCHECKED_CAST")
  fun parseObject(json: String): Map<String, Any?> {
    val parser = Parser(json)
    return (parser.parseValue() as? Map<String, Any?>).orEmpty()
  }

  private class Parser(private val src: String) {
    private var idx = 0

    private fun skipWs() {
      while (idx < src.length && src[idx].isWhitespace()) idx++
    }

    fun parseValue(): Any? {
      skipWs()
      if (idx >= src.length) return null
      return when (src[idx]) {
        '{' -> parseObject()
        '[' -> parseArray()
        '"' -> parseString()
        't', 'f' -> parseBoolean()
        'n' -> parseNull()
        else -> parseNumber()
      }
    }

    private fun parseObject(): Map<String, Any?> {
      val map = linkedMapOf<String, Any?>()
      idx++ // '{'
      skipWs()
      if (idx < src.length && src[idx] == '}') {
        idx++
        return map
      }
      while (idx < src.length) {
        skipWs()
        val key = parseString()
        skipWs()
        if (idx < src.length && src[idx] == ':') idx++
        val value = parseValue()
        map[key] = value
        skipWs()
        if (idx < src.length && src[idx] == ',') {
          idx++
        } else {
          if (idx < src.length && src[idx] == '}') idx++
          break
        }
      }
      return map
    }

    private fun parseArray(): List<Any?> {
      val list = mutableListOf<Any?>()
      idx++ // '['
      skipWs()
      if (idx < src.length && src[idx] == ']') {
        idx++
        return list
      }
      while (idx < src.length) {
        list.add(parseValue())
        skipWs()
        if (idx < src.length && src[idx] == ',') {
          idx++
        } else {
          if (idx < src.length && src[idx] == ']') idx++
          break
        }
      }
      return list
    }

    private fun parseString(): String {
      if (idx >= src.length || src[idx] != '"') return ""
      idx++
      val sb = StringBuilder()
      while (idx < src.length) {
        val ch = src[idx++]
        if (ch == '"') break
        if (ch == '\\' && idx < src.length) {
          when (val esc = src[idx++]) {
            '"', '\\', '/' -> sb.append(esc)
            'n' -> sb.append('\n')
            'r' -> sb.append('\r')
            't' -> sb.append('\t')
            'b' -> sb.append('\b')
            'f' -> sb.append('\u000C')
            'u' -> {
              if (idx + 4 <= src.length) {
                val hex = src.substring(idx, idx + 4)
                sb.append(hex.toInt(16).toChar())
                idx += 4
              }
            }
            else -> sb.append(esc)
          }
        } else {
          sb.append(ch)
        }
      }
      return sb.toString()
    }

    private fun parseBoolean(): Boolean {
      return if (src.startsWith("true", idx)) {
        idx += 4
        true
      } else {
        idx += 5
        false
      }
    }

    private fun parseNull(): Any? {
      idx += 4
      return null
    }

    private fun parseNumber(): Number {
      val start = idx
      while (idx < src.length && (src[idx].isDigit() || src[idx] in listOf('-', '+', '.', 'e', 'E'))) {
        idx++
      }
      val slice = src.substring(start, idx)
      return if (slice.contains('.') || slice.contains('e') || slice.contains('E')) {
        slice.toDoubleOrNull() ?: 0.0
      } else {
        slice.toLongOrNull() ?: 0L
      }
    }
  }
}

data class DesktopNotificationItem(
  val id: String,
  val actorUserId: String,
  val actorName: String,
  val actorUsername: String,
  val actorAvatarUrl: String,
  val kind: String,
  val title: String,
  val body: String,
  val conversationId: String,
  val messageId: String,
  val postId: String,
  val targetUserId: String,
  val isViewOnce: Boolean,
  val isRead: Boolean,
  val createdAtMs: Long,
)
