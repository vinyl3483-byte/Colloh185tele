import SwiftUI
import UIKit

// MARK: - Theme-Adaptive Verified Badge SVG Vector Views

struct TelefamIosVerifiedBadgeView: View {
    let tier: String
    var size: CGFloat = 18

    @Environment(\.colorScheme) private var colorScheme

    private var normTier: String {
        tier.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    var body: some View {
        if normTier == "business" || normTier == "government" || normTier == "blue" {
            Canvas { ctx, canvasSize in
                let w = canvasSize.width
                let h = canvasSize.height
                let cx = w / 2.0
                let cy = h / 2.0

                // Outer sunburst/seal path (12 lobes)
                var seal = Path()
                let lobes = 12
                let outerR = min(w, h) * 0.48
                let innerR = min(w, h) * 0.38
                for i in 0..<(lobes * 2) {
                    let angle = (Double(i) * .pi / Double(lobes)) - .pi / 2.0
                    let r = (i % 2 == 0) ? outerR : innerR
                    let pt = CGPoint(x: cx + r * cos(angle), y: cy + r * sin(angle))
                    if i == 0 {
                        seal.move(to: pt)
                    } else {
                        seal.addLine(to: pt)
                    }
                }
                seal.closeSubpath()

                let haloColor = colorScheme == .dark ? Color.white.opacity(0.32) : Color(red: 0.06, green: 0.09, blue: 0.16, opacity: 0.18)
                let fillColor: Color = {
                    switch normTier {
                    case "business":
                        return colorScheme == .dark ? Color(hex: 0xFBBF24) : Color(hex: 0xD99A18)
                    case "government":
                        return colorScheme == .dark ? Color(hex: 0xEF4444) : Color(hex: 0xD91B24)
                    default:
                        return colorScheme == .dark ? Color(hex: 0x38BDF8) : Color(hex: 0x1D9BF0)
                    }
                }()

                ctx.stroke(seal, with: .color(haloColor), lineWidth: max(1.0, w * 0.07))
                ctx.fill(seal, with: .color(fillColor))

                if normTier == "government" {
                    // White shield outline + inner check
                    var shield = Path()
                    shield.move(to: CGPoint(x: cx, y: cy - h * 0.21))
                    shield.addLine(to: CGPoint(x: cx + w * 0.17, y: cy - h * 0.13))
                    shield.addLine(to: CGPoint(x: cx + w * 0.15, y: cy + h * 0.06))
                    shield.addQuadCurve(
                        to: CGPoint(x: cx, y: cy + h * 0.22),
                        control: CGPoint(x: cx + w * 0.09, y: cy + h * 0.17)
                    )
                    shield.addQuadCurve(
                        to: CGPoint(x: cx - w * 0.15, y: cy + h * 0.06),
                        control: CGPoint(x: cx - w * 0.09, y: cy + h * 0.17)
                    )
                    shield.addLine(to: CGPoint(x: cx - w * 0.17, y: cy - h * 0.13))
                    shield.closeSubpath()
                    ctx.stroke(shield, with: .color(.white), style: StrokeStyle(lineWidth: max(1.2, w * 0.08), lineCap: .round, lineJoin: .round))

                    var chk = Path()
                    chk.move(to: CGPoint(x: cx - w * 0.08, y: cy + h * 0.01))
                    chk.addLine(to: CGPoint(x: cx - w * 0.01, y: cy + h * 0.08))
                    chk.addLine(to: CGPoint(x: cx + w * 0.09, y: cy - h * 0.05))
                    ctx.stroke(chk, with: .color(.white), style: StrokeStyle(lineWidth: max(1.1, w * 0.08), lineCap: .round, lineJoin: .round))
                } else {
                    // White crisp check mark
                    var chk = Path()
                    chk.move(to: CGPoint(x: cx - w * 0.17, y: cy + h * 0.02))
                    chk.addLine(to: CGPoint(x: cx - w * 0.04, y: cy + h * 0.15))
                    chk.addLine(to: CGPoint(x: cx + w * 0.19, y: cy - h * 0.12))
                    ctx.stroke(chk, with: .color(.white), style: StrokeStyle(lineWidth: max(1.4, w * 0.115), lineCap: .round, lineJoin: .round))
                }
            }
            .frame(width: size, height: size)
            .accessibilityLabel(normTier == "business" ? "Verified Business badge" : (normTier == "government" ? "Verified Government badge" : "Verified Blue badge"))
        }
    }
}

// MARK: - Benefit Vector SVG Icons (Zero Emojis)

private struct TelefamIosBenefitVectorIcon: View {
    let kind: String
    var tint: Color

