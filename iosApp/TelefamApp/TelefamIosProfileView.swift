import Combine
import PhotosUI
import SwiftUI
import UIKit

enum TelefamIosOwnerProfileTab: String, CaseIterable, Identifiable {
    case posts
    case saved
    case reposts
    case locked

    var id: String { rawValue }

    var label: String {
        switch self {
        case .posts: return "Posts"
        case .saved: return "Saved"
        case .reposts: return "Reposts"
        case .locked: return "Locked"
        }
    }
}

enum TelefamIosProfilePostFilter: String, CaseIterable, Identifiable {
    case all
    case videos
    case photos
    case text

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all: return "All posts"
        case .videos: return "Videos"
        case .photos: return "Photos"
        case .text: return "Text"
        }
    }
}

enum TelefamIosProfilePostSort: String, CaseIterable, Identifiable {
    case latest
    case mostViewed
    case mostLiked

    var id: String { rawValue }

    var label: String {
        switch self {
        case .latest: return "Latest"
        case .mostViewed: return "Most viewed"
        case .mostLiked: return "Most liked"
        }
    }
}

enum TelefamIosProfileReportOptions {
    static let categories: [String] = [
        "Spam or misleading",
        "Harassment or bullying",
        "Impersonation",
        "Hate or abusive content",
        "Scam or fraud",
        "Inappropriate content",
        "Other"
    ]
}

struct TelefamIosUserProfile: Codable, Equatable {
    var userId: String
    var fullName: String
    var username: String
    var avatarUrl: String
    var bio: String
    var location: String
    var countryIso: String
    var gender: String
    var dateOfBirth: String
    var phoneNumber: String
    var usernameChangedAtMs: Int64
    var nextUsernameChangeMs: Int64
    var backgroundUrls: [String]
    var backgrounds: [TelefamIosProfileBackgroundItem]
    var followingCount: Int64
    var followersCount: Int64
    var likesCount: Int64
    var isFollowing: Bool
    var followsYou: Bool
    var isOwner: Bool
    var isBlocked: Bool
    var isMuted: Bool = false
    var isPrivateRestricted: Bool = false
    var whoCanComment: String = "everyone"
    var whoCanMention: String = "everyone"
    var likesVisibility: String = "everyone"
    var followingVisibility: String = "everyone"
    var showActivityStatus: Bool = true
    var riskScore: Double
    var posts: [TelefamIosFeedItem]
    var savedPosts: [TelefamIosFeedItem]
    var repostedPosts: [TelefamIosFeedItem]
    var lockedPosts: [TelefamIosFeedItem]

    enum CodingKeys: String, CodingKey {
        case userId, fullName, username, avatarUrl, bio, location, countryIso, gender, dateOfBirth, phoneNumber
        case usernameChangedAtMs, nextUsernameChangeMs, backgroundUrls, backgrounds
        case followingCount, followersCount, likesCount, isFollowing, followsYou, isOwner, isBlocked
        case isMuted, isPrivateRestricted, whoCanComment, whoCanMention, likesVisibility, followingVisibility, showActivityStatus
        case riskScore, posts, savedPosts, repostedPosts, lockedPosts
    }

    init(
        userId: String,
        fullName: String,
        username: String,
        avatarUrl: String,
        bio: String,
        location: String,
        countryIso: String,
        gender: String,
        dateOfBirth: String,
        phoneNumber: String,
        usernameChangedAtMs: Int64,
        nextUsernameChangeMs: Int64,
        backgroundUrls: [String],
        backgrounds: [TelefamIosProfileBackgroundItem],
        followingCount: Int64,
        followersCount: Int64,
        likesCount: Int64,
        isFollowing: Bool,
        followsYou: Bool,
        isOwner: Bool,
        isBlocked: Bool,
        isMuted: Bool = false,
        isPrivateRestricted: Bool = false,
        whoCanComment: String = "everyone",
        whoCanMention: String = "everyone",
        likesVisibility: String = "everyone",
        followingVisibility: String = "everyone",
        showActivityStatus: Bool = true,
        riskScore: Double,
        posts: [TelefamIosFeedItem],
        savedPosts: [TelefamIosFeedItem],
        repostedPosts: [TelefamIosFeedItem],
        lockedPosts: [TelefamIosFeedItem]
    ) {
        self.userId = userId
        self.fullName = fullName
        self.username = username
        self.avatarUrl = avatarUrl
        self.bio = bio
        self.location = location
        self.countryIso = countryIso
        self.gender = gender
        self.dateOfBirth = dateOfBirth
        self.phoneNumber = phoneNumber
        self.usernameChangedAtMs = usernameChangedAtMs
        self.nextUsernameChangeMs = nextUsernameChangeMs
        self.backgroundUrls = backgroundUrls
        self.backgrounds = backgrounds
        self.followingCount = followingCount
        self.followersCount = followersCount
        self.likesCount = likesCount
        self.isFollowing = isFollowing
        self.followsYou = followsYou
        self.isOwner = isOwner
        self.isBlocked = isBlocked
        self.isMuted = isMuted
        self.isPrivateRestricted = isPrivateRestricted
        self.whoCanComment = whoCanComment
        self.whoCanMention = whoCanMention
        self.likesVisibility = likesVisibility
        self.followingVisibility = followingVisibility
        self.showActivityStatus = showActivityStatus
        self.riskScore = riskScore
        self.posts = posts
        self.savedPosts = savedPosts
        self.repostedPosts = repostedPosts
        self.lockedPosts = lockedPosts
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        userId = try c.decodeIfPresent(String.self, forKey: .userId) ?? ""
        fullName = try c.decodeIfPresent(String.self, forKey: .fullName) ?? ""
        username = try c.decodeIfPresent(String.self, forKey: .username) ?? ""
        avatarUrl = try c.decodeIfPresent(String.self, forKey: .avatarUrl) ?? ""
        bio = try c.decodeIfPresent(String.self, forKey: .bio) ?? ""
        location = try c.decodeIfPresent(String.self, forKey: .location) ?? ""
        countryIso = try c.decodeIfPresent(String.self, forKey: .countryIso) ?? ""
        gender = try c.decodeIfPresent(String.self, forKey: .gender) ?? ""
        dateOfBirth = try c.decodeIfPresent(String.self, forKey: .dateOfBirth) ?? ""
        phoneNumber = try c.decodeIfPresent(String.self, forKey: .phoneNumber) ?? ""
        usernameChangedAtMs = try c.decodeIfPresent(Int64.self, forKey: .usernameChangedAtMs) ?? 0
        nextUsernameChangeMs = try c.decodeIfPresent(Int64.self, forKey: .nextUsernameChangeMs) ?? 0
        backgroundUrls = try c.decodeIfPresent([String].self, forKey: .backgroundUrls) ?? []
        backgrounds = try c.decodeIfPresent([TelefamIosProfileBackgroundItem].self, forKey: .backgrounds) ?? []
        followingCount = try c.decodeIfPresent(Int64.self, forKey: .followingCount) ?? 0
        followersCount = try c.decodeIfPresent(Int64.self, forKey: .followersCount) ?? 0
        likesCount = try c.decodeIfPresent(Int64.self, forKey: .likesCount) ?? 0
        isFollowing = try c.decodeIfPresent(Bool.self, forKey: .isFollowing) ?? false
        followsYou = try c.decodeIfPresent(Bool.self, forKey: .followsYou) ?? false
        isOwner = try c.decodeIfPresent(Bool.self, forKey: .isOwner) ?? false
        isBlocked = try c.decodeIfPresent(Bool.self, forKey: .isBlocked) ?? false
        isMuted = try c.decodeIfPresent(Bool.self, forKey: .isMuted) ?? false
        isPrivateRestricted = try c.decodeIfPresent(Bool.self, forKey: .isPrivateRestricted) ?? false
        whoCanComment = try c.decodeIfPresent(String.self, forKey: .whoCanComment) ?? "everyone"
        whoCanMention = try c.decodeIfPresent(String.self, forKey: .whoCanMention) ?? "everyone"
        likesVisibility = try c.decodeIfPresent(String.self, forKey: .likesVisibility) ?? "everyone"
        followingVisibility = try c.decodeIfPresent(String.self, forKey: .followingVisibility) ?? "everyone"
        showActivityStatus = try c.decodeIfPresent(Bool.self, forKey: .showActivityStatus) ?? true
        riskScore = try c.decodeIfPresent(Double.self, forKey: .riskScore) ?? 0
        posts = try c.decodeIfPresent([TelefamIosFeedItem].self, forKey: .posts) ?? []
        savedPosts = try c.decodeIfPresent([TelefamIosFeedItem].self, forKey: .savedPosts) ?? []
        repostedPosts = try c.decodeIfPresent([TelefamIosFeedItem].self, forKey: .repostedPosts) ?? []
        lockedPosts = try c.decodeIfPresent([TelefamIosFeedItem].self, forKey: .lockedPosts) ?? []
    }

    var relationship: TelefamRelationshipState {
        TelefamRelationshipState.compute(isFollowing: isFollowing, followsYou: followsYou)
    }

    static func == (lhs: TelefamIosUserProfile, rhs: TelefamIosUserProfile) -> Bool {
        lhs.userId == rhs.userId &&
            lhs.fullName == rhs.fullName &&
            lhs.username == rhs.username &&
            lhs.avatarUrl == rhs.avatarUrl &&
            lhs.bio == rhs.bio &&
            lhs.location == rhs.location &&
            lhs.gender == rhs.gender &&
            lhs.dateOfBirth == rhs.dateOfBirth &&
            lhs.phoneNumber == rhs.phoneNumber &&
            lhs.nextUsernameChangeMs == rhs.nextUsernameChangeMs &&
            lhs.backgrounds == rhs.backgrounds &&
            lhs.followingCount == rhs.followingCount &&
            lhs.followersCount == rhs.followersCount &&
            lhs.likesCount == rhs.likesCount &&
            lhs.isFollowing == rhs.isFollowing &&
            lhs.followsYou == rhs.followsYou &&
            lhs.isOwner == rhs.isOwner &&
            lhs.isBlocked == rhs.isBlocked &&
            lhs.isMuted == rhs.isMuted &&
            lhs.isPrivateRestricted == rhs.isPrivateRestricted &&
            lhs.likesVisibility == rhs.likesVisibility &&
            lhs.followingVisibility == rhs.followingVisibility &&
            lhs.posts.count == rhs.posts.count &&
            lhs.savedPosts.count == rhs.savedPosts.count &&
            lhs.repostedPosts.count == rhs.repostedPosts.count &&
            lhs.lockedPosts.count == rhs.lockedPosts.count
    }

    func posts(for ownerTab: TelefamIosOwnerProfileTab) -> [TelefamIosFeedItem] {
        guard isOwner else {
            return posts.filter { $0.privacy.lowercased() != "private" }
        }
        switch ownerTab {
        case .posts:
            return posts.filter { $0.privacy.lowercased() != "private" }
        case .saved:
            return savedPosts
        case .reposts:
            return repostedPosts
        case .locked:
            if !lockedPosts.isEmpty { return lockedPosts }
            return posts.filter { $0.privacy.lowercased() == "private" }
        }
    }

    func filteredAndSortedPosts(
        ownerTab: TelefamIosOwnerProfileTab,
        filter: TelefamIosProfilePostFilter,
        sort: TelefamIosProfilePostSort
    ) -> [TelefamIosFeedItem] {
        let base = posts(for: ownerTab)
        let filtered: [TelefamIosFeedItem]
        switch filter {
        case .all: filtered = base
        case .videos: filtered = base.filter { $0.mode == "video" }
        case .photos: filtered = base.filter { $0.mode == "photos" }
        case .text: filtered = base.filter { $0.mode == "text" }
        }
        switch sort {
        case .latest:
            return filtered.sorted { $0.createdAtMs > $1.createdAtMs }
        case .mostViewed:
            return filtered.sorted { $0.viewsCount > $1.viewsCount }
        case .mostLiked:
            return filtered.sorted { $0.likesCount > $1.likesCount }
        }
    }
}

