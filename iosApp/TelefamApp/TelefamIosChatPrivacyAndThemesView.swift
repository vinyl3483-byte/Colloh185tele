import SwiftUI

private enum IosChatAudienceField: String, Identifiable {
    case messageRequests
    case whoCanCall
    case whoCanScreenshot
    case whoCanShareChats
    case whoCanCopyMessages
    case whoCanDownloadMedia
    case readReceipts
    case typingIndicators
    case onlineIndicators

    var id: String { rawValue }

    var allowContacts: Bool {
        self == .messageRequests || self == .whoCanCall
    }

    var title: String {
        switch self {
        case .messageRequests: return "Message Requests"
        case .whoCanCall: return "Who Can Call Me"
        case .whoCanScreenshot: return "Who Can Screenshot Chats"
        case .whoCanShareChats: return "Who Can ShareChats"
        case .whoCanCopyMessages: return "Who Can Copy Messages"
        case .whoCanDownloadMedia: return "Who Can Download My Media"
        case .readReceipts: return "Read Receipts"
        case .typingIndicators: return "Typing Indicators"
        case .onlineIndicators: return "Online Indicators"
        }
    }

    var subtitle: String {
        switch self {
        case .messageRequests: return "Set who can send you message requests."
        case .whoCanCall: return "Control who can call you."
        case .whoCanScreenshot: return "Choose who can take screenshots of your chats."
        case .whoCanShareChats: return "Control who can share your chats."
        case .whoCanCopyMessages: return "Control who can copy your messages."
        case .whoCanDownloadMedia: return "Control who can download your photos, videos and files."
        case .readReceipts: return "Control who can see if you read messages."
        case .typingIndicators: return "Control who can see when you are typing."
        case .onlineIndicators: return "Control who can see when you are online."
        }
    }
}

