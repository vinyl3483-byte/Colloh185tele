import SwiftUI
import Network

enum TelefamContactFilter: String, CaseIterable, Identifiable {
    case all
    case suggestions
    case followers
    case following
    case friends

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: return "All"
        case .suggestions: return "Suggestions"
        case .followers: return "Followers"
        case .following: return "Following"
        case .friends: return "Friends"
        }
    }

    var sectionSubtitle: String {
        title.uppercased()
    }
}

enum TelefamRelationshipState {
    case follow
    case following
    case followBack
    case friends

    static func compute(isFollowing: Bool, followsYou: Bool) -> TelefamRelationshipState {
        if isFollowing && followsYou { return .friends }
        if isFollowing { return .following }
        if followsYou { return .followBack }
        return .follow
    }

    var label: String {
        switch self {
        case .follow: return "Follow"
        case .following: return "Following"
        case .followBack: return "Follow back"
        case .friends: return "Friends"
        }
    }

    var systemIcon: String {
        switch self {
        case .follow, .followBack: return "person.badge.plus"
        case .following: return "checkmark"
        case .friends: return "person.2"
        }
    }
}

struct TelefamContactRecord: Codable, Identifiable, Equatable {
    let userId: String
    let fullName: String
    let username: String
    let location: String
    let countryIso: String
    let avatarUrl: String?
    var isFollowing: Bool
    var followsYou: Bool
    var riskScore: Double
    var updatedAtMs: Int64

    var id: String { userId }

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case fullName = "full_name"
        case username
        case location
        case countryIso = "country_iso"
        case avatarUrl = "avatar_url"
        case isFollowing = "is_following"
        case followsYou = "follows_you"
        case riskScore = "risk_score"
        case updatedAtMs = "updated_at_ms"
    }

    var countryDisplay: String {
        if !countryIso.isEmpty,
           let localized = Locale(identifier: "en_US").localizedString(forRegionCode: countryIso) {
            return localized
        }
        return location
    }

    var initials: String {
        let words = fullName.split(separator: " ")
        if words.count >= 2, let a = words.first?.first, let b = words.last?.first {
            return "\(a)\(b)".uppercased()
        }
        if let first = words.first, first.count >= 2 {
            return String(first.prefix(2)).uppercased()
        }
        return String(username.prefix(2)).uppercased()
    }

    var relationship: TelefamRelationshipState {
        TelefamRelationshipState.compute(isFollowing: isFollowing, followsYou: followsYou)
    }
}

private struct ContactsPagePayload: Codable {
    let success: Bool
    let items: [TelefamContactRecord]
    let page: Int
    let pageSize: Int
    let hasMore: Bool

    enum CodingKeys: String, CodingKey {
        case success, items, page
        case pageSize = "page_size"
        case hasMore = "has_more"
    }
}

private struct ContactPendingSyncOp: Codable {
    let clientOpId: String
    let targetUserId: String
    let wantFollow: Bool
    let createdAtMs: Int64
}

@MainActor
final class TelefamContactsStore: ObservableObject {
    @Published var allCached: [TelefamContactRecord] = []
    @Published var activeFilter: TelefamContactFilter = .all
    @Published var searchQuery: String = ""
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var hasMore: Bool = false
    @Published var currentPage: Int = 1
    @Published var toastMessage: String? = nil

    private var actionTimestamps: [Date] = []
    private var pendingSyncOps: [ContactPendingSyncOp] = []
    private var isOnline: Bool = true
    private let pathMonitor = NWPathMonitor()

    private let baseUrl: String
    private let sessionTokenProvider: () -> String?
    private let deviceFingerprintProvider: () -> String

    private var cacheFileURL: URL {
        let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        return dir.appendingPathComponent("telefam_contacts_cache.json")
    }

