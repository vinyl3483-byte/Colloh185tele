import AVFoundation
import CallKit
import CoreImage
import Network
import ReplayKit
import SwiftUI
import UIKit

// MARK: - Models & Wire Parsers

enum TelefamIosCallType: String, Codable, Equatable {
    case audio
    case video

    var displayLabel: String {
        self == .video ? "Video Call" : "Audio Call"
    }
}

enum TelefamIosCallPhase: String, Equatable {
    case idle
    case checking
    case calling
    case ringing
    case connecting
    case active
    case reconnecting
    case incoming
    case notAllowed
    case ended
}

enum TelefamIosVideoFilter: String, CaseIterable, Identifiable, Equatable {
    case natural = "NATURAL"
    case warm = "WARM"
    case cool = "COOL"
    case noir = "NOIR"
    case vivid = "VIVID"
    case studio = "SOFT"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .natural: return "Natural"
        case .warm: return "Warm"
        case .cool: return "Cool"
        case .noir: return "B&W"
        case .vivid: return "Vivid"
        case .studio: return "Studio"
        }
    }
}

enum TelefamIosCallQualityTier: String, CaseIterable, Identifiable, Equatable {
    case auto = "AUTO"
    case hd720p = "HD_720P"
    case sd480p = "SD_480P"
    case low240p = "LOW_240P"
    case audioPriority = "AUDIO_PRIORITY"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto: return "Auto"
        case .hd720p: return "HD 720p"
        case .sd480p: return "480p"
        case .low240p: return "Data Saver"
        case .audioPriority: return "Audio Priority"
        }
    }
}

struct TelefamIosSwitchVideoPromptState: Equatable {
    let callId: String
    let fromUserId: String
    let fromName: String
}

struct TelefamIosActiveCallState: Equatable {
    var callId: String = ""
    var conversationId: String = ""
    var peerUserId: String = ""
    var peerName: String = ""
    var peerAvatarUrl: String = ""
    var callType: TelefamIosCallType = .audio
    var isOutgoing: Bool = true
    var phase: TelefamIosCallPhase = .idle
    var startedAtMs: Int64 = 0
    var answeredAtMs: Int64 = 0
    var endedAtMs: Int64 = 0
    var backendDurationSec: Int = 0
    var elapsedDisplaySec: Int = 0
    var isMicMuted: Bool = false
    var isSpeakerOn: Bool = false
    var isCameraEnabled: Bool = true
    var isFrontCamera: Bool = true
    var isScreenSharing: Bool = false
    var activeFilter: TelefamIosVideoFilter = .natural
    var qualityTier: TelefamIosCallQualityTier = .auto
    var networkLabel: String = "HD"
    var lowBandwidthWarning: Bool = false
    var localInAppActive: Bool = true
    var remoteInAppActive: Bool = true
    var remoteCamEnabled: Bool = false
    var remoteScreenSharing: Bool = false
    var remoteMicMuted: Bool = false
    var isMinimizedToChat: Bool = false
    var statusText: String = ""
    var notAllowedReason: String = ""
    var autoSentRequestText: String = ""
    var switchVideoPrompt: TelefamIosSwitchVideoPromptState? = nil
    var isSwitchingToVideo: Bool = false
}

struct TelefamIosCallLogBubbleMeta: Equatable {
    let callId: String
    let callType: String
    let outcome: String
    let durationSec: Int
    let initiatorUserId: String
    let summaryText: String
    let canCallBack: Bool
}

enum TelefamIosCallMetaParser {
    static func formatDuration(_ totalSec: Int) -> String {
        let safe = max(0, totalSec)
        let mins = safe / 60
        let secs = safe % 60
        return String(format: "%02d:%02d", mins, secs)
    }

    static func parseCallMeta(plainText: String, messageType: String) -> TelefamIosCallLogBubbleMeta? {
        guard messageType == "call_log" || messageType == "call_request" else { return nil }
        let trimmed = plainText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.hasPrefix("{"),
              let data = trimmed.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              (obj["type"] as? String) == "telefam_call_meta_v1" else {
            return TelefamIosCallLogBubbleMeta(
                callId: "",
                callType: "audio",
                outcome: messageType == "call_request" ? "call_request" : "ended",
                durationSec: 0,
                initiatorUserId: "",
                summaryText: messageType == "call_request"
                    ? "Calls are restricted by privacy settings. Please call me when available."
                    : (trimmed.isEmpty ? "Call log" : trimmed),
                canCallBack: true
            )
        }
        return TelefamIosCallLogBubbleMeta(
            callId: obj["call_id"] as? String ?? "",
            callType: (obj["call_type"] as? String ?? "audio").lowercased(),
            outcome: (obj["outcome"] as? String ?? "ended").lowercased(),
            durationSec: (obj["duration_sec"] as? NSNumber)?.intValue ?? 0,
            initiatorUserId: obj["initiator_user_id"] as? String ?? "",
            summaryText: obj["summary_text"] as? String ?? "",
            canCallBack: (obj["can_call_back"] as? Bool) ?? true
        )
    }

    static func summaryPreview(plainText: String, messageType: String) -> String {
        guard let meta = parseCallMeta(plainText: plainText, messageType: messageType) else {
            return plainText
        }
        if messageType == "call_request" {
            return meta.summaryText.isEmpty ? "Call request" : meta.summaryText
        }
        let kind = meta.callType == "video" ? "Video call" : "Audio call"
        switch meta.outcome {
        case "missed": return "Missed \(kind.lowercased())"
        case "declined": return "Declined \(kind.lowercased())"
        case "not_allowed": return "Calls not allowed"
        default:
            if meta.durationSec > 0 {
                return "\(kind) (\(formatDuration(meta.durationSec)))"
            }
            return kind
        }
    }
}

// MARK: - Push Notification Bridge

@MainActor
final class TelefamIosCallPushBridge {
    static let shared = TelefamIosCallPushBridge()
    weak var controller: TelefamIosCallController?

    private init() {}

    func handleIncomingPush(data: [String: String]) {
        controller?.handleIncomingPush(data: data)
    }

    func handleCallCancelPush(callId: String) {
        controller?.handleRemoteCallCancel(callId: callId)
    }

    func handlePushActionAccept(callId: String) {
        controller?.acceptIncomingCallFromNotification(callId: callId)
    }

    func handlePushActionDecline(callId: String) {
        controller?.declineIncomingCallFromNotification(callId: callId)
    }
}

// MARK: - Sound Synthesizer Engine

@MainActor
final class TelefamIosCallSoundEngine {
    private var ringbackTask: Task<Void, Never>?
    private var ringtonePlayer: AVAudioPlayer?
    private var cuePlayer: AVAudioPlayer?

    func playOutgoingCallingPulse() {
        stopAll()
        ringbackTask = Task { [weak self] in
            while !Task.isCancelled {
                self?.playTone(freqA: 400, freqB: 440, durationMs: 240, gain: 0.14)
                try? await Task.sleep(nanoseconds: 2_300_000_000)
            }
        }
    }

    func playOutgoingRingback() {
        stopAll()
        ringbackTask = Task { [weak self] in
            while !Task.isCancelled {
                self?.playTone(freqA: 440, freqB: 480, durationMs: 950, gain: 0.18)
                try? await Task.sleep(nanoseconds: 3_000_000_000)
            }
        }
    }

