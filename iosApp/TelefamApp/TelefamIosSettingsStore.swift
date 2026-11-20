import Combine
import CryptoKit
import Foundation
import Network
import SwiftUI
import UIKit

enum TelefamSettingsDestination: String, CaseIterable, Hashable, Identifiable {
    case hub
    case privacy
    case shareProfile
    case notifications
    case contentPreferences
    case savedLinks
    case offlineSync
    case blockedAccounts
    case reportProblem
    case safetyCenter
    case helpSupport
    case termsPolicies
    case about

    var id: String { rawValue }

    var title: String {
        switch self {
        case .hub: return "Settings and privacy"
        case .privacy: return "Privacy"
        case .shareProfile: return "Share profile"
        case .notifications: return "Notifications"
        case .contentPreferences: return "Content preferences"
        case .savedLinks: return "Saved links"
        case .offlineSync: return "Offline sync"
        case .blockedAccounts: return "Blocked & muted"
        case .reportProblem: return "Report a problem"
        case .safetyCenter: return "Safety Center"
        case .helpSupport: return "Help & support"
        case .termsPolicies: return "Terms and policies"
        case .about: return "About"
        }
    }
}

enum TelefamAudienceOption: String, CaseIterable, Identifiable {
    case everyone = "everyone"
    case friends = "friends"
    case noOne = "no_one"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .everyone: return "Everyone"
        case .friends: return "Friends"
        case .noOne: return "No one"
        }
    }

    static func fromApi(_ raw: String?) -> TelefamAudienceOption {
        TelefamAudienceOption(rawValue: (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? .everyone
    }
}

enum TelefamDiscoverabilityOption: String, CaseIterable, Identifiable {
    case everyone = "everyone"
    case contacts = "contacts"
    case noOne = "no_one"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .everyone: return "Everyone"
        case .contacts: return "Contacts only"
        case .noOne: return "No one"
        }
    }

    static func fromApi(_ raw: String?) -> TelefamDiscoverabilityOption {
        TelefamDiscoverabilityOption(rawValue: (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? .everyone
    }
}

enum TelefamVisibilityOption: String, CaseIterable, Identifiable {
    case everyone = "everyone"
    case friends = "friends"
    case onlyMe = "only_me"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .everyone: return "Everyone"
        case .friends: return "Friends"
        case .onlyMe: return "Only me"
        }
    }

    static func fromApi(_ raw: String?) -> TelefamVisibilityOption {
        TelefamVisibilityOption(rawValue: (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? .everyone
    }
}

enum TelefamVideoQualityOption: String, CaseIterable, Identifiable {
    case auto = "auto"
    case high = "high"
    case dataSaver = "data_saver"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto: return "Auto"
        case .high: return "High"
        case .dataSaver: return "Data saver"
        }
    }

    static func fromApi(_ raw: String?) -> TelefamVideoQualityOption {
        TelefamVideoQualityOption(rawValue: (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? .auto
    }
}

enum TelefamAppLanguageOption: String, CaseIterable, Identifiable {
    case en = "en"
    case sw = "sw"
    case fr = "fr"
    case es = "es"
    case ar = "ar"
    case pt = "pt"
    case de = "de"
    case hi = "hi"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .en: return "English"
        case .sw: return "Kiswahili"
        case .fr: return "Français"
        case .es: return "Español"
        case .ar: return "العربية"
        case .pt: return "Português"
        case .de: return "Deutsch"
        case .hi: return "हिन्दी"
        }
    }

    static func fromCode(_ raw: String?) -> TelefamAppLanguageOption {
        TelefamAppLanguageOption(rawValue: (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()) ?? .en
    }
}

struct TelefamPrivacySettings: Codable, Equatable {
    var isPrivateAccount: Bool = false
    var whoCanComment: String = TelefamAudienceOption.everyone.rawValue
    var whoCanMention: String = TelefamAudienceOption.everyone.rawValue
    var whoCanFindYou: String = TelefamDiscoverabilityOption.everyone.rawValue
    var showActivityStatus: Bool = true
    var likesVisibility: String = TelefamVisibilityOption.everyone.rawValue
    var followingVisibility: String = TelefamVisibilityOption.everyone.rawValue
    var dataSharingPersonalization: Bool = true
    var analyticsSharing: Bool = true
    var essentialCookies: Bool = true
    var personalizationCookies: Bool = true
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case isPrivateAccount = "is_private_account"
        case whoCanComment = "who_can_comment"
        case whoCanMention = "who_can_mention"
        case whoCanFindYou = "who_can_find_you"
        case showActivityStatus = "show_activity_status"
        case likesVisibility = "likes_visibility"
        case followingVisibility = "following_visibility"
        case dataSharingPersonalization = "data_sharing_personalization"
        case analyticsSharing = "analytics_sharing"
        case essentialCookies = "essential_cookies"
        case personalizationCookies = "personalization_cookies"
        case updatedAtMs = "updated_at_ms"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        isPrivateAccount = try c.decodeIfPresent(Bool.self, forKey: .isPrivateAccount) ?? false
        whoCanComment = try c.decodeIfPresent(String.self, forKey: .whoCanComment) ?? "everyone"
        whoCanMention = try c.decodeIfPresent(String.self, forKey: .whoCanMention) ?? "everyone"
        whoCanFindYou = try c.decodeIfPresent(String.self, forKey: .whoCanFindYou) ?? "everyone"
        showActivityStatus = try c.decodeIfPresent(Bool.self, forKey: .showActivityStatus) ?? true
        likesVisibility = try c.decodeIfPresent(String.self, forKey: .likesVisibility) ?? "everyone"
        followingVisibility = try c.decodeIfPresent(String.self, forKey: .followingVisibility) ?? "everyone"
        dataSharingPersonalization = try c.decodeIfPresent(Bool.self, forKey: .dataSharingPersonalization) ?? true
        analyticsSharing = try c.decodeIfPresent(Bool.self, forKey: .analyticsSharing) ?? true
        essentialCookies = try c.decodeIfPresent(Bool.self, forKey: .essentialCookies) ?? true
        personalizationCookies = try c.decodeIfPresent(Bool.self, forKey: .personalizationCookies) ?? true
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
    }
}

struct TelefamNotificationSettings: Codable, Equatable {
    var masterPause: Bool = false
    var channelPush: Bool = true
    var channelEmail: Bool = true
    var channelSms: Bool = false
    var typeMessages: Bool = true
    var typeComments: Bool = true
    var typeFollowers: Bool = true
    var typeProductUpdates: Bool = false
    var quietHoursEnabled: Bool = false
    var quietStartMinutes: Int = 22 * 60
    var quietEndMinutes: Int = 7 * 60
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case masterPause = "master_pause"
        case channelPush = "channel_push"
        case channelEmail = "channel_email"
        case channelSms = "channel_sms"
        case typeMessages = "type_messages"
        case typeComments = "type_comments"
        case typeFollowers = "type_followers"
        case typeProductUpdates = "type_product_updates"
        case quietHoursEnabled = "quiet_hours_enabled"
        case quietStartMinutes = "quiet_start_minutes"
        case quietEndMinutes = "quiet_end_minutes"
        case updatedAtMs = "updated_at_ms"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        masterPause = try c.decodeIfPresent(Bool.self, forKey: .masterPause) ?? false
        channelPush = try c.decodeIfPresent(Bool.self, forKey: .channelPush) ?? true
        channelEmail = try c.decodeIfPresent(Bool.self, forKey: .channelEmail) ?? true
        channelSms = try c.decodeIfPresent(Bool.self, forKey: .channelSms) ?? false
        typeMessages = try c.decodeIfPresent(Bool.self, forKey: .typeMessages) ?? true
        typeComments = try c.decodeIfPresent(Bool.self, forKey: .typeComments) ?? true
        typeFollowers = try c.decodeIfPresent(Bool.self, forKey: .typeFollowers) ?? true
        typeProductUpdates = try c.decodeIfPresent(Bool.self, forKey: .typeProductUpdates) ?? false
        quietHoursEnabled = try c.decodeIfPresent(Bool.self, forKey: .quietHoursEnabled) ?? false
        quietStartMinutes = try c.decodeIfPresent(Int.self, forKey: .quietStartMinutes) ?? (22 * 60)
        quietEndMinutes = try c.decodeIfPresent(Int.self, forKey: .quietEndMinutes) ?? (7 * 60)
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
    }

    static func formatTime(_ minutesOfDay: Int) -> String {
        let clamped = ((minutesOfDay % 1440) + 1440) % 1440
        return String(format: "%02d:%02d", clamped / 60, clamped % 60)
    }
}

