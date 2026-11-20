import AuthenticationServices
import SwiftUI

struct TelefamIosPaperPlaneLogo: View {
    let size: CGFloat
    var inverted: Bool = false

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: inverted
                            ? [Color.white, Color(white: 0.94)]
                            : [TelefamPalette.crimsonBright, TelefamPalette.crimson],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .shadow(color: .black.opacity(0.18), radius: 10, x: 0, y: 4)

            Canvas { ctx, canvasSize in
                let w = canvasSize.width
                let h = canvasSize.height
                var outer = Path()
                outer.move(to: CGPoint(x: w * 0.77, y: h * 0.23))
                outer.addLine(to: CGPoint(x: w * 0.21, y: h * 0.48))
                outer.addLine(to: CGPoint(x: w * 0.44, y: h * 0.57))
                outer.addLine(to: CGPoint(x: w * 0.56, y: h * 0.80))
                outer.closeSubpath()

                let tint = inverted ? TelefamPalette.crimson : Color.white
                ctx.fill(outer, with: .color(tint))

                var fold = Path()
                fold.move(to: CGPoint(x: w * 0.77, y: h * 0.23))
                fold.addLine(to: CGPoint(x: w * 0.44, y: h * 0.57))
                fold.addLine(to: CGPoint(x: w * 0.48, y: h * 0.69))
                fold.closeSubpath()
                ctx.fill(fold, with: .color(tint.opacity(0.62)))
            }
        }
        .frame(width: size, height: size)
    }
}

struct TelefamIosBackgroundWaves: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let waveTint = colorScheme == .dark
                ? TelefamPalette.crimson.opacity(0.16)
                : TelefamPalette.blushLight.opacity(0.85)

            var topWave = Path()
            topWave.move(to: CGPoint(x: 0, y: 0))
            topWave.addLine(to: CGPoint(x: w, y: 0))
            topWave.addLine(to: CGPoint(x: w, y: h * 0.19))
            topWave.addCurve(
                to: CGPoint(x: 0, y: h * 0.25),
                control1: CGPoint(x: w * 0.68, y: h * 0.14),
                control2: CGPoint(x: w * 0.34, y: h * 0.30)
            )
            topWave.closeSubpath()
            ctx.fill(topWave, with: .color(waveTint))

            var bottomWave = Path()
            bottomWave.move(to: CGPoint(x: 0, y: h))
            bottomWave.addLine(to: CGPoint(x: w, y: h))
            bottomWave.addLine(to: CGPoint(x: w, y: h * 0.80))
            bottomWave.addCurve(
                to: CGPoint(x: 0, y: h * 0.86),
                control1: CGPoint(x: w * 0.64, y: h * 0.90),
                control2: CGPoint(x: w * 0.28, y: h * 0.76)
            )
            bottomWave.closeSubpath()
            ctx.fill(bottomWave, with: .color(waveTint))
        }
        .ignoresSafeArea()
    }
}