    func playIncomingRingtone() {
        stopAll()
        guard let wav = buildDualToneWav(freqA: 523.25, freqB: 659.25, durationMs: 1100, gain: 0.28) else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            let p = try AVAudioPlayer(data: wav)
            p.numberOfLoops = -1
            p.prepareToPlay()
            p.play()
            ringtonePlayer = p
        } catch { }
    }

    func playConnectedChime() {
        stopAll()
        playTone(freqA: 587.33, freqB: 880.0, durationMs: 180, gain: 0.22)
    }

    func playEndedTone() {
        stopAll()
        playTone(freqA: 340, freqB: 260, durationMs: 240, gain: 0.20)
    }

    func playBusyTone() {
        stopAll()
        playTone(freqA: 480, freqB: 620, durationMs: 260, gain: 0.20)
    }

    func stopAll() {
        ringbackTask?.cancel()
        ringbackTask = nil
        ringtonePlayer?.stop()
        ringtonePlayer = nil
    }

    private func playTone(freqA: Double, freqB: Double, durationMs: Int, gain: Double) {
        guard let wav = buildDualToneWav(freqA: freqA, freqB: freqB, durationMs: durationMs, gain: gain) else { return }
        do {
            let p = try AVAudioPlayer(data: wav)
            p.prepareToPlay()
            p.play()
            cuePlayer = p
        } catch { }
    }

    private func buildDualToneWav(freqA: Double, freqB: Double, durationMs: Int, gain: Double) -> Data? {
        let sampleRate = 22050
        let totalSamples = max(1, (sampleRate * durationMs) / 1000)
        var pcm = Data(capacity: 44 + totalSamples * 2)

        func appendU32(_ v: UInt32) {
            var le = v.littleEndian
            pcm.append(Data(bytes: &le, count: 4))
        }
        func appendU16(_ v: UInt16) {
            var le = v.littleEndian
            pcm.append(Data(bytes: &le, count: 2))
        }

        let byteRate = UInt32(sampleRate * 2)
        let dataLen = UInt32(totalSamples * 2)
        pcm.append(contentsOf: "RIFF".utf8)
        appendU32(36 + dataLen)
        pcm.append(contentsOf: "WAVE".utf8)
        pcm.append(contentsOf: "fmt ".utf8)
        appendU32(16)
        appendU16(1)
        appendU16(1)
        appendU32(UInt32(sampleRate))
        appendU32(byteRate)
        appendU16(2)
        appendU16(16)
        pcm.append(contentsOf: "data".utf8)
        appendU32(dataLen)

        let fade = min(totalSamples / 10, 440)
        for i in 0..<totalSamples {
            let t = Double(i) / Double(sampleRate)
            var env = 1.0
            if i < fade { env = Double(i) / Double(max(1, fade)) }
            else if i > totalSamples - fade { env = Double(totalSamples - i) / Double(max(1, fade)) }
            let wave = 0.5 * (sin(2.0 * .pi * freqA * t) + sin(2.0 * .pi * freqB * t))
            let scaled = max(-1.0, min(1.0, wave * env * gain))
            var sample = Int16(scaled * Double(Int16.max)).littleEndian
            pcm.append(Data(bytes: &sample, count: 2))
        }
        return pcm
    }
}

// MARK: - Media Capture, Live Filters, ReplayKit Screen Share & Adaptive Network Monitor

final class TelefamIosCallMediaEngine: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    @Published var localVideoFrame: UIImage? = nil
    @Published var remoteVideoFrame: UIImage? = nil
    @Published var detectedNetworkLabel: String = "HD"
    @Published var isWeakNetwork: Bool = false

    private let sessionQueue = DispatchQueue(label: "telefam.ios.call.capture")
    private var captureSession: AVCaptureSession?
    private let videoOutput = AVCaptureVideoDataOutput()
    private let ciContext = CIContext(options: [.useSoftwareRenderer: false])
    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "telefam.ios.call.nw")

    private var activeFilter: TelefamIosVideoFilter = .natural
    private var qualityTier: TelefamIosCallQualityTier = .auto
    private var isFrontCamera: Bool = true
    private var isVideoEnabled: Bool = false
    private var isScreenSharing: Bool = false
    private var isMicMuted: Bool = false
    private var isSpeakerOn: Bool = false
    private var isRunning: Bool = false

    private var onIceCandidateReady: ((String) -> Void)?
    private var onFrameForTransport: ((Data) -> Void)?
    private var lastFrameSentAt: TimeInterval = 0

    override init() {
        super.init()
        pathMonitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                guard let self else { return }
                let weak = path.status != .satisfied || path.isConstrained || path.isExpensive
                self.isWeakNetwork = weak
                self.detectedNetworkLabel = path.status != .satisfied
                    ? "Reconnecting"
                    : (path.isConstrained ? "Weak" : (path.isExpensive ? "480p" : "HD"))
            }
        }
        pathMonitor.start(queue: monitorQueue)
    }

    deinit {
        pathMonitor.cancel()
        stop()
    }

    func configureSession(
        isVideo: Bool,
        speakerOn: Bool,
        onIceCandidate: @escaping (String) -> Void,
        onFrameData: ((Data) -> Void)? = nil
    ) {
        self.onIceCandidateReady = onIceCandidate
        self.onFrameForTransport = onFrameData
        self.isVideoEnabled = isVideo
        self.isSpeakerOn = speakerOn
        self.isRunning = true

        configureAudioSession(speakerOn: speakerOn)
        gatherStunIceCandidate()

        if isVideo {
            startCameraCapture(front: isFrontCamera)
        }
    }

    func setSpeaker(_ enabled: Bool) {
        isSpeakerOn = enabled
        configureAudioSession(speakerOn: enabled)
    }

    func setMicMuted(_ muted: Bool) {
        isMicMuted = muted
    }

    func setFilter(_ filter: TelefamIosVideoFilter) {
        activeFilter = filter
    }

    func setQualityTier(_ tier: TelefamIosCallQualityTier) {
        qualityTier = tier
        if tier == .audioPriority {
            stopCameraCapture()
        } else if isVideoEnabled && !isScreenSharing && captureSession == nil {
            startCameraCapture(front: isFrontCamera)
        }
    }

    func setVideoEnabled(_ enabled: Bool) {
        isVideoEnabled = enabled
        if enabled && qualityTier != .audioPriority && !isScreenSharing {
            startCameraCapture(front: isFrontCamera)
        } else if !enabled {
            stopCameraCapture()
            DispatchQueue.main.async { self.localVideoFrame = nil }
        }
    }

    func flipCamera() {
        isFrontCamera.toggle()
        if isVideoEnabled && !isScreenSharing {
            stopCameraCapture()
            startCameraCapture(front: isFrontCamera)
        }
    }

    func startScreenShare(completion: @escaping (Bool) -> Void) {
        guard RPScreenRecorder.shared().isAvailable else {
            completion(false)
            return
        }
        stopCameraCapture()
        isScreenSharing = true
        RPScreenRecorder.shared().startCapture(handler: { [weak self] sampleBuffer, bufferType, error in
            guard let self, error == nil, bufferType == .video, self.isScreenSharing else { return }
            self.processSampleBuffer(sampleBuffer, mirror: false)
        }, completionHandler: { [weak self] error in
            DispatchQueue.main.async {
                if error != nil {
                    self?.isScreenSharing = false
                    completion(false)
                } else {
                    completion(true)
                }
            }
        })
    }

    func stopScreenShare() {
        guard isScreenSharing else { return }
        isScreenSharing = false
        RPScreenRecorder.shared().stopCapture { _ in }
        if isVideoEnabled && qualityTier != .audioPriority {
            startCameraCapture(front: isFrontCamera)
        }
    }

    func receiveRemoteFrameData(_ data: Data) {
        guard let image = UIImage(data: data) else { return }
        DispatchQueue.main.async {
            self.remoteVideoFrame = image
        }
    }

    func stop() {
        isRunning = false
        isVideoEnabled = false
        if isScreenSharing {
            isScreenSharing = false
            RPScreenRecorder.shared().stopCapture { _ in }
        }
        stopCameraCapture()
        DispatchQueue.main.async {
            self.localVideoFrame = nil
            self.remoteVideoFrame = nil
        }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func configureAudioSession(speakerOn: Bool) {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .allowBluetoothA2DP])
            try session.setActive(true)
            try session.overrideOutputAudioPort(speakerOn ? .speaker : .none)
        } catch { }
    }

    private func gatherStunIceCandidate() {
        let host = NWEndpoint.Host("stun.l.google.com")
        let port = NWEndpoint.Port(rawValue: 19302)!
        let conn = NWConnection(host: host, port: port, using: .udp)
        conn.stateUpdateHandler = { [weak self] state in
            if case .ready = state {
                let candidate: [String: Any] = [
                    "sdpMid": "0",
                    "sdpMLineIndex": 0,
                    "candidate": "candidate:1 1 UDP 2122252543 127.0.0.1 19302 typ srflx"
                ]
                if let json = try? JSONSerialization.data(withJSONObject: candidate),
                   let str = String(data: json, encoding: .utf8) {
                    self?.onIceCandidateReady?(str)
                }
                conn.cancel()
            } else if case .failed = state {
                conn.cancel()
            }
        }
        conn.start(queue: monitorQueue)
    }

    private func startCameraCapture(front: Bool) {
        sessionQueue.async { [weak self] in
            guard let self, self.isRunning else { return }
            if self.captureSession != nil { return }
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            guard status == .authorized || status == .notDetermined else { return }
            if status == .notDetermined {
                AVCaptureDevice.requestAccess(for: .video) { granted in
                    if granted {
                        self.startCameraCapture(front: front)
                    }
                }
                return
            }

            let session = AVCaptureSession()
            session.beginConfiguration()
            session.sessionPreset = self.qualityTier == .low240p ? .low : (self.qualityTier == .sd480p ? .vga640x480 : .hd1280x720)
            let position: AVCaptureDevice.Position = front ? .front : .back
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
                  let input = try? AVCaptureDeviceInput(device: device),
                  session.canAddInput(input) else {
                session.commitConfiguration()
                return
            }
            session.addInput(input)

            self.videoOutput.alwaysDiscardsLateVideoFrames = true
            self.videoOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
            self.videoOutput.setSampleBufferDelegate(self, queue: self.sessionQueue)
            if session.canAddOutput(self.videoOutput) {
                session.addOutput(self.videoOutput)
            }
            if let connection = self.videoOutput.connection(with: .video), connection.isVideoOrientationSupported {
                connection.videoOrientation = .portrait
            }
            session.commitConfiguration()
            session.startRunning()
            self.captureSession = session
        }
    }

    private func stopCameraCapture() {
        sessionQueue.async { [weak self] in
            self?.captureSession?.stopRunning()
            self?.captureSession = nil
        }
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard isRunning, isVideoEnabled, !isScreenSharing else { return }
        processSampleBuffer(sampleBuffer, mirror: isFrontCamera)
    }

    private func processSampleBuffer(_ sampleBuffer: CMSampleBuffer, mirror: Bool) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        var ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        if mirror {
            ciImage = ciImage.oriented(.upMirrored)
        }
        ciImage = applyFilter(activeFilter, to: ciImage)
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return }
        let uiImage = UIImage(cgImage: cgImage)
        DispatchQueue.main.async {
            self.localVideoFrame = uiImage
        }

        let now = Date().timeIntervalSince1970
        let minInterval: TimeInterval = (qualityTier == .low240p || isWeakNetwork) ? 0.12 : 0.05
        if now - lastFrameSentAt >= minInterval {
            lastFrameSentAt = now
            let q: CGFloat = (qualityTier == .low240p || isWeakNetwork) ? 0.32 : 0.55
            if let jpeg = uiImage.jpegData(compressionQuality: q) {
                onFrameForTransport?(jpeg)
            }
        }
    }

    private func applyFilter(_ filter: TelefamIosVideoFilter, to input: CIImage) -> CIImage {
        switch filter {
        case .natural:
            return input
        case .warm:
            guard let f = CIFilter(name: "CITemperatureAndTint") else { return input }
            f.setValue(input, forKey: kCIInputImageKey)
            f.setValue(CIVector(x: 7800, y: 0), forKey: "inputTargetNeutral")
            return f.outputImage ?? input
        case .cool:
            guard let f = CIFilter(name: "CITemperatureAndTint") else { return input }
            f.setValue(input, forKey: kCIInputImageKey)
            f.setValue(CIVector(x: 4900, y: 0), forKey: "inputTargetNeutral")
            return f.outputImage ?? input
        case .noir:
            guard let f = CIFilter(name: "CIPhotoEffectNoir") else { return input }
            f.setValue(input, forKey: kCIInputImageKey)
            return f.outputImage ?? input
        case .vivid:
            guard let f = CIFilter(name: "CIColorControls") else { return input }
            f.setValue(input, forKey: kCIInputImageKey)
            f.setValue(1.45, forKey: kCIInputSaturationKey)
            f.setValue(1.08, forKey: kCIInputContrastKey)
            return f.outputImage ?? input
        case .studio:
            guard let f = CIFilter(name: "CIColorControls") else { return input }
            f.setValue(input, forKey: kCIInputImageKey)
            f.setValue(0.06, forKey: kCIInputBrightnessKey)
            f.setValue(1.06, forKey: kCIInputContrastKey)
            f.setValue(1.08, forKey: kCIInputSaturationKey)
            return f.outputImage ?? input
        }
    }
}

