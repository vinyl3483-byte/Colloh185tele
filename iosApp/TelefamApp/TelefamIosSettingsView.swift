import CoreImage.CIFilterBuiltins
import CryptoKit
import PhotosUI
import SwiftUI
import UIKit

private enum SettingsBadgeSvgKind {
    case activityPulse
    case shareCard
    case bell
    case heart
    case bookmarkLink
    case cloudSync
    case lock
    case blockMinus
    case flag
    case shieldCheck
    case helpQuestion
    case documentLines
    case infoCircle
}

private struct SettingsHubRowItem: Identifiable {
    let id: String
    let section: String
    let title: String
    let subtitle: String
    let iconKind: SettingsBadgeSvgKind
    let destination: TelefamSettingsDestination
    let keywords: String
}

struct TelefamIosSettingsView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var smartLinksCoordinator: TelefamIosSmartLinksCoordinator

    @ObservedObject var sessionStore: TelefamIosSessionStore
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    @Binding var themeMode: TelefamThemeMode
    let displayName: String
    let username: String
    let avatarUrl: String
    let authorizedAvatarUrl: String
    let profileShareUrl: String

    @State private var navPath: [TelefamSettingsDestination] = []
    @State private var wideDetailDestination: TelefamSettingsDestination = .privacy
    @State private var searchQuery: String = ""
    @State private var showLogoutDialog: Bool = false
    @State private var logoutAllDevices: Bool = false
    @State private var isLoggingOut: Bool = false

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }

    private var pageBg: Color {
        colorScheme == .dark ? TelefamPalette.background(colorScheme) : Color(red: 0.953, green: 0.961, blue: 0.973)
    }

    private var allRows: [SettingsHubRowItem] {
        let syncSub = settingsStore.pendingOutboxCount > 0
            ? "\(settingsStore.pendingOutboxCount) queued · Tap to sync"
            : "Auto-sync, Wi-Fi only, cache"
        return [
            SettingsHubRowItem(
                id: "account_privacy",
                section: "Account",
                title: "Privacy",
                subtitle: "Private account, blocked accounts, activity",
                iconKind: .activityPulse,
                destination: .privacy,
                keywords: "private account discovery activity comment mention"
            ),
            SettingsHubRowItem(
                id: "share_profile",
                section: "Account",
                title: "Share profile",
                subtitle: "QR code, link, copy profile URL",
                iconKind: .shareCard,
                destination: .shareProfile,
                keywords: "qr code link url invite"
            ),
            SettingsHubRowItem(
                id: "notifications",
                section: "Content & activity",
                title: "Notifications",
                subtitle: "Push, email, SMS",
                iconKind: .bell,
                destination: .notifications,
                keywords: "push email sms quiet hours pause alerts messages comments"
            ),
            SettingsHubRowItem(
                id: "content_prefs",
                section: "Content & activity",
                title: "Content preferences",
                subtitle: "Suggested content, language, video quality",
                iconKind: .heart,
                destination: .contentPreferences,
                keywords: "autoplay captions reduced motion data saver topics language"
            ),
            SettingsHubRowItem(
                id: "saved_links",
                section: "Content & activity",
                title: "Saved links",
                subtitle: "Bookmarks from chats, feeds & browser",
                iconKind: .bookmarkLink,
                destination: .savedLinks,
                keywords: "saved links bookmarks url browser"
            ),
            SettingsHubRowItem(
                id: "offline_sync",
                section: "Content & activity",
                title: "Offline sync",
                subtitle: syncSub,
                iconKind: .cloudSync,
                destination: .offlineSync,
                keywords: "offline sync queue cache server wifi background upload"
            ),
            SettingsHubRowItem(
                id: "safety_privacy",
                section: "Privacy and safety",
                title: "Privacy",
                subtitle: "Who can see your videos, likes, comments, following",
                iconKind: .lock,
                destination: .privacy,
                keywords: "likes following visibility cookies data sharing"
            ),
        SettingsHubRowItem(
            id: "blocked_accounts",
            section: "Privacy and safety",
            title: "Blocked accounts",
            subtitle: "Manage blocked users",
            iconKind: .blockMinus,
            destination: .blockedAccounts,
            keywords: "unblock muted mute restrict"
        ),
        SettingsHubRowItem(
            id: "report_problem",
            section: "Privacy and safety",
            title: "Report a problem",
            subtitle: "Send feedback, report abuse",
            iconKind: .flag,
            destination: .reportProblem,
            keywords: "bug feedback screenshot ticket reference"
        ),
        SettingsHubRowItem(
            id: "safety_center",
            section: "Privacy and safety",
            title: "Safety Center",
            subtitle: "Tips, tools, resources",
            iconKind: .shieldCheck,
            destination: .safetyCenter,
            keywords: "parents guardians emergency helpline abuse reporting"
        ),
        SettingsHubRowItem(
            id: "help_support",
            section: "Support & about",
            title: "Help & support",
            subtitle: "FAQs, contact us, report issue",
            iconKind: .helpQuestion,
            destination: .helpSupport,
            keywords: "faq tickets system status outage"
        ),
        SettingsHubRowItem(
            id: "terms_policies",
            section: "Support & about",
            title: "Terms and policies",
            subtitle: "Terms of Service, Privacy Policy, Community Guidelines",
            iconKind: .documentLines,
            destination: .termsPolicies,
            keywords: "legal cookies licenses privacy policy guidelines"
        ),
        SettingsHubRowItem(
            id: "about",
            section: "Support & about",
            title: "About",
            subtitle: "Version, copyright, open source",
            iconKind: .infoCircle,
            destination: .about,
            keywords: "version build update check open source license company"
        )
        ]
    }

    var body: some View {
        GeometryReader { geo in
            let isWide = geo.size.width >= 820

            ZStack(alignment: .bottom) {
                if isWide {
                    VStack(spacing: 0) {
                        curvedHeader(title: "Settings and privacy", showBack: true) {
                            dismiss()
                        }

                        HStack(spacing: 0) {
                            hubBody(selectedDestination: wideDetailDestination) { dest in
                                wideDetailDestination = dest
                            }
                            .frame(width: 390)

                            Divider().background(TelefamPalette.border(colorScheme))

                            VStack(alignment: .leading, spacing: 0) {
                                Text(wideDetailDestination.title)
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    .padding(.horizontal, 22)
                                    .padding(.vertical, 16)

                                Divider().background(TelefamPalette.border(colorScheme))

                                detailHost(for: wideDetailDestination)
                                    .frame(maxWidth: 740, maxHeight: .infinity)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .background(pageBg.ignoresSafeArea())
                } else {
                    NavigationStack(path: $navPath) {
                        VStack(spacing: 0) {
                            curvedHeader(title: "Settings and privacy", showBack: true) {
                                dismiss()
                            }
                            hubBody(selectedDestination: nil) { dest in
                                navPath.append(dest)
                            }
                        }
                        .background(pageBg.ignoresSafeArea())
                        .toolbar(.hidden, for: .navigationBar)
                        .navigationDestination(for: TelefamSettingsDestination.self) { dest in
                            VStack(spacing: 0) {
                                curvedHeader(title: dest.title, showBack: true) {
                                    if !navPath.isEmpty { navPath.removeLast() }
                                }
                                detailHost(for: dest)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            }
                            .background(pageBg.ignoresSafeArea())
                            .toolbar(.hidden, for: .navigationBar)
                            .background(EnableInteractivePopGestureBridge())
                        }
                    }
                }

                if let toast = settingsStore.toastMessage {
                    Text(toast)
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.84))
                        .clipShape(Capsule())
                        .padding(.bottom, 26)
                }
            }
        }
        .task {
            await settingsStore.refreshAll()
        }
        .sheet(isPresented: $showLogoutDialog) {
            logoutConfirmSheet
                .presentationDetents([.height(290)])
                .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .telefamSmartOverlayHost(
            preferredColorScheme: themeMode.preferredColorScheme,
            showLocalToast: true
        )
        .preferredColorScheme(themeMode.preferredColorScheme)
    }

    private func curvedHeader(title: String, showBack: Bool, onBack: @escaping () -> Void) -> some View {
        ZStack {
            HStack {
                if showBack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 48, height: 48)
                    }
                }
                Spacer()
            }
            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
                .lineLimit(1)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
        .background(
            UnevenRoundedBottomShape(radius: 28)
                .fill(TelefamPalette.crimson)
                .ignoresSafeArea(edges: .top)
        )
    }

    private func hubBody(selectedDestination: TelefamSettingsDestination?, onSelect: @escaping (TelefamSettingsDestination) -> Void) -> some View {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let filtered = allRows.filter {
            q.isEmpty ||
            $0.title.lowercased().contains(q) ||
            $0.subtitle.lowercased().contains(q) ||
            $0.section.lowercased().contains(q) ||
            $0.keywords.contains(q)
        }
        let sectionOrder = ["Account", "Content & activity", "Privacy and safety", "Support & about"]

        return ScrollView {
            VStack(spacing: 14) {
                // Identity card (user avatar + name + theme toggle)
                HStack(spacing: 12) {
                    if !authorizedAvatarUrl.isEmpty {
                        TelefamIosRemoteImage(
                            urlString: authorizedAvatarUrl,
                            bearerToken: sessionStore.sessionToken,
                            deviceFingerprint: sessionStore.deviceFingerprint,
                            fallbackInitials: initials(displayName)
                        )
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                    } else {
                        Circle()
                            .fill(TelefamPalette.blush(colorScheme))
                            .frame(width: 48, height: 48)
                            .overlay(
                                Text(initials(displayName))
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(accentCrimson)
                            )
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(displayName.isEmpty ? "Telefam" : displayName)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            .lineLimit(1)
                        Text("@\(username.isEmpty ? "user" : username)")
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .lineLimit(1)
                    }

                    Spacer()

                    Button {
                        themeMode = themeMode.next
                    } label: {
                        HStack(spacing: 5) {
                            Image(systemName: themeMode == .dark ? "moon.fill" : (themeMode == .light ? "sun.max.fill" : "circle.lefthalf.filled"))
                                .font(.system(size: 12.5, weight: .semibold))
                            Text(themeMode.label)
                                .font(.system(size: 12, weight: .semibold))
                        }
                        .foregroundColor(accentCrimson)
                        .padding(.horizontal, 11)
                        .frame(height: 34)
                        .background(accentCrimson.opacity(0.12))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 20))

                // Search settings bar
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    TextField("Search settings", text: $searchQuery)
                        .font(.system(size: 14.5))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    if !searchQuery.isEmpty {
                        Button { searchQuery = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 18))

                // Grouped cards
                if filtered.isEmpty {
                    Text("No matching settings")
                        .font(.system(size: 14.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .padding(.vertical, 32)
                } else {
                    ForEach(sectionOrder, id: \.self) { sectionTitle in
                        let rows = filtered.filter { $0.section == sectionTitle }
                        if !rows.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(sectionTitle)
                                    .font(.system(size: 13.5, weight: .semibold))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    .padding(.leading, 4)

                                VStack(spacing: 0) {
                                    ForEach(Array(rows.enumerated()), id: \.element.id) { idx, row in
                                        let isSelected = selectedDestination == row.destination
                                        Button {
                                            onSelect(row.destination)
                                        } label: {
                                            HStack(spacing: 14) {
                                                SettingsRedCircleBadgeView(kind: row.iconKind, size: 42)
                                                VStack(alignment: .leading, spacing: 2) {
                                                    Text(row.title)
                                                        .font(.system(size: 15.5, weight: .semibold))
                                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                                    Text(row.subtitle)
                                                        .font(.system(size: 12.5))
                                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                                        .lineLimit(2)
                                                        .multilineTextAlignment(.leading)
                                                }
                                                Spacer()
                                                Image(systemName: "chevron.right")
                                                    .font(.system(size: 13.5, weight: .bold))
                                                    .foregroundColor(TelefamPalette.crimson)
                                            }
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 14)
                                            .background(isSelected ? TelefamPalette.crimson.opacity(0.06) : Color.clear)
                                            .contentShape(Rectangle())
                                        }
                                        .buttonStyle(.plain)

                                        if idx < rows.count - 1 {
                                            Divider()
                                                .padding(.leading, 72)
                                                .padding(.trailing, 16)
                                        }
                                    }
                                }
                                .background(TelefamPalette.surface(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                            }
                        }
                    }
                }

                // Bottom Log out crimson pill button matching screenshot
                Button {
                    logoutAllDevices = false
                    showLogoutDialog = true
                } label: {
                    HStack(spacing: 10) {
                        SettingsLogoutArrowSvgIcon()
                            .frame(width: 22, height: 22)
                        Text("Log out")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(TelefamPalette.crimson)
                    .clipShape(Capsule())
                }
                .padding(.top, 8)
                .padding(.bottom, 20)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
    }

    @ViewBuilder
    private func detailHost(for dest: TelefamSettingsDestination) -> some View {
        switch dest {
        case .hub, .privacy:
            IosPrivacySubscreen(settingsStore: settingsStore, colorScheme: colorScheme, accentCrimson: accentCrimson)
        case .shareProfile:
            IosShareProfileSubscreen(
                displayName: displayName,
                username: username,
                authorizedAvatarUrl: authorizedAvatarUrl,
                profileShareUrl: profileShareUrl,
                sessionStore: sessionStore,
                accentCrimson: accentCrimson,
                onCopied: { settingsStore.showToast("Profile link copied") }
            )
        case .notifications:
            IosNotificationsSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .contentPreferences:
            IosContentPreferencesSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .savedLinks:
            TelefamIosSavedLinksView()
        case .offlineSync:
            IosOfflineSyncSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .blockedAccounts:
            IosBlockedMutedSubscreen(settingsStore: settingsStore, sessionStore: sessionStore, accentCrimson: accentCrimson)
        case .reportProblem:
            IosReportProblemSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .safetyCenter:
            IosSafetyCenterSubscreen(
                accentCrimson: accentCrimson,
                onOpenBlocked: { navPath.append(.blockedAccounts) },
                onOpenReport: { navPath.append(.reportProblem) }
            )
        case .helpSupport:
            IosHelpSupportSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .termsPolicies:
            IosTermsPoliciesSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        case .about:
            IosAboutSubscreen(settingsStore: settingsStore, accentCrimson: accentCrimson)
        }
    }

    private var logoutConfirmSheet: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Log out of this device?")
                .font(.system(size: 19, weight: .bold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))

            Text("You can sign back in anytime using your email verification code or linked login.")
                .font(.system(size: 13.5))
                .foregroundColor(TelefamPalette.textMuted(colorScheme))

            Toggle(isOn: $logoutAllDevices) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Log out of all devices")
                        .font(.system(size: 14.5, weight: .semibold))
                    Text("Revokes active tokens across Android, iOS, and Desktop")
                        .font(.system(size: 12))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                }
            }
            .tint(accentCrimson)
            .padding(12)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 14))

            Spacer()

            HStack(spacing: 12) {
                Button {
                    showLogoutDialog = false
                } label: {
                    Text("Cancel")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(TelefamPalette.border(colorScheme)))
                }

                Button {
                    isLoggingOut = true
                    Task {
                        settingsStore.clearCachesForCurrentUser()
                        smartLinksCoordinator.clearOnLogout()
                        await sessionStore.logout(allDevices: logoutAllDevices)
                        isLoggingOut = false
                        showLogoutDialog = false
                        dismiss()
                    }
                } label: {
                    HStack {
                        if isLoggingOut { ProgressView().tint(.white) }
                        Text(logoutAllDevices ? "Log out everywhere" : "Log out")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(accentCrimson)
                    .clipShape(Capsule())
                }
                .disabled(isLoggingOut)
            }
        }
        .padding(22)
        .background(TelefamPalette.background(colorScheme).ignoresSafeArea())
    }

    private func initials(_ name: String) -> String {
        let words = name.split(separator: " ")
        if words.count >= 2, let a = words.first?.first, let b = words.last?.first {
            return "\(a)\(b)".uppercased()
        }
        return String((name.isEmpty ? "T" : name).prefix(2)).uppercased()
    }
}

