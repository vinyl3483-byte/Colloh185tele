import SwiftUI
import AVFoundation
import Network

enum TelefamIosFeedTab: String, Codable {
    case following
    case forYou = "for_you"

    var label: String {
        switch self {
        case .following: return "Following"
        case .forYou: return "For You"
        }
    }
}

struct TelefamIosVideoRendition: Codable, Hashable {
    let qualityLabel: String
    let width: Int
    let height: Int
    let bitrateKbps: Int
    let url: String
    let byteSize: Int

    enum CodingKeys: String, CodingKey {
        case qualityLabel = "quality_label"
        case width, height
        case bitrateKbps = "bitrate_kbps"
        case url
        case byteSize = "byte_size"
    }
}

struct TelefamIosFeedItem: Identifiable, Codable {
    let postId: String
    let authorUserId: String
    let authorUsername: String
    let authorFullName: String
    let authorAvatarUrl: String
    var isFollowingAuthor: Bool
    let isOwner: Bool
    let mode: String
    let textContent: String
    let caption: String
    let themeId: String
    let fontId: String
    let song: TelefamIosSongTrack?
    let hashtags: [String]
    let photoUrls: [String]
    let videoUrl: String
    let thumbnailUrl: String
    let renditions: [TelefamIosVideoRendition]
    let width: Int
    let height: Int
    let durationMs: Int64
    let muteOriginalAudio: Bool
    let contentId: String
    var likesCount: Int64
    var commentsCount: Int64
    var sharesCount: Int64
    var favouritesCount: Int64
    var repostsCount: Int64
    var viewsCount: Int64
    var isLiked: Bool
    var isFavourited: Bool
    var isReposted: Bool
    let repostedByUsername: String
    let recommendationLabel: String
    let createdAtMs: Int64
    var privacy: String
    let authorVerifiedTier: String

    var id: String { postId }

    enum CodingKeys: String, CodingKey {
        case postId = "post_id"
        case authorUserId = "author_user_id"
        case authorUsername = "author_username"
        case authorFullName = "author_full_name"
        case authorAvatarUrl = "author_avatar_url"
        case isFollowingAuthor = "is_following_author"
        case isOwner = "is_owner"
        case mode
        case textContent = "text_content"
        case caption
        case themeId = "theme_id"
        case fontId = "font_id"
        case song, hashtags
        case photoUrls = "photo_urls"
        case videoUrl = "video_url"
        case thumbnailUrl = "thumbnail_url"
        case renditions, width, height
        case durationMs = "duration_ms"
        case muteOriginalAudio = "mute_original_audio"
        case contentId = "content_id"
        case likesCount = "likes_count"
        case commentsCount = "comments_count"
        case sharesCount = "shares_count"
        case favouritesCount = "favourites_count"
        case repostsCount = "reposts_count"
        case viewsCount = "views_count"
        case isLiked = "is_liked"
        case isFavourited = "is_favourited"
        case isReposted = "is_reposted"
        case repostedByUsername = "reposted_by_username"
        case recommendationLabel = "recommendation_label"
        case createdAtMs = "created_at_ms"
        case privacy
        case authorVerifiedTier = "author_verified_tier"
    }

    init(
        postId: String,
        authorUserId: String = "",
        authorUsername: String = "",
        authorFullName: String = "",
        authorAvatarUrl: String = "",
        isFollowingAuthor: Bool = false,
        isOwner: Bool = false,
        mode: String = "video",
        textContent: String = "",
        caption: String = "",
        themeId: String = "crimson",
        fontId: String = "classic",
        song: TelefamIosSongTrack? = nil,
        hashtags: [String] = [],
        photoUrls: [String] = [],
        videoUrl: String = "",
        thumbnailUrl: String = "",
        renditions: [TelefamIosVideoRendition] = [],
        width: Int = 0,
        height: Int = 0,
        durationMs: Int64 = 0,
        muteOriginalAudio: Bool = false,
        contentId: String = "",
        likesCount: Int64 = 0,
        commentsCount: Int64 = 0,
        sharesCount: Int64 = 0,
        favouritesCount: Int64 = 0,
        repostsCount: Int64 = 0,
        viewsCount: Int64 = 0,
        isLiked: Bool = false,
        isFavourited: Bool = false,
        isReposted: Bool = false,
        repostedByUsername: String = "",
        recommendationLabel: String = "",
        createdAtMs: Int64 = 0,
        privacy: String = "public",
        authorVerifiedTier: String = "none"
    ) {
        self.postId = postId
        self.authorUserId = authorUserId
        self.authorUsername = authorUsername
        self.authorFullName = authorFullName
        self.authorAvatarUrl = authorAvatarUrl
        self.isFollowingAuthor = isFollowingAuthor
        self.isOwner = isOwner
        self.mode = mode
        self.textContent = textContent
        self.caption = caption
        self.themeId = themeId
        self.fontId = fontId
        self.song = song
        self.hashtags = hashtags
        self.photoUrls = photoUrls
        self.videoUrl = videoUrl
        self.thumbnailUrl = thumbnailUrl
        self.renditions = renditions
        self.width = width
        self.height = height
        self.durationMs = durationMs
        self.muteOriginalAudio = muteOriginalAudio
        self.contentId = contentId
        self.likesCount = likesCount
        self.commentsCount = commentsCount
        self.sharesCount = sharesCount
        self.favouritesCount = favouritesCount
        self.repostsCount = repostsCount
        self.viewsCount = viewsCount
        self.isLiked = isLiked
        self.isFavourited = isFavourited
        self.isReposted = isReposted
        self.repostedByUsername = repostedByUsername
        self.recommendationLabel = recommendationLabel
        self.createdAtMs = createdAtMs
        self.privacy = privacy
        self.authorVerifiedTier = authorVerifiedTier
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        postId = try c.decodeIfPresent(String.self, forKey: .postId) ?? ""
        authorUserId = try c.decodeIfPresent(String.self, forKey: .authorUserId) ?? ""
        authorUsername = try c.decodeIfPresent(String.self, forKey: .authorUsername) ?? ""
        authorFullName = try c.decodeIfPresent(String.self, forKey: .authorFullName) ?? ""
        authorAvatarUrl = try c.decodeIfPresent(String.self, forKey: .authorAvatarUrl) ?? ""
        isFollowingAuthor = try c.decodeIfPresent(Bool.self, forKey: .isFollowingAuthor) ?? false
        isOwner = try c.decodeIfPresent(Bool.self, forKey: .isOwner) ?? false
        mode = try c.decodeIfPresent(String.self, forKey: .mode) ?? "video"
        textContent = try c.decodeIfPresent(String.self, forKey: .textContent) ?? ""
        caption = try c.decodeIfPresent(String.self, forKey: .caption) ?? ""
        themeId = try c.decodeIfPresent(String.self, forKey: .themeId) ?? "crimson"
        fontId = try c.decodeIfPresent(String.self, forKey: .fontId) ?? "classic"
        song = try c.decodeIfPresent(TelefamIosSongTrack.self, forKey: .song)
        hashtags = try c.decodeIfPresent([String].self, forKey: .hashtags) ?? []
        photoUrls = try c.decodeIfPresent([String].self, forKey: .photoUrls) ?? []
        videoUrl = try c.decodeIfPresent(String.self, forKey: .videoUrl) ?? ""
        thumbnailUrl = try c.decodeIfPresent(String.self, forKey: .thumbnailUrl) ?? ""
        renditions = try c.decodeIfPresent([TelefamIosVideoRendition].self, forKey: .renditions) ?? []
        width = try c.decodeIfPresent(Int.self, forKey: .width) ?? 0
        height = try c.decodeIfPresent(Int.self, forKey: .height) ?? 0
        durationMs = try c.decodeIfPresent(Int64.self, forKey: .durationMs) ?? 0
        muteOriginalAudio = try c.decodeIfPresent(Bool.self, forKey: .muteOriginalAudio) ?? false
        contentId = try c.decodeIfPresent(String.self, forKey: .contentId) ?? ""
        likesCount = try c.decodeIfPresent(Int64.self, forKey: .likesCount) ?? 0
        commentsCount = try c.decodeIfPresent(Int64.self, forKey: .commentsCount) ?? 0
        sharesCount = try c.decodeIfPresent(Int64.self, forKey: .sharesCount) ?? 0
        favouritesCount = try c.decodeIfPresent(Int64.self, forKey: .favouritesCount) ?? 0
        repostsCount = try c.decodeIfPresent(Int64.self, forKey: .repostsCount) ?? 0
        viewsCount = try c.decodeIfPresent(Int64.self, forKey: .viewsCount) ?? 0
        isLiked = try c.decodeIfPresent(Bool.self, forKey: .isLiked) ?? false
        isFavourited = try c.decodeIfPresent(Bool.self, forKey: .isFavourited) ?? false
        isReposted = try c.decodeIfPresent(Bool.self, forKey: .isReposted) ?? false
        repostedByUsername = try c.decodeIfPresent(String.self, forKey: .repostedByUsername) ?? ""
        recommendationLabel = try c.decodeIfPresent(String.self, forKey: .recommendationLabel) ?? ""
        createdAtMs = try c.decodeIfPresent(Int64.self, forKey: .createdAtMs) ?? 0
        privacy = try c.decodeIfPresent(String.self, forKey: .privacy) ?? "public"
        authorVerifiedTier = try c.decodeIfPresent(String.self, forKey: .authorVerifiedTier) ?? "none"
    }
}