    private var pendingOpsFileURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("telefam_contacts_pending.json")
    }

    var currentToken: String { sessionTokenProvider() ?? "" }
    var currentDeviceFingerprint: String { deviceFingerprintProvider() }

    func resolvedURLString(_ raw: String?) -> String {
        guard let raw, !raw.isEmpty else { return "" }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return raw
        }
        let baseStr = baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let trimmed = raw.hasPrefix("/") ? String(raw.dropFirst()) : raw
        return baseStr.isEmpty ? raw : "\(baseStr)/\(trimmed)"
    }

    init(
        baseUrl: String,
        sessionTokenProvider: @escaping () -> String?,
        deviceFingerprintProvider: @escaping () -> String
    ) {
        self.baseUrl = baseUrl
        self.sessionTokenProvider = sessionTokenProvider
        self.deviceFingerprintProvider = deviceFingerprintProvider
        loadLocalCaches()
        startNetworkMonitor()
    }

    var filteredContacts: [TelefamContactRecord] {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let filtered = allCached.filter { contact in
            switch activeFilter {
            case .all:
                break
            case .suggestions:
                if contact.isFollowing || contact.riskScore >= 85.0 { return false }
            case .followers:
                if !contact.followsYou { return false }
            case .following:
                if !contact.isFollowing { return false }
            case .friends:
                if !(contact.isFollowing && contact.followsYou) { return false }
            }
            if q.isEmpty { return true }
            return contact.fullName.lowercased().contains(q) || contact.username.lowercased().contains(q)
        }
        if activeFilter == .suggestions {
            return filtered.sorted { lhs, rhs in
                if lhs.followsYou != rhs.followsYou { return lhs.followsYou && !rhs.followsYou }
                if lhs.riskScore != rhs.riskScore { return lhs.riskScore < rhs.riskScore }
                return lhs.fullName.lowercased() < rhs.fullName.lowercased()
            }
        }
        return filtered.sorted { $0.fullName.lowercased() < $1.fullName.lowercased() }
    }

    func refresh() async {
        await fetchPage(page: 1, append: false)
    }

    func loadNextPageIfNeeded(currentItem: TelefamContactRecord) {
        guard hasMore, !isLoading, !isLoadingMore else { return }
        let items = filteredContacts
        guard let idx = items.firstIndex(where: { $0.userId == currentItem.userId }),
              idx >= items.count - 4 else { return }
        Task {
            await fetchPage(page: currentPage + 1, append: true)
        }
    }

    func toggleFollow(for targetUserId: String) {
        let now = Date()
        actionTimestamps = actionTimestamps.filter { now.timeIntervalSince($0) < 60.0 }
        if actionTimestamps.count >= 20 {
            toastMessage = "Try again later"
            return
        }
        guard let index = allCached.firstIndex(where: { $0.userId == targetUserId }) else { return }
        actionTimestamps.append(now)

        let previous = allCached[index]
        let wantFollow = !previous.isFollowing
        allCached[index].isFollowing = wantFollow
        persistLocalCaches()

        Task {
            let synced = await sendFollowMutation(targetUserId: targetUserId, wantFollow: wantFollow, previousState: previous)
            if !synced {
                enqueuePendingOp(targetUserId: targetUserId, wantFollow: wantFollow)
            }
        }
    }

    private func fetchPage(page: Int, append: Bool) async {
        guard !baseUrl.isEmpty, let token = sessionTokenProvider(), !token.isEmpty else { return }
        if append { isLoadingMore = true } else { isLoading = true }
        defer {
            isLoading = false
            isLoadingMore = false
        }

        var components = URLComponents(string: "\(baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/")))/v1/contacts")
        components?.queryItems = [
            URLQueryItem(name: "filter", value: activeFilter.rawValue),
            URLQueryItem(name: "q", value: searchQuery),
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "page_size", value: "20")
        ]
        guard let url = components?.url else { return }

        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprintProvider(), forHTTPHeaderField: "X-Telefam-Device-FP")
        req.timeoutInterval = 12

        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return }
            let decoded = try JSONDecoder().decode(ContactsPagePayload.self, from: data)
            let pendingByTarget = Dictionary(uniqueKeysWithValues: pendingSyncOps.map { ($0.targetUserId, $0) })

            var mapById = Dictionary(uniqueKeysWithValues: allCached.map { ($0.userId, $0) })
            for var item in decoded.items {
                if let pending = pendingByTarget[item.userId] {
                    item.isFollowing = pending.wantFollow
                }
                mapById[item.userId] = item
            }
            allCached = Array(mapById.values)
            currentPage = decoded.page
            hasMore = decoded.hasMore
            persistLocalCaches()
        } catch {
            // Retain cached contacts seamlessly on weak or offline network
        }
    }

    private func sendFollowMutation(
        targetUserId: String,
        wantFollow: Bool,
        previousState: TelefamContactRecord
    ) async -> Bool {
        guard isOnline, !baseUrl.isEmpty, let token = sessionTokenProvider(), !token.isEmpty else {
            return false
        }
        let endpoint = wantFollow ? "follow" : "unfollow"
        guard let url = URL(string: "\(baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/")))/v1/contacts/\(endpoint)") else {
            return false
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprintProvider(), forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["target_user_id": targetUserId])

        do {
            let (_, response) = try await URLSession.shared.data(for: req)
            guard let http = response as? HTTPURLResponse else { return false }
            if http.statusCode == 429 {
                if let idx = allCached.firstIndex(where: { $0.userId == targetUserId }) {
                    allCached[idx] = previousState
                    persistLocalCaches()
                }
                toastMessage = "Try again later"
                return true
            }
            if http.statusCode == 200 {
                pendingSyncOps.removeAll { $0.targetUserId == targetUserId }
                persistLocalCaches()
                return true
            }
            return false
        } catch {
            return false
        }
    }

    private func enqueuePendingOp(targetUserId: String, wantFollow: Bool) {
        pendingSyncOps.removeAll { $0.targetUserId == targetUserId }
        pendingSyncOps.append(
            ContactPendingSyncOp(
                clientOpId: "ios_\(UUID().uuidString)",
                targetUserId: targetUserId,
                wantFollow: wantFollow,
                createdAtMs: Int64(Date().timeIntervalSince1970 * 1000)
            )
        )
        persistLocalCaches()
    }

    private func flushPendingOps() async {
        guard isOnline, !pendingSyncOps.isEmpty, !baseUrl.isEmpty,
              let token = sessionTokenProvider(), !token.isEmpty,
              let url = URL(string: "\(baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/")))/v1/contacts/sync") else {
            return
        }
        let payload = pendingSyncOps.map {
            [
                "client_op_id": $0.clientOpId,
                "target_user_id": $0.targetUserId,
                "action": $0.wantFollow ? "follow" : "unfollow",
                "created_at_ms": $0.createdAtMs
            ] as [String: Any]
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprintProvider(), forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["actions": payload])

        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, http.statusCode == 200 {
            pendingSyncOps.removeAll()
            persistLocalCaches()
        }
    }

    private func loadLocalCaches() {
        if let data = try? Data(contentsOf: cacheFileURL),
           let decoded = try? JSONDecoder().decode([TelefamContactRecord].self, from: data) {
            allCached = decoded
        }
        if let opData = try? Data(contentsOf: pendingOpsFileURL),
           let ops = try? JSONDecoder().decode([ContactPendingSyncOp].self, from: opData) {
            pendingSyncOps = ops
        }
    }

    private func persistLocalCaches() {
        if let data = try? JSONEncoder().encode(allCached) {
            try? data.write(to: cacheFileURL, options: .atomic)
        }
        if let opData = try? JSONEncoder().encode(pendingSyncOps) {
            try? opData.write(to: pendingOpsFileURL, options: .atomic)
        }
    }

    private func startNetworkMonitor() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            let online = path.status == .satisfied
            Task { @MainActor [weak self] in
                guard let self else { return }
                let wasOffline = !self.isOnline
                self.isOnline = online
                if wasOffline && online {
                    await self.flushPendingOps()
                    await self.refresh()
                }
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "telefam.ios.contacts.network"))
    }
}