private struct UserProfileAPIResponse: Codable {
    let success: Bool
    let userId: String?
    let fullName: String?
    let username: String?
    let avatarUrl: String?
    let bio: String?
    let location: String?
    let countryIso: String?
    let gender: String?
    let dateOfBirth: String?
    let phoneNumber: String?
    let usernameChangedAtMs: Int64?
    let nextUsernameChangeMs: Int64?
    let backgroundUrls: [String]?
    let backgrounds: [TelefamIosProfileBackgroundItem]?
    let followingCount: Int64?
    let followersCount: Int64?
    let likesCount: Int64?
    let isFollowing: Bool?
    let followsYou: Bool?
    let isOwner: Bool?
    let isBlocked: Bool?
    let isMuted: Bool?
    let isPrivateRestricted: Bool?
    let whoCanComment: String?
    let whoCanMention: String?
    let likesVisibility: String?
    let followingVisibility: String?
    let showActivityStatus: Bool?
    let riskScore: Double?
    let posts: [TelefamIosFeedItem]?
    let savedPosts: [TelefamIosFeedItem]?
    let repostedPosts: [TelefamIosFeedItem]?
    let lockedPosts: [TelefamIosFeedItem]?

    enum CodingKeys: String, CodingKey {
        case success
        case userId = "user_id"
        case fullName = "full_name"
        case username
        case avatarUrl = "avatar_url"
        case bio
        case location
        case countryIso = "country_iso"
        case gender
        case dateOfBirth = "date_of_birth"
        case phoneNumber = "phone_number"
        case usernameChangedAtMs = "username_changed_at_ms"
        case nextUsernameChangeMs = "next_username_change_ms"
        case backgroundUrls = "background_urls"
        case backgrounds
        case followingCount = "following_count"
        case followersCount = "followers_count"
        case likesCount = "likes_count"
        case isFollowing = "is_following"
        case followsYou = "follows_you"
        case isOwner = "is_owner"
        case isBlocked = "is_blocked"
        case isMuted = "is_muted"
        case isPrivateRestricted = "is_private_restricted"
        case whoCanComment = "who_can_comment"
        case whoCanMention = "who_can_mention"
        case likesVisibility = "likes_visibility"
        case followingVisibility = "following_visibility"
        case showActivityStatus = "show_activity_status"
        case riskScore = "risk_score"
        case posts
        case savedPosts = "saved_posts"
        case repostedPosts = "reposted_posts"
        case lockedPosts = "locked_posts"
    }
}

private struct UserConnectionsAPIResponse: Codable {
    let success: Bool
    let items: [TelefamContactRecord]
    let page: Int
    let hasMore: Bool

    enum CodingKeys: String, CodingKey {
        case success, items, page
        case hasMore = "has_more"
    }
}

private struct UserBlockAPIResponse: Codable {
    let success: Bool
    let isBlocked: Bool?

    enum CodingKeys: String, CodingKey {
        case success
        case isBlocked = "is_blocked"
    }
}

func formatTelefamProfileMetricCount(_ value: Int64) -> String {
    if value < 1_000 { return "\(max(0, value))" }
    if value < 1_000_000 {
        let tenths = value / 100
        let whole = tenths / 10
        let rem = tenths % 10
        return rem == 0 ? "\(whole)K" : "\(whole).\(rem)K"
    }
    let tenths = value / 100_000
    let whole = tenths / 10
    let rem = tenths % 10
    return rem == 0 ? "\(whole)M" : "\(whole).\(rem)M"
}

@MainActor
final class TelefamIosProfileStore: ObservableObject {
    @Published var profile: TelefamIosUserProfile
    @Published var isLoading: Bool = true
    @Published var connections: [TelefamContactRecord] = []
    @Published var connectionsIsFollowers: Bool? = nil
    @Published var isLoadingConnections: Bool = false
    @Published var connectionsPage: Int = 1
    @Published var connectionsHasMore: Bool = false
    @Published var toastMessage: String? = nil

    private let baseURL: URL
    private let sessionStore: TelefamIosSessionStore
    private let contactsStore: TelefamContactsStore
    private let feedStore: TelefamIosFeedStore

    init(
        targetUserId: String,
        isOwnHint: Bool,
        baseURL: URL,
        sessionStore: TelefamIosSessionStore,
        contactsStore: TelefamContactsStore,
        feedStore: TelefamIosFeedStore
    ) {
        self.baseURL = baseURL
        self.sessionStore = sessionStore
        self.contactsStore = contactsStore
        self.feedStore = feedStore

        let resolvedId = targetUserId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? (sessionStore.userId.isEmpty ? "me" : sessionStore.userId)
            : targetUserId.trimmingCharacters(in: .whitespacesAndNewlines)

        let ownerFlag = isOwnHint || resolvedId == "me" || (!sessionStore.userId.isEmpty && resolvedId == sessionStore.userId)
        let contactMatch = contactsStore.allCached.first { $0.userId == resolvedId }

        self.profile = TelefamIosUserProfile(
            userId: resolvedId,
            fullName: contactMatch?.fullName ?? (ownerFlag ? sessionStore.fullName : ""),
            username: contactMatch?.username ?? (ownerFlag ? sessionStore.username : ""),
            avatarUrl: contactMatch?.avatarUrl ?? "",
            bio: ownerFlag ? sessionStore.bio : "",
            location: contactMatch?.location ?? (ownerFlag ? sessionStore.location : ""),
            countryIso: contactMatch?.countryIso ?? "",
            gender: "",
            dateOfBirth: "",
            phoneNumber: ownerFlag ? sessionStore.phoneDisplay : "",
            usernameChangedAtMs: 0,
            nextUsernameChangeMs: 0,
            backgroundUrls: [],
            backgrounds: [],
            followingCount: 0,
            followersCount: 0,
            likesCount: 0,
            isFollowing: contactMatch?.isFollowing ?? false,
            followsYou: contactMatch?.followsYou ?? false,
            isOwner: ownerFlag,
            isBlocked: false,
            riskScore: contactMatch?.riskScore ?? 0,
            posts: [],
            savedPosts: [],
            repostedPosts: [],
            lockedPosts: []
        )

        if let cached = Self.loadCachedProfile(userId: resolvedId) {
            self.profile = cached
            self.isLoading = false
        }
    }

    private static func profileCacheURL(userId: String) -> URL {
        let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let safe = userId.replacingOccurrences(of: "/", with: "_")
        return dir.appendingPathComponent("telefam_ios_profile_\(safe).json")
    }

    private static func connectionsCacheURL(userId: String, isFollowers: Bool) -> URL {
        let dir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let safe = userId.replacingOccurrences(of: "/", with: "_")
        let kind = isFollowers ? "followers" : "following"
        return dir.appendingPathComponent("telefam_ios_conn_\(safe)_\(kind).json")
    }

    private static func loadCachedProfile(userId: String) -> TelefamIosUserProfile? {
        guard let data = try? Data(contentsOf: profileCacheURL(userId: userId)),
              let decoded = try? JSONDecoder().decode(TelefamIosUserProfile.self, from: data) else {
            return nil
        }
        return decoded
    }

    private func persistProfileCache() {
        if let data = try? JSONEncoder().encode(profile) {
            try? data.write(to: Self.profileCacheURL(userId: profile.userId))
        }
    }

    func fetchProfile(forceLoading: Bool = false) async {
        if forceLoading || profile.fullName.isEmpty {
            isLoading = true
        }
        defer { isLoading = false }

        let targetId = profile.userId.isEmpty ? "me" : profile.userId
        let url = baseURL
            .appendingPathComponent("v1/users")
            .appendingPathComponent(targetId)
            .appendingPathComponent("profile")

        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let decoded = try? JSONDecoder().decode(UserProfileAPIResponse.self, from: data),
           decoded.success {
            let finalId = (decoded.userId?.isEmpty == false) ? decoded.userId! : targetId
            let resolvedBgs: [TelefamIosProfileBackgroundItem] = {
                if let bgs = decoded.backgrounds, !bgs.isEmpty {
                    return bgs
                }
                return (decoded.backgroundUrls ?? []).enumerated().map { idx, u in
                    TelefamIosProfileBackgroundItem(mediaId: "bg_\(idx)", url: u)
                }
            }()
            profile = TelefamIosUserProfile(
                userId: finalId,
                fullName: decoded.fullName ?? profile.fullName,
                username: decoded.username ?? profile.username,
                avatarUrl: decoded.avatarUrl ?? profile.avatarUrl,
                bio: decoded.bio ?? profile.bio,
                location: decoded.location ?? profile.location,
                countryIso: decoded.countryIso ?? profile.countryIso,
                gender: decoded.gender ?? profile.gender,
                dateOfBirth: decoded.dateOfBirth ?? profile.dateOfBirth,
                phoneNumber: decoded.phoneNumber ?? profile.phoneNumber,
                usernameChangedAtMs: decoded.usernameChangedAtMs ?? profile.usernameChangedAtMs,
                nextUsernameChangeMs: decoded.nextUsernameChangeMs ?? profile.nextUsernameChangeMs,
                backgroundUrls: decoded.backgroundUrls ?? resolvedBgs.map(\.url),
                backgrounds: resolvedBgs,
                followingCount: decoded.followingCount ?? 0,
                followersCount: decoded.followersCount ?? 0,
                likesCount: decoded.likesCount ?? 0,
                isFollowing: decoded.isFollowing ?? false,
                followsYou: decoded.followsYou ?? false,
                isOwner: decoded.isOwner ?? profile.isOwner,
                isBlocked: decoded.isBlocked ?? false,
                isMuted: decoded.isMuted ?? profile.isMuted,
                isPrivateRestricted: decoded.isPrivateRestricted ?? false,
                whoCanComment: decoded.whoCanComment ?? "everyone",
                whoCanMention: decoded.whoCanMention ?? "everyone",
                likesVisibility: decoded.likesVisibility ?? "everyone",
                followingVisibility: decoded.followingVisibility ?? "everyone",
                showActivityStatus: decoded.showActivityStatus ?? true,
                riskScore: decoded.riskScore ?? 0,
                posts: decoded.posts ?? [],
                savedPosts: decoded.savedPosts ?? [],
                repostedPosts: decoded.repostedPosts ?? [],
                lockedPosts: decoded.lockedPosts ?? []
            )
            persistProfileCache()
        } else if let cached = Self.loadCachedProfile(userId: targetId) {
            profile = cached
        }
    }

    func applyUpdatedBackgrounds(_ bgs: [TelefamIosProfileBackgroundItem]) {
        profile.backgrounds = Array(bgs.prefix(5))
        profile.backgroundUrls = profile.backgrounds.map(\.url)
        persistProfileCache()
    }