    var body: some View {
        let sfSymbol: String = {
            switch kind {
            case "business_badge": return "crown.fill"
            case "custom_links": return "link"
            case "business_profile", "group_verification": return "person.3.fill"
            case "verified_contact", "blue_badge_check": return "checkmark.seal.fill"
            case "priority_support": return "megaphone.fill"
            case "featured_media": return "photo.on.rectangle.angled"
            case "enhanced_security", "gov_badge": return "checkmark.shield.fill"
            case "business_messages": return "text.bubble.fill"
            case "advanced_analytics": return "chart.bar.fill"
            case "payment_integration": return "creditcard.fill"
            case "exclusive_features": return "star.fill"
            case "new_stickers": return "face.smiling.inverse"
            case "hd_calls": return "video.fill"
            case "paid_messages": return "dollarsign.circle.fill"
            default: return "checkmark.seal.fill"
            }
        }()
        Image(systemName: sfSymbol)
            .font(.system(size: 16, weight: .semibold))
            .foregroundColor(tint)
    }
}

// MARK: - Host Container

struct TelefamIosVerificationHostView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            if let route = store.currentRoute {
                switch route {
                case .tiersPager:
                    TelefamIosVerificationTiersPagerView(store: store)
                case .trialAndPayment:
                    TelefamIosTrialAndPaymentView(store: store)
                case .selfieGuidance:
                    TelefamIosSelfieGuidanceView(store: store)
                case .selfieLiveness:
                    TelefamIosSelfieLivenessView(store: store)
                case .idGuidanceAndSelect:
                    TelefamIosIdGuidanceView(store: store)
                case .idLiveCapture:
                    TelefamIosIdLiveCaptureView(store: store)
                case .statusReview:
                    TelefamIosVerificationStatusView(store: store)
                }
            }
        }
        .sheet(isPresented: $store.showCheckoutWebView) {
            if let urlStr = store.activeCheckout?.authorizationUrl, !urlStr.isEmpty {
                NavigationStack {
                    TelefamIosCheckoutWebView(initialUrlString: urlStr) { returnedUrl in
                        store.showCheckoutWebView = false
                        Task { await store.handleCheckoutReturnUrl(returnedUrl) }
                    }
                    .navigationTitle("Secure Checkout")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button("Close") {
                                store.showCheckoutWebView = false
                            }
                        }
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Verify") {
                                store.showCheckoutWebView = false
                                Task { await store.verifyActivePayment() }
                            }
                            .fontWeight(.bold)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 1. 3-Tier Sideways Swipeable Pager + Interactive First-Open Tutorial

private struct TelefamIosVerificationTiersPagerView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedPageIndex: Int = 0
    @State private var expandedWhy: Bool = false
    @State private var tutorialPage: Int = 0

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }
    private var pageBg: Color {
        colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FB)
    }
    private var cardBg: Color {
        colorScheme == .dark ? Color(hex: 0x161A22) : .white
    }

    var body: some View {
        let tiers = TelefamIosVerificationTier.allCases
        let activeTier = tiers[selectedPageIndex]

        ZStack {
            VStack(spacing: 0) {
                // Crimson curved header matching design screenshots
                ZStack(alignment: .bottom) {
                    VStack(spacing: 0) {
                        HStack(spacing: 10) {
                            Button {
                                store.navigateBack()
                            } label: {
                                Image(systemName: "arrow.left")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(width: 48, height: 48)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Verification")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(.white)
                                Text(activeTier.headerTitle)
                                    .font(.system(size: 13))
                                    .foregroundColor(.white.opacity(0.92))
                            }
                            Spacer()
                            Button {
                                tutorialPage = 0
                                store.reopenTutorial()
                            } label: {
                                Image(systemName: "questionmark.circle")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 44, height: 44)
                            }
                        }
                        .padding(.horizontal, 8)
                        .padding(.top, 6)
                        .padding(.bottom, 16)
                    }
                    .frame(maxWidth: .infinity)
                    .background(accentCrimson)
                }

                TabView(selection: $selectedPageIndex) {
                    ForEach(Array(tiers.enumerated()), id: \.element.id) { idx, tier in
                        tierPageContent(tier: tier)
                            .tag(idx)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .onChange(of: selectedPageIndex) { newIdx in
                    expandedWhy = false
                    store.selectTier(tiers[newIdx])
                }

                // Bottom sticky pager dots + Apply/Subscribe button
                VStack(spacing: 10) {
                    HStack(spacing: 8) {
                        ForEach(0..<tiers.count, id: \.self) { idx in
                            Capsule()
                                .fill(selectedPageIndex == idx ? accentCrimson : TelefamPalette.border(colorScheme))
                                .frame(width: selectedPageIndex == idx ? 22 : 8, height: 8)
                                .animation(.easeInOut(duration: 0.2), value: selectedPageIndex)
                        }
                    }

                    Button {
                        store.openApplyOrSubscribe(for: activeTier)
                    } label: {
                        HStack(spacing: 8) {
                            Text(activeTier.ctaLabel)
                                .font(.system(size: 16, weight: .bold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: 640)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(accentCrimson)
                        .clipShape(Capsule())
                    }
                    .padding(.horizontal, 18)

                    Text(activeTier.footerTagline)
                        .font(.system(size: 12))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .padding(.bottom, 8)
                }
                .padding(.top, 8)
                .background(cardBg.shadow(color: Color.black.opacity(0.06), radius: 8, y: -2))
            }
            .background(pageBg.ignoresSafeArea())

            if store.showFirstOpenTutorial {
                firstOpenTutorialOverlay
            }
        }
        .onAppear {
            if let idx = tiers.firstIndex(of: store.selectedTier) {
                selectedPageIndex = idx
            }
        }
    }

    private func tierPageContent(tier: TelefamIosVerificationTier) -> some View {
        let monthlyFormatted = TelefamIosVerificationCatalog.formatMoney(baseKes: tier.monthlyBaseKes, countryIso: store.countryIso)
        let annualDiscounted = TelefamIosVerificationCatalog.formatMoney(baseKes: tier.annualDiscountedBaseKes, countryIso: store.countryIso)
        let annualOriginal = TelefamIosVerificationCatalog.formatMoney(baseKes: tier.annualUndiscountedBaseKes, countryIso: store.countryIso)

        let displayName: String = {
            if tier == .blue && !store.profileFullName.isEmpty { return store.profileFullName }
            return tier.sampleDisplayName
        }()
        let handle: String = {
            if tier == .blue && !store.profileUsername.isEmpty { return "@\(store.profileUsername)" }
            return tier.sampleHandle
        }()

        return ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                // Preview profile header
                HStack(spacing: 14) {
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(tier == .business ? Color(hex: 0x1E222B) : (tier == .government ? Color(hex: 0xF1F3F7) : accentCrimson.opacity(0.15)))
                            .frame(width: 68, height: 68)
                            .overlay(
                                TelefamIosBenefitVectorIcon(
                                    kind: tier == .business ? "business_badge" : (tier == .government ? "gov_badge" : "blue_badge_check"),
                                    tint: tier == .business ? Color(hex: 0xFBBF24) : (tier == .government ? Color(hex: 0x1F2937) : Color(hex: 0x1D9BF0))
                                )
                                .scaleEffect(1.35)
                            )
                            .overlay(Circle().stroke(tier == .business ? Color(hex: 0xD99A18) : accentCrimson, lineWidth: 2))

                        TelefamIosVerifiedBadgeView(tier: tier.rawValue, size: 22)
                            .offset(x: 2, y: 2)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 6) {
                            Text(displayName)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .lineLimit(1)
                            TelefamIosVerifiedBadgeView(tier: tier.rawValue, size: 18)
                        }
                        Text(handle)
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        Text(tier.sampleBio)
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                    Spacer()
                }
                .padding(14)
                .background(cardBg)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                // Pricing Card (Monthly + Annual 20% OFF Toggle matching screenshots)
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 6) {
                            Image(systemName: tier == .business ? "crown.fill" : (tier == .government ? "checkmark.shield.fill" : "checkmark.seal.fill"))
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(tier == .business ? Color(hex: 0xD99A18) : (tier == .government ? accentCrimson : Color(hex: 0x1D9BF0)))
                            Text("\(monthlyFormatted) / month")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        }
                        Text(store.isAnnualBilling ? "Billed annually" : "Billed monthly")
                            .font(.system(size: 12))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        if store.status.trialEligible {
                            Text("1 week free offer available")
                                .font(.system(size: 11.5, weight: .semibold))
                                .foregroundColor(accentCrimson)
                        }
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        HStack(spacing: 6) {
                            Toggle(isOn: $store.isAnnualBilling) {
                                Text("Annual")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white)
                            }
                            .labelsHidden()
                            .tint(accentCrimson)

                            Text("Annual")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(store.isAnnualBilling ? accentCrimson : TelefamPalette.textPrimary(colorScheme))

                            Text("20% OFF")
                                .font(.system(size: 10.5, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(accentCrimson)
                                .clipShape(Capsule())
                        }
                        Text("\(annualDiscounted) / year")
                            .font(.system(size: 14.5, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text(annualOriginal)
                            .font(.system(size: 12))
                            .strikethrough()
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                }
                .padding(14)
                .background(colorScheme == .dark ? Color(hex: 0x26161B) : Color(hex: 0xFFF5F6))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                Text(tier == .blue ? "Verification Benefits" : "\(tier.shortLabel) Verification Benefits")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                if tier == .blue {
                    VStack(spacing: 10) {
                        ForEach(tier.benefits) { b in
                            HStack(spacing: 12) {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(accentCrimson.opacity(0.12))
                                    .frame(width: 38, height: 38)
                                    .overlay(TelefamIosBenefitVectorIcon(kind: b.iconKind, tint: accentCrimson))

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(b.title)
                                        .font(.system(size: 14.5, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text(b.subtitle)
                                        .font(.system(size: 12.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(accentCrimson)
                            }
                            .padding(12)
                            .background(cardBg)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                    }
                } else {
                    let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
                    LazyVGrid(columns: cols, alignment: .leading, spacing: 12) {
                        ForEach(tier.benefits) { b in
                            HStack(alignment: .top, spacing: 10) {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(b.iconKind == "business_badge" ? Color(hex: 0xFEF3C7) : accentCrimson.opacity(0.12))
                                    .frame(width: 34, height: 34)
                                    .overlay(
                                        TelefamIosBenefitVectorIcon(
                                            kind: b.iconKind,
                                            tint: b.iconKind == "business_badge" ? Color(hex: 0xD97706) : accentCrimson
                                        )
                                    )
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(b.title)
                                        .font(.system(size: 13, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text(b.subtitle)
                                        .font(.system(size: 11.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(12)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }

                // Expandable "Why Go Business / Government?" card
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { expandedWhy.toggle() }
                } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 10) {
                            Image(systemName: "info.circle.fill")
                                .font(.system(size: 17))
                                .foregroundColor(accentCrimson)
                            Text(tier.whyTitle)
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Spacer()
                            Text(expandedWhy ? "Less" : "Read More")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(accentCrimson)
                        }
                        Text(tier.whyBody)
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .multilineTextAlignment(.leading)
                        if expandedWhy {
                            Text(tier.whyExpandedDetails)
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .padding(.top, 4)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .padding(14)
                    .background(colorScheme == .dark ? Color(hex: 0x1D202A) : Color(hex: 0xFFF5F6))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .frame(maxWidth: 680)
            .frame(maxWidth: .infinity)
        }
    }

    private var firstOpenTutorialOverlay: some View {
        let steps = TelefamIosVerificationCatalog.tutorialSteps
        let safeIdx = min(tutorialPage, steps.count - 1)

        return ZStack {
            Color.black.opacity(0.62).ignoresSafeArea()
            VStack(spacing: 16) {
                HStack {
                    Text("Get Verified Guide")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(accentCrimson)
                    Spacer()
                    Button("Skip") { store.dismissTutorial() }
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                }

                TelefamIosVerifiedBadgeView(
                    tier: safeIdx == 0 ? "business" : (safeIdx == 1 ? "government" : "blue"),
                    size: 52
                )

                Text(steps[safeIdx].title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    .multilineTextAlignment(.center)

                Text(steps[safeIdx].body)
                    .font(.system(size: 14))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .multilineTextAlignment(.center)

                HStack(spacing: 6) {
                    ForEach(0..<steps.count, id: \.self) { i in
                        Capsule()
                            .fill(i == safeIdx ? accentCrimson : TelefamPalette.border(colorScheme))
                            .frame(width: i == safeIdx ? 18 : 7, height: 7)
                    }
                }

                Button {
                    if safeIdx + 1 < steps.count {
                        tutorialPage = safeIdx + 1
                    } else {
                        store.dismissTutorial()
                    }
                } label: {
                    Text(safeIdx + 1 < steps.count ? "Next" : "Get Started")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(accentCrimson)
                        .clipShape(Capsule())
                }
            }
            .padding(22)
            .frame(maxWidth: 420)
            .background(cardBg)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(24)
        }
    }
}

// MARK: - 2. 1-Week Free Trial + How It Works + Paystack/PayPal Checkout

private struct TelefamIosTrialAndPaymentView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme

    private var accentCrimson: Color { colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson }
    private var cardBg: Color { colorScheme == .dark ? Color(hex: 0x161A22) : .white }
    private var pageBg: Color { colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FB) }

    var body: some View {
        let tier = store.selectedTier
        let basePrice = store.isAnnualBilling ? tier.annualDiscountedBaseKes : tier.monthlyBaseKes
        let priceStr = TelefamIosVerificationCatalog.formatMoney(baseKes: basePrice, countryIso: store.countryIso)
        let cycleLabel = store.isAnnualBilling ? "per year (20% OFF)" : "per month"

        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button { store.navigateBack() } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(width: 48, height: 48)
                }
                Text("Subscribe & Verify")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
            }
            .padding(.horizontal, 8)
            .background(cardBg)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.red)
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.red.opacity(0.10))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    // Selected plan summary
                    HStack(spacing: 12) {
                        TelefamIosVerifiedBadgeView(tier: tier.rawValue, size: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(tier.shortLabel) Verification")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text("\(priceStr) \(cycleLabel)")
                                .font(.system(size: 13.5, weight: .semibold))
                                .foregroundColor(accentCrimson)
                        }
                        Spacer()
                        Toggle("Annual", isOn: $store.isAnnualBilling)
                            .labelsHidden()
                            .tint(accentCrimson)
                    }
                    .padding(14)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    // First week free offer card
                    if store.status.trialEligible {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("First Week Free Offer")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(accentCrimson)
                            Text("Start your 7-day free trial immediately, complete live selfie and document checks, and unlock your badge after review.")
                                .font(.system(size: 13))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            Button {
                                Task { await store.activateFreeTrial() }
                            } label: {
                                Text(store.isSubmitting ? "Activating..." : "Unlock 1 Week Free")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity, minHeight: 48)
                                    .background(accentCrimson)
                                    .clipShape(Capsule())
                            }
                            .disabled(store.isSubmitting)
                        }
                        .padding(16)
                        .background(colorScheme == .dark ? Color(hex: 0x26161B) : Color(hex: 0xFFF2F4))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }

                    // How Verification Works Guidance
                    VStack(alignment: .leading, spacing: 10) {
                        Text("How Verification Works")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        stepLine("1", "Activate 1-week free trial or complete Paystack / PayPal checkout.")
                        stepLine("2", "Complete guided live selfie (Left, Right, Up, Left, Right) — auto-captures at 100%.")
                        stepLine("3", "Scan your official ID card for encrypted OCR matching. Documents are securely purged after verification; review takes 3 working days.")
                    }
                    .padding(14)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    // Paystack / PayPal provider choice
                    Text("Or Subscribe Now")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                    providerCard(provider: .paystack, title: "Paystack", subtitle: "Pay securely with M-Pesa, cards & bank transfers")
                    providerCard(provider: .paypal, title: "PayPal", subtitle: "Pay with your PayPal account or card")

                    Button {
                        Task { await store.proceedToSubscribePayment() }
                    } label: {
                        Text(store.isSubmitting ? "Processing..." : "Proceed to Pay (\(priceStr))")
                            .font(.system(size: 15.5, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                    .disabled(store.isSubmitting)

                    if store.activeCheckout != nil {
                        Button {
                            Task { await store.verifyActivePayment() }
                        } label: {
                            Text("Verify Payment Status")
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
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
        }
        .background(pageBg.ignoresSafeArea())
    }

    private func stepLine(_ num: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(num)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(accentCrimson)
                .clipShape(Circle())
            Text(text)
                .font(.system(size: 13))
                .foregroundColor(TelefamPalette.textMuted(colorScheme))
        }
    }

    private func providerCard(provider: TelefamIosStarPaymentProvider, title: String, subtitle: String) -> some View {
        let selected = store.selectedProvider == provider
        return Button { store.selectedProvider = provider } label: {
            HStack(spacing: 12) {
                Image(systemName: provider == .paystack ? "creditcard.and.123" : "globe")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(accentCrimson)
                    .frame(width: 40, height: 40)
                    .background(accentCrimson.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                }
                Spacer()
                Circle()
                    .stroke(selected ? accentCrimson : TelefamPalette.border(colorScheme), lineWidth: 2)
                    .frame(width: 22, height: 22)
                    .overlay(Circle().fill(selected ? accentCrimson : Color.clear).frame(width: 12, height: 12))
            }
            .padding(14)
            .background(cardBg)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? accentCrimson : TelefamPalette.border(colorScheme), lineWidth: selected ? 1.8 : 1))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - 3. Live Selfie Guidance & Liveness Camera

private struct TelefamIosSelfieGuidanceView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme

    private var accentCrimson: Color { colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson }
    private var cardBg: Color { colorScheme == .dark ? Color(hex: 0x161A22) : .white }
    private var pageBg: Color { colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FB) }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { store.navigateBack() } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(width: 48, height: 48)
                }
                Text("Live Selfie Guidance")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
            }
            .padding(.horizontal, 8)
            .background(cardBg)

            ScrollView {
                VStack(spacing: 16) {
                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 13))
                            .foregroundColor(.red)
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.red.opacity(0.10))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    // Vector face guidance illustration
                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(accentCrimson.opacity(0.08))
                            .frame(height: 170)
                        HStack(spacing: 22) {
                            Image(systemName: "arrow.left.circle.fill")
                                .font(.system(size: 28))
                                .foregroundColor(accentCrimson)
                            ZStack {
                                Ellipse()
                                    .stroke(accentCrimson, style: StrokeStyle(lineWidth: 3, dash: [6, 5]))
                                    .frame(width: 84, height: 112)
                                Image(systemName: "person.fill")
                                    .font(.system(size: 44))
                                    .foregroundColor(accentCrimson)
                            }
                            Image(systemName: "arrow.right.circle.fill")
                                .font(.system(size: 28))
                                .foregroundColor(accentCrimson)
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Before You Start")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        bullet("Face evenly lit; avoid hats, masks, or dark sunglasses.")
                        bullet("Follow server prompts: Turn Left, Right, Up, Left, Right, then Center.")
                        bullet("Camera auto-captures cleanly as soon as liveness score reaches 100%.")
                        bullet("Screen recording and mirroring are strictly blocked.")
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Button {
                        Task { await store.startLiveSelfieSession() }
                    } label: {
                        Text(store.isSubmitting ? "Preparing Camera..." : "Start Live Selfie")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                    .disabled(store.isSubmitting)
                }
                .padding(18)
                .frame(maxWidth: 620)
                .frame(maxWidth: .infinity)
            }
        }
        .background(pageBg.ignoresSafeArea())
    }

    private func bullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 15))
                .foregroundColor(accentCrimson)
                .padding(.top, 2)
            Text(text)
                .font(.system(size: 13.5))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
        }
    }
}