struct TelefamIosChatPrivacyAndThemesView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    var previewSenderName: String = "Telefam"

    @State private var pickingField: IosChatAudienceField? = nil

    private var state: TelefamChatPrivacySettings {
        settingsStore.bundle.chatPrivacy
    }

    private var isDark: Bool { colorScheme == .dark }
    private var sheetBg: Color { isDark ? Color(red: 0.055, green: 0.059, blue: 0.075) : Color(red: 0.965, green: 0.969, blue: 0.980) }
    private var cardBg: Color { isDark ? Color(red: 0.090, green: 0.094, blue: 0.118) : .white }
    private var cardBorder: Color { isDark ? Color(red: 0.149, green: 0.157, blue: 0.192) : Color(red: 0.898, green: 0.906, blue: 0.922) }
    private var titleColor: Color { isDark ? Color(red: 0.957, green: 0.961, blue: 0.973) : Color(red: 0.078, green: 0.082, blue: 0.102) }
    private var subtitleColor: Color { isDark ? Color(red: 0.620, green: 0.643, blue: 0.690) : Color(red: 0.365, green: 0.392, blue: 0.447) }
    private var badgeBg: Color { isDark ? Color(red: 0.200, green: 0.067, blue: 0.086) : Color(red: 0.992, green: 0.925, blue: 0.925) }
    private var badgeTint: Color { isDark ? Color(red: 0.918, green: 0.180, blue: 0.216) : TelefamPalette.crimson }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 6) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44, alignment: .leading)
                    }
                    .accessibilityLabel("Back")

                    Text("Privacy")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)

                    Text("Control who can interact with you and your messages.")
                        .font(.system(size: 13.5))
                        .foregroundColor(.white.opacity(0.92))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 18)
                .background(TelefamPalette.crimson)

                ScrollView {
                    VStack(spacing: 10) {
                        audienceRow(
                            field: .messageRequests,
                            symbol: "text.bubble.fill",
                            value: TelefamChatAudienceOption.fromApi(state.messageRequests, allowContacts: true).label
                        )
                        audienceRow(
                            field: .whoCanCall,
                            symbol: "phone.fill",
                            value: TelefamChatAudienceOption.fromApi(state.whoCanCall, allowContacts: true).label
                        )
                        audienceRow(
                            field: .whoCanScreenshot,
                            symbol: "iphone",
                            value: TelefamChatAudienceOption.fromApi(state.whoCanScreenshot, allowContacts: false, fallback: .nobody).label
                        )
                        audienceRow(
                            field: .whoCanShareChats,
                            symbol: "point.3.connected.trianglepath.dotted",
                            value: TelefamChatAudienceOption.fromApi(state.whoCanShareChats, allowContacts: false, fallback: .nobody).label
                        )
                        audienceRow(
                            field: .whoCanCopyMessages,
                            symbol: "doc.on.doc.fill",
                            value: TelefamChatAudienceOption.fromApi(state.whoCanCopyMessages, allowContacts: false, fallback: .nobody).label
                        )
                        audienceRow(
                            field: .whoCanDownloadMedia,
                            symbol: "arrow.down.to.line",
                            value: TelefamChatAudienceOption.fromApi(state.whoCanDownloadMedia, allowContacts: false, fallback: .anyone).label
                        )

                        NavigationLink(value: TelefamChatPrivacyRoute.chatsTheme) {
                            chatsThemeRowContent
                        }
                        .buttonStyle(.plain)

                        NavigationLink(value: TelefamChatPrivacyRoute.bubbleColors) {
                            bubbleColorRowContent
                        }
                        .buttonStyle(.plain)

                        audienceRow(
                            field: .readReceipts,
                            symbol: "checkmark.rectangle.stack.fill",
                            value: TelefamChatAudienceOption.fromApi(state.readReceipts, allowContacts: false, fallback: .anyone).label
                        )
                        audienceRow(
                            field: .typingIndicators,
                            symbol: "ellipsis.bubble.fill",
                            value: TelefamChatAudienceOption.fromApi(state.typingIndicators, allowContacts: false, fallback: .nobody).label
                        )
                        audienceRow(
                            field: .onlineIndicators,
                            symbol: "eye.fill",
                            value: TelefamChatAudienceOption.fromApi(state.onlineIndicators, allowContacts: false, fallback: .nobody).label
                        )
                    }
                    .frame(maxWidth: 720)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 16)
                    .frame(maxWidth: .infinity)
                }
                .background(sheetBg)
                .clipShape(PrivacyTopCornerShape(radius: 24))
            }
            .background(TelefamPalette.crimson.ignoresSafeArea(edges: .top))
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: TelefamChatPrivacyRoute.self) { route in
                switch route {
                case .chatsTheme:
                    TelefamIosChatsThemePickerView(settingsStore: settingsStore, previewSenderName: previewSenderName)
                case .bubbleColors:
                    TelefamIosBubbleColorsPickerView(settingsStore: settingsStore, previewSenderName: previewSenderName)
                }
            }
            .sheet(item: $pickingField) { field in
                audiencePickerSheet(for: field)
                    .presentationDetents([.medium])
            }
        }
    }

    private func audienceRow(field: IosChatAudienceField, symbol: String, value: String) -> some View {
        Button {
            pickingField = field
        } label: {
            HStack(spacing: 14) {
                Image(systemName: symbol)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(badgeTint)
                    .frame(width: 42, height: 42)
                    .background(badgeBg)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(field.title)
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(titleColor)
                    Text(field.subtitle)
                        .font(.system(size: 12.5))
                        .foregroundColor(subtitleColor)
                        .multilineTextAlignment(.leading)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text(value)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(TelefamPalette.crimson)

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(subtitleColor)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(cardBg)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(cardBorder, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }

    private var chatsThemeRowContent: some View {
        let activeTheme = TelefamChatPrivacyCatalog.themeById(state.chatThemeId)
        return HStack(spacing: 14) {
            Image(systemName: "paintpalette.fill")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(badgeTint)
                .frame(width: 42, height: 42)
                .background(badgeBg)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text("Chats Theme")
                    .font(.system(size: 15.5, weight: .bold))
                    .foregroundColor(titleColor)
                Text("Choose your chat background theme.")
                    .font(.system(size: 12.5))
                    .foregroundColor(subtitleColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if activeTheme.themeId == "none" {
                ZStack {
                    Image(systemName: "paintpalette.fill")
                        .font(.system(size: 13))
                        .foregroundColor(TelefamPalette.crimson)
                    Circle()
                        .stroke(TelefamPalette.crimson, lineWidth: 1.8)
                        .frame(width: 24, height: 24)
                    Rectangle()
                        .fill(TelefamPalette.crimson)
                        .frame(width: 22, height: 2)
                        .rotationEffect(.degrees(-45))
                }
            } else {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [
                                TelefamChatPrivacyCatalog.color(activeTheme.accentStartHex),
                                TelefamChatPrivacyCatalog.color(activeTheme.accentEndHex)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 24, height: 24)
                    .overlay(Circle().stroke(Color.white.opacity(0.65), lineWidth: 1.2))
            }

            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(subtitleColor)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(cardBg)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(cardBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var bubbleColorRowContent: some View {
        let activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(state.bubbleColorId)
        let swatch = TelefamChatPrivacyCatalog.color(isDark ? activeBubble.darkStartHex : activeBubble.lightStartHex)
        return HStack(spacing: 14) {
            Text("T")
                .font(.system(size: 19, weight: .black, design: .serif))
                .foregroundColor(badgeTint)
                .frame(width: 42, height: 42)
                .background(badgeBg)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text("Message Bubbles Colour")
                    .font(.system(size: 15.5, weight: .bold))
                    .foregroundColor(titleColor)
                Text("Choose the colour of your message bubbles.")
                    .font(.system(size: 12.5))
                    .foregroundColor(subtitleColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Circle()
                .fill(swatch)
                .frame(width: 24, height: 24)

            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(subtitleColor)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(cardBg)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(cardBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func audiencePickerSheet(for field: IosChatAudienceField) -> some View {
        let options = field.allowContacts ? TelefamChatPrivacyCatalog.audienceOptions4 : TelefamChatPrivacyCatalog.audienceOptions3
        let currentRaw: String = {
            switch field {
            case .messageRequests: return state.messageRequests
            case .whoCanCall: return state.whoCanCall
            case .whoCanScreenshot: return state.whoCanScreenshot
            case .whoCanShareChats: return state.whoCanShareChats
            case .whoCanCopyMessages: return state.whoCanCopyMessages
            case .whoCanDownloadMedia: return state.whoCanDownloadMedia
            case .readReceipts: return state.readReceipts
            case .typingIndicators: return state.typingIndicators
            case .onlineIndicators: return state.onlineIndicators
            }
        }()
        let current = TelefamChatAudienceOption.fromApi(currentRaw, allowContacts: field.allowContacts)

        return NavigationStack {
            List {
                Section {
                    Text(field.subtitle)
                        .font(.system(size: 13.5))
                        .foregroundColor(.secondary)
                }
                Section {
                    ForEach(options) { opt in
                        Button {
                            var updated = state
                            switch field {
                            case .messageRequests: updated.messageRequests = opt.rawValue
                            case .whoCanCall: updated.whoCanCall = opt.rawValue
                            case .whoCanScreenshot: updated.whoCanScreenshot = opt.rawValue
                            case .whoCanShareChats: updated.whoCanShareChats = opt.rawValue
                            case .whoCanCopyMessages: updated.whoCanCopyMessages = opt.rawValue
                            case .whoCanDownloadMedia: updated.whoCanDownloadMedia = opt.rawValue
                            case .readReceipts: updated.readReceipts = opt.rawValue
                            case .typingIndicators: updated.typingIndicators = opt.rawValue
                            case .onlineIndicators: updated.onlineIndicators = opt.rawValue
                            }
                            pickingField = nil
                            Task { await settingsStore.saveChatPrivacy(updated) }
                        } label: {
                            HStack {
                                Text(opt.label)
                                    .font(.system(size: 16, weight: opt == current ? .bold : .regular))
                                    .foregroundColor(opt == current ? TelefamPalette.crimson : .primary)
                                Spacer()
                                if opt == current {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(TelefamPalette.crimson)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(field.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { pickingField = nil }
                }
            }
        }
    }
}

struct TelefamIosChatsThemePickerView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    var previewSenderName: String = "Telefam"

    @State private var draftThemeId: String = "none"

    private var isDark: Bool { colorScheme == .dark }
    private var pageBg: Color { isDark ? Color(red: 0.039, green: 0.043, blue: 0.059) : Color(red: 0.953, green: 0.957, blue: 0.973) }
    private var cardBg: Color { isDark ? Color(red: 0.090, green: 0.098, blue: 0.114) : .white }
    private var cardBorder: Color { isDark ? Color(red: 0.157, green: 0.169, blue: 0.200) : Color(red: 0.898, green: 0.906, blue: 0.922) }

    var body: some View {
        let activeTheme = TelefamChatPrivacyCatalog.themeById(draftThemeId)
        let activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(settingsStore.bundle.chatPrivacy.bubbleColorId)
        let sectionAccent = TelefamChatPrivacyCatalog.color(isDark ? activeTheme.selectionHex : activeTheme.accentStartHex)

        GeometryReader { geo in
            let colCount = geo.size.width >= 900 ? 5 : (geo.size.width >= 600 ? 4 : 3)
            let cols = Array(repeating: GridItem(.flexible(), spacing: 10), count: colCount)

            ScrollView {
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Select theme")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(sectionAccent)

                        LazyVGrid(columns: cols, spacing: 10) {
                            ForEach(TelefamChatPrivacyCatalog.themes) { theme in
                                themeTile(theme: theme, selected: theme.themeId == draftThemeId)
                            }
                        }
                    }
                    .padding(16)
                    .background(cardBg)
                    .overlay(RoundedRectangle(cornerRadius: 22).stroke(cardBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 22))

                    TelefamIosLiveChatPreviewCard(
                        theme: activeTheme,
                        bubbleColor: activeBubble,
                        previewSenderName: previewSenderName
                    )

                    HStack(spacing: 12) {
                        Button {
                            draftThemeId = "none"
                        } label: {
                            Text("Reset default")
                                .font(.system(size: 15, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(cardBg)
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(cardBorder, lineWidth: 1))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)

                        Button {
                            let chosen = draftThemeId
                            dismiss()
                            Task {
                                await settingsStore.saveChatThemeAndBubble(themeId: chosen)
                                settingsStore.showToast("Chat theme applied")
                            }
                        } label: {
                            Text("Apply Theme")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(TelefamPalette.crimson)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.bottom, 12)
                }
                .frame(maxWidth: 820)
                .padding(.horizontal, 14)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity)
            }
            .background(pageBg)
        }
        .navigationTitle("Chats Theme")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
        .onAppear {
            draftThemeId = settingsStore.bundle.chatPrivacy.chatThemeId
        }
    }

    private func themeTile(theme: TelefamChatThemeSpec, selected: Bool) -> some View {
        let selColor = TelefamChatPrivacyCatalog.color(theme.selectionHex)
        let accentStart = TelefamChatPrivacyCatalog.color(theme.accentStartHex)
        let accentEnd = TelefamChatPrivacyCatalog.color(theme.accentEndHex)
        let incomingColor = isDark ? Color(red: 0.157, green: 0.169, blue: 0.192) : Color.white.opacity(0.94)

        return Button {
            draftThemeId = theme.themeId
        } label: {
            ZStack {
                TelefamIosChatWallpaperView(theme: theme)

                VStack {
                    VStack(spacing: 5) {
                        HStack {
                            Spacer()
                            RoundedRectangle(cornerRadius: 10)
                                .fill(LinearGradient(colors: [accentStart, accentEnd], startPoint: .leading, endPoint: .trailing))
                                .frame(width: 62, height: 19)
                        }
                        HStack {
                            RoundedRectangle(cornerRadius: 10)
                                .fill(incomingColor)
                                .frame(width: 62, height: 19)
                            Spacer()
                        }
                    }

                    Spacer()

                    VStack(spacing: 4) {
                        Image(systemName: motifSymbol(for: theme.patternKind))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(theme.themeId == "none" ? (isDark ? .white : .black) : accentEnd)
                            .frame(width: 32, height: 32)
                            .background(Color.black.opacity(isDark ? 0.36 : 0.12))
                            .clipShape(Circle())

                        Text(theme.title)
                            .font(.system(size: 10.5, weight: selected ? .bold : .medium))
                            .foregroundColor(isDark ? Color(red: 0.95, green: 0.96, blue: 0.97) : Color(red: 0.08, green: 0.08, blue: 0.10))
                            .lineLimit(1)
                    }
                }
                .padding(8)
            }
            .aspectRatio(0.75, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? selColor : Color.white.opacity(isDark ? 0.14 : 0.08), lineWidth: selected ? 2.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func motifSymbol(for kind: TelefamChatPatternKind) -> String {
        switch kind {
        case .clean: return "paintpalette.fill"
        case .home: return "house.fill"
        case .meadow: return "leaf.fill"
        case .winter: return "snowflake"
        case .ocean: return "water.waves"
        case .math: return "function"
        case .botanical: return "camera.macro"
        case .romance: return "heart.fill"
        case .fireside: return "flame.fill"
        case .arcade: return "gamecontroller.fill"
        case .cosmic: return "sparkles"
        case .safari: return "sun.horizon.fill"
        case .telefam: return "paperplane.fill"
        }
    }
}

struct TelefamIosBubbleColorsPickerView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    var previewSenderName: String = "Telefam"

    @State private var draftColorId: String = "cyan"

    private var isDark: Bool { colorScheme == .dark }
    private var pageBg: Color { isDark ? Color(red: 0.039, green: 0.043, blue: 0.059) : Color(red: 0.953, green: 0.957, blue: 0.973) }
    private var cardBg: Color { isDark ? Color(red: 0.090, green: 0.098, blue: 0.114) : .white }
    private var cardBorder: Color { isDark ? Color(red: 0.157, green: 0.169, blue: 0.200) : Color(red: 0.898, green: 0.906, blue: 0.922) }

    var body: some View {
        let activeTheme = TelefamChatPrivacyCatalog.themeById(settingsStore.bundle.chatPrivacy.chatThemeId)
        let activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(draftColorId)

        GeometryReader { geo in
            let colCount = geo.size.width >= 600 ? 3 : 2
            let cols = Array(repeating: GridItem(.flexible(), spacing: 12), count: colCount)

            ScrollView {
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 14) {
                        Text("6 Bubble Colours")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                        LazyVGrid(columns: cols, spacing: 12) {
                            ForEach(TelefamChatPrivacyCatalog.bubbleColors) { option in
                                let picked = option.colorId == draftColorId
                                let s = TelefamChatPrivacyCatalog.color(isDark ? option.darkStartHex : option.lightStartHex)
                                let e = TelefamChatPrivacyCatalog.color(isDark ? option.darkEndHex : option.lightEndHex)
                                Button {
                                    draftColorId = option.colorId
                                } label: {
                                    VStack(spacing: 8) {
                                        Circle()
                                            .fill(LinearGradient(colors: [s, e], startPoint: .topLeading, endPoint: .bottomTrailing))
                                            .frame(width: 44, height: 44)
                                            .overlay(Circle().stroke(Color.white.opacity(0.45), lineWidth: 2))

                                        Text(option.label)
                                            .font(.system(size: 12.5, weight: picked ? .bold : .medium))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            .lineLimit(1)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(picked ? s.opacity(0.14) : pageBg)
                                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(picked ? s : cardBorder, lineWidth: picked ? 2 : 1))
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(16)
                    .background(cardBg)
                    .overlay(RoundedRectangle(cornerRadius: 22).stroke(cardBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 22))

                    TelefamIosLiveChatPreviewCard(
                        theme: activeTheme,
                        bubbleColor: activeBubble,
                        previewSenderName: previewSenderName
                    )

                    Text("Bubbles automatically adapt across light and dark mode.")
                        .font(.system(size: 12.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button {
                        let chosen = draftColorId
                        dismiss()
                        Task {
                            await settingsStore.saveChatThemeAndBubble(bubbleColorId: chosen)
                            settingsStore.showToast("Message bubble colour updated")
                        }
                    } label: {
                        Text("Apply Bubble Colour")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(TelefamPalette.crimson)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .padding(.bottom, 12)
                }
                .frame(maxWidth: 740)
                .padding(.horizontal, 14)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity)
            }
            .background(pageBg)
        }
        .navigationTitle("Message Bubbles Colour")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
        .onAppear {
            draftColorId = settingsStore.bundle.chatPrivacy.bubbleColorId
        }
    }
}

struct TelefamIosLiveChatPreviewCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let theme: TelefamChatThemeSpec
    let bubbleColor: TelefamChatBubbleColorOption
    var previewSenderName: String = "Telefam"

    var body: some View {
        let resolvedSenderName: String = {
            let trimmed = previewSenderName.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? "Telefam" : trimmed
        }()
        let isDark = colorScheme == .dark
        let incomingBg = isDark ? Color(red: 0.129, green: 0.141, blue: 0.169) : .white
        let quoteBg = isDark ? Color(red: 0.176, green: 0.200, blue: 0.243) : Color(red: 0.945, green: 0.961, blue: 0.976)
        let quoteAccent = TelefamChatPrivacyCatalog.color(isDark ? theme.accentEndHex : theme.accentStartHex)
        let incomingText = isDark ? Color(red: 0.953, green: 0.957, blue: 0.965) : Color(red: 0.078, green: 0.082, blue: 0.102)
        let timeText = isDark ? Color(red: 0.620, green: 0.643, blue: 0.690) : Color(red: 0.420, green: 0.447, blue: 0.502)

        let outColors: [Color] = {
            if theme.themeId == "none" {
                return [
                    TelefamChatPrivacyCatalog.color(isDark ? bubbleColor.darkStartHex : bubbleColor.lightStartHex),
                    TelefamChatPrivacyCatalog.color(isDark ? bubbleColor.darkEndHex : bubbleColor.lightEndHex)
                ]
            } else {
                return [
                    TelefamChatPrivacyCatalog.color(theme.accentStartHex),
                    TelefamChatPrivacyCatalog.color(theme.accentEndHex)
                ]
            }
        }()

        ZStack {
            TelefamIosChatWallpaperView(theme: theme)

            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(quoteAccent)
                            .frame(width: 3, height: 30)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(resolvedSenderName)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(quoteAccent)
                                .lineLimit(1)
                            Text("Good morning!")
                                .font(.system(size: 12.5))
                                .foregroundColor(incomingText)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .background(quoteBg)
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                    HStack(alignment: .bottom, spacing: 8) {
                        Text("Do you know what time it is?")
                            .font(.system(size: 14))
                            .foregroundColor(incomingText)
                        Spacer()
                        Text("14:05")
                            .font(.system(size: 11))
                            .foregroundColor(timeText)
                    }
                }
                .padding(10)
                .background(incomingBg.opacity(0.96))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .frame(maxWidth: 320, alignment: .leading)

                HStack {
                    Spacer()
                    HStack(alignment: .bottom, spacing: 6) {
                        Text("It's morning in Tokyo")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white)
                        Text("14:20")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.88))
                        Image(systemName: "checkmark.rectangle.stack")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(LinearGradient(colors: outColors, startPoint: .leading, endPoint: .trailing))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
            .padding(16)
        }
        .frame(height: 205)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(isDark ? Color(red: 0.157, green: 0.169, blue: 0.200) : Color(red: 0.886, green: 0.910, blue: 0.941), lineWidth: 1)
        )
    }
}

struct TelefamIosChatWallpaperView: View {
    @Environment(\.colorScheme) private var colorScheme
    let theme: TelefamChatThemeSpec

    private func blendHex(_ baseHex: UInt32, _ accentHex: UInt32, ratio: Double) -> Color {
        let br = Double((baseHex >> 16) & 0xFF) / 255.0
        let bg = Double((baseHex >> 8) & 0xFF) / 255.0
        let bb = Double(baseHex & 0xFF) / 255.0
        let ar = Double((accentHex >> 16) & 0xFF) / 255.0
        let ag = Double((accentHex >> 8) & 0xFF) / 255.0
        let ab = Double(accentHex & 0xFF) / 255.0
        return Color(
            red: br + (ar - br) * ratio,
            green: bg + (ag - bg) * ratio,
            blue: bb + (ab - bb) * ratio
        )
    }

    var body: some View {
        let isDark = colorScheme == .dark
        let baseHex = isDark ? theme.darkBgHex : theme.lightBgHex
        let gradTopLeft = blendHex(baseHex, theme.accentStartHex, ratio: 0.0)
        let gradMid = blendHex(baseHex, theme.accentStartHex, ratio: isDark ? 0.08 : 0.07)
        let gradBottomRight = blendHex(baseHex, theme.accentEndHex, ratio: isDark ? 0.16 : 0.13)

        let accentStart = TelefamChatPrivacyCatalog.color(theme.accentStartHex)
        let accentEnd = TelefamChatPrivacyCatalog.color(theme.accentEndHex)
        let baseAlpha = isDark ? theme.darkPatternAlpha : theme.lightPatternAlpha
        let primaryInk = TelefamChatPrivacyCatalog.color(
            isDark ? theme.selectionHex : theme.accentStartHex,
            opacity: min(max(baseAlpha * (isDark ? 1.45 : 1.35), 0.12), 0.28)
        )
        let secondaryInk = TelefamChatPrivacyCatalog.color(
            isDark ? theme.accentEndHex : theme.selectionHex,
            opacity: min(max(baseAlpha * 1.15, 0.10), 0.22)
        )

        ZStack {
            LinearGradient(
                colors: [gradTopLeft, gradMid, gradBottomRight],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            GeometryReader { geo in
                let maxDim = max(geo.size.width, geo.size.height)
                RadialGradient(
                    colors: [accentEnd.opacity(isDark ? 0.22 : 0.14), .clear],
                    center: UnitPoint(x: 0.88, y: 0.14),
                    startRadius: 4,
                    endRadius: maxDim * 0.65
                )
                RadialGradient(
                    colors: [accentStart.opacity(isDark ? 0.18 : 0.11), .clear],
                    center: UnitPoint(x: 0.14, y: 0.86),
                    startRadius: 4,
                    endRadius: maxDim * 0.62
                )
            }

            Canvas { ctx, size in
                if theme.patternKind == .clean {
                    let dotStep: CGFloat = 38
                    let softDot = isDark ? Color.white.opacity(0.045) : Color.black.opacity(0.035)
                    let accentDot = accentEnd.opacity(isDark ? 0.08 : 0.06)
                    let cols = Int(size.width / dotStep) + 2
                    let rows = Int(size.height / dotStep) + 2
                    for r in 0..<rows {
                        for c in 0..<cols {
                            let x = dotStep * 0.5 + CGFloat(c) * dotStep + (r % 2 == 1 ? dotStep * 0.5 : 0)
                            let y = dotStep * 0.5 + CGFloat(r) * dotStep
                            let isAccent = (r + c) % 3 == 0
                            let rad: CGFloat = isAccent ? 2.0 : 1.4
                            ctx.fill(
                                Path(ellipseIn: CGRect(x: x - rad, y: y - rad, width: rad * 2, height: rad * 2)),
                                with: .color(isAccent ? accentDot : softDot)
                            )
                        }
                    }
                    return
                }

                let step: CGFloat = 68
                let cols = Int(size.width / step) + 2
                let rows = Int(size.height / step) + 2
                let angles: [Double] = [-18, 12, -9, 22, 6, -24]
                let fineStyle = StrokeStyle(lineWidth: 1.05, lineCap: .round, lineJoin: .round)

                for r in 0..<rows {
                    for c in 0..<cols {
                        let offsetX: CGFloat = r % 2 == 1 ? step * 0.48 : 0
                        let jitterY: CGFloat = (r + c) % 2 == 0 ? -step * 0.06 : step * 0.06
                        let cx = CGFloat(c) * step + offsetX + step * 0.26
                        let cy = CGFloat(r) * step + step * 0.34 + jitterY
                        let angle = angles[(r * 5 + c) % angles.count]
                        let cInk = (r + c) % 2 == 1 ? secondaryInk : primaryInk
                        let rad = step * ((r + c) % 3 == 0 ? 0.27 : 0.22)

                        var doodleCtx = ctx
                        doodleCtx.translateBy(x: cx, y: cy)
                        doodleCtx.rotate(by: .degrees(angle))
                        drawDoodle(in: &doodleCtx, kind: theme.patternKind, cx: 0, cy: 0, radius: rad, color: cInk)

                        if (r + c) % 2 == 0 {
                            let sx = cx + step * 0.46
                            let sy = cy + step * 0.40
                            if (r * 3 + c) % 4 == 0 {
                                let sr = step * 0.065
                                var star = Path()
                                star.move(to: CGPoint(x: sx, y: sy - sr))
                                star.addLine(to: CGPoint(x: sx + sr * 0.26, y: sy - sr * 0.26))
                                star.addLine(to: CGPoint(x: sx + sr, y: sy))
                                star.addLine(to: CGPoint(x: sx + sr * 0.26, y: sy + sr * 0.26))
                                star.addLine(to: CGPoint(x: sx, y: sy + sr))
                                star.addLine(to: CGPoint(x: sx - sr * 0.26, y: sy + sr * 0.26))
                                star.addLine(to: CGPoint(x: sx - sr, y: sy))
                                star.addLine(to: CGPoint(x: sx - sr * 0.26, y: sy - sr * 0.26))
                                star.closeSubpath()
                                ctx.stroke(star, with: .color(secondaryInk), style: fineStyle)
                            } else {
                                let dr: CGFloat = 1.6
                                ctx.fill(
                                    Path(ellipseIn: CGRect(x: sx - dr, y: sy - dr, width: dr * 2, height: dr * 2)),
                                    with: .color(primaryInk)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private func drawDoodle(in ctx: inout GraphicsContext, kind: TelefamChatPatternKind, cx: CGFloat, cy: CGFloat, radius r: CGFloat, color: Color) {
        let style = StrokeStyle(lineWidth: 1.3, lineCap: .round, lineJoin: .round)
        switch kind {
        case .clean:
            break
        case .home:
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy - r * 0.9))
            p.addLine(to: CGPoint(x: cx + r * 0.85, y: cy - r * 0.1))
            p.addLine(to: CGPoint(x: cx + r * 0.6, y: cy - r * 0.1))
            p.addLine(to: CGPoint(x: cx + r * 0.6, y: cy + r * 0.8))
            p.addLine(to: CGPoint(x: cx - r * 0.6, y: cy + r * 0.8))
            p.addLine(to: CGPoint(x: cx - r * 0.6, y: cy - r * 0.1))
            p.addLine(to: CGPoint(x: cx - r * 0.85, y: cy - r * 0.1))
            p.closeSubpath()
            ctx.stroke(p, with: .color(color), style: style)
        case .meadow:
            for i in 0..<5 {
                let ang = Double(i) * (2.0 * Double.pi / 5.0)
                let px = cx + CGFloat(cos(ang)) * r * 0.52
                let py = cy + CGFloat(sin(ang)) * r * 0.52
                ctx.stroke(Path(ellipseIn: CGRect(x: px - r * 0.3, y: py - r * 0.3, width: r * 0.6, height: r * 0.6)), with: .color(color), style: style)
            }
        case .winter:
            for i in 0..<3 {
                let ang = Double(i) * (Double.pi / 3.0)
                let dx = CGFloat(cos(ang)) * r * 0.9
                let dy = CGFloat(sin(ang)) * r * 0.9
                var p = Path()
                p.move(to: CGPoint(x: cx - dx, y: cy - dy))
                p.addLine(to: CGPoint(x: cx + dx, y: cy + dy))
                ctx.stroke(p, with: .color(color), style: style)
            }
        case .ocean:
            var p = Path()
            p.move(to: CGPoint(x: cx - r, y: cy))
            p.addCurve(to: CGPoint(x: cx + r, y: cy), control1: CGPoint(x: cx - r * 0.4, y: cy - r * 0.6), control2: CGPoint(x: cx + r * 0.4, y: cy + r * 0.6))
            ctx.stroke(p, with: .color(color), style: style)
        case .math:
            var p = Path()
            p.move(to: CGPoint(x: cx - r * 0.6, y: cy - r * 0.4))
            p.addLine(to: CGPoint(x: cx + r * 0.7, y: cy - r * 0.4))
            p.move(to: CGPoint(x: cx - r * 0.25, y: cy - r * 0.4))
            p.addLine(to: CGPoint(x: cx - r * 0.35, y: cy + r * 0.55))
            p.move(to: CGPoint(x: cx + r * 0.3, y: cy - r * 0.4))
            p.addLine(to: CGPoint(x: cx + r * 0.35, y: cy + r * 0.55))
            ctx.stroke(p, with: .color(color), style: style)
        case .botanical:
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy + r * 0.85))
            p.addCurve(to: CGPoint(x: cx, y: cy - r * 0.85), control1: CGPoint(x: cx - r * 0.9, y: cy), control2: CGPoint(x: cx - r * 0.6, y: cy - r * 0.7))
            p.addCurve(to: CGPoint(x: cx, y: cy + r * 0.85), control1: CGPoint(x: cx + r * 0.6, y: cy - r * 0.7), control2: CGPoint(x: cx + r * 0.9, y: cy))
            ctx.stroke(p, with: .color(color), style: style)
        case .romance:
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy + r * 0.75))
            p.addCurve(to: CGPoint(x: cx, y: cy - r * 0.25), control1: CGPoint(x: cx - r, y: cy + r * 0.05), control2: CGPoint(x: cx - r * 0.85, y: cy - r * 0.8))
            p.addCurve(to: CGPoint(x: cx, y: cy + r * 0.75), control1: CGPoint(x: cx + r * 0.85, y: cy - r * 0.8), control2: CGPoint(x: cx + r, y: cy + r * 0.05))
            ctx.stroke(p, with: .color(color), style: style)
        case .fireside:
            var p = Path()
            p.move(to: CGPoint(x: cx, y: cy - r * 0.85))
            p.addCurve(to: CGPoint(x: cx, y: cy + r * 0.8), control1: CGPoint(x: cx + r * 0.75, y: cy - r * 0.1), control2: CGPoint(x: cx + r * 0.75, y: cy + r * 0.8))
            p.addCurve(to: CGPoint(x: cx, y: cy - r * 0.85), control1: CGPoint(x: cx - r * 0.75, y: cy + r * 0.8), control2: CGPoint(x: cx - r * 0.75, y: cy - r * 0.1))
            ctx.stroke(p, with: .color(color), style: style)
        case .arcade:
            ctx.stroke(Path(roundedRect: CGRect(x: cx - r * 0.85, y: cy - r * 0.45, width: r * 1.7, height: r * 0.95), cornerRadius: r * 0.3), with: .color(color), style: style)
        case .cosmic:
            ctx.stroke(Path(ellipseIn: CGRect(x: cx - r * 0.45, y: cy - r * 0.45, width: r * 0.9, height: r * 0.9)), with: .color(color), style: style)
            ctx.stroke(Path(ellipseIn: CGRect(x: cx - r * 0.9, y: cy - r * 0.22, width: r * 1.8, height: r * 0.44)), with: .color(color), style: style)
        case .safari:
            ctx.stroke(Path(ellipseIn: CGRect(x: cx - r * 0.45, y: cy - r * 0.55, width: r * 0.9, height: r * 0.9)), with: .color(color), style: style)
            var h = Path()
            h.move(to: CGPoint(x: cx - r * 0.9, y: cy + r * 0.4))
            h.addLine(to: CGPoint(x: cx + r * 0.9, y: cy + r * 0.4))
            ctx.stroke(h, with: .color(color), style: style)
        case .telefam:
            ctx.stroke(Path(roundedRect: CGRect(x: cx - r * 0.65, y: cy - r * 0.65, width: r * 1.3, height: r * 1.1), cornerRadius: r * 0.3), with: .color(color), style: style)
        }
    }
}

private struct PrivacyTopCornerShape: Shape {
    let radius: CGFloat
    func path(in rect: CGRect) -> Path {
        let p = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(p.cgPath)
    }
}
