package telefam.company.core

enum class SettingsDestination {
  HUB,
  PRIVACY,
  SHARE_PROFILE,
  SAVED_LINKS,
  NOTIFICATIONS,
  CONTENT_PREFERENCES,
  OFFLINE_SYNC,
  BLOCKED_ACCOUNTS,
  REPORT_PROBLEM,
  SAFETY_CENTER,
  HELP_SUPPORT,
  TERMS_POLICIES,
  ABOUT,
}

enum class AudienceOption(val apiValue: String, val label: String) {
  EVERYONE("everyone", "Everyone"),
  FRIENDS("friends", "Friends"),
  NO_ONE("no_one", "No one");

  companion object {
    fun fromApi(raw: String?): AudienceOption =
      entries.firstOrNull { it.apiValue.equals(raw?.trim(), ignoreCase = true) } ?: EVERYONE
  }
}

enum class DiscoverabilityOption(val apiValue: String, val label: String) {
  EVERYONE("everyone", "Everyone"),
  CONTACTS("contacts", "Contacts only"),
  NO_ONE("no_one", "No one");

  companion object {
    fun fromApi(raw: String?): DiscoverabilityOption =
      entries.firstOrNull { it.apiValue.equals(raw?.trim(), ignoreCase = true) } ?: EVERYONE
  }
}

enum class VisibilityOption(val apiValue: String, val label: String) {
  EVERYONE("everyone", "Everyone"),
  FRIENDS("friends", "Friends"),
  ONLY_ME("only_me", "Only me");

  companion object {
    fun fromApi(raw: String?): VisibilityOption =
      entries.firstOrNull { it.apiValue.equals(raw?.trim(), ignoreCase = true) } ?: EVERYONE
  }
}

enum class VideoQualityOption(val apiValue: String, val label: String) {
  AUTO("auto", "Auto"),
  HIGH("high", "High"),
  DATA_SAVER("data_saver", "Data saver");

  companion object {
    fun fromApi(raw: String?): VideoQualityOption =
      entries.firstOrNull { it.apiValue.equals(raw?.trim(), ignoreCase = true) } ?: AUTO
  }
}

enum class AppLanguageOption(val code: String, val label: String) {
  EN("en", "English"),
  SW("sw", "Kiswahili"),
  FR("fr", "Français"),
  ES("es", "Español"),
  AR("ar", "العربية"),
  PT("pt", "Português"),
  DE("de", "Deutsch"),
  HI("hi", "हिन्दी");

  companion object {
    fun fromCode(raw: String?): AppLanguageOption =
      entries.firstOrNull { it.code.equals(raw?.trim(), ignoreCase = true) } ?: EN
  }
}

data class PrivacySettingsState(
  val isPrivateAccount: Boolean = false,
  val whoCanComment: AudienceOption = AudienceOption.EVERYONE,
  val whoCanMention: AudienceOption = AudienceOption.EVERYONE,
  val whoCanFindYou: DiscoverabilityOption = DiscoverabilityOption.EVERYONE,
  val showActivityStatus: Boolean = true,
  val likesVisibility: VisibilityOption = VisibilityOption.EVERYONE,
  val followingVisibility: VisibilityOption = VisibilityOption.EVERYONE,
  val dataSharingPersonalization: Boolean = true,
  val analyticsSharing: Boolean = true,
  val essentialCookies: Boolean = true,
  val personalizationCookies: Boolean = true,
  val updatedAtMs: Long = 0L,
)

data class NotificationSettingsState(
  val masterPause: Boolean = false,
  val channelPush: Boolean = true,
  val channelEmail: Boolean = true,
  val channelSms: Boolean = false,
  val typeMessages: Boolean = true,
  val typeComments: Boolean = true,
  val typeFollowers: Boolean = true,
  val typeProductUpdates: Boolean = false,
  val quietHoursEnabled: Boolean = false,
  val quietStartMinutes: Int = 22 * 60,
  val quietEndMinutes: Int = 7 * 60,
  val updatedAtMs: Long = 0L,
) {
  fun formatTime(minutesOfDay: Int): String {
    val clamped = ((minutesOfDay % 1440) + 1440) % 1440
    val h = clamped / 60
    val m = clamped % 60
    return String.format("%02d:%02d", h, m)
  }
}