private struct TelefamIosSelfieLivenessView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @StateObject private var cam = TelefamIosVerificationCameraCoordinator()
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let accent = colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
        let steps = store.livenessSession?.requiredSteps ?? ["left", "right", "up", "left", "right", "center"]

        ZStack {
            Color.black.ignoresSafeArea()

            if cam.permissionGranted {
                TelefamIosVerificationCameraPreview(session: cam.session)
                    .ignoresSafeArea()
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 40))
                        .foregroundColor(.white)
                    Text(cam.permissionDenied ? "Camera permission denied. Enable Camera access in Settings." : "Requesting camera access...")
                        .font(.system(size: 14))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
                .padding(24)
            }

            VStack(spacing: 14) {
                HStack {
                    Button {
                        cam.stop()
                        store.navigateBack()
                    } label: {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                    Spacer()
                    Text("\(store.livenessScorePct)%")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(store.livenessScorePct >= 100 ? Color.green : accent)
                        .clipShape(Capsule())
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)

                Spacer()

                ZStack {
                    Ellipse()
                        .stroke(Color.white.opacity(0.3), lineWidth: 5)
                        .frame(width: 240, height: 310)
                    Ellipse()
                        .trim(from: 0, to: CGFloat(store.livenessScorePct) / 100.0)
                        .stroke(store.livenessScorePct >= 100 ? Color.green : accent, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .frame(width: 240, height: 310)
                }

                Spacer()

                VStack(spacing: 8) {
                    HStack(spacing: 12) {
                        statusPill(title: store.faceVisible ? "Face Visible" : "Face Not Visible", ok: store.faceVisible)
                        statusPill(title: store.lightingGood ? "Lighting Good" : "Low Lighting", ok: store.lightingGood)
                    }
                    Text(store.livenessPrompt)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    if let err = store.errorMessage {
                        Text(err)
                            .font(.system(size: 12.5))
                            .foregroundColor(.red)
                    }
                }
                .padding(16)
                .frame(maxWidth: 520)
                .background(Color.black.opacity(0.75))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .padding(.horizontal, 16)
                .padding(.bottom, 22)
            }

            if store.screenCaptureBlocked {
                VStack(spacing: 12) {
                    Image(systemName: "eye.slash.fill")
                        .font(.system(size: 44))
                        .foregroundColor(.red)
                    Text("Screen Recording Detected")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                    Text("Turn off screen recording or AirPlay mirroring to protect your identity verification.")
                        .font(.system(size: 13.5))
                        .foregroundColor(.white.opacity(0.85))
                        .multilineTextAlignment(.center)
                }
                .padding(24)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black.opacity(0.94))
            }
        }
        .onAppear {
            cam.onSelfieProgress = { idx, pct, visible, light, prompt in
                store.updateLiveSelfieProgress(stepIdx: idx, pct: pct, visible: visible, lighting: light, prompt: prompt)
            }
            cam.onSelfieAutoCaptured = { jpegData, completed in
                cam.stop()
                Task { await store.submitAutoCapturedLiveness(jpegData: jpegData, completedSteps: completed) }
            }
            cam.start(mode: .selfie(requiredSteps: steps))
        }
        .onDisappear { cam.stop() }
    }

    private func statusPill(title: String, ok: Bool) -> some View {
        Text(title)
            .font(.system(size: 11.5, weight: .bold))
            .foregroundColor(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(ok ? Color.green.opacity(0.75) : Color.orange.opacity(0.8))
            .clipShape(Capsule())
    }
}