// MARK: - Main Call Controller + CallKit

@MainActor
final class TelefamIosCallController: NSObject, ObservableObject, CXProviderDelegate {
    @Published var callState = TelefamIosActiveCallState()
    @Published var toastMessage: String? = nil

    let mediaEngine = TelefamIosCallMediaEngine()
    private let soundEngine = TelefamIosCallSoundEngine()

    private let baseURL: URL
    private let userIdProvider: () -> String
    private let tokenProvider: () -> String
    private let deviceFingerprint: String

    private weak var chatStore: TelefamIosChatStore?
    var onOpenConversation: ((String) -> Void)?

    private var ringTimeoutTask: Task<Void, Never>? = nil
    private var elapsedTimerTask: Task<Void, Never>? = nil
    private var isAttached = false

    private let callKitProvider: CXProvider
    private let cxController = CXCallController()
    private var currentCallUUID: UUID? = nil

    init(
        baseURL: URL,
        userIdProvider: @escaping () -> String,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String
    ) {
        self.baseURL = baseURL
        self.userIdProvider = userIdProvider
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint

        let config = CXProviderConfiguration(localizedName: "Telefam")
        config.supportsVideo = true
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.generic]
        self.callKitProvider = CXProvider(configuration: config)
        super.init()
        self.callKitProvider.setDelegate(self, queue: nil)
        TelefamIosCallPushBridge.shared.controller = self
    }

    func attach(chatStore: TelefamIosChatStore, onOpenConversation: @escaping (String) -> Void) {
        self.chatStore = chatStore
        self.onOpenConversation = onOpenConversation
        self.isAttached = true
        chatStore.onCallWsEvent = { [weak self] event, payload in
            self?.handleWsEvent(event: event, payload: payload)
        }
    }

    func detach() {
        isAttached = false
        soundEngine.stopAll()
        mediaEngine.stop()
        ringTimeoutTask?.cancel()
        elapsedTimerTask?.cancel()
        ringTimeoutTask = nil
        elapsedTimerTask = nil
        if let uuid = currentCallUUID {
            callKitProvider.reportCall(with: uuid, endedAt: Date(), reason: .remoteEnded)
            currentCallUUID = nil
        }
        callState = TelefamIosActiveCallState()
    }

    // MARK: - Start / Incoming / Accept / Decline / HangUp

    func startCall(
        conversationId: String,
        peerUserId: String,
        peerName: String,
        peerAvatarUrl: String,
        callType: TelefamIosCallType
    ) {
        guard isAttached, callState.phase == .idle || callState.phase == .ended || callState.phase == .notAllowed else {
            return
        }
        let myId = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !myId.isEmpty else { return }

        Task {
            let granted = await ensureMediaPermissions(isVideo: callType == .video)
            if !granted {
                showToast("Microphone\(callType == .video ? " & camera" : "") permission required to place call.")
                return
            }

            let now = Int64(Date().timeIntervalSince1970 * 1000)
            callState = TelefamIosActiveCallState(
                callId: "",
                conversationId: conversationId,
                peerUserId: peerUserId,
                peerName: peerName.isEmpty ? "Telefam" : peerName,
                peerAvatarUrl: peerAvatarUrl,
                callType: callType,
                isOutgoing: true,
                phase: .calling,
                startedAtMs: now,
                isSpeakerOn: callType == .video,
                isCameraEnabled: callType == .video,
                localInAppActive: true,
                statusText: "Calling..."
            )
            soundEngine.playOutgoingCallingPulse()

            let body: [String: Any] = [
                "conversation_id": conversationId,
                "peer_user_id": peerUserId,
                "call_type": callType.rawValue
            ]
            guard let (data, http) = await request(path: "v1/calls/start", method: "POST", jsonBody: body),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                soundEngine.playEndedTone()
                callState = TelefamIosActiveCallState()
                showToast("Could not connect call right now.")
                return
            }

            let code = obj["code"] as? String ?? ""
            let allowed = (obj["allowed"] as? Bool) ?? (http.statusCode != 403 && code != "calls_not_allowed")
            if !allowed {
                soundEngine.playBusyTone()
                let reason = (obj["reason"] as? String ?? "").isEmpty ? "Calls not allowed by privacy settings." : (obj["reason"] as? String ?? "")
                let autoText = obj["auto_request_text"] as? String ?? "Calls are not allowed. Please call me back when you're available."
                callState = TelefamIosActiveCallState(
                    conversationId: conversationId,
                    peerUserId: peerUserId,
                    peerName: peerName.isEmpty ? "Telefam" : peerName,
                    peerAvatarUrl: peerAvatarUrl,
                    callType: callType,
                    isOutgoing: true,
                    phase: .notAllowed,
                    statusText: "Calls not allowed",
                    notAllowedReason: reason,
                    autoSentRequestText: autoText
                )
                await chatStore?.refreshConversationFromCall(conversationId)
                return
            }

            guard (200...299).contains(http.statusCode),
                  let callObj = obj["call"] as? [String: Any],
                  let callId = callObj["call_id"] as? String, !callId.isEmpty else {
                soundEngine.playEndedTone()
                callState = TelefamIosActiveCallState()
                showToast("Call failed to start.")
                return
            }

            let peerOnline = callObj["peer_online"] as? Bool ?? false
            callState.callId = callId
            callState.phase = peerOnline ? .ringing : .calling
            callState.statusText = peerOnline ? "Ringing..." : "Calling..."
            if peerOnline {
                soundEngine.playOutgoingRingback()
            }

            let uuid = UUID()
            currentCallUUID = uuid
            let handle = CXHandle(type: .generic, value: peerUserId)
            let startAction = CXStartCallAction(call: uuid, handle: handle)
            startAction.isVideo = (callType == .video)
            try? await cxController.request(CXTransaction(action: startAction))

            mediaEngine.configureSession(
                isVideo: callType == .video,
                speakerOn: callType == .video,
                onIceCandidate: { [weak self] cand in
                    self?.sendCallSignal(callId: callId, action: "ice", extras: ["candidate": cand])
                }
            )
            let sdpOffer = buildMinimalSdpOffer(isVideo: callType == .video)
            sendCallSignal(callId: callId, action: "offer", extras: ["sdp": sdpOffer])
            startRingTimeoutTask(callId: callId)
        }
    }

    func handleIncomingPush(data: [String: String]) {
        guard isAttached else { return }
        let callId = (data["call_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !callId.isEmpty else { return }
        let convId = (data["conversation_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let callerId = (data["caller_user_id"] ?? data["actor_user_id"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let callerName = (data["caller_name"] ?? data["actor_name"] ?? "Telefam").trimmingCharacters(in: .whitespacesAndNewlines)
        let callerAvatar = (data["caller_avatar_url"] ?? data["actor_avatar_url"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let callType: TelefamIosCallType = (data["call_type"] ?? "").lowercased() == "video" ? .video : .audio
        presentIncomingCall(
            callId: callId,
            conversationId: convId,
            callerUserId: callerId,
            callerName: callerName,
            callerAvatarUrl: callerAvatar,
            callType: callType
        )
    }

    func acceptIncomingCallFromNotification(callId: String) {
        if callState.callId == callId && callState.phase == .incoming {
            acceptIncomingCall()
        }
    }

    func declineIncomingCallFromNotification(callId: String) {
        if callState.callId == callId && callState.phase == .incoming {
            declineIncomingCall()
        } else if !callId.isEmpty {
            sendCallSignal(callId: callId, action: "decline")
        }
    }

    func handleRemoteCallCancel(callId: String) {
        if callState.callId == callId && callState.phase == .incoming {
            finishLocalCall(statusLabel: "Missed call", reason: .unanswered)
        }
    }

    func acceptIncomingCall() {
        let cid = callState.callId
        guard !cid.isEmpty, callState.phase == .incoming else { return }
        ringTimeoutTask?.cancel()
        soundEngine.stopAll()

        Task {
            let isVideo = callState.callType == .video
            let granted = await ensureMediaPermissions(isVideo: isVideo)
            if !granted {
                declineIncomingCall()
                showToast("Permission denied for call.")
                return
            }

            callState.phase = .connecting
            callState.statusText = "Connecting..."
            callState.isCameraEnabled = isVideo
            callState.isSpeakerOn = isVideo
            callState.localInAppActive = true

            mediaEngine.configureSession(
                isVideo: isVideo,
                speakerOn: isVideo,
                onIceCandidate: { [weak self] cand in
                    self?.sendCallSignal(callId: cid, action: "ice", extras: ["candidate": cand])
                }
            )
            let answerSdp = buildMinimalSdpOffer(isVideo: isVideo)
            sendCallSignal(callId: cid, action: "accept", extras: [
                "sdp": answerSdp,
                "cam_enabled": isVideo,
                "in_app_active": true
            ])
        }
    }

    func declineIncomingCall() {
        let cid = callState.callId
        ringTimeoutTask?.cancel()
        soundEngine.playEndedTone()
        if !cid.isEmpty {
            sendCallSignal(callId: cid, action: "decline")
        }
        finishLocalCall(statusLabel: "Declined", reason: .declinedElsewhere)
    }

    func hangUp(reason: String = "hangup") {
        let cid = callState.callId
        ringTimeoutTask?.cancel()
        elapsedTimerTask?.cancel()
        soundEngine.playEndedTone()
        if !cid.isEmpty {
            let action = (callState.isOutgoing && (callState.phase == .calling || callState.phase == .ringing)) ? "cancel" : "end"
            sendCallSignal(callId: cid, action: action, extras: ["reason": reason])
        }
        finishLocalCall(statusLabel: "Call ended", reason: .remoteEnded)
    }

    func dismissNotAllowedModal() {
        callState = TelefamIosActiveCallState()
    }

    // MARK: - Media Controls, Filters, Quality & Screen Share

    func toggleMic() {
        guard !callState.callId.isEmpty else { return }
        let next = !callState.isMicMuted
        callState.isMicMuted = next
        mediaEngine.setMicMuted(next)
        sendMediaStateSignal()
    }

    func toggleSpeaker() {
        let next = !callState.isSpeakerOn
        callState.isSpeakerOn = next
        mediaEngine.setSpeaker(next)
    }

    func toggleCamera() {
        guard !callState.callId.isEmpty, callState.callType == .video else { return }
        if callState.isScreenSharing {
            mediaEngine.stopScreenShare()
            callState.isScreenSharing = false
        }
        let next = !callState.isCameraEnabled
        callState.isCameraEnabled = next
        mediaEngine.setVideoEnabled(next)
        sendMediaStateSignal()
    }

    func flipCamera() {
        guard callState.callType == .video, callState.isCameraEnabled, !callState.isScreenSharing else { return }
        callState.isFrontCamera.toggle()
        mediaEngine.flipCamera()
    }

    func toggleScreenShare() {
        guard !callState.callId.isEmpty else { return }
        if callState.isScreenSharing {
            mediaEngine.stopScreenShare()
            callState.isScreenSharing = false
            callState.isCameraEnabled = (callState.callType == .video)
            sendMediaStateSignal()
        } else {
            mediaEngine.startScreenShare { [weak self] started in
                guard let self else { return }
                if started {
                    self.callState.isScreenSharing = true
                    self.callState.isCameraEnabled = false
                    self.sendMediaStateSignal()
                } else {
                    self.showToast("Screen sharing unavailable on this device.")
                }
            }
        }
    }

    func selectFilter(_ filter: TelefamIosVideoFilter) {
        callState.activeFilter = filter
        mediaEngine.setFilter(filter)
    }

    func setQualityTier(_ tier: TelefamIosCallQualityTier) {
        callState.qualityTier = tier
        callState.networkLabel = tier == .auto ? mediaEngine.detectedNetworkLabel : tier.label
        callState.lowBandwidthWarning = mediaEngine.isWeakNetwork || tier == .low240p || tier == .audioPriority
        mediaEngine.setQualityTier(tier)
    }

    func requestSwitchToVideo() {
        let cid = callState.callId
        guard !cid.isEmpty, callState.callType == .audio, !callState.isSwitchingToVideo else { return }
        callState.isSwitchingToVideo = true
        sendCallSignal(callId: cid, action: "switch_video_request")
        showToast("Requested switch to video call...")
    }

    func acceptSwitchToVideo() {
        let cid = callState.callId
        guard !cid.isEmpty else { return }
        callState.switchVideoPrompt = nil
        Task {
            let granted = await ensureMediaPermissions(isVideo: true)
            if !granted {
                declineSwitchToVideo()
                return
            }
            callState.callType = .video
            callState.isCameraEnabled = true
            callState.isSpeakerOn = true
            callState.localInAppActive = true
            mediaEngine.setSpeaker(true)
            mediaEngine.setVideoEnabled(true)
            sendCallSignal(callId: cid, action: "switch_video_accept", extras: [
                "cam_enabled": true,
                "in_app_active": true
            ])
        }
    }

    func declineSwitchToVideo() {
        let cid = callState.callId
        callState.switchVideoPrompt = nil
        guard !cid.isEmpty else { return }
        sendCallSignal(callId: cid, action: "switch_video_decline")
    }

    func minimizeToChat() {
        guard !callState.callId.isEmpty else { return }
        callState.isMinimizedToChat = true
        callState.localInAppActive = false
        if callState.callType == .video && !callState.isScreenSharing {
            mediaEngine.setVideoEnabled(false)
        }
        sendMediaStateSignal()
        if !callState.conversationId.isEmpty {
            onOpenConversation?(callState.conversationId)
        }
    }

    func expandFromMinimized() {
        guard !callState.callId.isEmpty else { return }
        callState.isMinimizedToChat = false
        callState.localInAppActive = true
        if callState.callType == .video && callState.isCameraEnabled && !callState.isScreenSharing {
            mediaEngine.setVideoEnabled(true)
        }
        sendMediaStateSignal()
    }

    func onScenePhaseChanged(_ scenePhase: ScenePhase) {
        guard !callState.callId.isEmpty,
              callState.phase == .active || callState.phase == .connecting || callState.phase == .reconnecting else {
            return
        }
        switch scenePhase {
        case .background, .inactive:
            if callState.localInAppActive {
                callState.localInAppActive = false
                if callState.callType == .video && !callState.isScreenSharing {
                    mediaEngine.setVideoEnabled(false)
                }
                sendMediaStateSignal()
            }
        case .active:
            if !callState.isMinimizedToChat && !callState.localInAppActive {
                callState.localInAppActive = true
                if callState.callType == .video && callState.isCameraEnabled && !callState.isScreenSharing {
                    mediaEngine.setVideoEnabled(true)
                }
                sendMediaStateSignal()
            }
        @unknown default:
            break
        }
    }

    // MARK: - WS Dispatch

    func handleWsEvent(event: String, payload: [String: Any]) {
        guard isAttached else { return }
        let cid = payload["call_id"] as? String ?? ""
        switch event {
        case "call_incoming":
            let convId = payload["conversation_id"] as? String ?? ""
            let callerId = payload["caller_user_id"] as? String ?? payload["from_user_id"] ?? ""
            let callerName = payload["caller_name"] as? String ?? "Telefam"
            let callerAvatar = payload["caller_avatar_url"] as? String ?? ""
            let type: TelefamIosCallType = (payload["call_type"] as? String ?? "").lowercased() == "video" ? .video : .audio
            presentIncomingCall(
                callId: cid,
                conversationId: convId,
                callerUserId: callerId as? String ?? "",
                callerName: callerName,
                callerAvatarUrl: callerAvatar,
                callType: type
            )

        case "call_ringing":
            if callState.callId == cid && callState.phase == .calling {
                callState.phase = .ringing
                callState.statusText = "Ringing..."
                soundEngine.playOutgoingRingback()
            }

        case "call_accepted":
            if callState.callId == cid {
                markCallConnected(
                    callId: cid,
                    remoteCam: payload["cam_enabled"] as? Bool ?? (callState.callType == .video),
                    remoteInApp: payload["in_app_active"] as? Bool ?? true
                )
            }

        case "call_signal":
            guard callState.callId == cid else { return }
            let action = (payload["action"] as? String ?? "").lowercased()
            switch action {
            case "offer", "answer":
                markCallConnected(
                    callId: cid,
                    remoteCam: payload["cam_enabled"] as? Bool ?? callState.remoteCamEnabled,
                    remoteInApp: payload["in_app_active"] as? Bool ?? true
                )
            case "media_state":
                if let cam = payload["cam_enabled"] as? Bool { callState.remoteCamEnabled = cam }
                if let active = payload["in_app_active"] as? Bool { callState.remoteInAppActive = active }
                if let mic = payload["mic_muted"] as? Bool { callState.remoteMicMuted = mic }
                if let screen = payload["screen_sharing"] as? Bool { callState.remoteScreenSharing = screen }
            case "switch_video_request":
                let fromUid = payload["from_user_id"] as? String ?? callState.peerUserId
                callState.switchVideoPrompt = TelefamIosSwitchVideoPromptState(
                    callId: cid,
                    fromUserId: fromUid,
                    fromName: callState.peerName.isEmpty ? "Caller" : callState.peerName
                )
            case "switch_video_accept":
                callState.isSwitchingToVideo = false
                callState.callType = .video
                callState.isCameraEnabled = true
                callState.isSpeakerOn = true
                callState.remoteCamEnabled = payload["cam_enabled"] as? Bool ?? true
                callState.remoteInAppActive = payload["in_app_active"] as? Bool ?? true
                mediaEngine.setSpeaker(true)
                mediaEngine.setVideoEnabled(true)
                showToast("Switched to video call")
            case "switch_video_decline":
                callState.isSwitchingToVideo = false
                showToast("Peer declined video switch")
            default:
                break
            }

        case "call_ended":
            let convId = payload["conversation_id"] as? String ?? callState.conversationId
            let durationSec = (payload["duration_sec"] as? NSNumber)?.intValue ?? 0
            let outcome = (payload["outcome"] as? String ?? "ended").lowercased()
            if callState.callId == cid || (!cid.isEmpty && callState.callId.isEmpty && callState.conversationId == convId) {
                callState.backendDurationSec = durationSec
                let label: String = {
                    switch outcome {
                    case "missed": return "No answer"
                    case "declined": return "Call declined"
                    default: return durationSec > 0 ? "Ended · \(TelefamIosCallMetaParser.formatDuration(durationSec))" : "Call ended"
                    }
                }()
                finishLocalCall(statusLabel: label, reason: outcome == "missed" ? .unanswered : .remoteEnded)
            }
            if !convId.isEmpty {
                Task { await chatStore?.refreshConversationFromCall(convId) }
            }

        default:
            break
        }
    }

    // MARK: - CXProviderDelegate

    nonisolated func providerDidReset(_ provider: CXProvider) {
        Task { @MainActor in
            self.finishLocalCall(statusLabel: "Call ended", reason: .failed)
        }
    }

    nonisolated func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        Task { @MainActor in
            self.acceptIncomingCall()
        }
        action.fulfill()
    }

    nonisolated func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        Task { @MainActor in
            if self.callState.phase == .incoming {
                self.declineIncomingCall()
            } else if !self.callState.callId.isEmpty {
                self.hangUp(reason: "callkit_end")
            }
        }
        action.fulfill()
    }

    // MARK: - Internal Helpers

    private func presentIncomingCall(
        callId: String,
        conversationId: String,
        callerUserId: String,
        callerName: String,
        callerAvatarUrl: String,
        callType: TelefamIosCallType
    ) {
        if !callState.callId.isEmpty && callState.callId != callId && callState.phase != .idle && callState.phase != .ended {
            return
        }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        callState = TelefamIosActiveCallState(
            callId: callId,
            conversationId: conversationId,
            peerUserId: callerUserId,
            peerName: callerName.isEmpty ? "Telefam" : callerName,
            peerAvatarUrl: callerAvatarUrl,
            callType: callType,
            isOutgoing: false,
            phase: .incoming,
            startedAtMs: now,
            isSpeakerOn: callType == .video,
            isCameraEnabled: callType == .video,
            localInAppActive: true,
            statusText: "Incoming \(callType.displayLabel.lowercased())"
        )
        soundEngine.playIncomingRingtone()
        sendCallSignal(callId: callId, action: "ringing")
        startRingTimeoutTask(callId: callId)

        let uuid = UUID()
        currentCallUUID = uuid
        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: callerUserId)
        update.localizedCallerName = callerName
        update.hasVideo = (callType == .video)
        callKitProvider.reportNewIncomingCall(with: uuid, update: update) { _ in }
    }

    private func markCallConnected(callId: String, remoteCam: Bool, remoteInApp: Bool) {
        ringTimeoutTask?.cancel()
        soundEngine.playConnectedChime()
        if callState.answeredAtMs == 0 {
            callState.answeredAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        }
        callState.phase = .active
        callState.remoteCamEnabled = remoteCam
        callState.remoteInAppActive = remoteInApp
        callState.statusText = TelefamIosCallMetaParser.formatDuration(callState.elapsedDisplaySec)

        if elapsedTimerTask == nil {
            elapsedTimerTask = Task { [weak self] in
                while !Task.isCancelled {
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                    guard let self, self.callState.phase == .active else { break }
                    self.callState.elapsedDisplaySec += 1
                    self.callState.statusText = TelefamIosCallMetaParser.formatDuration(self.callState.elapsedDisplaySec)
                    if self.callState.qualityTier == .auto {
                        self.callState.networkLabel = self.mediaEngine.detectedNetworkLabel
                        self.callState.lowBandwidthWarning = self.mediaEngine.isWeakNetwork
                    }
                }
            }
        }
    }

    private func startRingTimeoutTask(callId: String) {
        ringTimeoutTask?.cancel()
        ringTimeoutTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 25_000_000_000)
            guard !Task.isCancelled, let self else { return }
            if self.callState.callId == callId &&
                (self.callState.phase == .calling || self.callState.phase == .ringing || self.callState.phase == .incoming) {
                self.soundEngine.playEndedTone()
                self.sendCallSignal(callId: callId, action: "end", extras: ["reason": "timeout_25s"])
                self.finishLocalCall(statusLabel: "No answer", reason: .unanswered)
            }
        }
    }

    private func finishLocalCall(statusLabel: String, reason: CXCallEndedReason) {
        ringTimeoutTask?.cancel()
        ringTimeoutTask = nil
        elapsedTimerTask?.cancel()
        elapsedTimerTask = nil
        soundEngine.stopAll()
        mediaEngine.stop()

        if let uuid = currentCallUUID {
            callKitProvider.reportCall(with: uuid, endedAt: Date(), reason: reason)
            currentCallUUID = nil
        }

        let convId = callState.conversationId
        callState.phase = .ended
        callState.statusText = statusLabel
        callState.switchVideoPrompt = nil

        if !convId.isEmpty {
            Task { await chatStore?.refreshConversationFromCall(convId) }
        }
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: 900_000_000)
            guard let self else { return }
            if self.callState.phase == .ended {
                self.callState = TelefamIosActiveCallState()
            }
        }
    }

    private func sendMediaStateSignal() {
        let cid = callState.callId
        guard !cid.isEmpty else { return }
        sendCallSignal(callId: cid, action: "media_state", extras: [
            "cam_enabled": callState.isCameraEnabled && !callState.isScreenSharing && callState.localInAppActive,
            "in_app_active": callState.localInAppActive,
            "mic_muted": callState.isMicMuted,
            "screen_sharing": callState.isScreenSharing
        ])
    }

    private func sendCallSignal(callId: String, action: String, extras: [String: Any] = [:]) {
        guard !callId.isEmpty else { return }
        var body: [String: Any] = ["action": action]
        for (k, v) in extras { body[k] = v }
        chatStore?.sendCallSignalWs(callId: callId, payload: body)
        Task {
            _ = await request(path: "v1/calls/\(callId)/signal", method: "POST", jsonBody: body)
        }
    }

    private func ensureMediaPermissions(isVideo: Bool) async -> Bool {
        let micStatus = AVCaptureDevice.authorizationStatus(for: .audio)
        let micOk: Bool
        if micStatus == .authorized {
            micOk = true
        } else if micStatus == .notDetermined {
            micOk = await AVCaptureDevice.requestAccess(for: .audio)
        } else {
            micOk = false
        }
        guard micOk else { return false }
        guard isVideo else { return true }
        let camStatus = AVCaptureDevice.authorizationStatus(for: .video)
        if camStatus == .authorized { return true }
        if camStatus == .notDetermined {
            return await AVCaptureDevice.requestAccess(for: .video)
        }
        return false
    }

    private func buildMinimalSdpOffer(isVideo: Bool) -> String {
        "v=0\no=telefam-ios 1 1 IN IP4 127.0.0.1\ns=Telefam\nt=0 0\nm=audio 9 RTP/AVP 111" + (isVideo ? "\nm=video 9 RTP/AVP 96" : "")
    }

    private func request(path: String, method: String, jsonBody: [String: Any]) async -> (Data, HTTPURLResponse)? {
        let tok = tokenProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !tok.isEmpty else { return nil }
        let url = baseURL.appendingPathComponent(path)
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try? JSONSerialization.data(withJSONObject: jsonBody)
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else { return nil }
        return (data, http)
    }

    private func showToast(_ msg: String) {
        toastMessage = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) { [weak self] in
            if self?.toastMessage == msg { self?.toastMessage = nil }
        }
    }
}