struct TelefamContentPreferences: Codable, Equatable {
    var suggestedContentEnabled: Bool = true
    var notInterestedTopics: [String] = []
    var languageCode: String = TelefamAppLanguageOption.en.rawValue
    var autoplayVideos: Bool = true
    var videoQuality: String = TelefamVideoQualityOption.auto.rawValue
    var dataSaver: Bool = false
    var captionsEnabled: Bool = false
    var reducedMotion: Bool = false
    var resetSuggestions: Bool = false
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case suggestedContentEnabled = "suggested_content_enabled"
        case notInterestedTopics = "not_interested_topics"
        case languageCode = "language_code"
        case autoplayVideos = "autoplay_videos"
        case videoQuality = "video_quality"
        case dataSaver = "data_saver"
        case captionsEnabled = "captions_enabled"
        case reducedMotion = "reduced_motion"
        case resetSuggestions = "reset_suggestions"
        case updatedAtMs = "updated_at_ms"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        suggestedContentEnabled = try c.decodeIfPresent(Bool.self, forKey: .suggestedContentEnabled) ?? true
        notInterestedTopics = try c.decodeIfPresent([String].self, forKey: .notInterestedTopics) ?? []
        languageCode = try c.decodeIfPresent(String.self, forKey: .languageCode) ?? "en"
        autoplayVideos = try c.decodeIfPresent(Bool.self, forKey: .autoplayVideos) ?? true
        videoQuality = try c.decodeIfPresent(String.self, forKey: .videoQuality) ?? "auto"
        dataSaver = try c.decodeIfPresent(Bool.self, forKey: .dataSaver) ?? false
        captionsEnabled = try c.decodeIfPresent(Bool.self, forKey: .captionsEnabled) ?? false
        reducedMotion = try c.decodeIfPresent(Bool.self, forKey: .reducedMotion) ?? false
        resetSuggestions = try c.decodeIfPresent(Bool.self, forKey: .resetSuggestions) ?? false
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
    }
}

struct TelefamSyncPreferences: Codable, Equatable {
    var autoSyncEnabled: Bool = true
    var syncOverWifiOnly: Bool = false
    var backgroundSyncEnabled: Bool = true
    var cacheFeedsOffline: Bool = true
    var cacheChatMediaOffline: Bool = true
    var lastSyncedAtMs: Int64 = 0
    var lastSyncStatus: String = "idle"
    var totalSyncedOps: Int64 = 0
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case autoSyncEnabled = "auto_sync_enabled"
        case syncOverWifiOnly = "sync_over_wifi_only"
        case backgroundSyncEnabled = "background_sync_enabled"
        case cacheFeedsOffline = "cache_feeds_offline"
        case cacheChatMediaOffline = "cache_chat_media_offline"
        case lastSyncedAtMs = "last_synced_at_ms"
        case lastSyncStatus = "last_sync_status"
        case totalSyncedOps = "total_synced_ops"
        case updatedAtMs = "updated_at_ms"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        autoSyncEnabled = try c.decodeIfPresent(Bool.self, forKey: .autoSyncEnabled) ?? true
        syncOverWifiOnly = try c.decodeIfPresent(Bool.self, forKey: .syncOverWifiOnly) ?? false
        backgroundSyncEnabled = try c.decodeIfPresent(Bool.self, forKey: .backgroundSyncEnabled) ?? true
        cacheFeedsOffline = try c.decodeIfPresent(Bool.self, forKey: .cacheFeedsOffline) ?? true
        cacheChatMediaOffline = try c.decodeIfPresent(Bool.self, forKey: .cacheChatMediaOffline) ?? true
        lastSyncedAtMs = try c.decodeIfPresent(Int64.self, forKey: .lastSyncedAtMs) ?? 0
        lastSyncStatus = try c.decodeIfPresent(String.self, forKey: .lastSyncStatus) ?? "idle"
        totalSyncedOps = try c.decodeIfPresent(Int64.self, forKey: .totalSyncedOps) ?? 0
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
    }
}

enum TelefamChatPrivacyRoute: Hashable {
    case chatsTheme
    case bubbleColors
}

enum TelefamChatAudienceOption: String, CaseIterable, Identifiable {
    case anyone = "anyone"
    case friends = "friends"
    case contacts = "contacts"
    case nobody = "nobody"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .anyone: return "Anyone"
        case .friends: return "Friends"
        case .contacts: return "Contacts"
        case .nobody: return "Nobody"
        }
    }

    static func fromApi(_ raw: String?, allowContacts: Bool = true, fallback: TelefamChatAudienceOption = .anyone) -> TelefamChatAudienceOption {
        let clean = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let matched: TelefamChatAudienceOption
        switch clean {
        case "anyone", "everyone": matched = .anyone
        case "friends": matched = .friends
        case "contacts", "contacts_only": matched = allowContacts ? .contacts : .friends
        case "nobody", "no_one", "only_me": matched = .nobody
        default: matched = fallback
        }
        return (!allowContacts && matched == .contacts) ? .friends : matched
    }
}

enum TelefamChatPatternKind: String, CaseIterable {
    case clean, home, meadow, winter, ocean, math, botanical, romance, fireside, arcade, cosmic, safari, telefam
}

struct TelefamChatThemeSpec: Identifiable, Equatable {
    let themeId: String
    let title: String
    let patternKind: TelefamChatPatternKind
    let lightBgHex: UInt32
    let darkBgHex: UInt32
    let lightPatternAlpha: Double
    let darkPatternAlpha: Double
    let accentStartHex: UInt32
    let accentEndHex: UInt32
    let selectionHex: UInt32

    var id: String { themeId }
}

struct TelefamChatBubbleColorOption: Identifiable, Equatable {
    let colorId: String
    let label: String
    let lightStartHex: UInt32
    let lightEndHex: UInt32
    let darkStartHex: UInt32
    let darkEndHex: UInt32

    var id: String { colorId }
}

struct TelefamChatPrivacySettings: Codable, Equatable {
    var messageRequests: String = TelefamChatAudienceOption.anyone.rawValue
    var whoCanCall: String = TelefamChatAudienceOption.anyone.rawValue
    var whoCanScreenshot: String = TelefamChatAudienceOption.nobody.rawValue
    var whoCanShareChats: String = TelefamChatAudienceOption.nobody.rawValue
    var whoCanCopyMessages: String = TelefamChatAudienceOption.nobody.rawValue
    var whoCanDownloadMedia: String = TelefamChatAudienceOption.anyone.rawValue
    var chatThemeId: String = "none"
    var bubbleColorId: String = "cyan"
    var readReceipts: String = TelefamChatAudienceOption.anyone.rawValue
    var typingIndicators: String = TelefamChatAudienceOption.nobody.rawValue
    var onlineIndicators: String = TelefamChatAudienceOption.nobody.rawValue
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case messageRequests = "message_requests"
        case whoCanCall = "who_can_call"
        case whoCanScreenshot = "who_can_screenshot"
        case whoCanShareChats = "who_can_share_chats"
        case whoCanCopyMessages = "who_can_copy_messages"
        case whoCanDownloadMedia = "who_can_download_media"
        case chatThemeId = "chat_theme_id"
        case bubbleColorId = "bubble_color_id"
        case readReceipts = "read_receipts"
        case typingIndicators = "typing_indicators"
        case onlineIndicators = "online_indicators"
        case updatedAtMs = "updated_at_ms"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        messageRequests = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .messageRequests), allowContacts: true, fallback: .anyone).rawValue
        whoCanCall = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .whoCanCall), allowContacts: true, fallback: .anyone).rawValue
        whoCanScreenshot = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .whoCanScreenshot), allowContacts: false, fallback: .nobody).rawValue
        whoCanShareChats = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .whoCanShareChats), allowContacts: false, fallback: .nobody).rawValue
        whoCanCopyMessages = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .whoCanCopyMessages), allowContacts: false, fallback: .nobody).rawValue
        whoCanDownloadMedia = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .whoCanDownloadMedia), allowContacts: false, fallback: .anyone).rawValue
        chatThemeId = TelefamChatPrivacyCatalog.themeById(try c.decodeIfPresent(String.self, forKey: .chatThemeId)).themeId
        bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(try c.decodeIfPresent(String.self, forKey: .bubbleColorId)).colorId
        readReceipts = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .readReceipts), allowContacts: false, fallback: .anyone).rawValue
        typingIndicators = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .typingIndicators), allowContacts: false, fallback: .nobody).rawValue
        onlineIndicators = TelefamChatAudienceOption.fromApi(try c.decodeIfPresent(String.self, forKey: .onlineIndicators), allowContacts: false, fallback: .nobody).rawValue
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
    }
}

enum TelefamChatPrivacyCatalog {
    static let audienceOptions4: [TelefamChatAudienceOption] = [.anyone, .friends, .contacts, .nobody]
    static let audienceOptions3: [TelefamChatAudienceOption] = [.anyone, .friends, .nobody]