data class ContentPreferencesState(
  val suggestedContentEnabled: Boolean = true,
  val notInterestedTopics: List<String> = emptyList(),
  val language: AppLanguageOption = AppLanguageOption.EN,
  val autoplayVideos: Boolean = true,
  val videoQuality: VideoQualityOption = VideoQualityOption.AUTO,
  val dataSaver: Boolean = false,
  val captionsEnabled: Boolean = false,
  val reducedMotion: Boolean = false,
  val updatedAtMs: Long = 0L,
) {
  val effectiveNetworkTierHint: VideoQualityOption
    get() = if (dataSaver) VideoQualityOption.DATA_SAVER else videoQuality
}

data class SyncPreferencesState(
  val autoSyncEnabled: Boolean = true,
  val syncOverWifiOnly: Boolean = false,
  val backgroundSyncEnabled: Boolean = true,
  val cacheFeedsOffline: Boolean = true,
  val cacheChatMediaOffline: Boolean = true,
  val lastSyncedAtMs: Long = 0L,
  val lastSyncStatus: String = "idle",
  val totalSyncedOps: Long = 0L,
  val updatedAtMs: Long = 0L,
)

data class OfflineSyncStatusSnapshot(
  val pendingSettingsOps: Int = 0,
  val pendingContactOps: Int = 0,
  val pendingFeedOps: Int = 0,
  val pendingPostUploads: Int = 0,
  val pendingActionOps: Int = 0,
  val isSyncing: Boolean = false,
  val lastResultMessage: String = "",
  val lastSyncedAtMs: Long = 0L,
) {
  val totalPendingCount: Int
    get() = pendingSettingsOps + pendingContactOps + pendingFeedOps + pendingPostUploads + pendingActionOps
}

data class SettingsSnapshot(
  val privacy: PrivacySettingsState = PrivacySettingsState(),
  val notifications: NotificationSettingsState = NotificationSettingsState(),
  val contentPreferences: ContentPreferencesState = ContentPreferencesState(),
  val syncPreferences: SyncPreferencesState = SyncPreferencesState(),
)

enum class ChatPrivacyRoute {
  PRIVACY,
  CHATS_THEME,
  BUBBLE_COLORS,
}

enum class ChatAudienceOption(val apiValue: String, val label: String) {
  ANYONE("anyone", "Anyone"),
  FRIENDS("friends", "Friends"),
  CONTACTS("contacts", "Contacts"),
  NOBODY("nobody", "Nobody");

  companion object {
    fun fromApi(raw: String?, allowContacts: Boolean = true, fallback: ChatAudienceOption = ANYONE): ChatAudienceOption {
      val clean = raw?.trim()?.lowercase().orEmpty()
      val matched = when (clean) {
        "anyone", "everyone" -> ANYONE
        "friends" -> FRIENDS
        "contacts", "contacts_only" -> if (allowContacts) CONTACTS else FRIENDS
        "nobody", "no_one", "only_me" -> NOBODY
        else -> fallback
      }
      return if (!allowContacts && matched == CONTACTS) FRIENDS else matched
    }
  }
}

enum class ChatPatternKind(val apiValue: String) {
  CLEAN("clean"),
  HOME("home"),
  MEADOW("meadow"),
  WINTER("winter"),
  OCEAN("ocean"),
  MATH("math"),
  BOTANICAL("botanical"),
  ROMANCE("romance"),
  FIRESIDE("fireside"),
  ARCADE("arcade"),
  COSMIC("cosmic"),
  SAFARI("safari"),
  TELEFAM("telefam");

  companion object {
    fun fromApi(raw: String?): ChatPatternKind =
      entries.firstOrNull { it.apiValue.equals(raw?.trim(), ignoreCase = true) } ?: CLEAN
  }
}

data class ChatThemeSpec(
  val themeId: String,
  val title: String,
  val patternKind: ChatPatternKind,
  val lightBgHex: Long,
  val darkBgHex: Long,
  val lightPatternAlpha: Float,
  val darkPatternAlpha: Float,
  val accentStartHex: Long,
  val accentEndHex: Long,
  val selectionHex: Long,
)