// MARK: - 1. Privacy Subscreen

private struct IosPrivacySubscreen: View {
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let colorScheme: ColorScheme
    let accentCrimson: Color

    var body: some View {
        let p = settingsStore.bundle.privacy
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                SettingsGroupCard(title: "Discoverability", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Private account",
                        subtitle: "Only approved followers see your videos, photos, likes, and lists",
                        isOn: Binding(
                            get: { p.isPrivateAccount },
                            set: { v in
                                var next = settingsStore.bundle.privacy
                                next.isPrivateAccount = v
                                Task { await settingsStore.savePrivacy(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Activity status",
                        subtitle: "Show when you were recently active on Telefam",
                        isOn: Binding(
                            get: { p.showActivityStatus },
                            set: { v in
                                var next = settingsStore.bundle.privacy
                                next.showActivityStatus = v
                                Task { await settingsStore.savePrivacy(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Who can find your account")
                            .font(.system(size: 14.5, weight: .semibold))
                        SettingsChoiceRow(
                            options: TelefamDiscoverabilityOption.allCases.map { ($0.rawValue, $0.label) },
                            selectedValue: p.whoCanFindYou,
                            accent: accentCrimson
                        ) { chosen in
                            var next = settingsStore.bundle.privacy
                            next.whoCanFindYou = chosen
                            Task { await settingsStore.savePrivacy(next) }
                        }
                    }
                    .padding(14)
                }

                SettingsGroupCard(title: "Interactions & visibility", scheme: colorScheme) {
                    SettingsChoiceSection(
                        title: "Who can comment on your posts",
                        options: TelefamAudienceOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: p.whoCanComment,
                        accent: accentCrimson
                    ) { chosen in
                        var next = settingsStore.bundle.privacy
                        next.whoCanComment = chosen
                        Task { await settingsStore.savePrivacy(next) }
                    }
                    Divider()
                    SettingsChoiceSection(
                        title: "Who can mention @you",
                        options: TelefamAudienceOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: p.whoCanMention,
                        accent: accentCrimson
                    ) { chosen in
                        var next = settingsStore.bundle.privacy
                        next.whoCanMention = chosen
                        Task { await settingsStore.savePrivacy(next) }
                    }
                    Divider()
                    SettingsChoiceSection(
                        title: "Who can see videos you liked",
                        options: TelefamVisibilityOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: p.likesVisibility,
                        accent: accentCrimson
                    ) { chosen in
                        var next = settingsStore.bundle.privacy
                        next.likesVisibility = chosen
                        Task { await settingsStore.savePrivacy(next) }
                    }
                    Divider()
                    SettingsChoiceSection(
                        title: "Following list visibility",
                        options: TelefamVisibilityOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: p.followingVisibility,
                        accent: accentCrimson
                    ) { chosen in
                        var next = settingsStore.bundle.privacy
                        next.followingVisibility = chosen
                        Task { await settingsStore.savePrivacy(next) }
                    }
                }

                SettingsGroupCard(title: "Personalization & cookies", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Personalized recommendations",
                        subtitle: "Use watched topics and likes to tailor For You feeds",
                        isOn: Binding(
                            get: { p.dataSharingPersonalization },
                            set: { v in
                                var next = settingsStore.bundle.privacy
                                next.dataSharingPersonalization = v
                                Task { await settingsStore.savePrivacy(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Playback & crash analytics",
                        subtitle: "Share anonymous bitrate & latency metrics",
                        isOn: Binding(
                            get: { p.analyticsSharing },
                            set: { v in
                                var next = settingsStore.bundle.privacy
                                next.analyticsSharing = v
                                Task { await settingsStore.savePrivacy(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Personalization storage & cookies",
                        subtitle: "Remember preferred languages and muted hashtags",
                        isOn: Binding(
                            get: { p.personalizationCookies },
                            set: { v in
                                var next = settingsStore.bundle.privacy
                                next.personalizationCookies = v
                                Task { await settingsStore.savePrivacy(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Essential security storage",
                        subtitle: "Always active for Keychain auth & offline sync",
                        isOn: .constant(true),
                        tint: accentCrimson,
                        disabled: true
                    )
                }
            }
            .padding(16)
        }
    }
}

// MARK: - 2. Share Profile Subscreen (CoreImage QR Code)

private struct IosShareProfileSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    let displayName: String
    let username: String
    let authorizedAvatarUrl: String
    let profileShareUrl: String
    @ObservedObject var sessionStore: TelefamIosSessionStore
    let accentCrimson: Color
    let onCopied: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 14) {
                    if !authorizedAvatarUrl.isEmpty {
                        TelefamIosRemoteImage(
                            urlString: authorizedAvatarUrl,
                            bearerToken: sessionStore.sessionToken,
                            deviceFingerprint: sessionStore.deviceFingerprint,
                            fallbackInitials: String(displayName.prefix(1)).uppercased()
                        )
                        .frame(width: 68, height: 68)
                        .clipShape(Circle())
                    }

                    Text(displayName)
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                    Text("@\(username)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(accentCrimson)

                    if let qr = generateQrImage(from: profileShareUrl) {
                        Image(uiImage: qr)
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 210, height: 210)
                            .padding(14)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(TelefamPalette.border(colorScheme)))
                    }

                    Text(profileShareUrl)
                        .font(.system(size: 12.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(22)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 22))

                HStack(spacing: 12) {
                    Button {
                        UIPasteboard.general.string = profileShareUrl
                        onCopied()
                    } label: {
                        Label("Copy link", systemImage: "doc.on.doc")
                            .font(.system(size: 14.5, weight: .semibold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(TelefamPalette.border(colorScheme)))
                    }

                    Button {
                        let av = UIActivityViewController(activityItems: ["Follow me on Telefam: \(profileShareUrl)"], applicationActivities: nil)
                        UIApplication.shared.connectedScenes
                            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
                            .first?
                            .present(av, animated: true)
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .font(.system(size: 14.5, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 50)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(18)
        }
    }

    private func generateQrImage(from text: String) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let outputImage = filter.outputImage else { return nil }
        let scaled = outputImage.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
        guard let cgimg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgimg)
    }
}

// MARK: - 3. Notifications Subscreen

private struct IosNotificationsSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color

    var body: some View {
        let n = settingsStore.bundle.notifications
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                SettingsGroupCard(title: "Master control", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Pause all notifications",
                        subtitle: "Silence push, email, and SMS alerts until resumed",
                        isOn: Binding(
                            get: { n.masterPause },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.masterPause = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                }

                SettingsGroupCard(title: "Delivery channels", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Push notifications",
                        subtitle: "Instant alerts on this device",
                        isOn: Binding(
                            get: { n.channelPush },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.channelPush = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Email updates",
                        subtitle: "Security notices and weekly digests",
                        isOn: Binding(
                            get: { n.channelEmail },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.channelEmail = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "SMS alerts",
                        subtitle: "Critical security alerts to verified phone numbers",
                        isOn: Binding(
                            get: { n.channelSms },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.channelSms = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                }

                SettingsGroupCard(title: "Alert categories", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Messages",
                        subtitle: "Direct chats & replies",
                        isOn: Binding(
                            get: { n.typeMessages },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.typeMessages = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Comments",
                        subtitle: "Comments and mentions on your posts",
                        isOn: Binding(
                            get: { n.typeComments },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.typeComments = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Followers",
                        subtitle: "New follower requests and follows",
                        isOn: Binding(
                            get: { n.typeFollowers },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.typeFollowers = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Product updates",
                        subtitle: "New features & creator tools",
                        isOn: Binding(
                            get: { n.typeProductUpdates },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.typeProductUpdates = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson,
                        disabled: n.masterPause
                    )
                }

                SettingsGroupCard(title: "Quiet hours", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Scheduled quiet hours",
                        subtitle: "Mute non-critical alerts between \(TelefamNotificationSettings.formatTime(n.quietStartMinutes)) and \(TelefamNotificationSettings.formatTime(n.quietEndMinutes))",
                        isOn: Binding(
                            get: { n.quietHoursEnabled },
                            set: { v in
                                var next = settingsStore.bundle.notifications
                                next.quietHoursEnabled = v
                                Task { await settingsStore.saveNotifications(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    if n.quietHoursEnabled {
                        Divider()
                        HStack {
                            DatePicker(
                                "Start",
                                selection: timeBinding(minutes: n.quietStartMinutes) { mins in
                                    var next = settingsStore.bundle.notifications
                                    next.quietStartMinutes = mins
                                    Task { await settingsStore.saveNotifications(next) }
                                },
                                displayedComponents: .hourAndMinute
                            )
                            Spacer(minLength: 12)
                            DatePicker(
                                "End",
                                selection: timeBinding(minutes: n.quietEndMinutes) { mins in
                                    var next = settingsStore.bundle.notifications
                                    next.quietEndMinutes = mins
                                    Task { await settingsStore.saveNotifications(next) }
                                },
                                displayedComponents: .hourAndMinute
                            )
                        }
                        .padding(14)
                    }
                }
            }
            .padding(16)
        }
    }

    private func timeBinding(minutes: Int, onUpdate: @escaping (Int) -> Void) -> Binding<Date> {
        Binding<Date>(
            get: {
                let cal = Calendar.current
                var comps = cal.dateComponents([.year, .month, .day], from: Date())
                comps.hour = ((minutes % 1440) + 1440) % 1440 / 60
                comps.minute = ((minutes % 1440) + 1440) % 1440 % 60
                return cal.date(from: comps) ?? Date()
            },
            set: { newDate in
                let cal = Calendar.current
                let h = cal.component(.hour, from: newDate)
                let m = cal.component(.minute, from: newDate)
                onUpdate((h * 60) + m)
            }
        )
    }
}

// MARK: - 4. Content Preferences Subscreen (Live reactivity without refresh)

private struct IosContentPreferencesSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color
    @State private var newTopicInput: String = ""

    var body: some View {
        let c = settingsStore.bundle.contentPreferences
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                SettingsGroupCard(title: "Feed recommendations", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Suggested content",
                        subtitle: "Show tailored recommendations across For You and Search",
                        isOn: Binding(
                            get: { c.suggestedContentEnabled },
                            set: { v in
                                var next = settingsStore.bundle.contentPreferences
                                next.suggestedContentEnabled = v
                                Task { await settingsStore.saveContentPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Reset recommendation history")
                                .font(.system(size: 14.5, weight: .semibold))
                            Text("Clear watch-time & topic affinities for a fresh feed")
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                        Spacer()
                        Button("Reset") {
                            let next = settingsStore.bundle.contentPreferences
                            Task { await settingsStore.saveContentPreferences(next, resetSuggestions: true) }
                        }
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundColor(accentCrimson)
                        .padding(.horizontal, 14)
                        .frame(height: 36)
                        .overlay(Capsule().stroke(accentCrimson, lineWidth: 1))
                    }
                    .padding(14)
                }

                SettingsGroupCard(title: "\"Not interested\" topics", scheme: colorScheme) {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 8) {
                            TextField("Add topic or #hashtag", text: $newTopicInput)
                                .padding(.horizontal, 12)
                                .frame(height: 44)
                                .background(TelefamPalette.background(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

                            Button {
                                addTopic(newTopicInput)
                                newTopicInput = ""
                            } label: {
                                Text("Add")
                                    .font(.system(size: 13.5, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 16)
                                    .frame(height: 44)
                                    .background(accentCrimson)
                                    .clipShape(Capsule())
                            }
                        }

                        if !c.notInterestedTopics.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(c.notInterestedTopics, id: \.self) { topic in
                                        Button {
                                            var next = settingsStore.bundle.contentPreferences
                                            next.notInterestedTopics.removeAll { $0 == topic }
                                            Task { await settingsStore.saveContentPreferences(next) }
                                        } label: {
                                            HStack(spacing: 5) {
                                                Text("#\(topic)")
                                                    .font(.system(size: 12.5, weight: .semibold))
                                                Image(systemName: "xmark")
                                                    .font(.system(size: 10, weight: .bold))
                                            }
                                            .foregroundColor(accentCrimson)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 7)
                                            .background(accentCrimson.opacity(0.12))
                                            .clipShape(Capsule())
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                            }
                        }

                        Text("Quick filters")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(TelefamIosSettingsCatalog.quickTopicTags.filter { !c.notInterestedTopics.contains($0) }, id: \.self) { tag in
                                    Button {
                                        addTopic(tag)
                                    } label: {
                                        Text("+ #\(tag)")
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            .padding(.horizontal, 11)
                                            .padding(.vertical, 6)
                                            .background(TelefamPalette.background(colorScheme))
                                            .clipShape(Capsule())
                                            .overlay(Capsule().stroke(TelefamPalette.border(colorScheme)))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                    .padding(14)
                }

                SettingsGroupCard(title: "Language & playback", scheme: colorScheme) {
                    SettingsChoiceSection(
                        title: "Preferred content language",
                        options: TelefamAppLanguageOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: c.languageCode,
                        accent: accentCrimson
                    ) { code in
                        var next = settingsStore.bundle.contentPreferences
                        next.languageCode = code
                        Task { await settingsStore.saveContentPreferences(next) }
                    }
                    Divider()
                    SettingsSwitchRow(
                        title: "Autoplay videos",
                        subtitle: "Automatically begin playback in feeds and search",
                        isOn: Binding(
                            get: { c.autoplayVideos },
                            set: { v in
                                var next = settingsStore.bundle.contentPreferences
                                next.autoplayVideos = v
                                Task { await settingsStore.saveContentPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsChoiceSection(
                        title: "Video quality",
                        options: TelefamVideoQualityOption.allCases.map { ($0.rawValue, $0.label) },
                        selectedValue: c.videoQuality,
                        accent: accentCrimson
                    ) { q in
                        var next = settingsStore.bundle.contentPreferences
                        next.videoQuality = q
                        Task { await settingsStore.saveContentPreferences(next) }
                    }
                    Divider()
                    SettingsSwitchRow(
                        title: "Data saver",
                        subtitle: "Prefer compact streams and limit cellular preloading",
                        isOn: Binding(
                            get: { c.dataSaver },
                            set: { v in
                                var next = settingsStore.bundle.contentPreferences
                                next.dataSaver = v
                                Task { await settingsStore.saveContentPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                }

                SettingsGroupCard(title: "Accessibility", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Captions",
                        subtitle: "Display closed captions overlay during playback",
                        isOn: Binding(
                            get: { c.captionsEnabled },
                            set: { v in
                                var next = settingsStore.bundle.contentPreferences
                                next.captionsEnabled = v
                                Task { await settingsStore.saveContentPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Reduced motion",
                        subtitle: "Minimize celebratory animations & hero crossfades",
                        isOn: Binding(
                            get: { c.reducedMotion },
                            set: { v in
                                var next = settingsStore.bundle.contentPreferences
                                next.reducedMotion = v
                                Task { await settingsStore.saveContentPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                }
            }
            .padding(16)
        }
    }

    private func addTopic(_ raw: String) {
        let clean = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "#"))
            .lowercased()
        guard !clean.isEmpty else { return }
        var next = settingsStore.bundle.contentPreferences
        if !next.notInterestedTopics.contains(clean) {
            next.notInterestedTopics.append(clean)
            Task { await settingsStore.saveContentPreferences(next) }
        }
    }
}

// MARK: - 5. Blocked & Muted Accounts Subscreen

private struct IosBlockedMutedSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    @ObservedObject var sessionStore: TelefamIosSessionStore
    let accentCrimson: Color

    @State private var selectedTab: Int = 0
    @State private var query: String = ""

    var body: some View {
        let source = selectedTab == 0 ? settingsStore.blockedAccounts : settingsStore.mutedAccounts
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let filtered = source.filter {
            q.isEmpty || $0.fullName.lowercased().contains(q) || $0.username.lowercased().contains(q)
        }

        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                TextField("Search accounts", text: $query)
                    .font(.system(size: 14.5))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                if !query.isEmpty {
                    Button { query = "" } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                }
            }
            .padding(.horizontal, 14)
            .frame(height: 46)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 16)
            .padding(.top, 12)

            Picker("List type", selection: $selectedTab) {
                Text("Blocked (\(settingsStore.blockedAccounts.count))").tag(0)
                Text("Muted (\(settingsStore.mutedAccounts.count))").tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)

            if filtered.isEmpty {
                Spacer()
                Text(selectedTab == 0 ? "No blocked accounts" : "No muted accounts")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(filtered) { acc in
                            HStack(spacing: 12) {
                                let resolvedAvatar: String = {
                                    guard !acc.avatarUrl.isEmpty else { return "" }
                                    if acc.avatarUrl.hasPrefix("http") { return acc.avatarUrl }
                                    let base = settingsStore.baseURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                                    let path = acc.avatarUrl.hasPrefix("/") ? String(acc.avatarUrl.dropFirst()) : acc.avatarUrl
                                    return "\(base)/\(path)"
                                }()
                                if !resolvedAvatar.isEmpty {
                                    TelefamIosRemoteImage(
                                        urlString: resolvedAvatar,
                                        bearerToken: sessionStore.sessionToken,
                                        deviceFingerprint: sessionStore.deviceFingerprint,
                                        fallbackInitials: String(acc.fullName.prefix(1)).uppercased()
                                    )
                                    .frame(width: 44, height: 44)
                                    .clipShape(Circle())
                                } else {
                                    Circle()
                                        .fill(TelefamPalette.blush(colorScheme))
                                        .frame(width: 44, height: 44)
                                        .overlay(
                                            Text(String((acc.fullName.isEmpty ? acc.username : acc.fullName).prefix(1)).uppercased())
                                                .font(.system(size: 15, weight: .bold))
                                                .foregroundColor(accentCrimson)
                                        )
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(acc.fullName.isEmpty ? acc.username : acc.fullName)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text("@\(acc.username)")
                                        .font(.system(size: 12.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }

                                Spacer()

                                Button(selectedTab == 0 ? "Unblock" : "Unmute") {
                                    Task {
                                        if selectedTab == 0 {
                                            await settingsStore.unblockAccount(userId: acc.userId)
                                        } else {
                                            await settingsStore.unmuteAccount(userId: acc.userId)
                                        }
                                    }
                                }
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(accentCrimson)
                                .padding(.horizontal, 14)
                                .frame(height: 36)
                                .overlay(Capsule().stroke(accentCrimson, lineWidth: 1))
                            }
                            .padding(14)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 18)
                }
            }
        }
        .task {
            await settingsStore.refreshRestrictedAccounts(isMuted: false, query: "")
            await settingsStore.refreshRestrictedAccounts(isMuted: true, query: "")
        }
    }
}

// MARK: - 6. Report a Problem Subscreen

private struct IosReportProblemSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color

    @State private var selectedCategory: String = TelefamIosSettingsCatalog.reportCategories.first ?? "Bug or crash"
    @State private var descriptionText: String = ""
    @State private var pickedItem: PhotosPickerItem? = nil
    @State private var screenshotData: Data? = nil
    @State private var screenshotPreview: UIImage? = nil
    @State private var screenshotSha256: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let ticket = settingsStore.lastCreatedTicket {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(Color(red: 0.086, green: 0.639, blue: 0.290))
                            Text("Report submitted")
                                .font(.system(size: 16, weight: .bold))
                        }
                        Text("Reference: \(ticket.referenceNumber)")
                            .font(.system(size: 15, weight: .bold, design: .monospaced))
                            .foregroundColor(accentCrimson)
                        Text(ticket.resolutionNote)
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        Button("Submit another report") {
                            settingsStore.lastCreatedTicket = nil
                        }
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundColor(accentCrimson)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(TelefamPalette.surface(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }

                SettingsGroupCard(title: "Category", scheme: colorScheme) {
                    SettingsChoiceSection(
                        title: "Choose issue type",
                        options: TelefamIosSettingsCatalog.reportCategories.map { ($0, $0) },
                        selectedValue: selectedCategory,
                        accent: accentCrimson
                    ) { chosen in
                        selectedCategory = chosen
                    }
                }

                SettingsGroupCard(title: "Details", scheme: colorScheme) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Describe what happened (at least 5 characters)")
                            .font(.system(size: 13.5, weight: .semibold))
                        TextEditor(text: $descriptionText)
                            .frame(minHeight: 110)
                            .padding(8)
                            .background(TelefamPalette.background(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

                        HStack(spacing: 12) {
                            PhotosPicker(selection: $pickedItem, matching: .images) {
                                Label(screenshotPreview == nil ? "Attach screenshot" : "Change screenshot", systemImage: "photo")
                                    .font(.system(size: 13.5, weight: .semibold))
                                    .foregroundColor(accentCrimson)
                                    .padding(.horizontal, 14)
                                    .frame(height: 42)
                                    .overlay(Capsule().stroke(accentCrimson, lineWidth: 1))
                            }
                            if screenshotPreview != nil {
                                Button("Remove") {
                                    screenshotData = nil
                                    screenshotPreview = nil
                                    screenshotSha256 = ""
                                }
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                        }

                        if let img = screenshotPreview {
                            HStack(spacing: 12) {
                                Image(uiImage: img)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 68, height: 68)
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("Screenshot attached")
                                        .font(.system(size: 13.5, weight: .semibold))
                                    if !screenshotSha256.isEmpty {
                                        Text("SHA-256: \(screenshotSha256.prefix(16))...")
                                            .font(.system(size: 11, design: .monospaced))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    }
                                }
                            }
                        }
                    }
                    .padding(14)
                }

                Button {
                    let cat = selectedCategory
                    let desc = descriptionText
                    let data = screenshotData
                    Task {
                        await settingsStore.submitProblemReport(category: cat, description: desc, screenshotData: data)
                        if settingsStore.lastCreatedTicket != nil {
                            descriptionText = ""
                            screenshotData = nil
                            screenshotPreview = nil
                            screenshotSha256 = ""
                        }
                    }
                } label: {
                    HStack {
                        if settingsStore.isSubmittingReport { ProgressView().tint(.white) }
                        Text("Submit report")
                            .font(.system(size: 15.5, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(accentCrimson)
                    .clipShape(Capsule())
                }
                .disabled(settingsStore.isSubmittingReport || descriptionText.trimmingCharacters(in: .whitespacesAndNewlines).count < 5)
            }
            .padding(16)
        }
        .onChange(of: pickedItem) { item in
            guard let item else { return }
            Task {
                if let raw = try? await item.loadTransferable(type: Data.self),
                   let ui = UIImage(data: raw) {
                    screenshotData = raw
                    screenshotPreview = ui
                    screenshotSha256 = TelefamIosSessionStore.sha256Hex(data: raw)
                }
            }
        }
    }
}

// MARK: - 7. Safety Center Subscreen

private struct IosSafetyCenterSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    let accentCrimson: Color
    let onOpenBlocked: () -> Void
    let onOpenReport: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                ForEach(TelefamIosSettingsCatalog.safetySections) { sec in
                    SettingsGroupCard(title: sec.title, scheme: colorScheme) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(sec.summary)
                                .font(.system(size: 13.5, weight: .medium))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            ForEach(sec.bulletPoints, id: \.self) { point in
                                HStack(alignment: .top, spacing: 8) {
                                    Circle()
                                        .fill(accentCrimson)
                                        .frame(width: 6, height: 6)
                                        .padding(.top, 6)
                                    Text(point)
                                        .font(.system(size: 13.5))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                }
                            }
                            if sec.id == "harassment" {
                                HStack(spacing: 10) {
                                    Button("Blocked accounts", action: onOpenBlocked)
                                        .font(.system(size: 13, weight: .bold))
                                        .foregroundColor(accentCrimson)
                                        .padding(.horizontal, 14)
                                        .frame(height: 36)
                                        .overlay(Capsule().stroke(accentCrimson))
                                    Button("Report problem", action: onOpenReport)
                                        .font(.system(size: 13, weight: .bold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 14)
                                        .frame(height: 36)
                                        .background(accentCrimson)
                                        .clipShape(Capsule())
                                }
                                .padding(.top, 4)
                            }
                        }
                        .padding(14)
                    }
                }

                SettingsGroupCard(title: "Emergency contacts", scheme: colorScheme) {
                    VStack(spacing: 0) {
                        ForEach(Array(TelefamIosSettingsCatalog.emergencyContacts.enumerated()), id: \.element.id) { idx, c in
                            VStack(alignment: .leading, spacing: 3) {
                                Text("\(c.region) • \(c.serviceName)")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Text(c.contact)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(accentCrimson)
                                Text(c.notes)
                                    .font(.system(size: 12.5))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(14)
                            if idx < TelefamIosSettingsCatalog.emergencyContacts.count - 1 { Divider() }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

// MARK: - 8. Help & Support Subscreen

private struct IosHelpSupportSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color

    @State private var tab: Int = 0
    @State private var faqQuery: String = ""
    @State private var expandedFaqIds: Set<String> = []

    var body: some View {
        VStack(spacing: 12) {
            Picker("Support section", selection: $tab) {
                Text("FAQs").tag(0)
                Text("Tickets (\(settingsStore.supportTickets.count))").tag(1)
                Text("System status").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.top, 12)

            if tab == 0 {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    TextField("Search FAQs", text: $faqQuery)
                        .font(.system(size: 14.5))
                        .onChange(of: faqQuery) { q in
                            Task { await settingsStore.searchFaqs(query: q) }
                        }
                }
                .padding(.horizontal, 14)
                .frame(height: 46)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 16)

                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(settingsStore.faqItems) { faq in
                            let open = expandedFaqIds.contains(faq.id)
                            VStack(alignment: .leading, spacing: 8) {
                                Button {
                                    if open { expandedFaqIds.remove(faq.id) } else { expandedFaqIds.insert(faq.id) }
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(faq.category.uppercased())
                                                .font(.system(size: 11, weight: .bold))
                                                .foregroundColor(accentCrimson)
                                            Text(faq.question)
                                                .font(.system(size: 15, weight: .semibold))
                                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                                .multilineTextAlignment(.leading)
                                        }
                                        Spacer()
                                        Image(systemName: open ? "chevron.up" : "chevron.down")
                                            .font(.system(size: 13, weight: .bold))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    }
                                }
                                .buttonStyle(.plain)

                                if open {
                                    Text(faq.answer)
                                        .font(.system(size: 13.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                            }
                            .padding(14)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 18)
                }
            } else if tab == 1 {
                if settingsStore.supportTickets.isEmpty {
                    Spacer()
                    Text("No support tickets submitted yet")
                        .font(.system(size: 14.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(settingsStore.supportTickets) { ticket in
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text(ticket.referenceNumber)
                                            .font(.system(size: 13.5, weight: .bold, design: .monospaced))
                                            .foregroundColor(accentCrimson)
                                        Spacer()
                                        Text(ticket.status.uppercased())
                                            .font(.system(size: 11, weight: .bold))
                                            .padding(.horizontal, 9)
                                            .padding(.vertical, 4)
                                            .background(TelefamPalette.blush(colorScheme))
                                            .foregroundColor(accentCrimson)
                                            .clipShape(Capsule())
                                    }
                                    Text(ticket.category)
                                        .font(.system(size: 14.5, weight: .semibold))
                                    Text(ticket.description)
                                        .font(.system(size: 13))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    if !ticket.resolutionNote.isEmpty {
                                        Text(ticket.resolutionNote)
                                            .font(.system(size: 12))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    }
                                }
                                .padding(14)
                                .background(TelefamPalette.surface(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 18)
                    }
                }
            } else {
                ScrollView {
                    VStack(spacing: 10) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Overall system health")
                                    .font(.system(size: 13))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                Text(settingsStore.systemOverallStatus.capitalized)
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(Color(red: 0.086, green: 0.639, blue: 0.290))
                            }
                            Spacer()
                            Button("Refresh") {
                                Task { await settingsStore.refreshSystemStatus() }
                            }
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(accentCrimson)
                        }
                        .padding(14)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 16))

                        ForEach(settingsStore.systemServices) { svc in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(svc.serviceName)
                                        .font(.system(size: 14.5, weight: .semibold))
                                    Spacer()
                                    Text("\(svc.latencyMs) ms • \(svc.status.uppercased())")
                                        .font(.system(size: 11.5, weight: .bold))
                                        .foregroundColor(Color(red: 0.086, green: 0.639, blue: 0.290))
                                }
                                Text(svc.summary)
                                    .font(.system(size: 12.5))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            .padding(14)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 18)
                }
            }
        }
        .task {
            await settingsStore.searchFaqs(query: faqQuery)
            await settingsStore.refreshSupportTickets()
            await settingsStore.refreshSystemStatus()
        }
    }
}

// MARK: - 9. Terms and Policies Subscreen

private struct IosTermsPoliciesSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color
    @State private var selectedDocId: String = "terms"

    var body: some View {
        let docs = settingsStore.legalDocs
        let current = docs.first { $0.id == selectedDocId } ?? docs.first

        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(docs) { doc in
                        let selected = doc.id == current?.id
                        Button {
                            selectedDocId = doc.id
                        } label: {
                            Text(doc.title)
                                .font(.system(size: 13, weight: selected ? .bold : .medium))
                                .foregroundColor(selected ? .white : TelefamPalette.textPrimary(colorScheme))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selected ? accentCrimson : TelefamPalette.surface(colorScheme))
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }

            if let doc = current {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(doc.title)
                            .font(.system(size: 19, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text("Last updated: \(doc.lastUpdated)")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(accentCrimson)
                        ForEach(doc.sections, id: \.self) { para in
                            Text(para)
                                .font(.system(size: 14))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .lineSpacing(4)
                        }
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(TelefamPalette.surface(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
            }
        }
        .task {
            await settingsStore.refreshLegalDocuments()
        }
    }
}

// MARK: - 10. About Subscreen

private struct IosAboutSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color
    @State private var showLicenses: Bool = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                SettingsGroupCard(title: "Application version", scheme: colorScheme) {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Telefam")
                                    .font(.system(size: 17, weight: .bold))
                                Text("Version \(settingsStore.installedVersionString()) (Build \(settingsStore.installedBuildNumber()))")
                                    .font(.system(size: 13))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            Spacer()
                            Button {
                                Task { await settingsStore.checkForUpdates() }
                            } label: {
                                if settingsStore.isCheckingUpdates {
                                    ProgressView().tint(accentCrimson)
                                } else {
                                    Text("Check for updates")
                                        .font(.system(size: 13, weight: .bold))
                                        .foregroundColor(accentCrimson)
                                }
                            }
                            .padding(.horizontal, 12)
                            .frame(height: 36)
                            .overlay(Capsule().stroke(accentCrimson, lineWidth: 1))
                        }

                        if let res = settingsStore.versionCheckResult {
                            Text(res.updateAvailable ? "Update available: \(res.latestVersion)" : "Up to date (\(res.latestVersion))")
                                .font(.system(size: 13.5, weight: .bold))
                                .foregroundColor(res.updateAvailable ? accentCrimson : Color(red: 0.086, green: 0.639, blue: 0.290))
                            Text(res.releaseNotes)
                                .font(.system(size: 12.5))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                    .padding(14)
                }

                SettingsGroupCard(title: "Company & copyright", scheme: colorScheme) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Telefam Communications Inc.")
                            .font(.system(size: 15, weight: .bold))
                        Text("Copyright 2026 Telefam Communications Inc. All rights reserved.")
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        Text("Encrypted multi-platform family & creator community across Android, iOS, and Desktop.")
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                    .padding(14)
                }

                SettingsGroupCard(title: "Open source licenses", scheme: colorScheme) {
                    VStack(spacing: 0) {
                        Button {
                            withAnimation { showLicenses.toggle() }
                        } label: {
                            HStack {
                                Text("View third-party notices (\(TelefamIosSettingsCatalog.openSourceLicenses.count))")
                                    .font(.system(size: 14.5, weight: .semibold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Spacer()
                                Image(systemName: showLicenses ? "chevron.up" : "chevron.down")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(accentCrimson)
                            }
                            .padding(14)
                        }
                        .buttonStyle(.plain)

                        if showLicenses {
                            Divider()
                            ForEach(TelefamIosSettingsCatalog.openSourceLicenses) { lic in
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(lic.library)
                                        .font(.system(size: 13.5, weight: .bold))
                                    Text("\(lic.license) • \(lic.copyright)")
                                        .font(.system(size: 12))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                Divider()
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

// MARK: - 11. Offline Sync Subscreen

private struct IosOfflineSyncSubscreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let accentCrimson: Color

    private func formattedTimestamp(_ ms: Int64) -> String {
        guard ms > 0 else { return "Not synced yet" }
        let dt = Date(timeIntervalSince1970: Double(ms) / 1000.0)
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, HH:mm"
        return fmt.string(from: dt)
    }

    var body: some View {
        let sp = settingsStore.bundle.syncPreferences
        let pending = settingsStore.pendingOutboxCount
        let netLabel: String = {
            if !settingsStore.isNetworkConnected { return "Offline" }
            return settingsStore.isWifiConnected ? "Online (Wi-Fi)" : "Online (Cellular)"
        }()

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Server synchronization")
                                .font(.system(size: 16.5, weight: .bold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Text(
                                pending == 0
                                    ? "All changes synced • Last synced: \(formattedTimestamp(sp.lastSyncedAtMs))"
                                    : "\(pending) queued offline change(s) waiting to upload"
                            )
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                        Spacer()
                        Text(pending == 0 ? netLabel.uppercased() : "\(pending) PENDING")
                            .font(.system(size: 11, weight: .bold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(
                                pending == 0
                                    ? Color(red: 0.055, green: 0.478, blue: 0.271).opacity(0.14)
                                    : accentCrimson.opacity(0.14)
                            )
                            .foregroundColor(
                                pending == 0
                                    ? Color(red: 0.055, green: 0.478, blue: 0.271)
                                    : accentCrimson
                            )
                            .clipShape(Capsule())
                    }

                    if sp.totalSyncedOps > 0 {
                        Text("Total operations synced with server: \(sp.totalSyncedOps)")
                            .font(.system(size: 12))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }

                    if !settingsStore.lastSyncMessage.isEmpty {
                        Text(settingsStore.lastSyncMessage)
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundColor(accentCrimson)
                    }

                    HStack(spacing: 10) {
                        Button {
                            Task { await settingsStore.syncNowToServer() }
                        } label: {
                            HStack(spacing: 8) {
                                if settingsStore.isSyncingNow {
                                    ProgressView().tint(.white)
                                }
                                Text(settingsStore.isSyncingNow ? "Syncing..." : "Sync now")
                                    .font(.system(size: 14.5, weight: .bold))
                                    .foregroundColor(.white)
                            }
                            .frame(maxWidth: .infinity, minHeight: 46)
                            .background(accentCrimson)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .disabled(settingsStore.isSyncingNow)

                        Button {
                            Task { await settingsStore.refreshAll() }
                        } label: {
                            Text("Refresh")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .padding(.horizontal, 16)
                                .frame(minHeight: 46)
                                .background(TelefamPalette.background(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(TelefamPalette.border(colorScheme)))
                        }
                        .disabled(settingsStore.isSyncingNow)
                    }
                }
                .padding(16)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 20))

                SettingsGroupCard(title: "Sync automation", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Auto-sync when online",
                        subtitle: "Automatically upload queued offline edits when connectivity returns",
                        isOn: Binding(
                            get: { sp.autoSyncEnabled },
                            set: { v in
                                var next = settingsStore.bundle.syncPreferences
                                next.autoSyncEnabled = v
                                Task { await settingsStore.saveSyncPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Sync over Wi-Fi only",
                        subtitle: "Hold queued uploads until connected to Wi-Fi or Ethernet",
                        isOn: Binding(
                            get: { sp.syncOverWifiOnly },
                            set: { v in
                                var next = settingsStore.bundle.syncPreferences
                                next.syncOverWifiOnly = v
                                Task { await settingsStore.saveSyncPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Background sync",
                        subtitle: "Keep preferences, privacy settings, and themes synchronized quietly",
                        isOn: Binding(
                            get: { sp.backgroundSyncEnabled },
                            set: { v in
                                var next = settingsStore.bundle.syncPreferences
                                next.backgroundSyncEnabled = v
                                Task { await settingsStore.saveSyncPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                }

                SettingsGroupCard(title: "Offline cache", scheme: colorScheme) {
                    SettingsSwitchRow(
                        title: "Cache feeds offline",
                        subtitle: "Save recent feed items on device so browsing continues offline",
                        isOn: Binding(
                            get: { sp.cacheFeedsOffline },
                            set: { v in
                                var next = settingsStore.bundle.syncPreferences
                                next.cacheFeedsOffline = v
                                Task { await settingsStore.saveSyncPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                    Divider()
                    SettingsSwitchRow(
                        title: "Cache chat media offline",
                        subtitle: "Retain recent photos and voice clips for instant offline viewing",
                        isOn: Binding(
                            get: { sp.cacheChatMediaOffline },
                            set: { v in
                                var next = settingsStore.bundle.syncPreferences
                                next.cacheChatMediaOffline = v
                                Task { await settingsStore.saveSyncPreferences(next) }
                            }
                        ),
                        tint: accentCrimson
                    )
                }
            }
            .padding(16)
        }
    }
}

// MARK: - Reusable Setting Card & Toggle Rows

private struct SettingsGroupCard<Content: View>: View {
    let title: String
    let scheme: ColorScheme
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13.5, weight: .semibold))
                .foregroundColor(TelefamPalette.textMuted(scheme))
                .padding(.leading, 4)

            VStack(spacing: 0) {
                content()
            }
            .background(TelefamPalette.surface(scheme))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }
}

private struct SettingsSwitchRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let subtitle: String
    let isOn: Binding<Bool>
    let tint: Color
    var disabled: Bool = false

    var body: some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Text(subtitle)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
            }
        }
        .tint(tint)
        .disabled(disabled)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }
}

private struct SettingsChoiceSection: View {
    @Environment(\.colorScheme) private var colorScheme
    let title: String
    let options: [(String, String)]
    let selectedValue: String
    let accent: Color
    let onSelect: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 14.5, weight: .semibold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
            SettingsChoiceRow(options: options, selectedValue: selectedValue, accent: accent, onSelect: onSelect)
        }
        .padding(14)
    }
}

private struct SettingsChoiceRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let options: [(String, String)]
    let selectedValue: String
    let accent: Color
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(options, id: \.0) { value, label in
                    let selected = selectedValue.caseInsensitiveCompare(value) == .orderedSame
                    Button {
                        onSelect(value)
                    } label: {
                        Text(label)
                            .font(.system(size: 12.5, weight: selected ? .bold : .medium))
                            .foregroundColor(selected ? .white : TelefamPalette.textPrimary(colorScheme))
                            .padding(.horizontal, 13)
                            .frame(height: 36)
                            .background(selected ? accent : TelefamPalette.background(colorScheme))
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(selected ? Color.clear : TelefamPalette.border(colorScheme)))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

// MARK: - Custom Canvas Vector Shapes & Crimson Circle Badges (Zero Emojis)

private struct UnevenRoundedBottomShape: Shape {
    let radius: CGFloat
    func path(in rect: CGRect) -> Path {
        let p = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.bottomLeft, .bottomRight],
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(p.cgPath)
    }
}

private struct SettingsLogoutArrowSvgIcon: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 2.1, lineCap: .round, lineJoin: .round)
            var door = Path()
            door.move(to: CGPoint(x: w * 0.45, y: h * 0.20))
            door.addLine(to: CGPoint(x: w * 0.20, y: h * 0.20))
            door.addLine(to: CGPoint(x: w * 0.20, y: h * 0.80))
            door.addLine(to: CGPoint(x: w * 0.45, y: h * 0.80))
            ctx.stroke(door, with: .color(.white), style: stroke)

            var arrow = Path()
            arrow.move(to: CGPoint(x: w * 0.42, y: h * 0.50))
            arrow.addLine(to: CGPoint(x: w * 0.82, y: h * 0.50))
            arrow.move(to: CGPoint(x: w * 0.68, y: h * 0.34))
            arrow.addLine(to: CGPoint(x: w * 0.84, y: h * 0.50))
            arrow.addLine(to: CGPoint(x: w * 0.68, y: h * 0.66))
            ctx.stroke(arrow, with: .color(.white), style: stroke)
        }
    }
}

private struct SettingsRedCircleBadgeView: View {
    let kind: SettingsBadgeSvgKind
    let size: CGFloat

    var body: some View {
        ZStack {
            Circle()
                .fill(TelefamPalette.crimson)
                .frame(width: size, height: size)

            Canvas { ctx, sz in
                let w = sz.width
                let h = sz.height
                let stroke = StrokeStyle(lineWidth: 1.9, lineCap: .round, lineJoin: .round)
                switch kind {
                case .activityPulse:
                    var p = Path()
                    p.move(to: CGPoint(x: w * 0.14, y: h * 0.54))
                    p.addLine(to: CGPoint(x: w * 0.34, y: h * 0.54))
                    p.addLine(to: CGPoint(x: w * 0.44, y: h * 0.28))
                    p.addLine(to: CGPoint(x: w * 0.56, y: h * 0.72))
                    p.addLine(to: CGPoint(x: w * 0.66, y: h * 0.46))
                    p.addLine(to: CGPoint(x: w * 0.86, y: h * 0.46))
                    ctx.stroke(p, with: .color(.white), style: stroke)
                case .shareCard:
                    ctx.stroke(
                        Path(roundedRect: CGRect(x: w * 0.16, y: h * 0.26, width: w * 0.68, height: h * 0.48), cornerRadius: w * 0.08),
                        with: .color(.white),
                        style: stroke
                    )
                    var bar = Path()
                    bar.move(to: CGPoint(x: w * 0.16, y: h * 0.44))
                    bar.addLine(to: CGPoint(x: w * 0.84, y: h * 0.44))
                    ctx.stroke(bar, with: .color(.white), style: stroke)
                case .bell:
                    var p = Path()
                    p.move(to: CGPoint(x: w * 0.30, y: h * 0.66))
                    p.addLine(to: CGPoint(x: w * 0.30, y: h * 0.42))
                    p.addCurve(to: CGPoint(x: w * 0.70, y: h * 0.42), control1: CGPoint(x: w * 0.30, y: h * 0.22), control2: CGPoint(x: w * 0.70, y: h * 0.22))
                    p.addLine(to: CGPoint(x: w * 0.70, y: h * 0.66))
                    p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.72))
                    p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.72))
                    p.closeSubpath()
                    ctx.stroke(p, with: .color(.white), style: stroke)
                    var clapper = Path()
                    clapper.move(to: CGPoint(x: w * 0.44, y: h * 0.80))
                    clapper.addLine(to: CGPoint(x: w * 0.56, y: h * 0.80))
                    ctx.stroke(clapper, with: .color(.white), style: stroke)
                case .heart:
                    var p = Path()
                    p.move(to: CGPoint(x: w * 0.50, y: h * 0.80))
                    p.addCurve(to: CGPoint(x: w * 0.18, y: h * 0.40), control1: CGPoint(x: w * 0.24, y: h * 0.62), control2: CGPoint(x: w * 0.18, y: h * 0.48))
                    p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.32), control1: CGPoint(x: w * 0.18, y: h * 0.22), control2: CGPoint(x: w * 0.42, y: h * 0.20))
                    p.addCurve(to: CGPoint(x: w * 0.82, y: h * 0.40), control1: CGPoint(x: w * 0.58, y: h * 0.20), control2: CGPoint(x: w * 0.82, y: h * 0.22))
                    p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.80), control1: CGPoint(x: w * 0.82, y: h * 0.48), control2: CGPoint(x: w * 0.76, y: h * 0.62))
                    ctx.stroke(p, with: .color(.white), style: stroke)
                case .lock:
                    ctx.stroke(
                        Path(roundedRect: CGRect(x: w * 0.24, y: h * 0.44, width: w * 0.52, height: h * 0.38), cornerRadius: w * 0.08),
                        with: .color(.white),
                        style: stroke
                    )
                    var shackle = Path()
                    shackle.move(to: CGPoint(x: w * 0.34, y: h * 0.44))
                    shackle.addLine(to: CGPoint(x: w * 0.34, y: h * 0.32))
                    shackle.addCurve(to: CGPoint(x: w * 0.66, y: h * 0.32), control1: CGPoint(x: w * 0.34, y: h * 0.18), control2: CGPoint(x: w * 0.66, y: h * 0.18))
                    shackle.addLine(to: CGPoint(x: w * 0.66, y: h * 0.44))
                    ctx.stroke(shackle, with: .color(.white), style: stroke)
                case .blockMinus:
                    ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.18, width: w * 0.64, height: h * 0.64)), with: .color(.white), style: stroke)
                    var bar = Path()
                    bar.move(to: CGPoint(x: w * 0.34, y: h * 0.50))
                    bar.addLine(to: CGPoint(x: w * 0.66, y: h * 0.50))
                    ctx.stroke(bar, with: .color(.white), style: stroke)
                case .flag:
                    var p = Path()
                    p.move(to: CGPoint(x: w * 0.28, y: h * 0.82))
                    p.addLine(to: CGPoint(x: w * 0.28, y: h * 0.22))
                    p.addLine(to: CGPoint(x: w * 0.76, y: h * 0.30))
                    p.addLine(to: CGPoint(x: w * 0.64, y: h * 0.44))
                    p.addLine(to: CGPoint(x: w * 0.76, y: h * 0.56))
                    p.addLine(to: CGPoint(x: w * 0.28, y: h * 0.56))
                    ctx.stroke(p, with: .color(.white), style: stroke)
                case .shieldCheck:
                    var p = Path()
                    p.move(to: CGPoint(x: w * 0.50, y: h * 0.16))
                    p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.28))
                    p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.52))
                    p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.84), control1: CGPoint(x: w * 0.78, y: h * 0.70), control2: CGPoint(x: w * 0.62, y: h * 0.80))
                    p.addCurve(to: CGPoint(x: w * 0.22, y: h * 0.52), control1: CGPoint(x: w * 0.38, y: h * 0.80), control2: CGPoint(x: w * 0.22, y: h * 0.70))
                    p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.28))
                    p.closeSubpath()
                    ctx.stroke(p, with: .color(.white), style: stroke)
                    var chk = Path()
                    chk.move(to: CGPoint(x: w * 0.38, y: h * 0.50))
                    chk.addLine(to: CGPoint(x: w * 0.47, y: h * 0.60))
                    chk.addLine(to: CGPoint(x: w * 0.64, y: h * 0.40))
                    ctx.stroke(chk, with: .color(.white), style: stroke)
                case .helpQuestion:
                    ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.18, width: w * 0.64, height: h * 0.64)), with: .color(.white), style: stroke)
                    var q = Path()
                    q.move(to: CGPoint(x: w * 0.40, y: h * 0.40))
                    q.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.54), control1: CGPoint(x: w * 0.42, y: h * 0.28), control2: CGPoint(x: w * 0.64, y: h * 0.34))
                    ctx.stroke(q, with: .color(.white), style: stroke)
                    ctx.fill(Path(ellipseIn: CGRect(x: w * 0.47, y: h * 0.65, width: w * 0.06, height: w * 0.06)), with: .color(.white))
                case .documentLines:
                    ctx.stroke(
                        Path(roundedRect: CGRect(x: w * 0.24, y: h * 0.18, width: w * 0.52, height: h * 0.64), cornerRadius: w * 0.08),
                        with: .color(.white),
                        style: stroke
                    )
                    var lines = Path()
                    for y in [0.36, 0.50, 0.64] {
                        lines.move(to: CGPoint(x: w * 0.36, y: h * y))
                        lines.addLine(to: CGPoint(x: w * 0.64, y: h * y))
                    }
                    ctx.stroke(lines, with: .color(.white), style: stroke)
                case .infoCircle:
                    ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.18, width: w * 0.64, height: h * 0.64)), with: .color(.white), style: stroke)
                    ctx.fill(Path(ellipseIn: CGRect(x: w * 0.47, y: h * 0.32, width: w * 0.06, height: w * 0.06)), with: .color(.white))
                    var stem = Path()
                    stem.move(to: CGPoint(x: w * 0.50, y: h * 0.45))
                    stem.addLine(to: CGPoint(x: w * 0.50, y: h * 0.68))
                    ctx.stroke(stem, with: .color(.white), style: stroke)
                case .cloudSync:
                    var cloud = Path()
                    cloud.move(to: CGPoint(x: w * 0.28, y: h * 0.68))
                    cloud.addLine(to: CGPoint(x: w * 0.74, y: h * 0.68))
                    cloud.addCurve(to: CGPoint(x: w * 0.73, y: h * 0.45), control1: CGPoint(x: w * 0.88, y: h * 0.68), control2: CGPoint(x: w * 0.88, y: h * 0.46))
                    cloud.addCurve(to: CGPoint(x: w * 0.37, y: h * 0.42), control1: CGPoint(x: w * 0.71, y: h * 0.26), control2: CGPoint(x: w * 0.43, y: h * 0.24))
                    cloud.addCurve(to: CGPoint(x: w * 0.28, y: h * 0.68), control1: CGPoint(x: w * 0.18, y: h * 0.42), control2: CGPoint(x: w * 0.14, y: h * 0.68))
                    cloud.closeSubpath()
                    ctx.stroke(cloud, with: .color(.white), style: stroke)
                    var arrow = Path()
                    arrow.move(to: CGPoint(x: w * 0.50, y: h * 0.46))
                    arrow.addLine(to: CGPoint(x: w * 0.50, y: h * 0.62))
                    arrow.move(to: CGPoint(x: w * 0.43, y: h * 0.53))
                    arrow.addLine(to: CGPoint(x: w * 0.50, y: h * 0.45))
                    arrow.addLine(to: CGPoint(x: w * 0.57, y: h * 0.53))
                    ctx.stroke(arrow, with: .color(.white), style: stroke)
                case .bookmarkLink:
                    var bm = Path()
                    bm.move(to: CGPoint(x: w * 0.28, y: h * 0.18))
                    bm.addLine(to: CGPoint(x: w * 0.72, y: h * 0.18))
                    bm.addLine(to: CGPoint(x: w * 0.72, y: h * 0.82))
                    bm.addLine(to: CGPoint(x: w * 0.50, y: h * 0.65))
                    bm.addLine(to: CGPoint(x: w * 0.28, y: h * 0.82))
                    bm.closeSubpath()
                    ctx.stroke(bm, with: .color(.white), style: stroke)
                }
            }
            .frame(width: size * 0.56, height: size * 0.56)
        }
    }
}

private struct EnableInteractivePopGestureBridge: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> PopGestureVC {
        PopGestureVC()
    }
    func updateUIViewController(_ uiViewController: PopGestureVC, context: Context) {}

    final class PopGestureVC: UIViewController, UIGestureRecognizerDelegate {
        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            navigationController?.interactivePopGestureRecognizer?.isEnabled = true
            navigationController?.interactivePopGestureRecognizer?.delegate = self
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            (navigationController?.viewControllers.count ?? 0) > 1
        }
    }
}
