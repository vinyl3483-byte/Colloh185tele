import Foundation
import SwiftUI
import UIKit
import UserNotifications

// MARK: - Notification Models & Routes

struct TelefamIosNotificationItem: Identifiable, Codable, Equatable {
    let id: String
    let recipientUserId: String
    let actorUserId: String
    let actorName: String
    let actorUsername: String
    let actorAvatarUrl: String
    let kind: String // "chat_message", "follow", "like", "mention"
    let title: String
    let body: String
    let conversationId: String
    let messageId: String
    let postId: String
    let targetUserId: String
    let isViewOnce: Bool
    var isRead: Bool
    let createdAtMs: Int64

    enum CodingKeys: String, CodingKey {
        case id
        case recipientUserId = "recipient_user_id"
        case actorUserId = "actor_user_id"
        case actorName = "actor_name"
        case actorUsername = "actor_username"
        case actorAvatarUrl = "actor_avatar_url"
        case kind, title, body
        case conversationId = "conversation_id"
        case messageId = "message_id"
        case postId = "post_id"
        case targetUserId = "target_user_id"
        case isViewOnce = "is_view_once"
        case isRead = "is_read"
        case createdAtMs = "created_at_ms"
    }

    init(
        id: String,
        recipientUserId: String = "",
        actorUserId: String = "",
        actorName: String = "",
        actorUsername: String = "",
        actorAvatarUrl: String = "",
        kind: String,
        title: String,
        body: String,
        conversationId: String = "",
        messageId: String = "",
        postId: String = "",
        targetUserId: String = "",
        isViewOnce: Bool = false,
        isRead: Bool = false,
        createdAtMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.recipientUserId = recipientUserId
        self.actorUserId = actorUserId
        self.actorName = actorName
        self.actorUsername = actorUsername
        self.actorAvatarUrl = actorAvatarUrl
        self.kind = kind
        self.title = title
        self.body = body
        self.conversationId = conversationId
        self.messageId = messageId
        self.postId = postId
        self.targetUserId = targetUserId
        self.isViewOnce = isViewOnce
        self.isRead = isRead
        self.createdAtMs = createdAtMs
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? ""
        recipientUserId = try c.decodeIfPresent(String.self, forKey: .recipientUserId) ?? ""
        actorUserId = try c.decodeIfPresent(String.self, forKey: .actorUserId) ?? ""
        actorName = try c.decodeIfPresent(String.self, forKey: .actorName) ?? ""
        actorUsername = try c.decodeIfPresent(String.self, forKey: .actorUsername) ?? ""
        actorAvatarUrl = try c.decodeIfPresent(String.self, forKey: .actorAvatarUrl) ?? ""
        kind = try c.decodeIfPresent(String.self, forKey: .kind) ?? "mention"
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        body = try c.decodeIfPresent(String.self, forKey: .body) ?? ""
        conversationId = try c.decodeIfPresent(String.self, forKey: .conversationId) ?? ""
        messageId = try c.decodeIfPresent(String.self, forKey: .messageId) ?? ""
        postId = try c.decodeIfPresent(String.self, forKey: .postId) ?? ""
        targetUserId = try c.decodeIfPresent(String.self, forKey: .targetUserId) ?? ""
        isViewOnce = try c.decodeIfPresent(Bool.self, forKey: .isViewOnce) ?? false
        isRead = try c.decodeIfPresent(Bool.self, forKey: .isRead) ?? false
        createdAtMs = try c.decodeIfPresent(Int64.self, forKey: .createdAtMs) ?? 0
    }
}

enum TelefamIosNotificationRoute: Equatable {
    case chat(conversationId: String, peerUserId: String)
    case profile(userId: String)
    case post(postId: String)
    case stars
    case verification
    case inbox
}

struct TelefamIosIncomingNotificationBanner: Equatable, Identifiable {
    let id: String
    let kind: String
    let title: String
    let body: String
    let conversationId: String
    let peerUserId: String
    let messageId: String
    let postId: String
    let targetUserId: String
    let isViewOnce: Bool
    let canReply: Bool
}

// MARK: - AppDelegate for APNs / Remote Push Registration

final class TelefamIosAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        TelefamIosNotificationManager.shared.configureCategoriesAndDelegate()
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let hexToken = deviceToken.map { String(format: "%02x", $0) }.joined()
        Task { @MainActor in
            await TelefamIosNotificationManager.shared.onApnsTokenReceived(hexToken)
        }
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // APNs unavailable in simulator or restricted environment; WebSocket/polling fallback continues.
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Task { @MainActor in
            let handled = await TelefamIosNotificationManager.shared.handleRemoteNotificationPayload(userInfo)
            completionHandler(handled ? .newData : .noData)
        }
    }
}

// MARK: - Notification Center Delegate, FCM/APNs Sync & Inbox State