data class ChatBubbleColorOption(
  val colorId: String,
  val label: String,
  val lightStartHex: Long,
  val lightEndHex: Long,
  val darkStartHex: Long,
  val darkEndHex: Long,
)

data class ChatPrivacySettingsState(
  val messageRequests: ChatAudienceOption = ChatAudienceOption.ANYONE,
  val whoCanCall: ChatAudienceOption = ChatAudienceOption.ANYONE,
  val whoCanScreenshot: ChatAudienceOption = ChatAudienceOption.NOBODY,
  val whoCanShareChats: ChatAudienceOption = ChatAudienceOption.NOBODY,
  val whoCanCopyMessages: ChatAudienceOption = ChatAudienceOption.NOBODY,
  val whoCanDownloadMedia: ChatAudienceOption = ChatAudienceOption.ANYONE,
  val chatThemeId: String = "none",
  val bubbleColorId: String = "cyan",
  val readReceipts: ChatAudienceOption = ChatAudienceOption.ANYONE,
  val typingIndicators: ChatAudienceOption = ChatAudienceOption.NOBODY,
  val onlineIndicators: ChatAudienceOption = ChatAudienceOption.NOBODY,
  val updatedAtMs: Long = 0L,
) {
  val blocksScreenshots: Boolean
    get() = whoCanScreenshot == ChatAudienceOption.NOBODY
}

object TelefamChatPrivacyCatalog {
  val audienceOptions4: List<ChatAudienceOption> = listOf(
    ChatAudienceOption.ANYONE,
    ChatAudienceOption.FRIENDS,
    ChatAudienceOption.CONTACTS,
    ChatAudienceOption.NOBODY,
  )

  val audienceOptions3: List<ChatAudienceOption> = listOf(
    ChatAudienceOption.ANYONE,
    ChatAudienceOption.FRIENDS,
    ChatAudienceOption.NOBODY,
  )

  val themes: List<ChatThemeSpec> = listOf(
    ChatThemeSpec("none", "Default", ChatPatternKind.CLEAN, 0xFFF9FAFB, 0xFF0E1014, 0.04f, 0.04f, 0xFFD91E2A, 0xFFE11319, 0xFFD91E2A),
    ChatThemeSpec("classic_home", "Cozy Home", ChatPatternKind.HOME, 0xFFEAF3FC, 0xFF0C1219, 0.14f, 0.14f, 0xFF0EA5E9, 0xFF0284C7, 0xFF38BDF8),
    ChatThemeSpec("spring_meadow", "Spring Meadow", ChatPatternKind.MEADOW, 0xFFEAF5EE, 0xFF0C140E, 0.16f, 0.15f, 0xFF16A34A, 0xFF22C55E, 0xFF7ED957),
    ChatThemeSpec("arctic_snow", "Arctic Frost", ChatPatternKind.WINTER, 0xFFEBF3F9, 0xFF0B131A, 0.15f, 0.14f, 0xFF2B7A9E, 0xFF5BA4CF, 0xFF60A5FA),
    ChatThemeSpec("deep_sea", "Crystal Ocean", ChatPatternKind.OCEAN, 0xFFE9F2F8, 0xFF09121C, 0.15f, 0.14f, 0xFF3B82F6, 0xFF06B6D4, 0xFF22D3EE),
    ChatThemeSpec("academia_math", "Academia", ChatPatternKind.MATH, 0xFFEBF5F2, 0xFF0B1412, 0.14f, 0.14f, 0xFF0D9488, 0xFF2DD4BF, 0xFF2DD4BF),
    ChatThemeSpec("botanical_tulip", "Botanical", ChatPatternKind.BOTANICAL, 0xFFFAF0F3, 0xFF150C10, 0.14f, 0.14f, 0xFFE14D6A, 0xFFA855F7, 0xFFF472B6),
    ChatThemeSpec("neon_romance", "Velvet Heart", ChatPatternKind.ROMANCE, 0xFFF5EFFA, 0xFF130C18, 0.14f, 0.14f, 0xFF7C3AED, 0xFFEC4899, 0xFFC084FC),
    ChatThemeSpec("festive_fireside", "Warm Hearth", ChatPatternKind.FIRESIDE, 0xFFFBF2EB, 0xFF15100B, 0.14f, 0.14f, 0xFFD97706, 0xFFEA580C, 0xFFFB923C),
    ChatThemeSpec("retro_arcade", "Retro Arcade", ChatPatternKind.ARCADE, 0xFFF3EFFB, 0xFF0D0B18, 0.14f, 0.14f, 0xFFA855F7, 0xFFF59E0B, 0xFFF472B6),
    ChatThemeSpec("cosmic_nebula", "Cosmic Orbit", ChatPatternKind.COSMIC, 0xFFEEF1FA, 0xFF0A0D18, 0.14f, 0.14f, 0xFF4F46E5, 0xFF06B6D4, 0xFF818CF8),
    ChatThemeSpec("safari_sunset", "Savanna Dusk", ChatPatternKind.SAFARI, 0xFFFBF1EC, 0xFF170E0B, 0.14f, 0.14f, 0xFFB45309, 0xFFE11D48, 0xFFF97316),
    ChatThemeSpec("telefam_crimson", "Telefam Ruby", ChatPatternKind.TELEFAM, 0xFFFDF0F1, 0xFF14090B, 0.14f, 0.14f, 0xFFC80A0F, 0xFFEF233C, 0xFFEF4444),
  )