struct TelefamIosSearchShortcut: Identifiable, Codable {
    let key: String
    let label: String
    let count: Int64
    var id: String { key }
}

private struct TelefamIosPendingFeedOp: Codable {
    let clientOpId: String
    let postId: String
    let action: String
    let watchMs: Int64
    let durationMs: Int64
    let reason: String

    enum CodingKeys: String, CodingKey {
        case clientOpId = "client_op_id"
        case postId = "post_id"
        case action
        case watchMs = "watch_ms"
        case durationMs = "duration_ms"
        case reason
    }
}

@MainActor
final class TelefamIosFeedStore: ObservableObject {
    @Published var activeTab: TelefamIosFeedTab = .forYou
    @Published var followingItems: [TelefamIosFeedItem] = []
    @Published var forYouItems: [TelefamIosFeedItem] = []
    @Published var isLoading: Bool = false
    @Published var hasMoreFollowing: Bool = false
    @Published var hasMoreForYou: Bool = false
    @Published var isWeakNetwork: Bool = false
    @Published var toastMessage: String? = nil

    @Published var searchQuery: String = ""
    @Published var activeShortcut: String = ""
    @Published var searchShortcuts: [TelefamIosSearchShortcut] = []
    @Published var searchResults: [TelefamIosFeedItem] = []
    @Published var isSearching: Bool = false
    @Published var downloadedShareURL: URL? = nil

    private var followingPage: Int = 1
    private var forYouPage: Int = 1
    private var pendingQueue: [TelefamIosPendingFeedOp] = []
    private var preloadedAssets: [String: AVURLAsset] = [:]
    private let pathMonitor = NWPathMonitor()
    private static var configuredURLCache = false

    let baseURL: URL
    private let tokenProvider: () -> String
    let deviceFingerprint: String
    var bearerToken: String { tokenProvider() }

    private static let cacheKeyPrefix = "telefam_ios_feeds_cache_v1_"
    private static let queueKey = "telefam_ios_feeds_pending_ops_v1"

    init(baseURL: URL, tokenProvider: @escaping () -> String, deviceFingerprint: String) {
        self.baseURL = baseURL
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint
        if !Self.configuredURLCache {
            Self.configuredURLCache = true
            URLCache.shared = URLCache(
                memoryCapacity: 32 * 1024 * 1024,
                diskCapacity: 512 * 1024 * 1024,
                directory: FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first?.appendingPathComponent("telefam_video_http_cache")
            )
        }
        restoreCachedData()
        startNetworkObserver()
    }

    deinit {
        pathMonitor.cancel()
    }

    var activeItems: [TelefamIosFeedItem] {
        activeTab == .following ? followingItems : forYouItems
    }

    var hasMoreActive: Bool {
        activeTab == .following ? hasMoreFollowing : hasMoreForYou
    }