// MARK: - Pure Vector SVG Call Icons (Zero Emojis)

private struct TelefamCallVectorIcon: View {
    enum Kind {
        case phone, video, callEnd, mic, micOff, videoOff, speakerOn, flipCamera
        case screenShare, filters, expand, qualitySignal, message, check, close, shieldAlert
    }

    let kind: Kind
    var color: Color = .white
    var size: CGFloat = 22

    var body: some View {
        Canvas { ctx, canvasSize in
            let w = canvasSize.width
            let h = canvasSize.height
            let stroke = StrokeStyle(lineWidth: max(1.7, w * 0.088), lineCap: .round, lineJoin: .round)

            switch kind {
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

            case .video, .videoOff:
                ctx.stroke(
                    Path(roundedRect: CGRect(x: w * 0.10, y: h * 0.28, width: w * 0.54, height: h * 0.44), cornerRadius: w * 0.10),
                    with: .color(color),
                    style: stroke
                )
                var lens = Path()
                lens.move(to: CGPoint(x: w * 0.64, y: h * 0.42))
                lens.addLine(to: CGPoint(x: w * 0.88, y: h * 0.28))
                lens.addLine(to: CGPoint(x: w * 0.88, y: h * 0.72))
                lens.addLine(to: CGPoint(x: w * 0.64, y: h * 0.58))
                lens.closeSubpath()
                ctx.stroke(lens, with: .color(color), style: stroke)
                if kind == .videoOff {
                    var slash = Path()
                    slash.move(to: CGPoint(x: w * 0.14, y: h * 0.86))
                    slash.addLine(to: CGPoint(x: w * 0.86, y: h * 0.14))
                    ctx.stroke(slash, with: .color(color), style: stroke)
                }

            case .callEnd:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.12, y: h * 0.56))
                p.addQuadCurve(to: CGPoint(x: w * 0.88, y: h * 0.56), control: CGPoint(x: w * 0.50, y: h * 0.30))
                p.addLine(to: CGPoint(x: w * 0.80, y: h * 0.68))
                p.addLine(to: CGPoint(x: w * 0.66, y: h * 0.64))
                p.addLine(to: CGPoint(x: w * 0.66, y: h * 0.54))
                p.move(to: CGPoint(x: w * 0.12, y: h * 0.56))
                p.addLine(to: CGPoint(x: w * 0.20, y: h * 0.68))
                p.addLine(to: CGPoint(x: w * 0.34, y: h * 0.64))
                p.addLine(to: CGPoint(x: w * 0.34, y: h * 0.54))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .mic, .micOff:
                ctx.stroke(
                    Path(roundedRect: CGRect(x: w * 0.36, y: h * 0.14, width: w * 0.28, height: h * 0.44), cornerRadius: w * 0.14),
                    with: .color(color),
                    style: stroke
                )
                var arc = Path()
                arc.move(to: CGPoint(x: w * 0.24, y: h * 0.46))
                arc.addQuadCurve(to: CGPoint(x: w * 0.76, y: h * 0.46), control: CGPoint(x: w * 0.50, y: h * 0.80))
                arc.move(to: CGPoint(x: w * 0.50, y: h * 0.66))
                arc.addLine(to: CGPoint(x: w * 0.50, y: h * 0.86))
                arc.move(to: CGPoint(x: w * 0.36, y: h * 0.86))
                arc.addLine(to: CGPoint(x: w * 0.64, y: h * 0.86))
                ctx.stroke(arc, with: .color(color), style: stroke)
                if kind == .micOff {
                    var slash = Path()
                    slash.move(to: CGPoint(x: w * 0.16, y: h * 0.84))
                    slash.addLine(to: CGPoint(x: w * 0.84, y: h * 0.16))
                    ctx.stroke(slash, with: .color(color), style: stroke)
                }

            case .speakerOn:
                var sp = Path()
                sp.move(to: CGPoint(x: w * 0.14, y: h * 0.38))
                sp.addLine(to: CGPoint(x: w * 0.34, y: h * 0.38))
                sp.addLine(to: CGPoint(x: w * 0.54, y: h * 0.20))
                sp.addLine(to: CGPoint(x: w * 0.54, y: h * 0.80))
                sp.addLine(to: CGPoint(x: w * 0.34, y: h * 0.62))
                sp.addLine(to: CGPoint(x: w * 0.14, y: h * 0.62))
                sp.closeSubpath()
                ctx.stroke(sp, with: .color(color), style: stroke)
                var wv = Path()
                wv.move(to: CGPoint(x: w * 0.68, y: h * 0.35))
                wv.addQuadCurve(to: CGPoint(x: w * 0.68, y: h * 0.65), control: CGPoint(x: w * 0.79, y: h * 0.50))
                wv.move(to: CGPoint(x: w * 0.80, y: h * 0.24))
                wv.addQuadCurve(to: CGPoint(x: w * 0.80, y: h * 0.76), control: CGPoint(x: w * 0.94, y: h * 0.50))
                ctx.stroke(wv, with: .color(color), style: stroke)

            case .flipCamera:
                var p = Path()
                p.addArc(center: CGPoint(x: w * 0.50, y: h * 0.50), radius: w * 0.32, startAngle: .degrees(20), endAngle: .degrees(160), clockwise: false)
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.42))
                p.addArc(center: CGPoint(x: w * 0.50, y: h * 0.50), radius: w * 0.32, startAngle: .degrees(200), endAngle: .degrees(340), clockwise: false)
                ctx.stroke(p, with: .color(color), style: stroke)

            case .screenShare:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.12, y: h * 0.20, width: w * 0.76, height: h * 0.54), cornerRadius: w * 0.08), with: .color(color), style: stroke)
                var arr = Path()
                arr.move(to: CGPoint(x: w * 0.50, y: h * 0.62))
                arr.addLine(to: CGPoint(x: w * 0.50, y: h * 0.34))
                arr.move(to: CGPoint(x: w * 0.38, y: h * 0.44))
                arr.addLine(to: CGPoint(x: w * 0.50, y: h * 0.34))
                arr.addLine(to: CGPoint(x: w * 0.62, y: h * 0.44))
                arr.move(to: CGPoint(x: w * 0.34, y: h * 0.84))
                arr.addLine(to: CGPoint(x: w * 0.66, y: h * 0.84))
                ctx.stroke(arr, with: .color(color), style: stroke)

            case .filters:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.14, width: w * 0.40, height: h * 0.40)), with: .color(color), style: stroke)
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.42, y: h * 0.38, width: w * 0.40, height: h * 0.40)), with: .color(color), style: stroke)
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.18, y: h * 0.46, width: w * 0.40, height: h * 0.40)), with: .color(color), style: stroke)

            case .expand:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.58, y: h * 0.18))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.18))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.42))
                p.move(to: CGPoint(x: w * 0.82, y: h * 0.18))
                p.addLine(to: CGPoint(x: w * 0.54, y: h * 0.46))
                p.move(to: CGPoint(x: w * 0.42, y: h * 0.82))
                p.addLine(to: CGPoint(x: w * 0.18, y: h * 0.82))
                p.addLine(to: CGPoint(x: w * 0.18, y: h * 0.58))
                p.move(to: CGPoint(x: w * 0.18, y: h * 0.82))
                p.addLine(to: CGPoint(x: w * 0.46, y: h * 0.54))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .qualitySignal:
                for idx in 0..<4 {
                    let frac = Double(idx + 1) / 4.0
                    let x = w * (0.18 + CGFloat(idx) * 0.18)
                    let barH = h * CGFloat(0.24 + frac * 0.48)
                    let rect = CGRect(x: x, y: h * 0.84 - barH, width: w * 0.11, height: barH)
                    ctx.fill(Path(roundedRect: rect, cornerRadius: w * 0.04), with: .color(color))
                }

            case .message:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.14, y: h * 0.20, width: w * 0.72, height: h * 0.52), cornerRadius: w * 0.12), with: .color(color), style: stroke)
                var tail = Path()
                tail.move(to: CGPoint(x: w * 0.30, y: h * 0.72))
                tail.addLine(to: CGPoint(x: w * 0.22, y: h * 0.86))
                tail.addLine(to: CGPoint(x: w * 0.48, y: h * 0.72))
                ctx.stroke(tail, with: .color(color), style: stroke)

            case .check:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.18, y: h * 0.52))
                p.addLine(to: CGPoint(x: w * 0.40, y: h * 0.74))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.26))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .close:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.78))
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.78))
                ctx.stroke(p, with: .color(color), style: stroke)

            case .shieldAlert:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.50, y: h * 0.12))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.26))
                p.addQuadCurve(to: CGPoint(x: w * 0.50, y: h * 0.88), control: CGPoint(x: w * 0.82, y: h * 0.66))
                p.addQuadCurve(to: CGPoint(x: w * 0.18, y: h * 0.26), control: CGPoint(x: w * 0.18, y: h * 0.66))
                p.closeSubpath()
                ctx.stroke(p, with: .color(color), style: stroke)
                var excl = Path()
                excl.move(to: CGPoint(x: w * 0.50, y: h * 0.34))
                excl.addLine(to: CGPoint(x: w * 0.50, y: h * 0.56))
                ctx.stroke(excl, with: .color(color), style: stroke)
                ctx.fill(Path(ellipseIn: CGRect(x: w * 0.46, y: h * 0.64, width: w * 0.08, height: w * 0.08)), with: .color(color))
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Responsive Call Overlay Host (iPhone / iPad / Split-View)