  val bubbleColors: List<ChatBubbleColorOption> = listOf(
    ChatBubbleColorOption("cyan", "Lagoon Cyan", 0xFF0097A7, 0xFF00BCD4, 0xFF00ACC1, 0xFF26C6DA),
    ChatBubbleColorOption("crimson", "Telefam Crimson", 0xFFC80A0F, 0xFFE11319, 0xFFD91E2A, 0xFFEF4444),
    ChatBubbleColorOption("emerald", "Emerald Spring", 0xFF15803D, 0xFF22C55E, 0xFF1B8E4B, 0xFF2DB866),
    ChatBubbleColorOption("azure", "Ocean Azure", 0xFF0284C7, 0xFF38BDF8, 0xFF1D9BF0, 0xFF0EA5E9),
    ChatBubbleColorOption("violet", "Royal Violet", 0xFF6D28D9, 0xFF8B5CF6, 0xFF7C3AED, 0xFFA78BFA),
    ChatBubbleColorOption("amber", "Sunset Amber", 0xFFC2410C, 0xFFF97316, 0xFFD97706, 0xFFF59E0B),
  )

  fun themeById(id: String?): ChatThemeSpec =
    themes.firstOrNull { it.themeId.equals(id?.trim(), ignoreCase = true) } ?: themes.first()

  fun bubbleColorById(id: String?): ChatBubbleColorOption =
    bubbleColors.firstOrNull { it.colorId.equals(id?.trim(), ignoreCase = true) } ?: bubbleColors.first()
}

data class RestrictedAccountItem(
  val userId: String,
  val fullName: String,
  val username: String,
  val avatarUrl: String? = null,
  val avatarLocalPath: String? = null,
  val isMuted: Boolean = false,
  val createdAtMs: Long = 0L,
)

data class SupportTicketItem(
  val ticketId: String,
  val referenceNumber: String,
  val category: String,
  val description: String,
  val screenshotLocalPath: String? = null,
  val screenshotSha256: String = "",
  val status: String = "open",
  val resolutionNote: String = "",
  val isSynced: Boolean = true,
  val createdAtMs: Long = 0L,
)

data class FaqEntry(
  val id: String,
  val category: String,
  val question: String,
  val answer: String,
)

data class SafetyGuideSection(
  val id: String,
  val title: String,
  val summary: String,
  val bulletPoints: List<String>,
)

data class EmergencyContactItem(
  val region: String,
  val serviceName: String,
  val contact: String,
  val notes: String,
)

data class LegalDocumentItem(
  val id: String,
  val title: String,
  val lastUpdated: String,
  val sections: List<String>,
)

data class OpenSourceLicenseEntry(
  val library: String,
  val license: String,
  val copyright: String,
)

data class SystemServiceStatusItem(
  val serviceName: String,
  val status: String,
  val latencyMs: Int,
  val summary: String,
)

data class AppVersionCheckResult(
  val installedVersion: String,
  val installedBuild: Int,
  val latestVersion: String,
  val updateAvailable: Boolean,
  val releaseNotes: String,
  val checkedAtMs: Long,
)