    func toggleFollow() {
        let wasFollowing = profile.isFollowing
        let nextFollowing = !wasFollowing
        profile.isFollowing = nextFollowing
        profile.followersCount = nextFollowing
            ? profile.followersCount + 1
            : max(0, profile.followersCount - 1)
        persistProfileCache()
        feedStore.markAuthorFollowing(userId: profile.userId, isFollowing: nextFollowing)

        // Sync via contacts store if present, or call contacts/follow|unfollow directly.
        if contactsStore.allCached.contains(where: { $0.userId == profile.userId }) {
            contactsStore.toggleFollow(for: profile.userId)
            if let msg = contactsStore.toastMessage {
                showToast(msg)
            }
            return
        }

        Task {
            let endpoint = nextFollowing ? "v1/contacts/follow" : "v1/contacts/unfollow"
            var req = URLRequest(url: baseURL.appendingPathComponent(endpoint))
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
            req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
            req.httpBody = try? JSONSerialization.data(withJSONObject: ["target_user_id": profile.userId])

            if let (_, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse {
                if http.statusCode == 429 {
                    profile.isFollowing = wasFollowing
                    profile.followersCount = wasFollowing ? profile.followersCount + 1 : max(0, profile.followersCount - 1)
                    persistProfileCache()
                    showToast("Try again later")
                } else if !(200...299).contains(http.statusCode) {
                    showToast("Saved offline — will sync when online")
                }
            }
        }
    }

    func openConnections(isFollowers: Bool) async {
        connectionsIsFollowers = isFollowers
        connectionsPage = 1
        connectionsHasMore = false
        if let data = try? Data(contentsOf: Self.connectionsCacheURL(userId: profile.userId, isFollowers: isFollowers)),
           let cached = try? JSONDecoder().decode([TelefamContactRecord].self, from: data) {
            connections = cached
        } else {
            connections = []
        }
        await loadConnectionsPage(page: 1, append: false, isFollowers: isFollowers)
    }

    func loadMoreConnectionsIfNeeded() async {
        guard let isFollowers = connectionsIsFollowers,
              connectionsHasMore,
              !isLoadingConnections else { return }
        await loadConnectionsPage(page: connectionsPage + 1, append: true, isFollowers: isFollowers)
    }

    private func loadConnectionsPage(page: Int, append: Bool, isFollowers: Bool) async {
        isLoadingConnections = true
        defer { isLoadingConnections = false }

        let targetId = profile.userId.isEmpty ? "me" : profile.userId
        let pathSuffix = isFollowers ? "followers" : "following"
        let baseEndpoint = baseURL
            .appendingPathComponent("v1/users")
            .appendingPathComponent(targetId)
            .appendingPathComponent(pathSuffix)
        var comps = URLComponents(url: baseEndpoint, resolvingAgainstBaseURL: false)
        comps?.queryItems = [
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "page_size", value: "30")
        ]
        guard let url = comps?.url else { return }

        var req = URLRequest(url: url)
        req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(UserConnectionsAPIResponse.self, from: data),
           parsed.success {
            connectionsPage = parsed.page
            connectionsHasMore = parsed.hasMore
            if append {
                var merged = connections
                for item in parsed.items where !merged.contains(where: { $0.userId == item.userId }) {
                    merged.append(item)
                }
                connections = merged
            } else {
                connections = parsed.items
            }
            if let encoded = try? JSONEncoder().encode(connections) {
                try? encoded.write(to: Self.connectionsCacheURL(userId: targetId, isFollowers: isFollowers))
            }
        }
    }

    func toggleConnectionFollow(targetUserId: String) {
        if let idx = connections.firstIndex(where: { $0.userId == targetUserId }) {
            connections[idx].isFollowing.toggle()
        }
        if contactsStore.allCached.contains(where: { $0.userId == targetUserId }) {
            contactsStore.toggleFollow(for: targetUserId)
            return
        }
        let wantFollow = connections.first(where: { $0.userId == targetUserId })?.isFollowing ?? true
        Task {
            let endpoint = wantFollow ? "v1/contacts/follow" : "v1/contacts/unfollow"
            var req = URLRequest(url: baseURL.appendingPathComponent(endpoint))
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
            req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
            req.httpBody = try? JSONSerialization.data(withJSONObject: ["target_user_id": targetUserId])
            _ = try? await URLSession.shared.data(for: req)
        }
    }

    func blockUser() async {
        let targetId = profile.userId
        guard !targetId.isEmpty else { return }
        profile.isBlocked = true
        profile.isFollowing = false
        persistProfileCache()

        var req = URLRequest(
            url: baseURL.appendingPathComponent("v1/users").appendingPathComponent(targetId).appendingPathComponent("block")
        )
        req.httpMethod = "POST"
        req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(UserBlockAPIResponse.self, from: data),
           parsed.success {
            profile.isBlocked = parsed.isBlocked ?? true
            persistProfileCache()
            showToast("Account blocked")
        } else {
            showToast("Unable to update block status right now")
        }
    }

    func unblockUser() async {
        let targetId = profile.userId
        guard !targetId.isEmpty else { return }
        profile.isBlocked = false
        persistProfileCache()

        var req = URLRequest(
            url: baseURL.appendingPathComponent("v1/users").appendingPathComponent(targetId).appendingPathComponent("unblock")
        )
        req.httpMethod = "POST"
        req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(UserBlockAPIResponse.self, from: data),
           parsed.success {
            profile.isBlocked = parsed.isBlocked ?? false
            persistProfileCache()
            showToast("Account unblocked")
            await fetchProfile()
        } else {
            showToast("Unable to unblock right now")
        }
    }

    func muteUser(settingsStore: TelefamIosSettingsStore) async {
        let targetId = profile.userId
        guard !targetId.isEmpty else { return }
        profile.isMuted = true
        persistProfileCache()
        await settingsStore.muteAccount(
            userId: targetId,
            fullName: profile.fullName,
            username: profile.username,
            avatarUrl: profile.avatarUrl
        )
        showToast("Account muted")
    }

    func unmuteUser(settingsStore: TelefamIosSettingsStore) async {
        let targetId = profile.userId
        guard !targetId.isEmpty else { return }
        profile.isMuted = false
        persistProfileCache()
        await settingsStore.unmuteAccount(userId: targetId)
        showToast("Account unmuted")
    }

    func reportUser(reason: String, details: String) async {
        let targetId = profile.userId
        guard !targetId.isEmpty else { return }

        var req = URLRequest(
            url: baseURL.appendingPathComponent("v1/users").appendingPathComponent(targetId).appendingPathComponent("report")
        )
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(sessionStore.sessionToken)", forHTTPHeaderField: "Authorization")
        req.setValue(sessionStore.deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.httpBody = try? JSONSerialization.data(withJSONObject: ["reason": reason, "details": details])

        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode) {
            showToast("Report submitted")
        } else {
            showToast("Unable to submit report right now")
        }
    }

    func incrementLocalPostView(postId: String) {
        if let idx = profile.posts.firstIndex(where: { $0.postId == postId }) {
            profile.posts[idx].viewsCount += 1
            persistProfileCache()
        }
        feedStore.recordView(postId: postId)
    }

    func shareProfileURL() -> URL {
        let handle = profile.username.isEmpty ? profile.userId : profile.username
        if let host = baseURL.host?.lowercased(),
           host != "127.0.0.1", host != "localhost", host != "::1" {
            return baseURL.appendingPathComponent("u").appendingPathComponent(handle)
        }
        return URL(string: "telefam://u/\(handle)")!
    }

    func showToast(_ message: String) {
        toastMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in
            if self?.toastMessage == message { self?.toastMessage = nil }
        }
    }
}