struct TelefamIosCallOverlayHost: View {
    @ObservedObject var controller: TelefamIosCallController
    let authBearerToken: String
    let authDeviceFingerprint: String

    var body: some View {
        let state = controller.callState
        ZStack(alignment: .top) {
            if state.phase == .notAllowed {
                TelefamIosCallsNotAllowedModal(
                    state: state,
                    onDismiss: { controller.dismissNotAllowedModal() }
                )
                .zIndex(98)
            } else if state.phase == .incoming {
                TelefamIosIncomingCallView(
                    state: state,
                    authBearerToken: authBearerToken,
                    authDeviceFingerprint: authDeviceFingerprint,
                    onAccept: { controller.acceptIncomingCall() },
                    onDecline: { controller.declineIncomingCall() }
                )
                .zIndex(98)
            } else if state.phase != .idle {
                if state.isMinimizedToChat {
                    VStack {
                        TelefamIosMinimizedCallBar(
                            state: state,
                            onExpand: { controller.expandFromMinimized() },
                            onToggleMic: { controller.toggleMic() },
                            onHangUp: { controller.hangUp() }
                        )
                        .padding(.horizontal, 12)
                        .padding(.top, 8)
                        Spacer()
                    }
                    .zIndex(97)
                } else {
                    TelefamIosActiveCallView(
                        controller: controller,
                        state: state,
                        authBearerToken: authBearerToken,
                        authDeviceFingerprint: authDeviceFingerprint
                    )
                    .zIndex(98)
                }
            }
        }
        .animation(.easeInOut(duration: 0.22), value: state.phase)
        .animation(.easeInOut(duration: 0.20), value: state.isMinimizedToChat)
    }
}