    static let themes: [TelefamChatThemeSpec] = [
        TelefamChatThemeSpec(themeId: "none", title: "Default", patternKind: .clean, lightBgHex: 0xF9FAFB, darkBgHex: 0x0E1014, lightPatternAlpha: 0.04, darkPatternAlpha: 0.04, accentStartHex: 0xD91E2A, accentEndHex: 0xE11319, selectionHex: 0xD91E2A),
        TelefamChatThemeSpec(themeId: "classic_home", title: "Cozy Home", patternKind: .home, lightBgHex: 0xEAF3FC, darkBgHex: 0x0C1219, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0x0EA5E9, accentEndHex: 0x0284C7, selectionHex: 0x38BDF8),
        TelefamChatThemeSpec(themeId: "spring_meadow", title: "Spring Meadow", patternKind: .meadow, lightBgHex: 0xEAF5EE, darkBgHex: 0x0C140E, lightPatternAlpha: 0.16, darkPatternAlpha: 0.15, accentStartHex: 0x16A34A, accentEndHex: 0x22C55E, selectionHex: 0x7ED957),
        TelefamChatThemeSpec(themeId: "arctic_snow", title: "Arctic Frost", patternKind: .winter, lightBgHex: 0xEBF3F9, darkBgHex: 0x0B131A, lightPatternAlpha: 0.15, darkPatternAlpha: 0.14, accentStartHex: 0x2B7A9E, accentEndHex: 0x5BA4CF, selectionHex: 0x60A5FA),
        TelefamChatThemeSpec(themeId: "deep_sea", title: "Crystal Ocean", patternKind: .ocean, lightBgHex: 0xE9F2F8, darkBgHex: 0x09121C, lightPatternAlpha: 0.15, darkPatternAlpha: 0.14, accentStartHex: 0x3B82F6, accentEndHex: 0x06B6D4, selectionHex: 0x22D3EE),
        TelefamChatThemeSpec(themeId: "academia_math", title: "Academia", patternKind: .math, lightBgHex: 0xEBF5F2, darkBgHex: 0x0B1412, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0x0D9488, accentEndHex: 0x2DD4BF, selectionHex: 0x2DD4BF),
        TelefamChatThemeSpec(themeId: "botanical_tulip", title: "Botanical", patternKind: .botanical, lightBgHex: 0xFAF0F3, darkBgHex: 0x150C10, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0xE14D6A, accentEndHex: 0xA855F7, selectionHex: 0xF472B6),
        TelefamChatThemeSpec(themeId: "neon_romance", title: "Velvet Heart", patternKind: .romance, lightBgHex: 0xF5EFFA, darkBgHex: 0x130C18, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0x7C3AED, accentEndHex: 0xEC4899, selectionHex: 0xC084FC),
        TelefamChatThemeSpec(themeId: "festive_fireside", title: "Warm Hearth", patternKind: .fireside, lightBgHex: 0xFBF2EB, darkBgHex: 0x15100B, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0xD97706, accentEndHex: 0xEA580C, selectionHex: 0xFB923C),
        TelefamChatThemeSpec(themeId: "retro_arcade", title: "Retro Arcade", patternKind: .arcade, lightBgHex: 0xF3EFFB, darkBgHex: 0x0D0B18, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0xA855F7, accentEndHex: 0xF59E0B, selectionHex: 0xF472B6),
        TelefamChatThemeSpec(themeId: "cosmic_nebula", title: "Cosmic Orbit", patternKind: .cosmic, lightBgHex: 0xEEF1FA, darkBgHex: 0x0A0D18, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0x4F46E5, accentEndHex: 0x06B6D4, selectionHex: 0x818CF8),
        TelefamChatThemeSpec(themeId: "safari_sunset", title: "Savanna Dusk", patternKind: .safari, lightBgHex: 0xFBF1EC, darkBgHex: 0x170E0B, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0xB45309, accentEndHex: 0xE11D48, selectionHex: 0xF97316),
        TelefamChatThemeSpec(themeId: "telefam_crimson", title: "Telefam Ruby", patternKind: .telefam, lightBgHex: 0xFDF0F1, darkBgHex: 0x14090B, lightPatternAlpha: 0.14, darkPatternAlpha: 0.14, accentStartHex: 0xC80A0F, accentEndHex: 0xEF233C, selectionHex: 0xEF4444),
    ]

    static let bubbleColors: [TelefamChatBubbleColorOption] = [
        TelefamChatBubbleColorOption(colorId: "cyan", label: "Lagoon Cyan", lightStartHex: 0x0097A7, lightEndHex: 0x00BCD4, darkStartHex: 0x00ACC1, darkEndHex: 0x26C6DA),
        TelefamChatBubbleColorOption(colorId: "crimson", label: "Telefam Crimson", lightStartHex: 0xC80A0F, lightEndHex: 0xE11319, darkStartHex: 0xD91E2A, darkEndHex: 0xEF4444),
        TelefamChatBubbleColorOption(colorId: "emerald", label: "Emerald Spring", lightStartHex: 0x15803D, lightEndHex: 0x22C55E, darkStartHex: 0x1B8E4B, darkEndHex: 0x2DB866),
        TelefamChatBubbleColorOption(colorId: "azure", label: "Ocean Azure", lightStartHex: 0x0284C7, lightEndHex: 0x38BDF8, darkStartHex: 0x1D9BF0, darkEndHex: 0x0EA5E9),
        TelefamChatBubbleColorOption(colorId: "violet", label: "Royal Violet", lightStartHex: 0x6D28D9, lightEndHex: 0x8B5CF6, darkStartHex: 0x7C3AED, darkEndHex: 0xA78BFA),
        TelefamChatBubbleColorOption(colorId: "amber", label: "Sunset Amber", lightStartHex: 0xC2410C, lightEndHex: 0xF97316, darkStartHex: 0xD97706, darkEndHex: 0xF59E0B),
    ]

    static func themeById(_ id: String?) -> TelefamChatThemeSpec {
        let key = (id ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return themes.first(where: { $0.themeId == key }) ?? themes[0]
    }

    static func bubbleColorById(_ id: String?) -> TelefamChatBubbleColorOption {
        let key = (id ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return bubbleColors.first(where: { $0.colorId == key }) ?? bubbleColors[0]
    }

    static func color(_ hex: UInt32, opacity: Double = 1.0) -> Color {
        Color(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: opacity
        )
    }
}

struct TelefamSettingsBundle: Codable, Equatable {
    var privacy = TelefamPrivacySettings()
    var notifications = TelefamNotificationSettings()
    var contentPreferences = TelefamContentPreferences()
    var chatPrivacy = TelefamChatPrivacySettings()
    var syncPreferences = TelefamSyncPreferences()

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        privacy = try c.decodeIfPresent(TelefamPrivacySettings.self, forKey: .privacy) ?? TelefamPrivacySettings()
        notifications = try c.decodeIfPresent(TelefamNotificationSettings.self, forKey: .notifications) ?? TelefamNotificationSettings()
        contentPreferences = try c.decodeIfPresent(TelefamContentPreferences.self, forKey: .contentPreferences) ?? TelefamContentPreferences()
        chatPrivacy = try c.decodeIfPresent(TelefamChatPrivacySettings.self, forKey: .chatPrivacy) ?? TelefamChatPrivacySettings()
        syncPreferences = try c.decodeIfPresent(TelefamSyncPreferences.self, forKey: .syncPreferences) ?? TelefamSyncPreferences()
    }
}

struct TelefamRestrictedAccount: Identifiable, Codable, Equatable {
    let userId: String
    let fullName: String
    let username: String
    let avatarUrl: String
    let createdAtMs: Int64
    var isMuted: Bool = false

    var id: String { userId }

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case fullName = "full_name"
        case username
        case avatarUrl = "avatar_url"
        case createdAtMs = "created_at_ms"
    }

    init(
        userId: String,
        fullName: String,
        username: String,
        avatarUrl: String = "",
        createdAtMs: Int64 = 0,
        isMuted: Bool = false
    ) {
        self.userId = userId
        self.fullName = fullName
        self.username = username
        self.avatarUrl = avatarUrl
        self.createdAtMs = createdAtMs
        self.isMuted = isMuted
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        userId = try c.decodeIfPresent(String.self, forKey: .userId) ?? ""
        fullName = try c.decodeIfPresent(String.self, forKey: .fullName) ?? ""
        username = try c.decodeIfPresent(String.self, forKey: .username) ?? ""
        avatarUrl = try c.decodeIfPresent(String.self, forKey: .avatarUrl) ?? ""
        createdAtMs = try c.decodeIfPresent(Int64.self, forKey: .createdAtMs) ?? 0
        isMuted = false
    }
}

struct TelefamSupportTicket: Identifiable, Codable, Equatable {
    let ticketId: String
    let referenceNumber: String
    let category: String
    let description: String
    let screenshotMediaId: String
    let screenshotSha256: String
    let status: String
    let resolutionNote: String
    let createdAtMs: Int64
    let updatedAtMs: Int64
    var isSynced: Bool = true

    var id: String { ticketId.isEmpty ? referenceNumber : ticketId }

    enum CodingKeys: String, CodingKey {
        case ticketId = "ticket_id"
        case referenceNumber = "reference_number"
        case category
        case description
        case screenshotMediaId = "screenshot_media_id"
        case screenshotSha256 = "screenshot_sha256"
        case status
        case resolutionNote = "resolution_note"
        case createdAtMs = "created_at_ms"
        case updatedAtMs = "updated_at_ms"
    }

