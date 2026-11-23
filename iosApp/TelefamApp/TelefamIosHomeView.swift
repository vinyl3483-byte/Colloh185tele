import SwiftUI

enum TelefamIosHomeTab: String {
    case chats
    case contacts
    case post
    case feeds
    case profile
}

private enum TelefamIosPostLaunchTarget: Identifiable {
    case createNew
    case prefillHashtag(tag: String, mode: TelefamIosPostMode)
    case prefillSong(song: TelefamIosSongTrack, mode: TelefamIosPostMode)
    case edit(item: TelefamIosFeedItem)

    var id: String {
        switch self {
        case .createNew: return "new"
        case .prefillHashtag(let tag, _): return "tag_\(tag)"
        case .prefillSong(let song, _): return "song_\(song.trackId)"
        case .edit(let item): return "edit_\(item.postId)"
        }
    }
}

struct TelefamIosHomeView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject private var smartLinksCoordinator: TelefamIosSmartLinksCoordinator
    @ObservedObject var sessionStore: TelefamIosSessionStore
    @StateObject private var contactsStore: TelefamContactsStore
    @StateObject private var feedStore: TelefamIosFeedStore
    @StateObject private var settingsStore: TelefamIosSettingsStore
    @StateObject private var chatStore: TelefamIosChatStore
    @StateObject private var callController: TelefamIosCallController
    @StateObject private var starsStore: TelefamIosStarsStore
    @StateObject private var verificationStore: TelefamIosVerificationStore
    @Binding var themeMode: TelefamThemeMode

    private let serverURL: URL
    @ObservedObject private var notificationManager = TelefamIosNotificationManager.shared
    @State private var activeTab: TelefamIosHomeTab = .chats
    @State private var isBottomBarVisible: Bool = true
    @State private var postLaunchTarget: TelefamIosPostLaunchTarget? = nil
    @State private var showFeedSearch: Bool = false
    @State private var showChatPrivacy: Bool = false
    @State private var profileBackStack: [String] = []
    @State private var comingSoonBanner: String? = nil
    @State private var chatFilterQuery: String = ""

    init(sessionStore: TelefamIosSessionStore, serverURL: URL, themeMode: Binding<TelefamThemeMode>) {
        self.sessionStore = sessionStore
        self.serverURL = serverURL
        let cStore = TelefamContactsStore(
            baseUrl: serverURL.absoluteString,
            sessionTokenProvider: { sessionStore.sessionToken },
            deviceFingerprintProvider: { sessionStore.deviceFingerprint }
        )
        let fStore = TelefamIosFeedStore(
            baseURL: serverURL,
            tokenProvider: { sessionStore.sessionToken },
            deviceFingerprint: sessionStore.deviceFingerprint
        )
        let sStore = TelefamIosSettingsStore(
            baseURL: serverURL,
            userIdProvider: { sessionStore.userId },
            tokenProvider: { sessionStore.sessionToken },
            deviceFingerprint: sessionStore.deviceFingerprint
        )
        _contactsStore = StateObject(wrappedValue: cStore)
        _feedStore = StateObject(wrappedValue: fStore)
        _settingsStore = StateObject(wrappedValue: sStore)
        _chatStore = StateObject(
            wrappedValue: TelefamIosChatStore(
                baseURL: serverURL,
                userIdProvider: { sessionStore.userId },
                tokenProvider: { sessionStore.sessionToken },
                deviceFingerprint: sessionStore.deviceFingerprint,
                soundsEnabledProvider: {
                    !sStore.bundle.notifications.masterPause && sStore.bundle.notifications.typeMessages
                }
            )
        )
        _callController = StateObject(
            wrappedValue: TelefamIosCallController(
                baseURL: serverURL,
                userIdProvider: { sessionStore.userId },
                tokenProvider: { sessionStore.sessionToken },
                deviceFingerprint: sessionStore.deviceFingerprint
            )
        )
        _starsStore = StateObject(
            wrappedValue: TelefamIosStarsStore(
                baseURL: serverURL,
                userIdProvider: { sessionStore.userId },
                tokenProvider: { sessionStore.sessionToken },
                deviceFingerprint: sessionStore.deviceFingerprint,
                countryHintProvider: { sessionStore.countryIso }
            )
        )
        _verificationStore = StateObject(
            wrappedValue: TelefamIosVerificationStore(
                baseURL: serverURL,
                userIdProvider: { sessionStore.userId },
                tokenProvider: { sessionStore.sessionToken },
                deviceFingerprint: sessionStore.deviceFingerprint,
                countryHintProvider: { sessionStore.countryIso }
            )
        )
        _themeMode = themeMode
    }

    var body: some View {
        GeometryReader { geo in
            let isWide = geo.size.width >= 680

            ZStack(alignment: .bottom) {
                VStack(spacing: 0) {
                    if activeTab == .contacts {
                        TelefamContactsTopHeaderView(
                            store: contactsStore,
                            themeMode: $themeMode,
                            onOpenPrivacy: { showChatPrivacy = true }
                        )
                    } else if activeTab != .feeds && activeTab != .profile {
                        chatsTopHeader
                    }

                    Group {
                        if isWide {
                            HStack(spacing: 0) {
                                sideRail
                                Divider().background(TelefamPalette.border(colorScheme))
                                tabBody(isCompactWidth: false, safeBottomInset: geo.safeAreaInsets.bottom)
                            }
                        } else {
                            ZStack(alignment: .bottom) {
                                tabBody(isCompactWidth: true, safeBottomInset: geo.safeAreaInsets.bottom)
                                if activeTab != .contacts || isBottomBarVisible {
                                    bottomBar
                                        .transition(.move(edge: .bottom).combined(with: .opacity))
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(activeTab == .feeds ? Color(red: 0.027, green: 0.031, blue: 0.043) : TelefamPalette.background(colorScheme))
                    .clipShape(
                        RoundedCornerShapeShim(radius: (activeTab == .contacts || activeTab == .feeds || activeTab == .profile) ? 0 : 24)
                    )
                }
                .background((activeTab == .feeds ? Color(red: 0.027, green: 0.031, blue: 0.043) : (activeTab == .profile ? TelefamPalette.background(colorScheme) : TelefamPalette.crimson)).ignoresSafeArea(edges: .top))

                if let toast = chatStore.toastMessage ?? settingsStore.toastMessage ?? feedStore.toastMessage ?? contactsStore.toastMessage ?? smartLinksCoordinator.toastMessage ?? comingSoonBanner {
                    Text(toast)
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.84))
                        .clipShape(Capsule())
                        .padding(.bottom, 84)
                        .transition(.opacity)
                }

                if let incoming = sessionStore.incomingLoginRequest, sessionStore.loginApprovedOverlay == nil {
                    TelefamIosIncomingLoginRequestView(
                        request: incoming,
                        isResponding: sessionStore.isRespondingToLoginRequest,
                        actionError: sessionStore.loginRequestActionError,
                        onDeny: {
                            Task {
                                await sessionStore.respondToIncomingLoginRequest(accept: false)
                                if sessionStore.incomingLoginRequest == nil {
                                    showComingSoon("Login request denied and device blocked.")
                                }
                            }
                        },
                        onAccept: {
                            Task { await sessionStore.respondToIncomingLoginRequest(accept: true) }
                        }
                    )
                    .zIndex(90)
                }

                if let banner = notificationManager.activeBanner {
                    VStack {
                        TelefamIosForegroundNotifBannerView(
                            banner: banner,
                            onOpen: {
                                notificationManager.dismissBanner()
                                if !banner.conversationId.isEmpty {
                                    notificationManager.pendingRoute = .chat(conversationId: banner.conversationId, peerUserId: banner.peerUserId)
                                } else if !banner.postId.isEmpty {
                                    notificationManager.pendingRoute = .post(postId: banner.postId)
                                } else if !banner.targetUserId.isEmpty {
                                    notificationManager.pendingRoute = .profile(userId: banner.targetUserId)
                                }
                            },
                            onMarkRead: {
                                notificationManager.dismissBanner()
                                Task {
                                    if !banner.conversationId.isEmpty {
                                        await chatStore.markConversationReadFromNotification(conversationId: banner.conversationId)
                                    }
                                }
                            },
                            onQuickReply: { reply in
                                notificationManager.dismissBanner()
                                Task {
                                    await chatStore.sendQuickReplyFromNotification(
                                        conversationId: banner.conversationId,
                                        peerUserId: banner.peerUserId,
                                        text: reply
                                    )
                                }
                            },
                            onDismiss: { notificationManager.dismissBanner() }
                        )
                        .padding(.top, 8)
                        Spacer()
                    }
                    .zIndex(85)
                }

                if let approved = sessionStore.loginApprovedOverlay {
                    TelefamIosLoginApprovedView(
                        approved: approved,
                        onDone: { sessionStore.dismissLoginApprovedOverlay() }
                    )
                    .zIndex(95)
                }

                TelefamIosCallOverlayHost(
                    controller: callController,
                    authBearerToken: sessionStore.sessionToken,
                    authDeviceFingerprint: sessionStore.deviceFingerprint
                )
                .zIndex(98)

                if let burst = starsStore.celebrationBurst {
                    TelefamIosStarCelebrationOverlay(
                        burst: burst,
                        onDismiss: { starsStore.dismissCelebration() }
                    )
                    .zIndex(99)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: isBottomBarVisible)
            .onChange(of: contactsStore.toastMessage) { msg in
                guard msg != nil else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                    contactsStore.toastMessage = nil
                }
            }
        }
        .task {
            smartLinksCoordinator.configure(
                baseURL: serverURL,
                userIdProvider: { sessionStore.userId },
                tokenProvider: { sessionStore.sessionToken },
                deviceFingerprint: sessionStore.deviceFingerprint
            )
            chatStore.attachIfNeeded()
            callController.attach(chatStore: chatStore) { convId in
                activeTab = .chats
                isBottomBarVisible = true
                if !convId.isEmpty {
                    chatStore.openConversation(convId)
                }
            }
            notificationManager.attach(
                sessionStore: sessionStore,
                chatStore: chatStore,
                settingsStore: settingsStore
            )
            await settingsStore.refreshAll()
            sessionStore.startIncomingLoginRequestPoller()
            await sessionStore.captureAndSyncDeviceContext()
            while !Task.isCancelled && sessionStore.isAuthenticated {
                try? await Task.sleep(nanoseconds: 15_000_000_000)
                if sessionStore.isAuthenticated {
                    await notificationManager.refreshNotifications()
                }
            }
        }
        .onChange(of: scenePhase) { newPhase in
            callController.onScenePhaseChanged(newPhase)
        }
        .onChange(of: chatFilterQuery) { q in
            chatStore.runGlobalSearch(q)
        }
        .onChange(of: notificationManager.pendingRoute) { route in
            guard let route else { return }
            notificationManager.pendingRoute = nil
            handleNotificationRoute(route)
        }
        .onChange(of: sessionStore.isAuthenticated) { authed in
            if !authed {
                callController.detach()
                chatStore.detach()
                starsStore.clearOnLogout()
                verificationStore.clearOnLogout()
                smartLinksCoordinator.clearOnLogout()
                notificationManager.clearOnLogout()
                sessionStore.stopIncomingLoginRequestPoller()
                activeTab = .chats
                chatFilterQuery = ""
                profileBackStack.removeAll()
                showFeedSearch = false
                showChatPrivacy = false
                postLaunchTarget = nil
            }
        }
        .onDisappear {
            callController.detach()
            chatStore.detach()
            sessionStore.stopIncomingLoginRequestPoller()
        }
        .sheet(isPresented: $notificationManager.showInboxSheet) {
            TelefamIosNotificationsSheet(
                manager: notificationManager,
                bearerToken: sessionStore.sessionToken,
                deviceFingerprint: sessionStore.deviceFingerprint,
                onSelectNotification: { item in
                    notificationManager.openNotificationItem(item)
                }
            )
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: $showFeedSearch) {
            TelefamIosSearchView(
                store: feedStore,
                contactsStore: contactsStore,
                settingsStore: settingsStore,
                onEditOwnerPost: { item in
                    postLaunchTarget = .edit(item: item)
                },
                onTapHashtag: { tag, mode in
                    postLaunchTarget = .prefillHashtag(tag: tag, mode: mode)
                },
                onTapAudio: { song, mode in
                    postLaunchTarget = .prefillSong(song: song, mode: mode)
                },
                onCommentComingSoon: {
                    showComingSoon("Comments — Coming soon")
                },
                onOpenAuthorProfile: { uid in
                    pushProfile(uid)
                }
            )
            .environmentObject(smartLinksCoordinator)
            .telefamSmartOverlayHost(
                preferredColorScheme: themeMode.preferredColorScheme,
                showLocalToast: true
            )
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: Binding(get: { !profileBackStack.isEmpty }, set: { if !$0 { profileBackStack.removeAll() } })) {
            if let topUserId = profileBackStack.last {
                TelefamIosProfileView(
                    targetUserId: topUserId,
                    isEmbeddedOwnProfileTab: false,
                    bottomChromePadding: 0,
                    baseURL: serverURL,
                    sessionStore: sessionStore,
                    contactsStore: contactsStore,
                    feedStore: feedStore,
                    settingsStore: settingsStore,
                    themeMode: $themeMode,
                    onBack: {
                        if !profileBackStack.isEmpty {
                            profileBackStack.removeLast()
                        }
                    },
                    onOpenUserProfile: { uid in pushProfile(uid) },
                    onEditOwnerPost: { item in
                        profileBackStack.removeAll()
                        postLaunchTarget = .edit(item: item)
                    },
                    onTapHashtag: { tag, mode in
                        profileBackStack.removeAll()
                        postLaunchTarget = .prefillHashtag(tag: tag, mode: mode)
                    },
                    onTapAudio: { song, mode in
                        profileBackStack.removeAll()
                        postLaunchTarget = .prefillSong(song: song, mode: mode)
                    },
                    onMessageUser: { prof in
                        launchChat(with: prof)
                    },
                    onOpenStars: {
                        profileBackStack.removeAll()
                        starsStore.openStarsHub()
                    },
                    onOpenGetVerified: { tier in
                        profileBackStack.removeAll()
                        verificationStore.openGetVerified(
                            profileCountryIso: sessionStore.countryIso,
                            fullName: sessionStore.fullName,
                            username: sessionStore.username,
                            initialVerifiedTier: tier
                        )
                    }
                )
                .id(topUserId)
                .environmentObject(smartLinksCoordinator)
                .telefamSmartOverlayHost(
                    preferredColorScheme: themeMode.preferredColorScheme,
                    showLocalToast: true
                )
                .preferredColorScheme(themeMode.preferredColorScheme)
            }
        }
        .fullScreenCover(item: $postLaunchTarget) { target in
            buildPostView(for: target)
                .preferredColorScheme(themeMode.preferredColorScheme)
                .onDisappear {
                    Task { await feedStore.refresh() }
                }
        }
        .fullScreenCover(isPresented: $showChatPrivacy) {
            let cleanFull = sessionStore.fullName.trimmingCharacters(in: .whitespacesAndNewlines)
            let cleanUser = sessionStore.username.trimmingCharacters(in: .whitespacesAndNewlines)
            let previewName = cleanFull.isEmpty ? (cleanUser.isEmpty ? "Telefam" : cleanUser) : cleanFull
            TelefamIosChatPrivacyAndThemesView(settingsStore: settingsStore, previewSenderName: previewName)
                .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: Binding(
            get: { starsStore.route != nil },
            set: { if !$0 { starsStore.dismissFlow() } }
        )) {
            TelefamIosStarsHostView(store: starsStore)
                .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: $verificationStore.isPresented) {
            TelefamIosVerificationHostView(store: verificationStore)
                .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .telefamSmartOverlayHost(
            preferredColorScheme: themeMode.preferredColorScheme,
            showLocalToast: false
        )
        .preferredColorScheme(themeMode.preferredColorScheme)
    }

    private func pushProfile(_ userId: String) {
        let trimmed = userId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let ownId = sessionStore.userId.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed == "me" || (!ownId.isEmpty && trimmed == ownId) {
            profileBackStack.removeAll()
            showFeedSearch = false
            activeTab = .profile
            isBottomBarVisible = true
            return
        }
        if profileBackStack.last != trimmed {
            profileBackStack.append(trimmed)
        }
    }

    @ViewBuilder
    private func buildPostView(for target: TelefamIosPostLaunchTarget) -> some View {
        let store: TelefamIosPostStore = {
            switch target {
            case .createNew:
                return TelefamIosPostStore(
                    baseURL: serverURL,
                    tokenProvider: { sessionStore.sessionToken },
                    deviceFingerprint: sessionStore.deviceFingerprint
                )
            case .prefillHashtag(let tag, let mode):
                return TelefamIosPostStore(
                    baseURL: serverURL,
                    tokenProvider: { sessionStore.sessionToken },
                    deviceFingerprint: sessionStore.deviceFingerprint,
                    initialMode: mode,
                    initialHashtags: [tag]
                )
            case .prefillSong(let song, let mode):
                return TelefamIosPostStore(
                    baseURL: serverURL,
                    tokenProvider: { sessionStore.sessionToken },
                    deviceFingerprint: sessionStore.deviceFingerprint,
                    initialMode: mode,
                    initialSong: song
                )
            case .edit(let item):
                return TelefamIosPostStore(
                    baseURL: serverURL,
                    tokenProvider: { sessionStore.sessionToken },
                    deviceFingerprint: sessionStore.deviceFingerprint,
                    editingPostId: item.postId,
                    initialMode: TelefamIosPostMode(rawValue: item.mode) ?? .video,
                    initialText: item.textContent,
                    initialCaption: item.caption,
                    initialThemeId: item.themeId,
                    initialFont: TelefamIosPostFont(rawValue: item.fontId) ?? .classic,
                    initialSong: item.song,
                    initialHashtags: item.hashtags,
                    initialMuteOriginalAudio: item.muteOriginalAudio
                )
            }
        }()
        TelefamIosPostView(store: store, contactsStore: contactsStore)
    }

    private var chatsTopHeader: some View {
        HStack(spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                TextField("Search people or chats", text: $chatFilterQuery)
                    .font(.system(size: 14))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    .onSubmit {
                        chatStore.runGlobalSearch(chatFilterQuery)
                        activeTab = .chats
                    }
                if !chatFilterQuery.isEmpty {
                    Button { chatFilterQuery = "" } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                }
            }
            .padding(.horizontal, 14)
            .frame(height: 42)
            .background(TelefamPalette.searchFieldBackground(colorScheme))
            .clipShape(Capsule())

            Button {
                notificationManager.showInboxSheet = true
            } label: {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: notificationManager.unreadCount > 0 ? "bell.badge.fill" : "bell.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.white.opacity(0.18))
                        .clipShape(Circle())
                    if notificationManager.unreadCount > 0 {
                        Text(notificationManager.unreadCount > 99 ? "99+" : "\(notificationManager.unreadCount)")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 4)
                            .frame(minWidth: 16, minHeight: 16)
                            .background(Color.black.opacity(0.85))
                            .clipShape(Capsule())
                            .offset(x: 4, y: -4)
                    }
                }
            }
            .accessibilityLabel("Notifications")

            Button {
                themeMode = themeMode.next
            } label: {
                Image(systemName: themeMode == .dark ? "moon.fill" : (themeMode == .light ? "sun.max.fill" : "circle.lefthalf.filled"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
            }

            Button {
                showChatPrivacy = true
            } label: {
                Image(systemName: "ellipsis")
                    .rotationEffect(.degrees(90))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
            }
            .accessibilityLabel("Privacy and chat themes")
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(TelefamPalette.crimson)
    }

    @ViewBuilder
    private func tabBody(isCompactWidth: Bool, safeBottomInset: CGFloat) -> some View {
        switch activeTab {
        case .chats:
            TelefamIosChatsHostView(
                chatStore: chatStore,
                settingsStore: settingsStore,
                chatFilterQuery: chatFilterQuery,
                isCompactWidth: isCompactWidth,
                safeBottomInset: safeBottomInset,
                onOpenUserProfile: { uid in pushProfile(uid) },
                onOpenPrivacy: { showChatPrivacy = true },
                onBrowseContacts: { selectTab(.contacts) },
                onStartCall: { conv, callType in
                    let name = conv.peerFullName.isEmpty ? conv.peerUsername : conv.peerFullName
                    callController.startCall(
                        conversationId: conv.conversationId,
                        peerUserId: conv.peerUserId,
                        peerName: name,
                        peerAvatarUrl: conv.peerAvatarUrl,
                        callType: callType
                    )
                },
                onOpenSendStars: { conv in
                    starsStore.openSendStarsFromChat(
                        peer: TelefamIosSendStarsPeerContext(
                            userId: conv.peerUserId,
                            fullName: conv.peerFullName,
                            username: conv.peerUsername,
                            avatarUrl: conv.peerAvatarUrl,
                            conversationId: conv.conversationId,
                            isOnline: conv.isOnline
                        )
                    )
                }
            )
        case .contacts:
            TelefamIosContactsView(
                store: contactsStore,
                isCompactWidth: isCompactWidth,
                onBottomBarVisibilityChange: { visible in
                    isBottomBarVisible = visible
                },
                onOpenProfile: { uid in pushProfile(uid) }
            )
            .onAppear {
                Task { await contactsStore.refresh() }
            }
        case .feeds:
            TelefamIosFeedView(
                store: feedStore,
                contactsStore: contactsStore,
                settingsStore: settingsStore,
                items: feedStore.activeItems,
                initialIndex: 0,
                isStandaloneViewer: false,
                bottomChromePadding: isCompactWidth ? (66 + safeBottomInset) : 0,
                onLiveClick: { showComingSoon("LIVE — Coming soon") },
                onSearchClick: { showFeedSearch = true },
                onBackClick: {},
                onCommentClick: { showComingSoon("Comments — Coming soon") },
                onEditOwnerPost: { item in postLaunchTarget = .edit(item: item) },
                onTapHashtag: { tag, mode in postLaunchTarget = .prefillHashtag(tag: tag, mode: mode) },
                onTapAudio: { song, mode in postLaunchTarget = .prefillSong(song: song, mode: mode) },
                onOpenAuthorProfile: { uid in pushProfile(uid) }
            )
            .onAppear {
                Task { await feedStore.refresh() }
            }
        case .post:
            EmptyView()
        case .profile:
            TelefamIosProfileView(
                targetUserId: sessionStore.userId.isEmpty ? "me" : sessionStore.userId,
                isEmbeddedOwnProfileTab: true,
                bottomChromePadding: isCompactWidth ? (68 + safeBottomInset) : 0,
                baseURL: serverURL,
                sessionStore: sessionStore,
                contactsStore: contactsStore,
                feedStore: feedStore,
                settingsStore: settingsStore,
                themeMode: $themeMode,
                onBack: {},
                onOpenUserProfile: { uid in pushProfile(uid) },
                onEditOwnerPost: { item in postLaunchTarget = .edit(item: item) },
                onTapHashtag: { tag, mode in postLaunchTarget = .prefillHashtag(tag: tag, mode: mode) },
                onTapAudio: { song, mode in postLaunchTarget = .prefillSong(song: song, mode: mode) },
                onMessageUser: { prof in launchChat(with: prof) },
                onOpenStars: { starsStore.openStarsHub() },
                onOpenGetVerified: { tier in
                    verificationStore.openGetVerified(
                        profileCountryIso: sessionStore.countryIso,
                        fullName: sessionStore.fullName,
                        username: sessionStore.username,
                        initialVerifiedTier: tier
                    )
                }
            )
        }
    }

    private var bottomBar: some View {
        VStack(spacing: 0) {
            Divider().background(TelefamPalette.border(colorScheme).opacity(0.6))
            HStack {
                navItem(.chats, title: "Chats", icon: activeTab == .chats ? "bubble.left.fill" : "bubble.left")
                Spacer()
                navItem(.contacts, title: "Contacts", icon: activeTab == .contacts ? "person.2.fill" : "person.2")
                Spacer()
                Button { selectTab(.post) } label: {
                    VStack(spacing: 2) {
                        Image(systemName: "plus")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(TelefamPalette.crimson)
                            .clipShape(Circle())
                        Text("Post")
                            .font(.system(size: 10.5, weight: .medium))
                            .foregroundColor(activeTab == .post ? TelefamPalette.crimson : TelefamPalette.textMuted(colorScheme))
                    }
                }
                Spacer()
                navItem(.feeds, title: "Feeds", icon: activeTab == .feeds ? "square.stack.fill" : "square.stack")
                Spacer()
                navItem(.profile, title: "Profile", icon: activeTab == .profile ? "person.circle.fill" : "person.circle")
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 10)
            .background(TelefamPalette.surface(colorScheme))
        }
    }

    private var sideRail: some View {
        VStack(spacing: 22) {
            navItem(.chats, title: "Chats", icon: activeTab == .chats ? "bubble.left.fill" : "bubble.left")
            navItem(.contacts, title: "Contacts", icon: activeTab == .contacts ? "person.2.fill" : "person.2")
            Button { selectTab(.post) } label: {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(TelefamPalette.crimson)
                    .clipShape(Circle())
            }
            navItem(.feeds, title: "Feeds", icon: activeTab == .feeds ? "square.stack.fill" : "square.stack")
            navItem(.profile, title: "Profile", icon: activeTab == .profile ? "person.circle.fill" : "person.circle")
            Button {
                notificationManager.showInboxSheet = true
            } label: {
                VStack(spacing: 3) {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: notificationManager.unreadCount > 0 ? "bell.badge.fill" : "bell")
                            .font(.system(size: 19))
                        if notificationManager.unreadCount > 0 {
                            Text(notificationManager.unreadCount > 99 ? "99+" : "\(notificationManager.unreadCount)")
                                .font(.system(size: 9.5, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 4.5)
                                .frame(minWidth: 16, minHeight: 16)
                                .background(TelefamPalette.crimson)
                                .clipShape(Capsule())
                                .offset(x: 10, y: -6)
                        }
                    }
                    Text("Alerts")
                        .font(.system(size: 10.5, weight: .medium))
                }
                .foregroundColor(TelefamPalette.textMuted(colorScheme))
            }
            .accessibilityLabel("Notifications")
            Spacer()
        }
        .padding(.vertical, 20)
        .frame(width: 86)
        .background(TelefamPalette.surface(colorScheme))
    }

    private var unreadChatCount: Int {
        chatStore.conversations.reduce(0) { $0 + max(0, $1.unreadCount) }
    }

    private func navItem(_ tab: TelefamIosHomeTab, title: String, icon: String) -> some View {
        let selected = activeTab == tab
        let tint = selected ? TelefamPalette.crimson : TelefamPalette.textMuted(colorScheme)
        let badge = (tab == .chats) ? unreadChatCount : 0
        return Button {
            selectTab(tab)
        } label: {
            VStack(spacing: 3) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: icon)
                        .font(.system(size: 19))
                    if badge > 0 {
                        Text(badge > 99 ? "99+" : "\(badge)")
                            .font(.system(size: 9.5, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 4.5)
                            .frame(minWidth: 16, minHeight: 16)
                            .background(TelefamPalette.crimson)
                            .clipShape(Capsule())
                            .offset(x: 10, y: -6)
                    }
                }
                Text(title)
                    .font(.system(size: 10.5, weight: selected ? .semibold : .medium))
            }
            .foregroundColor(tint)
        }
    }

    private func selectTab(_ tab: TelefamIosHomeTab) {
        if tab == .post {
            postLaunchTarget = .createNew
            return
        }
        activeTab = tab
        isBottomBarVisible = true
        if tab == .feeds {
            Task { await feedStore.refresh() }
        }
    }

    private func launchChat(with prof: TelefamIosUserProfile) {
        profileBackStack.removeAll()
        showFeedSearch = false
        activeTab = .chats
        isBottomBarVisible = true
        Task {
            await chatStore.openDirectConversation(
                peerUserId: prof.userId,
                fullName: prof.fullName,
                username: prof.username,
                avatarUrl: prof.avatarUrl
            )
        }
    }

    private func handleNotificationRoute(_ route: TelefamIosNotificationRoute) {
        profileBackStack.removeAll()
        showFeedSearch = false
        showChatPrivacy = false
        switch route {
        case .chat(let convId, let peerUserId):
            activeTab = .chats
            isBottomBarVisible = true
            if !convId.isEmpty {
                chatStore.openConversation(convId)
            } else if !peerUserId.isEmpty {
                Task {
                    await chatStore.openDirectConversation(peerUserId: peerUserId, fullName: "", username: "", avatarUrl: "")
                }
            }
        case .profile(let uid):
            pushProfile(uid)
        case .post:
            activeTab = .feeds
            isBottomBarVisible = true
            Task { await feedStore.refresh() }
        case .stars:
            starsStore.openStarsHub()
        case .verification:
            verificationStore.openGetVerified(
                profileCountryIso: sessionStore.countryIso,
                fullName: sessionStore.fullName,
                username: sessionStore.username
            )
        case .inbox:
            notificationManager.showInboxSheet = true
        }
    }

    private func showComingSoon(_ msg: String) {
        comingSoonBanner = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            if comingSoonBanner == msg { comingSoonBanner = nil }
        }
    }
}

private struct RoundedCornerShapeShim: Shape {
    let radius: CGFloat
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