    private func startNetworkObserver() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                guard let self else { return }
                let weak = path.status != .satisfied || path.isConstrained
                self.isWeakNetwork = weak
                if path.status == .satisfied {
                    await self.flushPendingQueue()
                }
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "telefam.ios.feed.nw"))
    }

    private func restoreCachedData() {
        let decoder = JSONDecoder()
        if let d1 = UserDefaults.standard.data(forKey: Self.cacheKeyPrefix + "following"),
           let list1 = try? decoder.decode([TelefamIosFeedItem].self, from: d1) {
            followingItems = list1
        }
        if let d2 = UserDefaults.standard.data(forKey: Self.cacheKeyPrefix + "for_you"),
           let list2 = try? decoder.decode([TelefamIosFeedItem].self, from: d2) {
            forYouItems = list2
        }
        if let dq = UserDefaults.standard.data(forKey: Self.queueKey),
           let q = try? decoder.decode([TelefamIosPendingFeedOp].self, from: dq) {
            pendingQueue = q
        }
    }

    private func persistCaches() {
        let encoder = JSONEncoder()
        if let d1 = try? encoder.encode(Array(followingItems.prefix(60))) {
            UserDefaults.standard.set(d1, forKey: Self.cacheKeyPrefix + "following")
        }
        if let d2 = try? encoder.encode(Array(forYouItems.prefix(60))) {
            UserDefaults.standard.set(d2, forKey: Self.cacheKeyPrefix + "for_you")
        }
        if let dq = try? encoder.encode(pendingQueue) {
            UserDefaults.standard.set(dq, forKey: Self.queueKey)
        }
    }

    func authorizedURLString(_ raw: String) -> String {
        guard !raw.isEmpty else { return "" }
        if raw.hasPrefix("http://") || raw.hasPrefix("https://") {
            return raw
        }
        let baseStr = baseURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let trimmed = raw.hasPrefix("/") ? String(raw.dropFirst()) : raw
        return "\(baseStr)/\(trimmed)"
    }

    func sharePostURL(for postId: String) -> URL {
        if let host = baseURL.host?.lowercased(),
           host != "127.0.0.1", host != "localhost", host != "::1" {
            return baseURL.appendingPathComponent("v1/feeds/\(postId)/download")
        }
        return URL(string: "telefam://post/\(postId)")!
    }

    func markAuthorFollowing(userId: String, isFollowing: Bool) {
        for idx in forYouItems.indices where forYouItems[idx].authorUserId == userId {
            forYouItems[idx].isFollowingAuthor = isFollowing
        }
        for idx in followingItems.indices where followingItems[idx].authorUserId == userId {
            followingItems[idx].isFollowingAuthor = isFollowing
        }
        for idx in searchResults.indices where searchResults[idx].authorUserId == userId {
            searchResults[idx].isFollowingAuthor = isFollowing
        }
        persistCaches()
    }

    func selectRenditionURL(for item: TelefamIosFeedItem, qualityPreference: String = "auto", dataSaver: Bool = false) -> URL? {
        let sorted = item.renditions.sorted { $0.bitrateKbps < $1.bitrateKbps }
        let chosen: String
        if !sorted.isEmpty {
            let pref = qualityPreference.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if dataSaver || pref == "data_saver" {
                chosen = (sorted.first { $0.bitrateKbps >= 250 } ?? sorted.first)!.url
            } else if pref == "high" && !isWeakNetwork {
                chosen = sorted.last!.url
            } else if isWeakNetwork {
                chosen = (sorted.first { $0.bitrateKbps >= 300 } ?? sorted.first)!.url
            } else {
                chosen = (sorted.last { $0.bitrateKbps <= 1600 } ?? sorted.last)!.url
            }
        } else {
            chosen = item.videoUrl
        }
        return URL(string: authorizedURLString(chosen))
    }

    func makeAuthorizedAsset(for url: URL) -> AVURLAsset {
        let tokenSuffix = String(bearerToken.suffix(12))
        let cacheKey = "\(url.absoluteString)|\(tokenSuffix)"
        if let cached = preloadedAssets[cacheKey] {
            return cached
        }
        let headers: [String: String] = [
            "Authorization": "Bearer \(bearerToken)",
            "X-Telefam-Device-FP": deviceFingerprint
        ]
        let asset = AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": headers])
        preloadedAssets[cacheKey] = asset
        if preloadedAssets.count > 12, let firstKey = preloadedAssets.keys.first {
            preloadedAssets.removeValue(forKey: firstKey)
        }
        return asset
    }

    func preloadNextFiveVideos(
        after index: Int,
        in list: [TelefamIosFeedItem],
        qualityPreference: String = "auto",
        dataSaver: Bool = false
    ) {
        let count = dataSaver ? 1 : 5
        let upcoming = list.dropFirst(index + 1).filter { $0.mode == "video" }.prefix(count)
        for item in upcoming {
            guard let url = selectRenditionURL(for: item, qualityPreference: qualityPreference, dataSaver: dataSaver) else { continue }
            let asset = makeAuthorizedAsset(for: url)
            asset.loadValuesAsynchronously(forKeys: ["playable"]) {}
        }
    }

    func refresh(tab: TelefamIosFeedTab? = nil) async {
        let target = tab ?? activeTab
        await loadPage(tab: target, page: 1, append: false)
    }

    func loadMore(tab: TelefamIosFeedTab? = nil) async {
        let target = tab ?? activeTab
        let next = (target == .following ? followingPage : forYouPage) + 1
        await loadPage(tab: target, page: next, append: true)
    }

    private func loadPage(tab: TelefamIosFeedTab, page: Int, append: Bool) async {
        if !append { isLoading = true }
        defer { isLoading = false }

        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/feeds"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [
            URLQueryItem(name: "tab", value: tab.rawValue),
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "page_size", value: "15")
        ]
        guard let url = comps?.url else { return }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        struct PageResp: Codable {
            let success: Bool
            let items: [TelefamIosFeedItem]
            let page: Int
            let hasMore: Bool
            enum CodingKeys: String, CodingKey {
                case success, items, page
                case hasMore = "has_more"
            }
        }

        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(PageResp.self, from: data), parsed.success {
            if tab == .following {
                followingPage = parsed.page
                hasMoreFollowing = parsed.hasMore
                followingItems = append ? mergeUnique(followingItems + parsed.items) : parsed.items
            } else {
                forYouPage = parsed.page
                hasMoreForYou = parsed.hasMore
                forYouItems = append ? mergeUnique(forYouItems + parsed.items) : parsed.items
            }
            persistCaches()
        }
    }

    func searchFeeds(query: String? = nil, shortcut: String? = nil) async {
        if let query { searchQuery = query }
        if let shortcut { activeShortcut = shortcut }
        isSearching = true
        defer { isSearching = false }

        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/feeds/search"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [
            URLQueryItem(name: "q", value: searchQuery),
            URLQueryItem(name: "shortcut", value: activeShortcut),
            URLQueryItem(name: "page", value: "1"),
            URLQueryItem(name: "page_size", value: "24")
        ]
        guard let url = comps?.url else { return }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        struct SearchResp: Codable {
            let success: Bool
            let shortcuts: [TelefamIosSearchShortcut]
            let items: [TelefamIosFeedItem]
        }

        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(SearchResp.self, from: data), parsed.success {
            searchShortcuts = parsed.shortcuts
            searchResults = parsed.items
        }
    }

    func toggleLike(postId: String) {
        mutateItem(postId: postId) { item in
            let next = !item.isLiked
            item.isLiked = next
            item.likesCount = next ? item.likesCount + 1 : max(0, item.likesCount - 1)
            Task { await sendInteraction(postId: postId, action: next ? "like" : "unlike") }
        }
    }

    func doubleTapLike(postId: String) {
        if let existing = (forYouItems + followingItems + searchResults).first(where: { $0.postId == postId }), existing.isLiked {
            return
        }
        toggleLike(postId: postId)
    }

    func toggleFavourite(postId: String) {
        mutateItem(postId: postId) { item in
            let next = !item.isFavourited
            item.isFavourited = next
            item.favouritesCount = next ? item.favouritesCount + 1 : max(0, item.favouritesCount - 1)
            Task { await sendInteraction(postId: postId, action: next ? "favourite" : "unfavourite") }
        }
    }

    func toggleRepost(postId: String) {
        mutateItem(postId: postId) { item in
            let next = !item.isReposted
            item.isReposted = next
            item.repostsCount = next ? item.repostsCount + 1 : max(0, item.repostsCount - 1)
            if next { showToast("Reposted") }
            Task { await sendInteraction(postId: postId, action: next ? "repost" : "unrepost") }
        }
    }

    func recordShare(postId: String) {
        mutateItem(postId: postId) { item in
            item.sharesCount += 1
        }
        Task { await sendInteraction(postId: postId, action: "share") }
    }

    func notInterested(postId: String) {
        followingItems.removeAll { $0.postId == postId }
        forYouItems.removeAll { $0.postId == postId }
        searchResults.removeAll { $0.postId == postId }
        persistCaches()
        Task { await sendInteraction(postId: postId, action: "not_interested") }
    }

    func reportPost(postId: String, reason: String) {
        showToast("Report submitted")
        Task { await sendInteraction(postId: postId, action: "report", reason: reason) }
    }

    func blockAuthor(postId: String, authorUserId: String) {
        followingItems.removeAll { $0.authorUserId == authorUserId }
        forYouItems.removeAll { $0.authorUserId == authorUserId }
        searchResults.removeAll { $0.authorUserId == authorUserId }
        persistCaches()
        Task { await sendInteraction(postId: postId, action: "block") }
    }

    func deleteOwnerPost(postId: String) async {
        followingItems.removeAll { $0.postId == postId }
        forYouItems.removeAll { $0.postId == postId }
        searchResults.removeAll { $0.postId == postId }
        persistCaches()
        var req = URLRequest(url: baseURL.appendingPathComponent("v1/posts/\(postId)"))
        req.httpMethod = "DELETE"
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        _ = try? await URLSession.shared.data(for: req)
    }

    func updatePostPrivacy(postId: String, privacy: String) async {
        let normalized = privacy.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let valid = ["public", "friends", "private"].contains(normalized) ? normalized : "public"
        mutateItem(postId: postId) { item in
            item.privacy = valid
        }
        if valid == "private" {
            followingItems.removeAll { $0.postId == postId }
            forYouItems.removeAll { $0.postId == postId }
            searchResults.removeAll { $0.postId == postId }
            persistCaches()
        }
        var req = URLRequest(url: baseURL.appendingPathComponent("v1/posts/\(postId)"))
        req.httpMethod = "PATCH"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["privacy": valid])

        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode) {
            let label = valid == "private" ? "Private (moved to Locked)" : (valid == "friends" ? "Friends" : "Public")
            showToast("Privacy updated: \(label)")
        } else {
            showToast("Saved locally — will sync when online")
        }
    }

    func recordWatchProgress(postId: String, watchMs: Int64, durationMs: Int64) {
        let act = (watchMs < 1500 && durationMs > 4000) ? "skip" : "watch"
        Task { await sendInteraction(postId: postId, action: act, watchMs: watchMs, durationMs: durationMs) }
    }

    func recordView(postId: String) {
        mutateItem(postId: postId) { item in
            item.viewsCount += 1
        }
        Task { await sendInteraction(postId: postId, action: "view") }
    }

    func downloadVideoToFiles(item: TelefamIosFeedItem) async {
        guard let url = URL(string: authorizedURLString(item.videoUrl)) else {
            showToast("Download unavailable")
            return
        }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        if let (tempURL, resp) = try? await URLSession.shared.download(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            let dest = (docs ?? FileManager.default.temporaryDirectory).appendingPathComponent("telefam_\(item.postId).mp4")
            try? FileManager.default.removeItem(at: dest)
            try? FileManager.default.moveItem(at: tempURL, to: dest)
            downloadedShareURL = dest
            showToast("Saved to Files")
        } else {
            showToast("Download unavailable")
        }
    }

    private func sendInteraction(
        postId: String,
        action: String,
        watchMs: Int64 = 0,
        durationMs: Int64 = 0,
        reason: String = ""
    ) async {
        let op = TelefamIosPendingFeedOp(
            clientOpId: "ios_feed_\(UUID().uuidString)",
            postId: postId,
            action: action,
            watchMs: watchMs,
            durationMs: durationMs,
            reason: reason
        )
        var req = URLRequest(url: baseURL.appendingPathComponent("v1/feeds/interact"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONEncoder().encode(op)

        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
            return
        }
        pendingQueue.append(op)
        persistCaches()
    }

    func flushPendingQueue() async {
        guard !pendingQueue.isEmpty else { return }
        struct BatchPayload: Codable { let operations: [TelefamIosPendingFeedOp] }
        var req = URLRequest(url: baseURL.appendingPathComponent("v1/feeds/interact/batch"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONEncoder().encode(BatchPayload(operations: pendingQueue))
        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
            pendingQueue.removeAll()
            persistCaches()
        }
    }

    func showToast(_ message: String) {
        toastMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            if self?.toastMessage == message { self?.toastMessage = nil }
        }
    }

    private func mutateItem(postId: String, mutate: (inout TelefamIosFeedItem) -> Void) {
        if let idx = forYouItems.firstIndex(where: { $0.postId == postId }) {
            mutate(&forYouItems[idx])
        }
        if let idx = followingItems.firstIndex(where: { $0.postId == postId }) {
            mutate(&followingItems[idx])
        }
        if let idx = searchResults.firstIndex(where: { $0.postId == postId }) {
            mutate(&searchResults[idx])
        }
        persistCaches()
    }

    private func mergeUnique(_ list: [TelefamIosFeedItem]) -> [TelefamIosFeedItem] {
        var seen = Set<String>()
        var out: [TelefamIosFeedItem] = []
        for item in list where seen.insert(item.postId).inserted {
            out.append(item)
        }
        return out
    }
}