private struct TelefamIosMinimizedCallBar: View {
    let state: TelefamIosActiveCallState
    let onExpand: () -> Void
    let onToggleMic: () -> Void
    let onHangUp: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Button(action: onExpand) {
                HStack(spacing: 10) {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 9, height: 9)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(state.peerName.isEmpty ? "Telefam Call" : state.peerName)
                            .font(.system(size: 13.5, weight: .bold))
                            .foregroundColor(.white)
                            .lineLimit(1)
                        Text(state.statusText.isEmpty ? state.callType.displayLabel : state.statusText)
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundColor(.white.opacity(0.78))
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
            }
            .buttonStyle(.plain)

            Button(action: onToggleMic) {
                TelefamCallVectorIcon(kind: state.isMicMuted ? .micOff : .mic, color: .white, size: 18)
                    .frame(width: 44, height: 44)
                    .background(state.isMicMuted ? Color.white.opacity(0.25) : Color.white.opacity(0.14))
                    .clipShape(Circle())
            }

            Button(action: onExpand) {
                TelefamCallVectorIcon(kind: .expand, color: .white, size: 18)
                    .frame(width: 44, height: 44)
                    .background(Color.white.opacity(0.14))
                    .clipShape(Circle())
            }

            Button(action: onHangUp) {
                TelefamCallVectorIcon(kind: .callEnd, color: .white, size: 18)
                    .frame(width: 44, height: 44)
                    .background(TelefamPalette.crimson)
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
        .background(Color(red: 0.08, green: 0.10, blue: 0.14).opacity(0.96))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: Color.black.opacity(0.32), radius: 10, y: 4)
    }
}