    init(
        ticketId: String,
        referenceNumber: String,
        category: String,
        description: String,
        screenshotMediaId: String = "",
        screenshotSha256: String = "",
        status: String = "open",
        resolutionNote: String = "",
        createdAtMs: Int64 = 0,
        updatedAtMs: Int64 = 0,
        isSynced: Bool = true
    ) {
        self.ticketId = ticketId
        self.referenceNumber = referenceNumber
        self.category = category
        self.description = description
        self.screenshotMediaId = screenshotMediaId
        self.screenshotSha256 = screenshotSha256
        self.status = status
        self.resolutionNote = resolutionNote
        self.createdAtMs = createdAtMs
        self.updatedAtMs = updatedAtMs
        self.isSynced = isSynced
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        ticketId = try c.decodeIfPresent(String.self, forKey: .ticketId) ?? ""
        referenceNumber = try c.decodeIfPresent(String.self, forKey: .referenceNumber) ?? ""
        category = try c.decodeIfPresent(String.self, forKey: .category) ?? ""
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        screenshotMediaId = try c.decodeIfPresent(String.self, forKey: .screenshotMediaId) ?? ""
        screenshotSha256 = try c.decodeIfPresent(String.self, forKey: .screenshotSha256) ?? ""
        status = try c.decodeIfPresent(String.self, forKey: .status) ?? "open"
        resolutionNote = try c.decodeIfPresent(String.self, forKey: .resolutionNote) ?? ""
        createdAtMs = try c.decodeIfPresent(Int64.self, forKey: .createdAtMs) ?? 0
        updatedAtMs = try c.decodeIfPresent(Int64.self, forKey: .updatedAtMs) ?? 0
        isSynced = true
    }
}

struct TelefamFaqEntry: Identifiable, Codable, Equatable {
    let id: String
    let category: String
    let question: String
    let answer: String
}

struct TelefamSafetyGuideSection: Identifiable, Equatable {
    let id: String
    let title: String
    let summary: String
    let bulletPoints: [String]
}

struct TelefamEmergencyContact: Identifiable, Equatable {
    let region: String
    let serviceName: String
    let contact: String
    let notes: String
    var id: String { "\(region)_\(serviceName)" }
}

struct TelefamLegalDocument: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let lastUpdated: String
    let sections: [String]

    enum CodingKeys: String, CodingKey {
        case id, title, sections
        case lastUpdated = "last_updated"
    }
}

struct TelefamOpenSourceLicense: Identifiable, Equatable {
    let library: String
    let license: String
    let copyright: String
    var id: String { library }
}

struct TelefamSystemServiceStatus: Identifiable, Codable, Equatable {
    let serviceName: String
    let status: String
    let latencyMs: Int
    let summary: String

    var id: String { serviceName }

    enum CodingKeys: String, CodingKey {
        case serviceName = "service_name"
        case status
        case latencyMs = "latency_ms"
        case summary
    }
}

struct TelefamVersionCheckResult: Equatable {
    let installedVersion: String
    let installedBuild: Int
    let latestVersion: String
    let updateAvailable: Bool
    let releaseNotes: String
    let checkedAtMs: Int64
}

private struct TelefamPendingSettingsOp: Codable {
    let opId: String
    let opKind: String
    let payloadData: Data
    let createdAtMs: Int64
}

private struct PendingProblemReportPayload: Codable {
    let clientOpId: String
    let category: String
    let description: String
    let localJpegPath: String?
    let screenshotSha256: String

    enum CodingKeys: String, CodingKey {
        case clientOpId = "client_op_id"
        case category
        case description
        case localJpegPath = "local_jpeg_path"
        case screenshotSha256 = "screenshot_sha256"
    }
}

enum TelefamIosSettingsCatalog {
    static let reportCategories: [String] = [
        "Bug or crash",
        "Video playback",
        "Audio or sync",
        "Account & login",
        "Privacy & safety",
        "Harassment or abuse",
        "Performance",
        "Other"
    ]

    static let quickTopicTags: [String] = [
        "sports", "gaming", "politics", "finance", "fashion",
        "celebrities", "cooking", "comedy", "tech", "travel"
    ]

    static let defaultFaqs: [TelefamFaqEntry] = [
        TelefamFaqEntry(
            id: "faq_private",
            category: "Privacy",
            question: "How does Private account protect my posts?",
            answer: "When Private account is turned on, only your approved followers can see your posts, followers/following lists, and activity. Other people can still view your display name, handle, avatar, and privacy notice."
        ),
        TelefamFaqEntry(
            id: "faq_username",
            category: "Account",
            question: "Why can usernames only change every 14 days?",
            answer: "A 14-day cooldown prevents handle impersonation, broken shared profile links, and automated username squatting."
        ),
        TelefamFaqEntry(
            id: "faq_locked",
            category: "Posts",
            question: "Where do posts go when changed to Private?",
            answer: "Posts switched to Private are immediately removed from For You and Following feeds and moved to the Locked tab on your profile, visible only to you."
        ),
        TelefamFaqEntry(
            id: "faq_playback",
            category: "Playback",
            question: "Do Autoplay, Video quality, and Data saver apply immediately?",
            answer: "Yes. Every change in Content preferences applies live without restarting or refreshing feeds, including captions, reduced motion, and not-interested topic filters."
        ),
        TelefamFaqEntry(
            id: "faq_block_mute",
            category: "Safety",
            question: "What is the difference between Blocking and Muting?",
            answer: "Blocking stops another account from viewing your profile, finding your posts, or messaging you. Muting hides their posts from your feeds and search without notifying them."
        ),
        TelefamFaqEntry(
            id: "faq_offline",
            category: "Sync",
            question: "Can I change settings while offline?",
            answer: "Yes. Changes save immediately to encrypted local storage and synchronize cleanly in the background as soon as connectivity returns."
        )
    ]

    static let safetySections: [TelefamSafetyGuideSection] = [
        TelefamSafetyGuideSection(
            id: "tips",
            title: "Safety tips",
            summary: "Practical steps to keep your account and family spaces secure.",
            bulletPoints: [
                "Use Private account if you want only approved followers to view your videos and photos.",
                "Limit who can comment, mention, or find your profile inside Privacy settings.",
                "Review suspicious sessions anytime using Log out of all devices.",
                "Never share verification codes or sign-in links sent to your email."
            ]
        ),
        TelefamSafetyGuideSection(
            id: "parents",
            title: "Guide for parents and guardians",
            summary: "Support healthy, age-appropriate digital habits for teens.",
            bulletPoints: [
                "Telefam accounts require users to be at least 13 years old.",
                "Enable Private account, restrict comments to Friends, and turn on Data saver & Captions for calm viewing.",
                "Set Quiet hours in Notifications to silence alerts during school and sleep hours.",
                "Encourage open conversations before blocking or reporting distressing interactions."
            ]
        ),
        TelefamSafetyGuideSection(
            id: "harassment",
            title: "Harassment and abuse resources",
            summary: "Immediate tools when someone makes you feel unsafe.",
            bulletPoints: [
                "Block the account from their profile or Settings -> Blocked accounts to cut off visibility and interaction.",
                "Mute accounts to remove their posts silently from your feeds without alerting them.",
                "Attach screenshots in Report a problem to receive a tracked TF- reference number."
            ]
        ),
        TelefamSafetyGuideSection(
            id: "how_reporting_works",
            title: "How reporting works",
            summary: "What happens after you submit a report.",
            bulletPoints: [
                "1. Submit: Your report and encrypted media fingerprint are logged with a unique reference code.",
                "2. Review: Trust & Safety systems prioritize urgent harassment, impersonation, and child safety reports.",
                "3. Action: Verified violations lead to content removal, visibility limits, or permanent account bans.",
                "4. Track: Monitor open and resolved tickets anytime in Help & support -> Ticket history."
            ]
        )
    ]

    static let emergencyContacts: [TelefamEmergencyContact] = [
        TelefamEmergencyContact(region: "International", serviceName: "Find A Helpline (Free Confidential Support)", contact: "findahelpline.com", notes: "24/7 crisis support in over 130 countries"),
        TelefamEmergencyContact(region: "International", serviceName: "Child Helpline International", contact: "116 111", notes: "Free support helpline for children and young people across Europe & partner regions"),
        TelefamEmergencyContact(region: "Kenya", serviceName: "Befrienders Kenya / Red Cross Tele-counseling", contact: "+254 722 178 177 / 1199", notes: "Confidential emotional support & emergency response"),
        TelefamEmergencyContact(region: "United States & Canada", serviceName: "Suicide & Crisis Lifeline", contact: "988", notes: "24/7 free and confidential crisis support (call or text)"),
        TelefamEmergencyContact(region: "United Kingdom", serviceName: "Samaritans & Emergency", contact: "116 123 / 999", notes: "24/7 listening service and emergency dispatch"),
        TelefamEmergencyContact(region: "European Union", serviceName: "EU Emergency Services", contact: "112", notes: "Universal emergency telephone number across EU states")
    ]

