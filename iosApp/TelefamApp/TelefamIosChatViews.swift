import ImageIO
import SwiftUI
import UIKit

// MARK: - Custom Vector SVG-Style Icons (Zero Emojis)

private struct ChatSvgIcon: View {
    enum Kind {
        case back, search, phone, video, moreVertical, smiley, gift, paperclip
        case mic, send, play, pause, clock, checkSingle, checkDouble, shield
        case eyeOnce, archive, mute, trash, markUnread, block, forward, copy
        case edit, undo, close, lock, missedCall
    }

    let kind: Kind
    var color: Color = .primary
    var size: CGFloat = 20

    var body: some View {
        Canvas { ctx, canvasSize in
            let w = canvasSize.width
            let h = canvasSize.height
            let stroke = StrokeStyle(lineWidth: max(1.6, w * 0.085), lineCap: .round, lineJoin: .round)

            switch kind {
            case .back:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.5))
                p.addLine(to: CGPoint(x: w * 0.24, y: h * 0.5))
                p.move(to: CGPoint(x: w * 0.48, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.5))
                p.addLine(to: CGPoint(x: w * 0.48, y: h * 0.76))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .search:
                ctx.stroke(
                    Path(ellipseIn: CGRect(x: w * 0.14, y: h * 0.14, width: w * 0.52, height: h * 0.52)),
                    with: .color(color),
                    style: stroke
                )
                var handle = Path()
                handle.move(to: CGPoint(x: w * 0.60, y: h * 0.60))
                handle.addLine(to: CGPoint(x: w * 0.86, y: h * 0.86))
                ctx.stroke(handle, with: .color(color), style: stroke)

            case .phone:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addQuadCurve(to: CGPoint(x: w * 0.78, y: h * 0.78), control: CGPoint(x: w * 0.26, y: h * 0.74))
                p.addLine(to: CGPoint(x: w * 0.84, y: h * 0.65))
                p.addLine(to: CGPoint(x: w * 0.66, y: h * 0.57))
                p.addLine(to: CGPoint(x: w * 0.58, y: h * 0.63))
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.35, y: h * 0.16))
                p.addLine(to: CGPoint(x: w * 0.43, y: h * 0.34))
                p.addLine(to: CGPoint(x: w * 0.37, y: h * 0.42))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .video:
                ctx.stroke(
                    Path(roundedRect: CGRect(x: w * 0.10, y: h * 0.26, width: w * 0.56, height: h * 0.48), cornerRadius: w * 0.10),
                    with: .color(color),
                    style: stroke
                )
                var lens = Path()
                lens.move(to: CGPoint(x: w * 0.66, y: h * 0.42))
                lens.addLine(to: CGPoint(x: w * 0.90, y: h * 0.28))
                lens.addLine(to: CGPoint(x: w * 0.90, y: h * 0.72))
                lens.addLine(to: CGPoint(x: w * 0.66, y: h * 0.58))
                lens.closeSubpath()
                ctx.stroke(lens, with: .color(color), style: stroke)

            case .moreVertical:
                for cy in [0.24, 0.50, 0.76] {
                    ctx.fill(
                        Path(ellipseIn: CGRect(x: w * 0.43, y: h * cy - w * 0.07, width: w * 0.14, height: w * 0.14)),
                        with: .color(color)
                    )
                }

            case .smiley:
                ctx.stroke(
                    Path(ellipseIn: CGRect(x: w * 0.10, y: h * 0.10, width: w * 0.80, height: h * 0.80)),
                    with: .color(color),
                    style: stroke
                )
                ctx.fill(Path(ellipseIn: CGRect(x: w * 0.31, y: h * 0.36, width: w * 0.09, height: w * 0.09)), with: .color(color))
                ctx.fill(Path(ellipseIn: CGRect(x: w * 0.60, y: h * 0.36, width: w * 0.09, height: w * 0.09)), with: .color(color))
                var smile = Path()
                smile.move(to: CGPoint(x: w * 0.32, y: h * 0.58))
                smile.addQuadCurve(to: CGPoint(x: w * 0.68, y: h * 0.58), control: CGPoint(x: w * 0.50, y: h * 0.76))
                ctx.stroke(smile, with: .color(color), style: stroke)

            case .gift:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.16, y: h * 0.42, width: w * 0.68, height: h * 0.44), cornerRadius: w * 0.06), with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.12, y: h * 0.28, width: w * 0.76, height: h * 0.14), cornerRadius: w * 0.04), with: .color(color), style: stroke)
                var ribbon = Path()
                ribbon.move(to: CGPoint(x: w * 0.50, y: h * 0.28))
                ribbon.addLine(to: CGPoint(x: w * 0.50, y: h * 0.86))
                ribbon.move(to: CGPoint(x: w * 0.50, y: h * 0.28))
                ribbon.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.28), control1: CGPoint(x: w * 0.26, y: h * 0.08), control2: CGPoint(x: w * 0.38, y: h * 0.28))
                ribbon.addCurve(to: CGPoint(x: w * 0.50, y: h * 0.28), control1: CGPoint(x: w * 0.62, y: h * 0.28), control2: CGPoint(x: w * 0.74, y: h * 0.08))
                ctx.stroke(ribbon, with: .color(color), style: stroke)

            case .paperclip:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.68, y: h * 0.30))
                p.addLine(to: CGPoint(x: w * 0.32, y: h * 0.66))
                p.addQuadCurve(to: CGPoint(x: w * 0.48, y: h * 0.82), control: CGPoint(x: w * 0.20, y: h * 0.80))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.52))
                p.addQuadCurve(to: CGPoint(x: w * 0.54, y: h * 0.20), control: CGPoint(x: w * 0.90, y: h * 0.36))
                p.addLine(to: CGPoint(x: w * 0.26, y: h * 0.48))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .mic:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.36, y: h * 0.12, width: w * 0.28, height: h * 0.48), cornerRadius: w * 0.14), with: .color(color), style: stroke)
                var arc = Path()
                arc.move(to: CGPoint(x: w * 0.24, y: h * 0.46))
                arc.addQuadCurve(to: CGPoint(x: w * 0.76, y: h * 0.46), control: CGPoint(x: w * 0.50, y: h * 0.82))
                arc.move(to: CGPoint(x: w * 0.50, y: h * 0.68))
                arc.addLine(to: CGPoint(x: w * 0.50, y: h * 0.86))
                arc.move(to: CGPoint(x: w * 0.36, y: h * 0.86))
                arc.addLine(to: CGPoint(x: w * 0.64, y: h * 0.86))
                ctx.stroke(arc, with: .color(color), style: stroke)

            case .send:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.14, y: h * 0.20))
                p.addLine(to: CGPoint(x: w * 0.88, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.14, y: h * 0.80))
                p.addLine(to: CGPoint(x: w * 0.26, y: h * 0.52))
                p.addLine(to: CGPoint(x: w * 0.58, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.26, y: h * 0.48))
                p.closeSubpath()
                ctx.fill(p, with: .color(color))

            case .play:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.28, y: h * 0.20))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.28, y: h * 0.80))
                p.closeSubpath()
                ctx.fill(p, with: .color(color))

            case .pause:
                ctx.fill(Path(roundedRect: CGRect(x: w * 0.24, y: h * 0.22, width: w * 0.18, height: h * 0.56), cornerRadius: 2), with: .color(color))
                ctx.fill(Path(roundedRect: CGRect(x: w * 0.58, y: h * 0.22, width: w * 0.18, height: h * 0.56), cornerRadius: 2), with: .color(color))

            case .clock:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.14, y: h * 0.14, width: w * 0.72, height: h * 0.72)), with: .color(color), style: stroke)
                var hnd = Path()
                hnd.move(to: CGPoint(x: w * 0.50, y: h * 0.28))
                hnd.addLine(to: CGPoint(x: w * 0.50, y: h * 0.52))
                hnd.addLine(to: CGPoint(x: w * 0.66, y: h * 0.60))
                ctx.stroke(hnd, with: .color(color), style: stroke)

            case .checkSingle:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.20, y: h * 0.54))
                p.addLine(to: CGPoint(x: w * 0.42, y: h * 0.74))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.28))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .checkDouble:
                var p1 = Path()
                p1.move(to: CGPoint(x: w * 0.10, y: h * 0.54))
                p1.addLine(to: CGPoint(x: w * 0.30, y: h * 0.74))
                p1.addLine(to: CGPoint(x: w * 0.64, y: h * 0.28))
                var p2 = Path()
                p2.move(to: CGPoint(x: w * 0.42, y: h * 0.66))
                p2.addLine(to: CGPoint(x: w * 0.52, y: h * 0.74))
                p2.addLine(to: CGPoint(x: w * 0.90, y: h * 0.28))
                ctx.stroke(p1, with: .color(color), style: stroke)
                ctx.stroke(p2, with: .color(color), style: stroke)

            case .shield:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.50, y: h * 0.12))
                p.addLine(to: CGPoint(x: w * 0.84, y: h * 0.24))
                p.addQuadCurve(to: CGPoint(x: w * 0.50, y: h * 0.88), control: CGPoint(x: w * 0.82, y: h * 0.68))
                p.addQuadCurve(to: CGPoint(x: w * 0.16, y: h * 0.24), control: CGPoint(x: w * 0.18, y: h * 0.68))
                p.closeSubpath()
                ctx.stroke(p, with: .color(color), style: stroke)

            case .eyeOnce:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.12, y: h * 0.12, width: w * 0.76, height: h * 0.76)), with: .color(color), style: StrokeStyle(lineWidth: max(1.6, w * 0.085), dash: [w * 0.16, w * 0.10]))
                var one = Path()
                one.move(to: CGPoint(x: w * 0.44, y: h * 0.36))
                one.addLine(to: CGPoint(x: w * 0.52, y: h * 0.30))
                one.addLine(to: CGPoint(x: w * 0.52, y: h * 0.70))
                ctx.stroke(one, with: .color(color), style: stroke)

            case .archive:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.12, y: h * 0.18, width: w * 0.76, height: h * 0.18), cornerRadius: 3), with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.18, y: h * 0.36, width: w * 0.64, height: h * 0.46), cornerRadius: 3), with: .color(color), style: stroke)
                var bar = Path()
                bar.move(to: CGPoint(x: w * 0.38, y: h * 0.54))
                bar.addLine(to: CGPoint(x: w * 0.62, y: h * 0.54))
                ctx.stroke(bar, with: .color(color), style: stroke)

            case .mute:
                var spk = Path()
                spk.move(to: CGPoint(x: w * 0.16, y: h * 0.40))
                spk.addLine(to: CGPoint(x: w * 0.34, y: h * 0.40))
                spk.addLine(to: CGPoint(x: w * 0.54, y: h * 0.22))
                spk.addLine(to: CGPoint(x: w * 0.54, y: h * 0.78))
                spk.addLine(to: CGPoint(x: w * 0.34, y: h * 0.60))
                spk.addLine(to: CGPoint(x: w * 0.16, y: h * 0.60))
                spk.closeSubpath()
                ctx.stroke(spk, with: .color(color), style: stroke)
                var slash = Path()
                slash.move(to: CGPoint(x: w * 0.20, y: h * 0.80))
                slash.addLine(to: CGPoint(x: w * 0.82, y: h * 0.20))
                ctx.stroke(slash, with: .color(color), style: stroke)

            case .trash:
                var top = Path()
                top.move(to: CGPoint(x: w * 0.18, y: h * 0.28))
                top.addLine(to: CGPoint(x: w * 0.82, y: h * 0.28))
                top.move(to: CGPoint(x: w * 0.38, y: h * 0.28))
                top.addLine(to: CGPoint(x: w * 0.38, y: h * 0.16))
                top.addLine(to: CGPoint(x: w * 0.62, y: h * 0.16))
                top.addLine(to: CGPoint(x: w * 0.62, y: h * 0.28))
                ctx.stroke(top, with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.24, y: h * 0.28, width: w * 0.52, height: h * 0.56), cornerRadius: 3), with: .color(color), style: stroke)

            case .markUnread:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.14, y: h * 0.22, width: w * 0.72, height: h * 0.56), cornerRadius: w * 0.12), with: .color(color), style: stroke)
                ctx.fill(Path(ellipseIn: CGRect(x: w * 0.68, y: h * 0.14, width: w * 0.22, height: w * 0.22)), with: .color(TelefamPalette.crimson))

            case .block:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.14, y: h * 0.14, width: w * 0.72, height: w * 0.72)), with: .color(color), style: stroke)
                var sl = Path()
                sl.move(to: CGPoint(x: w * 0.26, y: h * 0.26))
                sl.addLine(to: CGPoint(x: w * 0.74, y: h * 0.74))
                ctx.stroke(sl, with: .color(color), style: stroke)

            case .forward:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.18, y: h * 0.74))
                p.addQuadCurve(to: CGPoint(x: w * 0.80, y: h * 0.38), control: CGPoint(x: w * 0.26, y: h * 0.36))
                p.move(to: CGPoint(x: w * 0.62, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.84, y: h * 0.38))
                p.addLine(to: CGPoint(x: w * 0.62, y: h * 0.54))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .copy:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.30, y: h * 0.14, width: w * 0.52, height: h * 0.58), cornerRadius: 3), with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.16, y: h * 0.30, width: w * 0.52, height: h * 0.58), cornerRadius: 3), with: .color(color), style: stroke)

            case .edit:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.20, y: h * 0.80))
                p.addLine(to: CGPoint(x: w * 0.26, y: h * 0.58))
                p.addLine(to: CGPoint(x: w * 0.68, y: h * 0.16))
                p.addLine(to: CGPoint(x: w * 0.84, y: h * 0.32))
                p.addLine(to: CGPoint(x: w * 0.42, y: h * 0.74))
                p.closeSubpath()
                ctx.stroke(p, with: .color(color), style: stroke)

            case .undo:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.26, y: h * 0.38))
                p.addLine(to: CGPoint(x: w * 0.64, y: h * 0.38))
                p.addQuadCurve(to: CGPoint(x: w * 0.64, y: h * 0.76), control: CGPoint(x: w * 0.86, y: h * 0.56))
                p.addLine(to: CGPoint(x: w * 0.38, y: h * 0.76))
                p.move(to: CGPoint(x: w * 0.42, y: h * 0.20))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.38))
                p.addLine(to: CGPoint(x: w * 0.42, y: h * 0.56))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .close:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.78))
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.78))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .lock:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.20, y: h * 0.44, width: w * 0.60, height: h * 0.42), cornerRadius: 4), with: .color(color), style: stroke)
                var arch = Path()
                arch.move(to: CGPoint(x: w * 0.32, y: h * 0.44))
                arch.addQuadCurve(to: CGPoint(x: w * 0.68, y: h * 0.44), control: CGPoint(x: w * 0.50, y: h * 0.10))
                ctx.stroke(arch, with: .color(color), style: stroke)

            case .missedCall:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.20, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.54))
                p.addLine(to: CGPoint(x: w * 0.80, y: h * 0.24))
                p.move(to: CGPoint(x: w * 0.62, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.80, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.80, y: h * 0.42))
                ctx.stroke(p, with: .color(color), style: stroke)
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Animated Gifly Media Loader (Transparent Background Autoplay via ImageIO)

