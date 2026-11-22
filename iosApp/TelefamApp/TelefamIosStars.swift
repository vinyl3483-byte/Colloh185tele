import Foundation
import SwiftUI
import UIKit
import WebKit

// MARK: - Notifications & Deep-Link Constants

extension Notification.Name {
    static let telefamStarsUpdatedWs = Notification.Name("TelefamStarsUpdatedWs")
    static let telefamStarsPaymentReturnUrl = Notification.Name("TelefamStarsPaymentReturnUrl")
    static let telefamOpenStarsHubFromPush = Notification.Name("TelefamOpenStarsHubFromPush")
}

// MARK: - Route, Provider, Catalog & Country Currency Conversion

enum TelefamIosStarsRoute: Equatable {
    case hub
    case sendStars
    case buyStars
    case checkout
}

enum TelefamIosStarPaymentProvider: String, CaseIterable, Identifiable {
    case paystack
    case paypal

    var id: String { rawValue }
}

struct TelefamIosCountryCurrencySpec: Equatable {
    let countryIso: String
    let currencyCode: String
    let symbol: String
    let ratePerStar: Double
    let zeroDecimal: Bool
}

struct TelefamIosStarGiftMeta: Equatable {
    let stars: Int64
    let senderUserId: String
    let senderName: String
    let recipientUserId: String
    let recipientName: String
    let animationTier: Int
    let ledgerEntryId: String
}

enum TelefamIosStarsCatalog {
    static let presetPackages: [Int64] = [
        5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 50_000
    ]