object TelefamSettingsContentCatalog {
  val reportCategories: List<String> = listOf(
    "Bug or crash",
    "Video playback",
    "Audio or sync",
    "Account & login",
    "Privacy & safety",
    "Harassment or abuse",
    "Performance",
    "Other",
  )

  val quickTopicTags: List<String> = listOf(
    "sports",
    "gaming",
    "politics",
    "finance",
    "fashion",
    "celebrities",
    "cooking",
    "comedy",
    "tech",
    "travel",
  )

  val defaultFaqs: List<FaqEntry> = listOf(
    FaqEntry(
      id = "faq_private",
      category = "Privacy",
      question = "How does Private account protect my posts?",
      answer = "When Private account is turned on, only your approved followers can see your posts, followers/following lists, and activity. Other people can still view your display name, handle, avatar, and privacy notice.",
    ),
    FaqEntry(
      id = "faq_username",
      category = "Account",
      question = "Why can usernames only change every 14 days?",
      answer = "A 14-day cooldown prevents handle impersonation, broken shared profile links, and automated username squatting.",
    ),
    FaqEntry(
      id = "faq_locked",
      category = "Posts",
      question = "Where do posts go when changed to Private?",
      answer = "Posts switched to Private are immediately removed from For You and Following feeds and moved to the Locked tab on your profile, visible only to you.",
    ),
    FaqEntry(
      id = "faq_playback",
      category = "Playback",
      question = "Do Autoplay, Video quality, and Data saver apply immediately?",
      answer = "Yes. Every change in Content preferences applies live without restarting or refreshing feeds, including captions, reduced motion, and not-interested topic filters.",
    ),
    FaqEntry(
      id = "faq_block_mute",
      category = "Safety",
      question = "What is the difference between Blocking and Muting?",
      answer = "Blocking stops another account from viewing your profile, finding your posts, or messaging you. Muting hides their posts from your feeds and search without notifying them.",
    ),
    FaqEntry(
      id = "faq_offline",
      category = "Sync",
      question = "Can I change settings while offline?",
      answer = "Yes. Changes save immediately to encrypted local storage and synchronize cleanly in the background as soon as connectivity returns.",
    ),
  )

  val safetySections: List<SafetyGuideSection> = listOf(
    SafetyGuideSection(
      id = "tips",
      title = "Safety tips",
      summary = "Practical steps to keep your account and family spaces secure.",
      bulletPoints = listOf(
        "Use Private account if you want only approved followers to view your videos and photos.",
        "Limit who can comment, mention, or find your profile inside Privacy settings.",
        "Review suspicious sessions anytime using Log out of all devices.",
        "Never share verification codes or sign-in links sent to your email.",
      ),
    ),
    SafetyGuideSection(
      id = "parents",
      title = "Guide for parents and guardians",
      summary = "Support healthy, age-appropriate digital habits for teens.",
      bulletPoints = listOf(
        "Telefam accounts require users to be at least 13 years old.",
        "Enable Private account, restrict comments to Friends, and turn on Data saver & Captions for calm viewing.",
        "Set Quiet hours in Notifications to silence alerts during school and sleep hours.",
        "Encourage open conversations before blocking or reporting distressing interactions.",
      ),
    ),
    SafetyGuideSection(
      id = "harassment",
      title = "Harassment and abuse resources",
      summary = "Immediate tools when someone makes you feel unsafe.",
      bulletPoints = listOf(
        "Block the account from their profile or Settings -> Blocked accounts to cut off visibility and interaction.",
        "Mute accounts to remove their posts silently from your feeds without alerting them.",
        "Attach screenshots in Report a problem to receive a tracked TF- reference number.",
      ),
    ),
    SafetyGuideSection(
      id = "how_reporting_works",
      title = "How reporting works",
      summary = "What happens after you submit a report.",
      bulletPoints = listOf(
        "1. Submit: Your report and encrypted media fingerprint are logged with a unique reference code.",
        "2. Review: Our Trust & Safety systems prioritize urgent harassment, impersonation, and child safety reports.",
        "3. Action: Verified violations lead to content removal, visibility limits, or permanent account bans.",
        "4. Track: Monitor open and resolved tickets anytime in Help & support -> Ticket history.",
      ),
    ),
  )

