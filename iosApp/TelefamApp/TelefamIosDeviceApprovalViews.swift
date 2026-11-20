import SwiftUI

private enum IosSecurityColors {
    static let bg = Color(red: 7.0 / 255.0, green: 4.0 / 255.0, blue: 5.0 / 255.0)
    static let cardBg = Color(red: 19.0 / 255.0, green: 11.0 / 255.0, blue: 13.0 / 255.0)
    static let border = Color.white.opacity(0.15)
    static let crimsonGlow = Color(red: 239.0 / 255.0, green: 35.0 / 255.0, blue: 60.0 / 255.0)
    static let crimsonRich = Color(red: 217.0 / 255.0, green: 30.0 / 255.0, blue: 42.0 / 255.0)
    static let crimsonDeep = Color(red: 125.0 / 255.0, green: 9.0 / 255.0, blue: 20.0 / 255.0)
    static let softWhite = Color(red: 249.0 / 255.0, green: 250.0 / 255.0, blue: 251.0 / 255.0)
    static let muted = Color(red: 156.0 / 255.0, green: 163.0 / 255.0, blue: 175.0 / 255.0)
    static let greenSuccess = Color(red: 34.0 / 255.0, green: 197.0 / 255.0, blue: 94.0 / 255.0)
}

struct TelefamIosDeviceApprovalWaitView: View {
    let waitState: TelefamIosDeviceApprovalWaitState
    let isBusy: Bool
    let errorMessage: String?
    let onBackChangeEmail: () -> Void
    let onTryAnotherWayGoogle: () -> Void

    var body: some View {
        GeometryReader { geo in
            let maxWidth: CGFloat = min(geo.size.width - 40, 440)
            let compact = geo.size.height < 690

            ZStack {
                IosSecurityColors.bg.ignoresSafeArea()
                IosSecurityRadialBackdrop(showWorldDots: false)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        HStack {
                            Button(action: onBackChangeEmail) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 19, weight: .semibold))
                                    .foregroundColor(IosSecurityColors.softWhite)
                                    .frame(width: 48, height: 48)
                            }
                            Spacer()
                            IosSecurityRoundedLogo(size: compact ? 52 : 60)
                            Spacer()
                            Menu {
                                Button("Verify with Google", action: onTryAnotherWayGoogle)
                                Button("Use another email", action: onBackChangeEmail)
                            } label: {
                                Image(systemName: "ellipsis")
                                    .rotationEffect(.degrees(90))
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(IosSecurityColors.softWhite)
                                    .frame(width: 48, height: 48)
                            }
                        }
                        .padding(.top, 8)

                        Text("Telefam")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .padding(.top, 8)

                        IosSmartphoneToClockArtwork()
                            .frame(height: compact ? 116 : 142)
                            .padding(.top, compact ? 10 : 18)

                        Text(waitState.isDeniedBlocked ? "Login request denied" : "We sent a request to")
                            .font(.system(size: 23, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .multilineTextAlignment(.center)
                            .padding(.top, 10)

                        Text(waitState.targetDeviceName.isEmpty ? "Your trusted device" : waitState.targetDeviceName)
                            .font(.system(size: 23, weight: .bold))
                            .foregroundColor(IosSecurityColors.crimsonGlow)
                            .multilineTextAlignment(.center)
                            .padding(.top, 2)

                        Text(
                            waitState.isDeniedBlocked
                            ? "This device was blocked by your trusted device.\nVerify with Google using your registered email."
                            : "Please click Accept in another device\nto confirm it's you."
                        )
                        .font(.system(size: 14.5))
                        .foregroundColor(IosSecurityColors.muted)
                        .multilineTextAlignment(.center)
                        .lineSpacing(3)
                        .padding(.top, 8)

                        if !waitState.isDeniedBlocked && waitState.remainingSeconds > 0 {
                            HStack(spacing: 6) {
                                Image(systemName: "clock")
                                    .font(.system(size: 13))
                                    .foregroundColor(IosSecurityColors.crimsonGlow)
                                Text("Waiting for approval • \(formatSecurityCountdown(waitState.remainingSeconds))")
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundColor(IosSecurityColors.muted)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(red: 28 / 255, green: 14 / 255, blue: 17 / 255))
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(IosSecurityColors.crimsonGlow.opacity(0.38), lineWidth: 1))
                            .padding(.top, 10)
                        }

                        VStack(spacing: 0) {
                            IosSecurityDetailRow(
                                systemIcon: "iphone",
                                label: "Device",
                                value: waitState.targetDeviceName.isEmpty ? "Trusted Device" : waitState.targetDeviceName,
                                redBadge: false
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "mappin.and.ellipse",
                                label: "Location",
                                value: waitState.targetLocation.isEmpty ? "Unknown location" : waitState.targetLocation,
                                redBadge: false
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "clock",
                                label: "Time",
                                value: formatSecurityTimestamp(waitState.requestTimeMs),
                                redBadge: false
                            )
                        }
                        .background(IosSecurityColors.cardBg.opacity(0.90))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(IosSecurityColors.border, lineWidth: 1))
                        .padding(.top, compact ? 14 : 18)