    private static let countrySpecs: [String: TelefamIosCountryCurrencySpec] = [
        "KE": TelefamIosCountryCurrencySpec(countryIso: "KE", currencyCode: "KES", symbol: "Ksh ", ratePerStar: 1.0, zeroDecimal: true),
        "NG": TelefamIosCountryCurrencySpec(countryIso: "NG", currencyCode: "NGN", symbol: "\u{20A6}", ratePerStar: 12.0, zeroDecimal: true),
        "GH": TelefamIosCountryCurrencySpec(countryIso: "GH", currencyCode: "GHS", symbol: "GH\u{20B5} ", ratePerStar: 0.12, zeroDecimal: false),
        "ZA": TelefamIosCountryCurrencySpec(countryIso: "ZA", currencyCode: "ZAR", symbol: "R ", ratePerStar: 0.15, zeroDecimal: false),
        "UG": TelefamIosCountryCurrencySpec(countryIso: "UG", currencyCode: "UGX", symbol: "UGX ", ratePerStar: 30.0, zeroDecimal: true),
        "TZ": TelefamIosCountryCurrencySpec(countryIso: "TZ", currencyCode: "TZS", symbol: "TZS ", ratePerStar: 20.0, zeroDecimal: true),
        "RW": TelefamIosCountryCurrencySpec(countryIso: "RW", currencyCode: "RWF", symbol: "RWF ", ratePerStar: 10.0, zeroDecimal: true),
        "GB": TelefamIosCountryCurrencySpec(countryIso: "GB", currencyCode: "GBP", symbol: "\u{00A3}", ratePerStar: 0.0064, zeroDecimal: false),
        "DE": TelefamIosCountryCurrencySpec(countryIso: "DE", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "FR": TelefamIosCountryCurrencySpec(countryIso: "FR", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "ES": TelefamIosCountryCurrencySpec(countryIso: "ES", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "IT": TelefamIosCountryCurrencySpec(countryIso: "IT", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "NL": TelefamIosCountryCurrencySpec(countryIso: "NL", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "IE": TelefamIosCountryCurrencySpec(countryIso: "IE", currencyCode: "EUR", symbol: "\u{20AC}", ratePerStar: 0.0074, zeroDecimal: false),
        "CA": TelefamIosCountryCurrencySpec(countryIso: "CA", currencyCode: "CAD", symbol: "CA$", ratePerStar: 0.011, zeroDecimal: false),
        "AU": TelefamIosCountrySpec(iso: "AU"),
        "IN": TelefamIosCountryCurrencySpec(countryIso: "IN", currencyCode: "INR", symbol: "\u{20B9}", ratePerStar: 0.68, zeroDecimal: false),
        "US": TelefamIosCountryCurrencySpec(countryIso: "US", currencyCode: "USD", symbol: "$", ratePerStar: 0.008, zeroDecimal: false)
    ]

    private static func TelefamIosCountrySpec(iso: String) -> TelefamIosCountryCurrencySpec {
        TelefamIosCountryCurrencySpec(countryIso: iso, currencyCode: "AUD", symbol: "A$", ratePerStar: 0.012, zeroDecimal: false)
    }

    static func resolveCountryIso(profileCountryIso: String?, locationHint: String = "") -> String {
        let clean = (profileCountryIso ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if clean.count == 2, clean.allSatisfy({ $0 >= "A" && $0 <= "Z" }) {
            return clean
        }
        let lower = locationHint.lowercased()
        if lower.contains("kenya") || lower.contains("nairobi") || lower.contains("mombasa") { return "KE" }
        if lower.contains("nigeria") || lower.contains("lagos") || lower.contains("abuja") { return "NG" }
        if lower.contains("ghana") || lower.contains("accra") { return "GH" }
        if lower.contains("south africa") || lower.contains("johannesburg") { return "ZA" }
        if lower.contains("united kingdom") || lower.contains("london") { return "GB" }
        let localeIso = (Locale.current.region?.identifier ?? Locale.current.regionCode ?? "").uppercased()
        if localeIso.count == 2, localeIso.allSatisfy({ $0 >= "A" && $0 <= "Z" }) {
            return localeIso
        }
        return "US"
    }

    static func spec(for countryIso: String) -> TelefamIosCountryCurrencySpec {
        countrySpecs[countryIso.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()]
            ?? countrySpecs["US"]!
    }

    static func formatStars(_ stars: Int64) -> String {
        let fmt = NumberFormatter()
        fmt.numberStyle = .decimal
        fmt.locale = Locale(identifier: "en_US_POSIX")
        return fmt.string(from: NSNumber(value: max(0, stars))) ?? "\(stars)"
    }

    static func formatCurrency(stars: Int64, countryIso: String) -> String {
        let s = spec(for: countryIso)
        let safe = max(0, stars)
        if safe == 0 {
            return s.zeroDecimal ? "\(s.symbol)0" : "\(s.symbol)0.00"
        }
        let amt = Double(safe) * s.ratePerStar
        let rounded = Int64(amt.rounded())
        if s.zeroDecimal || (s.ratePerStar >= 1.0 && abs(amt - Double(rounded)) < 0.001) {
            return "\(s.symbol)\(formatStars(rounded))"
        }
        return String(format: "%@%.2f", locale: Locale(identifier: "en_US_POSIX"), s.symbol, max(0.05, amt))
    }

    static func animationTier(for stars: Int64) -> Int {
        if stars >= 10_000 { return 5 }
        if stars >= 1_000 { return 4 }
        if stars >= 200 { return 3 }
        if stars >= 50 { return 2 }
        return 1
    }

    static func decodeBase64JsonString(_ ciphertextB64: String) -> String? {
        let clean = ciphertextB64.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty, let data = Data(base64Encoded: clean) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func parseStarGiftMeta(rawJson: String) -> TelefamIosStarGiftMeta? {
        let trimmed = rawJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              let data = trimmed.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        let stars = (obj["stars"] as? NSNumber)?.int64Value ?? 0
        guard stars > 0 else { return nil }
        let tier = (obj["animation_tier"] as? NSNumber)?.intValue ?? animationTier(for: stars)
        return TelefamIosStarGiftMeta(
            stars: stars,
            senderUserId: obj["sender_user_id"] as? String ?? "",
            senderName: obj["sender_name"] as? String ?? "",
            recipientUserId: obj["recipient_user_id"] as? String ?? "",
            recipientName: obj["recipient_name"] as? String ?? "",
            animationTier: tier,
            ledgerEntryId: obj["ledger_entry_id"] as? String ?? ""
        )
    }

    static func formatSummaryLine(rawJson: String) -> String {
        if let meta = parseStarGiftMeta(rawJson: rawJson) {
            let unit = meta.stars == 1 ? "Star" : "Stars"
            return "Sent \(formatStars(meta.stars)) \(unit)"
        }
        return "Sent Stars"
    }
}

// MARK: - DTOs

struct TelefamIosStarWallet: Equatable, Codable {
    var userId: String = ""
    var balanceStars: Int64 = 0
    var starsReceivedTotal: Int64 = 0
    var starsSentTotal: Int64 = 0
    var updatedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case balanceStars = "balance_stars"
        case starsReceivedTotal = "stars_received_total"
        case starsSentTotal = "stars_sent_total"
        case updatedAtMs = "updated_at_ms"
    }
}

struct TelefamIosStarSenderSummary: Identifiable, Equatable, Codable {
    var senderUserId: String = ""
    var senderFullName: String = ""
    var senderUsername: String = ""
    var senderAvatarUrl: String = ""
    var totalStarsSent: Int64 = 0
    var giftCount: Int = 0
    var lastSentAtMs: Int64 = 0

    var id: String { senderUserId }

    enum CodingKeys: String, CodingKey {
        case senderUserId = "sender_user_id"
        case senderFullName = "sender_full_name"
        case senderUsername = "sender_username"
        case senderAvatarUrl = "sender_avatar_url"
        case totalStarsSent = "total_stars_sent"
        case giftCount = "gift_count"
        case lastSentAtMs = "last_sent_at_ms"
    }
}

struct TelefamIosStarLedgerItem: Identifiable, Equatable, Codable {
    var entryId: String = ""
    var entryType: String = ""
    var deltaStars: Int64 = 0
    var balanceAfter: Int64 = 0
    var counterpartyUserId: String = ""
    var counterpartyName: String = ""
    var counterpartyUsername: String = ""
    var counterpartyAvatar: String = ""
    var provider: String = ""
    var amountMinor: Int64 = 0
    var currency: String = ""
    var paymentId: String = ""
    var messageId: String = ""
    var conversationId: String = ""
    var createdAtMs: Int64 = 0

    var id: String { entryId.isEmpty ? "\(createdAtMs)_\(deltaStars)" : entryId }

    enum CodingKeys: String, CodingKey {
        case entryId = "entry_id"
        case entryType = "entry_type"
        case deltaStars = "delta_stars"
        case balanceAfter = "balance_after"
        case counterpartyUserId = "counterparty_user_id"
        case counterpartyName = "counterparty_name"
        case counterpartyUsername = "counterparty_username"
        case counterpartyAvatar = "counterparty_avatar"
        case provider
        case amountMinor = "amount_minor"
        case currency
        case paymentId = "payment_id"
        case messageId = "message_id"
        case conversationId = "conversation_id"
        case createdAtMs = "created_at_ms"
    }
}

struct TelefamIosStarPayment: Equatable, Codable {
    var paymentId: String = ""
    var provider: String = ""
    var providerReference: String = ""
    var providerOrderId: String = ""
    var packageStars: Int64 = 0
    var expectedAmountMinor: Int64 = 0
    var expectedCurrency: String = ""
    var paidAmountMinor: Int64 = 0
    var paidCurrency: String = ""
    var status: String = ""
    var checkoutUrl: String = ""
    var purpose: String = "topup"
    var peerUserId: String = ""
    var conversationId: String = ""
    var verifiedVia: String = ""
    var createdAtMs: Int64 = 0
    var completedAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case paymentId = "payment_id"
        case provider
        case providerReference = "provider_reference"
        case providerOrderId = "provider_order_id"
        case packageStars = "package_stars"
        case expectedAmountMinor = "expected_amount_minor"
        case expectedCurrency = "expected_currency"
        case paidAmountMinor = "paid_amount_minor"
        case paidCurrency = "paid_currency"
        case status
        case checkoutUrl = "checkout_url"
        case purpose
        case peerUserId = "peer_user_id"
        case conversationId = "conversation_id"
        case verifiedVia = "verified_via"
        case createdAtMs = "created_at_ms"
        case completedAtMs = "completed_at_ms"
    }
}

struct TelefamIosSendStarsPeerContext: Equatable {
    let userId: String
    let fullName: String
    let username: String
    let avatarUrl: String
    let conversationId: String
    let isOnline: Bool
}

struct TelefamIosStarCelebrationBurst: Equatable, Identifiable {
    let id = UUID()
    let stars: Int64
    let isReceived: Bool
    let counterpartyName: String
}

// MARK: - Stars State & Payment / Gifting Store

@MainActor
final class TelefamIosStarsStore: ObservableObject {
    @Published var route: TelefamIosStarsRoute? = nil
    @Published var isLoadingSummary: Bool = false
    @Published var isSubmitting: Bool = false
    @Published var wallet: TelefamIosStarWallet = TelefamIosStarWallet()
    @Published var countryIso: String = "US"
    @Published var senders: [TelefamIosStarSenderSummary] = []
    @Published var history: [TelefamIosStarLedgerItem] = []
    @Published var sendPeer: TelefamIosSendStarsPeerContext? = nil
    @Published var selectedBuyStars: Int64 = 100
    @Published var selectedProvider: TelefamIosStarPaymentProvider = .paystack
    @Published var pendingSendAfterBuyStars: Int64? = nil
    @Published var activePayment: TelefamIosStarPayment? = nil
    @Published var checkoutWebUrl: String? = nil
    @Published var errorMessage: String? = nil
    @Published var toastMessage: String? = nil
    @Published var celebrationBurst: TelefamIosStarCelebrationBurst? = nil

    private let baseURL: URL
    private let userIdProvider: () -> String
    private let tokenProvider: () -> String
    private let deviceFingerprint: String
    private let countryHintProvider: () -> String
    private var celebrationTask: Task<Void, Never>? = nil
    private var toastTask: Task<Void, Never>? = nil
    private var observers: [NSObjectProtocol] = []

    init(
        baseURL: URL,
        userIdProvider: @escaping () -> String,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String,
        countryHintProvider: @escaping () -> String
    ) {
        self.baseURL = baseURL
        self.userIdProvider = userIdProvider
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint
        self.countryHintProvider = countryHintProvider
        self.countryIso = TelefamIosStarsCatalog.resolveCountryIso(profileCountryIso: countryHintProvider())

        let nc = NotificationCenter.default
        let wsObs = nc.addObserver(forName: .telefamStarsUpdatedWs, object: nil, queue: .main) { [weak self] note in
            let dict = note.userInfo as? [String: Any] ?? [:]
            Task { @MainActor in
                self?.handleWsStarsUpdated(dict)
            }
        }
        let retObs = nc.addObserver(forName: .telefamStarsPaymentReturnUrl, object: nil, queue: .main) { [weak self] note in
            let urlStr = (note.userInfo?["url"] as? String) ?? ""
            Task { @MainActor in
                self?.handlePaymentReturnUrl(urlStr)
            }
        }
        observers = [wsObs, retObs]
    }

    deinit {
        for obs in observers {
            NotificationCenter.default.removeObserver(obs)
        }
    }

    var authBearerToken: String { tokenProvider() }
    var authDeviceFingerprint: String { deviceFingerprint }

    func resolvedMediaURL(_ raw: String) -> String {
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.isEmpty { return "" }
        if t.hasPrefix("http://") || t.hasPrefix("https://") { return t }
        return URL(string: t, relativeTo: baseURL)?.absoluteString ?? t
    }

    func openStarsHub() {
        countryIso = TelefamIosStarsCatalog.resolveCountryIso(profileCountryIso: countryHintProvider())
        errorMessage = nil
        route = .hub
        Task { await refreshSummary() }
    }

    func openSendStarsFromChat(peer: TelefamIosSendStarsPeerContext) {
        countryIso = TelefamIosStarsCatalog.resolveCountryIso(profileCountryIso: countryHintProvider())
        errorMessage = nil
        sendPeer = peer
        pendingSendAfterBuyStars = nil
        Task {
            await refreshSummary()
            if wallet.balanceStars <= 0 {
                selectedBuyStars = 100
                route = .buyStars
                showToast("No available Stars balance. Choose a package to buy.")
            } else {
                route = .sendStars
            }
        }
    }

    func openBuyStars(preselectedPackage: Int64 = 5) {
        errorMessage = nil
        selectedBuyStars = preselectedPackage
        route = .buyStars
    }

    func selectPackageAndProceedToCheckout(stars: Int64) {
        errorMessage = nil
        selectedBuyStars = stars
        route = .checkout
    }

    func navigateBack() {
        errorMessage = nil
        checkoutWebUrl = nil
        switch route {
        case .checkout:
            route = .buyStars
        case .buyStars:
            if sendPeer != nil && wallet.balanceStars > 0 {
                pendingSendAfterBuyStars = nil
                route = .sendStars
            } else if sendPeer != nil {
                sendPeer = nil
                pendingSendAfterBuyStars = nil
                route = nil
            } else {
                route = .hub
            }
        case .sendStars, .hub, .none:
            route = nil
            sendPeer = nil
            pendingSendAfterBuyStars = nil
        }
    }

    func dismissFlow() {
        route = nil
        sendPeer = nil
        pendingSendAfterBuyStars = nil
        checkoutWebUrl = nil
        errorMessage = nil
    }

    func clearOnLogout() {
        route = nil
        isLoadingSummary = false
        isSubmitting = false
        wallet = TelefamIosStarWallet()
        senders = []
        history = []
        sendPeer = nil
        pendingSendAfterBuyStars = nil
        activePayment = nil
        checkoutWebUrl = nil
        errorMessage = nil
        celebrationBurst = nil
    }

    func refreshSummary() async {
        let tok = tokenProvider()
        guard !tok.isEmpty else { return }
        isLoadingSummary = true
        defer { isLoadingSummary = false }

        let resolvedCountry = TelefamIosStarsCatalog.resolveCountryIso(profileCountryIso: countryHintProvider())
        let cEnc = resolvedCountry.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "US"
        guard var comps = URLComponents(url: baseURL.appendingPathComponent("v1/stars/summary"), resolvingAgainstBaseURL: false) else {
            return
        }
        comps.queryItems = [URLQueryItem(name: "country_iso", value: cEnc)]
        guard let url = comps.url else { return }

        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = "GET"
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        struct SummaryResp: Codable {
            let success: Bool
            let wallet: TelefamIosStarWallet?
            let countryIso: String?
            let senders: [TelefamIosStarSenderSummary]?
            let history: [TelefamIosStarLedgerItem]?
            enum CodingKeys: String, CodingKey {
                case success, wallet, senders, history
                case countryIso = "country_iso"
            }
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Unable to connect to Stars server."
            return
        }
        guard (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(SummaryResp.self, from: data),
              parsed.success else {
            errorMessage = extractServerError(data: data) ?? "Failed to load Stars balance."
            return
        }
        wallet = parsed.wallet ?? wallet
        countryIso = resolvedCountry
        senders = parsed.senders ?? []
        history = parsed.history ?? []
    }

    func selectTierToSend(stars: Int64) async {
        guard let peer = sendPeer else { return }
        if wallet.balanceStars <= 0 || stars > wallet.balanceStars {
            pendingSendAfterBuyStars = stars
            selectedBuyStars = stars
            route = .buyStars
            showToast(
                wallet.balanceStars <= 0
                    ? "Buy Stars to send this gift."
                    : "Need \(TelefamIosStarsCatalog.formatStars(stars)) Stars. Redirected to Buy Stars."
            )
            return
        }
        await performSendStars(peer: peer, stars: stars)
    }

    private func performSendStars(peer: TelefamIosSendStarsPeerContext, stars: Int64) async {
        let tok = tokenProvider()
        guard !tok.isEmpty else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let idemKey = "ios_send_\(UUID().uuidString)"
        let url = baseURL.appendingPathComponent("v1/stars/send")
        let payload: [String: Any] = [
            "recipient_user_id": peer.userId,
            "conversation_id": peer.conversationId,
            "stars": stars,
            "idempotency_key": idemKey
        ]
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue(idemKey, forHTTPHeaderField: "Idempotency-Key")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        struct SendResp: Codable {
            let success: Bool
            let starsSent: Int64?
            let senderWallet: TelefamIosStarWallet?
            let insufficientBalance: Bool?
            enum CodingKeys: String, CodingKey {
                case success
                case starsSent = "stars_sent"
                case senderWallet = "sender_wallet"
                case insufficientBalance = "insufficient_balance"
            }
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error while sending Stars."
            return
        }

        if http.statusCode == 402 {
            pendingSendAfterBuyStars = stars
            selectedBuyStars = stars
            route = .buyStars
            showToast("Insufficient Stars balance. Choose a package to buy.")
            return
        }

        guard (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(SendResp.self, from: data),
              parsed.success else {
            errorMessage = extractServerError(data: data) ?? "Unable to send Stars right now."
            return
        }

        if let w = parsed.senderWallet {
            wallet = w
        }
        pendingSendAfterBuyStars = nil
        route = nil
        let targetName = peer.fullName.isEmpty ? peer.username : peer.fullName
        triggerCelebration(stars: stars, isReceived: false, counterpartyName: targetName)
        await refreshSummary()
    }

    func proceedToPay() async {
        let tok = tokenProvider()
        guard !tok.isEmpty else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let idemKey = "ios_chk_\(UUID().uuidString)"
        let url = baseURL.appendingPathComponent("v1/stars/checkout/init")
        let resolvedCountry = TelefamIosStarsCatalog.resolveCountryIso(profileCountryIso: countryHintProvider())
        let payload: [String: Any] = [
            "provider": selectedProvider.rawValue,
            "package_stars": selectedBuyStars,
            "country_iso": resolvedCountry,
            "idempotency_key": idemKey,
            "purpose": sendPeer != nil ? "chat_gift" : "topup",
            "peer_user_id": sendPeer?.userId ?? "",
            "conversation_id": sendPeer?.conversationId ?? ""
        ]
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue(idemKey, forHTTPHeaderField: "Idempotency-Key")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        struct InitResp: Codable {
            let success: Bool
            let payment: TelefamIosStarPayment?
            let wallet: TelefamIosStarWallet?
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error while initiating checkout."
            return
        }
        guard (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(InitResp.self, from: data),
              parsed.success,
              let pay = parsed.payment,
              !pay.checkoutUrl.isEmpty else {
            errorMessage = extractServerError(data: data) ?? "Could not start payment checkout."
            return
        }

        if let w = parsed.wallet { wallet = w }
        activePayment = pay
        checkoutWebUrl = pay.checkoutUrl
    }

    func handlePaymentReturnUrl(_ urlStr: String) {
        let lower = urlStr.lowercased()
        guard lower.contains("stars") || lower.contains("payment-return") || lower.contains("trxref=") || lower.contains("token=") else {
            return
        }
        checkoutWebUrl = nil
        if let url = URL(string: urlStr),
           let comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            let ref = comps.queryItems?.first(where: { $0.name == "reference" || $0.name == "trxref" })?.value ?? ""
            let orderId = comps.queryItems?.first(where: { $0.name == "token" || $0.name == "order_id" })?.value ?? ""
            Task {
                await verifyActivePayment(overrideReference: ref, overrideOrderId: orderId)
            }
        } else {
            Task { await verifyActivePayment() }
        }
    }

    func verifyActivePayment(overrideReference: String = "", overrideOrderId: String = "") async {
        guard let payment = activePayment else { return }
        let tok = tokenProvider()
        guard !tok.isEmpty else { return }

        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let idemKey = "ios_vrf_\(payment.paymentId)_\(UUID().uuidString.prefix(8))"
        let url = baseURL.appendingPathComponent("v1/stars/checkout/verify")
        let payload: [String: Any] = [
            "payment_id": payment.paymentId,
            "reference": overrideReference.isEmpty ? payment.providerReference : overrideReference,
            "paypal_order_id": overrideOrderId.isEmpty ? payment.providerOrderId : overrideOrderId,
            "idempotency_key": idemKey
        ]
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue(idemKey, forHTTPHeaderField: "Idempotency-Key")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        struct VerifyResp: Codable {
            let success: Bool
            let credited: Bool?
            let payment: TelefamIosStarPayment?
            let wallet: TelefamIosStarWallet?
        }

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error while verifying payment."
            return
        }
        guard (200...299).contains(http.statusCode),
              let parsed = try? JSONDecoder().decode(VerifyResp.self, from: data),
              parsed.success,
              let verifiedPay = parsed.payment else {
            errorMessage = extractServerError(data: data) ?? "Payment verification did not complete yet."
            return
        }

        activePayment = verifiedPay
        if let w = parsed.wallet { wallet = w }
        checkoutWebUrl = nil

        if verifiedPay.status.lowercased() == "succeeded" || parsed.credited == true {
            await refreshSummary()
            if let peer = sendPeer {
                if let pending = pendingSendAfterBuyStars, wallet.balanceStars >= pending {
                    pendingSendAfterBuyStars = nil
                    await performSendStars(peer: peer, stars: pending)
                } else {
                    route = .sendStars
                    showToast("\(TelefamIosStarsCatalog.formatStars(verifiedPay.packageStars)) Stars credited")
                }
            } else {
                route = .hub
                triggerCelebration(stars: verifiedPay.packageStars, isReceived: true, counterpartyName: "Telefam Stars")
            }
        } else {
            errorMessage = "Payment status: \(verifiedPay.status)"
        }
    }

    func handleWsStarsUpdated(_ obj: [String: Any]) {
        if let wDict = obj["wallet"] as? [String: Any],
           let wData = try? JSONSerialization.data(withJSONObject: wDict),
           let w = try? JSONDecoder().decode(TelefamIosStarWallet.self, from: wData) {
            wallet = w
        }
        let kind = (obj["kind"] as? String ?? "").lowercased()
        let delta = (obj["delta_stars"] as? NSNumber)?.int64Value ?? 0
        let cpName = obj["counterparty_name"] as? String ?? ""
        if kind == "received" && delta > 0 {
            triggerCelebration(stars: delta, isReceived: true, counterpartyName: cpName)
        }
        Task { await refreshSummary() }
    }

    func triggerCelebration(stars: Int64, isReceived: Bool, counterpartyName: String) {
        celebrationTask?.cancel()
        celebrationBurst = TelefamIosStarCelebrationBurst(stars: stars, isReceived: isReceived, counterpartyName: counterpartyName)
        celebrationTask = Task {
            try? await Task.sleep(nanoseconds: 3_600_000_000)
            if !Task.isCancelled {
                celebrationBurst = nil
            }
        }
    }

    func dismissCelebration() {
        celebrationTask?.cancel()
        celebrationBurst = nil
    }

    func showToast(_ msg: String) {
        toastTask?.cancel()
        toastMessage = msg
        toastTask = Task {
            try? await Task.sleep(nanoseconds: 2_600_000_000)
            if !Task.isCancelled && toastMessage == msg {
                toastMessage = nil
            }
        }
    }

    private func extractServerError(data: Data) -> String? {
        guard let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        for k in ["error", "message", "detail"] {
            if let v = obj[k] as? String, !v.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return v
            }
        }
        return nil
    }
}

// MARK: - Vector SVG Icons & 3D Animated Spinning Sparkle Star Canvas

struct TelefamIosGoldStarBadge: View {
    var size: CGFloat = 24
    var showSoftBg: Bool = true

    var body: some View {
        ZStack {
            if showSoftBg {
                Circle()
                    .fill(Color(hex: 0xFEF3C7))
                    .frame(width: size * 1.42, height: size * 1.42)
            }
            Canvas { ctx, cSize in
                let r = min(cSize.width, cSize.height) / 2.0
                let innerR = r * 0.44
                let cx = cSize.width / 2.0
                let cy = cSize.height / 2.0
                var star = Path()
                for i in 0..<10 {
                    let angle = -Double.pi / 2.0 + Double(i) * Double.pi / 5.0
                    let dist = (i % 2 == 0) ? r : innerR
                    let pt = CGPoint(x: cx + CGFloat(cos(angle)) * dist, y: cy + CGFloat(sin(angle)) * dist)
                    if i == 0 { star.move(to: pt) } else { star.addLine(to: pt) }
                }
                star.closeSubpath()
                ctx.fill(
                    star,
                    with: .linearGradient(
                        Gradient(colors: [Color(hex: 0xFDE047), Color(hex: 0xF59E0B)]),
                        startPoint: CGPoint(x: 0, y: 0),
                        endPoint: CGPoint(x: cSize.width, y: cSize.height)
                    )
                )
            }
            .frame(width: size, height: size)
        }
    }
}

struct TelefamIosAnimated3DStarView: View {
    let starsAmount: Int64
    var baseSize: CGFloat = 76
    var showTelefamWording: Bool = true

    private var tier: Int {
        TelefamIosStarsCatalog.animationTier(for: max(5, starsAmount))
    }

    private var scaledSize: CGFloat {
        baseSize + CGFloat(tier - 1) * (baseSize * 0.14)
    }

    var body: some View {
        VStack(spacing: 4) {
            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { tl in
                let sec = tl.date.timeIntervalSinceReferenceDate
                let speed = 1.15 + Double(tier) * 0.35
                let spinAngle = (sec * speed).truncatingRemainder(dividingBy: 2.0 * Double.pi)
                let scaleX = max(0.16, abs(CGFloat(cos(spinAngle))))
                let tiltY = CGFloat(sin(spinAngle)) * 4.0

                Canvas { ctx, cSize in
                    let w = cSize.width
                    let h = cSize.height
                    let cx = w / 2.0
                    let cy = h / 2.0

                    // Radiant halo glow
                    ctx.fill(
                        Path(ellipseIn: CGRect(x: cx - w * 0.42, y: cy - h * 0.42, width: w * 0.84, height: h * 0.84)),
                        with: .radialGradient(
                            Gradient(colors: [
                                Color(hex: 0xFBBF24, opacity: 0.25 + Double(tier) * 0.05),
                                Color(hex: 0xF59E0B, opacity: 0.06),
                                .clear
                            ]),
                            center: CGPoint(x: cx, y: cy),
                            startRadius: 2,
                            endRadius: w * 0.48
                        )
                    )

                    if tier >= 2 {
                        let ringRect = CGRect(x: cx - w * 0.38, y: cy - h * 0.38, width: w * 0.76, height: h * 0.76)
                        ctx.stroke(
                            Path(ellipseIn: ringRect),
                            with: .color(Color(hex: 0xFBBF24, opacity: 0.24)),
                            style: StrokeStyle(lineWidth: 1.2 + CGFloat(tier) * 0.35, dash: [5, 5])
                        )
                    }

                    // Orbiting diamond sparkles scaled by tier
                    let sparkleCount = 4 + tier * 2
                    for i in 0..<sparkleCount {
                        let orbit = spinAngle * (i % 2 == 0 ? 1.0 : -1.0) + Double(i) * (2.0 * Double.pi / Double(sparkleCount))
                        let rad = w * (0.31 + (i % 2 == 0 ? 0.13 : 0.06))
                        let sx = cx + CGFloat(cos(orbit)) * rad
                        let sy = cy + CGFloat(sin(orbit)) * rad
                        let spSize = w * 0.045 * (0.65 + 0.55 * CGFloat(abs(sin(sec * 2.4 + Double(i)))))
                        var sp = Path()
                        sp.move(to: CGPoint(x: sx, y: sy - spSize))
                        sp.addLine(to: CGPoint(x: sx + spSize * 0.28, y: sy - spSize * 0.28))
                        sp.addLine(to: CGPoint(x: sx + spSize, y: sy))
                        sp.addLine(to: CGPoint(x: sx + spSize * 0.28, y: sy + spSize * 0.28))
                        sp.addLine(to: CGPoint(x: sx, y: sy + spSize))
                        sp.addLine(to: CGPoint(x: sx - spSize * 0.28, y: sy + spSize * 0.28))
                        sp.addLine(to: CGPoint(x: sx - spSize, y: sy))
                        sp.addLine(to: CGPoint(x: sx - spSize * 0.28, y: sy - spSize * 0.28))
                        sp.closeSubpath()
                        ctx.fill(sp, with: .color(i % 2 == 0 ? Color(hex: 0xFEF08A) : Color(hex: 0xF59E0B)))
                    }

                    // 3D faceted extruded star edge for depth
                    let outerR = w * 0.30
                    let innerR = w * 0.135
                    let depthOffset = CGFloat(sin(spinAngle)) * w * 0.075

                    var edgePath = Path()
                    var frontPath = Path()
                    for i in 0..<10 {
                        let a = -Double.pi / 2.0 + Double(i) * Double.pi / 5.0
                        let r = (i % 2 == 0) ? outerR : innerR
                        let xFront = cx + CGFloat(cos(a)) * r * scaleX
                        let yFront = cy + CGFloat(sin(a)) * r + tiltY * 0.25
                        let xEdge = xFront + depthOffset
                        if i == 0 {
                            frontPath.move(to: CGPoint(x: xFront, y: yFront))
                            edgePath.move(to: CGPoint(x: xEdge, y: yFront + 1.5))
                        } else {
                            frontPath.addLine(to: CGPoint(x: xFront, y: yFront))
                            edgePath.addLine(to: CGPoint(x: xEdge, y: yFront + 1.5))
                        }
                    }
                    frontPath.closeSubpath()
                    edgePath.closeSubpath()

                    ctx.fill(edgePath, with: .color(Color(hex: 0x92400E)))
                    ctx.fill(
                        frontPath,
                        with: .linearGradient(
                            Gradient(colors: [Color(hex: 0xFEF9C3), Color(hex: 0xFACC15), Color(hex: 0xD97706)]),
                            startPoint: CGPoint(x: cx - outerR, y: cy - outerR),
                            endPoint: CGPoint(x: cx + outerR, y: cy + outerR)
                        )
                    )
                    ctx.stroke(frontPath, with: .color(Color(hex: 0xFEF08A)), style: StrokeStyle(lineWidth: 1.4))
                }
                .frame(width: scaledSize, height: scaledSize)
            }

            if showTelefamWording {
                Text("Telefam")
                    .font(.system(size: max(12.5, scaledSize * 0.18), weight: .heavy))
                    .foregroundColor(TelefamPalette.crimson)
                    .tracking(0.4)
            }
        }
    }
}

private struct TelefamIosPaystackLogo: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let c = Color(hex: 0x0AA5DB)
            let bars: [(y: CGFloat, wf: CGFloat)] = [
                (0.16, 0.80), (0.36, 0.80), (0.56, 0.80), (0.76, 0.44)
            ]
            for bar in bars {
                let rect = CGRect(x: w * 0.10, y: h * bar.y, width: w * bar.wf, height: h * 0.13)
                ctx.fill(Path(roundedRect: rect, cornerRadius: h * 0.065), with: .color(c))
            }
        }
    }
}

private struct TelefamIosPayPalLogo: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            var backP = Path()
            backP.move(to: CGPoint(x: w * 0.24, y: h * 0.16))
            backP.addLine(to: CGPoint(x: w * 0.62, y: h * 0.16))
            backP.addQuadCurve(to: CGPoint(x: w * 0.78, y: h * 0.38), control: CGPoint(x: w * 0.80, y: h * 0.16))
            backP.addQuadCurve(to: CGPoint(x: w * 0.56, y: h * 0.58), control: CGPoint(x: w * 0.76, y: h * 0.58))
            backP.addLine(to: CGPoint(x: w * 0.36, y: h * 0.58))
            backP.addLine(to: CGPoint(x: w * 0.30, y: h * 0.86))
            backP.addLine(to: CGPoint(x: w * 0.16, y: h * 0.86))
            backP.closeSubpath()
            ctx.fill(backP, with: .color(Color(hex: 0x003087)))

            var frontP = Path()
            frontP.move(to: CGPoint(x: w * 0.38, y: h * 0.32))
            frontP.addLine(to: CGPoint(x: w * 0.70, y: h * 0.32))
            frontP.addQuadCurve(to: CGPoint(x: w * 0.84, y: h * 0.52), control: CGPoint(x: w * 0.86, y: h * 0.32))
            frontP.addQuadCurve(to: CGPoint(x: w * 0.64, y: h * 0.70), control: CGPoint(x: w * 0.82, y: h * 0.70))
            frontP.addLine(to: CGPoint(x: w * 0.46, y: h * 0.70))
            frontP.addLine(to: CGPoint(x: w * 0.41, y: h * 0.90))
            frontP.addLine(to: CGPoint(x: w * 0.28, y: h * 0.90))
            frontP.closeSubpath()
            ctx.fill(frontP, with: .color(Color(hex: 0x0079C1)))
        }
    }
}