  val emergencyContacts: List<EmergencyContactItem> = listOf(
    EmergencyContactItem("International", "Find A Helpline (Free Confidential Support)", "findahelpline.com", "24/7 crisis support in over 130 countries"),
    EmergencyContactItem("International", "Child Helpline International", "116 111", "Free support helpline for children and young people across Europe & partner regions"),
    EmergencyContactItem("Kenya", "Befrienders Kenya / Red Cross Tele-counseling", "+254 722 178 177 / 1199", "Confidential emotional support & emergency response"),
    EmergencyContactItem("United States & Canada", "Suicide & Crisis Lifeline", "988", "24/7 free and confidential crisis support (call or text)"),
    EmergencyContactItem("United Kingdom", "Samaritans & Emergency", "116 123 / 999", "24/7 listening service and emergency dispatch"),
    EmergencyContactItem("European Union", "EU Emergency Services", "112", "Universal emergency telephone number across EU states"),
  )

  val legalDocuments: List<LegalDocumentItem> = listOf(
    LegalDocumentItem(
      id = "terms",
      title = "Terms of Service",
      lastUpdated = "November 14, 2026",
      sections = listOf(
        "1. Acceptance of Terms: By creating or using a Telefam account across Android, iOS, or Desktop, you agree to abide by these Terms of Service and confirm that you meet the minimum eligibility age of 13 years (or older where required by local law).",
        "2. Account Security & Identity Integrity: You are responsible for securing access to your email verification links and device sessions. Display names can be updated anytime, while usernames are subject to a 14-day cooldown to prevent impersonation and link hijacking.",
        "3. Content Ownership & License: You retain ownership of all videos, photos, audio selections, and text posts you upload. You grant Telefam a limited license strictly necessary to fingerprint media for safety, transcode adaptive bitrates, cache media cleanly, and display posts according to your chosen privacy scope (Public, Friends, or Private).",
        "4. Prohibited Conduct: Harassment, impersonation, non-consensual sharing, child endangerment, automated scraping, token replay, or bypassing Row-Level Security and privacy controls are strictly prohibited.",
        "5. Termination & Remedies: You may log out of individual devices or revoke all sessions at any time. Telefam may suspend accounts that violate safety laws or Community Guidelines.",
      ),
    ),
    LegalDocumentItem(
      id = "privacy",
      title = "Privacy Policy",
      lastUpdated = "November 14, 2026",
      sections = listOf(
        "1. Data We Collect: We collect registration details (email, name, username, country code, optional phone number, gender, date of birth, location, bio), uploaded avatars and background photos, posts, and settings preferences.",
        "2. Media & Device Fingerprinting: Uploaded media generates cryptographic SHA-256 digests, perceptual dHash values, and sampled video frame hashes solely to prevent duplicate abuse, verify upload integrity, and protect accounts from session theft.",
        "3. Your Privacy Controls: Inside Privacy settings, you control whether your account is Private, who can comment, mention, or discover you, whether activity status is visible, who sees your liked content or following list, and whether personalization informs your recommendations.",
        "4. Offline Caching & Retention: Essential profile and settings records are encrypted and cached on your device so preferences apply offline and sync securely upon reconnect.",
        "5. Data Rights: You may edit or remove profile content, unblock or unmute users, restrict non-essential cookies, or terminate all device sessions at any time.",
      ),
    ),
    LegalDocumentItem(
      id = "guidelines",
      title = "Community Guidelines",
      lastUpdated = "November 14, 2026",
      sections = listOf(
        "1. Respect & Dignity: Telefam brings families and communities together. Do not post threats, hate speech, targeted harassment, or bullying.",
        "2. Protect Youth: Content involving the exploitation, grooming, or endangerment of minors is removed immediately and reported to competent authorities.",
        "3. Honesty & Authentic Representation: Avoid deceptive impersonation, financial scams, phishing links, or coordinated artificial engagement.",
        "4. Respect Creator Privacy: Never redistribute Friends-only or Private content without permission.",
      ),
    ),
    LegalDocumentItem(
      id = "cookies",
      title = "Cookie & Local Storage Settings",
      lastUpdated = "November 14, 2026",
      sections = listOf(
        "1. Essential Storage (Required): Maintains authenticated device sessions, rotating refresh token security, and encrypted offline synchronization queues.",
        "2. Personalization Storage (Configurable): Remembers watched topics, muted topics, and recommendation preferences so feeds reflect your interests.",
        "3. Diagnostics & Performance (Configurable): Helps measure playback startup speed and adaptive streaming stability.",
      ),
    ),
    LegalDocumentItem(
      id = "licenses",
      title = "Licenses",
      lastUpdated = "November 14, 2026",
      sections = listOf(
        "1. Application License: Telefam client software is licensed to you on a personal, non-exclusive, non-transferable basis solely to access Telefam services in compliance with the Terms of Service.",
        "2. Open Source Attribution: Full third-party license notices are accessible under Settings and privacy -> About -> Open source licenses.",
      ),
    ),
  )