struct TelefamContactsTopHeaderView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var store: TelefamContactsStore
    @Binding var themeMode: TelefamThemeMode
    var onOpenPrivacy: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))

                    TextField("Search by name or username", text: $store.searchQuery)
                        .font(.system(size: 15))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)

                    if !store.searchQuery.isEmpty {
                        Button {
                            store.searchQuery = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 16))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                }
                .padding(.horizontal, 14)
                .frame(height: 44)
                .background(TelefamPalette.searchFieldBackground(colorScheme))
                .clipShape(Capsule())

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

                if let onOpenPrivacy {
                    Button(action: onOpenPrivacy) {
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
            }
            .padding(.horizontal, 14)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(TelefamContactFilter.allCases) { tab in
                        let selected = tab == store.activeFilter
                        Button {
                            store.activeFilter = tab
                            Task { await store.refresh() }
                        } label: {
                            Text(tab.title)
                                .font(.system(size: 14, weight: selected ? .bold : .semibold))
                                .foregroundColor(selected ? TelefamPalette.crimson : .white)
                                .padding(.horizontal, selected ? 16 : 10)
                                .padding(.vertical, 7)
                                .background(selected ? Color.white : Color.clear)
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(.horizontal, 14)
            }
        }
        .padding(.top, 10)
        .padding(.bottom, 12)
        .background(TelefamPalette.crimson)
    }
}