// MARK: - 4. ID Guidance (Screenshot 4) + ID Type Selector

private struct TelefamIosIdGuidanceView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme

    private var accentCrimson: Color { colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson }
    private var cardBg: Color { colorScheme == .dark ? Color(hex: 0x161A22) : .white }
    private var pageBg: Color { colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FB) }

    var body: some View {
        let allowed = store.selectedTier.allowedDocTypes

        VStack(spacing: 0) {
            HStack {
                Button { store.navigateBack() } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(width: 48, height: 48)
                }
                Text("Select & Prepare ID")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
            }
            .padding(.horizontal, 8)
            .background(cardBg)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Choose Document Type")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                    ForEach(allowed) { doc in
                        let selected = store.selectedDocType == doc
                        Button { store.selectedDocType = doc } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "person.text.rectangle.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(accentCrimson)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(doc.label)
                                        .font(.system(size: 14.5, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text(doc.subtitle)
                                        .font(.system(size: 12))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                                Spacer()
                                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                                    .font(.system(size: 20))
                                    .foregroundColor(selected ? accentCrimson : TelefamPalette.border(colorScheme))
                            }
                            .padding(12)
                            .background(cardBg)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? accentCrimson : TelefamPalette.border(colorScheme), lineWidth: selected ? 1.8 : 1))
                        }
                        .buttonStyle(.plain)
                    }

                    // Screenshot 4: Correct ID Card + 4 Rules
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 6) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Correct ID")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        }
                        ForEach(TelefamIosVerificationCatalog.correctIdRules, id: \.self) { rule in
                            Text("\u{2022} \(rule)")
                                .font(.system(size: 12.5))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    // Screenshot 4: 8 Incorrect Examples Grid
                    Text("Incorrect ID Examples")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                    let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
                    LazyVGrid(columns: cols, spacing: 10) {
                        ForEach(TelefamIosVerificationCatalog.incorrectExamples) { ex in
                            HStack(spacing: 8) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.red)
                                Text(ex.title)
                                    .font(.system(size: 12.5, weight: .semibold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Spacer()
                            }
                            .padding(10)
                            .background(cardBg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    // Explicit Secure Deletion Notice
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "lock.shield.fill")
                            .font(.system(size: 18))
                            .foregroundColor(accentCrimson)
                        Text(TelefamIosVerificationCatalog.secureDeletionNotice)
                            .font(.system(size: 12))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                    .padding(12)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button {
                        Task { await store.startLiveIdCapture() }
                    } label: {
                        Text(store.isSubmitting ? "Starting Scanner..." : "Start Live Document Scan")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                    .disabled(store.isSubmitting)
                }
                .padding(16)
                .frame(maxWidth: 680)
                .frame(maxWidth: .infinity)
            }
        }
        .background(pageBg.ignoresSafeArea())
    }
}

