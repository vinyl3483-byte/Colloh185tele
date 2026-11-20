import SwiftUI
import PhotosUI
import AVFoundation
import CryptoKit
import Network

enum TelefamIosPostMode: String, CaseIterable {
    case text, photos, video

    var title: String {
        switch self {
        case .text: return "Text"
        case .photos: return "Photos"
        case .video: return "Video"
        }
    }

    var systemIcon: String {
        switch self {
        case .text: return "textformat"
        case .photos: return "photo.on.rectangle"
        case .video: return "film"
        }
    }
}

struct TelefamIosPostTheme: Identifiable {
    let id: String
    let label: String
    let isScene: Bool
    let startHex: UInt
    let endHex: UInt
    let accentHex: UInt
    let isLightInk: Bool

    static let all: [TelefamIosPostTheme] = [
        .init(id: "crimson", label: "Crimson", isScene: false, startHex: 0xB8080D, endHex: 0xE31B23, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "coral", label: "Coral", isScene: false, startHex: 0xFF6A45, endHex: 0xFFB057, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "ocean", label: "Ocean", isScene: false, startHex: 0x0077CC, endHex: 0x1BB8F5, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "emerald", label: "Emerald", isScene: false, startHex: 0x0D7A4E, endHex: 0x28BA77, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "violet", label: "Violet", isScene: false, startHex: 0x6B2CD4, endHex: 0x9E5DF2, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "obsidian", label: "Obsidian", isScene: false, startHex: 0x141821, endHex: 0x262D3B, accentHex: 0xFFFFFF, isLightInk: true),
        .init(id: "cream", label: "Cream", isScene: false, startHex: 0xFAF3E0, endHex: 0xF2E6CA, accentHex: 0x1E222A, isLightInk: false),
        .init(id: "blush", label: "Blush", isScene: false, startHex: 0xFFAEC0, endHex: 0xFFCFD9, accentHex: 0x2A1419, isLightInk: false),
        .init(id: "scene_aurora", label: "Aurora", isScene: true, startHex: 0x0A1628, endHex: 0x163D4E, accentHex: 0x42E6B5, isLightInk: true),
        .init(id: "scene_crimson_silk", label: "Silk", isScene: true, startHex: 0x7B0509, endHex: 0xD91820, accentHex: 0xFF6B6B, isLightInk: true),
        .init(id: "scene_dunes", label: "Dunes", isScene: true, startHex: 0x7F2A22, endHex: 0xF28E4B, accentHex: 0xFFD180, isLightInk: true),
        .init(id: "scene_waves", label: "Waves", isScene: true, startHex: 0x0B3C5D, endHex: 0x1D7898, accentHex: 0x7FD8EA, isLightInk: true),
        .init(id: "scene_stars", label: "Stars", isScene: true, startHex: 0x0D1021, endHex: 0x232946, accentHex: 0xEEC643, isLightInk: true),
        .init(id: "scene_botanical", label: "Botanical", isScene: true, startHex: 0x3E1826, endHex: 0x7C273F, accentHex: 0xF7A1B5, isLightInk: true),
        .init(id: "scene_paper", label: "Paper", isScene: true, startHex: 0xF5EFE2, endHex: 0xEAE0CE, accentHex: 0xC8B89E, isLightInk: false),
    ]
}

enum TelefamIosPostFont: String, CaseIterable {
    case classic, serif, hand, typewriter, bold, mono, display, rounded, italic

    var label: String {
        rawValue.capitalized
    }

    var badge: String {
        self == .bold ? "AA" : "Aa"
    }

    var swiftFont: Font {
        switch self {
        case .classic: return .system(size: 25, weight: .bold)
        case .serif: return .system(size: 26, weight: .bold, design: .serif)
        case .hand: return .custom("SnellRoundhand-Bold", size: 29)
        case .typewriter: return .system(size: 22, weight: .bold, design: .monospaced)
        case .bold: return .system(size: 26, weight: .heavy)
        case .mono: return .system(size: 21, weight: .bold, design: .monospaced)
        case .display: return .system(size: 28, weight: .black)
        case .rounded: return .system(size: 26, weight: .bold, design: .rounded)
        case .italic: return .system(size: 24, weight: .medium, design: .serif).italic()
        }
    }
}

struct TelefamIosSongTrack: Identifiable, Codable {
    let trackId: String
    let title: String
    let artist: String
    let album: String
    let artworkUrl: String
    let previewUrl: String
    let durationMs: Int64
    let source: String

    var id: String { trackId }