struct TelefamIosProfileView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @ObservedObject var sessionStore: TelefamIosSessionStore
    @ObservedObject var contactsStore: TelefamContactsStore
    @ObservedObject var feedStore: TelefamIosFeedStore
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    @StateObject private var profileStore: TelefamIosProfileStore

    let isEmbeddedOwnProfileTab: Bool
    let bottomChromePadding: CGFloat
    @Binding var themeMode: TelefamThemeMode
    var onBack: () -> Void
    var onOpenUserProfile: (String) -> Void
    var onEditOwnerPost: (TelefamIosFeedItem) -> Void
    var onTapHashtag: (String, TelefamIosPostMode) -> Void
    var onTapAudio: (TelefamIosSongTrack, TelefamIosPostMode) -> Void
    var onMessageUser: ((TelefamIosUserProfile) -> Void)? = nil

    @EnvironmentObject private var smartLinksCoordinator: TelefamIosSmartLinksCoordinator
    @State private var activeOwnerTab: TelefamIosOwnerProfileTab = .posts
    @State private var savedSubTab: Int = 0
    @State private var selectedFilter: TelefamIosProfilePostFilter = .all
    @State private var selectedSort: TelefamIosProfilePostSort = .latest
    @State private var viewerPosts: [TelefamIosFeedItem]? = nil
    @State private var viewerStartIndex: Int = 0
    @State private var showConnectionsSheet: Bool = false
    @State private var showReportCategorySheet: Bool = false
    @State private var selectedReportCategory: String? = nil
    @State private var reportDetails: String = ""
    @State private var showSettingsSheet: Bool = false
    @State private var showBackgroundsSheet: Bool = false
    @State private var showEditProfileSheet: Bool = false
    @State private var heroIndex: Int = 0
    private let heroCycleTimer = Timer.publish(every: 5.0, on: .main, in: .common).autoconnect()

    init(
        targetUserId: String,
        isEmbeddedOwnProfileTab: Bool,
        bottomChromePadding: CGFloat,
        baseURL: URL,
        sessionStore: TelefamIosSessionStore,
        contactsStore: TelefamContactsStore,
        feedStore: TelefamIosFeedStore,
        settingsStore: TelefamIosSettingsStore,
        themeMode: Binding<TelefamThemeMode>,
        onBack: @escaping () -> Void,
        onOpenUserProfile: @escaping (String) -> Void,
        onEditOwnerPost: @escaping (TelefamIosFeedItem) -> Void,
        onTapHashtag: @escaping (String, TelefamIosPostMode) -> Void,
        onTapAudio: @escaping (TelefamIosSongTrack, TelefamIosPostMode) -> Void,
        onMessageUser: ((TelefamIosUserProfile) -> Void)? = nil
    ) {
        self.isEmbeddedOwnProfileTab = isEmbeddedOwnProfileTab
        self.bottomChromePadding = bottomChromePadding
        self.sessionStore = sessionStore
        self.contactsStore = contactsStore
        self.feedStore = feedStore
        self.settingsStore = settingsStore
        self._themeMode = themeMode
        self.onBack = onBack
        self.onOpenUserProfile = onOpenUserProfile
        self.onEditOwnerPost = onEditOwnerPost
        self.onTapHashtag = onTapHashtag
        self.onTapAudio = onTapAudio
        self.onMessageUser = onMessageUser
        _profileStore = StateObject(
            wrappedValue: TelefamIosProfileStore(
                targetUserId: targetUserId,
                isOwnHint: isEmbeddedOwnProfileTab,
                baseURL: baseURL,
                sessionStore: sessionStore,
                contactsStore: contactsStore,
                feedStore: feedStore
            )
        )
    }

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }

    var body: some View {
        let p = profileStore.profile
        let showHeroBanner = !p.isBlocked && (p.isOwner || !p.backgrounds.isEmpty)
        let filteredPosts = p.filteredAndSortedPosts(
            ownerTab: activeOwnerTab,
            filter: selectedFilter,
            sort: selectedSort
        )

        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if !showHeroBanner {
                    topBar(profile: p)
                }

                GeometryReader { geo in
                    let gridColumnsCount = geo.size.width >= 960 ? 5 : (geo.size.width >= 680 ? 4 : 3)
                    let gridCols = Array(repeating: GridItem(.flexible(), spacing: 2), count: gridColumnsCount)

                    ScrollView {
                        VStack(spacing: 0) {
                            profileHeader(profile: p, showHeroBanner: showHeroBanner)

                            // Owner 4 Tabs (Posts <count>, Saved, Reposts, Locked) or Visitor Posts Tab
                            VStack(spacing: 0) {
                                if p.isOwner {
                                    HStack(spacing: 0) {
                                        ForEach(TelefamIosOwnerProfileTab.allCases) { tab in
                                            let selected = activeOwnerTab == tab
                                            let tabColor = selected ? TelefamPalette.textPrimary(colorScheme) : TelefamPalette.textMuted(colorScheme)
                                            Button {
                                                activeOwnerTab = tab
                                            } label: {
                                                VStack(spacing: 10) {
                                                    HStack(spacing: 5) {
                                                        ownerTabIcon(tab: tab, color: tabColor)
                                                            .frame(width: 15, height: 15)
                                                        Text(
                                                            tab == .posts
                                                                ? "Posts \(formatTelefamProfileMetricCount(Int64(p.posts(for: .posts).count)))"
                                                                : tab.label
                                                        )
                                                        .font(.system(size: 13.5, weight: selected ? .bold : .medium))
                                                        .foregroundColor(tabColor)
                                                        .lineLimit(1)
                                                    }
                                                    Rectangle()
                                                        .fill(selected ? accentCrimson : Color.clear)
                                                        .frame(height: 2.5)
                                                }
                                                .frame(maxWidth: .infinity)
                                                .padding(.top, 14)
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                    .padding(.horizontal, 6)
                                } else {
                                    HStack {
                                        Spacer()
                                        VStack(spacing: 10) {
                                            HStack(spacing: 6) {
                                                PostsGrid2x2SvgIcon(color: TelefamPalette.textPrimary(colorScheme))
                                                    .frame(width: 17, height: 17)
                                                Text("Posts")
                                                    .font(.system(size: 14, weight: .bold))
                                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            }
                                            Rectangle()
                                                .fill(accentCrimson)
                                                .frame(width: 96, height: 2.5)
                                        }
                                        Spacer()
                                        Spacer()
                                        Spacer()
                                    }
                                    .padding(.top, 18)
                                }

                                Divider().background(TelefamPalette.border(colorScheme))

                                if p.isOwner && activeOwnerTab == .saved {
                                    HStack(spacing: 8) {
                                        ForEach([(0, "Posts"), (1, "Links")], id: \.0) { idx, label in
                                            let selected = savedSubTab == idx
                                            Button {
                                                savedSubTab = idx
                                            } label: {
                                                Text(label)
                                                    .font(.system(size: 12.5, weight: selected ? .bold : .semibold))
                                                    .foregroundColor(selected ? .white : TelefamPalette.textPrimary(colorScheme))
                                                    .padding(.horizontal, 14)
                                                    .padding(.vertical, 6)
                                                    .background(selected ? accentCrimson : TelefamPalette.surface(colorScheme))
                                                    .clipShape(Capsule())
                                                    .overlay(
                                                        Capsule().stroke(selected ? Color.clear : TelefamPalette.border(colorScheme), lineWidth: 1)
                                                    )
                                            }
                                            .buttonStyle(.plain)
                                        }
                                        Spacer()
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.top, 10)
                                    .padding(.bottom, 4)
                                }
                            }

                            if p.isOwner && activeOwnerTab == .saved && savedSubTab == 1 {
                                TelefamIosSavedLinksView()
                            } else {
                                // Filter & Sort Toolbar ("All posts v" | Sort Sliders)
                            HStack {
                                Menu {
                                    ForEach(TelefamIosProfilePostFilter.allCases) { f in
                                        Button {
                                            selectedFilter = f
                                        } label: {
                                            if selectedFilter == f {
                                                Label(f.label, systemImage: "checkmark")
                                            } else {
                                                Text(f.label)
                                            }
                                        }
                                    }
                                } label: {
                                    HStack(spacing: 4) {
                                        Text(selectedFilter.label)
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                        Image(systemName: "chevron.down")
                                            .font(.system(size: 11.5, weight: .bold))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    }
                                    .frame(minHeight: 44)
                                }

                                Spacer()

                                Menu {
                                    ForEach(TelefamIosProfilePostSort.allCases) { s in
                                        Button {
                                            selectedSort = s
                                        } label: {
                                            if selectedSort == s {
                                                Label(s.label, systemImage: "checkmark")
                                            } else {
                                                Text(s.label)
                                            }
                                        }
                                    }
                                } label: {
                                    FilterSlidersSvgIcon(color: TelefamPalette.textPrimary(colorScheme))
                                        .frame(width: 44, height: 44)
                                }
                            }
                            .padding(.horizontal, 14)

                            if profileStore.isLoading && filteredPosts.isEmpty {
                                HStack {
                                    Spacer()
                                    TelefamIosTricolorSpinner(size: 38)
                                        .padding(.vertical, 32)
                                    Spacer()
                                }
                            } else if filteredPosts.isEmpty {
                                VStack(spacing: 8) {
                                    let emptyTitle: String = {
                                        if p.isBlocked { return "Blocked account" }
                                        if !p.isOwner && p.isPrivateRestricted { return "Private account" }
                                        if !p.isOwner { return "No posts yet" }
                                        switch activeOwnerTab {
                                        case .posts: return "No posts yet"
                                        case .saved: return "No saved posts"
                                        case .reposts: return "No reposts yet"
                                        case .locked: return "No locked posts"
                                        }
                                    }()
                                    let emptyBody: String = {
                                        if p.isBlocked { return "Unblock this account to view their posts." }
                                        if !p.isOwner && p.isPrivateRestricted { return "Follow this account to see their posts." }
                                        if !p.isOwner { return "Published photos, text cards, and videos appear here." }
                                        switch activeOwnerTab {
                                        case .posts: return "Published photos, text cards, and videos appear here."
                                        case .saved: return "Bookmarked posts you save in feeds will appear here."
                                        case .reposts: return "Posts you repost will appear here."
                                        case .locked: return "Posts you switch to Private appear safely locked here."
                                        }
                                    }()
                                    Text(emptyTitle)
                                        .font(.system(size: 15.5, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text(emptyBody)
                                        .font(.system(size: 13))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        .multilineTextAlignment(.center)
                                }
                                .padding(.horizontal, 28)
                                .padding(.vertical, 36)
                            } else {
                                LazyVGrid(columns: gridCols, spacing: 2) {
                                    ForEach(Array(filteredPosts.enumerated()), id: \.element.postId) { idx, post in
                                        ProfilePostGridCell(
                                            post: post,
                                            isOwner: p.isOwner,
                                            feedStore: feedStore,
                                            accentCrimson: accentCrimson
                                        ) {
                                            profileStore.incrementLocalPostView(postId: post.postId)
                                            viewerPosts = filteredPosts
                                            viewerStartIndex = idx
                                        }
                                    }
                                }
                            }
                            }
                        }
                        .frame(maxWidth: 920)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 24 + bottomChromePadding)
                    }
                }
            }
            .background(TelefamPalette.background(colorScheme).ignoresSafeArea())

            if let toast = profileStore.toastMessage {
                Text(toast)
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.84))
                    .clipShape(Capsule())
                    .padding(.bottom, 28 + bottomChromePadding)
                    .transition(.opacity)
            }
        }
        .task {
            await profileStore.fetchProfile()
        }
        .sheet(isPresented: $showConnectionsSheet) {
            ProfileConnectionsSheetView(
                title: (profileStore.connectionsIsFollowers ?? true) ? "Followers" : "Following",
                profileStore: profileStore,
                contactsStore: contactsStore
            ) { clickedUserId in
                showConnectionsSheet = false
                onOpenUserProfile(clickedUserId)
            }
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .confirmationDialog("Report account", isPresented: $showReportCategorySheet) {
            ForEach(TelefamIosProfileReportOptions.categories, id: \.self) { cat in
                Button(cat) {
                    selectedReportCategory = cat
                    reportDetails = ""
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: Binding(get: { selectedReportCategory != nil }, set: { if !$0 { selectedReportCategory = nil } })) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 14) {
                    Text(selectedReportCategory ?? "")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                    TextEditor(text: $reportDetails)
                        .frame(minHeight: 110)
                        .padding(8)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

                    Spacer()

                    Button {
                        let cat = selectedReportCategory ?? "Other"
                        let det = reportDetails
                        selectedReportCategory = nil
                        Task { await profileStore.reportUser(reason: cat, details: det) }
                    } label: {
                        Text("Submit")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                    }
                }
                .padding(20)
                .background(TelefamPalette.background(colorScheme))
                .navigationTitle("Report")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { selectedReportCategory = nil }
                    }
                }
            }
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: $showSettingsSheet) {
            TelefamIosSettingsView(
                sessionStore: sessionStore,
                settingsStore: settingsStore,
                themeMode: $themeMode,
                displayName: p.fullName.isEmpty ? sessionStore.fullName : p.fullName,
                username: p.username.isEmpty ? sessionStore.username : p.username,
                avatarUrl: p.avatarUrl,
                authorizedAvatarUrl: feedStore.authorizedURLString(p.avatarUrl),
                profileShareUrl: profileStore.shareProfileURL().absoluteString
            )
            .environmentObject(smartLinksCoordinator)
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .sheet(isPresented: $showBackgroundsSheet) {
            EditBackgroundsSheet(
                sessionStore: sessionStore,
                profileStore: profileStore,
                feedStore: feedStore
            )
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: $showEditProfileSheet) {
            EditProfileSheet(
                sessionStore: sessionStore,
                profileStore: profileStore,
                feedStore: feedStore
            )
            .preferredColorScheme(themeMode.preferredColorScheme)
        }
        .fullScreenCover(isPresented: Binding(get: { viewerPosts != nil }, set: { if !$0 { viewerPosts = nil } })) {
            if let vPosts = viewerPosts {
                TelefamIosFeedView(
                    store: feedStore,
                    contactsStore: contactsStore,
                    settingsStore: settingsStore,
                    items: vPosts,
                    initialIndex: viewerStartIndex,
                    isStandaloneViewer: true,
                    bottomChromePadding: 0,
                    onLiveClick: {},
                    onSearchClick: { viewerPosts = nil },
                    onBackClick: { viewerPosts = nil },
                    onCommentClick: { profileStore.showToast("Comments — Coming soon") },
                    onEditOwnerPost: { item in
                        viewerPosts = nil
                        onEditOwnerPost(item)
                    },
                    onTapHashtag: { tag, mode in
                        viewerPosts = nil
                        onTapHashtag(tag, mode)
                    },
                    onTapAudio: { song, mode in
                        viewerPosts = nil
                        onTapAudio(song, mode)
                    },
                    onOpenAuthorProfile: { uid in
                        viewerPosts = nil
                        onOpenUserProfile(uid)
                    }
                )
                .environmentObject(smartLinksCoordinator)
                .telefamSmartOverlayHost(
                    preferredColorScheme: themeMode.preferredColorScheme,
                    showLocalToast: true
                )
                .preferredColorScheme(themeMode.preferredColorScheme)
            }
        }
        .onReceive(heroCycleTimer) { _ in
            let count = profileStore.profile.backgrounds.count
            if count > 1 {
                if settingsStore.bundle.contentPreferences.reducedMotion {
                    heroIndex = (heroIndex + 1) % count
                } else {
                    withAnimation(.easeInOut(duration: 0.7)) {
                        heroIndex = (heroIndex + 1) % count
                    }
                }
            } else {
                heroIndex = 0
            }
        }
    }

    private func topBar(profile: TelefamIosUserProfile) -> some View {
        HStack(spacing: 4) {
            if !isEmbeddedOwnProfileTab {
                Button {
                    onBack()
                } label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .frame(width: 44, height: 44)
                }
            } else {
                Spacer().frame(width: 44, height: 44)
            }

            Spacer()

            Text(profile.fullName.isEmpty ? "@\(profile.username.isEmpty ? profile.userId : profile.username)" : profile.fullName)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                .lineLimit(1)

            Spacer()

            Button {
                presentInviteShare()
            } label: {
                InviteTBadgeSvgIcon(badgeColor: accentCrimson)
                    .frame(width: 44, height: 44)
            }

            Menu {
                Button("Copy link") {
                    UIPasteboard.general.string = profileStore.shareProfileURL().absoluteString
                    profileStore.showToast("Link copied")
                }
                Button("Invite friends") {
                    presentInviteShare()
                }
                if profile.isOwner {
                    Menu("Theme: \(themeMode.label)") {
                        ForEach(TelefamThemeMode.allCases) { m in
                            Button(m.label) { themeMode = m }
                        }
                    }
                    Button("Sign out", role: .destructive) {
                        sessionStore.signOut()
                    }
                } else {
                    if profile.isMuted {
                        Button("Unmute") { Task { await profileStore.unmuteUser(settingsStore: settingsStore) } }
                    } else {
                        Button("Mute") { Task { await profileStore.muteUser(settingsStore: settingsStore) } }
                    }
                    if profile.isBlocked {
                        Button("Unblock") { Task { await profileStore.unblockUser() } }
                    } else {
                        Button("Block", role: .destructive) { Task { await profileStore.blockUser() } }
                    }
                    Button("Report", role: .destructive) { showReportCategorySheet = true }
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    .frame(width: 44, height: 44)
            }
        }
        .padding(.horizontal, 6)
        .frame(height: 52)
    }

    private func profileHeader(profile: TelefamIosUserProfile, showHeroBanner: Bool) -> some View {
        let bgColor = TelefamPalette.background(colorScheme)
        let bgs = profile.backgrounds
        let safeIdx = bgs.isEmpty ? 0 : (heroIndex % bgs.count)

        return VStack(spacing: 0) {
            if showHeroBanner {
                ZStack(alignment: .bottom) {
                    // Multi-background auto-crossfade + smooth bottom fade matching reference
                    ZStack(alignment: .top) {
                        if bgs.isEmpty {
                            LinearGradient(
                                colors: [Color(hex: 0x35101A), Color(hex: 0x161922), bgColor],
                                startPoint: .topLeading,
                                endPoint: .bottom
                            )
                        } else {
                            let bg = bgs[safeIdx]
                            TelefamIosRemoteImage(
                                urlString: feedStore.authorizedURLString(bg.url),
                                bearerToken: sessionStore.sessionToken,
                                deviceFingerprint: sessionStore.deviceFingerprint,
                                fallbackInitials: ""
                            )
                            .id(bg.mediaId)
                            .frame(maxWidth: .infinity, minHeight: 232, maxHeight: 232)
                            .clipped()
                            .transition(.opacity)
                        }

                        // Top dark scrim
                        LinearGradient(
                            colors: [Color.black.opacity(0.62), .clear],
                            startPoint: .top,
                            endPoint: .center
                        )
                        .frame(height: 120)

                        // Bottom smooth fade into page background
                        VStack {
                            Spacer()
                            LinearGradient(
                                colors: [.clear, bgColor.opacity(0.68), bgColor],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                            .frame(height: 112)
                        }

                        // Top overlay controls + Edit background pill
                        VStack {
                            HStack(spacing: 6) {
                                if !isEmbeddedOwnProfileTab {
                                    Button(action: onBack) {
                                        Image(systemName: "arrow.left")
                                            .font(.system(size: 17, weight: .bold))
                                            .foregroundColor(.white)
                                            .frame(width: 40, height: 40)
                                            .background(Color.black.opacity(0.38))
                                            .clipShape(Circle())
                                    }
                                } else {
                                    Spacer().frame(width: 8, height: 40)
                                }

                                Text(profile.fullName.isEmpty ? "@\(profile.username.isEmpty ? profile.userId : profile.username)" : profile.fullName)
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                                    .shadow(color: .black.opacity(0.5), radius: 3, y: 1)
                                    .lineLimit(1)

                                Spacer()

                                if profile.isOwner {
                                    // Crown -> Subscribers (Coming soon)
                                    Button {
                                        profileStore.showToast("Subscribers — Coming soon")
                                    } label: {
                                        SubscribeCrownSvgIcon(color: .white)
                                            .frame(width: 18, height: 18)
                                            .frame(width: 38, height: 38)
                                            .background(Color.black.opacity(0.38))
                                            .clipShape(Circle())
                                    }

                                    // Gear -> Settings (Log out first)
                                    Button {
                                        showSettingsSheet = true
                                    } label: {
                                        GearProfileSvgIcon(color: .white)
                                            .frame(width: 18, height: 18)
                                            .frame(width: 38, height: 38)
                                            .background(Color.black.opacity(0.38))
                                            .clipShape(Circle())
                                    }

                                    // Three horizontal dashes -> Menu (Coming soon)
                                    Button {
                                        profileStore.showToast("Menu — Coming soon")
                                    } label: {
                                        MenuDashesSvgIcon(color: .white)
                                            .frame(width: 18, height: 18)
                                            .frame(width: 38, height: 38)
                                            .background(Color.black.opacity(0.38))
                                            .clipShape(Circle())
                                    }
                                } else {
                                    Button(action: presentInviteShare) {
                                        InviteTBadgeSvgIcon(badgeColor: accentCrimson)
                                            .frame(width: 38, height: 38)
                                    }
                                    Menu {
                                        Button("Copy link") {
                                            UIPasteboard.general.string = profileStore.shareProfileURL().absoluteString
                                            profileStore.showToast("Link copied")
                                        }
                                        Button("Invite friends") { presentInviteShare() }
                                        if profile.isMuted {
                                            Button("Unmute") { Task { await profileStore.unmuteUser(settingsStore: settingsStore) } }
                                        } else {
                                            Button("Mute") { Task { await profileStore.muteUser(settingsStore: settingsStore) } }
                                        }
                                        if profile.isBlocked {
                                            Button("Unblock") { Task { await profileStore.unblockUser() } }
                                        } else {
                                            Button("Block", role: .destructive) { Task { await profileStore.blockUser() } }
                                        }
                                        Button("Report", role: .destructive) { showReportCategorySheet = true }
                                    } label: {
                                        Image(systemName: "ellipsis")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(.white)
                                            .frame(width: 38, height: 38)
                                            .background(Color.black.opacity(0.38))
                                            .clipShape(Circle())
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 10)

                            Spacer()

                            if profile.isOwner {
                                HStack {
                                    Spacer()
                                    Button {
                                        showBackgroundsSheet = true
                                    } label: {
                                        HStack(spacing: 6) {
                                            CameraBgSvgIcon(color: .white)
                                                .frame(width: 15, height: 15)
                                            Text(bgs.isEmpty ? "Edit background" : "Edit background (\(bgs.count)/5)")
                                                .font(.system(size: 12.5, weight: .semibold))
                                                .foregroundColor(.white)
                                        }
                                        .padding(.horizontal, 13)
                                        .frame(height: 34)
                                        .background(Color.black.opacity(0.56))
                                        .clipShape(Capsule())
                                        .overlay(Capsule().stroke(Color.white.opacity(0.28), lineWidth: 1))
                                    }
                                }
                                .padding(.horizontal, 14)
                                .padding(.bottom, 64)
                            }
                        }
                    }
                    .frame(height: 232)
                    .clipped()

                    // Overlapping gradient-ringed Avatar matching Chrome screenshot
                    avatarRingView(profile: profile)
                        .offset(y: 40)
                }
                .padding(.bottom, 44)
            } else {
                avatarRingView(profile: profile)
                    .padding(.top, 6)
            }

            Text("@\(profile.username.isEmpty ? profile.userId : profile.username)")
                .font(.system(size: 15.5, weight: .bold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                .lineLimit(1)
                .padding(.top, 8)

            // Stats Row: Following | Followers | Likes
            HStack(spacing: 0) {
                statButton(
                    count: formatTelefamProfileMetricCount(profile.followingCount),
                    label: "Following"
                ) {
                    if !profile.isOwner && profile.followingVisibility.lowercased() == "only_me" {
                        profileStore.showToast("Following list is private")
                    } else {
                        showConnectionsSheet = true
                        Task { await profileStore.openConnections(isFollowers: false) }
                    }
                }

                Rectangle()
                    .fill(TelefamPalette.border(colorScheme))
                    .frame(width: 1, height: 26)

                statButton(
                    count: formatTelefamProfileMetricCount(profile.followersCount),
                    label: "Followers"
                ) {
                    showConnectionsSheet = true
                    Task { await profileStore.openConnections(isFollowers: true) }
                }

                Rectangle()
                    .fill(TelefamPalette.border(colorScheme))
                    .frame(width: 1, height: 26)

                VStack(spacing: 2) {
                    Text(formatTelefamProfileMetricCount(profile.likesCount))
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    Text("Likes")
                        .font(.system(size: 12.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                }
                .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: 360)
            .padding(.horizontal, 24)
            .padding(.top, 14)

            // Action Buttons Row
            if profile.isBlocked {
                VStack(spacing: 8) {
                    Text("You blocked this account.")
                        .font(.system(size: 13.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    Button {
                        Task { await profileStore.unblockUser() }
                    } label: {
                        Text("Unblock")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(accentCrimson)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
                .frame(maxWidth: 380)
                .padding(.horizontal, 20)
                .padding(.top, 16)
            } else if !profile.isOwner {
                HStack(spacing: 8) {
                    followActionButton(state: profile.relationship)

                    Button {
                        onMessageUser?(profile)
                    } label: {
                        HStack(spacing: 6) {
                            PaperPlaneProfileSvgIcon(color: TelefamPalette.textPrimary(colorScheme))
                                .frame(width: 15, height: 15)
                            Text("Message")
                                .font(.system(size: 13.5, weight: .semibold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                    }

                    Button {
                        profileStore.showToast("Subscribe — Coming soon")
                    } label: {
                        HStack(spacing: 6) {
                            SubscribeCrownSvgIcon(color: .white)
                                .frame(width: 15, height: 15)
                            Text("Subscribe")
                                .font(.system(size: 13.5, weight: .semibold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(accentCrimson)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
                .frame(maxWidth: 460)
                .padding(.horizontal, 18)
                .padding(.top, 16)
            } else {
                // Owner Actions matching Chrome reference: [Pencil Edit profile] + [Share nodes Share profile]
                HStack(spacing: 10) {
                    Button { showEditProfileSheet = true } label: {
                        HStack(spacing: 7) {
                            EditPencilSvgIcon(color: .white)
                                .frame(width: 15, height: 15)
                            Text("Edit profile")
                                .font(.system(size: 13.5, weight: .semibold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 42)
                        .background(accentCrimson)
                        .clipShape(RoundedRectangle(cornerRadius: 21))
                    }
                    Button { presentInviteShare() } label: {
                        HStack(spacing: 7) {
                            ShareNodesSvgIcon(color: TelefamPalette.textPrimary(colorScheme))
                                .frame(width: 15, height: 15)
                            Text("Share profile")
                                .font(.system(size: 13.5, weight: .semibold))
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 42)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 21))
                        .overlay(RoundedRectangle(cornerRadius: 21).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                    }
                }
                .frame(maxWidth: 380)
                .padding(.horizontal, 24)
                .padding(.top, 16)
            }

            if !profile.bio.isEmpty {
                SmartInteractiveTextView(
                    text: profile.bio,
                    source: "profile",
                    font: .system(size: 13.5),
                    textColor: TelefamPalette.textPrimary(colorScheme),
                    linkColor: accentCrimson,
                    textAlignment: .center,
                    showOtpPills: false
                )
                .lineSpacing(3)
                .frame(maxWidth: 420)
                .padding(.horizontal, 28)
                .padding(.top, 14)
            }

            if !profile.location.isEmpty {
                HStack(spacing: 4) {
                    LocationPinSvgIcon(color: accentCrimson)
                        .frame(width: 14, height: 14)
                    Text(profile.location)
                        .font(.system(size: 13))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .lineLimit(1)
                }
                .padding(.top, 8)
            }
        }
    }

    private func avatarRingView(profile: TelefamIosUserProfile) -> some View {
        ZStack {
            Circle()
                .stroke(
                    LinearGradient(
                        colors: [
                            Color(hex: 0xFF7A00),
                            Color(hex: 0xE61E62),
                            Color(hex: 0x9C27B0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 2.8
                )
                .frame(width: 98, height: 98)

            if !profile.avatarUrl.isEmpty {
                TelefamIosRemoteImage(
                    urlString: feedStore.authorizedURLString(profile.avatarUrl),
                    bearerToken: sessionStore.sessionToken,
                    deviceFingerprint: sessionStore.deviceFingerprint,
                    fallbackInitials: initials(from: profile)
                )
                .frame(width: 90, height: 90)
                .clipShape(Circle())
            } else {
                Circle()
                    .fill(TelefamPalette.blush(colorScheme))
                    .frame(width: 90, height: 90)
                    .overlay(
                        Text(initials(from: profile))
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(accentCrimson)
                    )
            }
        }
    }

    @ViewBuilder
    private func ownerTabIcon(tab: TelefamIosOwnerProfileTab, color: Color) -> some View {
        switch tab {
        case .posts:
            PostsGrid2x2SvgIcon(color: color)
        case .saved:
            BookmarkTabSvgIcon(color: color)
        case .reposts:
            RepostTabSvgIcon(color: color)
        case .locked:
            LockTabSvgIcon(color: color)
        }
    }

    private func statButton(count: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(count)
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Text(label)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
            }
            .frame(maxWidth: .infinity, minHeight: 44)
        }
        .buttonStyle(.plain)
    }

    private func followActionButton(state: TelefamRelationshipState) -> some View {
        let filled = state == .follow || state == .followBack
        return Button {
            profileStore.toggleFollow()
        } label: {
            HStack(spacing: 6) {
                if filled {
                    PersonAddSvgIcon(color: .white)
                        .frame(width: 15, height: 15)
                } else {
                    Image(systemName: state.systemIcon)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                }
                Text(state.label)
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundColor(filled ? .white : TelefamPalette.textPrimary(colorScheme))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 40)
            .background(filled ? accentCrimson : TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(filled ? Color.clear : TelefamPalette.border(colorScheme), lineWidth: 1)
            )
        }
    }

    private func initials(from p: TelefamIosUserProfile) -> String {
        let words = p.fullName.split(separator: " ")
        if words.count >= 2, let a = words.first?.first, let b = words.last?.first {
            return "\(a)\(b)".uppercased()
        }
        let source = p.fullName.isEmpty ? p.username : p.fullName
        return String(source.prefix(2)).uppercased()
    }

    private func presentInviteShare() {
        let link = profileStore.shareProfileURL().absoluteString
        let text = "Join me on Telefam! \(link)"
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
            .first?
            .present(av, animated: true)
    }
}

private struct ProfilePostGridCell: View {
    let post: TelefamIosFeedItem
    let isOwner: Bool
    @ObservedObject var feedStore: TelefamIosFeedStore
    let accentCrimson: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                Color(red: 0.08, green: 0.09, blue: 0.13)

                if !post.thumbnailUrl.isEmpty {
                    TelefamIosRemoteImage(
                        urlString: feedStore.authorizedURLString(post.thumbnailUrl),
                        bearerToken: feedStore.bearerToken,
                        deviceFingerprint: feedStore.deviceFingerprint,
                        fallbackInitials: ""
                    )
                } else if !post.photoUrls.isEmpty {
                    TelefamIosRemoteImage(
                        urlString: feedStore.authorizedURLString(post.photoUrls[0]),
                        bearerToken: feedStore.bearerToken,
                        deviceFingerprint: feedStore.deviceFingerprint,
                        fallbackInitials: ""
                    )
                } else {
                    Text(post.textContent.isEmpty ? post.caption : post.textContent)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(4)
                        .padding(10)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(accentCrimson.opacity(0.82))
                }

                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.62)],
                    startPoint: .center,
                    endPoint: .bottom
                )

                if isOwner && post.privacy.lowercased() != "public" && !post.privacy.isEmpty {
                    VStack {
                        HStack {
                            Spacer()
                            Group {
                                if post.privacy.lowercased() == "private" {
                                    LockTabSvgIcon(color: .white)
                                        .frame(width: 12, height: 12)
                                } else {
                                    FriendsPrivacySvgIcon(color: .white)
                                        .frame(width: 12, height: 12)
                                }
                            }
                            .padding(5)
                            .background(Color.black.opacity(0.62))
                            .clipShape(Circle())
                            .padding(6)
                        }
                        Spacer()
                    }
                }

                HStack(spacing: 4) {
                    PlayOutlineSvgIcon()
                        .frame(width: 13, height: 13)
                    Text(formatTelefamProfileMetricCount(post.viewsCount))
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 7)
                .padding(.vertical, 6)
            }
            .aspectRatio(3.0 / 4.0, contentMode: .fit)
            .clipped()
        }
        .buttonStyle(.plain)
    }
}

private struct ProfileConnectionsSheetView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    let title: String
    @ObservedObject var profileStore: TelefamIosProfileStore
    @ObservedObject var contactsStore: TelefamContactsStore
    let onSelectUser: (String) -> Void

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }

    var body: some View {
        NavigationStack {
            Group {
                if profileStore.isLoadingConnections && profileStore.connections.isEmpty {
                    VStack {
                        Spacer()
                        ProgressView().tint(accentCrimson)
                        Spacer()
                    }
                } else if profileStore.connections.isEmpty {
                    VStack(spacing: 8) {
                        Spacer()
                        Text("No people found")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Spacer()
                    }
                } else {
                    List(profileStore.connections) { c in
                        HStack(spacing: 12) {
                            HStack(spacing: 12) {
                                if let av = c.avatarUrl, !av.isEmpty {
                                    TelefamIosRemoteImage(
                                        urlString: contactsStore.resolvedURLString(av),
                                        bearerToken: contactsStore.currentToken,
                                        deviceFingerprint: contactsStore.currentDeviceFingerprint,
                                        fallbackInitials: c.initials
                                    )
                                    .frame(width: 44, height: 44)
                                    .clipShape(Circle())
                                } else {
                                    Circle()
                                        .fill(TelefamPalette.blush(colorScheme))
                                        .frame(width: 44, height: 44)
                                        .overlay(
                                            Text(c.initials)
                                                .font(.system(size: 14, weight: .bold))
                                                .foregroundColor(accentCrimson)
                                        )
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(c.fullName.isEmpty ? "@\(c.username)" : c.fullName)
                                        .font(.system(size: 14.5, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                        .lineLimit(1)
                                    Text("@\(c.username)")
                                        .font(.system(size: 12.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        .lineLimit(1)
                                }
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { onSelectUser(c.userId) }

                            Spacer()

                            let rel = c.relationship
                            let filled = rel == .follow || rel == .followBack
                            Button {
                                profileStore.toggleConnectionFollow(targetUserId: c.userId)
                            } label: {
                                Text(rel.label)
                                    .font(.system(size: 12.5, weight: .semibold))
                                    .foregroundColor(filled ? .white : TelefamPalette.textPrimary(colorScheme))
                                    .padding(.horizontal, 14)
                                    .frame(height: 34)
                                    .background(filled ? accentCrimson : TelefamPalette.surface(colorScheme))
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(filled ? Color.clear : TelefamPalette.border(colorScheme), lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                        }
                        .listRowBackground(TelefamPalette.background(colorScheme))
                        .onAppear {
                            if c.userId == profileStore.connections.last?.userId {
                                Task { await profileStore.loadMoreConnectionsIfNeeded() }
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .background(TelefamPalette.background(colorScheme))
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Custom SVG Vector Icons Matching Reference Screenshot (Zero Emojis)

private struct InviteTBadgeSvgIcon: View {
    let badgeColor: Color
    var body: some View {
        ZStack {
            Canvas { ctx, size in
                let w = size.width
                let h = size.height
                let box = CGRect(x: w * 0.18, y: h * 0.22, width: w * 0.64, height: h * 0.50)
                ctx.fill(Path(roundedRect: box, cornerRadius: w * 0.14), with: .color(badgeColor))
                var tail = Path()
                tail.move(to: CGPoint(x: w * 0.42, y: h * 0.70))
                tail.addLine(to: CGPoint(x: w * 0.50, y: h * 0.84))
                tail.addLine(to: CGPoint(x: w * 0.58, y: h * 0.70))
                tail.closeSubpath()
                ctx.fill(tail, with: .color(badgeColor))
            }
            Text("T")
                .font(.system(size: 12.5, weight: .heavy))
                .foregroundColor(.white)
                .offset(y: -1)
        }
        .frame(width: 34, height: 34)
    }
}

private struct PersonAddSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.20, y: h * 0.14, width: w * 0.34, height: h * 0.34)), with: .color(color), style: stroke)
            var arc = Path()
            arc.move(to: CGPoint(x: w * 0.10, y: h * 0.86))
            arc.addQuadCurve(to: CGPoint(x: w * 0.64, y: h * 0.86), control: CGPoint(x: w * 0.37, y: h * 0.56))
            ctx.stroke(arc, with: .color(color), style: stroke)
            var plus = Path()
            plus.move(to: CGPoint(x: w * 0.76, y: h * 0.30))
            plus.addLine(to: CGPoint(x: w * 0.76, y: h * 0.58))
            plus.move(to: CGPoint(x: w * 0.62, y: h * 0.44))
            plus.addLine(to: CGPoint(x: w * 0.90, y: h * 0.44))
            ctx.stroke(plus, with: .color(color), style: stroke)
        }
    }
}

private struct PaperPlaneProfileSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            var p = Path()
            p.move(to: CGPoint(x: w * 0.90, y: h * 0.12))
            p.addLine(to: CGPoint(x: w * 0.10, y: h * 0.44))
            p.addLine(to: CGPoint(x: w * 0.44, y: h * 0.58))
            p.addLine(to: CGPoint(x: w * 0.60, y: h * 0.90))
            p.closeSubpath()
            ctx.stroke(p, with: .color(color), style: stroke)
            var mid = Path()
            mid.move(to: CGPoint(x: w * 0.44, y: h * 0.58))
            mid.addLine(to: CGPoint(x: w * 0.90, y: h * 0.12))
            ctx.stroke(mid, with: .color(color), style: stroke)
        }
    }
}

private struct SubscribeCrownSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            var p = Path()
            p.move(to: CGPoint(x: w * 0.12, y: h * 0.30))
            p.addLine(to: CGPoint(x: w * 0.32, y: h * 0.52))
            p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.22))
            p.addLine(to: CGPoint(x: w * 0.68, y: h * 0.52))
            p.addLine(to: CGPoint(x: w * 0.88, y: h * 0.30))
            p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.80))
            p.addLine(to: CGPoint(x: w * 0.18, y: h * 0.80))
            p.closeSubpath()
            ctx.stroke(p, with: .color(color), style: stroke)
        }
    }
}

private struct LocationPinSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            var p = Path()
            p.move(to: CGPoint(x: w * 0.50, y: h * 0.92))
            p.addCurve(to: CGPoint(x: w * 0.16, y: h * 0.40), control1: CGPoint(x: w * 0.24, y: h * 0.66), control2: CGPoint(x: w * 0.16, y: h * 0.52))
            p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.10), control1: CGPoint(x: w * 0.16, y: h * 0.20), control2: CGPoint(x: w * 0.30, y: h * 0.10))
            p.addCurve(to: CGPoint(x: w * 0.84, y: h * 0.40), control1: CGPoint(x: w * 0.70, y: h * 0.10), control2: CGPoint(x: w * 0.84, y: h * 0.20))
            p.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.92), control1: CGPoint(x: w * 0.84, y: h * 0.52), control2: CGPoint(x: w * 0.76, y: h * 0.66))
            p.closeSubpath()
            ctx.stroke(p, with: .color(color), style: stroke)
            ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.38, y: h * 0.28, width: w * 0.24, height: h * 0.24)), with: .color(color), style: stroke)
        }
    }
}

private struct PostsGrid2x2SvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round)
            let boxW = w * 0.34
            let boxH = h * 0.34
            for (rx, ry) in [(0.12, 0.12), (0.54, 0.12), (0.12, 0.54), (0.54, 0.54)] {
                ctx.stroke(Path(roundedRect: CGRect(x: w * rx, y: h * ry, width: boxW, height: boxH), cornerRadius: w * 0.06), with: .color(color), style: stroke)
            }
        }
    }
}

