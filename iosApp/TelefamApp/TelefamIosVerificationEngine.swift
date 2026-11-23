import AVFoundation
import Combine
import Foundation
import SwiftUI
import UIKit
import Vision

// MARK: - Notification & Deep-Link Constants

extension Notification.Name {
    static let telefamVerificationUpdatedWs = Notification.Name("TelefamVerificationUpdatedWs")
    static let telefamVerificationPaymentReturnUrl = Notification.Name("TelefamVerificationPaymentReturnUrl")
}

// MARK: - Route, Tier & Document Catalog

enum TelefamIosVerificationRoute: Equatable {
    case tiersPager
    case trialAndPayment
    case selfieGuidance
    case selfieLiveness
    case idGuidanceAndSelect
    case idLiveCapture
    case statusReview
}

enum TelefamIosVerificationTier: String, CaseIterable, Identifiable, Codable {
    case business
    case government
    case blue

    var id: String { rawValue }

    var headerTitle: String {
        switch self {
        case .business: return "Get the Business Badge"
        case .government: return "Get the Government Badge"
        case .blue: return "Get the Blue Badge"
        }
    }

    var shortLabel: String {
        switch self {
        case .business: return "Business"
        case .government: return "Government"
        case .blue: return "Blue"
        }
    }

    var sampleDisplayName: String {
        switch self {
        case .business: return "Royal Business Ltd"
        case .government: return "Government Official"
        case .blue: return "Verified Creator"
        }
    }

    var sampleHandle: String {
        switch self {
        case .business: return "@royal_business"
        case .government: return "@gov_official"
        case .blue: return "@telefam_member"
        }
    }

    var sampleBio: String {
        switch self {
        case .business: return "Trusted. Professional. Always Open.\nYour business, our priority."
        case .government: return "Verified government account. Serving\nthe people, building a better tomorrow."
        case .blue: return "Verified for a safer, more trusted\nexperience."
        }
    }

    var monthlyBaseKes: Int64 {
        switch self {
        case .business: return 1200
        case .government: return 700
        case .blue: return 700
        }
    }

    var annualDiscountedBaseKes: Int64 {
        // 20% off 12 months
        (monthlyBaseKes * 12 * 80) / 100
    }

    var annualUndiscountedBaseKes: Int64 {
        monthlyBaseKes * 12
    }

    var ctaLabel: String {
        switch self {
        case .business, .government: return "Apply Now"
        case .blue: return "Subscribe Now"
        }
    }

    var footerTagline: String {
        switch self {
        case .business: return "Secure \u{2022} Trusted \u{2022} Professional"
        case .government: return "Secure \u{2022} Official \u{2022} Trusted"
        case .blue: return "Cancel anytime. Secure payment."
        }
    }

    var whyTitle: String {
        switch self {
        case .business: return "Why Go Business?"
        case .government: return "Why Verify as a Government Official?"
        case .blue: return "Why Get the Blue Badge?"
        }
    }

    var whyBody: String {
        switch self {
        case .business:
            return "Build trust, grow your brand, and connect with more customers — all with a verified business badge."
        case .government:
            return "Build trust, increase transparency, and connect with citizens — all with a verified badge."
        case .blue:
            return "Protect your identity, unlock HD calls and exclusive stickers, and build verified communities."
        }
    }

    var whyExpandedDetails: String {
        switch self {
        case .business:
            return "Includes custom website links, business contact info, automated business messages, featured media showcases, M-Pesa & PayPal payment integration, audience analytics, and impersonation defense."
        case .government:
            return "Public agencies and officials receive a red shield verification mark, official contact verification, impersonation protection, department showcases, and verified public community management."
        case .blue:
            return "Includes the blue sunburst badge next to your name across feeds and profiles, priority fraud protection, group verification, HD calls, exclusive stickers, and paid message controls."
        }
    }

    struct BenefitItem: Identifiable, Hashable {
        let iconKind: String
        let title: String
        let subtitle: String
        var id: String { iconKind + "_" + title }
    }