func formatTelefamCompactCount(_ count: Int64) -> String {
    if count <= 0 { return "0" }
    if count >= 1_000_000 {
        let tenths = count / 100_000
        let whole = tenths / 10
        let rem = tenths % 10
        return rem == 0 ? "\(whole)M" : "\(whole).\(rem)M"
    }
    if count >= 10_000 {
        let tenths = count / 100
        let whole = tenths / 10
        let rem = tenths % 10
        return rem == 0 ? "\(whole)K" : "\(whole).\(rem)K"
    }
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: count)) ?? "\(count)"
}

struct TelefamIosTricolorSpinner: View {
    var size: CGFloat = 44
    @State private var angle: Double = 0

    var body: some View {
        ZStack {
            Circle().fill(Color(red: 0.784, green: 0.039, blue: 0.059)).frame(width: size * 0.24, height: size * 0.24).offset(x: 0, y: -size * 0.30)
            Circle().fill(Color.white).frame(width: size * 0.24, height: size * 0.24).offset(x: size * 0.26, y: size * 0.15)
            Circle().fill(Color(red: 0.086, green: 0.639, blue: 0.290)).frame(width: size * 0.24, height: size * 0.24).offset(x: -size * 0.26, y: size * 0.15)
        }
        .frame(width: size, height: size)
        .rotationEffect(.degrees(angle))
        .onAppear {
            withAnimation(.linear(duration: 0.95).repeatForever(autoreverses: false)) {
                angle = 360
            }
        }
    }
}

struct TelefamIosAudioBars: View {
    let isPlaying: Bool
    @State private var tick: Bool = false

    var body: some View {
        HStack(spacing: 2.2) {
            Capsule().fill(Color.white).frame(width: 2.2, height: isPlaying ? (tick ? 10 : 5) : 5)
            Capsule().fill(Color.white).frame(width: 2.2, height: isPlaying ? (tick ? 5 : 13) : 9)
            Capsule().fill(Color.white).frame(width: 2.2, height: isPlaying ? (tick ? 12 : 6) : 6)
        }
        .frame(height: 14)
        .onAppear {
            withAnimation(.easeInOut(duration: 0.36).repeatForever(autoreverses: true)) {
                tick.toggle()
            }
        }
    }
}

// MARK: - Aspect-Preserving AVPlayerLayer Bridge

struct TelefamIosVideoSurfaceView: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerLayerView {
        let v = PlayerLayerView()
        v.backgroundColor = .clear
        v.isOpaque = false
        v.playerLayer.backgroundColor = UIColor.clear.cgColor
        v.playerLayer.videoGravity = .resizeAspect
        v.playerLayer.player = player
        return v
    }

    func updateUIView(_ uiView: PlayerLayerView, context: Context) {
        uiView.playerLayer.videoGravity = .resizeAspect
        if uiView.playerLayer.player !== player {
            uiView.playerLayer.player = player
        }
    }

    final class PlayerLayerView: UIView {
        override static var layerClass: AnyClass { AVPlayerLayer.self }
        var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
    }
}

// MARK: - Custom SVG Vector Icons Matching Reference Screenshot

private struct LiveTvSvgIcon: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let strokeStyle = StrokeStyle(lineWidth: 2.0, lineCap: .round, lineJoin: .round)
            var ant = Path()
            ant.move(to: CGPoint(x: w * 0.35, y: h * 0.14))
            ant.addLine(to: CGPoint(x: w * 0.50, y: h * 0.30))
            ant.addLine(to: CGPoint(x: w * 0.65, y: h * 0.14))
            ctx.stroke(ant, with: .color(.white), style: strokeStyle)
            let box = CGRect(x: w * 0.10, y: h * 0.30, width: w * 0.80, height: h * 0.58)
            ctx.stroke(Path(roundedRect: box, cornerRadius: w * 0.16), with: .color(.white), style: strokeStyle)
        }
        .frame(width: 32, height: 28)
        .overlay(
            Text("LIVE")
                .font(.system(size: 8.5, weight: .heavy))
                .foregroundColor(.white)
                .offset(y: 4)
        )
    }
}