// MARK: - 5. Live Document OCR Auto-Capture

private struct TelefamIosIdLiveCaptureView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @StateObject private var cam = TelefamIosVerificationCameraCoordinator()
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let accent = colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson

        ZStack {
            Color.black.ignoresSafeArea()

            if cam.permissionGranted {
                TelefamIosVerificationCameraPreview(session: cam.session)
                    .ignoresSafeArea()
            }

            VStack {
                HStack {
                    Button {
                        cam.stop()
                        store.navigateBack()
                    } label: {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                    Spacer()
                    Text("\(store.docReadinessPct)% Ready")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(store.docReadinessPct >= 100 ? Color.green : accent)
                        .clipShape(Capsule())
                }
                .padding(16)

                Spacer()

                RoundedRectangle(cornerRadius: 18)
                    .stroke(store.docReadinessPct >= 100 ? Color.green : .white, lineWidth: 3)
                    .frame(maxWidth: 420)
                    .frame(height: 230)
                    .padding(.horizontal, 24)

                Spacer()

                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        checkChip("4 Corners", store.docCornersReady)
                        checkChip("Sharp", store.docSharpReady)
                        checkChip("Lit", store.docLightingReady)
                        checkChip("No Glare", store.docGlareFreeReady)
                    }
                    if !store.docDetectedOcrPreview.isEmpty {
                        Text("OCR Detected: \(store.docDetectedOcrPreview.prefix(68))")
                            .font(.system(size: 11.5))
                            .foregroundColor(.white.opacity(0.9))
                            .lineLimit(2)
                    }
                    if let err = store.errorMessage {
                        Text(err).font(.system(size: 12)).foregroundColor(.red)
                    }
                }
                .padding(14)
                .background(Color.black.opacity(0.75))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(16)
            }
        }
        .onAppear {
            cam.onDocProgress = { c, s, l, g, ocr, pct in
                store.updateLiveDocReadiness(corners: c, sharp: s, lighting: l, glareFree: g, ocrText: ocr, pct: pct)
            }
            cam.onDocAutoCaptured = { jpeg, ocr, sharp, luma, glare, corners in
                cam.stop()
                Task {
                    await store.submitAutoCapturedDocument(
                        jpegData: jpeg,
                        ocrText: ocr,
                        sharpnessScore: sharp,
                        luminanceMean: luma,
                        glareRatio: glare,
                        cornersDetected: corners
                    )
                }
            }
            cam.start(mode: .document)
        }
        .onDisappear { cam.stop() }
    }

    private func checkChip(_ title: String, _ ok: Bool) -> some View {
        Text(title)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(ok ? Color.green.opacity(0.8) : Color.white.opacity(0.2))
            .clipShape(Capsule())
    }
}