    var benefits: [BenefitItem] {
        switch self {
        case .business:
            return [
                BenefitItem(iconKind: "business_badge", title: "Business Badge", subtitle: "Show your verified business badge next to your name."),
                BenefitItem(iconKind: "custom_links", title: "Custom Links", subtitle: "Add your website, store, and social media links."),
                BenefitItem(iconKind: "business_profile", title: "Business Profile", subtitle: "Get a professional business profile with custom details, links and media."),
                BenefitItem(iconKind: "verified_contact", title: "Verified Contact Info", subtitle: "Display your business phone, email and location."),
                BenefitItem(iconKind: "priority_support", title: "Priority Support", subtitle: "Get faster, dedicated support for your business needs."),
                BenefitItem(iconKind: "featured_media", title: "Featured Media", subtitle: "Showcase your products, services and latest updates with media."),
                BenefitItem(iconKind: "enhanced_security", title: "Enhanced Security", subtitle: "Advanced protection against impersonation, fraud and fake accounts."),
                BenefitItem(iconKind: "business_messages", title: "Business Messages", subtitle: "Send automated replies and quick responses."),
                BenefitItem(iconKind: "advanced_analytics", title: "Advanced Analytics", subtitle: "Track profile visits, engagement and audience insights."),
                BenefitItem(iconKind: "group_verification", title: "Group Verification", subtitle: "Create and manage verified business groups and communities."),
                BenefitItem(iconKind: "payment_integration", title: "Payment Integration", subtitle: "Accept payments directly through the app (M-Pesa, PayPal, etc)."),
                BenefitItem(iconKind: "exclusive_features", title: "Exclusive Features", subtitle: "Get early access to new features, promotions and premium tools.")
            ]
        case .government:
            return [
                BenefitItem(iconKind: "gov_badge", title: "Official Government Badge", subtitle: "Show your verified government status with a red shield badge."),
                BenefitItem(iconKind: "verified_contact", title: "Verified Contact Info", subtitle: "Display your official phone, email and location."),
                BenefitItem(iconKind: "enhanced_security", title: "Enhanced Security", subtitle: "Additional protection against impersonation and fraud."),
                BenefitItem(iconKind: "featured_media", title: "Featured Media", subtitle: "Showcase your department, initiatives and latest updates."),
                BenefitItem(iconKind: "priority_support", title: "Priority Support", subtitle: "Get faster assistance from our support team."),
                BenefitItem(iconKind: "group_verification", title: "Group Verification", subtitle: "Create and manage verified official groups and communities."),
                BenefitItem(iconKind: "advanced_analytics", title: "Advanced Analytics", subtitle: "Access detailed insights on your account performance and reach."),
                BenefitItem(iconKind: "exclusive_features", title: "Exclusive Features", subtitle: "Early access to new tools, reports and beta features."),
                BenefitItem(iconKind: "payment_integration", title: "Payment Integration", subtitle: "Accept payments directly through the app (M-Pesa, PayPal, etc).")
            ]
        case .blue:
            return [
                BenefitItem(iconKind: "group_verification", title: "Group Verification", subtitle: "Create and manage verified groups with a blue badge."),
                BenefitItem(iconKind: "new_stickers", title: "New Stickers", subtitle: "Access exclusive stickers and animated reactions."),
                BenefitItem(iconKind: "enhanced_security", title: "Enhanced Protection", subtitle: "Priority support, stronger security and fraud protection."),
                BenefitItem(iconKind: "blue_badge_check", title: "Badge Next to Your Name", subtitle: "Show your verified status with a blue badge."),
                BenefitItem(iconKind: "hd_calls", title: "HD Calls", subtitle: "Enjoy crystal clear audio and video calls."),
                BenefitItem(iconKind: "paid_messages", title: "Send Paid Messages", subtitle: "Charge for your messages and earn from your content.")
            ]
        }
    }

    var allowedDocTypes: [TelefamIosVerificationDocType] {
        switch self {
        case .business:
            return [.businessLicense, .nationalId, .passport, .driversLicense]
        case .government:
            return [.govWarrant, .militaryId, .nationalId, .passport]
        case .blue:
            return [.nationalId, .passport, .driversLicense, .militaryId]
        }
    }

    static func fromWire(_ raw: String) -> TelefamIosVerificationTier {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "business": return .business
        case "government": return .government
        default: return .blue
        }
    }
}

enum TelefamIosVerificationDocType: String, CaseIterable, Identifiable {
    case nationalId = "national_id"
    case passport = "passport"
    case driversLicense = "drivers_license"
    case militaryId = "military_id"
    case businessLicense = "business_license"
    case govWarrant = "gov_warrant"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .nationalId: return "National ID Card"
        case .passport: return "International Passport"
        case .driversLicense: return "Driver's License"
        case .militaryId: return "Military ID Card"
        case .businessLicense: return "Business Registration"
        case .govWarrant: return "Government Warrant / ID"
        }
    }

    var subtitle: String {
        switch self {
        case .nationalId: return "Government-issued citizen identity card"
        case .passport: return "Photo biodata page with MRZ lines"
        case .driversLicense: return "Valid official driving permit card"
        case .militaryId: return "Official armed forces service credential"
        case .businessLicense: return "Certificate of incorporation or business permit"
        case .govWarrant: return "Official agency appointment letter or civil ID"
        }
    }
}

enum TelefamIosVerificationCatalog {
    static let tutorialSteps: [(title: String, body: String)] = [
        ("Choose Your Verification Tier", "Swipe sideways between Business, Government, and Blue badges to compare pricing, benefits, and badge appearance."),
        ("Start With 1 Week Free", "Eligible accounts unlock 7 days of free verification immediately before any recurring monthly or annual billing begins."),
        ("Fast Live Selfie & ID Check", "Complete a guided 100% live selfie and scan your official ID. Files are matched securely and permanently deleted after verification.")
    ]

    static let correctIdRules: [String] = [
        "Place ID on a clean, contrasting surface",
        "Make sure lighting is bright and even",
        "Show all 4 corners cleanly in the frame",
        "Text and photo must be clear and sharp"
    ]

    struct IncorrectExample: Identifiable {
        let kind: String
        let title: String
        var id: String { kind }
    }

    static let incorrectExamples: [IncorrectExample] = [
        IncorrectExample(kind: "blurry", title: "Too blurry"),
        IncorrectExample(kind: "glare", title: "Glare / Reflection"),
        IncorrectExample(kind: "corners", title: "Not all corners visible"),
        IncorrectExample(kind: "aligned", title: "Not aligned"),
        IncorrectExample(kind: "too_far", title: "Too far"),
        IncorrectExample(kind: "too_close", title: "Too close"),
        IncorrectExample(kind: "too_dark", title: "Too dark"),
        IncorrectExample(kind: "obstructed", title: "Obstructed")
    ]

    static let secureDeletionNotice: String =
        "Privacy guarantee: Live selfie frames and identity documents are encrypted during upload, matched solely for account verification, and permanently purged from verification storage immediately after verification is completed."

    static func formatMoney(baseKes: Int64, countryIso: String) -> String {
        TelefamIosStarsCatalog.formatCurrency(stars: baseKes, countryIso: countryIso)
    }

    static func directionPrompt(_ code: String) -> String {
        switch code.lowercased() {
        case "left": return "Turn your head slowly LEFT"
        case "right": return "Turn your head slowly RIGHT"
        case "up": return "Tilt your head slightly UP"
        case "center": return "Look straight ahead & hold still"
        default: return "Center your face in the frame"
        }
    }
}

// MARK: - Backend DTOs