  val openSourceLicenses: List<OpenSourceLicenseEntry> = listOf(
    OpenSourceLicenseEntry("Kotlin & Coroutines", "Apache License 2.0", "Copyright JetBrains s.r.o."),
    OpenSourceLicenseEntry("Jetpack Compose & Material 3", "Apache License 2.0", "Copyright The Android Open Source Project"),
    OpenSourceLicenseEntry("AndroidX Room & Lifecycle", "Apache License 2.0", "Copyright The Android Open Source Project"),
    OpenSourceLicenseEntry("Media3 ExoPlayer", "Apache License 2.0", "Copyright The Android Open Source Project"),
    OpenSourceLicenseEntry("Retrofit, OkHttp & Moshi", "Apache License 2.0", "Copyright Square, Inc."),
    OpenSourceLicenseEntry("Coil Image Loading", "Apache License 2.0", "Copyright Coil Contributors"),
    OpenSourceLicenseEntry("Go Standard Library", "BSD 3-Clause License", "Copyright The Go Authors"),
  )
}

/**
 * Pure-Kotlin Version 3 / 4 Byte-Mode ISO/IEC 18004 QR code generator (ECC-L)
 * for scannable Share Profile QR codes without external library bloat.
 */
object TelefamQrEncoder {
  fun encodeToMatrix(rawText: String): Array<BooleanArray> {
    val bytes = rawText.trim().take(74).encodeToByteArray()
    // Version 4 (33x33) supports up to 78 bytes at ECC Level L (total codewords 100: 80 data + 20 EC)
    val version = 4
    val size = 17 + version * 4 // 33x33
    val dataCodewordsCount = 80
    val ecCodewordsCount = 20

    val bits = ArrayList<Int>(dataCodewordsCount * 8)
    fun pushBits(value: Int, count: Int) {
      for (i in (count - 1) downTo 0) {
        bits.add((value ushr i) and 1)
      }
    }

    pushBits(0b0100, 4) // Byte mode indicator
    pushBits(bytes.size, 8) // Char count for Version 1-9
    for (b in bytes) {
      pushBits(b.toInt() and 0xFF, 8)
    }
    // Terminator (up to 4 zeros)
    val maxDataBits = dataCodewordsCount * 8
    val term = minOf(4, maxDataBits - bits.size)
    pushBits(0, term)
    while (bits.size % 8 != 0) bits.add(0)
    var padIdx = 0
    while (bits.size < maxDataBits) {
      pushBits(if (padIdx % 2 == 0) 0xEC else 0x11, 8)
      padIdx++
    }

    val data = IntArray(dataCodewordsCount) { idx ->
      var v = 0
      for (bit in 0 until 8) {
        v = (v shl 1) or bits[idx * 8 + bit]
      }
      v
    }
    val ecc = computeReedSolomon(data, ecCodewordsCount)
    val allCodewords = IntArray(data.size + ecc.size)
    System.arraycopy(data, 0, allCodewords, 0, data.size)
    System.arraycopy(ecc, 0, allCodewords, data.size, ecc.size)

    val grid = Array(size) { BooleanArray(size) }
    val reserved = Array(size) { BooleanArray(size) }

    fun setModule(x: Int, y: Int, dark: Boolean, lock: Boolean = true) {
      if (x in 0 until size && y in 0 until size) {
        grid[y][x] = dark
        if (lock) reserved[y][x] = true
      }
    }

    fun drawFinder(ox: Int, oy: Int) {
      for (dy in -1..7) {
        for (dx in -1..7) {
          val xx = ox + dx
          val yy = oy + dy
          if (xx !in 0 until size || yy !in 0 until size) continue
          val isDark = if (dx in 0..6 && dy in 0..6) {
            dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4)
          } else false
          setModule(xx, yy, isDark)
        }
      }
    }