private struct TelefamIosVectorIcon: View {
    enum Kind { case backArrow, shieldCheck, chevronRight, infoDot, close }
    let kind: Kind
    var color: Color
    var size: CGFloat = 18

    var body: some View {
        Canvas { ctx, s in
            let w = s.width
            let h = s.height
            let stroke = StrokeStyle(lineWidth: max(1.6, w * 0.095), lineCap: .round, lineJoin: .round)
            switch kind {
            case .backArrow:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.24, y: h * 0.50))
                p.move(to: CGPoint(x: w * 0.48, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.48, y: h * 0.76))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .shieldCheck:
                var shield = Path()
                shield.move(to: CGPoint(x: w * 0.50, y: h * 0.12))
                shield.addLine(to: CGPoint(x: w * 0.84, y: h * 0.24))
                shield.addQuadCurve(to: CGPoint(x: w * 0.50, y: h * 0.88), control: CGPoint(x: w * 0.82, y: h * 0.68))
                shield.addQuadCurve(to: CGPoint(x: w * 0.16, y: h * 0.24), control: CGPoint(x: w * 0.18, y: h * 0.68))
                shield.closeSubpath()
                ctx.fill(shield, with: .color(color))
                var chk = Path()
                chk.move(to: CGPoint(x: w * 0.34, y: h * 0.50))
                chk.addLine(to: CGPoint(x: w * 0.46, y: h * 0.62))
                chk.addLine(to: CGPoint(x: w * 0.68, y: h * 0.38))
                ctx.stroke(chk, with: .color(.white), style: StrokeStyle(lineWidth: max(1.6, w * 0.10), lineCap: .round, lineJoin: .round))
            case .chevronRight:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.36, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.66, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.36, y: h * 0.78))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .infoDot:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.12, y: h * 0.12, width: w * 0.76, height: h * 0.76)), with: .color(color), style: stroke)
                ctx.fill(Path(ellipseIn: CGRect(x: w * 0.45, y: h * 0.28, width: w * 0.10, height: h * 0.10)), with: .color(color))
                var bar = Path()
                bar.move(to: CGPoint(x: w * 0.50, y: h * 0.46))
                bar.addLine(to: CGPoint(x: w * 0.50, y: h * 0.72))
                ctx.stroke(bar, with: .color(color), style: stroke)
            case .close:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.78))
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.78))
                ctx.stroke(p, with: .color(color), style: stroke)
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - In-Chat Animated Star Gift Bubble