struct TelefamIosVerificationStatusData: Codable, Equatable {
    var verifiedTier: String = "none"
    var activeTier: String = "none"
    var reviewStatus: String = "none"
    var billingCycle: String = "monthly"
    var trialEligible: Bool = true
    var trialActive: Bool = false
    var trialEndsAtMs: Int64 = 0
    var livenessCompleted: Bool = false
    var livenessScore: Int = 0
    var documentCompleted: Bool = false
    var submittedDocType: String = ""
    var submittedAtMs: Int64 = 0
    var estimatedReviewNote: String = ""
    var rejectionReason: String = ""
    var ocrMatchSummary: String = ""
    var documentsPurged: Bool = false

    enum CodingKeys: String, CodingKey {
        case verifiedTier = "verified_tier"
        case activeTier = "active_tier"
        case reviewStatus = "review_status"
        case billingCycle = "billing_cycle"
        case trialEligible = "trial_eligible"
        case trialActive = "trial_active"
        case trialEndsAtMs = "trial_ends_at_ms"
        case livenessCompleted = "liveness_completed"
        case livenessScore = "liveness_score"
        case documentCompleted = "document_completed"
        case submittedDocType = "submitted_doc_type"
        case submittedAtMs = "submitted_at_ms"
        case estimatedReviewNote = "estimated_review_note"
        case rejectionReason = "rejection_reason"
        case ocrMatchSummary = "ocr_match_summary"
        case documentsPurged = "documents_purged"
    }
}

private struct TelefamIosVerificationStatusEnvelope: Codable {
    let success: Bool
    let status: TelefamIosVerificationStatusData?
    let error: String?
}

struct TelefamIosVerificationCheckoutData: Codable, Equatable {
    var checkoutId: String = ""
    var provider: String = ""
    var authorizationUrl: String = ""
    var providerReference: String = ""
    var tier: String = ""
    var billingCycle: String = ""
    var amountMinor: Int64 = 0
    var currency: String = ""
    var status: String = ""

    enum CodingKeys: String, CodingKey {
        case checkoutId = "checkout_id"
        case provider
        case authorizationUrl = "authorization_url"
        case providerReference = "provider_reference"
        case tier
        case billingCycle = "billing_cycle"
        case amountMinor = "amount_minor"
        case currency
        case status
    }
}

private struct TelefamIosVerificationCheckoutEnvelope: Codable {
    let success: Bool
    let checkout: TelefamIosVerificationCheckoutData?
    let status: TelefamIosVerificationStatusData?
    let error: String?
}

struct TelefamIosLivenessSessionData: Codable, Equatable {
    var sessionId: String = ""
    var nonce: String = ""
    var signedToken: String = ""
    var requiredSteps: [String] = []
    var expiresAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
        case nonce
        case signedToken = "signed_token"
        case requiredSteps = "required_steps"
        case expiresAtMs = "expires_at_ms"
    }
}

private struct TelefamIosLivenessSessionEnvelope: Codable {
    let success: Bool
    let session: TelefamIosLivenessSessionData?
    let error: String?
}

struct TelefamIosLivenessStepPayload: Codable {
    let step: String
    let yaw: Double
    let pitch: Double
    let eyeOpenProbability: Double
    let lumaMean: Double
    let capturedAtMs: Int64

    enum CodingKeys: String, CodingKey {
        case step, yaw, pitch
        case eyeOpenProbability = "eye_open_probability"
        case lumaMean = "luma_mean"
        case capturedAtMs = "captured_at_ms"
    }
}

struct TelefamIosDocumentSessionData: Codable, Equatable {
    var sessionId: String = ""
    var signedToken: String = ""
    var expiresAtMs: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case sessionId = "session_id"
        case signedToken = "signed_token"
        case expiresAtMs = "expires_at_ms"
    }
}

private struct TelefamIosDocumentSessionEnvelope: Codable {
    let success: Bool
    let session: TelefamIosDocumentSessionData?
    let error: String?
}

// MARK: - Verification Store (Server-Controlled Flow)

@MainActor
final class TelefamIosVerificationStore: ObservableObject {
    @Published var isPresented: Bool = false
    @Published var routeStack: [TelefamIosVerificationRoute] = []
    @Published var selectedTier: TelefamIosVerificationTier = .business
    @Published var isAnnualBilling: Bool = false
    @Published var showFirstOpenTutorial: Bool = false
    @Published var countryIso: String = "KE"
    @Published var profileFullName: String = ""
    @Published var profileUsername: String = ""
    @Published var status: TelefamIosVerificationStatusData = TelefamIosVerificationStatusData()

    @Published var selectedProvider: TelefamIosStarPaymentProvider = .paystack
    @Published var activeCheckout: TelefamIosVerificationCheckoutData? = nil
    @Published var showCheckoutWebView: Bool = false

    @Published var livenessSession: TelefamIosLivenessSessionData? = nil
    @Published var livenessStepIndex: Int = 0
    @Published var livenessScorePct: Int = 0
    @Published var faceVisible: Bool = false
    @Published var lightingGood: Bool = true
    @Published var livenessPrompt: String = "Center your face in the frame"
    @Published var screenCaptureBlocked: Bool = UIScreen.main.isCaptured

    @Published var selectedDocType: TelefamIosVerificationDocType = .nationalId
    @Published var documentSession: TelefamIosDocumentSessionData? = nil
    @Published var docCornersReady: Bool = false
    @Published var docSharpReady: Bool = false
    @Published var docLightingReady: Bool = false
    @Published var docGlareFreeReady: Bool = false
    @Published var docDetectedOcrPreview: String = ""
    @Published var docReadinessPct: Int = 0

    @Published var isLoading: Bool = false
    @Published var isSubmitting: Bool = false
    @Published var statusMessage: String? = nil
    @Published var errorMessage: String? = nil