struct TelefamIosContactsView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var store: TelefamContactsStore
    let isCompactWidth: Bool
    let onBottomBarVisibilityChange: (Bool) -> Void
    var onOpenProfile: (String) -> Void = { _ in }

    @State private var lastScrollY: CGFloat = 0
    @State private var scrollStopWorkItem: DispatchWorkItem?

    var body: some View {
        GeometryReader { geo in
            let columnsCount = geo.size.width >= 960 ? 3 : (geo.size.width >= 600 ? 2 : 1)
            let contacts = store.filteredContacts

            Group {
                if store.isLoading && contacts.isEmpty {
                    VStack {
                        Spacer()
                        ProgressView()
                            .tint(TelefamPalette.crimson)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else if contacts.isEmpty {
                    VStack(spacing: 10) {
                        Spacer()
                        Image(systemName: "person.2")
                            .font(.system(size: 28))
                            .foregroundColor(TelefamPalette.crimson)
                            .frame(width: 62, height: 62)
                            .background(TelefamPalette.blush(colorScheme))
                            .clipShape(Circle())
                        Text("No people found")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text("Try a different search or filter tab.")
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            GeometryReader { offsetProxy in
                                Color.clear
                                    .preference(
                                        key: ScrollOffsetPreferenceKey.self,
                                        value: offsetProxy.frame(in: .named("contactsScroll")).minY
                                    )
                            }
                            .frame(height: 0)

                            Text(store.activeFilter.sectionSubtitle)
                                .font(.system(size: 12, weight: .semibold))
                                .tracking(0.8)
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                .padding(.horizontal, columnsCount > 1 ? 20 : 16)
                                .padding(.top, 10)

                            if columnsCount > 1 {
                                let gridCols = Array(repeating: GridItem(.flexible(), spacing: 12), count: columnsCount)
                                LazyVGrid(columns: gridCols, spacing: 10) {
                                    ForEach(contacts) { contact in
                                        ContactRowCard(
                                            contact: contact,
                                            avatarResolvedUrl: store.resolvedURLString(contact.avatarUrl),
                                            bearerToken: store.currentToken,
                                            deviceFingerprint: store.currentDeviceFingerprint,
                                            cardStyle: true,
                                            onOpenProfile: { onOpenProfile(contact.userId) }
                                        ) {
                                            store.toggleFollow(for: contact.userId)
                                        }
                                        .onAppear { store.loadNextPageIfNeeded(currentItem: contact) }
                                    }
                                }
                                .padding(.horizontal, 20)
                                .frame(maxWidth: 1180)
                            } else {
                                LazyVStack(spacing: 0) {
                                    ForEach(contacts) { contact in
                                        ContactRowCard(
                                            contact: contact,
                                            avatarResolvedUrl: store.resolvedURLString(contact.avatarUrl),
                                            bearerToken: store.currentToken,
                                            deviceFingerprint: store.currentDeviceFingerprint,
                                            cardStyle: false,
                                            onOpenProfile: { onOpenProfile(contact.userId) }
                                        ) {
                                            store.toggleFollow(for: contact.userId)
                                        }
                                        .onAppear { store.loadNextPageIfNeeded(currentItem: contact) }
                                    }
                                }
                            }

                            if store.isLoadingMore {
                                HStack {
                                    Spacer()
                                    ProgressView().tint(TelefamPalette.crimson)
                                    Spacer()
                                }
                                .padding(.vertical, 14)
                            }
                        }
                        .padding(.bottom, isCompactWidth ? 88 : 24)
                    }
                    .coordinateSpace(name: "contactsScroll")
                    .onPreferenceChange(ScrollOffsetPreferenceKey.self) { minY in
                        let delta = minY - lastScrollY
                        lastScrollY = minY
                        if delta < -6 {
                            onBottomBarVisibilityChange(false)
                        } else if delta > 6 {
                            onBottomBarVisibilityChange(true)
                        }
                        scrollStopWorkItem?.cancel()
                        let item = DispatchWorkItem {
                            onBottomBarVisibilityChange(true)
                        }
                        scrollStopWorkItem = item
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22, execute: item)
                    }
                }
            }
            .background(TelefamPalette.background(colorScheme))
        }
        .onChange(of: store.searchQuery) { _ in
            Task {
                try? await Task.sleep(nanoseconds: 280_000_000)
                await store.refresh()
            }
        }
    }
}

private struct ContactRowCard: View {
    @Environment(\.colorScheme) private var colorScheme
    let contact: TelefamContactRecord
    let avatarResolvedUrl: String
    let bearerToken: String
    let deviceFingerprint: String
    let cardStyle: Bool
    var onOpenProfile: () -> Void = {}
    let onToggleFollow: () -> Void