struct TelefamIosAuthView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var sessionStore: TelefamIosSessionStore
    @Binding var themeMode: TelefamThemeMode

    @State private var emailInput: String = ""
    @State private var otpCode: String = ""
    @State private var magicLinkToken: String = ""
    @State private var googleIdTokenInput: String = ""
    @State private var serverInput: String = ""
    @State private var showServerConfigSheet: Bool = false
    @State private var showMagicLinkSheet: Bool = false
    @State private var showGoogleTokenSheet: Bool = false
    @State private var emailCheckTask: Task<Void, Never>? = nil

    private let appleCoordinator = AppleSignInCoordinator()

    var body: some View {
        GeometryReader { proxy in
            let maxCardWidth: CGFloat = proxy.size.width > 600 ? 430 : min(proxy.size.width - 36, 400)
            let isShort = proxy.size.height < 660

            ZStack {
                if let wait = sessionStore.approvalWaitState {
                    TelefamIosDeviceApprovalWaitView(
                        waitState: wait,
                        isBusy: sessionStore.isBusy,
                        errorMessage: sessionStore.authError,
                        onBackChangeEmail: {
                            otpCode = ""
                            sessionStore.resetToEmailStep()
                        },
                        onTryAnotherWayGoogle: {
                            sessionStore.prepareTryAnotherWayGoogleSignIn()
                            showGoogleTokenSheet = true
                        }
                    )
                } else {
                    TelefamPalette.background(colorScheme).ignoresSafeArea()
                    TelefamIosBackgroundWaves()

                    VStack {
                        HStack {
                            Spacer()
                            Button {
                                themeMode = themeMode.next
                            } label: {
                                HStack(spacing: 5) {
                                    Image(systemName: themeMode == .dark ? "moon.fill" : (themeMode == .light ? "sun.max.fill" : "circle.lefthalf.filled"))
                                        .font(.system(size: 13, weight: .semibold))
                                    Text(themeMode.label)
                                        .font(.system(size: 12, weight: .semibold))
                                }
                                .foregroundColor(TelefamPalette.crimson)
                                .padding(.horizontal, 12)
                                .frame(height: 36)
                                .background(TelefamPalette.surface(colorScheme).opacity(0.92))
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 10)
                        Spacer()
                    }

                    ScrollView(showsIndicators: false) {
                        VStack(spacing: isShort ? 10 : 14) {
                            Spacer(minLength: isShort ? 36 : 56)

                            TelefamIosPaperPlaneLogo(size: isShort ? 62 : 74)

                            Text("Telefam")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(TelefamPalette.crimson)

                            Text("Stay connected. Stay together.")
                                .font(.system(size: 13))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))

                            VStack(spacing: 14) {
                                if !sessionStore.otpSent {
                                    emailCardContent
                                } else {
                                    otpCardContent
                                }
                            }
                            .padding(24)
                            .frame(width: maxCardWidth)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(TelefamPalette.border(colorScheme), lineWidth: 1)
                            )
                            .shadow(color: .black.opacity(colorScheme == .dark ? 0.38 : 0.08), radius: 18, x: 0, y: 6)

                            if sessionStore.serverURL == nil {
                                Button {
                                    serverInput = sessionStore.baseUrlString
                                    showServerConfigSheet = true
                                } label: {
                                    Label("Configure server endpoint", systemImage: "server.rack")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(TelefamPalette.crimson)
                                }
                                .padding(.top, 6)
                            }

                            Spacer(minLength: 28)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .onAppear {
            if emailInput.isEmpty && !sessionStore.email.isEmpty {
                emailInput = sessionStore.email
            }
        }
        .sheet(isPresented: $showServerConfigSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    Text(TelefamIosSecurityGuard.isReleaseEnforced() ? "Enter your HTTPS Telefam server URL." : "Enter your Telefam server URL.")
                        .font(.system(size: 13.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    TextField("https://api.example.com", text: $serverInput)
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)
                        .keyboardType(.URL)
                        .padding(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    Button("Save") {
                        if sessionStore.updateServerEndpoint(serverInput) {
                            showServerConfigSheet = false
                        }
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(TelefamPalette.crimson)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    Spacer()
                }
                .padding(20)
                .navigationTitle("Server Endpoint")
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showMagicLinkSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Paste the verification token from your Telefam sign-in email.")
                        .font(.system(size: 13.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    TextField("Verification token", text: $magicLinkToken)
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)
                        .padding(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    Button("Verify link token") {
                        Task {
                            await sessionStore.verifyMagicLink(token: magicLinkToken)
                            if sessionStore.isAuthenticated {
                                showMagicLinkSheet = false
                            }
                        }
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(TelefamPalette.crimson)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    Spacer()
                }
                .padding(20)
                .navigationTitle("Verify Link")
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showGoogleTokenSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    if let expected = sessionStore.tryAnotherWayExpectedEmail, !expected.isEmpty {
                        Text("Verify using Google account: \(expected)")
                            .font(.system(size: 13.5, weight: .semibold))
                            .foregroundColor(TelefamPalette.crimson)
                    }
                    Text("Paste your Google ID token for server OAuth verification.")
                        .font(.system(size: 13.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    TextField("Google ID Token", text: $googleIdTokenInput)
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)
                        .padding(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    if let error = sessionStore.authError, !error.isEmpty {
                        Text(error)
                            .font(.system(size: 12.5))
                            .foregroundColor(TelefamPalette.crimson)
                    }
                    Button {
                        let tok = googleIdTokenInput.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !tok.isEmpty else { return }
                        Task {
                            await sessionStore.verifyGoogleOAuth(identityToken: tok)
                            if sessionStore.isAuthenticated {
                                showGoogleTokenSheet = false
                            }
                        }
                    } label: {
                        if sessionStore.isBusy {
                            ProgressView().tint(.white)
                        } else {
                            Text("Verify Google Account")
                        }
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(TelefamPalette.crimson)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .disabled(sessionStore.isBusy)
                    Spacer()
                }
                .padding(20)
                .navigationTitle("Google Sign-In")
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
        }
    }

    private var emailCardContent: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Enter Your Email")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(TelefamPalette.crimson)
                .frame(maxWidth: .infinity, alignment: .center)

            Text("Enter your email to get OTP")
                .font(.system(size: 13))
                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                .frame(maxWidth: .infinity, alignment: .center)

            Text("Email")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))

            HStack(spacing: 10) {
                Image(systemName: "envelope")
                    .foregroundColor(TelefamPalette.crimson)
                TextField("Enter your email", text: $emailInput)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .keyboardType(.emailAddress)
                    .submitLabel(.done)
                    .onChange(of: emailInput) { newVal in
                        sessionStore.emailAccountNotice = nil
                        emailCheckTask?.cancel()
                        emailCheckTask = Task {
                            try? await Task.sleep(nanoseconds: 380_000_000)
                            if !Task.isCancelled {
                                await sessionStore.checkEmailStatus(newVal)
                            }
                        }
                    }
                    .onSubmit {
                        Task { await sessionStore.requestOtp(for: emailInput) }
                    }
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(TelefamPalette.surface(colorScheme))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

            if let notice = sessionStore.emailAccountNotice {
                Text(notice)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(TelefamPalette.crimson)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(TelefamPalette.crimson.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            }

            if let error = sessionStore.authError {
                Text(error)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.crimson)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }

            Button {
                Task { await sessionStore.requestOtp(for: emailInput) }
            } label: {
                if sessionStore.isBusy {
                    ProgressView().tint(.white)
                } else {
                    Text("Send OTP")
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(TelefamPalette.crimson)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .disabled(sessionStore.isBusy)

            HStack {
                Rectangle().fill(TelefamPalette.border(colorScheme)).frame(height: 1)
                Text("or")
                    .font(.system(size: 12))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .padding(.horizontal, 8)
                Rectangle().fill(TelefamPalette.border(colorScheme)).frame(height: 1)
            }

            Button {
                showGoogleTokenSheet = true
            } label: {
                HStack(spacing: 10) {
                    googleBadgeSvg
                    Text("Continue with Google")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                }
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(TelefamPalette.surface(colorScheme))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
            }
            .disabled(sessionStore.isBusy)

            SignInWithAppleButton(.continue) { request in
                request.requestedScopes = [.fullName, .email]
            } onCompletion: { _ in
                appleCoordinator.startSignInWithApple { result in
                    switch result {
                    case .success(let payload):
                        Task {
                            await sessionStore.verifyAppleOAuth(
                                identityToken: payload.identityToken,
                                nonce: payload.nonce
                            )
                        }
                    case .failure(let err):
                        sessionStore.authError = err.localizedDescription
                    }
                }
            }
            .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
            .frame(height: 48)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private var otpCardContent: some View {
        VStack(spacing: 14) {
            Text("Enter OTP")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(TelefamPalette.crimson)

            Text("We sent a verification code to\n\(sessionStore.email)")
                .font(.system(size: 13))
                .multilineTextAlignment(.center)
                .foregroundColor(TelefamPalette.textMuted(colorScheme))

            Text("Check your inbox or use the sign-in link in your email.")
                .font(.system(size: 11.5))
                .multilineTextAlignment(.center)
                .foregroundColor(TelefamPalette.textMuted(colorScheme))

            ZStack {
                HStack(spacing: 8) {
                    ForEach(0..<6, id: \.self) { idx in
                        let char = idx < otpCode.count ? String(otpCode[otpCode.index(otpCode.startIndex, offsetBy: idx)]) : ""
                        let isFocused = otpCode.count == idx || (otpCode.count == 6 && idx == 5)
                        Text(char)
                            .font(.system(size: 20, weight: .bold, design: .monospaced))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(TelefamPalette.surfaceVariant(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(isFocused ? TelefamPalette.crimson : TelefamPalette.border(colorScheme), lineWidth: isFocused ? 1.8 : 1)
                            )
                    }
                }
                TextField("", text: $otpCode)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .foregroundColor(.clear)
                    .accentColor(.clear)
                    .frame(height: 50)
                    .onChange(of: otpCode) { newValue in
                        let filtered = String(newValue.filter(\.isNumber).prefix(6))
                        if filtered != otpCode {
                            otpCode = filtered
                        }
                    }
            }

            if let error = sessionStore.authError {
                Text(error)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.crimson)
                    .multilineTextAlignment(.center)
            }

            Button {
                Task { await sessionStore.verifyOtp(code: otpCode) }
            } label: {
                if sessionStore.isBusy {
                    ProgressView().tint(.white)
                } else {
                    Text("Verify OTP")
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(TelefamPalette.crimson)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .disabled(sessionStore.isBusy || otpCode.count < 6)

            HStack(spacing: 14) {
                Button {
                    Task { await sessionStore.requestOtp(for: sessionStore.email) }
                } label: {
                    if sessionStore.cooldownRemainingSec > 0 {
                        Text("Resend in \(sessionStore.cooldownRemainingSec)s")
                    } else {
                        Text("Resend OTP")
                    }
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(sessionStore.cooldownRemainingSec > 0 ? TelefamPalette.textMuted(colorScheme) : TelefamPalette.crimson)
                .disabled(sessionStore.cooldownRemainingSec > 0 || sessionStore.isBusy)

                Button("Use link token") {
                    showMagicLinkSheet = true
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(TelefamPalette.crimson)
            }

            Button("Change email") {
                otpCode = ""
                sessionStore.resetToEmailStep()
            }
            .foregroundColor(TelefamPalette.crimson)
            .font(.system(size: 13, weight: .semibold))
        }
    }

    private var googleBadgeSvg: some View {
        Canvas { ctx, size in
            let s = min(size.width, size.height)
            let rect = CGRect(x: 2, y: 2, width: s - 4, height: s - 4)
            let stroke = StrokeStyle(lineWidth: 2.8, lineCap: .round)

            var blue = Path()
            blue.addArc(center: CGPoint(x: s / 2, y: s / 2), radius: (s - 4) / 2, startAngle: .degrees(-15), endAngle: .degrees(55), clockwise: false)
            ctx.stroke(blue, with: .color(Color(red: 0.259, green: 0.522, blue: 0.957)), style: stroke)

            var green = Path()
            green.addArc(center: CGPoint(x: s / 2, y: s / 2), radius: (s - 4) / 2, startAngle: .degrees(55), endAngle: .degrees(145), clockwise: false)
            ctx.stroke(green, with: .color(Color(red: 0.204, green: 0.659, blue: 0.325)), style: stroke)

            var yellow = Path()
            yellow.addArc(center: CGPoint(x: s / 2, y: s / 2), radius: (s - 4) / 2, startAngle: .degrees(145), endAngle: .degrees(215), clockwise: false)
            ctx.stroke(yellow, with: .color(Color(red: 0.984, green: 0.737, blue: 0.016)), style: stroke)

            var red = Path()
            red.addArc(center: CGPoint(x: s / 2, y: s / 2), radius: (s - 4) / 2, startAngle: .degrees(215), endAngle: .degrees(320), clockwise: false)
            ctx.stroke(red, with: .color(Color(red: 0.918, green: 0.263, blue: 0.208)), style: stroke)

            var bar = Path()
            bar.move(to: CGPoint(x: s * 0.52, y: s * 0.50))
            bar.addLine(to: CGPoint(x: rect.maxX, y: s * 0.50))
            ctx.stroke(bar, with: .color(Color(red: 0.259, green: 0.522, blue: 0.957)), style: StrokeStyle(lineWidth: 2.4, lineCap: .round))
        }
        .frame(width: 20, height: 20)
    }
}