// MARK: - 6. Under Review (3 Working Days) & Unlocked Badge Status

private struct TelefamIosVerificationStatusView: View {
    @ObservedObject var store: TelefamIosVerificationStore
    @Environment(\.colorScheme) private var colorScheme

    private var accentCrimson: Color { colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson }
    private var cardBg: Color { colorScheme == .dark ? Color(hex: 0x161A22) : .white }
    private var pageBg: Color { colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(hex: 0xF7F8FB) }

    var body: some View {
        let st = store.status
        let rev = st.reviewStatus

        VStack(spacing: 0) {
            HStack {
                Button { store.dismissFlow() } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(width: 48, height: 48)
                }
                Text("Verification Status")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
                Button("Refresh") {
                    Task { await store.refreshStatus(silent: false) }
                }
                .font(.system(size: 13.5, weight: .bold))
                .foregroundColor(accentCrimson)
                .padding(.trailing, 12)
            }
            .background(cardBg)

            ScrollView {
                VStack(spacing: 16) {
                    if rev == "verified" && st.verifiedTier != "none" {
                        TelefamIosVerifiedBadgeView(tier: st.verifiedTier, size: 64)
                        Text("You Are Verified")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text("Your \(TelefamIosVerificationTier.fromWire(st.verifiedTier).shortLabel) badge is active across your profile, posts, and groups.")
                            .font(.system(size: 14))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .multilineTextAlignment(.center)
                    } else if rev == "action_required" {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text("Action Required")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text(st.rejectionReason.isEmpty ? "Please resubmit your live selfie and ID card." : st.rejectionReason)
                            .font(.system(size: 13.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .multilineTextAlignment(.center)
                        Button {
                            store.routeStack = [.selfieGuidance]
                        } label: {
                            Text("Retry Verification")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity, minHeight: 48)
                                .background(accentCrimson)
                                .clipShape(Capsule())
                        }
                    } else {
                        Image(systemName: "clock.badge.checkmark.fill")
                            .font(.system(size: 52))
                            .foregroundColor(accentCrimson)
                        Text("Application Under Review")
                            .font(.system(size: 21, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text("It always takes 3 working days to review. You will be notified once your badge is unlocked.")
                            .font(.system(size: 14))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .multilineTextAlignment(.center)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Security & Data Protection")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text(TelefamIosVerificationCatalog.secureDeletionNotice)
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(cardBg)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    Button { store.dismissFlow() } label: {
                        Text("Done")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                }
                .padding(20)
                .frame(maxWidth: 620)
                .frame(maxWidth: .infinity)
            }
        }
        .background(pageBg.ignoresSafeArea())
    }
}