private struct FilterSlidersSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width * 0.48
            let h = size.height * 0.48
            let ox = (size.width - w) / 2
            let oy = (size.height - h) / 2
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round)
            for (yFrac, knobX) in [(0.24, 0.35), (0.50, 0.68), (0.76, 0.42)] {
                var line = Path()
                line.move(to: CGPoint(x: ox + w * 0.10, y: oy + h * yFrac))
                line.addLine(to: CGPoint(x: ox + w * 0.90, y: oy + h * yFrac))
                ctx.stroke(line, with: .color(color), style: stroke)
                ctx.fill(Path(ellipseIn: CGRect(x: ox + w * knobX - 2.6, y: oy + h * yFrac - 2.6, width: 5.2, height: 5.2)), with: .color(color))
            }
        }
    }
}

private struct PlayOutlineSvgIcon: View {
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            var p = Path()
            p.move(to: CGPoint(x: w * 0.22, y: h * 0.14))
            p.addLine(to: CGPoint(x: w * 0.86, y: h * 0.50))
            p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.86))
            p.closeSubpath()
            ctx.stroke(p, with: .color(.white), style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
        }
    }
}

private struct GearProfileSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let cx = w * 0.5
            let cy = h * 0.5
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            ctx.stroke(Path(ellipseIn: CGRect(x: cx - w * 0.16, y: cy - h * 0.16, width: w * 0.32, height: h * 0.32)), with: .color(color), style: stroke)
            for i in 0..<8 {
                let a = Double(i) * (.pi / 4.0)
                var tooth = Path()
                tooth.move(to: CGPoint(x: cx + CGFloat(cos(a)) * w * 0.26, y: cy + CGFloat(sin(a)) * h * 0.26))
                tooth.addLine(to: CGPoint(x: cx + CGFloat(cos(a)) * w * 0.40, y: cy + CGFloat(sin(a)) * h * 0.40))
                ctx.stroke(tooth, with: .color(color), style: StrokeStyle(lineWidth: 2.0, lineCap: .round))
            }
        }
    }
}