    enum CodingKeys: String, CodingKey {
        case trackId = "track_id"
        case title, artist, album
        case artworkUrl = "artwork_url"
        case previewUrl = "preview_url"
        case durationMs = "duration_ms"
        case source
    }
}

struct TelefamIosHashtagItem: Identifiable, Codable {
    let tag: String
    let postCount: Int64
    var id: String { tag }

    enum CodingKeys: String, CodingKey {
        case tag
        case postCount = "post_count"
    }
}

struct TelefamIosVideoFrame: Identifiable {
    let index: Int
    let image: UIImage
    let dHashHex: String
    var id: Int { index }
}

@MainActor
final class TelefamIosPostStore: ObservableObject {
    @Published var mode: TelefamIosPostMode = .text
    @Published var textContent: String = ""
    @Published var caption: String = ""
    @Published var selectedThemeId: String = "crimson"
    @Published var selectedFont: TelefamIosPostFont = .classic
    @Published var selectedPhotos: [UIImage] = []
    @Published var videoURL: URL? = nil
    @Published var videoFrames: [TelefamIosVideoFrame] = []
    @Published var selectedFrameIndex: Int = 0
    @Published var videoDurationSec: Double = 0
    @Published var trimStartSec: Double = 0
    @Published var trimEndSec: Double = 0
    @Published var muteOriginalAudio: Bool = false
    @Published var contentId: String = ""

    @Published var selectedSong: TelefamIosSongTrack? = nil
    @Published var taggedUserIds: Set<String> = []
    @Published var selectedHashtags: [String] = []

    @Published var songResults: [TelefamIosSongTrack] = []
    @Published var hashtagResults: [TelefamIosHashtagItem] = []
    @Published var isSearchingSongs: Bool = false
    @Published var uploadStatusLabel: String = ""
    @Published var isUploading: Bool = false
    @Published var errorMessage: String? = nil

    private var previewPlayer: AVPlayer? = nil
    @Published var playingTrackId: String? = nil
    @Published var editingPostId: String? = nil
    @Published var uploadNamePrefix: String = "telefam_media"

    private let baseURL: URL
    private let tokenProvider: () -> String
    private let deviceFingerprint: String
    private var bearerToken: String { tokenProvider() }
    private let pathMonitor = NWPathMonitor()

    private struct QueuedPostPayload: Codable {
        let clientOpId: String
        let payloadJsonData: Data
    }
    private var queuedPosts: [QueuedPostPayload] = []