@MainActor
final class TelefamIosNotificationManager: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    static let shared = TelefamIosNotificationManager()

    static let categoryChatReply = "TELEFAM_CHAT_MESSAGE"
    static let categoryChatViewOnce = "TELEFAM_CHAT_VIEW_ONCE"
    static let categorySocial = "TELEFAM_SOCIAL"
    static let categoryIncomingCall = "TELEFAM_INCOMING_CALL"

    static let actionReply = "TELEFAM_ACTION_REPLY"
    static let actionMarkRead = "TELEFAM_ACTION_MARK_READ"
    static let actionCallAccept = "TELEFAM_CALL_ACCEPT"
    static let actionCallDecline = "TELEFAM_CALL_DECLINE"

    @Published var notifications: [TelefamIosNotificationItem] = []
    @Published var unreadCount: Int = 0
    @Published var showInboxSheet: Bool = false
    @Published var pendingRoute: TelefamIosNotificationRoute? = nil
    @Published var activeBanner: TelefamIosIncomingNotificationBanner? = nil
    @Published var lastSyncedToken: String = ""

    private weak var sessionStore: TelefamIosSessionStore?
    private weak var chatStore: TelefamIosChatStore?
    private weak var settingsStore: TelefamIosSettingsStore?
    private var lastRegisteredUserId: String = ""
    private var bannerDismissTask: Task<Void, Never>? = nil

    private override init() {
        super.init()
    }

    func configureCategoriesAndDelegate() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self

        let replyAction = UNTextInputNotificationAction(
            identifier: Self.actionReply,
            title: "Reply",
            options: [],
            textInputButtonTitle: "Send",
            textInputPlaceholder: "Message"
        )
        let markReadAction = UNNotificationAction(
            identifier: Self.actionMarkRead,
            title: "Mark as read",
            options: []
        )

        let chatCategory = UNNotificationCategory(
            identifier: Self.categoryChatReply,
            actions: [replyAction, markReadAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        // Strictly omit inline direct reply for view-once voice notes
        let viewOnceCategory = UNNotificationCategory(
            identifier: Self.categoryChatViewOnce,
            actions: [markReadAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        let socialCategory = UNNotificationCategory(
            identifier: Self.categorySocial,
            actions: [markReadAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        let acceptCallAction = UNNotificationAction(
            identifier: Self.actionCallAccept,
            title: "Accept",
            options: [.foreground]
        )
        let declineCallAction = UNNotificationAction(
            identifier: Self.actionCallDecline,
            title: "Decline",
            options: [.destructive]
        )
        let incomingCallCategory = UNNotificationCategory(
            identifier: Self.categoryIncomingCall,
            actions: [acceptCallAction, declineCallAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        center.setNotificationCategories([chatCategory, viewOnceCategory, socialCategory, incomingCallCategory])
    }

    func requestAuthorizationIfNeeded() {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            if settings.authorizationStatus == .notDetermined {
                center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                    if granted {
                        DispatchQueue.main.async {
                            UIApplication.shared.registerForRemoteNotifications()
                        }
                    }
                }
            }
        }
    }

    func attach(
        sessionStore: TelefamIosSessionStore,
        chatStore: TelefamIosChatStore,
        settingsStore: TelefamIosSettingsStore
    ) {
        self.sessionStore = sessionStore
        self.chatStore = chatStore
        self.settingsStore = settingsStore

        let uid = sessionStore.userId.trimmingCharacters(in: .whitespacesAndNewlines)
        if !uid.isEmpty, uid != lastRegisteredUserId {
            lastRegisteredUserId = uid
            restoreCachedInbox(userId: uid)
        }
        requestAuthorizationIfNeeded()

        Task {
            if !lastSyncedToken.isEmpty {
                await registerPushTokenWithBackend(token: lastSyncedToken)
            }
            await refreshNotifications()
        }
    }

    func onApnsTokenReceived(_ token: String) async {
        let clean = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        lastSyncedToken = clean
        await registerPushTokenWithBackend(token: clean)
    }

    func registerPushTokenWithBackend(token: String) async {
        guard let session = sessionStore,
              session.isAuthenticated,
              !session.sessionToken.isEmpty,
              let url = session.serverURL?.appendingPathComponent("v1/notifications/push-token") else {
            return
        }
        let bundleId = Bundle.main.bundleIdentifier ?? "com.telefam.company.ios"
        let payload: [String: Any] = [
            "token": token,
            "platform": "ios",
            "app_bundle": bundleId
        ]
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 10)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(session.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(session.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)
        _ = try? await URLSession.shared.data(for: req)
    }

    func unregisterPushTokenBeforeLogout(
        serverURL: URL?,
        bearerToken: String,
        deviceFingerprint: String,
        allDevices: Bool
    ) async {
        guard let serverURL, !bearerToken.isEmpty,
              var comps = URLComponents(url: serverURL.appendingPathComponent("v1/notifications/push-token"), resolvingAgainstBaseURL: false) else {
            return
        }
        comps.queryItems = [URLQueryItem(name: "all_devices", value: allDevices ? "true" : "false")]
        guard let url = comps.url else { return }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 6)
        req.httpMethod = "DELETE"
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        _ = try? await URLSession.shared.data(for: req)
    }

    func clearOnLogout() {
        bannerDismissTask?.cancel()
        bannerDismissTask = nil
        activeBanner = nil
        pendingRoute = nil
        showInboxSheet = false
        notifications = []
        unreadCount = 0
        if !lastRegisteredUserId.isEmpty {
            try? FileManager.default.removeItem(at: cacheURL(userId: lastRegisteredUserId))
        }
        lastRegisteredUserId = ""
        let center = UNUserNotificationCenter.current()
        center.removeAllDeliveredNotifications()
        center.removeAllPendingNotificationRequests()
        UIApplication.shared.applicationIconBadgeNumber = 0
    }

    func refreshNotifications() async {
        guard let session = sessionStore,
              session.isAuthenticated,
              !session.sessionToken.isEmpty,
              let base = session.serverURL,
              var comps = URLComponents(url: base.appendingPathComponent("v1/notifications"), resolvingAgainstBaseURL: false) else {
            return
        }
        comps.queryItems = [URLQueryItem(name: "limit", value: "50")]
        guard let url = comps.url else { return }

        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 10)
        req.httpMethod = "GET"
        req.setValue("Bearer \(session.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(session.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        struct ListResp: Codable {
            let success: Bool
            let items: [TelefamIosNotificationItem]?
            let unreadCount: Int?
            enum CodingKeys: String, CodingKey {
                case success, items
                case unreadCount = "unread_count"
            }
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(ListResp.self, from: data),
              parsed.success else {
            return
        }

        let sorted = (parsed.items ?? []).sorted { $0.createdAtMs > $1.createdAtMs }
        notifications = sorted
        unreadCount = parsed.unreadCount ?? sorted.filter { !$0.isRead }.count
        saveCachedInbox(userId: session.userId)
        UIApplication.shared.applicationIconBadgeNumber = unreadCount
    }

    func markNotificationRead(_ notificationId: String) async {
        guard !notificationId.isEmpty else { return }
        if let idx = notifications.firstIndex(where: { $0.id == notificationId }), !notifications[idx].isRead {
            notifications[idx].isRead = true
            unreadCount = notifications.filter { !$0.isRead }.count
            if let uid = sessionStore?.userId { saveCachedInbox(userId: uid) }
        }
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ["social_\(notificationId)"])

        guard let session = sessionStore,
              session.isAuthenticated,
              !session.sessionToken.isEmpty,
              let url = session.serverURL?.appendingPathComponent("v1/notifications/read") else { return }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(session.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(session.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["notification_id": notificationId])
        _ = try? await URLSession.shared.data(for: req)
        UIApplication.shared.applicationIconBadgeNumber = unreadCount
    }

    func markAllNotificationsRead() async {
        for idx in notifications.indices {
            notifications[idx].isRead = true
        }
        unreadCount = 0
        if let uid = sessionStore?.userId { saveCachedInbox(userId: uid) }
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        UIApplication.shared.applicationIconBadgeNumber = 0

        guard let session = sessionStore,
              session.isAuthenticated,
              !session.sessionToken.isEmpty,
              let url = session.serverURL?.appendingPathComponent("v1/notifications/read-all") else { return }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(session.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(session.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = Data("{}".utf8)
        _ = try? await URLSession.shared.data(for: req)
    }

    func cancelConversationNotifications(conversationId: String) {
        guard !conversationId.isEmpty else { return }
        let prefix = "chat_\(conversationId)"
        let center = UNUserNotificationCenter.current()
        center.getDeliveredNotifications { delivered in
            let matches = delivered.map(\.request.identifier).filter { $0 == prefix || $0.hasPrefix("\(prefix)_") }
            if !matches.isEmpty {
                center.removeDeliveredNotifications(withIdentifiers: matches)
            }
        }
        if activeBanner?.conversationId == conversationId {
            activeBanner = nil
        }
    }

    func openNotificationItem(_ item: TelefamIosNotificationItem) {
        showInboxSheet = false
        Task { await markNotificationRead(item.id) }
        if item.kind == "star_received" {
            pendingRoute = .stars
        } else if item.kind == "verification_update" {
            pendingRoute = .verification
        } else if !item.conversationId.isEmpty {
            pendingRoute = .chat(conversationId: item.conversationId, peerUserId: item.actorUserId)
        } else if !item.postId.isEmpty {
            pendingRoute = .post(postId: item.postId)
        } else if !item.actorUserId.isEmpty {
            pendingRoute = .profile(userId: item.actorUserId)
        } else if !item.targetUserId.isEmpty {
            pendingRoute = .profile(userId: item.targetUserId)
        }
    }

    func dismissBanner() {
        bannerDismissTask?.cancel()
        bannerDismissTask = nil
        activeBanner = nil
    }

    // MARK: - Quiet Hours & Push Preferences

    static func isQuietHoursActive(
        settings: TelefamNotificationSettings,
        nowMinutesOfDay: Int = currentMinutesOfDay()
    ) -> Bool {
        guard settings.quietHoursEnabled else { return false }
        let start = ((settings.quietStartMinutes % 1440) + 1440) % 1440
        let end = ((settings.quietEndMinutes % 1440) + 1440) % 1440
        if start == end { return false }
        if start < end {
            return nowMinutesOfDay >= start && nowMinutesOfDay < end
        } else {
            return nowMinutesOfDay >= start || nowMinutesOfDay < end
        }
    }

    static func shouldDeliverNotification(
        settings: TelefamNotificationSettings,
        kind: String,
        nowMinutesOfDay: Int = currentMinutesOfDay()
    ) -> Bool {
        guard !settings.masterPause, settings.channelPush else { return false }
        guard !isQuietHoursActive(settings: settings, nowMinutesOfDay: nowMinutesOfDay) else { return false }
        switch kind.lowercased() {
        case "chat_message": return settings.typeMessages
        case "follow": return settings.typeFollowers
        case "like", "mention", "comment": return settings.typeComments
        default: return true
        }
    }

    private static func currentMinutesOfDay() -> Int {
        let comps = Calendar.current.dateComponents([.hour, .minute], from: Date())
        return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
    }

    // MARK: - Remote Push & In-App Notification Dispatch

    func handleRemoteNotificationPayload(_ userInfo: [AnyHashable: Any]) async -> Bool {
        guard let session = sessionStore,
              session.isAuthenticated,
              !session.userId.isEmpty else { return false }

        var data: [String: String] = [:]
        for (k, v) in userInfo {
            if let ks = k as? String {
                if let vs = v as? String {
                    data[ks] = vs
                } else if let vn = v as? NSNumber {
                    data[ks] = vn.stringValue
                }
            }
        }

        let targetRecipient = (data["recipient_user_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if !targetRecipient.isEmpty && targetRecipient != session.userId {
            return false
        }

        let type = (data["type"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let notifSettings = settingsStore?.bundle.notifications ?? TelefamNotificationSettings()

        switch type {
        case "chat_message":
            let convId = (data["conversation_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let msgId = (data["message_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let senderId = (data["sender_user_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let senderName = (data["sender_name"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let senderUsername = (data["sender_username"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let msgType = (data["message_type"] ?? "text").trimmingCharacters(in: .whitespacesAndNewlines)
            let codec = (data["compression_codec"] ?? "zlib").trimmingCharacters(in: .whitespacesAndNewlines)
            let cB64 = data["ciphertext_b64"] ?? ""
            let isViewOnce = data["is_view_once"] == "true" || msgType == "voice_once"

            let decoded = await chatStore?.decryptRemotePushEnvelope(
                conversationId: convId,
                messageId: msgId,
                senderUserId: senderId,
                senderName: senderName,
                senderUsername: senderUsername,
                messageType: msgType,
                compressionCodec: codec,
                ciphertextB64: cB64,
                isViewOnce: isViewOnce,
                createdAtMs: Int64(data["created_at_ms"] ?? "") ?? Int64(Date().timeIntervalSince1970 * 1000)
            )
            let isMuted = decoded?.isMuted ?? false
            let preview = isViewOnce ? "Sent a view-once voice message" : (decoded?.preview ?? "New encrypted message")
            let canReply = !isViewOnce && (decoded?.canReply ?? true)
            let displaySender = senderName.isEmpty ? (senderUsername.isEmpty ? "Telefam" : "@\(senderUsername)") : senderName

            if !isMuted && Self.shouldDeliverNotification(settings: notifSettings, kind: "chat_message") {
                scheduleLocalChatNotification(
                    conversationId: convId,
                    messageId: msgId,
                    peerUserId: senderId,
                    senderName: displaySender,
                    body: preview,
                    isViewOnce: isViewOnce,
                    canReply: canReply
                )
            }
            return true

        case "message_cancel":
            let convId = (data["conversation_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let msgId = (data["message_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            await chatStore?.handleRemoteMessageCancel(conversationId: convId, messageId: msgId)
            notifications.removeAll { !$0.messageId.isEmpty && $0.messageId == msgId }
            unreadCount = notifications.filter { !$0.isRead }.count
            saveCachedInbox(userId: session.userId)
            cancelConversationNotifications(conversationId: convId)
            return true

        case "follow", "like", "mention", "star_received", "verification_update":
            let actorId = (data["actor_user_id"] ?? data["sender_user_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if type != "verification_update" && actorId == session.userId { return false }
            let notifId = (data["notification_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let finalId = notifId.isEmpty ? "push_\(type)_\(actorId)_\(Int(Date().timeIntervalSince1970 * 1000))" : notifId
            let title = (data["title"] ?? data["actor_name"] ?? "Telefam").trimmingCharacters(in: .whitespacesAndNewlines)
            let defaultBody: String = {
                switch type {
                case "follow": return "Started following you"
                case "like": return "Liked your post"
                case "star_received": return "Sent you Telefam Stars"
                case "verification_update": return "Your Telefam verification status was updated"
                default: return "Mentioned you"
                }
            }()
            let body = (data["body"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? defaultBody : (data["body"] ?? defaultBody)
            let postId = (data["post_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let convId = (data["conversation_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let item = TelefamIosNotificationItem(
                id: finalId,
                recipientUserId: session.userId,
                actorUserId: actorId,
                actorName: data["actor_name"] ?? "",
                actorUsername: data["actor_username"] ?? "",
                actorAvatarUrl: data["actor_avatar_url"] ?? "",
                kind: type,
                title: title,
                body: body,
                conversationId: convId,
                messageId: data["message_id"] ?? "",
                postId: postId,
                targetUserId: data["target_user_id"] ?? actorId,
                isViewOnce: false,
                isRead: false,
                createdAtMs: Int64(data["created_at_ms"] ?? "") ?? Int64(Date().timeIntervalSince1970 * 1000)
            )
            notifications.removeAll { $0.id == finalId }
            notifications.insert(item, at: 0)
            unreadCount = notifications.filter { !$0.isRead }.count
            saveCachedInbox(userId: session.userId)

            if type == "star_received" {
                NotificationCenter.default.post(name: .telefamStarsUpdatedWs, object: nil, userInfo: [
                    "kind": "received",
                    "delta_stars": Int64(data["stars"] ?? "") ?? 0,
                    "counterparty_name": data["actor_name"] ?? ""
                ])
            } else if type == "verification_update" {
                NotificationCenter.default.post(name: .telefamVerificationUpdatedWs, object: nil, userInfo: [
                    "verified_tier": data["verified_tier"] ?? "",
                    "review_status": data["review_status"] ?? ""
                ])
            }
            if Self.shouldDeliverNotification(settings: notifSettings, kind: type) {
                scheduleLocalSocialNotification(item: item)
            }
            return true

        case "incoming_call":
            let callId = (data["call_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            guard !callId.isEmpty else { return false }
            let callerName = (data["caller_name"] ?? data["actor_name"] ?? "Telefam").trimmingCharacters(in: .whitespacesAndNewlines)
            let callType = (data["call_type"] ?? "audio").lowercased() == "video" ? "Video call" : "Audio call"
            TelefamIosCallPushBridge.shared.handleIncomingPush(data: data)
            let content = UNMutableNotificationContent()
            content.title = callerName.isEmpty ? "Telefam" : callerName
            content.body = "Incoming \(callType.lowercased())"
            content.sound = .default
            content.categoryIdentifier = Self.categoryIncomingCall
            content.userInfo = [
                "route": "incoming_call",
                "call_id": callId,
                "conversation_id": data["conversation_id"] ?? "",
                "peer_user_id": data["caller_user_id"] ?? data["actor_user_id"] ?? ""
            ]
            let req = UNNotificationRequest(identifier: "call_\(callId)", content: content, trigger: nil)
            UNUserNotificationCenter.current().add(req)
            return true

        case "call_cancel":
            let callId = (data["call_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !callId.isEmpty {
                UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ["call_\(callId)"])
                UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["call_\(callId)"])
                TelefamIosCallPushBridge.shared.handleCallCancelPush(callId: callId)
            }
            return true

        case "missed_call":
            let callId = (data["call_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !callId.isEmpty {
                UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ["call_\(callId)"])
            }
            let convId = (data["conversation_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !convId.isEmpty {
                await chatStore?.refreshConversationFromCall(convId)
            }
            return true

        default:
            return false
        }
    }

    func notifyIncomingChatMessage(
        conversationId: String,
        message: TelefamIosChatMessage,
        senderName: String,
        isMuted: Bool
    ) {
        guard !isMuted else { return }
        let notifSettings = settingsStore?.bundle.notifications ?? TelefamNotificationSettings()
        guard Self.shouldDeliverNotification(settings: notifSettings, kind: "chat_message") else { return }
        let isViewOnce = message.messageType == "voice_once"
        let body: String
        if isViewOnce {
            body = "Sent a view-once voice message"
        } else {
            switch message.messageType {
            case "voice": body = "Sent a voice message"
            case "sticker": body = "Sent a sticker"
            case "gif": body = "Sent a GIF"
            case "clip": body = "Sent a clip"
            case "star_gift": body = TelefamIosStarsCatalog.formatSummaryLine(rawJson: message.plainText)
            default:
                let clean = message.plainText.trimmingCharacters(in: .whitespacesAndNewlines)
                body = (clean.isEmpty || clean == "[Encrypted message]") ? "New encrypted message" : clean
            }
        }
        showForegroundBanner(
            TelefamIosIncomingNotificationBanner(
                id: "chat_\(message.messageId)",
                kind: "chat_message",
                title: senderName.isEmpty ? "Telefam" : senderName,
                body: body,
                conversationId: conversationId,
                peerUserId: message.senderUserId,
                messageId: message.messageId,
                postId: "",
                targetUserId: message.senderUserId,
                isViewOnce: isViewOnce,
                canReply: !isViewOnce
            )
        )
    }

    private func scheduleLocalChatNotification(
        conversationId: String,
        messageId: String,
        peerUserId: String,
        senderName: String,
        body: String,
        isViewOnce: Bool,
        canReply: Bool
    ) {
        let content = UNMutableNotificationContent()
        content.title = senderName
        content.body = isViewOnce ? "Sent a view-once voice message" : body
        content.sound = .default
        content.threadIdentifier = conversationId
        content.categoryIdentifier = (isViewOnce || !canReply) ? Self.categoryChatViewOnce : Self.categoryChatReply
        content.userInfo = [
            "route": "chat",
            "conversation_id": conversationId,
            "peer_user_id": peerUserId,
            "message_id": messageId,
            "is_view_once": isViewOnce
        ]
        let req = UNNotificationRequest(
            identifier: "chat_\(conversationId)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(req)
    }

    private func scheduleLocalSocialNotification(item: TelefamIosNotificationItem) {
        let content = UNMutableNotificationContent()
        content.title = item.title
        content.body = item.body
        content.sound = .default
        content.categoryIdentifier = Self.categorySocial
        let route = item.kind == "star_received" ? "stars" : (!item.conversationId.isEmpty ? "chat" : (!item.postId.isEmpty ? "post" : "profile"))
        content.userInfo = [
            "route": route,
            "notification_id": item.id,
            "conversation_id": item.conversationId,
            "peer_user_id": item.actorUserId,
            "target_user_id": item.targetUserId.isEmpty ? item.actorUserId : item.targetUserId,
            "post_id": item.postId
        ]
        let req = UNNotificationRequest(
            identifier: "social_\(item.id)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(req)
    }

    private func showForegroundBanner(_ banner: TelefamIosIncomingNotificationBanner) {
        bannerDismissTask?.cancel()
        withAnimation(.spring(response: 0.32, dampingFraction: 0.85)) {
            activeBanner = banner
        }
        bannerDismissTask = Task {
            try? await Task.sleep(nanoseconds: 6_500_000_000)
            if !Task.isCancelled && activeBanner?.id == banner.id {
                withAnimation(.easeOut(duration: 0.2)) {
                    activeBanner = nil
                }
            }
        }
    }

    // MARK: - UNUserNotificationCenterDelegate

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        Task { @MainActor in
            let convId = userInfo["conversation_id"] as? String ?? ""
            if !convId.isEmpty && chatStore?.activeConversationId == convId {
                completionHandler([])
                return
            }
            completionHandler([.banner, .sound, .badge])
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let actionId = response.actionIdentifier
        let textResponse = (response as? UNTextInputNotificationResponse)?.userText ?? ""

        Task { @MainActor in
            let convId = userInfo["conversation_id"] as? String ?? ""
            let peerId = userInfo["peer_user_id"] as? String ?? ""
            let notifId = userInfo["notification_id"] as? String ?? ""
            let postId = userInfo["post_id"] as? String ?? ""
            let targetUid = userInfo["target_user_id"] as? String ?? peerId
            let isViewOnce = userInfo["is_view_once"] as? Bool ?? false

            switch actionId {
            case Self.actionReply:
                let clean = textResponse.trimmingCharacters(in: .whitespacesAndNewlines)
                if !isViewOnce && !convId.isEmpty && !peerId.isEmpty && !clean.isEmpty {
                    await chatStore?.sendQuickReplyFromNotification(conversationId: convId, peerUserId: peerId, text: clean)
                }
                cancelConversationNotifications(conversationId: convId)

            case Self.actionMarkRead:
                if !notifId.isEmpty {
                    await markNotificationRead(notifId)
                }
                if !convId.isEmpty {
                    await chatStore?.markConversationReadFromNotification(conversationId: convId)
                    cancelConversationNotifications(conversationId: convId)
                }

            case Self.actionCallAccept:
                let callId = userInfo["call_id"] as? String ?? ""
                if !callId.isEmpty {
                    TelefamIosCallPushBridge.shared.handlePushActionAccept(callId: callId)
                    center.removeDeliveredNotifications(withIdentifiers: ["call_\(callId)"])
                }

            case Self.actionCallDecline:
                let callId = userInfo["call_id"] as? String ?? ""
                if !callId.isEmpty {
                    TelefamIosCallPushBridge.shared.handlePushActionDecline(callId: callId)
                    center.removeDeliveredNotifications(withIdentifiers: ["call_\(callId)"])
                }

            case UNNotificationDefaultActionIdentifier:
                if !notifId.isEmpty {
                    await markNotificationRead(notifId)
                }
                let routeHint = userInfo["route"] as? String ?? ""
                if routeHint == "stars" {
                    pendingRoute = .stars
                } else if !convId.isEmpty {
                    pendingRoute = .chat(conversationId: convId, peerUserId: peerId)
                } else if !postId.isEmpty {
                    pendingRoute = .post(postId: postId)
                } else if !targetUid.isEmpty {
                    pendingRoute = .profile(userId: targetUid)
                } else {
                    pendingRoute = .inbox
                }

            default:
                break
            }
            completionHandler()
        }
    }

    // MARK: - Local Cache Persistence (Scoped Per User)

    private func cacheURL(userId: String) -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let folder = base.appendingPathComponent("TelefamNotificationsVault", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder.appendingPathComponent("inbox_\(userId).json")
    }

    private func saveCachedInbox(userId: String) {
        guard !userId.isEmpty,
              let data = try? JSONEncoder().encode(notifications) else { return }
        try? data.write(to: cacheURL(userId: userId), options: [.atomic, .completeFileProtection])
    }

    private func restoreCachedInbox(userId: String) {
        guard !userId.isEmpty,
              let data = try? Data(contentsOf: cacheURL(userId: userId)),
              let decoded = try? JSONDecoder().decode([TelefamIosNotificationItem].self, from: data) else {
            notifications = []
            unreadCount = 0
            return
        }
        notifications = decoded.sorted { $0.createdAtMs > $1.createdAtMs }
        unreadCount = notifications.filter { !$0.isRead }.count
    }
}

// MARK: - Responsive Foreground Notification Banner (with Inline Reply, View-Once Masked)

struct TelefamIosForegroundNotifBannerView: View {
    @Environment(\.colorScheme) private var colorScheme
    let banner: TelefamIosIncomingNotificationBanner
    @State private var replyText: String = ""
    let onOpen: () -> Void
    let onMarkRead: () -> Void
    let onQuickReply: (String) -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: iconName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(TelefamPalette.crimson)
                    .frame(width: 32, height: 32)
                    .background(TelefamPalette.crimson.opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(banner.title)
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .lineLimit(1)
                    Text(banner.body)
                        .font(.system(size: 12.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .lineLimit(2)
                }
                .contentShape(Rectangle())
                .onTapGesture(perform: onOpen)

                Spacer(minLength: 4)

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .frame(width: 28, height: 28)
                }
            }

            HStack(spacing: 8) {
                if !banner.isViewOnce && banner.canReply && !banner.conversationId.isEmpty {
                    TextField("Reply...", text: $replyText)
                        .font(.system(size: 13))
                        .padding(.horizontal, 10)
                        .frame(height: 34)
                        .background(TelefamPalette.searchFieldBackground(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 10))

                    Button {
                        let clean = replyText.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !clean.isEmpty else { return }
                        onQuickReply(clean)
                    } label: {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 34, height: 34)
                            .background(TelefamPalette.crimson)
                            .clipShape(Circle())
                    }
                    .disabled(replyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }

                Spacer()

                Button("Mark read", action: onMarkRead)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(TelefamPalette.crimson)
                    .padding(.horizontal, 10)
                    .frame(height: 30)
                    .background(TelefamPalette.crimson.opacity(0.10))
                    .clipShape(Capsule())

                Button("Open", action: onOpen)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    .padding(.horizontal, 10)
                    .frame(height: 30)
                    .background(TelefamPalette.border(colorScheme).opacity(0.35))
                    .clipShape(Capsule())
            }
        }
        .padding(12)
        .background(TelefamPalette.surface(colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(TelefamPalette.border(colorScheme), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.16), radius: 12, x: 0, y: 4)
        .frame(maxWidth: 480)
        .padding(.horizontal, 12)
    }

    private var iconName: String {
        if banner.isViewOnce { return "waveform" }
        switch banner.kind {
        case "follow": return "person.badge.plus.fill"
        case "like": return "heart.fill"
        case "mention": return "at"
        default: return "bubble.left.fill"
        }
    }
}

// MARK: - Responsive Notification Inbox Sheet

private enum TelefamIosNotifFilter: String, CaseIterable, Identifiable {
    case all = "All"
    case mentions = "Mentions"
    case likes = "Likes"
    case follows = "Follows"

    var id: String { rawValue }
}

struct TelefamIosNotificationsSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var manager: TelefamIosNotificationManager
    let bearerToken: String
    let deviceFingerprint: String
    let onSelectNotification: (TelefamIosNotificationItem) -> Void

    @State private var selectedFilter: TelefamIosNotifFilter = .all

    private var filteredItems: [TelefamIosNotificationItem] {
        switch selectedFilter {
        case .all: return manager.notifications
        case .mentions: return manager.notifications.filter { $0.kind == "mention" }
        case .likes: return manager.notifications.filter { $0.kind == "like" }
        case .follows: return manager.notifications.filter { $0.kind == "follow" }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(TelefamIosNotifFilter.allCases) { filter in
                            let selected = selectedFilter == filter
                            Button {
                                selectedFilter = filter
                            } label: {
                                Text(filter.rawValue)
                                    .font(.system(size: 13, weight: selected ? .bold : .medium))
                                    .foregroundColor(selected ? .white : TelefamPalette.textPrimary(colorScheme))
                                    .padding(.horizontal, 14)
                                    .frame(height: 34)
                                    .background(selected ? TelefamPalette.crimson : TelefamPalette.surface(colorScheme))
                                    .overlay(
                                        Capsule().stroke(selected ? Color.clear : TelefamPalette.border(colorScheme), lineWidth: 1)
                                    )
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }

                Divider().background(TelefamPalette.border(colorScheme))

                if filteredItems.isEmpty {
                    VStack(spacing: 12) {
                        Spacer()
                        Image(systemName: "bell.slash")
                            .font(.system(size: 36, weight: .light))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        Text("No notifications")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Spacer()
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(filteredItems) { item in
                            Button {
                                onSelectNotification(item)
                                dismiss()
                            } label: {
                                HStack(spacing: 12) {
                                    ZStack(alignment: .bottomTrailing) {
                                        TelefamIosRemoteImage(
                                            urlString: item.actorAvatarUrl,
                                            bearerToken: bearerToken,
                                            deviceFingerprint: deviceFingerprint,
                                            fallbackInitials: String(item.actorName.prefix(1).uppercased().ifEmpty("T"))
                                        )
                                        .frame(width: 44, height: 44)
                                        .clipShape(Circle())

                                        Image(systemName: iconForKind(item.kind, isViewOnce: item.isViewOnce))
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(.white)
                                            .frame(width: 20, height: 20)
                                            .background(colorForKind(item.kind))
                                            .clipShape(Circle())
                                            .overlay(Circle().stroke(TelefamPalette.background(colorScheme), lineWidth: 1.5))
                                            .offset(x: 3, y: 3)
                                    }

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(item.title)
                                            .font(.system(size: 14, weight: item.isRead ? .medium : .bold))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            .lineLimit(1)
                                        Text(item.isViewOnce ? "Sent a view-once voice message" : item.body)
                                            .font(.system(size: 13))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                            .lineLimit(2)
                                    }

                                    Spacer(minLength: 6)

                                    VStack(alignment: .trailing, spacing: 6) {
                                        Text(relativeTime(item.createdAtMs))
                                            .font(.system(size: 11))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        if !item.isRead {
                                            Circle()
                                                .fill(TelefamPalette.crimson)
                                                .frame(width: 8, height: 8)
                                        }
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                            .listRowBackground(item.isRead ? Color.clear : TelefamPalette.crimson.opacity(0.05))
                        }
                    }
                    .listStyle(.plain)
                    .refreshable {
                        await manager.refreshNotifications()
                    }
                }
            }
            .background(TelefamPalette.background(colorScheme))
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if manager.unreadCount > 0 {
                        Button("Mark all read") {
                            Task { await manager.markAllNotificationsRead() }
                        }
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(TelefamPalette.crimson)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                    }
                }
            }
        }
        .task {
            await manager.refreshNotifications()
        }
    }

    private func iconForKind(_ kind: String, isViewOnce: Bool) -> String {
        if isViewOnce { return "waveform" }
        switch kind {
        case "follow": return "person.badge.plus.fill"
        case "like": return "heart.fill"
        case "mention": return "at"
        case "star_received": return "star.fill"
        case "verification_update": return "checkmark.seal.fill"
        default: return "bubble.left.fill"
        }
    }

    private func colorForKind(_ kind: String) -> Color {
        switch kind {
        case "like": return TelefamPalette.crimson
        case "follow": return Color.blue
        case "mention": return Color.orange
        case "star_received": return Color(red: 0.96, green: 0.62, blue: 0.04)
        case "verification_update": return Color(red: 0.11, green: 0.61, blue: 0.94)
        default: return Color.teal
        }
    }

    private func relativeTime(_ ms: Int64) -> String {
        guard ms > 0 else { return "" }
        let sec = max(0, Int64(Date().timeIntervalSince1970) - ms / 1000)
        if sec < 60 { return "now" }
        if sec < 3600 { return "\(sec / 60)m" }
        if sec < 86400 { return "\(sec / 3600)h" }
        return "\(sec / 86400)d"
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String {
        isEmpty ? fallback : self
    }
}