private struct MenuDashesSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 2.0, lineCap: .round)
            for yFrac in [0.28, 0.50, 0.72] {
                var line = Path()
                line.move(to: CGPoint(x: w * 0.16, y: h * yFrac))
                line.addLine(to: CGPoint(x: w * 0.84, y: h * yFrac))
                ctx.stroke(line, with: .color(color), style: stroke)
            }
        }
    }
}

private struct CameraBgSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round)
            ctx.stroke(Path(roundedRect: CGRect(x: w * 0.10, y: h * 0.26, width: w * 0.80, height: h * 0.58), cornerRadius: w * 0.12), with: .color(color), style: stroke)
            var top = Path()
            top.move(to: CGPoint(x: w * 0.34, y: h * 0.26))
            top.addLine(to: CGPoint(x: w * 0.40, y: h * 0.14))
            top.addLine(to: CGPoint(x: w * 0.60, y: h * 0.14))
            top.addLine(to: CGPoint(x: w * 0.66, y: h * 0.26))
            ctx.stroke(top, with: .color(color), style: stroke)
            ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.34, y: h * 0.39, width: w * 0.32, height: h * 0.32)), with: .color(color), style: stroke)
        }
    }
}

private struct EditPencilSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            var p = Path()
            p.move(to: CGPoint(x: w * 0.16, y: h * 0.84))
            p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.62))
            p.addLine(to: CGPoint(x: w * 0.72, y: h * 0.12))
            p.addLine(to: CGPoint(x: w * 0.88, y: h * 0.28))
            p.addLine(to: CGPoint(x: w * 0.38, y: h * 0.78))
            p.closeSubpath()
            ctx.stroke(p, with: .color(color), style: stroke)
        }
    }
}