private final class AnimatedImageCache {
    static let shared = AnimatedImageCache()
    private var framesCache: [String: (frames: [UIImage], durations: [Double])] = [:]
    private let lock = NSLock()

    func get(_ urlString: String) -> (frames: [UIImage], durations: [Double])? {
        lock.lock()
        defer { lock.unlock() }
        return framesCache[urlString]
    }

    func set(_ urlString: String, frames: [UIImage], durations: [Double]) {
        lock.lock()
        defer { lock.unlock() }
        if framesCache.count > 64 { framesCache.removeAll() }
        framesCache[urlString] = (frames, durations)
    }
}

struct TelefamIosAnimatedMediaView: View {
    let urlString: String
    var cornerRadius: CGFloat = 0

    @State private var frames: [UIImage] = []
    @State private var durations: [Double] = []
    @State private var currentFrame: Int = 0
    @State private var animTask: Task<Void, Never>? = nil

    var body: some View {
        Group {
            if !frames.isEmpty {
                let idx = min(currentFrame, frames.count - 1)
                Image(uiImage: frames[idx])
                    .resizable()
                    .scaledToFit()
                    .background(Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            } else {
                ProgressView()
                    .tint(.white.opacity(0.65))
            }
        }
        .task(id: urlString) {
            await loadAndAnimate()
        }
        .onDisappear {
            animTask?.cancel()
            animTask = nil
        }
    }

    private func loadAndAnimate() async {
        animTask?.cancel()
        currentFrame = 0
        guard let url = URL(string: urlString), !urlString.isEmpty else { return }
        if let cached = AnimatedImageCache.shared.get(urlString) {
            frames = cached.frames
            durations = cached.durations
            startLoopIfAnimated()
            return
        }
        guard let (data, _) = try? await URLSession.shared.data(from: url) else { return }
        let decoded = Self.decodeFrames(data: data)
        guard !decoded.frames.isEmpty else { return }
        AnimatedImageCache.shared.set(urlString, frames: decoded.frames, durations: decoded.durations)
        frames = decoded.frames
        durations = decoded.durations
        startLoopIfAnimated()
    }

    private func startLoopIfAnimated() {
        animTask?.cancel()
        guard frames.count > 1 else { return }
        animTask = Task {
            while !Task.isCancelled {
                let idx = min(currentFrame, durations.count - 1)
                let delay = max(0.035, idx >= 0 ? durations[idx] : 0.08)
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                if Task.isCancelled { break }
                currentFrame = (currentFrame + 1) % max(1, frames.count)
            }
        }
    }

    private static func decodeFrames(data: Data) -> (frames: [UIImage], durations: [Double]) {
        guard let src = CGImageSourceCreateWithData(data as CFData, nil) else { return ([], []) }
        let count = min(CGImageSourceGetCount(src), 64)
        if count <= 1 {
            if let img = UIImage(data: data) { return ([img], [0.1]) }
            return ([], [])
        }
        var imgs: [UIImage] = []
        var durs: [Double] = []
        for i in 0..<count {
            guard let cg = CGImageSourceCreateImageAtIndex(src, i, nil) else { continue }
            imgs.append(UIImage(cgImage: cg))
            var dur = 0.08
            if let props = CGImageSourceCopyPropertiesAtIndex(src, i, nil) as? [CFString: Any] {
                if let gifProps = props[kCGImagePropertyGIFDictionary] as? [CFString: Any] {
                    dur = (gifProps[kCGImagePropertyGIFUnclampedDelayTime] as? Double)
                        ?? (gifProps[kCGImagePropertyGIFDelayTime] as? Double)
                        ?? 0.08
                } else if let webpProps = props[kCGImagePropertyWebPDictionary] as? [CFString: Any] {
                    dur = (webpProps[kCGImagePropertyWebPUnclampedDelayTime] as? Double)
                        ?? (webpProps[kCGImagePropertyWebPDelayTime] as? Double)
                        ?? 0.08
                }
            }
            durs.append(dur < 0.02 ? 0.08 : dur)
        }
        return (imgs, durs)
    }
}

// MARK: - Split-Screen / Adaptive Chats Host View

struct TelefamIosChatsHostView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var chatStore: TelefamIosChatStore
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let chatFilterQuery: String
    let isCompactWidth: Bool
    let safeBottomInset: CGFloat
    let onOpenUserProfile: (String) -> Void
    let onOpenPrivacy: () -> Void
    let onBrowseContacts: () -> Void
    var onStartCall: (TelefamIosChatConversation, TelefamIosCallType) -> Void = { _, _ in }
    var onOpenSendStars: (TelefamIosChatConversation) -> Void = { _ in }