    var body: some View {
        let content = HStack(spacing: 12) {
            HStack(spacing: 12) {
                avatarView
                VStack(alignment: .leading, spacing: 2) {
                    Text(contact.fullName)
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .lineLimit(1)
                    let sub = contact.countryDisplay.isEmpty ? "@\(contact.username)" : "@\(contact.username) · \(contact.countryDisplay)"
                    Text(sub)
                        .font(.system(size: 13.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .lineLimit(1)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { onOpenProfile() }
            Spacer(minLength: 8)
            relationshipButton
        }
        .padding(.horizontal, cardStyle ? 14 : 16)
        .padding(.vertical, cardStyle ? 12 : 10)

        if cardStyle {
            content
                .background(TelefamPalette.surface(colorScheme))
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(TelefamPalette.border(colorScheme).opacity(0.65), lineWidth: 1)
                )
        } else {
            content
        }
    }

    @ViewBuilder
    private var avatarView: some View {
        if !avatarResolvedUrl.isEmpty {
            TelefamIosRemoteImage(
                urlString: avatarResolvedUrl,
                bearerToken: bearerToken,
                deviceFingerprint: deviceFingerprint,
                fallbackInitials: contact.initials
            )
            .frame(width: 48, height: 48)
            .clipShape(Circle())
        } else {
            initialsBadge
                .frame(width: 48, height: 48)
                .clipShape(Circle())
        }
    }

    private var initialsBadge: some View {
        let paletteIdx = abs(contact.userId.hashValue) % 4
        let bg: Color = {
            switch paletteIdx {
            case 0: return colorScheme == .dark ? Color(red: 0.28, green: 0.07, blue: 0.08) : Color(red: 0.99, green: 0.91, blue: 0.91)
            case 1: return colorScheme == .dark ? Color(red: 0.18, green: 0.12, blue: 0.29) : Color(red: 0.93, green: 0.91, blue: 1.0)
            case 2: return colorScheme == .dark ? Color(red: 0.08, green: 0.20, blue: 0.26) : Color(red: 0.88, green: 0.95, blue: 1.0)
            default: return colorScheme == .dark ? Color(red: 0.10, green: 0.23, blue: 0.16) : Color(red: 0.82, green: 0.98, blue: 0.90)
            }
        }()
        let fg: Color = {
            switch paletteIdx {
            case 0: return colorScheme == .dark ? Color(red: 0.97, green: 0.44, blue: 0.44) : TelefamPalette.crimson
            case 1: return colorScheme == .dark ? Color(red: 0.77, green: 0.71, blue: 0.99) : Color(red: 0.36, green: 0.13, blue: 0.71)
            case 2: return colorScheme == .dark ? Color(red: 0.49, green: 0.83, blue: 0.99) : Color(red: 0.01, green: 0.41, blue: 0.63)
            default: return colorScheme == .dark ? Color(red: 0.43, green: 0.91, blue: 0.72) : Color(red: 0.02, green: 0.47, blue: 0.34)
            }
        }()

        return ZStack {
            bg
            Text(contact.initials)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(fg)
        }
    }

    private var relationshipButton: some View {
        let rel = contact.relationship
        let bg: Color = {
            switch rel {
            case .follow, .followBack: return colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
            case .following: return TelefamPalette.surfaceVariant(colorScheme)
            case .friends: return TelefamPalette.blush(colorScheme)
            }
        }()
        let fg: Color = {
            switch rel {
            case .follow, .followBack: return .white
            case .following: return TelefamPalette.textPrimary(colorScheme)
            case .friends: return colorScheme == .dark ? Color(red: 1.0, green: 0.54, blue: 0.56) : TelefamPalette.crimson
            }
        }()

        return Button(action: onToggleFollow) {
            HStack(spacing: 6) {
                Image(systemName: rel.systemIcon)
                    .font(.system(size: 13, weight: .semibold))
                Text(rel.label)
                    .font(.system(size: 13.5, weight: .semibold))
            }
            .foregroundColor(fg)
            .padding(.horizontal, 15)
            .frame(height: 38)
            .background(bg)
            .clipShape(Capsule())
            .overlay(
                Capsule().stroke(
                    rel == .following
                        ? TelefamPalette.border(colorScheme)
                        : (rel == .friends ? TelefamPalette.crimson.opacity(0.5) : Color.clear),
                    lineWidth: 1
                )
            )
        }
    }
}

private struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}