private struct ShareNodesSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.7, lineCap: .round, lineJoin: .round)
            var l1 = Path()
            l1.move(to: CGPoint(x: w * 0.28, y: h * 0.50))
            l1.addLine(to: CGPoint(x: w * 0.70, y: h * 0.24))
            var l2 = Path()
            l2.move(to: CGPoint(x: w * 0.28, y: h * 0.50))
            l2.addLine(to: CGPoint(x: w * 0.70, y: h * 0.76))
            ctx.stroke(l1, with: .color(color), style: stroke)
            ctx.stroke(l2, with: .color(color), style: stroke)
            for (cx, cy) in [(0.22, 0.50), (0.76, 0.20), (0.76, 0.80)] {
                ctx.fill(Path(ellipseIn: CGRect(x: w * cx - w * 0.10, y: h * cy - h * 0.10, width: w * 0.20, height: h * 0.20)), with: .color(color))
            }
        }
    }
}

private struct BookmarkTabSvgIcon: View {
    let color: Color
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
            ctx.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
        }
    }
}

private struct RepostTabSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round)
            var top = Path()
            top.move(to: CGPoint(x: w * 0.16, y: h * 0.36))
            top.addLine(to: CGPoint(x: w * 0.76, y: h * 0.36))
            top.addLine(to: CGPoint(x: w * 0.62, y: h * 0.20))
            var bot = Path()
            bot.move(to: CGPoint(x: w * 0.84, y: h * 0.64))
            bot.addLine(to: CGPoint(x: w * 0.24, y: h * 0.64))
            bot.addLine(to: CGPoint(x: w * 0.38, y: h * 0.80))
            ctx.stroke(top, with: .color(color), style: stroke)
            ctx.stroke(bot, with: .color(color), style: stroke)
        }
    }
}

private struct LockTabSvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round)
            ctx.stroke(Path(roundedRect: CGRect(x: w * 0.20, y: h * 0.44, width: w * 0.60, height: h * 0.44), cornerRadius: w * 0.10), with: .color(color), style: stroke)
            var shackle = Path()
            shackle.move(to: CGPoint(x: w * 0.32, y: h * 0.44))
            shackle.addLine(to: CGPoint(x: w * 0.32, y: h * 0.30))
            shackle.addCurve(to: CGPoint(x: w * 0.68, y: h * 0.30), control1: CGPoint(x: w * 0.32, y: h * 0.12), control2: CGPoint(x: w * 0.68, y: h * 0.12))
            shackle.addLine(to: CGPoint(x: w * 0.68, y: h * 0.44))
            ctx.stroke(shackle, with: .color(color), style: stroke)
        }
    }
}

private struct FriendsPrivacySvgIcon: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let stroke = StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round)
            ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.20, y: h * 0.18, width: w * 0.30, height: h * 0.30)), with: .color(color), style: stroke)
            var a1 = Path()
            a1.move(to: CGPoint(x: w * 0.12, y: h * 0.82))
            a1.addQuadCurve(to: CGPoint(x: w * 0.58, y: h * 0.82), control: CGPoint(x: w * 0.35, y: h * 0.54))
            ctx.stroke(a1, with: .color(color), style: stroke)
            ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.58, y: h * 0.24, width: w * 0.24, height: h * 0.24)), with: .color(color), style: stroke)
            var a2 = Path()
            a2.move(to: CGPoint(x: w * 0.62, y: h * 0.60))
            a2.addQuadCurve(to: CGPoint(x: w * 0.88, y: h * 0.82), control: CGPoint(x: w * 0.78, y: h * 0.58))
            ctx.stroke(a2, with: .color(color), style: stroke)
        }
    }
}

// MARK: - Edit Background Photos Sheet (Up to 5 photos, SHA-256 & dHash fingerprinted)