struct TelefamIosStarGiftChatBubbleContent: View {
    let rawMetaJson: String
    let isOwn: Bool
    let textColor: Color
    let subColor: Color

    var body: some View {
        let meta = TelefamIosStarsCatalog.parseStarGiftMeta(rawJson: rawMetaJson)
        let stars = meta?.stars ?? 5
        let label = isOwn
            ? "You sent \(TelefamIosStarsCatalog.formatStars(stars)) Stars"
            : "Received \(TelefamIosStarsCatalog.formatStars(stars)) Stars"
        VStack(spacing: 6) {
            TelefamIosAnimated3DStarView(starsAmount: stars, baseSize: 64, showTelefamWording: true)
            Text(label)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(textColor)
            Text("Telefam Stars Gift")
                .font(.system(size: 11.5, weight: .medium))
                .foregroundColor(subColor)
        }
        .padding(.vertical, 4)
        .frame(minWidth: 168)
    }
}

// MARK: - Fullscreen Star Celebration Overlay

struct TelefamIosStarCelebrationOverlay: View {
    let burst: TelefamIosStarCelebrationBurst
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.62)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 12) {
                TelefamIosAnimated3DStarView(starsAmount: burst.stars, baseSize: 108, showTelefamWording: true)
                Text("\(burst.isReceived ? "Received" : "Sent") \(TelefamIosStarsCatalog.formatStars(burst.stars)) Stars")
                    .font(.system(size: 21, weight: .heavy))
                    .foregroundColor(.white)
                if !burst.counterpartyName.isEmpty {
                    Text(burst.isReceived ? "From \(burst.counterpartyName)" : "To \(burst.counterpartyName)")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.white.opacity(0.84))
                }
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 24)
            .background(Color(hex: 0x161922).opacity(0.94))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(Color(hex: 0xFBBF24).opacity(0.45), lineWidth: 1.5))
            .shadow(radius: 20)
            .padding(.horizontal, 24)
        }
        .transition(.opacity)
    }
}