    @State private var selectedConvIds: Set<String> = []
    @State private var showArchivedSheet: Bool = false

    private var activeConv: TelefamIosChatConversation? {
        guard let cid = chatStore.activeConversationId else { return nil }
        return chatStore.conversations.first(where: { $0.conversationId == cid })
            ?? chatStore.archivedConversations.first(where: { $0.conversationId == cid })
    }

    var body: some View {
        let cp = settingsStore.bundle.chatPrivacy
        let activeTheme = TelefamChatPrivacyCatalog.themeById(cp.chatThemeId)
        let activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(cp.bubbleColorId)

        Group {
            if isCompactWidth {
                ZStack {
                    conversationListColumn(theme: activeTheme, bubble: activeBubble, isCompact: true)

                    if let conv = activeConv {
                        TelefamIosOneOnOneChatView(
                            chatStore: chatStore,
                            settingsStore: settingsStore,
                            conversation: conv,
                            theme: activeTheme,
                            bubble: activeBubble,
                            showBackButton: true,
                            safeBottomInset: safeBottomInset,
                            onBack: { chatStore.closeConversation() },
                            onOpenProfile: { onOpenUserProfile(conv.peerUserId) },
                            onOpenPrivacy: onOpenPrivacy,
                            onStartCall: { callType in onStartCall(conv, callType) },
                            onOpenSendStars: { onOpenSendStars(conv) }
                        )
                        .transition(.move(edge: .trailing))
                        .zIndex(5)
                    }
                }
            } else {
                HStack(spacing: 0) {
                    conversationListColumn(theme: activeTheme, bubble: activeBubble, isCompact: false)
                        .frame(width: 340)

                    Divider().background(TelefamPalette.border(colorScheme))

                    if let conv = activeConv {
                        TelefamIosOneOnOneChatView(
                            chatStore: chatStore,
                            settingsStore: settingsStore,
                            conversation: conv,
                            theme: activeTheme,
                            bubble: activeBubble,
                            showBackButton: false,
                            safeBottomInset: 0,
                            onBack: { chatStore.closeConversation() },
                            onOpenProfile: { onOpenUserProfile(conv.peerUserId) },
                            onOpenPrivacy: onOpenPrivacy,
                            onStartCall: { callType in onStartCall(conv, callType) },
                            onOpenSendStars: { onOpenSendStars(conv) }
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        splitEmptyState(theme: activeTheme, bubble: activeBubble)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
        .sheet(isPresented: $showArchivedSheet) {
            NavigationStack {
                List {
                    ForEach(chatStore.archivedConversations) { conv in
                        HStack(spacing: 12) {
                            avatarCircle(url: conv.peerAvatarUrl, name: conv.peerFullName.isEmpty ? conv.peerUsername : conv.peerFullName, isOnline: false, size: 46)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(conv.peerFullName.isEmpty ? conv.peerUsername : conv.peerFullName)
                                    .font(.system(size: 15.5, weight: .semibold))
                                Text(conv.lastMessagePreviewPlain)
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                            Spacer()
                            Button("Unarchive") {
                                chatStore.batchAction([conv.conversationId], action: "unarchive")
                            }
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(TelefamPalette.crimson)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            showArchivedSheet = false
                            chatStore.openConversation(conv.conversationId)
                        }
                    }
                }
                .navigationTitle("Archived Chats")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showArchivedSheet = false }
                    }
                }
            }
        }
    }

    private func conversationListColumn(theme: TelefamChatThemeSpec, bubble: TelefamChatBubbleColorOption, isCompact: Bool) -> some View {
        let accentColor = TelefamChatPrivacyCatalog.color(colorScheme == .dark ? bubble.darkStartHex : bubble.lightStartHex)
        let isSearching = !chatFilterQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        return ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if !selectedConvIds.isEmpty {
                    batchSelectionBar(accentColor: accentColor)
                }

                if isSearching {
                    searchResultsScroll(accentColor: accentColor, bottomPad: isCompact ? (74 + safeBottomInset) : 16)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            if !chatStore.archivedConversations.isEmpty {
                                Button {
                                    showArchivedSheet = true
                                } label: {
                                    HStack(spacing: 12) {
                                        ChatSvgIcon(kind: .archive, color: accentColor, size: 19)
                                            .frame(width: 40, height: 40)
                                            .background(accentColor.opacity(0.14))
                                            .clipShape(Circle())
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("Archived Chats")
                                                .font(.system(size: 15, weight: .semibold))
                                                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            Text("\(chatStore.archivedConversations.count) hidden conversation(s)")
                                                .font(.system(size: 12.5))
                                                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .font(.system(size: 12.5, weight: .semibold))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 10)
                                }
                                .buttonStyle(.plain)
                                Divider().background(TelefamPalette.border(colorScheme).opacity(0.5))
                            }

                            if chatStore.conversations.isEmpty {
                                VStack(spacing: 14) {
                                    Spacer().frame(height: 56)
                                    ZStack {
                                        Circle()
                                            .fill(accentColor.opacity(0.14))
                                            .frame(width: 66, height: 66)
                                        ChatSvgIcon(kind: .lock, color: accentColor, size: 28)
                                    }
                                    Text("No conversations yet")
                                        .font(.system(size: 17, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text("Search above or open any profile to start an end-to-end encrypted chat.")
                                        .font(.system(size: 13))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                        .multilineTextAlignment(.center)
                                        .padding(.horizontal, 28)
                                    Button(action: onBrowseContacts) {
                                        Text("Browse Contacts")
                                            .font(.system(size: 14, weight: .semibold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 20)
                                            .frame(height: 44)
                                            .background(accentColor)
                                            .clipShape(Capsule())
                                    }
                                }
                                .frame(maxWidth: .infinity)
                            } else {
                                ForEach(chatStore.conversations) { conv in
                                    conversationRow(conv, accentColor: accentColor)
                                    Divider().padding(.leading, 78).background(TelefamPalette.border(colorScheme).opacity(0.45))
                                }
                            }
                        }
                        .padding(.bottom, isCompact ? (84 + safeBottomInset) : 16)
                    }
                }
            }
            .background(TelefamPalette.background(colorScheme))

            if let undo = chatStore.undoBanner {
                HStack(spacing: 12) {
                    Text(undo.message)
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundColor(.white)
                    Spacer()
                    Button {
                        chatStore.undoLastBatch()
                    } label: {
                        HStack(spacing: 5) {
                            ChatSvgIcon(kind: .undo, color: TelefamPalette.crimsonBright, size: 15)
                            Text("Undo")
                                .font(.system(size: 13.5, weight: .bold))
                                .foregroundColor(TelefamPalette.crimsonBright)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(red: 0.11, green: 0.12, blue: 0.16))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .shadow(radius: 8, y: 3)
                .padding(.horizontal, 14)
                .padding(.bottom, isCompact ? (78 + safeBottomInset) : 16)
            }
        }
    }

    private func batchSelectionBar(accentColor: Color) -> some View {
        let ids = Array(selectedConvIds)
        let firstSelected = chatStore.conversations.first(where: { ids.contains($0.conversationId) })
        let allMuted = firstSelected?.isMuted ?? false
        let anyUnread = chatStore.conversations.contains { ids.contains($0.conversationId) && $0.unreadCount > 0 }

        return HStack(spacing: 10) {
            Button {
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .close, color: TelefamPalette.textPrimary(colorScheme), size: 18)
                    .frame(width: 40, height: 40)
            }
            Text("\(ids.count)")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(accentColor)
            Spacer()
            Button {
                chatStore.batchAction(ids, action: anyUnread ? "mark_read" : "mark_unread")
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .markUnread, color: TelefamPalette.textPrimary(colorScheme), size: 19)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel(anyUnread ? "Mark read" : "Mark unread")

            Button {
                chatStore.batchAction(ids, action: allMuted ? "unmute" : "mute")
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .mute, color: TelefamPalette.textPrimary(colorScheme), size: 19)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel(allMuted ? "Unmute" : "Mute")

            Button {
                chatStore.batchAction(ids, action: "archive")
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .archive, color: TelefamPalette.textPrimary(colorScheme), size: 19)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("Archive")

            Button {
                chatStore.batchAction(ids, action: "block")
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .block, color: TelefamPalette.crimson, size: 19)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("Block")

            Button {
                chatStore.batchAction(ids, action: "delete")
                selectedConvIds.removeAll()
            } label: {
                ChatSvgIcon(kind: .trash, color: TelefamPalette.crimson, size: 19)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("Delete")
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(TelefamPalette.surface(colorScheme))
        .overlay(Divider(), alignment: .bottom)
    }

    private func conversationRow(_ conv: TelefamIosChatConversation, accentColor: Color) -> some View {
        let isSelected = selectedConvIds.contains(conv.conversationId)
        let isActive = chatStore.activeConversationId == conv.conversationId
        let typingMode = chatStore.typingModeByConv[conv.conversationId] ?? ""
        let isOwnLast = conv.lastSenderId == chatStore.currentUserId

        return HStack(spacing: 12) {
            avatarCircle(
                url: conv.peerAvatarUrl,
                name: conv.peerFullName.isEmpty ? conv.peerUsername : conv.peerFullName,
                isOnline: conv.isOnline && conv.permissions.showOnlineIndicator,
                size: 52
            )

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(conv.peerFullName.isEmpty ? "@\(conv.peerUsername)" : conv.peerFullName)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .lineLimit(1)

                    if conv.isMuted {
                        ChatSvgIcon(kind: .mute, color: TelefamPalette.textMuted(colorScheme), size: 14)
                    }

                    Spacer()

                    if isOwnLast && !conv.lastMessageId.isEmpty {
                        DeliveryTickBadge(
                            status: conv.lastDeliveryStatus,
                            peerAvatarUrl: conv.peerAvatarUrl,
                            peerName: conv.peerFullName,
                            chatStore: chatStore,
                            showReadReceipts: conv.permissions.showReadReceipts,
                            tintColor: TelefamPalette.textMuted(colorScheme)
                        )
                    }

                    Text(formatCompactChatTime(conv.lastMessageAtMs))
                        .font(.system(size: 12))
                        .foregroundColor(conv.unreadCount > 0 ? accentColor : TelefamPalette.textMuted(colorScheme))
                }

                HStack(spacing: 8) {
                    if typingMode == "recording" {
                        Text("recording voice...")
                            .font(.system(size: 13.5, weight: .medium))
                            .foregroundColor(accentColor)
                            .lineLimit(1)
                    } else if typingMode == "typing" {
                        Text("typing...")
                            .font(.system(size: 13.5, weight: .medium))
                            .foregroundColor(accentColor)
                            .lineLimit(1)
                    } else {
                        Text(conv.lastMessagePreviewPlain.isEmpty ? "Start encrypted chat" : conv.lastMessagePreviewPlain)
                            .font(.system(size: 13.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .lineLimit(1)
                    }

                    Spacer()

                    if conv.unreadCount > 0 {
                        Text(conv.unreadCount > 99 ? "99+" : "\(conv.unreadCount)")
                            .font(.system(size: 11.5, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 7)
                            .frame(minWidth: 22, minHeight: 22)
                            .background(conv.isMuted ? TelefamPalette.textMuted(colorScheme) : accentColor)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            isSelected
                ? accentColor.opacity(0.16)
                : (isActive ? accentColor.opacity(0.08) : Color.clear)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            if !selectedConvIds.isEmpty {
                if isSelected { selectedConvIds.remove(conv.conversationId) }
                else { selectedConvIds.insert(conv.conversationId) }
            } else {
                chatStore.openConversation(conv.conversationId)
            }
        }
        .onLongPressGesture(minimumDuration: 0.42) {
            if isSelected { selectedConvIds.remove(conv.conversationId) }
            else { selectedConvIds.insert(conv.conversationId) }
        }
    }

    private func searchResultsScroll(accentColor: Color, bottomPad: CGFloat) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if !chatStore.searchConvs.isEmpty {
                    searchSectionHeader("CHATS")
                    ForEach(chatStore.searchConvs) { conv in
                        conversationRow(conv, accentColor: accentColor)
                        Divider().padding(.leading, 78)
                    }
                }
                if !chatStore.searchMessages.isEmpty {
                    searchSectionHeader("ENCRYPTED MESSAGES")
                    ForEach(chatStore.searchMessages) { msg in
                        Button {
                            chatStore.openConversation(msg.conversationId)
                        } label: {
                            VStack(alignment: .leading, spacing: 3) {
                                HStack {
                                    Text(chatStore.conversations.first(where: { $0.conversationId == msg.conversationId })?.peerFullName ?? "Conversation")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Spacer()
                                    Text(formatCompactChatTime(msg.createdAtMs))
                                        .font(.system(size: 11.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                                Text(msg.plainText)
                                    .font(.system(size: 13))
                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                    .lineLimit(2)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.plain)
                        Divider().padding(.leading, 16)
                    }
                }
                if !chatStore.searchPeople.isEmpty {
                    searchSectionHeader("PEOPLE")
                    ForEach(chatStore.searchPeople) { person in
                        Button {
                            Task {
                                await chatStore.openDirectConversation(
                                    peerUserId: person.userId,
                                    fullName: person.fullName,
                                    username: person.username,
                                    avatarUrl: person.avatarUrl
                                )
                            }
                        } label: {
                            HStack(spacing: 12) {
                                avatarCircle(url: person.avatarUrl, name: person.fullName.isEmpty ? person.username : person.fullName, isOnline: false, size: 46)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(person.fullName.isEmpty ? "@\(person.username)" : person.fullName)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    Text("@\(person.username)")
                                        .font(.system(size: 12.5))
                                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                }
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 9)
                        }
                        .buttonStyle(.plain)
                        Divider().padding(.leading, 72)
                    }
                }
                if chatStore.searchConvs.isEmpty && chatStore.searchMessages.isEmpty && chatStore.searchPeople.isEmpty {
                    Text("No matching chats or people")
                        .font(.system(size: 14))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 48)
                }
            }
            .padding(.bottom, bottomPad)
        }
    }

    private func searchSectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 11.5, weight: .bold))
            .foregroundColor(TelefamPalette.textMuted(colorScheme))
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 6)
    }

    private func splitEmptyState(theme: TelefamChatThemeSpec, bubble: TelefamChatBubbleColorOption) -> some View {
        let bubbleColor = TelefamChatPrivacyCatalog.color(colorScheme == .dark ? bubble.darkStartHex : bubble.lightStartHex)
        return ZStack {
            TelefamIosChatWallpaperView(theme: theme)
                .ignoresSafeArea(edges: .bottom)
            VStack(spacing: 12) {
                ChatSvgIcon(kind: .lock, color: .white, size: 28)
                    .frame(width: 62, height: 62)
                    .background(bubbleColor)
                    .clipShape(Circle())
                Text("Select a chat to start messaging")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                Text("Messages are compressed and end-to-end encrypted")
                    .font(.system(size: 13))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
            }
            .padding(24)
            .background(Color.black.opacity(colorScheme == .dark ? 0.42 : 0.25))
            .clipShape(RoundedRectangle(cornerRadius: 22))
        }
    }

    private func avatarCircle(url: String, name: String, isOnline: Bool, size: CGFloat) -> some View {
        let initials = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(2)).uppercased()
        return ZStack(alignment: .bottomTrailing) {
            Group {
                let resolved = chatStore.resolvedMediaURL(url)
                if !resolved.isEmpty {
                    TelefamIosRemoteImage(
                        urlString: resolved,
                        bearerToken: chatStore.authBearerToken,
                        deviceFingerprint: chatStore.authDeviceFingerprint,
                        fallbackInitials: initials.isEmpty ? "T" : initials
                    )
                } else {
                    Circle()
                        .fill(TelefamPalette.crimson.opacity(0.18))
                        .overlay(
                            Text(initials.isEmpty ? "T" : initials)
                                .font(.system(size: size * 0.34, weight: .bold))
                                .foregroundColor(TelefamPalette.crimson)
                        )
                }
            }
            .frame(width: size, height: size)
            .clipShape(Circle())

            if isOnline {
                Circle()
                    .fill(Color(red: 0.13, green: 0.77, blue: 0.37))
                    .frame(width: 13, height: 13)
                    .overlay(Circle().stroke(TelefamPalette.background(colorScheme), lineWidth: 2))
            }
        }
    }
}

// MARK: - 1-on-1 Chat Detail Screen

struct TelefamIosOneOnOneChatView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var chatStore: TelefamIosChatStore
    @ObservedObject var settingsStore: TelefamIosSettingsStore
    let conversation: TelefamIosChatConversation
    let theme: TelefamChatThemeSpec
    let bubble: TelefamChatBubbleColorOption
    let showBackButton: Bool
    let safeBottomInset: CGFloat
    let onBack: () -> Void
    let onOpenProfile: () -> Void
    let onOpenPrivacy: () -> Void
    var onStartCall: (TelefamIosCallType) -> Void = { _ in }
    var onOpenSendStars: () -> Void = {}

    @State private var draftText: String = ""
    @State private var editingMsg: TelefamIosChatMessage? = nil
    @State private var showGiflyPicker: Bool = false
    @State private var forwardingMsg: TelefamIosChatMessage? = nil
    @State private var showVaultSheet: Bool = false
    @State private var isRecordingVoice: Bool = false
    @State private var recordElapsedSec: Int = 0
    @State private var recordTimer: Task<Void, Never>? = nil
    @State private var pendingVoiceUrl: URL? = nil
    @State private var pendingVoiceDurMs: Int64 = 0
    @State private var pendingVoiceViewOnce: Bool = false
    @State private var pendingVoiceTtlSec: Int = 0

    private var accentColor: Color {
        TelefamChatPrivacyCatalog.color(colorScheme == .dark ? bubble.darkStartHex : bubble.lightStartHex)
    }

    var body: some View {
        ZStack {
            TelefamIosChatWallpaperView(theme: theme)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                floatingHeader

                if !chatStore.activeSecurityFlags.isEmpty {
                    securityFlagBanner(chatStore.activeSecurityFlags[0])
                }

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            if chatStore.activeMessages.isEmpty {
                                greetingEmptyCard
                                    .padding(.top, 72)
                            } else {
                                Color.clear.frame(height: 6)
                                ForEach(chatStore.activeMessages) { msg in
                                    let isOwn = msg.senderUserId == chatStore.currentUserId
                                    let isDissolving = chatStore.dissolvingIds.contains(msg.messageId)
                                    MessageBubbleRow(
                                        msg: msg,
                                        isOwn: isOwn,
                                        isDissolving: isDissolving,
                                        isPlaying: chatStore.playingMessageId == msg.messageId,
                                        bubble: bubble,
                                        peerAvatarUrl: conversation.peerAvatarUrl,
                                        peerName: conversation.peerFullName,
                                        permissions: conversation.permissions,
                                        chatStore: chatStore,
                                        onPlayVoice: { chatStore.toggleVoicePlayback(msg) },
                                        onCopy: {
                                            UIPasteboard.general.string = msg.plainText
                                            chatStore.showToast("Copied")
                                        },
                                        onForward: { forwardingMsg = msg },
                                        onEdit: {
                                            editingMsg = msg
                                            draftText = msg.plainText
                                        },
                                        onUnsend: { chatStore.unsendMessage(msg) },
                                        onDeleteForSelf: { chatStore.deleteForSelf(msg) },
                                        onCallBack: { callType in onStartCall(callType) }
                                    )
                                    .id(msg.messageId)
                                }
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.bottom, 12)
                    }
                    .onChange(of: chatStore.activeMessages.count) { _ in
                        if let last = chatStore.activeMessages.last {
                            withAnimation(.easeOut(duration: 0.2)) {
                                proxy.scrollTo(last.messageId, anchor: .bottom)
                            }
                        }
                    }
                }

                if let editTarget = editingMsg {
                    editingBanner(editTarget)
                }

                if let voiceUrl = pendingVoiceUrl {
                    voicePreviewBar(url: voiceUrl)
                } else {
                    composerBar
                }
            }
            .padding(.bottom, safeBottomInset)