private struct EditBackgroundsSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @ObservedObject var sessionStore: TelefamIosSessionStore
    @ObservedObject var profileStore: TelefamIosProfileStore
    @ObservedObject var feedStore: TelefamIosFeedStore

    @State private var pickedItems: [PhotosPickerItem] = []
    @State private var isUploading: Bool = false
    @State private var errorMessage: String? = nil

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }

    var body: some View {
        let backgrounds = profileStore.profile.backgrounds
        let remainingSlots = max(0, 5 - backgrounds.count)

        NavigationStack {
            VStack(spacing: 14) {
                Text("Upload up to 5 photos. They fade smoothly and rotate every 5 seconds for all visitors.")
                    .font(.system(size: 13))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .frame(maxWidth: .infinity, alignment: .leading)

                if isUploading {
                    ProgressView().tint(accentCrimson)
                }

                if let err = errorMessage, !err.isEmpty {
                    Text(err)
                        .font(.system(size: 12.5, weight: .semibold))
                        .foregroundColor(accentCrimson)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if remainingSlots > 0 {
                    PhotosPicker(
                        selection: $pickedItems,
                        maxSelectionCount: remainingSlots,
                        matching: .images
                    ) {
                        HStack(spacing: 8) {
                            CameraBgSvgIcon(color: .white)
                                .frame(width: 17, height: 17)
                            Text("Add background photo (\(remainingSlots) left)")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(accentCrimson)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(isUploading)
                }

                if backgrounds.isEmpty {
                    Spacer()
                    Text("No background photos uploaded yet.")
                        .font(.system(size: 14))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    Spacer()
                } else {
                    List {
                        ForEach(Array(backgrounds.enumerated()), id: \.element.mediaId) { idx, bg in
                            HStack(spacing: 12) {
                                TelefamIosRemoteImage(
                                    urlString: feedStore.authorizedURLString(bg.url),
                                    bearerToken: sessionStore.sessionToken,
                                    deviceFingerprint: sessionStore.deviceFingerprint,
                                    fallbackInitials: ""
                                )
                                .frame(width: 82, height: 56)
                                .clipShape(RoundedRectangle(cornerRadius: 10))

                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Slide #\(idx + 1)")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    if bg.width > 0 && bg.height > 0 {
                                        Text("\(bg.width)x\(bg.height) · \(max(1, bg.byteSize / 1024)) KB")
                                            .font(.system(size: 11.5))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    }
                                    if !bg.sha256Fingerprint.isEmpty || !bg.dHashFingerprint.isEmpty {
                                        Text("SHA:\(bg.sha256Fingerprint.prefix(8)) dH:\(bg.dHashFingerprint.prefix(8))")
                                            .font(.system(size: 10, design: .monospaced))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                            .lineLimit(1)
                                    }
                                }

                                Spacer()

                                if idx > 0 {
                                    Button {
                                        var ids = backgrounds.map(\.mediaId)
                                        ids.swapAt(idx - 1, idx)
                                        Task { await reorder(ids) }
                                    } label: {
                                        Image(systemName: "arrow.up")
                                            .font(.system(size: 14, weight: .semibold))
                                            .frame(width: 30, height: 30)
                                    }
                                    .buttonStyle(.plain)
                                }

                                if idx < backgrounds.count - 1 {
                                    Button {
                                        var ids = backgrounds.map(\.mediaId)
                                        ids.swapAt(idx, idx + 1)
                                        Task { await reorder(ids) }
                                    } label: {
                                        Image(systemName: "arrow.down")
                                            .font(.system(size: 14, weight: .semibold))
                                            .frame(width: 30, height: 30)
                                    }
                                    .buttonStyle(.plain)
                                }

                                Button(role: .destructive) {
                                    Task {
                                        isUploading = true
                                        let res = await sessionStore.deleteProfileBackground(mediaId: bg.mediaId)
                                        isUploading = false
                                        if res.ok {
                                            profileStore.applyUpdatedBackgrounds(res.backgrounds)
                                        } else {
                                            errorMessage = res.error
                                        }
                                    }
                                } label: {
                                    Image(systemName: "trash")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(accentCrimson)
                                        .frame(width: 32, height: 32)
                                }
                                .buttonStyle(.plain)
                            }
                            .listRowBackground(TelefamPalette.surface(colorScheme))
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .padding(18)
            .background(TelefamPalette.background(colorScheme).ignoresSafeArea())
            .navigationTitle("Background photos (\(backgrounds.count)/5)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .onChange(of: pickedItems) { items in
                guard !items.isEmpty else { return }
                let allowed = Array(items.prefix(max(0, 5 - profileStore.profile.backgrounds.count)))
                pickedItems = []
                Task {
                    isUploading = true
                    errorMessage = nil
                    for item in allowed {
                        if let raw = try? await item.loadTransferable(type: Data.self),
                           let uiImg = UIImage(data: raw),
                           let jpeg = uiImg.jpegData(compressionQuality: 0.86) {
                            let res = await sessionStore.uploadProfileBackground(jpegData: jpeg)
                            if res.ok {
                                profileStore.applyUpdatedBackgrounds(res.backgrounds)
                            } else {
                                errorMessage = res.error
                                break
                            }
                        }
                    }
                    isUploading = false
                }
            }
        }
    }

    private func reorder(_ ids: [String]) async {
        isUploading = true
        let res = await sessionStore.reorderProfileBackgrounds(mediaIds: ids)
        isUploading = false
        if res.ok {
            profileStore.applyUpdatedBackgrounds(res.backgrounds)
        } else {
            errorMessage = res.error
        }
    }
}

// MARK: - Edit Profile Modal Sheet (14-day username lock + full editing + DP circular cropper)

private struct EditProfileSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @ObservedObject var sessionStore: TelefamIosSessionStore
    @ObservedObject var profileStore: TelefamIosProfileStore
    @ObservedObject var feedStore: TelefamIosFeedStore

    @State private var fullName: String = ""
    @State private var username: String = ""
    @State private var selectedCountry: TelefamIosCountryOption = TelefamIosCountryCatalog.defaultOption()
    @State private var phoneDigits: String = ""
    @State private var gender: String = "Prefer not to say"
    @State private var dobDate: Date = Calendar.current.date(byAdding: .year, value: -20, to: Date()) ?? Date()
    @State private var location: String = ""
    @State private var bio: String = ""
    @State private var avatarMediaId: String? = nil

    @State private var avatarPickerItem: PhotosPickerItem? = nil
    @State private var rawPickedImage: UIImage? = nil
    @State private var avatarPreview: UIImage? = nil
    @State private var cropOffset: CGSize = .zero
    @State private var cropScale: CGFloat = 1.0
    @State private var showCropSheet: Bool = false

    @State private var showCountrySheet: Bool = false
    @State private var countrySearch: String = ""
    @State private var showDobPickerSheet: Bool = false
    @State private var isSaving: Bool = false
    @State private var validationError: String? = nil

    private var accentCrimson: Color {
        colorScheme == .dark ? TelefamPalette.crimsonBright : TelefamPalette.crimson
    }

    private var isUsernameLocked: Bool {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let p = profileStore.profile
        let nextMs = p.nextUsernameChangeMs > 0
            ? p.nextUsernameChangeMs
            : (p.usernameChangedAtMs > 0 ? p.usernameChangedAtMs + (14 * 24 * 60 * 60 * 1000) : 0)
        return nextMs > nowMs
    }

    private var usernameLockText: String {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let p = profileStore.profile
        let nextMs = p.nextUsernameChangeMs > 0
            ? p.nextUsernameChangeMs
            : (p.usernameChangedAtMs > 0 ? p.usernameChangedAtMs + (14 * 24 * 60 * 60 * 1000) : 0)
        guard nextMs > nowMs else {
            return "Username can only be changed once every 2 weeks."
        }
        let remMs = nextMs - nowMs
        let days = remMs / (24 * 60 * 60 * 1000)
        let hours = (remMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        return days > 0
            ? "Locked for \(days)d \(hours)h (once per 2 weeks)"
            : "Locked for \(max(1, hours))h (once per 2 weeks)"
    }

    private var isoDobString: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: dobDate)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // Circular DP / Avatar Picker
                    VStack(spacing: 8) {
                        PhotosPicker(selection: $avatarPickerItem, matching: .images) {
                            ZStack(alignment: .bottomTrailing) {
                                if let img = avatarPreview {
                                    Image(uiImage: img)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 92, height: 92)
                                        .clipShape(Circle())
                                } else if !profileStore.profile.avatarUrl.isEmpty {
                                    TelefamIosRemoteImage(
                                        urlString: feedStore.authorizedURLString(profileStore.profile.avatarUrl),
                                        bearerToken: sessionStore.sessionToken,
                                        deviceFingerprint: sessionStore.deviceFingerprint,
                                        fallbackInitials: "DP"
                                    )
                                    .frame(width: 92, height: 92)
                                    .clipShape(Circle())
                                } else {
                                    Circle()
                                        .fill(TelefamPalette.blush(colorScheme))
                                        .frame(width: 92, height: 92)
                                        .overlay(
                                            CameraBgSvgIcon(color: accentCrimson)
                                                .frame(width: 28, height: 28)
                                        )
                                }

                                Circle()
                                    .fill(accentCrimson)
                                    .frame(width: 28, height: 28)
                                    .overlay(
                                        CameraBgSvgIcon(color: .white)
                                            .frame(width: 14, height: 14)
                                    )
                                    .overlay(Circle().stroke(TelefamPalette.background(colorScheme), lineWidth: 2))
                            }
                        }
                        Text("Change profile photo (DP)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(accentCrimson)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)

                    if let err = validationError, !err.isEmpty {
                        Text(err)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(accentCrimson)
                    }

                    editLabel("Full name *")
                    editField("Your full name", text: $fullName)

                    editLabel("Username *")
                    VStack(alignment: .leading, spacing: 4) {
                        TextField("username", text: $username)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .disabled(isUsernameLocked)
                            .padding(.horizontal, 12)
                            .frame(height: 48)
                            .background(TelefamPalette.surface(colorScheme).opacity(isUsernameLocked ? 0.55 : 1.0))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                        Text(usernameLockText)
                            .font(.system(size: 11.5, weight: isUsernameLocked ? .semibold : .regular))
                            .foregroundColor(isUsernameLocked ? accentCrimson : TelefamPalette.textMuted(colorScheme))
                    }

                    editLabel("Phone")
                    HStack(spacing: 10) {
                        Button { showCountrySheet = true } label: {
                            HStack(spacing: 4) {
                                Text("\(selectedCountry.flag) \(selectedCountry.dialCode)")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 11))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            }
                            .padding(.horizontal, 10)
                            .frame(height: 48)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                        }
                        TextField("Phone number", text: $phoneDigits)
                            .keyboardType(.phonePad)
                            .padding(.horizontal, 12)
                            .frame(height: 48)
                            .background(TelefamPalette.surface(colorScheme))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    }

                    editLabel("Gender")
                    Menu {
                        ForEach(["Female", "Male", "Non-binary", "Prefer not to say"], id: \.self) { g in
                            Button(g) { gender = g }
                        }
                    } label: {
                        HStack {
                            Text(gender)
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Spacer()
                            Image(systemName: "chevron.down")
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                        .padding(.horizontal, 12)
                        .frame(height: 48)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    }

                    editLabel("Date of birth")
                    Button { showDobPickerSheet = true } label: {
                        HStack {
                            Text(isoDobString)
                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Spacer()
                            Image(systemName: "calendar")
                                .foregroundColor(accentCrimson)
                        }
                        .padding(.horizontal, 12)
                        .frame(height: 48)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    }

                    editLabel("Location *")
                    editField("City, Country", text: $location)

                    editLabel("Bio")
                    TextEditor(text: $bio)
                        .frame(minHeight: 88)
                        .padding(8)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

                    Button {
                        saveChanges()
                    } label: {
                        HStack {
                            if isSaving { ProgressView().tint(.white) }
                            Text(isSaving ? "Saving..." : "Save changes")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(accentCrimson)
                        .clipShape(Capsule())
                    }
                    .disabled(isSaving)
                    .padding(.top, 8)
                }
                .padding(20)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
            .background(TelefamPalette.background(colorScheme).ignoresSafeArea())
            .navigationTitle("Edit profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { saveChanges() }
                        .disabled(isSaving)
                        .foregroundColor(accentCrimson)
                }
            }
            .onAppear {
                let p = profileStore.profile
                fullName = p.fullName.isEmpty ? sessionStore.fullName : p.fullName
                username = p.username.isEmpty ? sessionStore.username : p.username
                location = p.location.isEmpty ? sessionStore.location : p.location
                bio = p.bio.isEmpty ? sessionStore.bio : p.bio
                if !p.gender.isEmpty { gender = p.gender }
                if let match = TelefamIosCountryCatalog.all.first(where: { $0.isoCode.caseInsensitiveCompare(p.countryIso) == .orderedSame }) {
                    selectedCountry = match
                }
                let rawPhone = p.phoneNumber.isEmpty ? sessionStore.phoneDisplay : p.phoneNumber
                let cleanDigits = rawPhone.filter(\.isNumber)
                let dialDigits = selectedCountry.dialCode.filter(\.isNumber)
                if !dialDigits.isEmpty && cleanDigits.hasPrefix(dialDigits) && cleanDigits.count > dialDigits.count {
                    phoneDigits = String(cleanDigits.dropFirst(dialDigits.count))
                } else {
                    phoneDigits = cleanDigits
                }
                let f = DateFormatter()
                f.dateFormat = "yyyy-MM-dd"
                f.locale = Locale(identifier: "en_US_POSIX")
                if let parsed = f.date(from: p.dateOfBirth) {
                    dobDate = parsed
                }
            }
            .onChange(of: avatarPickerItem) { item in
                guard let item else { return }
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) {
                        rawPickedImage = img
                        cropOffset = .zero
                        cropScale = 1.0
                        showCropSheet = true
                    }
                }
            }
            .sheet(isPresented: $showCropSheet) {
                NavigationStack {
                    VStack(spacing: 18) {
                        if let img = rawPickedImage {
                            ZStack {
                                Color.black.opacity(0.88)
                                Image(uiImage: img)
                                    .resizable()
                                    .scaledToFill()
                                    .scaleEffect(cropScale)
                                    .offset(cropOffset)
                                    .gesture(DragGesture().onChanged { cropOffset = $0.translation })
                                    .gesture(MagnificationGesture().onChanged { cropScale = max(1.0, min(3.0, $0)) })
                                    .frame(width: 240, height: 240)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            }
                            .frame(height: 300)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        Button("Use Photo") {
                            guard let img = rawPickedImage, let data = img.jpegData(compressionQuality: 0.84) else {
                                showCropSheet = false
                                return
                            }
                            avatarPreview = img
                            showCropSheet = false
                            Task {
                                let cx = Float(0.5 - (cropOffset.width / 480.0))
                                let cy = Float(0.5 - (cropOffset.height / 480.0))
                                avatarMediaId = await sessionStore.uploadAvatar(
                                    jpegData: data,
                                    cx: max(0.0, min(1.0, cx)),
                                    cy: max(0.0, min(1.0, cy)),
                                    scale: Float(cropScale)
                                )
                            }
                        }
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(accentCrimson)
                        .clipShape(Capsule())
                    }
                    .padding(20)
                    .navigationTitle("Crop Photo")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showCropSheet = false }
                        }
                    }
                }
            }
            .sheet(isPresented: $showCountrySheet) {
                NavigationStack {
                    VStack {
                        TextField("Search country or dial code", text: $countrySearch)
                            .padding(10)
                            .overlay(Capsule().stroke(accentCrimson, lineWidth: 1.5))
                            .padding(.horizontal, 16)
                        List(TelefamIosCountryCatalog.all.filter {
                            countrySearch.isEmpty ||
                            $0.name.localizedCaseInsensitiveContains(countrySearch) ||
                            $0.dialCode.contains(countrySearch)
                        }) { option in
                            Button {
                                selectedCountry = option
                                showCountrySheet = false
                            } label: {
                                HStack {
                                    Text(option.flag)
                                    Text(option.name).foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Spacer()
                                    Text(option.dialCode).foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                            }
                        }
                    }
                    .navigationTitle("Select Country")
                    .navigationBarTitleDisplayMode(.inline)
                }
            }
            .sheet(isPresented: $showDobPickerSheet) {
                NavigationStack {
                    VStack {
                        DatePicker("Date of Birth", selection: $dobDate, in: ...Date(), displayedComponents: .date)
                            .datePickerStyle(.graphical)
                            .tint(accentCrimson)
                        Button("OK") { showDobPickerSheet = false }
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(accentCrimson)
                            .clipShape(Capsule())
                            .padding(.horizontal, 16)
                    }
                    .padding(16)
                    .navigationTitle("Date of Birth")
                    .navigationBarTitleDisplayMode(.inline)
                }
            }
        }
    }

    private func saveChanges() {
        let cleanName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUser = username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let cleanLoc = location.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanBio = bio.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanName.count >= 2 else {
            validationError = "Enter your full name."
            return
        }
        let validUsernameRegex = try? NSRegularExpression(pattern: "^[a-z0-9._]{3,24}$")
        let range = NSRange(cleanUser.startIndex..<cleanUser.endIndex, in: cleanUser)
        guard validUsernameRegex?.firstMatch(in: cleanUser, range: range) != nil else {
            validationError = "Username must be 3-24 characters (letters, numbers, . or _)."
            return
        }
        let age = Calendar.current.dateComponents([.year], from: dobDate, to: Date()).year ?? 0
        guard age >= 13 else {
            validationError = "You must be at least 13 years old."
            return
        }
        guard !cleanLoc.isEmpty else {
            validationError = "Enter your location."
            return
        }
        let phoneE164 = phoneDigits.isEmpty ? "" : "\(selectedCountry.dialCode)\(phoneDigits.filter(\.isNumber))"
        validationError = nil
        isSaving = true
        Task {
            let res = await sessionStore.updateProfile(
                fullName: cleanName,
                username: cleanUser,
                phoneE164: phoneE164,
                countryIso: selectedCountry.isoCode,
                gender: gender,
                dateOfBirthIso: isoDobString,
                location: cleanLoc,
                bio: cleanBio,
                avatarMediaId: avatarMediaId
            )
            isSaving = false
            if res.ok {
                await profileStore.fetchProfile()
                dismiss()
                profileStore.showToast("Profile updated")
            } else {
                validationError = res.error
            }
        }
    }

    private func editLabel(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
    }

    private func editField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
    }
}