private struct TelefamIosCallsNotAllowedModal: View {
    @Environment(\.colorScheme) private var colorScheme
    let state: TelefamIosActiveCallState
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.62)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 14) {
                TelefamCallVectorIcon(kind: .shieldAlert, color: TelefamPalette.crimson, size: 34)
                    .frame(width: 60, height: 60)
                    .background(TelefamPalette.crimson.opacity(0.14))
                    .clipShape(Circle())

                Text("Calls not allowed")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))

                Text(state.notAllowedReason.isEmpty ? "\(state.peerName) does not allow calls." : state.notAllowedReason)
                    .font(.system(size: 14))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .multilineTextAlignment(.center)

                if !state.autoSentRequestText.isEmpty {
                    Text(state.autoSentRequestText)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(TelefamPalette.background(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                Button(action: onDismiss) {
                    Text("Close")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(TelefamPalette.crimson)
                        .clipShape(Capsule())
                }
            }
            .padding(24)
            .frame(maxWidth: 400)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .padding(24)
        }
    }
}

private struct TelefamIosIncomingCallView: View {
    let state: TelefamIosActiveCallState
    let authBearerToken: String
    let authDeviceFingerprint: String
    let onAccept: () -> Void
    let onDecline: () -> Void

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.06, green: 0.07, blue: 0.11), Color(red: 0.13, green: 0.07, blue: 0.11)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                Spacer()

                Text(state.callType.displayLabel)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white.opacity(0.75))

                TelefamIosCallAvatarView(
                    url: state.peerAvatarUrl,
                    name: state.peerName,
                    size: 112,
                    authBearerToken: authBearerToken,
                    authDeviceFingerprint: authDeviceFingerprint
                )

                Text(state.peerName.isEmpty ? "Telefam" : state.peerName)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(.white)

                Text("Incoming call...")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white.opacity(0.78))

                Spacer()

                HStack(spacing: 56) {
                    VStack(spacing: 8) {
                        Button(action: onDecline) {
                            TelefamCallVectorIcon(kind: .callEnd, color: .white, size: 28)
                                .frame(width: 68, height: 68)
                                .background(TelefamPalette.crimson)
                                .clipShape(Circle())
                        }
                        Text("Decline")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white.opacity(0.85))
                    }

                    VStack(spacing: 8) {
                        Button(action: onAccept) {
                            TelefamCallVectorIcon(kind: state.callType == .video ? .video : .phone, color: .white, size: 28)
                                .frame(width: 68, height: 68)
                                .background(Color(red: 0.11, green: 0.72, blue: 0.42))
                                .clipShape(Circle())
                        }
                        Text("Accept")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white.opacity(0.85))
                    }
                }
                .padding(.bottom, 48)
            }
            .padding(24)
        }
    }
}