    static let legalDocuments: [TelefamLegalDocument] = [
        TelefamLegalDocument(
            id: "terms",
            title: "Terms of Service",
            lastUpdated: "November 14, 2026",
            sections: [
                "1. Acceptance of Terms: By creating or using a Telefam account across Android, iOS, or Desktop, you agree to abide by these Terms of Service and confirm that you meet the minimum eligibility age of 13 years (or older where required by local law).",
                "2. Account Security & Identity Integrity: You are responsible for securing access to your email verification links and device sessions. Display names can be updated anytime, while usernames are subject to a 14-day cooldown to prevent impersonation and link hijacking.",
                "3. Content Ownership & License: You retain ownership of all videos, photos, audio selections, and text posts you upload. You grant Telefam a limited license strictly necessary to fingerprint media for safety, transcode adaptive bitrates, cache media cleanly, and display posts according to your chosen privacy scope (Public, Friends, or Private).",
                "4. Prohibited Conduct: Harassment, impersonation, non-consensual sharing, child endangerment, automated scraping, token replay, or bypassing Row-Level Security and privacy controls are strictly prohibited.",
                "5. Termination & Remedies: You may log out of individual devices or revoke all sessions at any time. Telefam may suspend accounts that violate safety laws or Community Guidelines."
            ]
        ),
        TelefamLegalDocument(
            id: "privacy",
            title: "Privacy Policy",
            lastUpdated: "November 14, 2026",
            sections: [
                "1. Data We Collect: We collect registration details (email, name, username, country code, optional phone number, gender, date of birth, location, bio), uploaded avatars and background photos, posts, and settings preferences.",
                "2. Media & Device Fingerprinting: Uploaded media generates cryptographic SHA-256 digests, perceptual dHash values, and sampled video frame hashes solely to prevent duplicate abuse, verify upload integrity, and protect accounts from session theft.",
                "3. Your Privacy Controls: Inside Privacy settings, you control whether your account is Private, who can comment, mention, or discover you, whether activity status is visible, who sees your liked content or following list, and whether personalization informs your recommendations.",
                "4. Offline Caching & Retention: Essential profile and settings records are encrypted and cached on your device so preferences apply offline and sync securely upon reconnect.",
                "5. Your Rights: You may edit profile details, manage blocked or muted accounts, restrict non-essential cookies, or terminate all device sessions at any time."
            ]
        ),
        TelefamLegalDocument(
            id: "guidelines",
            title: "Community Guidelines",
            lastUpdated: "November 14, 2026",
            sections: [
                "1. Respect & Dignity: Telefam brings families and communities together. Do not post threats, hate speech, targeted harassment, or bullying.",
                "2. Protect Youth: Content involving the exploitation, grooming, or endangerment of minors is removed immediately and reported to competent authorities.",
                "3. Honesty & Authentic Representation: Avoid deceptive impersonation, financial scams, phishing links, or coordinated artificial engagement.",
                "4. Respect Creator Privacy: Never redistribute Friends-only or Private content without permission."
            ]
        ),
        TelefamLegalDocument(
            id: "cookies",
            title: "Cookie & Local Storage Settings",
            lastUpdated: "November 14, 2026",
            sections: [
                "1. Essential Storage (Required): Maintains authenticated device sessions, Keychain credentials, rotating refresh token security, and encrypted offline synchronization queues.",
                "2. Personalization Storage (Configurable): Remembers watched topics, muted topics, and recommendation preferences so feeds reflect your interests.",
                "3. Diagnostics & Performance (Configurable): Helps measure playback startup speed and adaptive streaming stability."
            ]
        ),
        TelefamLegalDocument(
            id: "licenses",
            title: "Licenses",
            lastUpdated: "November 14, 2026",
            sections: [
                "1. Application License: Telefam client software is licensed to you on a personal, non-exclusive, non-transferable basis solely to access Telefam services in compliance with the Terms of Service.",
                "2. Open Source Attribution: Full third-party license notices are accessible under Settings and privacy -> About -> Open source licenses."
            ]
        )
    ]

    static let openSourceLicenses: [TelefamOpenSourceLicense] = [
        TelefamOpenSourceLicense(library: "Swift Standard Library & SwiftUI", license: "Apache License 2.0", copyright: "Copyright Apple Inc."),
        TelefamOpenSourceLicense(library: "AVFoundation & CoreImage", license: "Apple SDK License", copyright: "Copyright Apple Inc."),
        TelefamOpenSourceLicense(library: "Apple CryptoKit & Security", license: "Apache License 2.0", copyright: "Copyright Apple Inc."),
        TelefamOpenSourceLicense(library: "Go Standard Library", license: "BSD 3-Clause License", copyright: "Copyright The Go Authors"),
        TelefamOpenSourceLicense(library: "PostgreSQL Row-Level Security", license: "PostgreSQL License", copyright: "Copyright PostgreSQL Global Development Group")
    ]
}

@MainActor
final class TelefamIosSettingsStore: ObservableObject {
    @Published var bundle = TelefamSettingsBundle()
    @Published var blockedAccounts: [TelefamRestrictedAccount] = []
    @Published var mutedAccounts: [TelefamRestrictedAccount] = []
    @Published var supportTickets: [TelefamSupportTicket] = []
    @Published var faqItems: [TelefamFaqEntry] = TelefamIosSettingsCatalog.defaultFaqs
    @Published var systemServices: [TelefamSystemServiceStatus] = []
    @Published var systemOverallStatus: String = "operational"
    @Published var systemCheckedAtMs: Int64 = 0
    @Published var legalDocs: [TelefamLegalDocument] = TelefamIosSettingsCatalog.legalDocuments
    @Published var versionCheckResult: TelefamVersionCheckResult? = nil
    @Published var lastCreatedTicket: TelefamSupportTicket? = nil
    @Published var isSubmittingReport: Bool = false
    @Published var isCheckingUpdates: Bool = false
    @Published var pendingOutboxCount: Int = 0
    @Published var isNetworkConnected: Bool = true
    @Published var isWifiConnected: Bool = true
    @Published var isSyncingNow: Bool = false
    @Published var lastSyncMessage: String = ""
    @Published var toastMessage: String? = nil

    private var outboxOps: [TelefamPendingSettingsOp] = []
    private let pathMonitor = NWPathMonitor()
    private var isNetworkSatisfied: Bool = true
    private var activeCacheUserId: String = ""

    let baseURL: URL
    private let userIdProvider: () -> String
    private let tokenProvider: () -> String
    let deviceFingerprint: String

    init(
        baseURL: URL,
        userIdProvider: @escaping () -> String,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String
    ) {
        self.baseURL = baseURL
        self.userIdProvider = userIdProvider
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint
        loadLocalCaches()
        startConnectivityObserver()
    }

    deinit {
        pathMonitor.cancel()
    }