    private var queueFileURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("telefam_ios_pending_posts.json")
    }

    init(
        baseURL: URL,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String,
        editingPostId: String? = nil,
        initialMode: TelefamIosPostMode = .text,
        initialText: String = "",
        initialCaption: String = "",
        initialThemeId: String = "crimson",
        initialFont: TelefamIosPostFont = .classic,
        initialSong: TelefamIosSongTrack? = nil,
        initialHashtags: [String] = [],
        initialMuteOriginalAudio: Bool = false
    ) {
        self.baseURL = baseURL
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint
        self.editingPostId = editingPostId
        self.mode = initialMode
        self.textContent = initialText
        self.caption = initialCaption
        self.selectedThemeId = initialThemeId.isEmpty ? "crimson" : initialThemeId
        self.selectedFont = initialFont
        self.selectedSong = initialSong
        self.selectedHashtags = initialHashtags
        self.muteOriginalAudio = initialMuteOriginalAudio
        restoreQueue()
        startNetworkMonitor()
    }

    deinit {
        pathMonitor.cancel()
    }

    private func restoreQueue() {
        if let d = try? Data(contentsOf: queueFileURL),
           let decoded = try? JSONDecoder().decode([QueuedPostPayload].self, from: d) {
            queuedPosts = decoded
        }
    }

    private func persistQueue() {
        if let d = try? JSONEncoder().encode(queuedPosts) {
            try? d.write(to: queueFileURL, options: .atomic)
        }
    }

    private func startNetworkMonitor() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            guard path.status == .satisfied else { return }
            Task { @MainActor [weak self] in
                await self?.flushQueuedPosts()
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "telefam.ios.post.queue"))
    }

    func flushQueuedPosts() async {
        guard !queuedPosts.isEmpty, !bearerToken.isEmpty else { return }
        var remaining: [QueuedPostPayload] = []
        for item in queuedPosts {
            var req = URLRequest(url: baseURL.appendingPathComponent("v1/posts"))
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
            req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
            req.httpBody = item.payloadJsonData
            if let (_, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
                continue
            }
            remaining.append(item)
        }
        queuedPosts = remaining
        persistQueue()
    }

    private func sanitizedUploadBaseName() -> String {
        let filtered = uploadNamePrefix.lowercased().filter { $0.isLetter || $0.isNumber || $0 == "_" || $0 == "-" }
        return filtered.isEmpty ? "telefam_media" : String(filtered.prefix(32))
    }

    var canShare: Bool {
        guard !isUploading else { return false }
        switch mode {
        case .text: return !textContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .photos: return !selectedPhotos.isEmpty || editingPostId != nil
        case .video: return (videoURL != nil && trimEndSec > trimStartSec) || editingPostId != nil
        }
    }

    func prepareVideo(url: URL) async {
        do {
            let values = try url.resourceValues(forKeys: [.fileSizeKey])
            if let fileSize = values.fileSize, Int64(fileSize) > 200 * 1024 * 1024 {
                errorMessage = "Video exceeds 200 MB limit"
                return
            }
            let asset = AVURLAsset(url: url)
            let duration = try await asset.load(.duration)
            let sec = max(1.0, CMTimeGetSeconds(duration))
            let generator = AVAssetImageGenerator(asset: asset)
            generator.appliesPreferredTrackTransform = true
            generator.maximumSize = CGSize(width: 480, height: 480)

            var extracted: [TelefamIosVideoFrame] = []
            for idx in 0..<8 {
                let t = CMTime(seconds: (sec * Double(idx)) / 7.0, preferredTimescale: 600)
                if let cg = try? generator.copyCGImage(at: t, actualTime: nil) {
                    let ui = UIImage(cgImage: cg)
                    let hash = Self.computeDHashHex(cgImage: cg)
                    extracted.append(TelefamIosVideoFrame(index: idx, image: ui, dHashHex: hash))
                }
            }
            videoURL = url
            videoFrames = extracted
            selectedFrameIndex = min(1, max(0, extracted.count - 1))
            videoDurationSec = sec
            trimStartSec = 0
            trimEndSec = sec
            recomputeContentId()
            errorMessage = nil
        } catch {
            errorMessage = "Unable to inspect selected video"
        }
    }

    func recomputeContentId() {
        let hashes = videoFrames.map { $0.dHashHex }.joined(separator: ",")
        let payload = "tf_vid_fp_v1|\(hashes)|dur=\(Int64(videoDurationSec * 1000))|trim=\(Int64(trimStartSec * 1000)):\(Int64(trimEndSec * 1000))"
        let digest = SHA256.hash(data: Data(payload.utf8))
        contentId = "vid_" + digest.prefix(16).map { String(format: "%02x", $0) }.joined()
    }

    func searchSongs(query: String) async {
        isSearchingSongs = true
        defer { isSearchingSongs = false }
        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/songs/search"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [URLQueryItem(name: "q", value: query)]
        guard let url = comps?.url else { return }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        struct Wrapper: Codable { let success: Bool; let items: [TelefamIosSongTrack] }
        if let (data, _) = try? await URLSession.shared.data(for: req),
           let parsed = try? JSONDecoder().decode(Wrapper.self, from: data),
           parsed.success {
            songResults = parsed.items
        }
    }

    func searchHashtags(query: String) async {
        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/hashtags/search"), resolvingAgainstBaseURL: false)
        comps?.queryItems = [URLQueryItem(name: "q", value: query)]
        guard let url = comps?.url else { return }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        struct Wrapper: Codable { let success: Bool; let items: [TelefamIosHashtagItem] }
        if let (data, _) = try? await URLSession.shared.data(for: req),
           let parsed = try? JSONDecoder().decode(Wrapper.self, from: data),
           parsed.success {
            hashtagResults = parsed.items
        }
    }

    private var previewEndObserver: NSObjectProtocol? = nil

    func togglePreview(track: TelefamIosSongTrack) {
        if playingTrackId == track.trackId {
            stopPreview()
            return
        }
        guard let url = URL(string: track.previewUrl) else { return }
        stopPreview()
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        let item = AVPlayerItem(url: url)
        let player = AVPlayer(playerItem: item)
        previewEndObserver = NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: item, queue: .main) { [weak self] _ in
            Task { @MainActor in
                self?.playingTrackId = nil
            }
        }
        previewPlayer = player
        playingTrackId = track.trackId
        player.play()
    }

    func stopPreview() {
        if let obs = previewEndObserver {
            NotificationCenter.default.removeObserver(obs)
            previewEndObserver = nil
        }
        previewPlayer?.pause()
        previewPlayer = nil
        playingTrackId = nil
    }

    func sharePost(onFinished: () -> Void) async {
        guard canShare else { return }
        stopPreview()
        isUploading = true
        uploadStatusLabel = "Uploading"
        defer { isUploading = false }

        if let editId = editingPostId {
            var patchReq = URLRequest(url: baseURL.appendingPathComponent("v1/posts/\(editId)"))
            patchReq.httpMethod = "PATCH"
            patchReq.setValue("application/json", forHTTPHeaderField: "Content-Type")
            patchReq.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
            patchReq.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

            var patchPayload: [String: Any] = [
                "text_content": textContent,
                "caption": caption,
                "theme_id": selectedThemeId,
                "font_id": selectedFont.rawValue,
                "tagged_user_ids": Array(taggedUserIds),
                "hashtags": selectedHashtags,
                "mute_original_audio": muteOriginalAudio
            ]
            if let song = selectedSong {
                patchPayload["song"] = [
                    "track_id": song.trackId,
                    "title": song.title,
                    "artist": song.artist,
                    "album": song.album,
                    "artwork_url": song.artworkUrl,
                    "preview_url": song.previewUrl,
                    "duration_ms": song.durationMs,
                    "source": song.source
                ]
            }
            patchReq.httpBody = try? JSONSerialization.data(withJSONObject: patchPayload)
            if let (_, resp) = try? await URLSession.shared.data(for: patchReq),
               let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
                uploadStatusLabel = "Uploaded"
                onFinished()
            } else {
                errorMessage = "An error has occurred."
                uploadStatusLabel = ""
            }
            return
        }

        var photoMediaIDs: [String] = []
        var videoMediaID = ""
        var thumbMediaID = ""
        var resolvedContentID = contentId

        if mode == .photos {
            for (idx, image) in selectedPhotos.prefix(10).enumerated() {
                uploadStatusLabel = "Uploading \(Int((Double(idx + 1) / Double(max(1, selectedPhotos.count))) * 85.0))%"
                guard let jpegData = image.jpegData(compressionQuality: 0.78) else { continue }
                var mReq = URLRequest(url: baseURL.appendingPathComponent("v1/media/photo"))
                mReq.httpMethod = "POST"
                mReq.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
                mReq.setValue("\(sanitizedUploadBaseName())_\(idx + 1).jpg", forHTTPHeaderField: "X-Telefam-Upload-Name")
                mReq.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
                mReq.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
                mReq.httpBody = jpegData
                if let (data, resp) = try? await URLSession.shared.data(for: mReq),
                   let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode),
                   let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let mid = obj["media_id"] as? String {
                    photoMediaIDs.append(mid)
                }
            }
            guard !photoMediaIDs.isEmpty else {
                errorMessage = "An error has occurred."
                uploadStatusLabel = ""
                return
            }
        } else if mode == .video, let vURL = videoURL, let videoData = try? Data(contentsOf: vURL) {
            uploadStatusLabel = "Uploading"
            let boundary = "Boundary-\(UUID().uuidString)"
            var vComps = URLComponents(url: baseURL.appendingPathComponent("v1/media/video"), resolvingAgainstBaseURL: false)
            vComps?.queryItems = [
                URLQueryItem(name: "duration_ms", value: "\(Int64(videoDurationSec * 1000))"),
                URLQueryItem(name: "trim_start_ms", value: "\(Int64(trimStartSec * 1000))"),
                URLQueryItem(name: "trim_end_ms", value: "\(Int64(trimEndSec * 1000))")
            ]
            guard let uploadURL = vComps?.url else { return }
            var vReq = URLRequest(url: uploadURL)
            vReq.httpMethod = "POST"
            vReq.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
            vReq.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
            vReq.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
            vReq.setValue(videoFrames.map { $0.dHashHex }.joined(separator: ","), forHTTPHeaderField: "X-Telefam-Frame-Hashes")

            let safeBase = sanitizedUploadBaseName()
            var body = Data()
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"video\"; filename=\"\(safeBase).mp4\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: video/mp4\r\n\r\n".data(using: .utf8)!)
            body.append(videoData)
            body.append("\r\n".data(using: .utf8)!)
            if let thumbData = videoFrames[safe: selectedFrameIndex]?.image.jpegData(compressionQuality: 0.78) {
                body.append("--\(boundary)\r\n".data(using: .utf8)!)
                body.append("Content-Disposition: form-data; name=\"thumbnail\"; filename=\"\(safeBase)_thumb.jpg\"\r\n".data(using: .utf8)!)
                body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
                body.append(thumbData)
                body.append("\r\n".data(using: .utf8)!)
            }
            body.append("--\(boundary)--\r\n".data(using: .utf8)!)
            vReq.httpBody = body

            if let (data, resp) = try? await URLSession.shared.data(for: vReq),
               let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode),
               let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let vid = obj["media_id"] as? String {
                videoMediaID = vid
                thumbMediaID = (obj["thumbnail_media_id"] as? String) ?? ""
                if let cid = obj["content_id"] as? String, !cid.isEmpty {
                    resolvedContentID = cid
                }
            } else {
                errorMessage = "An error has occurred."
                uploadStatusLabel = ""
                return
            }
        }

        var req = URLRequest(url: baseURL.appendingPathComponent("v1/posts"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")

        let opId = "ios_post_\(UUID().uuidString)"
        var payload: [String: Any] = [
            "client_op_id": opId,
            "mode": mode.rawValue,
            "text_content": textContent,
            "caption": caption,
            "theme_id": selectedThemeId,
            "font_id": selectedFont.rawValue,
            "tagged_user_ids": Array(taggedUserIds),
            "hashtags": selectedHashtags,
            "photo_media_ids": photoMediaIDs,
            "video_media_id": videoMediaID,
            "thumbnail_media_id": thumbMediaID,
            "trim_start_ms": Int64(trimStartSec * 1000),
            "trim_end_ms": Int64(trimEndSec * 1000),
            "mute_original_audio": muteOriginalAudio,
            "content_id": resolvedContentID
        ]
        if let song = selectedSong {
            payload["song"] = [
                "track_id": song.trackId,
                "title": song.title,
                "artist": song.artist,
                "album": song.album,
                "artwork_url": song.artworkUrl,
                "preview_url": song.previewUrl,
                "duration_ms": song.durationMs,
                "source": song.source
            ]
        }
        let bodyData = try? JSONSerialization.data(withJSONObject: payload)
        req.httpBody = bodyData
        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) {
            uploadStatusLabel = "Uploaded"
            onFinished()
        } else {
            if let bodyData {
                queuedPosts.append(QueuedPostPayload(clientOpId: opId, payloadJsonData: bodyData))
                persistQueue()
            }
            uploadStatusLabel = "Queued"
            onFinished()
        }
    }

    private static func computeDHashHex(cgImage: CGImage) -> String {
        let colorSpace = CGColorSpaceCreateDeviceGray()
        var pixels = [UInt8](repeating: 0, count: 9 * 8)
        guard let ctx = CGContext(
            data: &pixels,
            width: 9,
            height: 8,
            bitsPerComponent: 8,
            bytesPerRow: 9,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        ) else {
            return String(repeating: "0", count: 16)
        }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: 9, height: 8))
        var hash: UInt64 = 0
        var bit = 0
        for y in 0..<8 {
            for x in 0..<8 {
                if pixels[y * 9 + x] > pixels[y * 9 + x + 1] {
                    hash |= (1 << bit)
                }
                bit += 1
            }
        }
        return String(format: "%016llx", hash)
    }
}