private struct TelefamIosActiveCallView: View {
    @ObservedObject var controller: TelefamIosCallController
    @ObservedObject private var mediaEngine: TelefamIosCallMediaEngine
    let state: TelefamIosActiveCallState
    let authBearerToken: String
    let authDeviceFingerprint: String

    @State private var showFilterStrip = false
    @State private var showQualityMenu = false

    init(
        controller: TelefamIosCallController,
        state: TelefamIosActiveCallState,
        authBearerToken: String,
        authDeviceFingerprint: String
    ) {
        self.controller = controller
        self.mediaEngine = controller.mediaEngine
        self.state = state
        self.authBearerToken = authBearerToken
        self.authDeviceFingerprint = authDeviceFingerprint
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.05, green: 0.06, blue: 0.09), Color(red: 0.10, green: 0.11, blue: 0.16)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            // Remote video or Avatar placeholder when remote turned off cam / left screen
            if state.callType == .video && state.remoteInAppActive && (state.remoteCamEnabled || state.remoteScreenSharing),
               let remoteFrame = mediaEngine.remoteVideoFrame {
                Image(uiImage: remoteFrame)
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()
            } else {
                VStack(spacing: 14) {
                    TelefamIosCallAvatarView(
                        url: state.peerAvatarUrl,
                        name: state.peerName,
                        size: 118,
                        authBearerToken: authBearerToken,
                        authDeviceFingerprint: authDeviceFingerprint
                    )
                    Text(state.peerName.isEmpty ? "Telefam" : state.peerName)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                    Text(remoteSubtitle)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundColor(.white.opacity(0.78))
                }
                .padding(.horizontal, 24)
            }

            VStack(spacing: 0) {
                topBar
                Spacer()

                if state.callType == .video && showFilterStrip {
                    filterStrip
                        .padding(.bottom, 12)
                }

                if let prompt = state.switchVideoPrompt {
                    switchVideoBanner(prompt)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                }

                if state.callType == .video && !state.isScreenSharing {
                    localPipPreview
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                }

                controlsDock
            }
            .padding(.bottom, 16)

            if let toast = controller.toastMessage {
                VStack {
                    Spacer()
                    Text(toast)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(Color.black.opacity(0.84))
                        .clipShape(Capsule())
                        .padding(.bottom, 140)
                }
            }
        }
    }

    private var remoteSubtitle: String {
        if state.phase == .calling { return "Calling..." }
        if state.phase == .ringing { return "Ringing..." }
        if state.phase == .connecting { return "Connecting..." }
        if !state.remoteInAppActive { return "Away from call" }
        if state.callType == .video && !state.remoteCamEnabled { return "Camera off" }
        return state.statusText.isEmpty ? state.callType.displayLabel : state.statusText
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            Button(action: { controller.minimizeToChat() }) {
                TelefamCallVectorIcon(kind: .message, color: .white, size: 19)
                    .frame(width: 44, height: 44)
                    .background(Color.white.opacity(0.14))
                    .clipShape(Circle())
            }
            .accessibilityLabel("Message")

            VStack(alignment: .leading, spacing: 2) {
                Text(state.peerName.isEmpty ? "Telefam" : state.peerName)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                Text(state.statusText.isEmpty ? state.callType.displayLabel : state.statusText)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundColor(.white.opacity(0.78))
            }

            Spacer()

            Menu {
                ForEach(TelefamIosCallQualityTier.allCases) { tier in
                    Button(tier.label) {
                        controller.setQualityTier(tier)
                    }
                }
            } label: {
                HStack(spacing: 5) {
                    TelefamCallVectorIcon(
                        kind: .qualitySignal,
                        color: state.lowBandwidthWarning ? .orange : Color(red: 0.42, green: 0.92, blue: 0.56),
                        size: 15
                    )
                    Text(state.lowBandwidthWarning ? "Weak link" : state.networkLabel)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .frame(height: 36)
                .background(Color.white.opacity(0.14))
                .clipShape(Capsule())
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
    }

    private var localPipPreview: some View {
        Group {
            if state.isCameraEnabled && state.localInAppActive, let localImg = mediaEngine.localVideoFrame {
                Image(uiImage: localImg)
                    .resizable()
                    .scaledToFill()
            } else {
                ZStack {
                    Color(red: 0.16, green: 0.18, blue: 0.24)
                    TelefamCallVectorIcon(kind: .videoOff, color: .white.opacity(0.78), size: 24)
                }
            }
        }
        .frame(width: 108, height: 152)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.28), lineWidth: 1)
        )
    }

    private var filterStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(TelefamIosVideoFilter.allCases) { filter in
                    let selected = state.activeFilter == filter
                    Button(filter.label) {
                        controller.selectFilter(filter)
                    }
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .frame(minHeight: 38)
                    .background(selected ? TelefamPalette.crimson : Color.white.opacity(0.16))
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private func switchVideoBanner(_ prompt: TelefamIosSwitchVideoPromptState) -> some View {
        HStack(spacing: 12) {
            TelefamCallVectorIcon(kind: .video, color: .white, size: 20)
            Text("\(prompt.fromName) requested video")
                .font(.system(size: 13.5, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: { controller.declineSwitchToVideo() }) {
                TelefamCallVectorIcon(kind: .close, color: .white, size: 16)
                    .frame(width: 40, height: 40)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
            }
            Button(action: { controller.acceptSwitchToVideo() }) {
                TelefamCallVectorIcon(kind: .check, color: .white, size: 16)
                    .frame(width: 40, height: 40)
                    .background(Color(red: 0.12, green: 0.72, blue: 0.42))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.75))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var controlsDock: some View {
        VStack(spacing: 14) {
            HStack(spacing: 14) {
                if state.callType == .video {
                    callCircleButton(
                        kind: state.isCameraEnabled ? .video : .videoOff,
                        active: !state.isCameraEnabled,
                        label: state.isCameraEnabled ? "Cam" : "Cam off"
                    ) { controller.toggleCamera() }

                    callCircleButton(
                        kind: .flipCamera,
                        active: false,
                        label: "Flip"
                    ) { controller.flipCamera() }

                    callCircleButton(
                        kind: .filters,
                        active: showFilterStrip,
                        label: "Filters"
                    ) { showFilterStrip.toggle() }

                    callCircleButton(
                        kind: .screenShare,
                        active: state.isScreenSharing,
                        label: "Share"
                    ) { controller.toggleScreenShare() }
                } else {
                    callCircleButton(
                        kind: .video,
                        active: state.isSwitchingToVideo,
                        label: "Video"
                    ) { controller.requestSwitchToVideo() }

                    callCircleButton(
                        kind: .screenShare,
                        active: state.isScreenSharing,
                        label: "Share"
                    ) { controller.toggleScreenShare() }
                }
            }

            HStack(spacing: 18) {
                callCircleButton(
                    kind: .speakerOn,
                    active: state.isSpeakerOn,
                    label: "Speaker"
                ) { controller.toggleSpeaker() }

                callCircleButton(
                    kind: state.isMicMuted ? .micOff : .mic,
                    active: state.isMicMuted,
                    label: state.isMicMuted ? "Muted" : "Mic"
                ) { controller.toggleMic() }

                callCircleButton(
                    kind: .message,
                    active: false,
                    label: "Message"
                ) { controller.minimizeToChat() }

                VStack(spacing: 4) {
                    Button(action: { controller.hangUp() }) {
                        TelefamCallVectorIcon(kind: .callEnd, color: .white, size: 24)
                            .frame(width: 58, height: 58)
                            .background(TelefamPalette.crimson)
                            .clipShape(Circle())
                    }
                    Text("End")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.white.opacity(0.78))
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color.black.opacity(0.45))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal, 14)
    }

    private func callCircleButton(
        kind: TelefamCallVectorIcon.Kind,
        active: Bool,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        VStack(spacing: 4) {
            Button(action: action) {
                TelefamCallVectorIcon(kind: kind, color: .white, size: 21)
                    .frame(width: 50, height: 50)
                    .background(active ? TelefamPalette.crimson : Color.white.opacity(0.16))
                    .clipShape(Circle())
            }
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.white.opacity(0.78))
        }
    }
}

private struct TelefamIosCallAvatarView: View {
    let url: String
    let name: String
    let size: CGFloat
    let authBearerToken: String
    let authDeviceFingerprint: String

    var body: some View {
        let initials = String((name.isEmpty ? "T" : name).prefix(2)).uppercased()
        Group {
            if !url.isEmpty {
                TelefamIosRemoteImage(
                    urlString: url,
                    bearerToken: authBearerToken,
                    deviceFingerprint: authDeviceFingerprint,
                    fallbackInitials: initials
                )
            } else {
                Circle()
                    .fill(TelefamPalette.crimson.opacity(0.28))
                    .overlay(
                        Text(initials)
                            .font(.system(size: size * 0.34, weight: .bold))
                            .foregroundColor(.white)
                    )
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white.opacity(0.24), lineWidth: 2))
    }
}