private struct FeedHeartSvgIcon: View {
    let filled: Bool
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            var p = Path()
            p.move(to: CGPoint(x: w * 0.50, y: h * 0.86))
            p.addCurve(to: CGPoint(x: w * 0.08, y: h * 0.35), control1: CGPoint(x: w * 0.14, y: h * 0.62), control2: CGPoint(x: w * 0.08, y: h * 0.44))
            p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.25), control1: CGPoint(x: w * 0.08, y: h * 0.16), control2: CGPoint(x: w * 0.34, y: h * 0.12))
            p.addCurve(to: CGPoint(x: w * 0.92, y: h * 0.35), control1: CGPoint(x: w * 0.66, y: h * 0.12), control2: CGPoint(x: w * 0.92, y: h * 0.16))
            p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.86), control1: CGPoint(x: w * 0.92, y: h * 0.44), control2: CGPoint(x: w * 0.86, y: h * 0.62))
            p.closeSubpath()
            if filled {
                ctx.fill(p, with: .color(Color(red: 1.0, green: 0.36, blue: 0.46)))
            } else {
                ctx.fill(p, with: .color(Color.black.opacity(0.26)))
                ctx.stroke(p, with: .color(.white), style: StrokeStyle(lineWidth: 2.2, lineCap: .round, lineJoin: .round))
            }
        }
        .frame(width: 30, height: 30)
    }
}

private struct FeedCommentSvgIcon: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let bubble = Path(roundedRect: CGRect(x: w * 0.10, y: h * 0.12, width: w * 0.80, height: h * 0.62), cornerRadius: w * 0.26)
            var tail = Path()
            tail.move(to: CGPoint(x: w * 0.30, y: h * 0.72))
            tail.addLine(to: CGPoint(x: w * 0.18, y: h * 0.88))
            tail.addLine(to: CGPoint(x: w * 0.46, y: h * 0.74))
            tail.closeSubpath()
            ctx.fill(bubble, with: .color(.white))
            ctx.fill(tail, with: .color(.white))
            let r = w * 0.058
            for x in [0.33, 0.50, 0.67] {
                ctx.fill(Path(ellipseIn: CGRect(x: w * x - r, y: h * 0.43 - r, width: r * 2, height: r * 2)), with: .color(Color(white: 0.14)))
            }
        }
        .frame(width: 30, height: 30)
    }
}

private struct FeedPaperPlaneSvgIcon: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            var outer = Path()
            outer.move(to: CGPoint(x: w * 0.90, y: h * 0.12))
            outer.addLine(to: CGPoint(x: w * 0.10, y: h * 0.44))
            outer.addLine(to: CGPoint(x: w * 0.44, y: h * 0.58))
            outer.addLine(to: CGPoint(x: w * 0.62, y: h * 0.90))
            outer.closeSubpath()
            ctx.stroke(outer, with: .color(TelefamPalette.crimson), style: StrokeStyle(lineWidth: 2.3, lineCap: .round, lineJoin: .round))
            var mid = Path()
            mid.move(to: CGPoint(x: w * 0.44, y: h * 0.58))
            mid.addLine(to: CGPoint(x: w * 0.90, y: h * 0.12))
            ctx.stroke(mid, with: .color(TelefamPalette.crimson), style: StrokeStyle(lineWidth: 2.0, lineCap: .round))
        }
        .frame(width: 28, height: 28)
    }
}

private struct FeedBookmarkSvgIcon: View {
    let saved: Bool
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            var p = Path()
            p.move(to: CGPoint(x: w * 0.22, y: h * 0.14))
            p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.14))
            p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.88))
            p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.66))
            p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.88))
            p.closeSubpath()
            if saved {
                ctx.fill(p, with: .color(TelefamPalette.crimson))
            }
            ctx.stroke(p, with: .color(TelefamPalette.crimson), style: StrokeStyle(lineWidth: 2.3, lineCap: .round, lineJoin: .round))
        }
        .frame(width: 28, height: 28)
    }
}

// MARK: - Reusable Fullscreen Feed View (Feeds Tab & Search Viewer)

struct TelefamIosFeedView: View {
    @ObservedObject var store: TelefamIosFeedStore
    @ObservedObject var contactsStore: TelefamContactsStore
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let items: [TelefamIosFeedItem]
    let initialIndex: Int
    let isStandaloneViewer: Bool
    let bottomChromePadding: CGFloat
    var onLiveClick: () -> Void
    var onSearchClick: () -> Void
    var onBackClick: () -> Void
    var onCommentClick: () -> Void
    var onEditOwnerPost: (TelefamIosFeedItem) -> Void
    var onTapHashtag: (String, TelefamIosPostMode) -> Void
    var onTapAudio: (TelefamIosSongTrack, TelefamIosPostMode) -> Void
    var onOpenAuthorProfile: (String) -> Void = { _ in }

    @State private var currentIndex: Int = 0
    @State private var sharedPlayer = AVPlayer()
    @State private var songPlayer = AVPlayer()
    @State private var videoEndObserver: NSObjectProtocol? = nil
    @State private var videoFailObserver: NSObjectProtocol? = nil
    @State private var songEndObserver: NSObjectProtocol? = nil
    @State private var isPaused: Bool = false
    @State private var isFastForward2x: Bool = false
    @State private var isBuffering: Bool = false
    @State private var isPlaybackFailed: Bool = false
    @State private var showDoubleTapHeart: Bool = false
    @State private var expandedPostIds: Set<String> = []
    @State private var activeMoreItem: TelefamIosFeedItem? = nil
    @State private var activeShareItem: TelefamIosFeedItem? = nil
    @State private var reportPostId: String? = nil
    @State private var privacyPostItem: TelefamIosFeedItem? = nil