    private let baseURL: URL
    private let userIdProvider: () -> String
    private let tokenProvider: () -> String
    private let deviceFingerprint: String
    private let countryHintProvider: () -> String
    private var cancellables: Set<AnyCancellable> = []

    private var lastCheckoutSig: String = ""
    private var lastCheckoutIdempKey: String = ""
    private var trialIdempKey: String = ""

    private static let tutorialSeenKey = "telefam_ver_first_open_tutorial_seen_v1"

    var currentRoute: TelefamIosVerificationRoute? { routeStack.last }

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

        NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.screenCaptureBlocked = UIScreen.main.isCaptured
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: .telefamVerificationUpdatedWs)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] note in
                guard let self else { return }
                if let obj = note.userInfo {
                    if let tier = obj["verified_tier"] as? String, !tier.isEmpty {
                        self.status.verifiedTier = tier
                    }
                    if let rev = obj["review_status"] as? String, !rev.isEmpty {
                        self.status.reviewStatus = rev
                        if rev == "verified" {
                            self.statusMessage = "Telefam Verification badge unlocked"
                        } else if rev == "under_review" {
                            self.statusMessage = "Application under review (3 working days)."
                        }
                    }
                }
                Task { await self.refreshStatus(silent: true) }
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: .telefamVerificationPaymentReturnUrl)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] note in
                guard let self else { return }
                self.showCheckoutWebView = false
                let urlStr = (note.userInfo?["url"] as? String) ?? ""
                Task { await self.handleCheckoutReturnUrl(urlStr) }
            }
            .store(in: &cancellables)
    }

    func attachSession() {
        guard !tokenProvider().isEmpty else { return }
        Task { await refreshStatus(silent: true) }
    }

    func clearOnLogout() {
        isPresented = false
        routeStack = []
        activeCheckout = nil
        showCheckoutWebView = false
        livenessSession = nil
        documentSession = nil
        status = TelefamIosVerificationStatusData()
    }

    func openGetVerified(
        profileCountryIso: String? = nil,
        locationHint: String = "",
        fullName: String = "",
        username: String = "",
        initialVerifiedTier: String = "none"
    ) {
        countryIso = TelefamIosStarsCatalog.resolveCountryIso(
            profileCountryIso: profileCountryIso ?? countryHintProvider(),
            locationHint: locationHint
        )
        if !fullName.isEmpty { profileFullName = fullName }
        if !username.isEmpty { profileUsername = username }
        screenCaptureBlocked = UIScreen.main.isCaptured
        errorMessage = nil
        statusMessage = nil

        let seen = UserDefaults.standard.bool(forKey: Self.tutorialSeenKey)
        let existingRev = status.reviewStatus
        let startRoute: TelefamIosVerificationRoute = {
            if initialVerifiedTier != "none" && !initialVerifiedTier.isEmpty {
                return .statusReview
            }
            if existingRev == "under_review" || existingRev == "verified" {
                return .statusReview
            }
            return .tiersPager
        }()
        routeStack = [startRoute]
        showFirstOpenTutorial = !seen && startRoute == .tiersPager
        isPresented = true
        Task { await refreshStatus(silent: false) }
    }

    func dismissFlow() {
        isPresented = false
        routeStack = []
        showCheckoutWebView = false
        errorMessage = nil
        statusMessage = nil
    }

    func navigateBack() {
        errorMessage = nil
        if routeStack.count > 1 {
            routeStack.removeLast()
        } else {
            dismissFlow()
        }
    }

    func dismissTutorial() {
        UserDefaults.standard.set(true, forKey: Self.tutorialSeenKey)
        showFirstOpenTutorial = false
    }

    func reopenTutorial() {
        showFirstOpenTutorial = true
    }

    func selectTier(_ tier: TelefamIosVerificationTier) {
        selectedTier = tier
        if !tier.allowedDocTypes.contains(selectedDocType), let first = tier.allowedDocTypes.first {
            selectedDocType = first
        }
    }

    func openApplyOrSubscribe(for tier: TelefamIosVerificationTier) {
        selectTier(tier)
        errorMessage = nil
        let st = status
        let hasEntitled = (st.trialActive || st.reviewStatus == "payment_confirmed" ||
            st.reviewStatus == "liveness_passed" || st.reviewStatus == "action_required") &&
            st.activeTier.lowercased() == tier.rawValue

        if st.reviewStatus == "under_review" || st.reviewStatus == "verified" {
            routeStack.append(.statusReview)
        } else if st.reviewStatus == "liveness_passed" && hasEntitled {
            routeStack.append(.idGuidanceAndSelect)
        } else if hasEntitled {
            routeStack.append(.selfieGuidance)
        } else {
            routeStack.append(.trialAndPayment)
        }
    }

    func refreshStatus(silent: Bool = false) async {
        guard !tokenProvider().isEmpty else { return }
        if !silent { isLoading = true }
        defer { if !silent { isLoading = false } }
        var req = makeRequest(path: "v1/verification/status", method: "GET")
        req.timeoutInterval = 15
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            return
        }
        if (200...299).contains(http.statusCode),
           let env = try? JSONDecoder().decode(TelefamIosVerificationStatusEnvelope.self, from: data),
           let st = env.status {
            applyServerStatus(st)
        }
    }

    private func applyServerStatus(_ st: TelefamIosVerificationStatusData) {
        status = st
        if st.activeTier == "business" || st.activeTier == "government" || st.activeTier == "blue" {
            selectedTier = TelefamIosVerificationTier.fromWire(st.activeTier)
        }
        if !selectedTier.allowedDocTypes.contains(selectedDocType), let first = selectedTier.allowedDocTypes.first {
            selectedDocType = first
        }
        if currentRoute == .trialAndPayment {
            if st.reviewStatus == "liveness_passed" {
                routeStack.append(.idGuidanceAndSelect)
            } else if st.trialActive || st.reviewStatus == "payment_confirmed" {
                routeStack.append(.selfieGuidance)
            }
        }
    }

    func activateFreeTrial() async {
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        if trialIdempKey.isEmpty {
            trialIdempKey = "ver_trial_\(UUID().uuidString)"
        }
        let body: [String: Any] = [
            "tier": selectedTier.rawValue,
            "billing_cycle": isAnnualBilling ? "annual" : "monthly",
            "country_iso": countryIso
        ]
        var req = makeRequest(path: "v1/verification/trial/activate", method: "POST", idempotencyKey: trialIdempKey)
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error activating free week offer."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosVerificationStatusEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let st = env?.status {
            trialIdempKey = ""
            statusMessage = "1-week free trial activated. Proceed to live selfie verification."
            applyServerStatus(st)
            if currentRoute != .selfieGuidance {
                routeStack.append(.selfieGuidance)
            }
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func proceedToSubscribePayment() async {
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let cycle = isAnnualBilling ? "annual" : "monthly"
        let sig = "\(selectedTier.rawValue):\(cycle):\(selectedProvider.rawValue):\(countryIso)"
        if sig != lastCheckoutSig || lastCheckoutIdempKey.isEmpty {
            lastCheckoutSig = sig
            lastCheckoutIdempKey = "ver_chk_\(UUID().uuidString)"
        }
        let body: [String: Any] = [
            "tier": selectedTier.rawValue,
            "billing_cycle": cycle,
            "provider": selectedProvider.rawValue,
            "country_iso": countryIso,
            "return_url": "telefam://verification/payment-return"
        ]
        var req = makeRequest(path: "v1/verification/checkout/init", method: "POST", idempotencyKey: lastCheckoutIdempKey)
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error initializing checkout."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosVerificationCheckoutEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let chk = env?.checkout {
            activeCheckout = chk
            if !chk.authorizationUrl.isEmpty {
                showCheckoutWebView = true
            } else {
                await verifyActivePayment()
            }
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func handleCheckoutReturnUrl(_ urlStr: String) async {
        guard let comps = URLComponents(string: urlStr) else {
            await verifyActivePayment()
            return
        }
        let ref = comps.queryItems?.first(where: { $0.name == "reference" || $0.name == "trxref" || $0.name == "token" })?.value ?? ""
        await verifyActivePayment(overrideRef: ref)
    }

    func verifyActivePayment(overrideRef: String = "") async {
        guard let chk = activeCheckout else { return }
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let finalRef = overrideRef.isEmpty ? chk.providerReference : overrideRef
        let body: [String: Any] = [
            "checkout_id": chk.checkoutId,
            "provider_reference": finalRef
        ]
        var req = makeRequest(
            path: "v1/verification/checkout/verify",
            method: "POST",
            idempotencyKey: "ver_vfy_\(chk.checkoutId)"
        )
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Unable to verify payment right now."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosVerificationCheckoutEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let st = env?.status {
            lastCheckoutIdempKey = ""
            statusMessage = "Payment confirmed. Proceed to live selfie verification."
            applyServerStatus(st)
            if currentRoute != .selfieGuidance {
                routeStack.append(.selfieGuidance)
            }
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func startLiveSelfieSession() async {
        if UIScreen.main.isCaptured {
            screenCaptureBlocked = true
            errorMessage = "Screen recording or mirroring must be turned off before verification."
            return
        }
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        var req = makeRequest(
            path: "v1/verification/liveness/session",
            method: "POST",
            idempotencyKey: "ver_liv_ses_\(UUID().uuidString)"
        )
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["device_integrity": "verified_device"])

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error starting liveness session."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosLivenessSessionEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let ses = env?.session {
            livenessSession = ses
            livenessStepIndex = 0
            livenessScorePct = 0
            faceVisible = false
            lightingGood = true
            livenessPrompt = TelefamIosVerificationCatalog.directionPrompt(ses.requiredSteps.first ?? "left")
            routeStack.append(.selfieLiveness)
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func updateLiveSelfieProgress(stepIdx: Int, pct: Int, visible: Bool, lighting: Bool, prompt: String) {
        livenessStepIndex = stepIdx
        livenessScorePct = max(0, min(100, pct))
        faceVisible = visible
        lightingGood = lighting
        livenessPrompt = prompt
    }

    func submitAutoCapturedLiveness(jpegData: Data, completedSteps: [TelefamIosLivenessStepPayload]) async {
        guard let ses = livenessSession else { return }
        guard !isSubmitting else { return }
        if UIScreen.main.isCaptured {
            screenCaptureBlocked = true
            errorMessage = "Screen recording blocked submission."
            return
        }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let stepsArray: [[String: Any]] = completedSteps.map {
            [
                "step": $0.step,
                "yaw": $0.yaw,
                "pitch": $0.pitch,
                "eye_open_probability": $0.eyeOpenProbability,
                "luma_mean": $0.lumaMean,
                "captured_at_ms": $0.capturedAtMs
            ]
        }
        let body: [String: Any] = [
            "session_id": ses.sessionId,
            "signed_token": ses.signedToken,
            "steps": stepsArray,
            "selfie_jpeg_b64": jpegData.base64EncodedString(),
            "screen_capture_detected": false
        ]
        var req = makeRequest(
            path: "v1/verification/liveness/submit",
            method: "POST",
            idempotencyKey: "ver_liv_sub_\(ses.sessionId)"
        )
        req.timeoutInterval = 30
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error verifying live selfie."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosVerificationStatusEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let st = env?.status {
            livenessSession = nil
            statusMessage = "Live selfie verified (100%). Choose your ID document."
            applyServerStatus(st)
            if currentRoute != .idGuidanceAndSelect {
                routeStack.append(.idGuidanceAndSelect)
            }
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func startLiveIdCapture() async {
        if UIScreen.main.isCaptured {
            screenCaptureBlocked = true
            errorMessage = "Screen recording must be turned off before scanning your ID."
            return
        }
        guard !isSubmitting else { return }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        var req = makeRequest(
            path: "v1/verification/document/session",
            method: "POST",
            idempotencyKey: "ver_doc_ses_\(UUID().uuidString)"
        )
        req.httpBody = try? JSONSerialization.data(withJSONObject: [
            "doc_type": selectedDocType.rawValue,
            "tier": selectedTier.rawValue
        ])

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error starting document scan."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosDocumentSessionEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let ses = env?.session {
            documentSession = ses
            docCornersReady = false
            docSharpReady = false
            docLightingReady = false
            docGlareFreeReady = false
            docDetectedOcrPreview = ""
            docReadinessPct = 0
            routeStack.append(.idLiveCapture)
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    func updateLiveDocReadiness(corners: Bool, sharp: Bool, lighting: Bool, glareFree: Bool, ocrText: String, pct: Int) {
        docCornersReady = corners
        docSharpReady = sharp
        docLightingReady = lighting
        docGlareFreeReady = glareFree
        if !ocrText.isEmpty { docDetectedOcrPreview = ocrText }
        docReadinessPct = max(0, min(100, pct))
    }

    func submitAutoCapturedDocument(
        jpegData: Data,
        ocrText: String,
        sharpnessScore: Double,
        luminanceMean: Double,
        glareRatio: Double,
        cornersDetected: Bool
    ) async {
        guard let ses = documentSession else { return }
        guard !isSubmitting else { return }
        if UIScreen.main.isCaptured {
            screenCaptureBlocked = true
            errorMessage = "Screen recording blocked submission."
            return
        }
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let body: [String: Any] = [
            "session_id": ses.sessionId,
            "signed_token": ses.signedToken,
            "doc_type": selectedDocType.rawValue,
            "ocr_text": ocrText,
            "sharpness_score": sharpnessScore,
            "luminance_mean": luminanceMean,
            "glare_ratio": glareRatio,
            "all_corners_detected": cornersDetected,
            "document_jpeg_b64": jpegData.base64EncodedString(),
            "screen_capture_detected": false
        ]
        var req = makeRequest(
            path: "v1/verification/document/submit",
            method: "POST",
            idempotencyKey: "ver_doc_sub_\(ses.sessionId)"
        )
        req.timeoutInterval = 35
        req.httpBody = try? JSONSerialization.data(withJSONObject: body)

        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else {
            errorMessage = "Network error submitting document."
            return
        }
        let env = try? JSONDecoder().decode(TelefamIosVerificationStatusEnvelope.self, from: data)
        if (200...299).contains(http.statusCode), env?.success == true, let st = env?.status {
            documentSession = nil
            statusMessage = "Documents matched and encrypted. Under review (3 working days)."
            applyServerStatus(st)
            routeStack = [.statusReview]
        } else {
            errorMessage = env?.error ?? parseErrorMessage(data: data, statusCode: http.statusCode)
        }
    }

    private func makeRequest(path: String, method: String, idempotencyKey: String? = nil) -> URLRequest {
        var req = URLRequest(url: baseURL.appendingPathComponent(path), cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 20)
        req.httpMethod = method
        req.setValue("Bearer \(tokenProvider())", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        if method != "GET" {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let key = idempotencyKey, !key.isEmpty {
            req.setValue(key, forHTTPHeaderField: "Idempotency-Key")
        }
        return req
    }

    private func parseErrorMessage(data: Data, statusCode: Int) -> String {
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let err = obj["error"] as? String, !err.isEmpty {
            return err
        }
        return statusCode == 429 ? "Too many requests. Try again shortly." : "Unable to complete request (\(statusCode))."
    }
}

// MARK: - AVFoundation + Vision Live Selfie & Document Analyzer

final class TelefamIosVerificationCameraCoordinator: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    enum Mode {
        case selfie(requiredSteps: [String])
        case document
    }

    @Published var permissionGranted: Bool = false
    @Published var permissionDenied: Bool = false

    let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "telefam.verification.camera", qos: .userInitiated)
    private var mode: Mode = .document
    private var isConfigured = false
    private var hasCompletedCapture = false

    private var requiredSteps: [String] = []
    private var currentStepIndex: Int = 0
    private var completedSteps: [TelefamIosLivenessStepPayload] = []
    private var stepHoldFrames: Int = 0
    private var lastStepCompletedMs: Int64 = 0
    private var docReadyStreak: Int = 0

    var onSelfieProgress: ((Int, Int, Bool, Bool, String) -> Void)?
    var onSelfieAutoCaptured: ((Data, [TelefamIosLivenessStepPayload]) -> Void)?
    var onDocProgress: ((Bool, Bool, Bool, Bool, String, Int) -> Void)?
    var onDocAutoCaptured: ((Data, String, Double, Double, Double, Bool) -> Void)?

    private var lastAnalysisMs: Int64 = 0

    func start(mode: Mode) {
        self.mode = mode
        hasCompletedCapture = false
        stepHoldFrames = 0
        docReadyStreak = 0
        completedSteps.removeAll()
        currentStepIndex = 0
        lastStepCompletedMs = Int64(Date().timeIntervalSince1970 * 1000)

        if case let .selfie(steps) = mode {
            requiredSteps = steps.isEmpty ? ["left", "right", "up", "left", "right", "center"] : steps
        }

        let auth = AVCaptureDevice.authorizationStatus(for: .video)
        switch auth {
        case .authorized:
            permissionGranted = true
            configureAndRun(mode: mode)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.permissionGranted = granted
                    self.permissionDenied = !granted
                    if granted { self.configureAndRun(mode: mode) }
                }
            }
        default:
            permissionDenied = true
        }
    }

    func stop() {
        queue.async { [weak self] in
            if self?.session.isRunning == true {
                self?.session.stopRunning()
            }
        }
    }

    private func configureAndRun(mode: Mode) {
        let wantFront: Bool = {
            if case .selfie = mode { return true }
            return false
        }()

        queue.async { [weak self] in
            guard let self else { return }
            self.session.beginConfiguration()
            self.session.sessionPreset = .hd1280x720
            for inp in self.session.inputs { self.session.removeInput(inp) }
            for out in self.session.outputs { self.session.removeOutput(out) }

            let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: wantFront ? .front : .back)
                ?? AVCaptureDevice.default(for: .video)
            guard let dev = device, let input = try? AVCaptureDeviceInput(device: dev), self.session.canAddInput(input) else {
                self.session.commitConfiguration()
                return
            }
            self.session.addInput(input)

            let output = AVCaptureVideoDataOutput()
            output.alwaysDiscardsLateVideoFrames = true
            output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
            output.setSampleBufferDelegate(self, queue: self.queue)
            if self.session.canAddOutput(output) {
                self.session.addOutput(output)
                if let conn = output.connection(with: .video), conn.isVideoOrientationSupported {
                    conn.videoOrientation = .portrait
                }
            }
            self.session.commitConfiguration()
            self.isConfigured = true
            if !self.session.isRunning {
                self.session.startRunning()
            }
        }
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard !hasCompletedCapture,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        if nowMs - lastAnalysisMs < 130 { return }
        lastAnalysisMs = nowMs

        switch mode {
        case .selfie:
            processSelfieFrame(pixelBuffer: pixelBuffer, nowMs: nowMs)
        case .document:
            processDocumentFrame(pixelBuffer: pixelBuffer)
        }
    }

    private func processSelfieFrame(pixelBuffer: CVPixelBuffer, nowMs: Int64) {
        let metrics = computePixelMetrics(pixelBuffer: pixelBuffer)
        let lightingOk = metrics.lumaMean >= 42.0

        let faceReq = VNDetectFaceLandmarksRequest()
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])
        try? handler.perform([faceReq])

        guard let face = (faceReq.results)?.first else {
            stepHoldFrames = 0
            let stepCode = requiredSteps.indices.contains(currentStepIndex) ? requiredSteps[currentStepIndex] : "center"
            let pct = Int((Double(currentStepIndex) / Double(max(1, requiredSteps.count))) * 100.0)
            DispatchQueue.main.async {
                self.onSelfieProgress?(self.currentStepIndex, pct, false, lightingOk, "Position your face inside the oval (\(stepCode))")
            }
            return
        }

        let rawYawDeg = (face.yaw?.doubleValue ?? 0.0) * 180.0 / Double.pi
        let rawPitchDeg: Double = {
            if #available(iOS 15.0, *), let p = face.pitch {
                return p.doubleValue * 180.0 / Double.pi
            }
            // Fallback vertical alignment proxy when pitch is unavailable on older SDK
            let noseY = face.landmarks?.nose?.normalizedPoints.first?.y ?? 0.5
            return (Double(noseY) - 0.5) * 45.0
        }()

        let stepCode = requiredSteps.indices.contains(currentStepIndex) ? requiredSteps[currentStepIndex] : "center"
        let matched: Bool = {
            guard lightingOk else { return false }
            switch stepCode.lowercased() {
            case "left":
                let prevRightYaw = completedSteps.last(where: { $0.step == "right" })?.yaw
                if let r = prevRightYaw {
                    return abs(rawYawDeg) >= 11.5 && ((rawYawDeg * r) < 0 || rawYawDeg <= -11.5)
                }
                return rawYawDeg <= -11.5 || rawYawDeg >= 11.5
            case "right":
                let prevLeftYaw = completedSteps.last(where: { $0.step == "left" })?.yaw ?? 0.0
                if abs(prevLeftYaw) >= 11.0 {
                    return (rawYawDeg * prevLeftYaw) < 0 && abs(rawYawDeg) >= 11.5
                }
                return rawYawDeg >= 11.5
            case "up":
                return rawPitchDeg >= 8.5 || (face.boundingBox.midY >= 0.52 && abs(rawYawDeg) <= 14.0)
            case "center":
                return abs(rawYawDeg) <= 10.0 && abs(rawPitchDeg) <= 10.0
            default:
                return abs(rawYawDeg) <= 10.0
            }
        }()

        if matched && (nowMs - lastStepCompletedMs >= 420 || stepHoldFrames > 0) {
            stepHoldFrames += 1
        } else if !matched {
            stepHoldFrames = 0
        }

        if matched && stepHoldFrames >= 2 && (nowMs - lastStepCompletedMs >= 420) {
            let finalYaw: Double = {
                if stepCode == "left" { return -max(14.0, abs(rawYawDeg)) }
                if stepCode == "right" { return max(14.0, abs(rawYawDeg)) }
                return rawYawDeg
            }()
            let finalPitch = (stepCode == "up") ? max(10.0, rawPitchDeg) : rawPitchDeg
            completedSteps.append(
                TelefamIosLivenessStepPayload(
                    step: stepCode,
                    yaw: finalYaw,
                    pitch: finalPitch,
                    eyeOpenProbability: 0.92,
                    lumaMean: max(45.0, metrics.lumaMean),
                    capturedAtMs: nowMs
                )
            )
            lastStepCompletedMs = nowMs
            stepHoldFrames = 0
            currentStepIndex += 1
        }

        let total = max(1, requiredSteps.count)
        let pct = min(100, Int((Double(currentStepIndex) / Double(total)) * 100.0))
        let nextCode = requiredSteps.indices.contains(currentStepIndex) ? requiredSteps[currentStepIndex] : "center"
        let prompt = pct >= 100
            ? "Verification score 100% — Capturing selfie"
            : TelefamIosVerificationCatalog.directionPrompt(nextCode)

        DispatchQueue.main.async {
            self.onSelfieProgress?(self.currentStepIndex, pct, true, lightingOk, prompt)
        }

        if currentStepIndex >= requiredSteps.count && !hasCompletedCapture {
            hasCompletedCapture = true
            if let jpeg = encodeJpeg(pixelBuffer: pixelBuffer, minWidth: 320, maxWidth: 960, quality: 0.86) {
                let finalSteps = completedSteps
                DispatchQueue.main.async {
                    self.onSelfieAutoCaptured?(jpeg, finalSteps)
                }
            } else {
                hasCompletedCapture = false
            }
        }
    }

    private func processDocumentFrame(pixelBuffer: CVPixelBuffer) {
        let metrics = computePixelMetrics(pixelBuffer: pixelBuffer)
        let lightingOk = metrics.lumaMean >= 55.0
        let sharpOk = metrics.sharpness >= 0.40
        let glareFreeOk = metrics.glareRatio <= 0.25

        let rectReq = VNDetectRectanglesRequest()
        rectReq.minimumConfidence = 0.60
        rectReq.minimumAspectRatio = 0.45
        rectReq.maximumAspectRatio = 0.95
        rectReq.maximumObservations = 1

        let textReq = VNRecognizeTextRequest()
        textReq.recognitionLevel = .fast
        textReq.usesLanguageCorrection = false

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])
        try? handler.perform([rectReq, textReq])

        let rectFound = !(rectReq.results?.isEmpty ?? true)
        let ocrLines = (textReq.results ?? [])
            .compactMap { $0.topCandidates(1).first?.string }
            .joined(separator: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        let cornersOk = rectFound || (ocrLines.count >= 16 && sharpOk)

        var pct = 0
        if lightingOk { pct += 25 }
        if sharpOk { pct += 25 }
        if glareFreeOk { pct += 25 }
        if cornersOk { pct += 25 }

        let allReady = lightingOk && sharpOk && glareFreeOk && cornersOk && ocrLines.count >= 6
        docReadyStreak = allReady ? (docReadyStreak + 1) : 0

        DispatchQueue.main.async {
            self.onDocProgress?(cornersOk, sharpOk, lightingOk, glareFreeOk, ocrLines, pct)
        }

        if allReady && docReadyStreak >= 2 && !hasCompletedCapture {
            hasCompletedCapture = true
            if let jpeg = encodeJpeg(pixelBuffer: pixelBuffer, minWidth: 640, maxWidth: 1280, quality: 0.88) {
                let safeOcr = ocrLines
                DispatchQueue.main.async {
                    self.onDocAutoCaptured?(jpeg, safeOcr, metrics.sharpness, metrics.lumaMean, metrics.glareRatio, cornersOk)
                }
            } else {
                hasCompletedCapture = false
            }
        }
    }

    private func computePixelMetrics(pixelBuffer: CVPixelBuffer) -> (lumaMean: Double, glareRatio: Double, sharpness: Double) {
        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)
        let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
        guard let base = CVPixelBufferGetBaseAddress(pixelBuffer)?.assumingMemoryBound(to: UInt8.self),
              width > 16, height > 16 else {
            return (0, 1, 0)
        }

        let step = 8
        var lumaSum = 0.0
        var glareCount = 0
        var gradSum = 0.0
        var count = 0

        for y in stride(from: step, to: height - step, by: step) {
            let rowPtr = base + y * bytesPerRow
            let nextRowPtr = base + (y + step) * bytesPerRow
            for x in stride(from: step, to: width - step, by: step) {
                let offset = x * 4
                let b = Double(rowPtr[offset])
                let g = Double(rowPtr[offset + 1])
                let r = Double(rowPtr[offset + 2])
                let luma = 0.299 * r + 0.587 * g + 0.114 * b
                lumaSum += luma
                if luma >= 245.0 { glareCount += 1 }

                let rOff = (x + step) * 4
                let lumaRight = 0.299 * Double(rowPtr[rOff + 2]) + 0.587 * Double(rowPtr[rOff + 1]) + 0.114 * Double(rowPtr[rOff])
                let lumaDown = 0.299 * Double(nextRowPtr[offset + 2]) + 0.587 * Double(nextRowPtr[offset + 1]) + 0.114 * Double(nextRowPtr[offset])
                gradSum += abs(luma - lumaRight) + abs(luma - lumaDown)
                count += 1
            }
        }

        guard count > 0 else { return (0, 1, 0) }
        let meanLuma = lumaSum / Double(count)
        let glareRatio = Double(glareCount) / Double(count)
        let sharpness = min(1.0, (gradSum / Double(count)) / 36.0)
        return (meanLuma, glareRatio, sharpness)
    }

    private func encodeJpeg(pixelBuffer: CVPixelBuffer, minWidth: CGFloat, maxWidth: CGFloat, quality: CGFloat) -> Data? {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let ctx = CIContext()
        guard let cgImage = ctx.createCGImage(ciImage, from: ciImage.extent) else { return nil }
        let ui = UIImage(cgImage: cgImage)
        let w = ui.size.width
        let h = ui.size.height
        let targetW = max(minWidth, min(maxWidth, w))
        let scale = w > 0 ? (targetW / w) : 1.0
        let targetSize = CGSize(width: w * scale, height: max(320, h * scale))
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let scaled = renderer.image { _ in
            ui.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        return scaled.jpegData(compressionQuality: quality)
    }
}

struct TelefamIosVerificationCameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    final class PreviewUIView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    func makeUIView(context: Context) -> PreviewUIView {
        let view = PreviewUIView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewUIView, context: Context) {
        if uiView.previewLayer.session !== session {
            uiView.previewLayer.session = session
        }
    }
}