    private func startConnectivityObserver() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                guard let self else { return }
                let nowSatisfied = path.status == .satisfied
                let nowWifi = path.usesInterfaceType(.wifi) || path.usesInterfaceType(.wiredEthernet)
                let recovered = !self.isNetworkSatisfied && nowSatisfied
                self.isNetworkSatisfied = nowSatisfied
                self.isNetworkConnected = nowSatisfied
                self.isWifiConnected = nowWifi
                let sp = self.bundle.syncPreferences
                if (recovered || nowSatisfied) && sp.autoSyncEnabled && (!sp.syncOverWifiOnly || nowWifi) {
                    await self.flushPendingOps()
                }
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "telefam.ios.settings.nw"))
    }

    private func storageDirectory() -> URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("telefam_settings", isDirectory: true)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    private func safeUid() -> String {
        let raw = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        if raw.isEmpty { return "anon" }
        return raw.replacingOccurrences(of: "/", with: "_")
    }

    private func cacheFile(_ name: String) -> URL {
        storageDirectory().appendingPathComponent("\(name)_\(safeUid()).json")
    }

    private func writeProtected<T: Encodable>(_ value: T, to fileName: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        try? data.write(to: cacheFile(fileName), options: [.atomic, .completeFileProtection])
    }

    private func readProtected<T: Decodable>(_ type: T.Type, from fileName: String) -> T? {
        guard let data = try? Data(contentsOf: cacheFile(fileName)) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    func loadLocalCaches() {
        let uid = safeUid()
        if activeCacheUserId == uid && !outboxOps.isEmpty { return }
        activeCacheUserId = uid
        if let saved = readProtected(TelefamSettingsBundle.self, from: "bundle") {
            bundle = saved
        } else {
            bundle = TelefamSettingsBundle()
        }
        blockedAccounts = readProtected([TelefamRestrictedAccount].self, from: "blocked") ?? []
        mutedAccounts = readProtected([TelefamRestrictedAccount].self, from: "muted") ?? []
        supportTickets = readProtected([TelefamSupportTicket].self, from: "tickets") ?? []
        outboxOps = readProtected([TelefamPendingSettingsOp].self, from: "outbox") ?? []
        pendingOutboxCount = outboxOps.count
    }

    func clearCachesForCurrentUser() {
        for name in ["bundle", "blocked", "muted", "tickets", "outbox"] {
            try? FileManager.default.removeItem(at: cacheFile(name))
        }
        bundle = TelefamSettingsBundle()
        blockedAccounts = []
        mutedAccounts = []
        supportTickets = []
        outboxOps = []
        pendingOutboxCount = 0
        lastSyncMessage = ""
    }

    func installedVersionString() -> String {
        let short = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        return (short?.isEmpty == false) ? short! : "1.0"
    }

    func installedBuildNumber() -> Int {
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String
        return Int(b ?? "") ?? 1
    }

    private func authorizedRequest(url: URL, method: String, contentType: String? = "application/json", body: Data? = nil) -> URLRequest {
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = method
        if let contentType {
            req.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        let token = tokenProvider()
        if !token.isEmpty {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = body
        return req
    }

    private struct BundleResp: Codable {
        let success: Bool
        let privacy: TelefamPrivacySettings?
        let notifications: TelefamNotificationSettings?
        let contentPreferences: TelefamContentPreferences?
        let chatPrivacy: TelefamChatPrivacySettings?
        let syncPreferences: TelefamSyncPreferences?

        enum CodingKeys: String, CodingKey {
            case success, privacy, notifications
            case contentPreferences = "content_preferences"
            case chatPrivacy = "chat_privacy"
            case syncPreferences = "sync_preferences"
        }
    }

    private struct SyncSettingsResp: Codable {
        let success: Bool
        let syncPreferences: TelefamSyncPreferences?

        enum CodingKeys: String, CodingKey {
            case success
            case syncPreferences = "sync_preferences"
        }
    }

    private struct ManualSyncRequestPayload: Codable {
        let clientSyncId: String
        let privacy: TelefamPrivacySettings
        let notifications: TelefamNotificationSettings
        let contentPreferences: TelefamContentPreferences
        let chatPrivacy: TelefamChatPrivacySettings
        let syncPreferences: TelefamSyncPreferences

        enum CodingKeys: String, CodingKey {
            case clientSyncId = "client_sync_id"
            case privacy, notifications
            case contentPreferences = "content_preferences"
            case chatPrivacy = "chat_privacy"
            case syncPreferences = "sync_preferences"
        }
    }

    private struct ManualSyncResp: Codable {
        let success: Bool
        let syncedOpsCount: Int?
        let privacy: TelefamPrivacySettings?
        let notifications: TelefamNotificationSettings?
        let contentPreferences: TelefamContentPreferences?
        let chatPrivacy: TelefamChatPrivacySettings?
        let syncPreferences: TelefamSyncPreferences?

        enum CodingKeys: String, CodingKey {
            case success
            case syncedOpsCount = "synced_ops_count"
            case privacy, notifications
            case contentPreferences = "content_preferences"
            case chatPrivacy = "chat_privacy"
            case syncPreferences = "sync_preferences"
        }
    }

    private struct ChatPrivacyResp: Codable {
        let success: Bool
        let chatPrivacy: TelefamChatPrivacySettings?

        enum CodingKeys: String, CodingKey {
            case success
            case chatPrivacy = "chat_privacy"
        }
    }

    private struct ChatThemeUpdatePayload: Codable {
        let chatThemeId: String
        let bubbleColorId: String

        enum CodingKeys: String, CodingKey {
            case chatThemeId = "chat_theme_id"
            case bubbleColorId = "bubble_color_id"
        }
    }

    func refreshAll() async {
        loadLocalCaches()
        await flushPendingOps()

        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings"), method: "GET", contentType: nil)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(BundleResp.self, from: data),
           parsed.success {
            var next = bundle
            if let p = parsed.privacy { next.privacy = p }
            if let n = parsed.notifications { next.notifications = n }
            if let c = parsed.contentPreferences { next.contentPreferences = c }
            if let cp = parsed.chatPrivacy { next.chatPrivacy = cp }
            if let sp = parsed.syncPreferences { next.syncPreferences = sp }
            bundle = next
            writeProtected(bundle, to: "bundle")
        }

        async let b = refreshRestrictedAccounts(isMuted: false, query: "")
        async let m = refreshRestrictedAccounts(isMuted: true, query: "")
        async let t = refreshSupportTickets()
        async let s = refreshSystemStatus()
        _ = await (b, m, t, s)
    }

    func saveChatPrivacy(_ next: TelefamChatPrivacySettings) async {
        var stamped = next
        stamped.chatThemeId = TelefamChatPrivacyCatalog.themeById(next.chatThemeId).themeId
        stamped.bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(next.bubbleColorId).colorId
        stamped.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        bundle.chatPrivacy = stamped
        writeProtected(bundle, to: "bundle")

        guard let encoded = try? JSONEncoder().encode(stamped) else { return }
        if await pushChatPrivacy(encoded) { return }
        enqueueOp(kind: "chat_privacy", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushChatPrivacy(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/chat/privacy"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(ChatPrivacyResp.self, from: data),
              parsed.success else {
            return false
        }
        if let cp = parsed.chatPrivacy {
            bundle.chatPrivacy = cp
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func saveChatThemeAndBubble(themeId: String? = nil, bubbleColorId: String? = nil) async {
        var cur = bundle.chatPrivacy
        let resolvedTheme = themeId.map { TelefamChatPrivacyCatalog.themeById($0).themeId } ?? cur.chatThemeId
        let resolvedBubble = bubbleColorId.map { TelefamChatPrivacyCatalog.bubbleColorById($0).colorId } ?? cur.bubbleColorId
        cur.chatThemeId = resolvedTheme
        cur.bubbleColorId = resolvedBubble
        cur.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        bundle.chatPrivacy = cur
        writeProtected(bundle, to: "bundle")

        let payload = ChatThemeUpdatePayload(chatThemeId: resolvedTheme, bubbleColorId: resolvedBubble)
        guard let encoded = try? JSONEncoder().encode(payload) else { return }
        if await pushChatTheme(encoded) { return }
        enqueueOp(kind: "chat_theme", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushChatTheme(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/chat/theme"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(ChatPrivacyResp.self, from: data),
              parsed.success else {
            return false
        }
        if let cp = parsed.chatPrivacy {
            bundle.chatPrivacy = cp
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func savePrivacy(_ next: TelefamPrivacySettings) async {
        var stamped = next
        stamped.essentialCookies = true
        stamped.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        bundle.privacy = stamped
        writeProtected(bundle, to: "bundle")

        guard let encoded = try? JSONEncoder().encode(stamped) else { return }
        if await pushPrivacy(encoded) { return }
        enqueueOp(kind: "privacy", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushPrivacy(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings/privacy"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(BundleResp.self, from: data),
              parsed.success else {
            return false
        }
        if let p = parsed.privacy {
            bundle.privacy = p
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func saveNotifications(_ next: TelefamNotificationSettings) async {
        var stamped = next
        stamped.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        bundle.notifications = stamped
        writeProtected(bundle, to: "bundle")

        guard let encoded = try? JSONEncoder().encode(stamped) else { return }
        if await pushNotifications(encoded) { return }
        enqueueOp(kind: "notifications", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushNotifications(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings/notifications"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(BundleResp.self, from: data),
              parsed.success else {
            return false
        }
        if let n = parsed.notifications {
            bundle.notifications = n
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func saveContentPreferences(_ next: TelefamContentPreferences, resetSuggestions: Bool = false) async {
        var stamped = next
        stamped.resetSuggestions = resetSuggestions
        stamped.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        var cleanTopics: [String] = []
        for raw in stamped.notInterestedTopics {
            let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
                .trimmingCharacters(in: CharacterSet(charactersIn: "#"))
                .lowercased()
            if !t.isEmpty && !cleanTopics.contains(t) && cleanTopics.count < 30 {
                cleanTopics.append(t)
            }
        }
        stamped.notInterestedTopics = cleanTopics
        bundle.contentPreferences = stamped
        bundle.contentPreferences.resetSuggestions = false
        writeProtected(bundle, to: "bundle")

        guard let encoded = try? JSONEncoder().encode(stamped) else { return }
        if await pushContentPreferences(encoded) {
            if resetSuggestions {
                showToast("Suggested feed signals reset")
            }
            return
        }
        enqueueOp(kind: "content_prefs", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushContentPreferences(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings/content-preferences"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(BundleResp.self, from: data),
              parsed.success else {
            return false
        }
        if var c = parsed.contentPreferences {
            c.resetSuggestions = false
            bundle.contentPreferences = c
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func saveSyncPreferences(_ next: TelefamSyncPreferences) async {
        var stamped = next
        stamped.updatedAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        bundle.syncPreferences = stamped
        writeProtected(bundle, to: "bundle")

        guard let encoded = try? JSONEncoder().encode(stamped) else { return }
        if await pushSyncPreferences(encoded) { return }
        enqueueOp(kind: "sync_prefs", data: encoded)
        showToast("Saved locally — will sync when online")
    }

    private func pushSyncPreferences(_ body: Data) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings/sync"), method: "PUT", body: body)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(SyncSettingsResp.self, from: data),
              parsed.success else {
            return false
        }
        if let sp = parsed.syncPreferences {
            bundle.syncPreferences = sp
            writeProtected(bundle, to: "bundle")
        }
        return true
    }

    func syncNowToServer() async {
        guard !isSyncingNow else { return }
        isSyncingNow = true
        lastSyncMessage = "Syncing queued changes with server..."
        defer { isSyncingNow = false }

        await flushPendingOps()

        let reqPayload = ManualSyncRequestPayload(
            clientSyncId: UUID().uuidString,
            privacy: bundle.privacy,
            notifications: bundle.notifications,
            contentPreferences: bundle.contentPreferences,
            chatPrivacy: bundle.chatPrivacy,
            syncPreferences: bundle.syncPreferences
        )
        guard let body = try? JSONEncoder().encode(reqPayload) else {
            lastSyncMessage = "Unable to encode sync payload"
            return
        }
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/sync/now"), method: "POST", body: body)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(ManualSyncResp.self, from: data),
           parsed.success {
            var next = bundle
            if let p = parsed.privacy { next.privacy = p }
            if let n = parsed.notifications { next.notifications = n }
            if var c = parsed.contentPreferences {
                c.resetSuggestions = false
                next.contentPreferences = c
            }
            if let cp = parsed.chatPrivacy { next.chatPrivacy = cp }
            if let sp = parsed.syncPreferences { next.syncPreferences = sp }
            bundle = next
            writeProtected(bundle, to: "bundle")
            pendingOutboxCount = outboxOps.count
            let msg = pendingOutboxCount == 0
                ? "All data synced with server"
                : "Synced \(parsed.syncedOpsCount ?? 0) item(s), \(pendingOutboxCount) pending"
            lastSyncMessage = msg
            showToast(msg)
            return
        }

        bundle.syncPreferences.lastSyncStatus = outboxOps.isEmpty ? bundle.syncPreferences.lastSyncStatus : "queued"
        writeProtected(bundle, to: "bundle")
        pendingOutboxCount = outboxOps.count
        lastSyncMessage = "Offline or unreachable — changes remain safely queued"
        showToast(lastSyncMessage)
    }

    func refreshRestrictedAccounts(isMuted: Bool, query: String) async {
        struct ListResp: Codable {
            let success: Bool
            let items: [TelefamRestrictedAccount]
        }
        let endpoint = isMuted ? "v1/settings/muted-accounts" : "v1/settings/blocked-accounts"
        var comps = URLComponents(url: baseURL.appendingPathComponent(endpoint), resolvingAgainstBaseURL: false)
        comps?.queryItems = [URLQueryItem(name: "q", value: query.trimmingCharacters(in: .whitespacesAndNewlines))]
        guard let url = comps?.url else { return }

        let req = authorizedRequest(url: url, method: "GET", contentType: nil)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(ListResp.self, from: data),
           parsed.success {
            let mapped = parsed.items.map {
                TelefamRestrictedAccount(
                    userId: $0.userId,
                    fullName: $0.fullName,
                    username: $0.username,
                    avatarUrl: $0.avatarUrl,
                    createdAtMs: $0.createdAtMs,
                    isMuted: isMuted
                )
            }
            if isMuted {
                mutedAccounts = mapped
                writeProtected(mapped, to: "muted")
            } else {
                blockedAccounts = mapped
                writeProtected(mapped, to: "blocked")
            }
        }
    }

    func unblockAccount(userId: String) async {
        blockedAccounts.removeAll { $0.userId == userId }
        writeProtected(blockedAccounts, to: "blocked")

        if await pushSimpleUserAction(path: "v1/users/\(userId)/unblock") {
            showToast("Account unblocked")
            return
        }
        enqueueOp(kind: "unblock", data: Data(userId.utf8))
        showToast("Saved offline — will unblock on reconnect")
    }

    func muteAccount(userId: String, fullName: String = "", username: String = "", avatarUrl: String = "") async {
        if !mutedAccounts.contains(where: { $0.userId == userId }) {
            mutedAccounts.insert(
                TelefamRestrictedAccount(
                    userId: userId,
                    fullName: fullName.isEmpty ? userId : fullName,
                    username: username.isEmpty ? userId : username,
                    avatarUrl: avatarUrl,
                    createdAtMs: Int64(Date().timeIntervalSince1970 * 1000),
                    isMuted: true
                ),
                at: 0
            )
            writeProtected(mutedAccounts, to: "muted")
        }
        if await pushSimpleUserAction(path: "v1/users/\(userId)/mute") {
            showToast("Account muted")
            return
        }
        enqueueOp(kind: "mute", data: Data(userId.utf8))
        showToast("Muted locally — will sync when online")
    }

    func unmuteAccount(userId: String) async {
        mutedAccounts.removeAll { $0.userId == userId }
        writeProtected(mutedAccounts, to: "muted")

        if await pushSimpleUserAction(path: "v1/users/\(userId)/unmute") {
            showToast("Account unmuted")
            return
        }
        enqueueOp(kind: "unmute", data: Data(userId.utf8))
        showToast("Saved offline — will unmute on reconnect")
    }

    private func pushSimpleUserAction(path: String) async -> Bool {
        let req = authorizedRequest(url: baseURL.appendingPathComponent(path), method: "POST", contentType: nil)
        guard let (_, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            return false
        }
        return true
    }

    func submitProblemReport(category: String, description: String, screenshotData: Data?) async {
        let cleanCat = category.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanDesc = description.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanCat.isEmpty, cleanDesc.count >= 5 else {
            showToast("Select a category and enter at least 5 characters")
            return
        }
        isSubmittingReport = true
        defer { isSubmittingReport = false }

        let opId = "rpt_\(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(14))"
        var jpegBytes: Data? = nil
        var shaHex = ""
        if let raw = screenshotData, let img = UIImage(data: raw), let compressed = img.jpegData(compressionQuality: 0.82) {
            jpegBytes = compressed
            shaHex = TelefamIosSessionStore.sha256Hex(data: compressed)
        }

        if let ticket = await uploadAndSubmitReport(opId: opId, category: cleanCat, description: cleanDesc, jpegBytes: jpegBytes, shaHex: shaHex) {
            supportTickets.insert(ticket, at: 0)
            writeProtected(supportTickets, to: "tickets")
            lastCreatedTicket = ticket
            return
        }

        var localJpegPath: String? = nil
        if let jpegBytes {
            let fileURL = storageDirectory().appendingPathComponent("rpt_\(opId).jpg")
            try? jpegBytes.write(to: fileURL, options: [.atomic, .completeFileProtection])
            localJpegPath = fileURL.path
        }

        let localRef = "TF-OFFLINE-\(opId.suffix(6).uppercased())"
        let offlineTicket = TelefamSupportTicket(
            ticketId: opId,
            referenceNumber: localRef,
            category: cleanCat,
            description: cleanDesc,
            screenshotMediaId: "",
            screenshotSha256: shaHex,
            status: "queued",
            resolutionNote: "Saved securely offline. Syncs automatically when internet returns.",
            createdAtMs: Int64(Date().timeIntervalSince1970 * 1000),
            updatedAtMs: Int64(Date().timeIntervalSince1970 * 1000),
            isSynced: false
        )
        supportTickets.insert(offlineTicket, at: 0)
        writeProtected(supportTickets, to: "tickets")
        lastCreatedTicket = offlineTicket

        let queued = PendingProblemReportPayload(
            clientOpId: opId,
            category: cleanCat,
            description: cleanDesc,
            localJpegPath: localJpegPath,
            screenshotSha256: shaHex
        )
        if let qData = try? JSONEncoder().encode(queued) {
            enqueueOp(kind: "problem_report", data: qData)
        }
    }

    private func uploadAndSubmitReport(
        opId: String,
        category: String,
        description: String,
        jpegBytes: Data?,
        shaHex: String
    ) async -> TelefamSupportTicket? {
        var mediaId = ""
        if let jpegBytes {
            var mReq = authorizedRequest(url: baseURL.appendingPathComponent("v1/media/photo"), method: "POST", contentType: "image/jpeg", body: jpegBytes)
            mReq.setValue("report_\(opId).jpg", forHTTPHeaderField: "X-Telefam-Upload-Name")
            if !shaHex.isEmpty {
                mReq.setValue(shaHex, forHTTPHeaderField: "X-Telefam-Content-SHA256")
            }
            if let (mData, mResp) = try? await URLSession.shared.data(for: mReq),
               let http = mResp as? HTTPURLResponse, (200...299).contains(http.statusCode),
               let obj = try? JSONSerialization.jsonObject(with: mData) as? [String: Any],
               let mid = obj["media_id"] as? String {
                mediaId = mid
            } else {
                return nil
            }
        }

        let bodyObj: [String: Any] = [
            "client_op_id": opId,
            "category": category,
            "description": description,
            "screenshot_media_id": mediaId,
            "screenshot_sha256": shaHex
        ]
        guard let bodyData = try? JSONSerialization.data(withJSONObject: bodyObj) else { return nil }
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/support/reports"), method: "POST", body: bodyData)

        struct Resp: Codable {
            let success: Bool
            let referenceNumber: String?
            let ticket: TelefamSupportTicket?
            enum CodingKeys: String, CodingKey {
                case success
                case referenceNumber = "reference_number"
                case ticket
            }
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(Resp.self, from: data),
              parsed.success else {
            return nil
        }

        if let t = parsed.ticket {
            return t
        }
        return TelefamSupportTicket(
            ticketId: opId,
            referenceNumber: parsed.referenceNumber ?? opId,
            category: category,
            description: description,
            screenshotMediaId: mediaId,
            screenshotSha256: shaHex,
            status: "open",
            resolutionNote: "Received by Telefam Support.",
            createdAtMs: Int64(Date().timeIntervalSince1970 * 1000),
            updatedAtMs: Int64(Date().timeIntervalSince1970 * 1000),
            isSynced: true
        )
    }

    func refreshSupportTickets() async {
        struct Resp: Codable {
            let success: Bool
            let items: [TelefamSupportTicket]
        }
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/support/tickets"), method: "GET", contentType: nil)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(Resp.self, from: data),
           parsed.success {
            let unsynced = supportTickets.filter { !$0.isSynced }
            supportTickets = unsynced + parsed.items
            writeProtected(supportTickets, to: "tickets")
        }
    }

    func searchFaqs(query: String) async {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        struct Resp: Codable {
            let success: Bool
            let items: [TelefamFaqEntry]
        }
        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/support/faqs"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [URLQueryItem(name: "q", value: q)]
        if let url = comps?.url {
            let req = authorizedRequest(url: url, method: "GET", contentType: nil)
            if let (data, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse,
               (200...299).contains(http.statusCode),
               let parsed = try? JSONDecoder().decode(Resp.self, from: data),
               parsed.success, !parsed.items.isEmpty {
                faqItems = parsed.items
                return
            }
        }
        let lower = q.lowercased()
        faqItems = TelefamIosSettingsCatalog.defaultFaqs.filter {
            lower.isEmpty ||
            $0.question.lowercased().contains(lower) ||
            $0.answer.lowercased().contains(lower) ||
            $0.category.lowercased().contains(lower)
        }
    }

    func refreshSystemStatus() async {
        struct Resp: Codable {
            let success: Bool
            let overallStatus: String?
            let services: [TelefamSystemServiceStatus]?
            let checkedAtMs: Int64?
            enum CodingKeys: String, CodingKey {
                case success
                case overallStatus = "overall_status"
                case services
                case checkedAtMs = "checked_at_ms"
            }
        }
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/settings/system-status"), method: "GET", contentType: nil)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(Resp.self, from: data),
           parsed.success {
            systemOverallStatus = parsed.overallStatus ?? "operational"
            if let services = parsed.services, !services.isEmpty {
                systemServices = services
            }
            systemCheckedAtMs = parsed.checkedAtMs ?? Int64(Date().timeIntervalSince1970 * 1000)
        } else if systemServices.isEmpty {
            systemOverallStatus = isNetworkSatisfied ? "degraded" : "offline"
            systemServices = [
                TelefamSystemServiceStatus(
                    serviceName: "Offline Cache & Outbox",
                    status: "operational",
                    latencyMs: 0,
                    summary: "Settings preferences are stored locally and sync upon reconnect."
                )
            ]
        }
    }

    func refreshLegalDocuments() async {
        struct Resp: Codable {
            let success: Bool
            let items: [TelefamLegalDocument]
        }
        let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/legal/documents"), method: "GET", contentType: nil)
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(Resp.self, from: data),
           parsed.success, !parsed.items.isEmpty {
            legalDocs = parsed.items
        }
    }

    func checkForUpdates() async {
        isCheckingUpdates = true
        defer { isCheckingUpdates = false }

        let installed = installedVersionString()
        let build = installedBuildNumber()
        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/app/version"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [URLQueryItem(name: "version", value: installed)]

        struct Resp: Codable {
            let success: Bool
            let latestVersion: String?
            let updateAvailable: Bool?
            let releaseNotes: String?
            enum CodingKeys: String, CodingKey {
                case success
                case latestVersion = "latest_version"
                case updateAvailable = "update_available"
                case releaseNotes = "release_notes"
            }
        }

        if let url = comps?.url {
            let req = authorizedRequest(url: url, method: "GET", contentType: nil)
            if let (data, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse,
               (200...299).contains(http.statusCode),
               let parsed = try? JSONDecoder().decode(Resp.self, from: data),
               parsed.success {
                versionCheckResult = TelefamVersionCheckResult(
                    installedVersion: installed,
                    installedBuild: build,
                    latestVersion: parsed.latestVersion ?? installed,
                    updateAvailable: parsed.updateAvailable ?? false,
                    releaseNotes: parsed.releaseNotes ?? "You are running the latest version of Telefam.",
                    checkedAtMs: Int64(Date().timeIntervalSince1970 * 1000)
                )
                return
            }
        }

        versionCheckResult = TelefamVersionCheckResult(
            installedVersion: installed,
            installedBuild: build,
            latestVersion: installed,
            updateAvailable: false,
            releaseNotes: "Unable to reach update server. Installed version verified locally.",
            checkedAtMs: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }

    private func enqueueOp(kind: String, data: Data) {
        outboxOps.removeAll {
            $0.opKind == kind && (kind == "privacy" || kind == "notifications" || kind == "content_prefs" || kind == "chat_privacy" || kind == "chat_theme" || kind == "sync_prefs")
        }
        outboxOps.append(
            TelefamPendingSettingsOp(
                opId: UUID().uuidString,
                opKind: kind,
                payloadData: data,
                createdAtMs: Int64(Date().timeIntervalSince1970 * 1000)
            )
        )
        pendingOutboxCount = outboxOps.count
        writeProtected(outboxOps, to: "outbox")
    }

    func flushPendingOps() async {
        guard !outboxOps.isEmpty, !tokenProvider().isEmpty else {
            pendingOutboxCount = outboxOps.count
            return
        }
        var remaining: [TelefamPendingSettingsOp] = []
        for op in outboxOps {
            var ok = false
            switch op.opKind {
            case "privacy":
                ok = await pushPrivacy(op.payloadData)
            case "notifications":
                ok = await pushNotifications(op.payloadData)
            case "content_prefs":
                ok = await pushContentPreferences(op.payloadData)
            case "chat_privacy":
                ok = await pushChatPrivacy(op.payloadData)
            case "chat_theme":
                ok = await pushChatTheme(op.payloadData)
            case "sync_prefs":
                ok = await pushSyncPreferences(op.payloadData)
            case "unblock":
                if let uid = String(data: op.payloadData, encoding: .utf8), !uid.isEmpty {
                    ok = await pushSimpleUserAction(path: "v1/users/\(uid)/unblock")
                }
            case "mute":
                if let uid = String(data: op.payloadData, encoding: .utf8), !uid.isEmpty {
                    ok = await pushSimpleUserAction(path: "v1/users/\(uid)/mute")
                }
            case "unmute":
                if let uid = String(data: op.payloadData, encoding: .utf8), !uid.isEmpty {
                    ok = await pushSimpleUserAction(path: "v1/users/\(uid)/unmute")
                }
            case "problem_report":
                if let decoded = try? JSONDecoder().decode(PendingProblemReportPayload.self, from: op.payloadData) {
                    var jpegBytes: Data? = nil
                    if let path = decoded.localJpegPath, !path.isEmpty {
                        jpegBytes = try? Data(contentsOf: URL(fileURLWithPath: path))
                    }
                    if let ticket = await uploadAndSubmitReport(
                        opId: decoded.clientOpId,
                        category: decoded.category,
                        description: decoded.description,
                        jpegBytes: jpegBytes,
                        shaHex: decoded.screenshotSha256
                    ) {
                        supportTickets.removeAll { $0.ticketId == decoded.clientOpId }
                        supportTickets.insert(ticket, at: 0)
                        writeProtected(supportTickets, to: "tickets")
                        if let path = decoded.localJpegPath {
                            try? FileManager.default.removeItem(atPath: path)
                        }
                        ok = true
                    }
                }
            default:
                ok = true
            }
            if !ok {
                remaining.append(op)
            }
        }
        outboxOps = remaining
        pendingOutboxCount = outboxOps.count
        writeProtected(outboxOps, to: "outbox")
    }

    func showToast(_ message: String) {
        toastMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in
            if self?.toastMessage == message { self?.toastMessage = nil }
        }
    }
}
