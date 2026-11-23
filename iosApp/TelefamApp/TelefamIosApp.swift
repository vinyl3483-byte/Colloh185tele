import SwiftUI

@main
struct TelefamIosApp: App {
    @UIApplicationDelegateAdaptor(TelefamIosAppDelegate.self) private var appDelegate
    @StateObject private var sessionStore = TelefamIosSessionStore()
    @StateObject private var smartLinksCoordinator = TelefamIosSmartLinksCoordinator()
    @StateObject private var notificationManager = TelefamIosNotificationManager.shared
    @AppStorage("telefam_ios_theme_mode") private var themeModeRaw: String = TelefamThemeMode.system.rawValue
    @State private var isSplashing: Bool = true
    @State private var deepLinkBlockedToast: String? = nil

    private var themeModeBinding: Binding<TelefamThemeMode> {
        Binding(
            get: { TelefamThemeMode(rawValue: themeModeRaw) ?? .system },
            set: { themeModeRaw = $0.rawValue }
        )
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                if isSplashing {
                    splashView
                } else if !sessionStore.isAuthenticated {
                    TelefamIosAuthView(
                        sessionStore: sessionStore,
                        themeMode: themeModeBinding
                    )
                } else if !sessionStore.profileCompleted {
                    TelefamIosProfileSetupView(
                        sessionStore: sessionStore,
                        themeMode: themeModeBinding
                    )
                } else if let serverURL = sessionStore.serverURL {
                    TelefamIosHomeView(
                        sessionStore: sessionStore,
                        serverURL: serverURL,
                        themeMode: themeModeBinding
                    )
                } else {
                    TelefamIosAuthView(
                        sessionStore: sessionStore,
                        themeMode: themeModeBinding
                    )
                }

                if let deepLinkBlockedToast {
                    VStack {
                        Spacer()
                        Text(deepLinkBlockedToast)
                            .font(.system(size: 13.5, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 18)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.84))
                            .clipShape(Capsule())
                            .padding(.bottom, 40)
                    }
                }
            }
            .environmentObject(smartLinksCoordinator)
            .preferredColorScheme((TelefamThemeMode(rawValue: themeModeRaw) ?? .system).preferredColorScheme)
            .task {
                if TelefamIosSecurityGuard.isReleaseEnforced() && TelefamIosSecurityGuard.isDebuggerAttached() {
                    sessionStore.signOut()
                }
                try? await Task.sleep(nanoseconds: 550_000_000)
                withAnimation(.easeOut(duration: 0.25)) {
                    isSplashing = false
                }
            }
            .onOpenURL { incomingURL in
                let lower = incomingURL.absoluteString.lowercased()
                if lower.contains("verification") && lower.contains("payment-return") {
                    NotificationCenter.default.post(
                        name: .telefamVerificationPaymentReturnUrl,
                        object: nil,
                        userInfo: ["url": incomingURL.absoluteString]
                    )
                    return
                }
                if lower.contains("stars") || lower.contains("payment-return") {
                    NotificationCenter.default.post(
                        name: .telefamStarsPaymentReturnUrl,
                        object: nil,
                        userInfo: ["url": incomingURL.absoluteString]
                    )
                    return
                }
                guard let comps = URLComponents(url: incomingURL, resolvingAgainstBaseURL: false),
                      let token = comps.queryItems?.first(where: { $0.name == "token" })?.value,
                      !token.isEmpty else {
                    return
                }
                Task {
                    await sessionStore.verifyMagicLink(token: token)
                    if sessionStore.authError != nil {
                        deepLinkBlockedToast = sessionStore.authError
                    }
                }
            }
        }
    }

    private var splashView: some View {
        ZStack {
            TelefamPalette.crimson.ignoresSafeArea()
            VStack(spacing: 14) {
                TelefamIosPaperPlaneLogo(size: 86, inverted: true)
                Text("Telefam")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.white)
                Text("Stay connected. Stay together.")
                    .font(.system(size: 13.5, weight: .medium))
                    .foregroundColor(Color.white.opacity(0.88))
            }
        }
    }
}