                        if let err = errorMessage, !err.isEmpty {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.circle")
                                    .foregroundColor(Color(red: 252 / 255, green: 165 / 255, blue: 165 / 255))
                                Text(err)
                                    .font(.system(size: 12.5))
                                    .foregroundColor(Color(red: 254 / 255, green: 226 / 255, blue: 226 / 255))
                                Spacer()
                            }
                            .padding(12)
                            .background(Color(red: 43 / 255, green: 11 / 255, blue: 15 / 255))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(IosSecurityColors.crimsonGlow.opacity(0.55), lineWidth: 1))
                            .padding(.top, 12)
                        }

                        Button(action: onTryAnotherWayGoogle) {
                            HStack(spacing: 12) {
                                if isBusy {
                                    ProgressView().tint(IosSecurityColors.crimsonGlow)
                                } else {
                                    ZStack {
                                        Circle()
                                            .fill(IosSecurityColors.crimsonDeep.opacity(0.35))
                                            .frame(width: 26, height: 26)
                                            .overlay(Circle().stroke(IosSecurityColors.crimsonGlow, lineWidth: 1.5))
                                        Image(systemName: "ellipsis")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(IosSecurityColors.crimsonGlow)
                                    }
                                    Text("Try Another Way")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(IosSecurityColors.softWhite)
                                }
                            }
                            .frame(maxWidth: .infinity, minHeight: 54)
                            .background(Color(red: 23 / 255, green: 7 / 255, blue: 9 / 255))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(IosSecurityColors.crimsonRich, lineWidth: 1.5))
                        }
                        .disabled(isBusy)
                        .padding(.top, compact ? 16 : 22)

                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.shield")
                                .font(.system(size: 15))
                                .foregroundColor(IosSecurityColors.muted)
                            Text("If you didn't request this, your account may be at risk.")
                                .font(.system(size: 12.5))
                                .foregroundColor(IosSecurityColors.muted)
                        }
                        .padding(.top, 18)
                        .padding(.bottom, 24)
                    }
                    .frame(width: maxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

struct TelefamIosIncomingLoginRequestView: View {
    let request: TelefamIosIncomingLoginRequest
    let isResponding: Bool
    let actionError: String?
    let onDeny: () -> Void
    let onAccept: () -> Void

    var body: some View {
        GeometryReader { geo in
            let maxWidth: CGFloat = min(geo.size.width - 36, 440)
            let compact = geo.size.height < 690

            ZStack {
                IosSecurityColors.bg.ignoresSafeArea()
                IosSecurityRadialBackdrop(showWorldDots: true)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        IosSecurityRoundedLogo(size: compact ? 52 : 60)
                            .padding(.top, compact ? 10 : 20)

                        Text("Telefam")
                            .font(.system(size: 21, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .padding(.top, 8)

                        ZStack {
                            Image(systemName: "shield.fill")
                                .font(.system(size: compact ? 56 : 66))
                                .foregroundStyle(
                                    LinearGradient(
                                        colors: [IosSecurityColors.crimsonGlow, IosSecurityColors.crimsonDeep],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .shadow(color: IosSecurityColors.crimsonGlow.opacity(0.45), radius: 14, x: 0, y: 4)
                            Image(systemName: "lock.fill")
                                .font(.system(size: compact ? 22 : 26))
                                .foregroundColor(IosSecurityColors.softWhite)
                        }
                        .padding(.top, compact ? 12 : 18)

                        Text("New login request")
                            .font(.system(size: 25, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .padding(.top, 12)

                        (
                            Text("Someone is trying to log in to your\n")
                                .foregroundColor(IosSecurityColors.muted)
                            + Text("Telefam")
                                .foregroundColor(IosSecurityColors.crimsonGlow)
                                .fontWeight(.semibold)
                            + Text(" account on another device.")
                                .foregroundColor(IosSecurityColors.muted)
                        )
                        .font(.system(size: 14.5))
                        .multilineTextAlignment(.center)
                        .lineSpacing(3)
                        .padding(.top, 6)

                        VStack(spacing: 0) {
                            IosSecurityDetailRow(
                                systemIcon: "iphone",
                                label: "Device",
                                value: request.deviceName.isEmpty ? "Unknown Device" : request.deviceName,
                                redBadge: true
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "mappin.and.ellipse",
                                label: "Location",
                                value: "\(request.location.isEmpty ? "Approximate location unavailable" : request.location) (Approximate)",
                                redBadge: true
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "clock",
                                label: "Time",
                                value: formatSecurityTimestamp(request.requestedAtMs),
                                redBadge: true
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "globe",
                                label: "IP Address",
                                value: request.ipAddressMasked.isEmpty ? "Hidden" : request.ipAddressMasked,
                                redBadge: true
                            )
                        }
                        .background(IosSecurityColors.cardBg.opacity(0.90))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(IosSecurityColors.border, lineWidth: 1))
                        .padding(.top, 16)

                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(IosSecurityColors.crimsonRich.opacity(0.26))
                                    .frame(width: 34, height: 34)
                                    .overlay(Circle().stroke(IosSecurityColors.crimsonGlow.opacity(0.65), lineWidth: 1))
                                Image(systemName: "checkmark.shield.fill")
                                    .font(.system(size: 15))
                                    .foregroundColor(IosSecurityColors.crimsonGlow)
                            }
                            (
                                Text("If this wasn't you,").fontWeight(.semibold).foregroundColor(IosSecurityColors.softWhite)
                                + Text(" you can deny this request and secure your account.")
                                    .foregroundColor(Color(white: 0.88))
                            )
                            .font(.system(size: 13))
                            Spacer()
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color(red: 26 / 255, green: 9 / 255, blue: 11 / 255))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(IosSecurityColors.crimsonGlow.opacity(0.40), lineWidth: 1))
                        .padding(.top, 12)

                        VStack(spacing: 3) {
                            HStack(spacing: 6) {
                                Image(systemName: "clock")
                                    .font(.system(size: 13))
                                    .foregroundColor(IosSecurityColors.crimsonGlow)
                                Text("Request expires in")
                                    .font(.system(size: 12.5))
                                    .foregroundColor(IosSecurityColors.muted)
                            }
                            Text(formatSecurityCountdown(request.remainingSeconds))
                                .font(.system(size: 24, weight: .bold, design: .monospaced))
                                .foregroundColor(IosSecurityColors.softWhite)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(IosSecurityColors.cardBg.opacity(0.90))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(IosSecurityColors.border, lineWidth: 1))
                        .padding(.top, 10)

                        if let err = actionError, !err.isEmpty {
                            Text(err)
                                .font(.system(size: 12.5))
                                .foregroundColor(Color(red: 252 / 255, green: 165 / 255, blue: 165 / 255))
                                .padding(.top, 8)
                        }

                        HStack(spacing: 12) {
                            Button(action: onDeny) {
                                HStack(spacing: 8) {
                                    Image(systemName: "xmark.circle")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(IosSecurityColors.crimsonGlow)
                                    Text("Deny")
                                        .font(.system(size: 15.5, weight: .semibold))
                                        .foregroundColor(Color(red: 248 / 255, green: 113 / 255, blue: 113 / 255))
                                }
                                .frame(maxWidth: .infinity, minHeight: 52)
                                .background(Color(red: 23 / 255, green: 7 / 255, blue: 9 / 255))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(IosSecurityColors.crimsonGlow.opacity(0.75), lineWidth: 1.5))
                            }
                            .disabled(isResponding)

                            Button(action: onAccept) {
                                HStack(spacing: 8) {
                                    if isResponding {
                                        ProgressView().tint(IosSecurityColors.softWhite)
                                    } else {
                                        Image(systemName: "checkmark.circle.fill")
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundColor(IosSecurityColors.softWhite)
                                        Text("Accept")
                                            .font(.system(size: 15.5, weight: .semibold))
                                            .foregroundColor(IosSecurityColors.softWhite)
                                    }
                                }
                                .frame(maxWidth: .infinity, minHeight: 52)
                                .background(
                                    LinearGradient(
                                        colors: [IosSecurityColors.crimsonGlow, IosSecurityColors.crimsonDeep],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .disabled(isResponding)
                        }
                        .padding(.top, 14)

                        HStack(spacing: 6) {
                            Image(systemName: "lock")
                                .font(.system(size: 12))
                                .foregroundColor(IosSecurityColors.muted)
                            Text("We'll never share your information")
                                .font(.system(size: 12))
                                .foregroundColor(IosSecurityColors.muted)
                        }
                        .padding(.top, 14)
                        .padding(.bottom, 22)
                    }
                    .frame(width: maxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

struct TelefamIosLoginApprovedView: View {
    let approved: TelefamIosLoginApprovedState
    let onDone: () -> Void

    var body: some View {
        GeometryReader { geo in
            let maxWidth: CGFloat = min(geo.size.width - 36, 440)

            ZStack {
                IosSecurityColors.bg.ignoresSafeArea()
                IosSecurityRadialBackdrop(showWorldDots: false)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        IosSecurityRoundedLogo(size: 62)
                            .padding(.top, 24)

                        Text("Telefam")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .padding(.top, 10)

                        IosApprovedCheckEmblem()
                            .frame(width: 120, height: 120)
                            .padding(.top, 24)

                        Text("Login Approved!")
                            .font(.system(size: 25, weight: .bold))
                            .foregroundColor(IosSecurityColors.softWhite)
                            .padding(.top, 20)

                        Text("You have successfully approved the login\non another device.")
                            .font(.system(size: 14.5))
                            .foregroundColor(IosSecurityColors.muted)
                            .multilineTextAlignment(.center)
                            .lineSpacing(3)
                            .padding(.top, 8)

                        VStack(spacing: 0) {
                            IosSecurityDetailRow(
                                systemIcon: "iphone",
                                label: "Device",
                                value: approved.deviceName.isEmpty ? "Approved Device" : approved.deviceName,
                                redBadge: false
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "mappin.and.ellipse",
                                label: "Location",
                                value: approved.location.isEmpty ? "Approximate location" : approved.location,
                                redBadge: false
                            )
                            Divider().background(Color.white.opacity(0.08))
                            IosSecurityDetailRow(
                                systemIcon: "clock",
                                label: "Time",
                                value: formatSecurityTimestamp(approved.requestedAtMs),
                                redBadge: false
                            )
                        }
                        .background(IosSecurityColors.cardBg.opacity(0.90))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(IosSecurityColors.border, lineWidth: 1))
                        .padding(.top, 22)

                        HStack(spacing: 12) {
                            Image(systemName: "checkmark.shield.fill")
                                .font(.system(size: 22))
                                .foregroundColor(IosSecurityColors.greenSuccess)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("All set!")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(IosSecurityColors.softWhite)
                                Text("The new device will now be able to access your Telefam account.")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(white: 0.85))
                            }
                            Spacer()
                        }
                        .padding(14)
                        .background(Color(red: 11 / 255, green: 34 / 255, blue: 18 / 255))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(IosSecurityColors.greenSuccess.opacity(0.45), lineWidth: 1))
                        .padding(.top, 14)

                        Button(action: onDone) {
                            Text("Done")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(IosSecurityColors.softWhite)
                                .frame(maxWidth: .infinity, minHeight: 52)
                                .background(
                                    LinearGradient(
                                        colors: [IosSecurityColors.crimsonGlow, IosSecurityColors.crimsonDeep],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .padding(.top, 24)
                        .padding(.bottom, 26)
                    }
                    .frame(width: maxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

private struct IosSecurityRoundedLogo: View {
    let size: CGFloat

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.28)
                .fill(
                    LinearGradient(
                        colors: [Color(red: 244 / 255, green: 42 / 255, blue: 62 / 255), Color(red: 161 / 255, green: 9 / 255, blue: 22 / 255)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .shadow(color: IosSecurityColors.crimsonGlow.opacity(0.42), radius: 14, x: 0, y: 4)

            Canvas { ctx, s in
                let w = s.width
                let h = s.height
                var plane = Path()
                plane.move(to: CGPoint(x: w * 0.77, y: h * 0.24))
                plane.addLine(to: CGPoint(x: w * 0.20, y: h * 0.49))
                plane.addLine(to: CGPoint(x: w * 0.45, y: h * 0.58))
                plane.addLine(to: CGPoint(x: w * 0.57, y: h * 0.80))
                plane.closeSubpath()
                ctx.fill(plane, with: .color(.white))
            }
        }
        .frame(width: size, height: size)
    }
}

private struct IosSecurityRadialBackdrop: View {
    let showWorldDots: Bool

    var body: some View {
        Canvas { ctx, size in
            let center = CGPoint(x: size.width * 0.5, y: size.height * 0.30)
            ctx.fill(
                Path(CGRect(origin: .zero, size: size)),
                with: .radialGradient(
                    Gradient(colors: [
                        IosSecurityColors.crimsonDeep.opacity(0.42),
                        IosSecurityColors.bg,
                        Color(red: 5 / 255, green: 2 / 255, blue: 3 / 255)
                    ]),
                    center: center,
                    startRadius: 10,
                    endRadius: size.width * 0.92
                )
            )
            if showWorldDots {
                let cols = 22
                let rows = 10
                let dotColor = IosSecurityColors.crimsonGlow.opacity(0.22)
                for r in 0..<rows {
                    for c in 0..<cols {
                        let nx = Double(c) / Double(cols - 1)
                        let ny = Double(r) / Double(rows - 1)
                        let inCluster =
                            (nx > 0.08 && nx < 0.36 && ny > 0.14 && ny < 0.65) ||
                            (nx > 0.44 && nx < 0.88 && ny > 0.10 && ny < 0.72)
                        if inCluster && (r + c) % 2 == 0 {
                            let pt = CGPoint(x: size.width * (0.04 + nx * 0.92), y: size.height * (0.06 + ny * 0.36))
                            ctx.fill(Path(ellipseIn: CGRect(x: pt.x - 1.5, y: pt.y - 1.5, width: 3, height: 3)), with: .color(dotColor))
                        }
                    }
                }
            }
        }
        .ignoresSafeArea()
    }
}

private struct IosSmartphoneToClockArtwork: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height

            ctx.fill(
                Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.10, width: w * 0.42, height: h * 0.80)),
                with: .radialGradient(
                    Gradient(colors: [IosSecurityColors.crimsonRich.opacity(0.32), .clear]),
                    center: CGPoint(x: w * 0.39, y: h * 0.50),
                    startRadius: 4,
                    endRadius: w * 0.25
                )
            )

            let phoneRect = CGRect(x: w * 0.24, y: h * 0.14, width: w * 0.25, height: h * 0.72)
            let phonePath = Path(roundedRect: phoneRect, cornerRadius: 14)
            ctx.fill(phonePath, with: .color(Color(red: 19 / 255, green: 13 / 255, blue: 16 / 255)))
            ctx.stroke(phonePath, with: .color(Color(red: 110 / 255, green: 112 / 255, blue: 120 / 255)), lineWidth: 1.8)

            let badgeSize = min(phoneRect.width * 0.48, 38)
            let badgeRect = CGRect(
                x: phoneRect.midX - badgeSize * 0.5,
                y: phoneRect.midY - badgeSize * 0.5,
                width: badgeSize,
                height: badgeSize
            )
            ctx.fill(Path(roundedRect: badgeRect, cornerRadius: 9), with: .color(IosSecurityColors.crimsonRich))

            var dash = Path()
            dash.move(to: CGPoint(x: phoneRect.maxX + 8, y: phoneRect.minY + 14))
            dash.addQuadCurve(
                to: CGPoint(x: w * 0.72, y: h * 0.36),
                control: CGPoint(x: w * 0.60, y: h * 0.10)
            )
            ctx.stroke(dash, with: .color(IosSecurityColors.softWhite.opacity(0.8)), style: StrokeStyle(lineWidth: 1.8, lineCap: .round, dash: [4, 4]))

            let clockCenter = CGPoint(x: w * 0.75, y: h * 0.56)
            let clockR = min(w * 0.085, h * 0.22)
            ctx.stroke(
                Path(ellipseIn: CGRect(x: clockCenter.x - clockR, y: clockCenter.y - clockR, width: clockR * 2, height: clockR * 2)),
                with: .color(IosSecurityColors.crimsonGlow.opacity(0.75)),
                lineWidth: 2
            )
            var hands = Path()
            hands.move(to: clockCenter)
            hands.addLine(to: CGPoint(x: clockCenter.x, y: clockCenter.y - clockR * 0.52))
            hands.move(to: clockCenter)
            hands.addLine(to: CGPoint(x: clockCenter.x + clockR * 0.42, y: clockCenter.y))
            ctx.stroke(hands, with: .color(IosSecurityColors.softWhite), style: StrokeStyle(lineWidth: 2, lineCap: .round))
        }
    }
}

private struct IosApprovedCheckEmblem: View {
    var body: some View {
        ZStack {
            Canvas { ctx, size in
                let dots: [(CGFloat, CGFloat, CGFloat, Color)] = [
                    (0.14, 0.25, 3, IosSecurityColors.crimsonGlow),
                    (0.86, 0.22, 3, IosSecurityColors.muted),
                    (0.10, 0.72, 4, IosSecurityColors.crimsonGlow),
                    (0.90, 0.68, 3.5, IosSecurityColors.crimsonRich),
                    (0.50, 0.08, 2.5, Color.white.opacity(0.55))
                ]
                for (nx, ny, r, c) in dots {
                    let rect = CGRect(x: size.width * nx - r, y: size.height * ny - r, width: r * 2, height: r * 2)
                    ctx.fill(Path(ellipseIn: rect), with: .color(c))
                }
            }
            Circle()
                .fill(IosSecurityColors.crimsonDeep.opacity(0.32))
                .frame(width: 96, height: 96)
                .overlay(Circle().stroke(IosSecurityColors.crimsonGlow, lineWidth: 2.5))
            Image(systemName: "checkmark")
                .font(.system(size: 38, weight: .bold))
                .foregroundColor(IosSecurityColors.softWhite)
        }
    }
}

private struct IosSecurityDetailRow: View {
    let systemIcon: String
    let label: String
    let value: String
    let redBadge: Bool

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(redBadge ? IosSecurityColors.crimsonRich.opacity(0.24) : Color(red: 43 / 255, green: 30 / 255, blue: 32 / 255))
                    .frame(width: 36, height: 36)
                    .overlay(Circle().stroke(redBadge ? IosSecurityColors.crimsonGlow.opacity(0.55) : Color.white.opacity(0.14), lineWidth: 1))
                Image(systemName: systemIcon)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(redBadge ? IosSecurityColors.crimsonGlow : IosSecurityColors.softWhite)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(IosSecurityColors.muted)
                Text(value)
                    .font(.system(size: 14.5, weight: .semibold))
                    .foregroundColor(IosSecurityColors.softWhite)
                    .lineLimit(2)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private func formatSecurityCountdown(_ seconds: Int) -> String {
    let clamped = max(0, seconds)
    return String(format: "%02d:%02d", clamped / 60, clamped % 60)
}

private func formatSecurityTimestamp(_ epochMs: Int64) -> String {
    let ts = epochMs > 0 ? TimeInterval(epochMs) / 1000.0 : Date().timeIntervalSince1970
    let date = Date(timeIntervalSince1970: ts)
    let df = DateFormatter()
    df.dateFormat = "h:mm a"
    if Calendar.current.isDateInToday(date) {
        return "Today, \(df.string(from: date))"
    }
    let full = DateFormatter()
    full.dateFormat = "MMM d, h:mm a"
    return full.string(from: date)
}