struct TelefamIosPostView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: TelefamIosPostStore
    @ObservedObject var contactsStore: TelefamContactsStore
    @State private var photoPickerItems: [PhotosPickerItem] = []
    @State private var videoPickerItem: PhotosPickerItem? = nil
    @State private var showTagSheet: Bool = false
    @State private var showSongSheet: Bool = false
    @State private var showHashtagSheet: Bool = false
    @State private var sheetSearch: String = ""

    var body: some View {
        GeometryReader { geo in
            let isWide = geo.size.width >= 680
            VStack(spacing: 0) {
                headerBar
                ScrollView {
                    if isWide {
                        HStack(alignment: .top, spacing: 20) {
                            primaryStage.frame(maxWidth: .infinity)
                            controlsStage.frame(maxWidth: .infinity)
                        }
                        .padding(20)
                        .frame(maxWidth: 1040)
                    } else {
                        VStack(spacing: 16) {
                            primaryStage
                            controlsStage
                        }
                        .padding(16)
                    }
                }
                .background(TelefamPalette.background(colorScheme))
            }
            .background(TelefamPalette.crimson.ignoresSafeArea(edges: .top))
        }
        .sheet(isPresented: $showTagSheet) { tagPeopleSheet }
        .sheet(isPresented: $showSongSheet) { songSearchSheet }
        .sheet(isPresented: $showHashtagSheet) { hashtagsSheet }
        .onChange(of: photoPickerItems) { items in
            Task {
                var loaded: [UIImage] = []
                for item in items.prefix(10) {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) {
                        loaded.append(img)
                    }
                }
                store.selectedPhotos = Array((store.selectedPhotos + loaded).prefix(10))
            }
        }
        .onChange(of: videoPickerItem) { item in
            guard let item else { return }
            Task {
                if let movieData = try? await item.loadTransferable(type: Data.self) {
                    let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).mp4")
                    try? movieData.write(to: tempURL)
                    await store.prepareVideo(url: tempURL)
                }
            }
        }
    }

    private var headerBar: some View {
        VStack(spacing: 10) {
            HStack {
                Button {
                    store.stopPreview()
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                }
                Text(store.editingPostId != nil ? "Edit post" : "New post")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                Button {
                    Task { await store.sharePost { dismiss() } }
                } label: {
                    Text(store.isUploading ? store.uploadStatusLabel : (store.editingPostId != nil ? "Save" : "Share"))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(TelefamPalette.crimson.opacity(store.canShare ? 1.0 : 0.55))
                        .padding(.horizontal, 18)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(store.canShare ? 1.0 : 0.72))
                        .clipShape(Capsule())
                }
                .disabled(!store.canShare)
            }
            if store.editingPostId == nil {
                HStack(spacing: 8) {
                    ForEach(TelefamIosPostMode.allCases, id: \.self) { m in
                        let active = store.mode == m
                        Button {
                            store.mode = m
                        } label: {
                            Label(m.title, systemImage: m.systemIcon)
                                .font(.system(size: 13.5, weight: active ? .bold : .medium))
                                .foregroundColor(active ? TelefamPalette.crimson : .white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(active ? Color.white : Color.clear)
                                .clipShape(Capsule())
                        }
                    }
                    Spacer()
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(TelefamPalette.crimson)
    }

    @ViewBuilder
    private var primaryStage: some View {
        let theme = TelefamIosPostTheme.all.first { $0.id == store.selectedThemeId } ?? TelefamIosPostTheme.all[0]
        switch store.mode {
        case .text:
            ZStack(alignment: .bottomTrailing) {
                ZStack {
                    LinearGradient(
                        colors: [Color(hex: theme.startHex), Color(hex: theme.endHex)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    if theme.isScene {
                        themeSceneArt(theme)
                    }
                }
                .frame(height: 310)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .overlay {
                    TextField("What's on your mind?", text: $store.textContent, axis: .vertical)
                        .font(store.selectedFont.swiftFont)
                        .foregroundColor(theme.isLightInk ? .white : Color(hex: 0x1C1F26))
                        .multilineTextAlignment(.center)
                        .padding(24)
                        .onChange(of: store.textContent) { val in
                            if val.count > 280 { store.textContent = String(val.prefix(280)) }
                        }
                }
                Text("\(store.textContent.count)/280")
                    .font(.system(size: 12))
                    .foregroundColor((theme.isLightInk ? Color.white : Color.black).opacity(0.78))
                    .padding(14)
            }
        case .photos:
            if store.selectedPhotos.isEmpty {
                PhotosPicker(selection: $photoPickerItems, maxSelectionCount: 10, matching: .images) {
                    dashedCard(icon: "photo.badge.plus", title: "Add photos", subtitle: "Up to 10")
                }
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(Array(store.selectedPhotos.enumerated()), id: \.offset) { idx, uiImg in
                            ZStack(alignment: .topTrailing) {
                                Image(uiImage: uiImg)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 140, height: 140)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                Button {
                                    store.selectedPhotos.remove(at: idx)
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.white)
                                        .padding(6)
                                }
                            }
                        }
                    }
                    .padding(12)
                }
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
        case .video:
            if store.videoURL == nil {
                PhotosPicker(selection: $videoPickerItem, matching: .videos) {
                    dashedCard(icon: "film", title: "Add video", subtitle: "Up to 200 MB")
                }
            } else {
                VStack(spacing: 12) {
                    if let thumb = store.videoFrames[safe: store.selectedFrameIndex]?.image {
                        Image(uiImage: thumb)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 200)
                            .frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(store.videoFrames) { frame in
                                Image(uiImage: frame.image)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 54, height: 64)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(frame.index == store.selectedFrameIndex ? TelefamPalette.crimson : .clear, lineWidth: 2.5)
                                    )
                                    .onTapGesture { store.selectedFrameIndex = frame.index }
                            }
                        }
                    }
                    VStack(spacing: 6) {
                        HStack {
                            Text(String(format: "%.1fs", store.trimStartSec))
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            Spacer()
                            Text(String(format: "%.1fs", store.trimEndSec))
                                .font(.system(size: 12))
                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                        Slider(
                            value: Binding(
                                get: { store.trimStartSec },
                                set: { v in
                                    store.trimStartSec = min(v, max(0, store.trimEndSec - 0.5))
                                    store.recomputeContentId()
                                }
                            ),
                            in: 0...max(1.0, store.videoDurationSec)
                        )
                        .tint(TelefamPalette.crimson)
                        Slider(
                            value: Binding(
                                get: { store.trimEndSec },
                                set: { v in
                                    store.trimEndSec = max(v, min(store.videoDurationSec, store.trimStartSec + 0.5))
                                    store.recomputeContentId()
                                }
                            ),
                            in: 0...max(1.0, store.videoDurationSec)
                        )
                        .tint(TelefamPalette.crimson)
                    }
                    Toggle("Mute original audio", isOn: $store.muteOriginalAudio)
                        .tint(TelefamPalette.crimson)
                }
                .padding(14)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
        }
    }

    @ViewBuilder
    private var controlsStage: some View {
        VStack(spacing: 14) {
            if store.mode == .text {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(TelefamIosPostTheme.all) { t in
                            Circle()
                                .fill(LinearGradient(colors: [Color(hex: t.startHex), Color(hex: t.endHex)], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 38, height: 38)
                                .overlay {
                                    if t.id == store.selectedThemeId {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 13, weight: .bold))
                                            .foregroundColor(t.isLightInk ? .white : .black)
                                    }
                                }
                                .onTapGesture { store.selectedThemeId = t.id }
                        }
                    }
                }
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 98), spacing: 8)], spacing: 8) {
                    ForEach(TelefamIosPostFont.allCases, id: \.self) { f in
                        let sel = f == store.selectedFont
                        Button { store.selectedFont = f } label: {
                            HStack(spacing: 5) {
                                Text(f.badge).font(.system(size: 13, weight: .bold))
                                Text(f.label).font(.system(size: 13, weight: sel ? .semibold : .medium))
                            }
                            .foregroundColor(sel ? TelefamPalette.crimson : TelefamPalette.textPrimary(colorScheme))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 7)
                            .background(sel ? TelefamPalette.blush(colorScheme) : TelefamPalette.surface(colorScheme))
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(sel ? TelefamPalette.crimson : TelefamPalette.border(colorScheme), lineWidth: 1))
                        }
                    }
                }
            } else {
                ZStack(alignment: .bottomTrailing) {
                    TextField("Write a caption...", text: $store.caption, axis: .vertical)
                        .padding(14)
                        .padding(.bottom, 22)
                        .frame(minHeight: 108, alignment: .topLeading)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                        .onChange(of: store.caption) { v in
                            if v.count > 2200 { store.caption = String(v.prefix(2200)) }
                        }
                    Text("\(store.caption.count)/2200")
                        .font(.system(size: 11.5))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .padding(12)
                }

                if store.editingPostId == nil {
                    HStack(spacing: 8) {
                        Image(systemName: "doc.text")
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.crimson)
                        TextField("Upload filename", text: $store.uploadNamePrefix)
                            .font(.system(size: 13.5))
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                    }
                    .padding(.horizontal, 12)
                    .frame(height: 44)
                    .background(TelefamPalette.surface(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                }
            }

            VStack(spacing: 0) {
                metadataRow(icon: "music.note", title: store.selectedSong?.title ?? "Song", summary: store.selectedSong?.artist ?? "Add") {
                    sheetSearch = ""
                    showSongSheet = true
                    Task { await store.searchSongs(query: "") }
                }
                Divider()
                metadataRow(icon: "person.badge.plus", title: "Tag people", summary: store.taggedUserIds.isEmpty ? "Add" : "\(store.taggedUserIds.count)") {
                    sheetSearch = ""
                    showTagSheet = true
                }
                Divider()
                metadataRow(icon: "number", title: "Hashtags", summary: store.selectedHashtags.isEmpty ? "Add" : store.selectedHashtags.map { "#\($0)" }.joined(separator: " ")) {
                    sheetSearch = ""
                    showHashtagSheet = true
                    Task { await store.searchHashtags(query: "") }
                }
            }
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
        }
    }

    private func dashedCard(icon: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(TelefamPalette.crimson)
                .frame(width: 56, height: 56)
                .background(TelefamPalette.blush(colorScheme))
                .clipShape(Circle())
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
            Text(subtitle)
                .font(.system(size: 13))
                .foregroundColor(TelefamPalette.textMuted(colorScheme))
        }
        .frame(maxWidth: .infinity)
        .frame(height: 300)
        .background(TelefamPalette.surface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .stroke(style: StrokeStyle(lineWidth: 1.5, dash: [7, 6]))
                .foregroundColor(TelefamPalette.border(colorScheme))
        )
    }

    private func metadataRow(icon: String, title: String, summary: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(TelefamPalette.crimson)
                    .frame(width: 34, height: 34)
                    .background(TelefamPalette.blush(colorScheme))
                    .clipShape(Circle())
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Spacer()
                Text(summary)
                    .font(.system(size: 13.5))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .lineLimit(1)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
    }

    private var tagPeopleSheet: some View {
        NavigationStack {
            VStack(spacing: 12) {
                TextField("Search people", text: $sheetSearch)
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .overlay(Capsule().stroke(TelefamPalette.crimson, lineWidth: 1.5))
                    .padding(.horizontal, 16)
                List(contactsStore.allCached.filter {
                    sheetSearch.isEmpty ||
                    $0.fullName.localizedCaseInsensitiveContains(sheetSearch) ||
                    $0.username.localizedCaseInsensitiveContains(sheetSearch)
                }) { person in
                    let sel = store.taggedUserIds.contains(person.userId)
                    HStack {
                        VStack(alignment: .leading) {
                            Text(person.fullName).font(.system(size: 15, weight: .bold))
                            Text("@\(person.username)").font(.system(size: 13)).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: sel ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(sel ? TelefamPalette.crimson : .secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if sel { store.taggedUserIds.remove(person.userId) } else { store.taggedUserIds.insert(person.userId) }
                    }
                }
                Button("Done") { showTagSheet = false }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(TelefamPalette.crimson)
                    .clipShape(Capsule())
                    .padding(.horizontal, 16)
            }
            .navigationTitle("Tag people")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var songSearchSheet: some View {
        NavigationStack {
            VStack(spacing: 12) {
                TextField("Search songs", text: $sheetSearch)
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .overlay(Capsule().stroke(TelefamPalette.crimson, lineWidth: 1.5))
                    .padding(.horizontal, 16)
                    .onChange(of: sheetSearch) { q in Task { await store.searchSongs(query: q) } }
                List(store.songResults) { track in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(track.title).font(.system(size: 15, weight: .bold))
                            Text(track.artist).font(.system(size: 13)).foregroundColor(.secondary)
                        }
                        Spacer()
                        if !track.previewUrl.isEmpty {
                            Button {
                                store.togglePreview(track: track)
                            } label: {
                                Image(systemName: store.playingTrackId == track.trackId ? "pause.fill" : "play.fill")
                                    .foregroundColor(TelefamPalette.crimson)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        store.selectedSong = track
                        store.stopPreview()
                        showSongSheet = false
                    }
                }
            }
            .navigationTitle("Song")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var hashtagsSheet: some View {
        NavigationStack {
            VStack(spacing: 12) {
                TextField("Search hashtags", text: $sheetSearch)
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .overlay(Capsule().stroke(TelefamPalette.crimson, lineWidth: 1.5))
                    .padding(.horizontal, 16)
                    .onChange(of: sheetSearch) { q in Task { await store.searchHashtags(query: q) } }
                List(store.hashtagResults) { item in
                    let sel = store.selectedHashtags.contains(item.tag)
                    HStack {
                        VStack(alignment: .leading) {
                            Text("#\(item.tag)").font(.system(size: 15, weight: .bold))
                            Text("\(item.postCount) posts").font(.system(size: 12.5)).foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: sel ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(sel ? TelefamPalette.crimson : .secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if sel {
                            store.selectedHashtags.removeAll { $0 == item.tag }
                        } else {
                            store.selectedHashtags.append(item.tag)
                        }
                    }
                }
                Button("Done") { showHashtagSheet = false }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(TelefamPalette.crimson)
                    .clipShape(Capsule())
                    .padding(.horizontal, 16)
            }
            .navigationTitle("Hashtags")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func themeSceneArt(_ theme: TelefamIosPostTheme) -> some View {
        Canvas { ctx, size in
            let w = size.width
            let h = size.height
            let accent = Color(hex: theme.accentHex)
            var wave = Path()
            wave.move(to: CGPoint(x: 0, y: h * 0.72))
            wave.addCurve(
                to: CGPoint(x: w, y: h * 0.56),
                control1: CGPoint(x: w * 0.34, y: h * 0.45),
                control2: CGPoint(x: w * 0.68, y: h * 0.88)
            )
            wave.addLine(to: CGPoint(x: w, y: h))
            wave.addLine(to: CGPoint(x: 0, y: h))
            wave.closeSubpath()
            ctx.fill(wave, with: .color(accent.opacity(0.22)))

            ctx.fill(
                Path(ellipseIn: CGRect(x: w * 0.68, y: h * 0.12, width: w * 0.36, height: w * 0.36)),
                with: .color(accent.opacity(0.14))
            )
        }
    }
}

private extension Array {
    subscript(safe idx: Int) -> Element? {
        indices.contains(idx) ? self[idx] : nil
    }
}