    private var visibleItems: [TelefamIosFeedItem] {
        let mutedIds = Set(settingsStore.mutedAccounts.map(\.userId))
        let blockedIds = Set(settingsStore.blockedAccounts.map(\.userId))
        let hiddenTopics = Set(
            settingsStore.bundle.contentPreferences.notInterestedTopics
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
                .filter { !$0.isEmpty }
        )
        return items.filter { item in
            if !item.isOwner && (mutedIds.contains(item.authorUserId) || blockedIds.contains(item.authorUserId)) {
                return false
            }
            if !isStandaloneViewer && !hiddenTopics.isEmpty {
                let captionLower = item.caption.lowercased()
                let textLower = item.textContent.lowercased()
                let tagsLower = item.hashtags.map { $0.lowercased() }
                for topic in hiddenTopics {
                    if captionLower.contains(topic) || textLower.contains(topic) || tagsLower.contains(where: { $0.contains(topic) }) {
                        return false
                    }
                }
            }
            return true
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color(red: 0.027, green: 0.031, blue: 0.043).ignoresSafeArea()

            if visibleItems.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    if store.isLoading || store.isWeakNetwork {
                        TelefamIosTricolorSpinner(size: 46)
                    } else {
                        Text("No videos yet")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                        Text("Follow people or publish a video to start streaming.")
                            .font(.system(size: 13))
                            .foregroundColor(Color.white.opacity(0.75))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Button("Refresh") {
                            Task { await store.refresh() }
                        }
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .frame(height: 40)
                        .background(TelefamPalette.crimson)
                        .clipShape(Capsule())
                    }
                    Spacer()
                }
                .padding(.bottom, bottomChromePadding)
            } else {
                TabView(selection: $currentIndex) {
                    ForEach(Array(visibleItems.enumerated()), id: \.element.postId) { idx, item in
                        feedPage(item: item, isActive: currentIndex == idx)
                            .rotationEffect(.degrees(-90))
                            .tag(idx)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .rotationEffect(.degrees(90))
                .ignoresSafeArea()
            }

            topOverlayBar
        }
        .onAppear {
            currentIndex = min(max(0, initialIndex), max(0, visibleItems.count - 1))
            activatePage(at: currentIndex)
        }
        .onChange(of: currentIndex) { idx in
            activatePage(at: idx)
        }
        .onChange(of: visibleItems.map(\.postId)) { _ in
            let clamped = min(max(0, currentIndex), max(0, visibleItems.count - 1))
            currentIndex = clamped
            activatePage(at: clamped)
        }
        .onChange(of: settingsStore.bundle.contentPreferences) { _ in
            activatePage(at: currentIndex)
        }
        .onDisappear {
            sharedPlayer.pause()
            songPlayer.pause()
            clearPlaybackObservers()
        }
        .confirmationDialog("Share", isPresented: Binding(get: { activeShareItem != nil }, set: { if !$0 { activeShareItem = nil } }), presenting: activeShareItem) { item in
            Button("Share via apps") {
                store.recordShare(postId: item.postId)
                let link = store.sharePostURL(for: item.postId)
                let av = UIActivityViewController(activityItems: [link], applicationActivities: nil)
                UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }.first?.present(av, animated: true)
            }
            Button("Copy link") {
                UIPasteboard.general.string = store.sharePostURL(for: item.postId).absoluteString
                store.recordShare(postId: item.postId)
                store.showToast("Link copied")
            }
            Button(item.isReposted ? "Remove repost" : "Repost") {
                store.toggleRepost(postId: item.postId)
            }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Options", isPresented: Binding(get: { activeMoreItem != nil }, set: { if !$0 { activeMoreItem = nil } }), presenting: activeMoreItem) { item in
            Button(item.isReposted ? "Remove repost" : "Repost") { store.toggleRepost(postId: item.postId) }
            if item.mode == "video" {
                Button("Download") { Task { await store.downloadVideoToFiles(item: item) } }
            }
            if !item.isOwner {
                Button("Not interested") { store.notInterested(postId: item.postId) }
                Button("Mute @\(item.authorUsername)") {
                    Task {
                        await settingsStore.muteAccount(
                            userId: item.authorUserId,
                            fullName: item.authorFullName,
                            username: item.authorUsername,
                            avatarUrl: item.authorAvatarUrl
                        )
                        store.showToast("Muted @\(item.authorUsername)")
                    }
                }
                Button("Report") { reportPostId = item.postId }
            }
            if item.isOwner {
                Button("Edit video") { onEditOwnerPost(item) }
                let privLabel = item.privacy.lowercased() == "private" ? "Private" : (item.privacy.lowercased() == "friends" ? "Friends" : "Public")
                Button("Privacy: \(privLabel)") { privacyPostItem = item }
                Button("Delete video", role: .destructive) { Task { await store.deleteOwnerPost(postId: item.postId) } }
            } else {
                Button("Block @\(item.authorUsername)", role: .destructive) {
                    store.blockAuthor(postId: item.postId, authorUserId: item.authorUserId)
                    Task { await settingsStore.refreshBlockedAccounts() }
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Post privacy", isPresented: Binding(get: { privacyPostItem != nil }, set: { if !$0 { privacyPostItem = nil } }), presenting: privacyPostItem) { item in
            Button("Public" + (item.privacy.lowercased() == "public" || item.privacy.isEmpty ? " (Current)" : "")) {
                Task { await store.updatePostPrivacy(postId: item.postId, privacy: "public") }
            }
            Button("Friends" + (item.privacy.lowercased() == "friends" ? " (Current)" : "")) {
                Task { await store.updatePostPrivacy(postId: item.postId, privacy: "friends") }
            }
            Button("Private — Move to Locked" + (item.privacy.lowercased() == "private" ? " (Current)" : "")) {
                Task { await store.updatePostPrivacy(postId: item.postId, privacy: "private") }
            }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Report", isPresented: Binding(get: { reportPostId != nil }, set: { if !$0 { reportPostId = nil } })) {
            ForEach(["Spam or misleading", "Harassment or unsafe content", "Duplicate or unauthorized re-upload", "Other"], id: \.self) { reason in
                Button(reason) {
                    if let pid = reportPostId {
                        store.reportPost(postId: pid, reason: reason)
                    }
                    reportPostId = nil
                }
            }
            Button("Cancel", role: .cancel) { reportPostId = nil }
        }
        .sheet(isPresented: Binding(get: { store.downloadedShareURL != nil }, set: { if !$0 { store.downloadedShareURL = nil } })) {
            if let fileURL = store.downloadedShareURL {
                TelefamIosShareActivitySheet(items: [fileURL])
            }
        }
    }

    @ViewBuilder
    private func feedPage(item: TelefamIosFeedItem, isActive: Bool) -> some View {
        let hasSongAudio = !(item.song?.previewUrl ?? "").isEmpty
        ZStack(alignment: .bottom) {
            ZStack {
                Color(red: 0.027, green: 0.031, blue: 0.043)

                if item.mode == "video" {
                    ZStack {
                        if !item.thumbnailUrl.isEmpty {
                            TelefamIosRemoteImage(
                                urlString: store.authorizedURLString(item.thumbnailUrl),
                                bearerToken: store.bearerToken,
                                deviceFingerprint: store.deviceFingerprint,
                                fallbackInitials: ""
                            )
                            .scaledToFill()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .clipped()
                            .blur(radius: 28)
                            .overlay(Color.black.opacity(0.44))

                            TelefamIosRemoteImage(
                                urlString: store.authorizedURLString(item.thumbnailUrl),
                                bearerToken: store.bearerToken,
                                deviceFingerprint: store.deviceFingerprint,
                                fallbackInitials: "",
                                contentMode: .fit
                            )
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                        if isActive && !isPlaybackFailed {
                            TelefamIosVideoSurfaceView(player: sharedPlayer)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if item.mode == "photos" && !item.photoUrls.isEmpty {
                    ZStack {
                        TelefamIosRemoteImage(
                            urlString: store.authorizedURLString(item.photoUrls.first ?? ""),
                            bearerToken: store.bearerToken,
                            deviceFingerprint: store.deviceFingerprint,
                            fallbackInitials: ""
                        )
                        .scaledToFill()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipped()
                        .blur(radius: 28)
                        .overlay(Color.black.opacity(0.44))

                        TabView {
                            ForEach(item.photoUrls, id: \.self) { rawUrl in
                                TelefamIosRemoteImage(
                                    urlString: store.authorizedURLString(rawUrl),
                                    bearerToken: store.bearerToken,
                                    deviceFingerprint: store.deviceFingerprint,
                                    fallbackInitials: "",
                                    contentMode: .fit
                                )
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                            }
                        }
                        .tabViewStyle(.page(indexDisplayMode: item.photoUrls.count > 1 ? .automatic : .never))
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    let theme = TelefamIosPostTheme.all.first { $0.id == item.themeId } ?? TelefamIosPostTheme.all[0]
                    LinearGradient(
                        colors: [
                            Color(red: Double((theme.startHex >> 16) & 0xff) / 255.0, green: Double((theme.startHex >> 8) & 0xff) / 255.0, blue: Double(theme.startHex & 0xff) / 255.0),
                            Color(red: Double((theme.endHex >> 16) & 0xff) / 255.0, green: Double((theme.endHex >> 8) & 0xff) / 255.0, blue: Double(theme.endHex & 0xff) / 255.0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .ignoresSafeArea()
                }

                // Bottom gradient scrim for contrast
                VStack {
                    Spacer()
                    LinearGradient(colors: [.clear, Color.black.opacity(0.72)], startPoint: .top, endPoint: .bottom)
                        .frame(height: 240)
                }

                // Single tap / double tap / long-press 2x gesture surface
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture(count: 2) {
                        showDoubleTapHeart = true
                        store.doubleTapLike(postId: item.postId)
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) { showDoubleTapHeart = false }
                    }
                    .onTapGesture(count: 1) {
                        if item.mode == "video" || hasSongAudio {
                            isPaused.toggle()
                            if isPaused {
                                sharedPlayer.pause()
                                songPlayer.pause()
                            } else {
                                if item.mode == "video" {
                                    sharedPlayer.play()
                                    sharedPlayer.rate = 1.0
                                }
                                if hasSongAudio {
                                    songPlayer.play()
                                }
                            }
                        }
                    }
                    .onLongPressGesture(minimumDuration: 0.35, maximumDistance: 40, pressing: { pressing in
                        guard item.mode == "video" else { return }
                        isFastForward2x = pressing
                        sharedPlayer.rate = pressing ? 2.0 : (isPaused ? 0.0 : 1.0)
                    }, perform: {})

                if item.mode != "video" && !(item.mode == "photos" && !item.photoUrls.isEmpty) {
                    let theme = TelefamIosPostTheme.all.first { $0.id == item.themeId } ?? TelefamIosPostTheme.all[0]
                    let font = TelefamIosPostFont(rawValue: item.fontId) ?? .classic
                    SmartInteractiveTextView(
                        text: item.textContent,
                        source: "feed",
                        font: font.swiftFont,
                        textColor: theme.isLightInk ? .white : Color(red: 0.11, green: 0.12, blue: 0.15),
                        linkColor: theme.isLightInk ? Color(red: 1.0, green: 0.85, blue: 0.88) : TelefamPalette.crimson,
                        textAlignment: .center,
                        showOtpPills: true
                    )
                    .padding(.horizontal, 40)
                }

                if isActive && isPaused && (item.mode == "video" || hasSongAudio) {
                    Image(systemName: "play.fill")
                        .font(.system(size: 30))
                        .foregroundColor(.white)
                        .frame(width: 64, height: 64)
                        .background(Color.black.opacity(0.45))
                        .clipShape(Circle())
                }

                if isActive && isFastForward2x && item.mode == "video" {
                    VStack {
                        Text(">> 2x")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.68))
                            .clipShape(Capsule())
                            .padding(.top, 96)
                        Spacer()
                    }
                }

                if isActive && item.mode == "video" && !isPlaybackFailed && (isBuffering || (store.isWeakNetwork && !isPaused)) {
                    TelefamIosTricolorSpinner(size: 44)
                }

                if showDoubleTapHeart {
                    if settingsStore.bundle.contentPreferences.reducedMotion {
                        FeedHeartSvgIcon(filled: true)
                            .scaleEffect(1.85)
                    } else {
                        FeedHeartSvgIcon(filled: true)
                            .scaleEffect(2.4)
                            .transition(.scale.combined(with: .opacity))
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                let ccText = item.caption.isEmpty ? item.textContent : item.caption
                if isActive && settingsStore.bundle.contentPreferences.captionsEnabled && !ccText.isEmpty {
                    HStack(spacing: 6) {
                        Text("CC")
                            .font(.system(size: 10.5, weight: .heavy))
                            .foregroundColor(.black)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                        Text(ccText)
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundColor(.white)
                            .lineLimit(2)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.black.opacity(0.68))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                HStack(alignment: .bottom) {
                    bottomLeftMetadata(item: item, isPlayingAudio: isActive && !isPaused && !settingsStore.bundle.contentPreferences.reducedMotion)
                    Spacer(minLength: 12)
                    rightActionRail(item: item)
                }
            }
            .padding(.horizontal, 14)
            .padding(.bottom, 16 + bottomChromePadding)
        }
    }

    private var topOverlayBar: some View {
        ZStack {
            HStack {
                if isStandaloneViewer {
                    Button(action: onBackClick) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }
                } else {
                    Button(action: onLiveClick) {
                        LiveTvSvgIcon()
                            .frame(width: 44, height: 44)
                    }
                }
                Spacer()
                Button(action: onSearchClick) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
            }

            if !isStandaloneViewer {
                HStack(spacing: 24) {
                    ForEach([TelefamIosFeedTab.following, TelefamIosFeedTab.forYou], id: \.self) { tab in
                        let selected = store.activeTab == tab
                        Button {
                            store.activeTab = tab
                            Task { await store.refresh(tab: tab) }
                        } label: {
                            VStack(spacing: 5) {
                                Text(tab.label)
                                    .font(.system(size: 16.5, weight: selected ? .bold : .medium))
                                    .foregroundColor(selected ? .white : Color.white.opacity(0.68))
                                Capsule()
                                    .fill(selected ? Color.white : Color.clear)
                                    .frame(width: 30, height: 3)
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 8)
        .background(LinearGradient(colors: [Color.black.opacity(0.55), .clear], startPoint: .top, endPoint: .bottom))
    }

    private func rightActionRail(item: TelefamIosFeedItem) -> some View {
        VStack(spacing: 15) {
            ZStack(alignment: .bottom) {
                Button {
                    if !item.authorUserId.isEmpty {
                        onOpenAuthorProfile(item.authorUserId)
                    }
                } label: {
                    if !item.authorAvatarUrl.isEmpty {
                        TelefamIosRemoteImage(
                            urlString: store.authorizedURLString(item.authorAvatarUrl),
                            bearerToken: store.bearerToken,
                            deviceFingerprint: store.deviceFingerprint,
                            fallbackInitials: String(item.authorFullName.prefix(1)).uppercased()
                        )
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                    } else {
                        Circle()
                            .fill(Color(white: 0.22))
                            .frame(width: 48, height: 48)
                            .overlay(Text(String(item.authorFullName.prefix(1)).uppercased()).foregroundColor(.white).font(.system(size: 18, weight: .bold)))
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                    }
                }
                .buttonStyle(.plain)

                if !item.isFollowingAuthor && !item.isOwner {
                    Button {
                        contactsStore.toggleFollow(for: item.authorUserId)
                        store.markAuthorFollowing(userId: item.authorUserId, isFollowing: true)
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(.white)
                            .frame(width: 22, height: 22)
                            .background(Color(red: 1.0, green: 0.30, blue: 0.42))
                            .clipShape(Circle())
                    }
                    .offset(y: 9)
                }
            }
            .padding(.bottom, 8)

            actionItem(count: formatTelefamCompactCount(item.likesCount)) {
                store.toggleLike(postId: item.postId)
            } icon: {
                FeedHeartSvgIcon(filled: item.isLiked)
            }

            actionItem(count: formatTelefamCompactCount(item.commentsCount), action: onCommentClick) {
                FeedCommentSvgIcon()
            }

            actionItem(count: formatTelefamCompactCount(item.sharesCount)) {
                activeShareItem = item
            } icon: {
                FeedPaperPlaneSvgIcon()
            }

            actionItem(count: formatTelefamCompactCount(item.favouritesCount)) {
                store.toggleFavourite(postId: item.postId)
            } icon: {
                FeedBookmarkSvgIcon(saved: item.isFavourited)
            }

            Button { activeMoreItem = item } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 32)
            }
        }
    }

    private func actionItem<Icon: View>(count: String, action: @escaping () -> Void, @ViewBuilder icon: () -> Icon) -> some View {
        Button(action: action) {
            VStack(spacing: 3) {
                icon()
                Text(count)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(minWidth: 46, minHeight: 46)
        }
    }

    private func bottomLeftMetadata(item: TelefamIosFeedItem, isPlayingAudio: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !item.repostedByUsername.isEmpty {
                HStack(spacing: 6) {
                    Image(systemName: "repeat").font(.system(size: 11, weight: .bold)).foregroundColor(TelefamPalette.crimson)
                    Text("Reposted • @\(item.repostedByUsername)")
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Color(white: 0.10, opacity: 0.72))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                HStack(spacing: 6) {
                    Circle()
                        .fill(TelefamPalette.crimson)
                        .frame(width: 18, height: 18)
                        .overlay(Image(systemName: "sparkle").font(.system(size: 9, weight: .bold)).foregroundColor(.white))
                    Text(item.recommendationLabel.isEmpty ? "You might like this" : item.recommendationLabel)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Color(white: 0.10, opacity: 0.72))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.14), lineWidth: 1))
            }

            Button {
                if !item.authorUserId.isEmpty {
                    onOpenAuthorProfile(item.authorUserId)
                }
            } label: {
                HStack(spacing: 5) {
                    Text(item.authorUsername.isEmpty ? item.authorFullName : item.authorUsername)
                        .font(.system(size: 16.5, weight: .bold))
                        .foregroundColor(.white)
                    TelefamIosVerifiedBadgeView(tier: item.authorVerifiedTier, size: 16)
                }
            }
            .buttonStyle(.plain)

            let captionText = item.caption.isEmpty ? (item.mode != "text" ? item.textContent : "") : item.caption
            if !captionText.isEmpty {
                let isExpanded = expandedPostIds.contains(item.postId)
                let showToggle = isExpanded || captionText.count > 75 || captionText.contains("\n") || !SmartTextEngine.analyze(captionText).urls.isEmpty
                VStack(alignment: .leading, spacing: 3) {
                    if isExpanded {
                        ScrollView(.vertical, showsIndicators: false) {
                            VStack(alignment: .leading, spacing: 6) {
                                SmartInteractiveTextView(
                                    text: captionText,
                                    source: "feed",
                                    font: .system(size: 14),
                                    textColor: Color.white.opacity(0.94),
                                    linkColor: Color(red: 1.0, green: 0.80, blue: 0.84),
                                    otpHighlightColor: Color.white.opacity(0.22)
                                )
                                SmartLinkPreviewCardStack(
                                    text: captionText,
                                    source: "feed",
                                    isDarkBubble: true
                                )
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxHeight: 220)
                    } else {
                        SmartInteractiveTextView(
                            text: captionText,
                            source: "feed",
                            font: .system(size: 14),
                            textColor: Color.white.opacity(0.94),
                            linkColor: Color(red: 1.0, green: 0.80, blue: 0.84),
                            otpHighlightColor: Color.white.opacity(0.22),
                            lineLimit: 2,
                            showOtpPills: false
                        )
                    }
                    if showToggle {
                        Button {
                            if isExpanded {
                                expandedPostIds.remove(item.postId)
                            } else {
                                expandedPostIds.insert(item.postId)
                            }
                        } label: {
                            Text(isExpanded ? "Read less" : "Read more")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if !item.hashtags.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(item.hashtags, id: \.self) { tag in
                            Button {
                                let pMode = TelefamIosPostMode(rawValue: item.mode) ?? .video
                                onTapHashtag(tag, pMode)
                            } label: {
                                Text("#\(tag)")
                                    .font(.system(size: 13.5, weight: .bold))
                                    .foregroundColor(Color(red: 1.0, green: 0.30, blue: 0.42))
                            }
                        }
                    }
                }
            }

            Button {
                let track = item.song ?? TelefamIosSongTrack(
                    trackId: "orig_\(item.postId)",
                    title: "Original sound",
                    artist: item.authorUsername,
                    album: "",
                    artworkUrl: item.thumbnailUrl,
                    previewUrl: "",
                    durationMs: item.durationMs,
                    source: "original"
                )
                let pMode = TelefamIosPostMode(rawValue: item.mode) ?? .video
                onTapAudio(track, pMode)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "music.note")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                    Text(item.song != nil ? "\(item.song!.title) • \(item.song!.artist)" : "Original sound")
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundColor(.white)
                        .lineLimit(1)
                    TelefamIosAudioBars(isPlaying: isPlayingAudio)
                }
            }
        }
    }

    private func clearPlaybackObservers() {
        if let obs = videoEndObserver { NotificationCenter.default.removeObserver(obs); videoEndObserver = nil }
        if let obs = videoFailObserver { NotificationCenter.default.removeObserver(obs); videoFailObserver = nil }
        if let obs = songEndObserver { NotificationCenter.default.removeObserver(obs); songEndObserver = nil }
    }

    private func activatePage(at index: Int) {
        clearPlaybackObservers()
        let list = visibleItems
        guard list.indices.contains(index) else {
            sharedPlayer.pause()
            sharedPlayer.replaceCurrentItem(with: nil)
            songPlayer.pause()
            songPlayer.replaceCurrentItem(with: nil)
            return
        }
        let active = list[index]
        let prefs = settingsStore.bundle.contentPreferences
        let shouldAutoplay = prefs.autoplayVideos
        isPaused = !shouldAutoplay
        isFastForward2x = false
        isPlaybackFailed = false
        store.preloadNextFiveVideos(
            after: index,
            in: list,
            qualityPreference: prefs.videoQuality,
            dataSaver: prefs.dataSaver
        )

        if index >= list.count - 3 && store.hasMoreActive && !isStandaloneViewer {
            Task { await store.loadMore() }
        }

        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)

        let songUrlStr = store.authorizedURLString(active.song?.previewUrl ?? "")
        let hasSong = !songUrlStr.isEmpty

        if active.mode == "video",
           let url = store.selectRenditionURL(for: active, qualityPreference: prefs.videoQuality, dataSaver: prefs.dataSaver)
               ?? URL(string: store.authorizedURLString(active.videoUrl)) {
            let asset = store.makeAuthorizedAsset(for: url)
            let playerItem = AVPlayerItem(asset: asset)
            playerItem.preferredForwardBufferDuration = prefs.dataSaver ? 4 : 12
            sharedPlayer.isMuted = active.muteOriginalAudio || hasSong
            sharedPlayer.automaticallyWaitsToMinimizeStalling = true
            sharedPlayer.replaceCurrentItem(with: playerItem)
            videoEndObserver = NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: playerItem, queue: .main) { _ in
                sharedPlayer.seek(to: .zero)
                if !isPaused { sharedPlayer.play() }
            }
            videoFailObserver = NotificationCenter.default.addObserver(forName: .AVPlayerItemFailedToPlayToEndTime, object: playerItem, queue: .main) { _ in
                if let fallbackURL = URL(string: store.authorizedURLString(active.videoUrl)), fallbackURL != url {
                    let fallbackItem = AVPlayerItem(asset: store.makeAuthorizedAsset(for: fallbackURL))
                    sharedPlayer.replaceCurrentItem(with: fallbackItem)
                    if !isPaused { sharedPlayer.play() }
                } else {
                    isBuffering = false
                    isPlaybackFailed = true
                }
            }
            if shouldAutoplay {
                sharedPlayer.play()
                sharedPlayer.rate = 1.0
            } else {
                sharedPlayer.pause()
            }
            isBuffering = false
        } else {
            sharedPlayer.pause()
            sharedPlayer.replaceCurrentItem(with: nil)
            isBuffering = false
        }

        if hasSong, let songURL = URL(string: songUrlStr) {
            let sItem = AVPlayerItem(url: songURL)
            songPlayer.replaceCurrentItem(with: sItem)
            songEndObserver = NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: sItem, queue: .main) { _ in
                songPlayer.seek(to: .zero)
                if !isPaused { songPlayer.play() }
            }
            if shouldAutoplay {
                songPlayer.play()
            } else {
                songPlayer.pause()
            }
        } else {
            songPlayer.pause()
            songPlayer.replaceCurrentItem(with: nil)
        }
    }
}

private struct TelefamIosShareActivitySheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