            if let voMsg = chatStore.activeViewOnceMsg {
                viewOncePlaybackOverlay(voMsg)
            }
        }
        .sheet(isPresented: $showGiflyPicker) {
            GiflyMediaPickerSheet(chatStore: chatStore, accentColor: accentColor) { item, mediaType in
                chatStore.sendGiflyMedia(convId: conversation.conversationId, peerUserId: conversation.peerUserId, item: item, type: mediaType)
            }
        }
        .sheet(item: $forwardingMsg) { msg in
            NavigationStack {
                List {
                    ForEach(chatStore.conversations.filter { $0.conversationId != conversation.conversationId && !$0.permissions.isBlocked }) { target in
                        Button {
                            chatStore.forwardMessage(msg, toConv: target)
                            forwardingMsg = nil
                        } label: {
                            HStack(spacing: 12) {
                                Text(target.peerFullName.isEmpty ? "@\(target.peerUsername)" : target.peerFullName)
                                    .font(.system(size: 15.5, weight: .semibold))
                                    .foregroundColor(.primary)
                                Spacer()
                                ChatSvgIcon(kind: .forward, color: accentColor, size: 18)
                            }
                        }
                    }
                }
                .navigationTitle("Forward to...")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { forwardingMsg = nil }
                    }
                }
            }
        }
        .sheet(isPresented: $showVaultSheet) {
            SharedMediaVaultSheet(messages: chatStore.activeMessages, accentColor: accentColor)
        }
    }

    private var floatingHeader: some View {
        let typingMode = chatStore.typingModeByConv[conversation.conversationId] ?? ""
        let subtitle: String = {
            if typingMode == "recording" { return "recording voice..." }
            if typingMode == "typing" { return "typing..." }
            if !conversation.permissions.showOnlineIndicator { return "last seen recently" }
            return conversation.isOnline ? "online" : (conversation.lastSeenLabel.isEmpty ? "last seen recently" : conversation.lastSeenLabel)
        }()
        let pillBg = colorScheme == .dark ? Color.black.opacity(0.60) : Color.white.opacity(0.86)
        let primaryInk = colorScheme == .dark ? Color.white : Color(red: 0.08, green: 0.09, blue: 0.12)
        let subInk = typingMode.isEmpty ? (colorScheme == .dark ? Color.white.opacity(0.68) : Color.black.opacity(0.56)) : accentColor

        return HStack(spacing: 8) {
            if showBackButton {
                Button(action: onBack) {
                    ChatSvgIcon(kind: .back, color: primaryInk, size: 20)
                        .frame(width: 44, height: 44)
                        .background(pillBg)
                        .clipShape(Circle())
                }
            }

            Button(action: onOpenProfile) {
                HStack(spacing: 10) {
                    let displayName = conversation.peerFullName.isEmpty ? "@\(conversation.peerUsername)" : conversation.peerFullName
                    let initials = String(displayName.prefix(2)).uppercased()
                    Group {
                        let resolved = chatStore.resolvedMediaURL(conversation.peerAvatarUrl)
                        if !resolved.isEmpty {
                            TelefamIosRemoteImage(
                                urlString: resolved,
                                bearerToken: chatStore.authBearerToken,
                                deviceFingerprint: chatStore.authDeviceFingerprint,
                                fallbackInitials: initials.isEmpty ? "T" : initials
                            )
                        } else {
                            Circle()
                                .fill(accentColor.opacity(0.24))
                                .overlay(Text(initials.isEmpty ? "T" : initials).font(.system(size: 13, weight: .bold)).foregroundColor(accentColor))
                        }
                    }
                    .frame(width: 36, height: 36)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 1) {
                        Text(displayName)
                            .font(.system(size: 15.5, weight: .bold))
                            .foregroundColor(primaryInk)
                            .lineLimit(1)
                        Text(subtitle)
                            .font(.system(size: 12, weight: typingMode.isEmpty ? .regular : .semibold))
                            .foregroundColor(subInk)
                            .lineLimit(1)
                    }
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(pillBg)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)

            HStack(spacing: 2) {
                Button {
                    onStartCall(.audio)
                } label: {
                    ChatSvgIcon(kind: .phone, color: primaryInk, size: 19)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Audio call")

                Button {
                    onStartCall(.video)
                } label: {
                    ChatSvgIcon(kind: .video, color: primaryInk, size: 19)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Video call")

                Menu {
                    Button("View Profile") { onOpenProfile() }
                    Button("Media, Links & Docs") { showVaultSheet = true }
                    Button(conversation.isMuted ? "Unmute Notifications" : "Mute Notifications") {
                        chatStore.batchAction([conversation.conversationId], action: conversation.isMuted ? "unmute" : "mute")
                    }
                    Button("Privacy & Themes") { onOpenPrivacy() }
                    if conversation.permissions.isBlocked {
                        Button("Unblock User") {
                            chatStore.unblockPeer(convId: conversation.conversationId, peerUserId: conversation.peerUserId)
                        }
                    } else {
                        Button("Block User", role: .destructive) {
                            chatStore.batchAction([conversation.conversationId], action: "block")
                        }
                    }
                    Button("Report User", role: .destructive) {
                        chatStore.reportPeer(convId: conversation.conversationId, peerUserId: conversation.peerUserId, reason: "harassment")
                    }
                } label: {
                    ChatSvgIcon(kind: .moreVertical, color: primaryInk, size: 19)
                        .frame(width: 40, height: 40)
                }
            }
            .padding(.horizontal, 4)
            .background(pillBg)
            .clipShape(Capsule())
        }
        .padding(.horizontal, 10)
        .padding(.top, 8)
        .padding(.bottom, 6)
    }

    private func securityFlagBanner(_ flag: TelefamIosChatSecurityFlag) -> some View {
        HStack(spacing: 8) {
            ChatSvgIcon(kind: .shield, color: TelefamPalette.crimsonBright, size: 16)
            Text(flag.details.isEmpty ? flag.flagType.replacingOccurrences(of: "_", with: " ") : flag.details)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.white)
                .lineLimit(2)
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.68))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 14)
        .padding(.bottom, 4)
    }

    private var greetingEmptyCard: some View {
        VStack(spacing: 12) {
            Text("No messages here yet...")
                .font(.system(size: 15.5, weight: .bold))
                .foregroundColor(.white)
            Text("Send a message or tap\nthe greeting below.")
                .font(.system(size: 13.5))
                .foregroundColor(.white.opacity(0.84))
                .multilineTextAlignment(.center)

            if let sticker = chatStore.greetingSticker {
                Button {
                    chatStore.sendGiflyMedia(
                        convId: conversation.conversationId,
                        peerUserId: conversation.peerUserId,
                        item: sticker,
                        type: "sticker"
                    )
                } label: {
                    TelefamIosAnimatedMediaView(urlString: sticker.bestUrl)
                        .frame(width: 146, height: 146)
                        .background(Color.clear)
                }
                .buttonStyle(.plain)
            } else {
                ProgressView()
                    .tint(.white.opacity(0.75))
                    .frame(width: 120, height: 120)
            }
        }
        .padding(.horizontal, 26)
        .padding(.vertical, 22)
        .background(Color.black.opacity(0.46))
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color.white.opacity(0.12), lineWidth: 1))
    }

    private func editingBanner(_ msg: TelefamIosChatMessage) -> some View {
        HStack(spacing: 10) {
            ChatSvgIcon(kind: .edit, color: accentColor, size: 17)
            VStack(alignment: .leading, spacing: 1) {
                Text("Editing message")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(accentColor)
                Text(msg.plainText)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .lineLimit(1)
            }
            Spacer()
            Button {
                editingMsg = nil
                draftText = ""
            } label: {
                ChatSvgIcon(kind: .close, color: TelefamPalette.textMuted(colorScheme), size: 16)
                    .frame(width: 34, height: 34)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(TelefamPalette.surface(colorScheme).opacity(0.95))
    }

    private func voicePreviewBar(url: URL) -> some View {
        VStack(spacing: 8) {
            HStack(spacing: 10) {
                Button {
                    try? FileManager.default.removeItem(at: url)
                    pendingVoiceUrl = nil
                    pendingVoiceDurMs = 0
                } label: {
                    ChatSvgIcon(kind: .trash, color: TelefamPalette.crimson, size: 18)
                        .frame(width: 40, height: 40)
                }

                Text("Voice \(String(format: "%.1fs", Double(pendingVoiceDurMs) / 1000.0))")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                Spacer()

                Button {
                    pendingVoiceViewOnce.toggle()
                } label: {
                    HStack(spacing: 5) {
                        ChatSvgIcon(kind: .eyeOnce, color: pendingVoiceViewOnce ? .white : accentColor, size: 16)
                        Text("View Once")
                            .font(.system(size: 12.5, weight: .semibold))
                            .foregroundColor(pendingVoiceViewOnce ? .white : accentColor)
                    }
                    .padding(.horizontal, 10)
                    .frame(height: 34)
                    .background(pendingVoiceViewOnce ? accentColor : accentColor.opacity(0.14))
                    .clipShape(Capsule())
                }

                Button {
                    chatStore.sendVoice(
                        convId: conversation.conversationId,
                        peerUserId: conversation.peerUserId,
                        fileUrl: url,
                        durationMs: pendingVoiceDurMs,
                        viewOnce: pendingVoiceViewOnce,
                        ttlSec: pendingVoiceTtlSec
                    )
                    pendingVoiceUrl = nil
                    pendingVoiceDurMs = 0
                    pendingVoiceViewOnce = false
                    pendingVoiceTtlSec = 0
                } label: {
                    ChatSvgIcon(kind: .send, color: .white, size: 18)
                        .frame(width: 42, height: 42)
                        .background(accentColor)
                        .clipShape(Circle())
                }
            }

            if pendingVoiceViewOnce {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach([(0, "Play Once"), (5, "5s"), (10, "10s"), (30, "30s"), (60, "60s")], id: \.0) { sec, label in
                            Button {
                                pendingVoiceTtlSec = sec
                            } label: {
                                Text(label)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(pendingVoiceTtlSec == sec ? .white : TelefamPalette.textPrimary(colorScheme))
                                    .padding(.horizontal, 12)
                                    .frame(height: 28)
                                    .background(pendingVoiceTtlSec == sec ? accentColor : TelefamPalette.surfaceVariant(colorScheme))
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(TelefamPalette.surface(colorScheme).opacity(0.95))
    }

    private var composerBar: some View {
        let isBlocked = conversation.permissions.isBlocked
        let hasText = !draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let barBg = colorScheme == .dark ? Color.black.opacity(0.76) : Color.white.opacity(0.94)

        return VStack(spacing: 4) {
            HStack(spacing: 6) {
                if isRecordingVoice {
                    HStack(spacing: 10) {
                        Circle()
                            .fill(TelefamPalette.crimson)
                            .frame(width: 10, height: 10)
                        Text("Recording \(recordElapsedSec)s...")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(TelefamPalette.crimson)
                        Spacer()
                        Button("Cancel") {
                            recordTimer?.cancel()
                            _ = chatStore.audio.stopRecording(discard: true)
                            isRecordingVoice = false
                            chatStore.sendTypingWs(convId: conversation.conversationId, mode: "idle")
                        }
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))

                        Button {
                            finishVoiceRecording()
                        } label: {
                            Text("Preview")
                                .font(.system(size: 13.5, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .frame(height: 36)
                                .background(accentColor)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 46)
                    .background(barBg)
                    .clipShape(Capsule())
                } else {
                    HStack(spacing: 6) {
                        Button {
                            if !isBlocked { showGiflyPicker = true }
                        } label: {
                            ChatSvgIcon(kind: .smiley, color: TelefamPalette.textMuted(colorScheme), size: 22)
                                .frame(width: 40, height: 40)
                        }
                        .disabled(isBlocked)

                        TextField(isBlocked ? "Messaging unavailable" : "Message", text: $draftText, axis: .vertical)
                            .font(.system(size: 15))
                            .lineLimit(1...5)
                            .disabled(isBlocked)
                            .onChange(of: draftText) { val in
                                if !isBlocked {
                                    chatStore.sendTypingWs(
                                        convId: conversation.conversationId,
                                        mode: val.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "idle" : "typing"
                                    )
                                }
                            }

                        if !hasText {
                            Button {
                                onOpenSendStars()
                            } label: {
                                ChatSvgIcon(kind: .gift, color: accentColor, size: 20)
                                    .frame(width: 38, height: 40)
                            }
                            .accessibilityLabel("Send Stars")
                            .disabled(isBlocked)

                            Button {
                                chatStore.showToast("File attachments — Coming soon")
                            } label: {
                                ChatSvgIcon(kind: .paperclip, color: TelefamPalette.textMuted(colorScheme), size: 20)
                                    .frame(width: 38, height: 40)
                            }
                            .disabled(isBlocked)
                        }
                    }
                    .padding(.horizontal, 8)
                    .frame(minHeight: 46)
                    .background(barBg)
                    .clipShape(Capsule())
                }

                if !isRecordingVoice {
                    Button {
                        if isBlocked { return }
                        if hasText {
                            let content = draftText
                            draftText = ""
                            chatStore.sendTypingWs(convId: conversation.conversationId, mode: "idle")
                            if let target = editingMsg {
                                editingMsg = nil
                                chatStore.editMessage(target, newText: content)
                            } else {
                                chatStore.sendText(convId: conversation.conversationId, peerUserId: conversation.peerUserId, text: content)
                            }
                        } else {
                            startVoiceRecording()
                        }
                    } label: {
                        ChatSvgIcon(kind: hasText ? .send : .mic, color: .white, size: 20)
                            .frame(width: 46, height: 46)
                            .background(isBlocked ? TelefamPalette.textMuted(colorScheme).opacity(0.45) : accentColor)
                            .clipShape(Circle())
                    }
                    .disabled(isBlocked)
                }
            }
            .padding(.horizontal, 10)

            if isBlocked {
                Text("You cannot message this user")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundColor(TelefamPalette.crimsonBright)
                    .padding(.vertical, 3)
            }
        }
        .padding(.bottom, 8)
        .padding(.top, 4)
    }

    private func viewOncePlaybackOverlay(_ voMsg: TelefamIosChatMessage) -> some View {
        ZStack {
            Color.black.opacity(0.88).ignoresSafeArea()
            VStack(spacing: 18) {
                ZStack {
                    Circle()
                        .stroke(accentColor, style: StrokeStyle(lineWidth: 4, dash: [8, 6]))
                        .frame(width: 96, height: 96)
                    ChatSvgIcon(kind: .eyeOnce, color: .white, size: 38)
                }
                Text("View-Once Voice Note")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Text(
                    chatStore.viewOnceRemainingSec > 0
                        ? "Auto-expiring in \(chatStore.viewOnceRemainingSec)s"
                        : "Auto-expiring when playback finishes"
                )
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.white.opacity(0.8))

                Button {
                    chatStore.consumeViewOnce(voMsg)
                } label: {
                    Text("Close & Burn Now")
                        .font(.system(size: 14.5, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 22)
                        .frame(height: 44)
                        .background(TelefamPalette.crimson)
                        .clipShape(Capsule())
                }
            }
            .padding(28)
        }
        .transition(.opacity)
    }

    private func startVoiceRecording() {
        guard chatStore.audio.startRecording() != nil else {
            chatStore.showToast("Microphone permission required to record voice")
            return
        }
        isRecordingVoice = true
        recordElapsedSec = 0
        chatStore.sendTypingWs(convId: conversation.conversationId, mode: "recording")
        recordTimer?.cancel()
        recordTimer = Task {
            while !Task.isCancelled && isRecordingVoice {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if isRecordingVoice { recordElapsedSec += 1 }
            }
        }
    }

    private func finishVoiceRecording() {
        recordTimer?.cancel()
        isRecordingVoice = false
        chatStore.sendTypingWs(convId: conversation.conversationId, mode: "idle")
        let (url, dur) = chatStore.audio.stopRecording(discard: false)
        if let u = url {
            pendingVoiceUrl = u
            pendingVoiceDurMs = dur
            pendingVoiceViewOnce = false
            pendingVoiceTtlSec = 0
        }
    }
}

// MARK: - Message Bubble + Sparkle Dissolve + Read More/Less + Decorated Title/Subtitle

private struct MessageBubbleRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let msg: TelefamIosChatMessage
    let isOwn: Bool
    let isDissolving: Bool
    let isPlaying: Bool
    let bubble: TelefamChatBubbleColorOption
    let peerAvatarUrl: String
    let peerName: String
    let permissions: TelefamIosChatCapabilities
    let chatStore: TelefamIosChatStore
    let onPlayVoice: () -> Void
    let onCopy: () -> Void
    let onForward: () -> Void
    let onEdit: () -> Void
    let onUnsend: () -> Void
    let onDeleteForSelf: () -> Void
    let onCallBack: (TelefamIosCallType) -> Void

    @State private var expanded: Bool = false

    private var ownGradient: LinearGradient {
        let s = TelefamChatPrivacyCatalog.color(colorScheme == .dark ? bubble.darkStartHex : bubble.lightStartHex)
        let e = TelefamChatPrivacyCatalog.color(colorScheme == .dark ? bubble.darkEndHex : bubble.lightEndHex)
        return LinearGradient(colors: [s, e], startPoint: .leading, endPoint: .trailing)
    }

    private var peerFill: Color {
        colorScheme == .dark ? Color(red: 0.14, green: 0.16, blue: 0.21) : Color.white.opacity(0.95)
    }

    private var textInk: Color {
        if isOwn { return .white }
        return colorScheme == .dark ? .white : Color(red: 0.10, green: 0.11, blue: 0.14)
    }

    private var subInk: Color {
        if isOwn { return .white.opacity(0.76) }
        return colorScheme == .dark ? .white.opacity(0.62) : Color.black.opacity(0.55)
    }

    var body: some View {
        HStack {
            if isOwn { Spacer(minLength: 48) }

            ZStack {
                if msg.messageType == "sticker" || msg.messageType == "gif" || msg.messageType == "clip" {
                    mediaMessageContent
                } else {
                    bubbleMessageContent
                }

                if isDissolving {
                    SparkleDissolveOverlay(color: TelefamChatPrivacyCatalog.color(bubble.darkEndHex))
                }
            }
            .opacity(isDissolving ? 0.18 : 1.0)
            .scaleEffect(isDissolving ? 0.88 : 1.0)
            .animation(.easeOut(duration: 0.38), value: isDissolving)
            .contextMenu {
                if permissions.canCopyMessages && !msg.plainText.isEmpty && (msg.messageType == "text") {
                    Button {
                        onCopy()
                    } label: {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                }
                if permissions.canShareChats && msg.messageType != "voice_once" {
                    Button {
                        onForward()
                    } label: {
                        Label("Forward", systemImage: "arrowshape.turn.up.right")
                    }
                }
                if isOwn && msg.messageType == "text" {
                    Button {
                        onEdit()
                    } label: {
                        Label("Edit", systemImage: "pencil")
                    }
                }
                if isOwn {
                    Button(role: .destructive) {
                        onUnsend()
                    } label: {
                        Label("Unsend", systemImage: "sparkles")
                    }
                }
                Button(role: .destructive) {
                    onDeleteForSelf()
                } label: {
                    Label("Delete for me", systemImage: "trash")
                }
            }

            if !isOwn { Spacer(minLength: 48) }
        }
    }

    private var mediaMessageContent: some View {
        VStack(alignment: isOwn ? .trailing : .leading, spacing: 4) {
            let corner: CGFloat = msg.messageType == "sticker" ? 0 : 14
            TelefamIosAnimatedMediaView(urlString: msg.mediaUrl, cornerRadius: corner)
                .frame(maxWidth: msg.messageType == "sticker" ? 154 : 210, maxHeight: msg.messageType == "sticker" ? 154 : 180)
                .background(Color.clear)

            HStack(spacing: 4) {
                Text(formatCompactChatTime(msg.createdAtMs))
                    .font(.system(size: 11))
                    .foregroundColor(.white.opacity(0.85))
                if isOwn {
                    DeliveryTickBadge(
                        status: msg.deliveryStatus,
                        peerAvatarUrl: peerAvatarUrl,
                        peerName: peerName,
                        chatStore: chatStore,
                        showReadReceipts: permissions.showReadReceipts,
                        tintColor: .white
                    )
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.black.opacity(0.45))
            .clipShape(Capsule())
        }
    }

    private var bubbleMessageContent: some View {
        VStack(alignment: .leading, spacing: 5) {
            if !msg.forwardedFromUserId.isEmpty {
                HStack(spacing: 4) {
                    ChatSvgIcon(kind: .forward, color: subInk, size: 12)
                    Text("Forwarded")
                        .font(.system(size: 11.5, weight: .medium).italic())
                        .foregroundColor(subInk)
                }
            }

            if msg.messageType == "voice" || msg.messageType == "voice_once" {
                voiceRowContent
            } else if msg.messageType == "star_gift" {
                TelefamIosStarGiftChatBubbleContent(
                    rawMetaJson: msg.plainText,
                    isOwn: isOwn,
                    textColor: textInk,
                    subColor: subInk
                )
            } else if msg.messageType == "call_log" || msg.messageType == "call_request" {
                callMetaCardContent
            } else {
                decoratedTextContent
            }

            HStack(spacing: 4) {
                if msg.editedAtMs > 0 {
                    Text("edited")
                        .font(.system(size: 10.5))
                        .foregroundColor(subInk)
                }
                Text(formatCompactChatTime(msg.createdAtMs))
                    .font(.system(size: 10.5))
                    .foregroundColor(subInk)
                if isOwn {
                    DeliveryTickBadge(
                        status: msg.deliveryStatus,
                        peerAvatarUrl: peerAvatarUrl,
                        peerName: peerName,
                        chatStore: chatStore,
                        showReadReceipts: permissions.showReadReceipts,
                        tintColor: subInk
                    )
                }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background {
            if isOwn {
                RoundedRectangle(cornerRadius: 17).fill(ownGradient)
            } else {
                RoundedRectangle(cornerRadius: 17).fill(peerFill)
            }
        }
    }

    private var voiceRowContent: some View {
        let isOnce = msg.messageType == "voice_once"
        let lockedForSender = isOnce && isOwn
        return HStack(spacing: 10) {
            Button {
                if !lockedForSender { onPlayVoice() }
            } label: {
                ZStack {
                    if isOnce {
                        ChatSvgIcon(kind: .eyeOnce, color: textInk, size: 28)
                    } else {
                        Circle()
                            .fill(isOwn ? Color.white.opacity(0.22) : Color.black.opacity(0.08))
                            .frame(width: 36, height: 36)
                        ChatSvgIcon(kind: isPlaying ? .pause : .play, color: textInk, size: 16)
                    }
                }
                .frame(width: 40, height: 40)
            }
            .disabled(lockedForSender)

            VStack(alignment: .leading, spacing: 3) {
                Text(isOnce ? (isOwn ? "Sent view-once voice" : "Tap to listen once") : "Voice message")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(textInk)
                Text(String(format: "%.1fs", max(0.3, Double(msg.voiceDurationMs) / 1000.0)) + (msg.viewOnceTtlSec > 0 ? "  ·  \(msg.viewOnceTtlSec)s timer" : ""))
                    .font(.system(size: 11.5))
                    .foregroundColor(subInk)
            }
        }
    }

    @ViewBuilder
    private var callMetaCardContent: some View {
        let meta = TelefamIosCallMetaParser.parseCallMeta(plainText: msg.plainText, messageType: msg.messageType)
        let callTypeEnum: TelefamIosCallType = (meta?.callType == "video") ? .video : .audio
        let isRequest = msg.messageType == "call_request" || meta?.outcome == "call_request"
        let isMissedOrBlocked = meta?.outcome == "missed" || meta?.outcome == "declined" || meta?.outcome == "not_allowed"
        let title: String = {
            if isRequest { return "Call request" }
            let kind = callTypeEnum == .video ? "Video call" : "Audio call"
            switch meta?.outcome ?? "ended" {
            case "missed": return "Missed \(kind.lowercased())"
            case "declined": return "Declined \(kind.lowercased())"
            case "not_allowed": return "Calls not allowed"
            default: return kind
            }
        }()
        let subtitle: String = {
            if let s = meta?.summaryText, !s.isEmpty { return s }
            if isRequest { return "Calls not allowed. Please call me back." }
            if isMissedOrBlocked { return isOwn ? "No answer" : "I tried to call you" }
            let dur = meta?.durationSec ?? 0
            return dur > 0 ? "Duration \(TelefamIosCallMetaParser.formatDuration(dur))" : "Call ended"
        }()
        let iconKind: ChatSvgIcon.Kind = isMissedOrBlocked ? .missedCall : (callTypeEnum == .video ? .video : .phone)

        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                ChatSvgIcon(kind: iconKind, color: textInk, size: 19)
                    .frame(width: 36, height: 36)
                    .background(isOwn ? Color.white.opacity(0.20) : TelefamPalette.crimson.opacity(0.14))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(textInk)
                    Text(subtitle)
                        .font(.system(size: 12.5))
                        .foregroundColor(subInk)
                }
            }
            if meta?.canCallBack ?? true {
                Button {
                    onCallBack(callTypeEnum)
                } label: {
                    HStack(spacing: 6) {
                        ChatSvgIcon(kind: callTypeEnum == .video ? .video : .phone, color: isOwn ? .white : TelefamPalette.crimson, size: 14)
                        Text(isRequest ? "Call now" : "Call back")
                            .font(.system(size: 12.5, weight: .bold))
                            .foregroundColor(isOwn ? .white : TelefamPalette.crimson)
                    }
                    .padding(.horizontal, 12)
                    .frame(minHeight: 36)
                    .background(isOwn ? Color.white.opacity(0.20) : TelefamPalette.crimson.opacity(0.12))
                    .clipShape(Capsule())
                }
            }
        }
    }

    @ViewBuilder
    private var decoratedTextContent: some View {
        let raw = msg.plainText
        let lines = raw.components(separatedBy: .newlines)
        let hasDecoratedHeader = lines.count >= 2 && lines[0].count <= 48 && lines.dropFirst().contains(where: { !$0.trimmingCharacters(in: .whitespaces).isEmpty })
        let needsCollapse = raw.count > 280
        let shownBody: String = {
            if hasDecoratedHeader {
                let body = lines.dropFirst().joined(separator: "\n").trimmingCharacters(in: .newlines)
                if needsCollapse && !expanded { return String(body.prefix(240)) + "..." }
                return body
            } else {
                if needsCollapse && !expanded { return String(raw.prefix(280)) + "..." }
                return raw
            }
        }()

        VStack(alignment: .leading, spacing: 6) {
            if hasDecoratedHeader {
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(isOwn ? Color.white.opacity(0.75) : TelefamPalette.crimson)
                        .frame(width: 3, height: 15)
                    Text(lines[0])
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(textInk)
                }
            }
            SmartInteractiveTextView(
                text: shownBody,
                source: "chat",
                font: .body,
                textColor: textInk,
                linkColor: isOwn ? Color(red: 1.0, green: 0.85, blue: 0.88) : TelefamPalette.crimson,
                otpHighlightColor: isOwn ? Color.white.opacity(0.22) : TelefamPalette.crimson.opacity(0.14)
            )

            SmartLinkPreviewCardStack(
                text: msg.plainText,
                source: "chat",
                isDarkBubble: isOwn
            )

            if needsCollapse {
                Button {
                    expanded.toggle()
                } label: {
                    Text(expanded ? "Read less" : "Read more")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(isOwn ? .white.opacity(0.92) : TelefamPalette.crimson)
                }
                .padding(.top, 2)
            }
        }
    }
}

private struct DeliveryTickBadge: View {
    let status: String
    let peerAvatarUrl: String
    let peerName: String
    let chatStore: TelefamIosChatStore
    let showReadReceipts: Bool
    let tintColor: Color

    var body: some View {
        switch status {
        case "sending", "pending":
            ChatSvgIcon(kind: .clock, color: tintColor, size: 13)
        case "delivered":
            ChatSvgIcon(kind: .checkDouble, color: TelefamPalette.crimsonBright, size: 14)
        case "seen":
            if showReadReceipts {
                let resolved = chatStore.resolvedMediaURL(peerAvatarUrl)
                if !resolved.isEmpty {
                    TelefamIosRemoteImage(
                        urlString: resolved,
                        bearerToken: chatStore.authBearerToken,
                        deviceFingerprint: chatStore.authDeviceFingerprint,
                        fallbackInitials: String(peerName.prefix(1)).uppercased()
                    )
                    .frame(width: 14, height: 14)
                    .clipShape(Circle())
                } else {
                    ChatSvgIcon(kind: .checkDouble, color: Color(red: 0.16, green: 0.80, blue: 0.96), size: 14)
                }
            } else {
                ChatSvgIcon(kind: .checkDouble, color: TelefamPalette.crimsonBright, size: 14)
            }
        default:
            ChatSvgIcon(kind: .checkSingle, color: tintColor, size: 13)
        }
    }
}

private struct SparkleDissolveOverlay: View {
    let color: Color
    var body: some View {
        Canvas { ctx, size in
            let pts: [(CGFloat, CGFloat, CGFloat)] = [
                (0.18, 0.25, 4), (0.42, 0.18, 3), (0.76, 0.30, 5),
                (0.28, 0.72, 4), (0.62, 0.78, 3.5), (0.85, 0.64, 4)
            ]
            for (px, py, rad) in pts {
                let cx = size.width * px
                let cy = size.height * py
                var star = Path()
                star.move(to: CGPoint(x: cx, y: cy - rad))
                star.addLine(to: CGPoint(x: cx + rad * 0.35, y: cy - rad * 0.35))
                star.addLine(to: CGPoint(x: cx + rad, y: cy))
                star.addLine(to: CGPoint(x: cx + rad * 0.35, y: cy + rad * 0.35))
                star.addLine(to: CGPoint(x: cx, y: cy + rad))
                star.addLine(to: CGPoint(x: cx - rad * 0.35, y: cy + rad * 0.35))
                star.addLine(to: CGPoint(x: cx - rad, y: cy))
                star.addLine(to: CGPoint(x: cx - rad * 0.35, y: cy - rad * 0.35))
                star.closeSubpath()
                ctx.fill(star, with: .color(color))
            }
        }
        .allowsHitTesting(false)
    }
}

// MARK: - Gifly Stickers / GIFs / Clips Sheet

private struct GiflyMediaPickerSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var chatStore: TelefamIosChatStore
    let accentColor: Color
    let onSendMedia: (TelefamIosGiflyItem, String) -> Void

    @State private var selectedTab: String = "sticker"
    @State private var query: String = ""
    @State private var items: [TelefamIosGiflyItem] = []
    @State private var page: Int = 1
    @State private var hasNext: Bool = false
    @State private var isLoading: Bool = false
    @State private var previewItem: TelefamIosGiflyItem? = nil

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                Picker("Media Type", selection: $selectedTab) {
                    Text("Stickers").tag("sticker")
                    Text("GIFs").tag("gif")
                    Text("Clips").tag("clip")
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 14)

                HStack(spacing: 8) {
                    ChatSvgIcon(kind: .search, color: .secondary, size: 17)
                    TextField("Search \(selectedTab)s...", text: $query)
                        .font(.system(size: 14.5))
                        .onSubmit { Task { await reload(reset: true) } }
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .background(TelefamPalette.surfaceVariant(colorScheme))
                .clipShape(Capsule())
                .padding(.horizontal, 14)

                if let pv = previewItem {
                    VStack(spacing: 12) {
                        Spacer()
                        TelefamIosAnimatedMediaView(urlString: pv.bestUrl, cornerRadius: selectedTab == "sticker" ? 0 : 16)
                            .frame(maxWidth: 220, maxHeight: 220)
                            .background(Color.clear)
                        if !pv.title.isEmpty {
                            Text(pv.title)
                                .font(.system(size: 14, weight: .semibold))
                        }
                        HStack(spacing: 12) {
                            Button("Back") { previewItem = nil }
                                .frame(height: 44)
                                .padding(.horizontal, 20)
                                .background(TelefamPalette.surfaceVariant(colorScheme))
                                .clipShape(Capsule())
                            Button {
                                onSendMedia(pv, selectedTab)
                                dismiss()
                            } label: {
                                HStack(spacing: 6) {
                                    ChatSvgIcon(kind: .send, color: .white, size: 16)
                                    Text("Send \(selectedTab.capitalized)")
                                        .font(.system(size: 14.5, weight: .bold))
                                        .foregroundColor(.white)
                                }
                                .frame(height: 44)
                                .padding(.horizontal, 22)
                                .background(accentColor)
                                .clipShape(Capsule())
                            }
                        }
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    ScrollView {
                        let cols = Array(repeating: GridItem(.flexible(), spacing: 10), count: selectedTab == "sticker" ? 3 : 2)
                        LazyVGrid(columns: cols, spacing: 10) {
                            if isLoading && items.isEmpty {
                                ForEach(0..<9, id: \.self) { _ in
                                    RoundedRectangle(cornerRadius: 14)
                                        .fill(TelefamPalette.surfaceVariant(colorScheme))
                                        .frame(height: 112)
                                }
                            } else {
                                ForEach(items) { item in
                                    Button {
                                        previewItem = item
                                    } label: {
                                        TelefamIosAnimatedMediaView(urlString: item.bestUrl, cornerRadius: selectedTab == "sticker" ? 0 : 12)
                                            .frame(height: 112)
                                            .frame(maxWidth: .infinity)
                                            .background(selectedTab == "sticker" ? Color.clear : TelefamPalette.surfaceVariant(colorScheme))
                                            .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(14)

                        if hasNext && !isLoading {
                            Button("Load more") {
                                Task { await reload(reset: false) }
                            }
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(accentColor)
                            .padding(.bottom, 16)
                        }
                    }
                }
            }
            .navigationTitle("Media & Stickers")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .task { await reload(reset: true) }
        .onChange(of: selectedTab) { _ in
            previewItem = nil
            Task { await reload(reset: true) }
        }
    }

    private func reload(reset: Bool) async {
        isLoading = true
        let nextPage = reset ? 1 : (page + 1)
        let (fetched, more) = await chatStore.searchGifly(type: selectedTab, query: query, page: nextPage)
        if reset { items = fetched } else { items.append(contentsOf: fetched) }
        page = nextPage
        hasNext = more
        isLoading = false
    }
}

// MARK: - Media, Links & Docs Shortcut Sheet

private struct SharedMediaVaultSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var smartLinksCoordinator: TelefamIosSmartLinksCoordinator
    let messages: [TelefamIosChatMessage]
    let accentColor: Color
    @State private var filterTab: String = "media"

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                Picker("Category", selection: $filterTab) {
                    Text("Media").tag("media")
                    Text("Links").tag("links")
                    Text("Voice").tag("voice")
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 14)

                List {
                    let filtered = messages.filter { m in
                        switch filterTab {
                        case "links": return !SmartTextEngine.analyze(m.plainText).urls.isEmpty
                        case "voice": return m.messageType == "voice"
                        default: return m.messageType == "sticker" || m.messageType == "gif" || m.messageType == "clip"
                        }
                    }
                    if filtered.isEmpty {
                        Text("No \(filterTab) shared yet")
                            .foregroundColor(.secondary)
                    } else if filterTab == "links" {
                        ForEach(filtered) { m in
                            let urls = SmartTextEngine.analyze(m.plainText).urls
                            ForEach(urls, id: \.normalizedUrl) { u in
                                Button {
                                    let targetUrl = u.normalizedUrl
                                    dismiss()
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.16) {
                                        smartLinksCoordinator.openUrl(targetUrl, source: "chat")
                                    }
                                } label: {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(u.domain.isEmpty ? u.normalizedUrl : u.domain)
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(accentColor)
                                        Text(u.normalizedUrl)
                                            .font(.system(size: 12.5))
                                            .foregroundColor(.primary)
                                            .lineLimit(2)
                                        Text(formatCompactChatTime(m.createdAtMs))
                                            .font(.system(size: 11))
                                            .foregroundColor(.secondary)
                                    }
                                    .padding(.vertical, 2)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    } else {
                        ForEach(filtered) { m in
                            VStack(alignment: .leading, spacing: 3) {
                                Text(m.plainText.isEmpty ? m.messageType.capitalized : m.plainText)
                                    .font(.system(size: 14, weight: .medium))
                                Text(formatCompactChatTime(m.createdAtMs))
                                    .font(.system(size: 12))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Media, Links & Docs")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

private func formatCompactChatTime(_ epochMs: Int64) -> String {
    guard epochMs > 0 else { return "" }
    let d = Date(timeIntervalSince1970: Double(epochMs) / 1000.0)
    let f = DateFormatter()
    f.timeStyle = .short
    return f.string(from: d)
}