// MARK: - Stars Host View (Hub / Send Stars / Buy Stars / Checkout / WebView)

struct TelefamIosStarsHostView: View {
    @ObservedObject var store: TelefamIosStarsStore
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var sizeClass

    @State private var hubTabIndex: Int = 0 // 0 = Senders, 1 = History

    private var accentCrimson: Color { TelefamPalette.crimson }

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch store.route {
                case .hub, .none:
                    starsHubScreen
                case .sendStars:
                    sendStarsScreen
                case .buyStars:
                    buyStarsScreen
                case .checkout:
                    checkoutScreen
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if let toast = store.toastMessage {
                Text(toast)
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.86))
                    .clipShape(Capsule())
                    .padding(.bottom, 28)
                    .transition(.opacity)
            }
        }
        .sheet(isPresented: Binding(
            get: { store.checkoutWebUrl != nil },
            set: { if !$0 { store.checkoutWebUrl = nil } }
        )) {
            if let urlStr = store.checkoutWebUrl {
                NavigationStack {
                    TelefamIosCheckoutWebView(
                        initialUrlString: urlStr,
                        onReturnUrlDetected: { retUrl in
                            store.handlePaymentReturnUrl(retUrl)
                        }
                    )
                    .navigationTitle(store.selectedProvider == .paystack ? "Paystack Checkout" : "PayPal Checkout")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Done") {
                                store.checkoutWebUrl = nil
                                Task { await store.verifyActivePayment() }
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: 1. Stars Hub Screen (Screenshot 1 - Skip Payouts)

    private var starsHubScreen: some View {
        let isDark = colorScheme == .dark
        let bg = isDark ? Color(hex: 0x0B0E14) : Color(hex: 0xF5F6FA)
        let card = isDark ? Color(hex: 0x161A22) : .white
        let innerCard = isDark ? Color(hex: 0x1E232E) : Color(hex: 0xF2F4F8)
        let stroke = isDark ? Color(hex: 0x262C39) : Color(hex: 0xE5E8F0)

        return VStack(spacing: 0) {
            HStack {
                Button { store.navigateBack() } label: {
                    TelefamIosVectorIcon(kind: .backArrow, color: TelefamPalette.textPrimary(colorScheme), size: 20)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Back")
                Text("Stars")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
                Button {
                    Task { await store.refreshSummary() }
                } label: {
                    Text(store.isLoadingSummary ? "Loading..." : "Refresh")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .padding(.horizontal, 16)
                        .frame(minHeight: 38)
                        .background(card)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(stroke, lineWidth: 1))
                }
                .disabled(store.isLoadingSummary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(bg)

            ScrollView {
                VStack(spacing: 16) {
                    // Balance Card
                    VStack(spacing: 16) {
                        HStack(alignment: .center, spacing: 14) {
                            TelefamIosAnimated3DStarView(starsAmount: max(5, store.wallet.balanceStars), baseSize: 58, showTelefamWording: true)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Available Balance")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                Text("\(TelefamIosStarsCatalog.formatStars(store.wallet.balanceStars)) Stars")
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text(TelefamIosStarsCatalog.formatCurrency(stars: store.wallet.balanceStars, countryIso: store.countryIso))
                                    .font(.system(size: 13.5, weight: .medium))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            Button {
                                store.openBuyStars(preselectedPackage: 5)
                            } label: {
                                Text("Buy Stars")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 18)
                                    .frame(minHeight: 44)
                                    .background(accentCrimson)
                                    .clipShape(Capsule())
                            }
                        }

                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Stars Received")
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                HStack(spacing: 7) {
                                    TelefamIosGoldStarBadge(size: 18, showSoftBg: false)
                                    Text(TelefamIosStarsCatalog.formatStars(store.wallet.starsReceivedTotal))
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(Color(hex: 0x22C55E))
                                }
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(innerCard)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(stroke, lineWidth: 1))

                            VStack(alignment: .leading, spacing: 6) {
                                Text("Stars Sent")
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                HStack(spacing: 7) {
                                    TelefamIosGoldStarBadge(size: 18, showSoftBg: false)
                                    Text(TelefamIosStarsCatalog.formatStars(store.wallet.starsSentTotal))
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(accentCrimson)
                                }
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(innerCard)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(stroke, lineWidth: 1))
                        }
                    }
                    .padding(18)
                    .background(card)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(stroke, lineWidth: 1))

                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(accentCrimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(TelefamPalette.blush(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }

                    // Segmented Switcher: Senders | History
                    HStack(spacing: 4) {
                        Button { hubTabIndex = 0 } label: {
                            Text("Senders (\(store.senders.count))")
                                .font(.system(size: 14, weight: hubTabIndex == 0 ? .bold : .medium))
                                .foregroundColor(hubTabIndex == 0 ? .white : TelefamPalette.textMuted(colorScheme))
                                .frame(maxWidth: .infinity, minHeight: 42)
                                .background(hubTabIndex == 0 ? accentCrimson : Color.clear)
                                .clipShape(Capsule())
                        }
                        Button { hubTabIndex = 1 } label: {
                            Text("History (\(store.history.count))")
                                .font(.system(size: 14, weight: hubTabIndex == 1 ? .bold : .medium))
                                .foregroundColor(hubTabIndex == 1 ? .white : TelefamPalette.textMuted(colorScheme))
                                .frame(maxWidth: .infinity, minHeight: 42)
                                .background(hubTabIndex == 1 ? accentCrimson : Color.clear)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(4)
                    .background(card)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(stroke, lineWidth: 1))

                    if hubTabIndex == 0 {
                        if store.senders.isEmpty {
                            VStack(spacing: 12) {
                                TelefamIosAnimated3DStarView(starsAmount: 50, baseSize: 64, showTelefamWording: false)
                                Text("No star senders yet")
                                    .font(.system(size: 16.5, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text("Supporters who send Stars to you will appear here in real time.")
                                    .font(.system(size: 13))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(28)
                            .background(card)
                            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(stroke, lineWidth: 1))
                        } else {
                            VStack(spacing: 10) {
                                ForEach(store.senders) { s in
                                    HStack(spacing: 12) {
                                        avatarBadge(url: s.senderAvatarUrl, name: s.senderFullName.isEmpty ? s.senderUsername : s.senderFullName, size: 42)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(s.senderFullName.isEmpty ? "@\(s.senderUsername)" : s.senderFullName)
                                                .font(.system(size: 15, weight: .bold))
                                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            Text("@\(s.senderUsername)  ·  \(s.giftCount) gift(s)")
                                                .font(.system(size: 12))
                                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        }
                                        Spacer()
                                        HStack(spacing: 5) {
                                            TelefamIosGoldStarBadge(size: 16, showSoftBg: false)
                                            Text("+\(TelefamIosStarsCatalog.formatStars(s.totalStarsSent))")
                                                .font(.system(size: 14, weight: .bold))
                                                .foregroundColor(Color(hex: 0x22C55E))
                                        }
                                    }
                                    .padding(14)
                                    .background(card)
                                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(stroke, lineWidth: 1))
                                }
                            }
                        }
                    } else {
                        if store.history.isEmpty {
                            VStack(spacing: 12) {
                                TelefamIosAnimated3DStarView(starsAmount: 20, baseSize: 60, showTelefamWording: false)
                                Text("No Stars transactions yet")
                                    .font(.system(size: 16.5, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text("Purchases, gifts sent, and gifts received appear here automatically.")
                                    .font(.system(size: 13))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(28)
                            .background(card)
                            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(stroke, lineWidth: 1))
                        } else {
                            VStack(spacing: 10) {
                                ForEach(store.history) { item in
                                    let isCredit = item.deltaStars >= 0
                                    let title: String = {
                                        switch item.entryType {
                                        case "purchase_credit": return "Bought Stars via \(item.provider.capitalized)"
                                        case "gift_receive": return "Received from \(item.counterpartyName.isEmpty ? "Supporter" : item.counterpartyName)"
                                        case "gift_send": return "Sent to \(item.counterpartyName.isEmpty ? "Creator" : item.counterpartyName)"
                                        default: return item.entryType
                                        }
                                    }()
                                    HStack(spacing: 12) {
                                        TelefamIosGoldStarBadge(size: 22, showSoftBg: true)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(title)
                                                .font(.system(size: 14.5, weight: .bold))
                                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                                .lineLimit(1)
                                            Text("Balance after: \(TelefamIosStarsCatalog.formatStars(item.balanceAfter)) Stars")
                                                .font(.system(size: 12))
                                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        }
                                        Spacer()
                                        Text("\(isCredit ? "+" : "")\(TelefamIosStarsCatalog.formatStars(item.deltaStars))")
                                            .font(.system(size: 14.5, weight: .bold))
                                            .foregroundColor(isCredit ? Color(hex: 0x22C55E) : accentCrimson)
                                    }
                                    .padding(14)
                                    .background(card)
                                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(stroke, lineWidth: 1))
                                }
                            }
                        }
                    }
                }
                .padding(16)
                .frame(maxWidth: sizeClass == .regular ? 680 : 600)
                .frame(maxWidth: .infinity)
            }
        }
        .background(bg.ignoresSafeArea())
    }

    // MARK: 2. Send Stars Screen (Screenshot 2)

    private var sendStarsScreen: some View {
        let pageBg = colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FC)
        let cardBg = TelefamPalette.surface(colorScheme)
        let stroke = TelefamPalette.border(colorScheme)

        return VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button { store.navigateBack() } label: {
                    TelefamIosVectorIcon(kind: .backArrow, color: .white, size: 20)
                        .frame(width: 44, height: 44)
                }
                Text("Send Stars")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                Button { store.openBuyStars(preselectedPackage: 100) } label: {
                    HStack(spacing: 6) {
                        TelefamIosGoldStarBadge(size: 16, showSoftBg: false)
                        Text("Balance: \(TelefamIosStarsCatalog.formatStars(store.wallet.balanceStars))")
                            .font(.system(size: 12.5, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 12)
                    .frame(minHeight: 36)
                    .background(Color.black.opacity(0.25))
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(accentCrimson)

            ScrollView {
                VStack(spacing: 12) {
                    if let peer = store.sendPeer {
                        HStack(spacing: 12) {
                            ZStack(alignment: .bottomTrailing) {
                                avatarBadge(url: peer.avatarUrl, name: peer.fullName.isEmpty ? peer.username : peer.fullName, size: 46)
                                if peer.isOnline {
                                    Circle()
                                        .fill(Color(hex: 0x22C55E))
                                        .frame(width: 12, height: 12)
                                        .overlay(Circle().stroke(cardBg, lineWidth: 2))
                                }
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(peer.fullName.isEmpty ? peer.username : peer.fullName)
                                    .font(.system(size: 15.5, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text("@\(peer.username)")
                                    .font(.system(size: 12.5))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            Spacer()
                            TelefamIosVectorIcon(kind: .chevronRight, color: TelefamPalette.textMuted(colorScheme), size: 16)
                        }
                        .padding(14)
                        .background(colorScheme == .dark ? TelefamPalette.blushDark : Color(hex: 0xFDF2F4))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }

                    HStack(alignment: .center, spacing: 14) {
                        TelefamIosGoldStarBadge(size: 32, showSoftBg: true)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Select stars to send")
                                .font(.system(size: 15.5, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text("Show your support and help them grow")
                                .font(.system(size: 12.5))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            HStack(spacing: 5) {
                                TelefamIosGoldStarBadge(size: 12, showSoftBg: false)
                                Text("1 star = 1 appreciation")
                                    .font(.system(size: 11.5, weight: .bold))
                                    .foregroundColor(accentCrimson)
                            }
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(TelefamPalette.blush(colorScheme))
                            .clipShape(Capsule())
                        }
                        Spacer()
                    }
                    .padding(14)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(stroke, lineWidth: 1))

                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(accentCrimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    ForEach(TelefamIosStarsCatalog.presetPackages, id: \.self) { stars in
                        HStack(spacing: 12) {
                            TelefamIosGoldStarBadge(size: 22, showSoftBg: true)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(TelefamIosStarsCatalog.formatStars(stars))
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text(TelefamIosStarsCatalog.formatCurrency(stars: stars, countryIso: store.countryIso))
                                    .font(.system(size: 12.5))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            Spacer()
                            Button {
                                Task { await store.selectTierToSend(stars: stars) }
                            } label: {
                                Text("Send")
                                    .font(.system(size: 13.5, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(minWidth: 88, minHeight: 40)
                                    .background(accentCrimson)
                                    .clipShape(Capsule())
                            }
                            .disabled(store.isSubmitting)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(cardBg)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(stroke, lineWidth: 1))
                    }
                }
                .padding(16)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
            .background(pageBg)
        }
    }

    // MARK: 3. Buy Stars Screen (Screenshot 3 Left)

    private var buyStarsScreen: some View {
        let pageBg = colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FC)
        let cardBg = TelefamPalette.surface(colorScheme)
        let stroke = TelefamPalette.border(colorScheme)

        return VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button { store.navigateBack() } label: {
                    TelefamIosVectorIcon(kind: .backArrow, color: .white, size: 20)
                        .frame(width: 44, height: 44)
                }
                Text("Buy Stars")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(accentCrimson)

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 14) {
                        TelefamIosGoldStarBadge(size: 36, showSoftBg: false)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Support your favorite creators and help them grow!")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text("Buy Stars and send them as a gift in chats and creator moments.")
                                .font(.system(size: 12.5))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(TelefamPalette.blush(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                    Text("Choose a package")
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .padding(.top, 4)

                    ForEach(TelefamIosStarsCatalog.presetPackages, id: \.self) { stars in
                        let isPopular = stars == 5
                        HStack(spacing: 12) {
                            TelefamIosGoldStarBadge(size: 22, showSoftBg: true)
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 8) {
                                    Text(TelefamIosStarsCatalog.formatStars(stars))
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    if isPopular {
                                        Text("Popular")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(accentCrimson)
                                            .clipShape(Capsule())
                                    }
                                }
                                Text(TelefamIosStarsCatalog.formatCurrency(stars: stars, countryIso: store.countryIso))
                                    .font(.system(size: 12.5))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            Spacer()
                            Button {
                                store.selectPackageAndProceedToCheckout(stars: stars)
                            } label: {
                                Text("Buy")
                                    .font(.system(size: 13.5, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(minWidth: 84, minHeight: 40)
                                    .background(accentCrimson)
                                    .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(cardBg)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(isPopular ? accentCrimson : stroke, lineWidth: isPopular ? 1.8 : 1)
                        )
                    }
                }
                .padding(16)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
            .background(pageBg)
        }
    }

    // MARK: 4. Checkout Screen (Screenshot 3 Right - Paystack & PayPal + Verify)

    private var checkoutScreen: some View {
        let pageBg = colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FC)
        let cardBg = TelefamPalette.surface(colorScheme)
        let stroke = TelefamPalette.border(colorScheme)

        return VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button { store.navigateBack() } label: {
                    TelefamIosVectorIcon(kind: .backArrow, color: .white, size: 20)
                        .frame(width: 44, height: 44)
                }
                Text("Checkout")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(accentCrimson)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // Summary Card
                    HStack(spacing: 14) {
                        TelefamIosGoldStarBadge(size: 28, showSoftBg: true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(TelefamIosStarsCatalog.formatStars(store.selectedBuyStars)) Stars")
                                .font(.system(size: 19, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text(TelefamIosStarsCatalog.formatCurrency(stars: store.selectedBuyStars, countryIso: store.countryIso))
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                        Spacer()
                    }
                    .padding(16)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(stroke, lineWidth: 1))

                    Text("Payment Method")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .padding(.top, 4)

                    // Paystack Card
                    paymentMethodCard(
                        selected: store.selectedProvider == .paystack,
                        title: "Paystack",
                        subtitle: "Pay securely with M-Pesa, cards and bank transfers",
                        cardBg: cardBg,
                        stroke: stroke,
                        logo: AnyView(TelefamIosPaystackLogo().frame(width: 34, height: 34))
                    ) {
                        store.selectedProvider = .paystack
                    }

                    // PayPal Card
                    paymentMethodCard(
                        selected: store.selectedProvider == .paypal,
                        title: "PayPal",
                        subtitle: "Pay with your PayPal account or card",
                        cardBg: cardBg,
                        stroke: stroke,
                        logo: AnyView(TelefamIosPayPalLogo().frame(width: 34, height: 34))
                    ) {
                        store.selectedProvider = .paypal
                    }

                    // Encryption Security Banner
                    HStack(alignment: .top, spacing: 12) {
                        TelefamIosVectorIcon(kind: .shieldCheck, color: accentCrimson, size: 22)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Your payment is secure and encrypted")
                                .font(.system(size: 13.5, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text("We use industry-standard encryption, idempotency verification, and signed webhooks.")
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(TelefamPalette.blush(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(accentCrimson)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    Button {
                        Task { await store.proceedToPay() }
                    } label: {
                        Text(store.isSubmitting ? "Processing..." : "Proceed to Pay")
                            .font(.system(size: 15.5, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                    .disabled(store.isSubmitting)

                    if store.activePayment != nil {
                        Button {
                            Task { await store.verifyActivePayment() }
                        } label: {
                            Text("Verify Payment & Credit Stars")
                                .font(.system(size: 14.5, weight: .bold))
                                .foregroundColor(accentCrimson)
                                .frame(maxWidth: .infinity, minHeight: 46)
                                .background(cardBg)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(accentCrimson, lineWidth: 1.4))
                        }
                        .disabled(store.isSubmitting)
                    }
                }
                .padding(16)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
            .background(pageBg)
        }
    }

    private func paymentMethodCard(
        selected: Bool,
        title: String,
        subtitle: String,
        cardBg: Color,
        stroke: Color,
        logo: AnyView,
        onTap: @escaping () -> Void
    ) -> some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                logo
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    Text(subtitle)
                        .font(.system(size: 12.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                ZStack {
                    Circle()
                        .stroke(selected ? accentCrimson : TelefamPalette.textMuted(colorScheme).opacity(0.6), lineWidth: 2)
                        .frame(width: 22, height: 22)
                    if selected {
                        Circle()
                            .fill(accentCrimson)
                            .frame(width: 12, height: 12)
                    }
                }
            }
            .padding(16)
            .background(cardBg)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(selected ? accentCrimson : stroke, lineWidth: selected ? 1.8 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func avatarBadge(url: String, name: String, size: CGFloat) -> some View {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let initials = String(cleanName.prefix(2)).uppercased()
        let resolved = store.resolvedMediaURL(url)
        Group {
            if !resolved.isEmpty {
                TelefamIosRemoteImage(
                    urlString: resolved,
                    bearerToken: store.authBearerToken,
                    deviceFingerprint: store.authDeviceFingerprint,
                    fallbackInitials: initials.isEmpty ? "T" : initials
                )
            } else {
                Circle()
                    .fill(TelefamPalette.crimson.opacity(0.18))
                    .overlay(
                        Text(initials.isEmpty ? "T" : initials)
                            .font(.system(size: size * 0.34, weight: .bold))
                            .foregroundColor(TelefamPalette.crimson)
                    )
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }
}

// MARK: - In-App WKWebView Checkout Interceptor

struct TelefamIosCheckoutWebView: UIViewRepresentable {
    let initialUrlString: String
    let onReturnUrlDetected: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onReturnUrlDetected: onReturnUrlDetected)
    }

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.websiteDataStore = .nonPersistent()
        let wv = WKWebView(frame: .zero, configuration: cfg)
        wv.navigationDelegate = context.coordinator
        if let url = URL(string: initialUrlString) {
            wv.load(URLRequest(url: url))
        }
        return wv
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate {
        let onReturnUrlDetected: (String) -> Void

        init(onReturnUrlDetected: @escaping (String) -> Void) {
            self.onReturnUrlDetected = onReturnUrlDetected
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            if let url = navigationAction.request.url {
                let s = url.absoluteString.lowercased()
                if url.scheme?.lowercased() == "telefam" || s.contains("/v1/stars/checkout/return") || s.contains("payment-return") {
                    decisionHandler(.cancel)
                    onReturnUrlDetected(url.absoluteString)
                    return
                }
            }
            decisionHandler(.allow)
        }
    }
}