    drawFinder(0, 0)
    drawFinder(size - 7, 0)
    drawFinder(0, size - 7)

    // Alignment pattern at (26, 26) for Version 4
    val alignCenter = 26
    for (dy in -2..2) {
      for (dx in -2..2) {
        val isDark = kotlin.math.abs(dx) == 2 || kotlin.math.abs(dy) == 2 || (dx == 0 && dy == 0)
        setModule(alignCenter + dx, alignCenter + dy, isDark)
      }
    }

    // Timing patterns
    for (i in 8 until size - 8) {
      setModule(i, 6, i % 2 == 0)
      setModule(6, i, i % 2 == 0)
    }

    // Dark module + reserve format info strips
    setModule(8, size - 8, true)
    for (i in 0..8) {
      if (i != 6) {
        reserved[8][i] = true
        reserved[i][8] = true
      }
    }
    for (i in size - 8 until size) {
      reserved[8][i] = true
      reserved[i][8] = true
    }

    // Place data + ECC bits in standard zig-zag
    var bitIdx = 0
    val totalBits = allCodewords.size * 8
    var up = true
    var x = size - 1
    while (x > 0) {
      if (x == 6) x--
      var y = if (up) size - 1 else 0
      while (y in 0 until size) {
        for (colOffset in 0..1) {
          val cx = x - colOffset
          if (!reserved[y][cx]) {
            val bit = if (bitIdx < totalBits) {
              (allCodewords[bitIdx / 8] ushr (7 - (bitIdx % 8))) and 1
            } else 0
            bitIdx++
            // Use Mask pattern 0: (row + col) % 2 == 0
            val masked = if ((y + cx) % 2 == 0) bit xor 1 else bit
            grid[y][cx] = (masked == 1)
          }
        }
        y += if (up) -1 else 1
      }
      up = !up
      x -= 2
    }

    // Write 15-bit Format Info for ECC-L (01) + Mask 0 (000) -> 0b01000 + BCH = 0x77C4 (111011111000100)
    val formatBits = 0b111011111000100
    for (i in 0 until 15) {
      val bit = ((formatBits ushr (14 - i)) and 1) == 1
      if (i < 6) setModule(8, i, bit)
      else if (i < 8) setModule(8, i + 1, bit)
      else setModule(8, size - 15 + i, bit)

      if (i < 8) setModule(size - 1 - i, 8, bit)
      else if (i == 8) setModule(7, 8, bit)
      else setModule(14 - i, 8, bit)
    }

    return grid
  }

  private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
    val gen = rsGeneratorPoly(ecCount)
    val res = IntArray(ecCount)
    for (b in data) {
      val factor = b xor res[0]
      for (i in 0 until ecCount - 1) res[i] = res[i + 1]
      res[ecCount - 1] = 0
      for (i in 0 until ecCount) {
        res[i] = res[i] xor gfMul(gen[i], factor)
      }
    }
    return res
  }

  private fun rsGeneratorPoly(degree: Int): IntArray {
    var poly = intArrayOf(1)
    var root = 1
    for (i in 0 until degree) {
      poly = polyMultiply(poly, intArrayOf(1, root))
      root = gfMul(root, 2)
    }
    return poly.copyOfRange(1, poly.size)
  }

  private fun polyMultiply(a: IntArray, b: IntArray): IntArray {
    val out = IntArray(a.size + b.size - 1)
    for (i in a.indices) {
      for (j in b.indices) {
        out[i + j] = out[i + j] xor gfMul(a[i], b[j])
      }
    }
    return out
  }

  private fun gfMul(x: Int, y: Int): Int {
    var a = x
    var b = y
    var r = 0
    while (b > 0) {
      if ((b and 1) != 0) r = r xor a
      b = b ushr 1
      a = (a shl 1) xor (if ((a and 0x80) != 0) 0x11D else 0)
    }
    return r and 0xFF
  }
}
