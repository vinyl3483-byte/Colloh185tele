import AVFoundation
import Compression
import CryptoKit
import Foundation
import SwiftUI
import UIKit

// MARK: - RFC 1950 Zlib & RFC 1952 Gzip compression using Apple Compression framework

enum TelefamIosCompression {
    static func zlibCompress(_ data: Data) -> Data {
        if data.isEmpty { return data }
        let rawDeflate = compressRawDeflate(data)
        var out = Data([0x78, 0x9C])
        out.append(rawDeflate)
        var adler = adler32(data).bigEndian
        withUnsafeBytes(of: &adler) { out.append(contentsOf: $0) }
        return out
    }

    static func zlibDecompress(_ data: Data) -> Data? {
        guard data.count > 6 else { return nil }
        let payload = data.subdata(in: 2..<(data.count - 4))
        return decompressRawDeflate(payload, expectedCapacity: max(data.count * 6, 4096))
    }

    static func gzipCompress(_ data: Data) -> Data {
        let rawDeflate = compressRawDeflate(data)
        var out = Data([0x1F, 0x8B, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03])
        out.append(rawDeflate)
        var crc = crc32(data).littleEndian
        var isize = UInt32(truncatingIfNeeded: data.count).littleEndian
        withUnsafeBytes(of: &crc) { out.append(contentsOf: $0) }
        withUnsafeBytes(of: &isize) { out.append(contentsOf: $0) }
        return out
    }

    private static func compressRawDeflate(_ data: Data) -> Data {
        let dstCapacity = max(data.count + 256, 1024)
        let dstBuffer = UnsafeMutablePointer<UInt8>.allocate(capacity: dstCapacity)
        defer { dstBuffer.deallocate() }
        let written: Int = data.withUnsafeBytes { srcPtr in
            guard let base = srcPtr.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return 0 }
            return compression_encode_buffer(dstBuffer, dstCapacity, base, data.count, nil, COMPRESSION_ZLIB)
        }
        return written > 0 ? Data(bytes: dstBuffer, count: written) : data
    }

    private static func decompressRawDeflate(_ data: Data, expectedCapacity: Int) -> Data? {
        var cap = expectedCapacity
        for _ in 0..<4 {
            let dstBuffer = UnsafeMutablePointer<UInt8>.allocate(capacity: cap)
            let decoded: Int = data.withUnsafeBytes { srcPtr in
                guard let base = srcPtr.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return 0 }
                return compression_decode_buffer(dstBuffer, cap, base, data.count, nil, COMPRESSION_ZLIB)
            }
            if decoded > 0 && decoded < cap {
                let res = Data(bytes: dstBuffer, count: decoded)
                dstBuffer.deallocate()
                return res
            }
            dstBuffer.deallocate()
            cap *= 3
        }
        return nil
    }

    private static func adler32(_ data: Data) -> UInt32 {
        var a: UInt32 = 1
        var b: UInt32 = 0
        for byte in data {
            a = (a + UInt32(byte)) % 65521
            b = (b + a) % 65521
        }
        return (b << 16) | a
    }

    private static func crc32(_ data: Data) -> UInt32 {
        var crc: UInt32 = 0xFFFF_FFFF
        for byte in data {
            crc ^= UInt32(byte)
            for _ in 0..<8 {
                let mask = (crc & 1) != 0 ? UInt32(0xEDB8_8320) : 0
                crc = (crc >> 1) ^ mask
            }
        }
        return ~crc
    }
}

// MARK: - Curve25519 + HKDF-SHA256 + AES-256-GCM E2EE Manager

final class TelefamIosE2eeManager {
    private let ownerUserId: String
    private var registrationId: Int = 0
    private var identityPriv: Curve25519.KeyAgreement.PrivateKey
    private var signedPreKeyPriv: Curve25519.KeyAgreement.PrivateKey
    private var preKeys: [Int: Curve25519.KeyAgreement.PrivateKey] = [:]
    private(set) var keysUploaded: Bool = false

    private struct StoredKeys: Codable {
        var registrationId: Int
        var keysUploaded: Bool
        var identityPrivB64: String
        var spkPrivB64: String
        var preKeysB64: [Int: String]
    }

    init(ownerUserId: String) {
        self.ownerUserId = ownerUserId
        if let restored = Self.loadFromVault(ownerUserId: ownerUserId),
           let idData = Data(base64Encoded: restored.identityPrivB64),
           let spkData = Data(base64Encoded: restored.spkPrivB64),
           let idKey = try? Curve25519.KeyAgreement.PrivateKey(rawRepresentation: idData),
           let spkKey = try? Curve25519.KeyAgreement.PrivateKey(rawRepresentation: spkData) {
            self.registrationId = restored.registrationId
            self.keysUploaded = restored.keysUploaded
            self.identityPriv = idKey
            self.signedPreKeyPriv = spkKey
            for (k, b64) in restored.preKeysB64 {
                if let d = Data(base64Encoded: b64),
                   let p = try? Curve25519.KeyAgreement.PrivateKey(rawRepresentation: d) {
                    self.preKeys[k] = p
                }
            }
        } else {
            self.registrationId = Int.random(in: 10000...99999)
            self.identityPriv = Curve25519.KeyAgreement.PrivateKey()
            self.signedPreKeyPriv = Curve25519.KeyAgreement.PrivateKey()
            for id in 1...20 {
                self.preKeys[id] = Curve25519.KeyAgreement.PrivateKey()
            }
            persist()
        }
    }

    func markUploaded() {
        keysUploaded = true
        persist()
    }

    func exportUploadBody() -> [String: Any] {
        let idPubWire = Data([0x05]) + identityPriv.publicKey.rawRepresentation
        let spkPubWire = Data([0x05]) + signedPreKeyPriv.publicKey.rawRepresentation
        let sigMac = HMAC<SHA256>.authenticationCode(
            for: spkPubWire,
            using: SymmetricKey(data: identityPriv.publicKey.rawRepresentation)
        )
        let kyberRandom = SymmetricKey(size: .bits256).withUnsafeBytes { Data($0) }
        let kyberSig = HMAC<SHA256>.authenticationCode(
            for: kyberRandom,
            using: SymmetricKey(data: identityPriv.publicKey.rawRepresentation)
        )
        let otks: [[String: Any]] = preKeys.keys.sorted().compactMap { id in
            guard let kp = preKeys[id] else { return nil }
            let wire = Data([0x05]) + kp.publicKey.rawRepresentation
            return ["key_id": id, "public_key_b64": wire.base64EncodedString()]
        }
        return [
            "registration_id": registrationId,
            "identity_pub_b64": idPubWire.base64EncodedString(),
            "signed_prekey_id": 1,
            "signed_prekey_pub_b64": spkPubWire.base64EncodedString(),
            "signed_prekey_sig_b64": Data(sigMac).base64EncodedString(),
            "kyber_prekey_id": 1,
            "kyber_prekey_pub_b64": kyberRandom.base64EncodedString(),
            "kyber_prekey_sig_b64": Data(kyberSig).base64EncodedString(),
            "one_time_prekeys": otks
        ]
    }

    func compressAndEncrypt(peerPubB64: String, plaintext: String) -> (cipherB64: String, selfCipherB64: String)? {
        guard let plainData = plaintext.data(using: .utf8),
              let peerRaw = extractRaw32(fromB64: peerPubB64),
              let peerPub = try? Curve25519.KeyAgreement.PublicKey(rawRepresentation: peerRaw) else {
            return nil
        }
        let compressed = TelefamIosCompression.zlibCompress(plainData)
        guard let peerEnv = seal(data: compressed, recipientPub: peerPub, info: "telefam-ecdh-msg-v1"),
              let selfEnv = seal(data: compressed, recipientPub: identityPriv.publicKey, info: "telefam-ecdh-self-v1") else {
            return nil
        }
        return (peerEnv.base64EncodedString(), selfEnv.base64EncodedString())
    }

    func decryptAndDecompress(isOwn: Bool, ciphertextB64: String, senderCiphertextB64: String, codec: String) -> String? {
        let b64 = (isOwn && !senderCiphertextB64.isEmpty) ? senderCiphertextB64 : ciphertextB64
        guard let packet = Data(base64Encoded: b64) else { return nil }

        let compressed: Data?
        if isOwn {
            compressed = open(packet: packet, priv: identityPriv, info: "telefam-ecdh-self-v1")
        } else {
            compressed = open(packet: packet, priv: signedPreKeyPriv, info: "telefam-ecdh-msg-v1")
                ?? open(packet: packet, priv: identityPriv, info: "telefam-ecdh-msg-v1")
                ?? preKeys.values.lazy.compactMap { open(packet: packet, priv: $0, info: "telefam-ecdh-msg-v1") }.first
        }
        guard let rawCompressed = compressed else { return nil }
        let decoded = codec.lowercased() == "zlib"
            ? (TelefamIosCompression.zlibDecompress(rawCompressed) ?? rawCompressed)
            : rawCompressed
        return String(data: decoded, encoding: .utf8)
    }

    private func seal(data: Data, recipientPub: Curve25519.KeyAgreement.PublicKey, info: String) -> Data? {
        let ephPriv = Curve25519.KeyAgreement.PrivateKey()
        guard let shared = try? ephPriv.sharedSecretFromKeyAgreement(with: recipientPub) else { return nil }
        let ephPubRaw = ephPriv.publicKey.rawRepresentation
        let aesKey = shared.hkdfDerivedSymmetricKey(
            using: SHA256.self,
            salt: ephPubRaw,
            sharedInfo: Data(info.utf8),
            outputByteCount: 32
        )
        let nonce = AES.GCM.Nonce()
        guard let sealed = try? AES.GCM.seal(data, using: aesKey, nonce: nonce) else { return nil }
        var packet = Data([0x01])
        packet.append(contentsOf: nonce)
        packet.append(ephPubRaw)
        packet.append(sealed.ciphertext)
        packet.append(sealed.tag)
        return packet
    }

    private func open(packet: Data, priv: Curve25519.KeyAgreement.PrivateKey, info: String) -> Data? {
        guard packet.count > 61, packet[0] == 0x01 else { return nil }
        let nonceBytes = packet.subdata(in: 1..<13)
        let ephRaw = packet.subdata(in: 13..<45)
        let encAndTag = packet.subdata(in: 45..<packet.count)
        guard encAndTag.count >= 16,
              let ephPub = try? Curve25519.KeyAgreement.PublicKey(rawRepresentation: ephRaw),
              let shared = try? priv.sharedSecretFromKeyAgreement(with: ephPub),
              let nonce = try? AES.GCM.Nonce(data: nonceBytes) else {
            return nil
        }
        let aesKey = shared.hkdfDerivedSymmetricKey(
            using: SHA256.self,
            salt: ephRaw,
            sharedInfo: Data(info.utf8),
            outputByteCount: 32
        )
        let ct = encAndTag.prefix(encAndTag.count - 16)
        let tag = encAndTag.suffix(16)
        guard let box = try? AES.GCM.SealedBox(nonce: nonce, ciphertext: ct, tag: tag) else { return nil }
        return try? AES.GCM.open(box, using: aesKey)
    }

    private func extractRaw32(fromB64 b64: String) -> Data? {
        guard let raw = Data(base64Encoded: b64) else { return nil }
        if raw.count == 33 && raw[0] == 0x05 { return raw.subdata(in: 1..<33) }
        if raw.count >= 32 { return raw.suffix(32) }
        return nil
    }

    private func persist() {
        var preDict: [Int: String] = [:]
        for (k, v) in preKeys { preDict[k] = v.rawRepresentation.base64EncodedString() }
        let st = StoredKeys(
            registrationId: registrationId,
            keysUploaded: keysUploaded,
            identityPrivB64: identityPriv.rawRepresentation.base64EncodedString(),
            spkPrivB64: signedPreKeyPriv.rawRepresentation.base64EncodedString(),
            preKeysB64: preDict
        )
        TelefamIosChatVault.writeEncrypted(st, file: "e2ee_\(ownerUserId).bin", uid: ownerUserId)
    }

    private static func loadFromVault(ownerUserId: String) -> StoredKeys? {
        TelefamIosChatVault.readEncrypted(StoredKeys.self, file: "e2ee_\(ownerUserId).bin", uid: ownerUserId)
    }
}

private enum TelefamIosChatVault {
    private static func dir() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let folder = base.appendingPathComponent("TelefamChatVault", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }

    private static func key(uid: String) -> SymmetricKey {
        let seed = "telefam-ios-chat-vault-v1|\(uid)|\(UIDevice.current.identifierForVendor?.uuidString ?? "ios")"
        let digest = SHA256.hash(data: Data(seed.utf8))
        return SymmetricKey(data: digest)
    }

    static func writeEncrypted<T: Encodable>(_ obj: T, file: String, uid: String) {
        guard let plain = try? JSONEncoder().encode(obj),
              let sealed = try? AES.GCM.seal(plain, using: key(uid: uid)),
              let combined = sealed.combined else { return }
        let target = dir().appendingPathComponent(file)
        try? combined.write(to: target, options: [.atomic, .completeFileProtection])
    }

    static func readEncrypted<T: Decodable>(_ type: T.Type, file: String, uid: String) -> T? {
        let target = dir().appendingPathComponent(file)
        guard let data = try? Data(contentsOf: target),
              let box = try? AES.GCM.SealedBox(combined: data),
              let plain = try? AES.GCM.open(box, using: key(uid: uid)) else { return nil }
        return try? JSONDecoder().decode(type, from: plain)
    }
}

// MARK: - Models

struct TelefamIosChatCapabilities: Codable, Equatable {
    var canCall: Bool = true
    var canScreenshot: Bool = false
    var canShareChats: Bool = false
    var canCopyMessages: Bool = false
    var canDownloadMedia: Bool = true
    var showReadReceipts: Bool = true
    var showTypingIndicator: Bool = false
    var showOnlineIndicator: Bool = false
    var isBlocked: Bool = false
    var isMuted: Bool = false

    enum CodingKeys: String, CodingKey {
        case canCall = "can_call"
        case canScreenshot = "can_screenshot"
        case canShareChats = "can_share_chats"
        case canCopyMessages = "can_copy_messages"
        case canDownloadMedia = "can_download_media"
        case showReadReceipts = "show_read_receipts"
        case showTypingIndicator = "show_typing_indicator"
        case showOnlineIndicator = "show_online_indicator"
        case isBlocked = "is_blocked"
        case isMuted = "is_muted"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        canCall = (try? c.decodeIfPresent(Bool.self, forKey: .canCall)) ?? true
        canScreenshot = (try? c.decodeIfPresent(Bool.self, forKey: .canScreenshot)) ?? false
        canShareChats = (try? c.decodeIfPresent(Bool.self, forKey: .canShareChats)) ?? false
        canCopyMessages = (try? c.decodeIfPresent(Bool.self, forKey: .canCopyMessages)) ?? false
        canDownloadMedia = (try? c.decodeIfPresent(Bool.self, forKey: .canDownloadMedia)) ?? true
        showReadReceipts = (try? c.decodeIfPresent(Bool.self, forKey: .showReadReceipts)) ?? true
        showTypingIndicator = (try? c.decodeIfPresent(Bool.self, forKey: .showTypingIndicator)) ?? false
        showOnlineIndicator = (try? c.decodeIfPresent(Bool.self, forKey: .showOnlineIndicator)) ?? false
        isBlocked = (try? c.decodeIfPresent(Bool.self, forKey: .isBlocked)) ?? false
        isMuted = (try? c.decodeIfPresent(Bool.self, forKey: .isMuted)) ?? false
    }
}

struct TelefamIosChatConversation: Codable, Identifiable, Equatable {
    var id: String { conversationId }
    var conversationId: String
    var peerUserId: String
    var peerFullName: String
    var peerUsername: String
    var peerAvatarUrl: String = ""
    var lastMessageId: String = ""
    var lastMessageType: String = ""
    var lastSenderId: String = ""
    var lastMessagePreviewPlain: String = ""
    var lastDeliveryStatus: String = "sent"
    var lastMessageAtMs: Int64 = 0
    var unreadCount: Int = 0
    var isArchived: Bool = false
    var isMuted: Bool = false
    var isDeletedLocally: Bool = false
    var isOnline: Bool = false
    var lastSeenLabel: String = "last seen recently"
    var permissions: TelefamIosChatCapabilities = TelefamIosChatCapabilities()
    var updatedAtMs: Int64 = 0
}

struct TelefamIosChatMessage: Codable, Identifiable, Equatable {
    var id: String { messageId }
    var messageId: String
    var conversationId: String
    var senderUserId: String
    var recipientUserId: String
    var clientOpId: String = ""
    var messageType: String = "text"
    var compressionCodec: String = "zlib"
    var encryptionScheme: String = "telefam_ecdh_v1"
    var ciphertextB64: String = ""
    var senderCiphertextB64: String = ""
    var signalMsgType: Int = 3
    var plainText: String = ""
    var mediaUrl: String = ""
    var mediaLocalPath: String = ""
    var mediaId: String = ""
    var voiceDurationMs: Int64 = 0
    var viewOnceTtlSec: Int = 0
    var viewOnceOpenedAtMs: Int64 = 0
    var viewOnceExpiresAtMs: Int64 = 0
    var forwardedFromUserId: String = ""
    var deliveryStatus: String = "sent"
    var syncState: String = "synced"
    var editedAtMs: Int64 = 0
    var createdAtMs: Int64 = 0
}

struct TelefamIosChatSecurityFlag: Codable, Identifiable, Equatable {
    var id: String { flagId }
    var flagId: String
    var conversationId: String
    var messageId: String = ""
    var actorUserId: String = ""
    var targetUserId: String = ""
    var flagType: String
    var details: String = ""
    var isSynced: Bool = true
    var createdAtMs: Int64 = 0
}

struct TelefamIosGiflyItem: Codable, Identifiable, Equatable {
    var id: Int64
    var slug: String
    var title: String
    var type: String
    var preview: String
    var previewWebp: String
    var url: String
    var urlHd: String

    var bestUrl: String {
        if !url.isEmpty { return url }
        if !previewWebp.isEmpty { return previewWebp }
        if !preview.isEmpty { return preview }
        return urlHd
    }
}

struct TelefamIosChatSearchPerson: Codable, Identifiable, Equatable {
    var id: String { userId }
    var userId: String
    var fullName: String
    var username: String
    var avatarUrl: String = ""
    var location: String = ""
    var conversationId: String = ""
}

struct TelefamIosPendingChatOp: Codable {
    var opId: String
    var opKind: String
    var conversationId: String
    var targetMessageId: String = ""
    var payloadJson: String = "{}"
    var nextRetryAtMs: Int64 = 0
    var retryCount: Int = 0
}

struct TelefamIosChatUndoBanner: Equatable {
    var message: String
    var previousAction: String
    var snapshot: [TelefamIosChatConversation]
}

// MARK: - Audio Recorder, Playback & Sound Engine

final class TelefamIosChatAudioEngine: NSObject, AVAudioPlayerDelegate {
    private var recorder: AVAudioRecorder?
    private var recordingUrl: URL?
    private var recordStart: Date?
    private var player: AVAudioPlayer?
    private var tonePlayer: AVAudioPlayer?
    private var onPlaybackCompleted: (() -> Void)?

    private func voicesDir() -> URL {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let dir = base.appendingPathComponent("TelefamVoices", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    func startRecording() -> URL? {
        stopRecording(discard: true)
        let session = AVAudioSession.sharedInstance()
        if session.recordPermission == .undetermined {
            session.requestRecordPermission { _ in }
            return nil
        }
        if session.recordPermission == .denied { return nil }
        try? session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
        try? session.setActive(true)
        let target = voicesDir().appendingPathComponent("voice_\(Int(Date().timeIntervalSince1970 * 1000)).m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 16000,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
        ]
        guard let rec = try? AVAudioRecorder(url: target, settings: settings) else { return nil }
        guard rec.record() else { return nil }
        recorder = rec
        recordingUrl = target
        recordStart = Date()
        return target
    }

    func stopRecording(discard: Bool) -> (URL?, Int64) {
        recorder?.stop()
        recorder = nil
        let url = recordingUrl
        recordingUrl = nil
        let dur = Int64(max(300.0, (Date().timeIntervalSince(recordStart ?? Date())) * 1000.0))
        recordStart = nil
        if discard, let u = url {
            try? FileManager.default.removeItem(at: u)
            return (nil, 0)
        }
        guard let u = url, FileManager.default.fileExists(atPath: u.path) else { return (nil, 0) }
        return (u, dur)
    }

    func playFile(_ url: URL, onComplete: @escaping () -> Void) -> Bool {
        stopPlayback()
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        guard let pl = try? AVAudioPlayer(contentsOf: url) else { return false }
        pl.delegate = self
        player = pl
        onPlaybackCompleted = onComplete
        return pl.play()
    }

    func stopPlayback() {
        player?.stop()
        player = nil
        onPlaybackCompleted = nil
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        let cb = onPlaybackCompleted
        onPlaybackCompleted = nil
        self.player = nil
        cb?()
    }

    func playTone(freqStart: Double, freqEnd: Double, durationMs: Int, gain: Double, enabled: Bool) {
        guard enabled else { return }
        DispatchQueue.global(qos: .utility).async { [weak self] in
            let wav = Self.buildWavTone(freqStart: freqStart, freqEnd: freqEnd, durationMs: durationMs, gain: gain)
            DispatchQueue.main.async {
                guard let p = try? AVAudioPlayer(data: wav) else { return }
                self?.tonePlayer = p
                p.play()
            }
        }
    }

    private static func buildWavTone(freqStart: Double, freqEnd: Double, durationMs: Int, gain: Double) -> Data {
        let sampleRate = 22050
        let count = max(1, (sampleRate * durationMs) / 1000)
        var pcm = Data(capacity: count * 2)
        var phase = 0.0
        for i in 0..<count {
            let t = Double(i) / Double(count)
            let f = freqStart + (freqEnd - freqStart) * t
            phase += 2.0 * .pi * f / Double(sampleRate)
            let env = sin(.pi * t) * exp(-2.2 * t)
            var sample = Int16((sin(phase) * env * gain * 32767.0).rounded()).littleEndian
            withUnsafeBytes(of: &sample) { pcm.append(contentsOf: $0) }
        }
        var hdr = Data()
        hdr.append("RIFF".data(using: .ascii)!)
        var chunkSize = UInt32(36 + pcm.count).littleEndian
        withUnsafeBytes(of: &chunkSize) { hdr.append(contentsOf: $0) }
        hdr.append("WAVEfmt ".data(using: .ascii)!)
        var sub1 = UInt32(16).littleEndian
        var fmt = UInt16(1).littleEndian
        var ch = UInt16(1).littleEndian
        var sr = UInt32(sampleRate).littleEndian
        var br = UInt32(sampleRate * 2).littleEndian
        var ba = UInt16(2).littleEndian
        var bps = UInt16(16).littleEndian
        withUnsafeBytes(of: &sub1) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &fmt) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &ch) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &sr) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &br) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &ba) { hdr.append(contentsOf: $0) }
        withUnsafeBytes(of: &bps) { hdr.append(contentsOf: $0) }
        hdr.append("data".data(using: .ascii)!)
        var dSize = UInt32(pcm.count).littleEndian
        withUnsafeBytes(of: &dSize) { hdr.append(contentsOf: $0) }
        hdr.append(pcm)
        return hdr
    }
}

// MARK: - Chat Store (E2EE + Offline Vault + WebSocket + Screenshot Flagging + View-Once Auto-Burn)

@MainActor
final class TelefamIosChatStore: ObservableObject {
    @Published var conversations: [TelefamIosChatConversation] = []
    @Published var archivedConversations: [TelefamIosChatConversation] = []
    @Published var activeConversationId: String? = nil
    @Published var activeMessages: [TelefamIosChatMessage] = []
    @Published var activeSecurityFlags: [TelefamIosChatSecurityFlag] = []
    @Published var typingModeByConv: [String: String] = [:]
    @Published var greetingSticker: TelefamIosGiflyItem? = nil
    @Published var searchConvs: [TelefamIosChatConversation] = []
    @Published var searchMessages: [TelefamIosChatMessage] = []
    @Published var searchPeople: [TelefamIosChatSearchPerson] = []
    @Published var undoBanner: TelefamIosChatUndoBanner? = nil
    @Published var dissolvingIds: Set<String> = []
    @Published var playingMessageId: String? = nil
    @Published var activeViewOnceMsg: TelefamIosChatMessage? = nil
    @Published var viewOnceRemainingSec: Int = 0
    @Published var toastMessage: String? = nil

    private let baseURL: URL
    private let userIdProvider: () -> String
    private let tokenProvider: () -> String
    private let deviceFingerprint: String
    private let soundsEnabledProvider: () -> Bool

    var currentUserId: String { attachedUserId }
    var authBearerToken: String { tokenProvider() }
    var authDeviceFingerprint: String { deviceFingerprint }
    var onCallWsEvent: ((String, [String: Any]) -> Void)? = nil

    func resolvedMediaURL(_ raw: String) -> String {
        let clean = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return "" }
        if clean.hasPrefix("http://") || clean.hasPrefix("https://") { return clean }
        return URL(string: clean, relativeTo: baseURL)?.absoluteString ?? clean
    }

    let audio = TelefamIosChatAudioEngine()
    private var e2ee: TelefamIosE2eeManager?
    private var attachedUserId: String = ""

    private var allConvs: [String: TelefamIosChatConversation] = [:]
    private var allMsgs: [String: TelefamIosChatMessage] = [:]
    private var allFlags: [String: TelefamIosChatSecurityFlag] = [:]
    private var pendingOps: [String: TelefamIosPendingChatOp] = [:]
    private var peerKeys: [String: String] = [:]

    private var wsTask: URLSessionWebSocketTask?
    private var maintenanceTask: Task<Void, Never>?
    private var burnTask: Task<Void, Never>?
    private var screenshotObserver: NSObjectProtocol?
    private var isDetached: Bool = false

    private struct CacheSnapshot: Codable {
        var convs: [TelefamIosChatConversation]
        var msgs: [TelefamIosChatMessage]
        var flags: [TelefamIosChatSecurityFlag]
        var ops: [TelefamIosPendingChatOp]
    }

    init(
        baseURL: URL,
        userIdProvider: @escaping () -> String,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String,
        soundsEnabledProvider: @escaping () -> Bool
    ) {
        self.baseURL = baseURL
        self.userIdProvider = userIdProvider
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint
        self.soundsEnabledProvider = soundsEnabledProvider

        screenshotObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.userDidTakeScreenshotNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                await self?.handleSystemScreenshotDetected()
            }
        }
    }

    deinit {
        if let obs = screenshotObserver { NotificationCenter.default.removeObserver(obs) }
        wsTask?.cancel(with: .goingAway, reason: nil)
        maintenanceTask?.cancel()
        burnTask?.cancel()
    }

    func attachIfNeeded() {
        let uid = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !uid.isEmpty, !tokenProvider().isEmpty else { return }
        guard uid != attachedUserId || e2ee == nil else { return }
        isDetached = false
        attachedUserId = uid
        e2ee = TelefamIosE2eeManager(ownerUserId: uid)
        if let snap = TelefamIosChatVault.readEncrypted(CacheSnapshot.self, file: "chat_cache_\(uid).bin", uid: uid) {
            allConvs = Dictionary(uniqueKeysWithValues: snap.convs.map { ($0.conversationId, $0) })
            allMsgs = Dictionary(uniqueKeysWithValues: snap.msgs.map { ($0.messageId, $0) })
            allFlags = Dictionary(uniqueKeysWithValues: snap.flags.map { ($0.flagId, $0) })
            pendingOps = Dictionary(uniqueKeysWithValues: snap.ops.map { ($0.opId, $0) })
        }
        publishLists()
        connectWebSocket()
        startMaintenanceLoop()
        Task {
            await ensureKeysUploaded()
            await refreshConversations(archived: false)
            await refreshConversations(archived: true)
            await flushOutbox()
        }
    }

    func detach() {
        isDetached = true
        wsTask?.cancel(with: .goingAway, reason: nil)
        wsTask = nil
        maintenanceTask?.cancel()
        maintenanceTask = nil
        burnTask?.cancel()
        burnTask = nil
        audio.stopPlayback()
        _ = audio.stopRecording(discard: true)
        if let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let dir = base.appendingPathComponent("TelefamVoices", isDirectory: true)
            if let files = try? FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil) {
                for f in files where f.lastPathComponent.hasPrefix("dl_") {
                    try? FileManager.default.removeItem(at: f)
                }
            }
        }
        attachedUserId = ""
        e2ee = nil
        allConvs = [:]
        allMsgs = [:]
        allFlags = [:]
        pendingOps = [:]
        peerKeys = [:]
        conversations = []
        archivedConversations = []
        activeConversationId = nil
        activeMessages = []
        activeSecurityFlags = []
        typingModeByConv = [:]
        greetingSticker = nil
        searchConvs = []
        searchMessages = []
        searchPeople = []
        undoBanner = nil
        dissolvingIds = []
        playingMessageId = nil
        activeViewOnceMsg = nil
        viewOnceRemainingSec = 0
        toastMessage = nil
    }

    func openConversation(_ convId: String) {
        activeConversationId = convId
        TelefamIosNotificationManager.shared.cancelConversationNotifications(conversationId: convId)
        if var c = allConvs[convId], c.unreadCount > 0 {
            c.unreadCount = 0
            allConvs[convId] = c
            saveCache()
            publishLists()
        }
        publishActiveMessages(convId)
        Task {
            if let peer = allConvs[convId]?.peerUserId {
                await fetchGreetingSticker(peerUserId: peer)
            }
            await syncMessages(convId: convId, markAsRead: true)
        }
    }

    func closeConversation() {
        if let cid = activeConversationId { sendTypingWs(convId: cid, mode: "idle") }
        audio.stopPlayback()
        playingMessageId = nil
        activeConversationId = nil
        activeMessages = []
        activeSecurityFlags = []
    }

    func openDirectConversation(peerUserId: String, fullName: String, username: String, avatarUrl: String) async {
        attachIfNeeded()
        let encoded = peerUserId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? peerUserId
        let req = authorizedRequest(path: "v1/chat/conversations/direct/\(encoded)", method: "POST")
        if let (data, http) = await perform(req), (200...299).contains(http.statusCode),
           let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let convDict = root["conversation"] as? [String: Any] {
            let conv = parseConversation(convDict)
            if !conv.conversationId.isEmpty {
                allConvs[conv.conversationId] = conv
                saveCache()
                publishLists()
                openConversation(conv.conversationId)
                return
            }
        }
        if let existing = allConvs.values.first(where: { $0.peerUserId == peerUserId && !$0.isDeletedLocally }) {
            openConversation(existing.conversationId)
            return
        }
        let localId = "local_direct_\(peerUserId)"
        allConvs[localId] = TelefamIosChatConversation(
            conversationId: localId,
            peerUserId: peerUserId,
            peerFullName: fullName.isEmpty ? username : fullName,
            peerUsername: username,
            peerAvatarUrl: avatarUrl,
            updatedAtMs: Int64(Date().timeIntervalSince1970 * 1000)
        )
        saveCache()
        publishLists()
        openConversation(localId)
    }

    func sendText(convId: String, peerUserId: String, text: String, forwardedFrom: String = "") {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        sendEnvelope(convId: convId, peerUserId: peerUserId, type: "text", plainText: clean, forwardedFrom: forwardedFrom)
    }

    func sendGiflyMedia(convId: String, peerUserId: String, item: TelefamIosGiflyItem, type: String) {
        sendEnvelope(
            convId: convId,
            peerUserId: peerUserId,
            type: type,
            plainText: item.title.isEmpty ? type.capitalized : item.title,
            mediaUrl: item.bestUrl,
            mediaId: item.slug.isEmpty ? String(item.id) : item.slug
        )
    }

    func sendVoice(convId: String, peerUserId: String, fileUrl: URL, durationMs: Int64, viewOnce: Bool, ttlSec: Int) {
        sendEnvelope(
            convId: convId,
            peerUserId: peerUserId,
            type: viewOnce ? "voice_once" : "voice",
            plainText: viewOnce ? "View-once voice note" : "Voice note",
            mediaLocalPath: fileUrl.path,
            voiceDurationMs: durationMs,
            viewOnceTtlSec: viewOnce ? ttlSec : 0
        )
    }

    private func sendEnvelope(
        convId: String,
        peerUserId: String,
        type: String,
        plainText: String,
        mediaUrl: String = "",
        mediaLocalPath: String = "",
        mediaId: String = "",
        voiceDurationMs: Int64 = 0,
        viewOnceTtlSec: Int = 0,
        forwardedFrom: String = ""
    ) {
        if allConvs[convId]?.permissions.isBlocked == true { return }
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let opId = "op_\(UUID().uuidString)"
        let localMsg = TelefamIosChatMessage(
            messageId: opId,
            conversationId: convId,
            senderUserId: attachedUserId,
            recipientUserId: peerUserId,
            clientOpId: opId,
            messageType: type,
            plainText: plainText,
            mediaUrl: mediaUrl,
            mediaLocalPath: mediaLocalPath,
            mediaId: mediaId,
            voiceDurationMs: voiceDurationMs,
            viewOnceTtlSec: viewOnceTtlSec,
            forwardedFromUserId: forwardedFrom,
            deliveryStatus: "sending",
            syncState: "pending",
            createdAtMs: nowMs
        )
        allMsgs[localMsg.messageId] = localMsg
        if var c = allConvs[convId] {
            c.lastMessageId = localMsg.messageId
            c.lastMessageType = type
            c.lastSenderId = attachedUserId
            c.lastMessagePreviewPlain = previewText(for: localMsg)
            c.lastDeliveryStatus = "sending"
            c.lastMessageAtMs = nowMs
            c.updatedAtMs = nowMs
            allConvs[convId] = c
        }
        saveCache()
        publishLists()
        if activeConversationId == convId { publishActiveMessages(convId) }
        let muted = allConvs[convId]?.isMuted == true
        audio.playTone(freqStart: 760, freqEnd: 1040, durationMs: 75, gain: 0.22, enabled: soundsEnabledProvider() && !muted)

        Task {
            let ok = await tryDeliver(localMsg)
            if !ok {
                pendingOps[opId] = TelefamIosPendingChatOp(
                    opId: opId,
                    opKind: "send",
                    conversationId: convId,
                    targetMessageId: localMsg.messageId,
                    nextRetryAtMs: nowMs + 2000
                )
                saveCache()
            }
        }
    }

    private func tryDeliver(_ msg: TelefamIosChatMessage) async -> Bool {
        guard let engine = e2ee else { return false }
        await ensureKeysUploaded()
        var resolvedMediaUrl = msg.mediaUrl
        var resolvedMediaId = msg.mediaId
        if (msg.messageType == "voice" || msg.messageType == "voice_once") && resolvedMediaUrl.isEmpty && !msg.mediaLocalPath.isEmpty {
            guard let bytes = try? Data(contentsOf: URL(fileURLWithPath: msg.mediaLocalPath)),
                  let uploaded = await uploadVoiceMultipart(convId: msg.conversationId, durationMs: msg.voiceDurationMs, viewOnce: msg.messageType == "voice_once", bytes: bytes, filename: URL(fileURLWithPath: msg.mediaLocalPath).lastPathComponent) else {
                return false
            }
            resolvedMediaId = uploaded.mediaId
            resolvedMediaUrl = uploaded.mediaUrl
        }
        guard let peerKey = await fetchPeerKey(peerUserId: msg.recipientUserId),
              let env = engine.compressAndEncrypt(peerPubB64: peerKey, plaintext: msg.plainText) else {
            return false
        }
        var targetConvId = msg.conversationId
        if targetConvId.hasPrefix("local_direct_") {
            let encoded = msg.recipientUserId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.recipientUserId
            let openReq = authorizedRequest(path: "v1/chat/conversations/direct/\(encoded)", method: "POST")
            if let (d, h) = await perform(openReq), (200...299).contains(h.statusCode),
               let obj = (try? JSONSerialization.jsonObject(with: d) as? [String: Any])?["conversation"] as? [String: Any] {
                let parsed = parseConversation(obj)
                if !parsed.conversationId.isEmpty {
                    targetConvId = parsed.conversationId
                    allConvs.removeValue(forKey: msg.conversationId)
                    allConvs[targetConvId] = parsed
                }
            }
        }
        let body: [String: Any] = [
            "client_op_id": msg.clientOpId,
            "message_type": msg.messageType,
            "compression_codec": "zlib",
            "encryption_scheme": "telefam_ecdh_v1",
            "ciphertext_b64": env.cipherB64,
            "sender_ciphertext_b64": env.selfCipherB64,
            "signal_msg_type": 3,
            "media_url": resolvedMediaUrl,
            "media_id": resolvedMediaId,
            "voice_duration_ms": msg.voiceDurationMs,
            "view_once_ttl_sec": msg.viewOnceTtlSec,
            "forwarded_from_user_id": msg.forwardedFromUserId
        ]
        guard let bodyData = try? JSONSerialization.data(withJSONObject: body) else { return false }
        let cidEnc = targetConvId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? targetConvId
        let req = authorizedRequest(path: "v1/chat/conversations/\(cidEnc)/messages", method: "POST", body: bodyData)
        guard let (data, http) = await perform(req), (200...299).contains(http.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let mObj = root["message"] as? [String: Any] else {
            return false
        }
        let persisted = parseMessage(mObj, fallbackPlain: msg.plainText, localPath: msg.mediaLocalPath)
        guard !persisted.messageId.isEmpty else { return false }
        if targetConvId != msg.conversationId {
            for (mid, var existingMsg) in allMsgs where existingMsg.conversationId == msg.conversationId {
                existingMsg.conversationId = targetConvId
                allMsgs[mid] = existingMsg
            }
        }
        allMsgs.removeValue(forKey: msg.messageId)
        allMsgs[persisted.messageId] = persisted
        if var c = allConvs[targetConvId] {
            c.lastMessageId = persisted.messageId
            c.lastMessageType = persisted.messageType
            c.lastSenderId = persisted.senderUserId
            c.lastDeliveryStatus = persisted.deliveryStatus
            c.lastMessagePreviewPlain = previewText(for: persisted)
            c.lastMessageAtMs = persisted.createdAtMs
            c.updatedAtMs = persisted.createdAtMs
            allConvs[targetConvId] = c
        }
        saveCache()
        publishLists()
        if activeConversationId == targetConvId || activeConversationId == msg.conversationId {
            activeConversationId = targetConvId
            publishActiveMessages(targetConvId)
        }
        return true
    }

    func editMessage(_ msg: TelefamIosChatMessage, newText: String) {
        let clean = newText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        var updated = msg
        updated.plainText = clean
        updated.editedAtMs = nowMs
        allMsgs[msg.messageId] = updated
        refreshPreview(msg.conversationId)
        saveCache()
        publishLists()
        if activeConversationId == msg.conversationId { publishActiveMessages(msg.conversationId) }

        Task {
            guard let engine = e2ee,
                  let peerKey = await fetchPeerKey(peerUserId: msg.recipientUserId),
                  let env = engine.compressAndEncrypt(peerPubB64: peerKey, plaintext: clean),
                  let body = try? JSONSerialization.data(withJSONObject: [
                    "ciphertext_b64": env.cipherB64,
                    "sender_ciphertext_b64": env.selfCipherB64,
                    "signal_msg_type": 3
                  ]) else { return }
            let enc = msg.messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.messageId
            let req = authorizedRequest(path: "v1/chat/messages/\(enc)", method: "PATCH", body: body)
            if let (_, h) = await perform(req), (200...299).contains(h.statusCode) { return }
            pendingOps["edit_\(msg.messageId)"] = TelefamIosPendingChatOp(
                opId: "edit_\(msg.messageId)",
                opKind: "edit",
                conversationId: msg.conversationId,
                targetMessageId: msg.messageId,
                payloadJson: String(data: body, encoding: .utf8) ?? "{}",
                nextRetryAtMs: nowMs + 2500
            )
            saveCache()
        }
    }

    func unsendMessage(_ msg: TelefamIosChatMessage) {
        dissolvingIds.insert(msg.messageId)
        Task {
            try? await Task.sleep(nanoseconds: 420_000_000)
            if !msg.mediaLocalPath.isEmpty { try? FileManager.default.removeItem(atPath: msg.mediaLocalPath) }
            allMsgs.removeValue(forKey: msg.messageId)
            dissolvingIds.remove(msg.messageId)
            refreshPreview(msg.conversationId)
            saveCache()
            publishLists()
            if activeConversationId == msg.conversationId { publishActiveMessages(msg.conversationId) }
            let enc = msg.messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.messageId
            let req = authorizedRequest(path: "v1/chat/messages/\(enc)", method: "DELETE", query: "mode=unsend")
            _ = await perform(req)
        }
    }

    func deleteForSelf(_ msg: TelefamIosChatMessage) {
        if !msg.mediaLocalPath.isEmpty { try? FileManager.default.removeItem(atPath: msg.mediaLocalPath) }
        allMsgs.removeValue(forKey: msg.messageId)
        refreshPreview(msg.conversationId)
        saveCache()
        publishLists()
        if activeConversationId == msg.conversationId { publishActiveMessages(msg.conversationId) }
        Task {
            let enc = msg.messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.messageId
            let req = authorizedRequest(path: "v1/chat/messages/\(enc)", method: "DELETE", query: "mode=self")
            _ = await perform(req)
        }
    }

    func forwardMessage(_ msg: TelefamIosChatMessage, toConv: TelefamIosChatConversation) {
        if toConv.permissions.isBlocked {
            showToast("Cannot message blocked user")
            return
        }
        sendEnvelope(
            convId: toConv.conversationId,
            peerUserId: toConv.peerUserId,
            type: msg.messageType == "voice_once" ? "voice" : msg.messageType,
            plainText: msg.plainText,
            mediaUrl: msg.mediaUrl,
            mediaLocalPath: msg.mediaLocalPath,
            mediaId: msg.mediaId,
            voiceDurationMs: msg.voiceDurationMs,
            viewOnceTtlSec: 0,
            forwardedFrom: msg.senderUserId
        )
        showToast("Forwarded to \(toConv.peerFullName.isEmpty ? toConv.peerUsername : toConv.peerFullName)")
    }

    func unblockPeer(convId: String, peerUserId: String) {
        if var c = allConvs[convId] {
            c.permissions.isBlocked = false
            allConvs[convId] = c
            saveCache()
            publishLists()
        }
        Task {
            let enc = peerUserId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? peerUserId
            let req = authorizedRequest(path: "v1/users/\(enc)/block", method: "DELETE")
            _ = await perform(req)
            await syncMessages(convId: convId)
        }
    }

    func reportPeer(convId: String, peerUserId: String, reason: String) {
        Task {
            guard let body = try? JSONSerialization.data(withJSONObject: ["reason": reason, "details": "Reported from 1-on-1 chat \(convId)"]) else { return }
            let enc = peerUserId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? peerUserId
            let req = authorizedRequest(path: "v1/users/\(enc)/report", method: "POST", body: body)
            if let (_, h) = await perform(req), (200...299).contains(h.statusCode) {
                showToast("Report submitted")
            }
        }
    }

    func batchAction(_ ids: [String], action: String) {
        guard !ids.isEmpty else { return }
        let snap = ids.compactMap { allConvs[$0] }
        for cid in ids {
            guard var c = allConvs[cid] else { continue }
            switch action {
            case "delete": c.isDeletedLocally = true
            case "archive": c.isArchived = true
            case "unarchive": c.isArchived = false
            case "mute": c.isMuted = true
            case "unmute": c.isMuted = false
            case "mark_read": c.unreadCount = 0
            case "mark_unread": if c.unreadCount == 0 { c.unreadCount = 1 }
            case "block": c.permissions.isBlocked = true
            default: break
            }
            allConvs[cid] = c
        }
        saveCache()
        publishLists()
        if action != "block" {
            undoBanner = TelefamIosChatUndoBanner(message: "Updated \(ids.count) chat(s)", previousAction: action, snapshot: snap)
        }
        Task {
            guard let body = try? JSONSerialization.data(withJSONObject: ["conversation_ids": ids, "action": action]) else { return }
            let req = authorizedRequest(path: "v1/chat/conversations/batch-action", method: "POST", body: body)
            _ = await perform(req)
        }
    }

    func undoLastBatch() {
        guard let b = undoBanner else { return }
        undoBanner = nil
        for c in b.snapshot { allConvs[c.conversationId] = c }
        saveCache()
        publishLists()
        let inverse: String? = switch b.previousAction {
        case "archive": "unarchive"
        case "mute": "unmute"
        case "mark_read": "mark_unread"
        case "mark_unread": "mark_read"
        case "delete": "unarchive"
        default: nil
        }
        guard let inv = inverse else { return }
        let ids = b.snapshot.map(\.conversationId)
        Task {
            guard let body = try? JSONSerialization.data(withJSONObject: ["conversation_ids": ids, "action": inv]) else { return }
            let req = authorizedRequest(path: "v1/chat/conversations/batch-action", method: "POST", body: body)
            _ = await perform(req)
        }
    }

    func toggleVoicePlayback(_ msg: TelefamIosChatMessage) {
        if msg.messageType == "voice_once" {
            openViewOnce(msg)
            return
        }
        if playingMessageId == msg.messageId {
            audio.stopPlayback()
            playingMessageId = nil
            return
        }
        Task {
            guard let url = await ensureLocalVoiceUrl(msg) else { return }
            playingMessageId = msg.messageId
            let ok = audio.playFile(url) { [weak self] in
                Task { @MainActor in self?.playingMessageId = nil }
            }
            if !ok { playingMessageId = nil }
        }
    }

    func openViewOnce(_ msg: TelefamIosChatMessage) {
        if msg.senderUserId == attachedUserId { return }
        Task {
            guard let url = await ensureLocalVoiceUrl(msg) else { return }
            await recordFlag(
                convId: msg.conversationId,
                msgId: msg.messageId,
                type: "view_once_opened",
                details: "View-once voice note opened (\(msg.viewOnceTtlSec > 0 ? "\(msg.viewOnceTtlSec)s timer" : "play once"))"
            )
            let enc = msg.messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.messageId
            let req = authorizedRequest(path: "v1/chat/messages/\(enc)/view-once/open", method: "POST", body: Data("{}".utf8))
            _ = await perform(req)
            activeViewOnceMsg = msg
            playingMessageId = msg.messageId
            _ = audio.playFile(url) { [weak self] in
                Task { @MainActor in self?.consumeViewOnce(msg) }
            }
            burnTask?.cancel()
            if msg.viewOnceTtlSec > 0 {
                burnTask = Task {
                    for s in stride(from: msg.viewOnceTtlSec, through: 1, by: -1) {
                        viewOnceRemainingSec = s
                        try? await Task.sleep(nanoseconds: 1_000_000_000)
                    }
                    viewOnceRemainingSec = 0
                    consumeViewOnce(msg)
                }
            }
        }
    }

    func consumeViewOnce(_ msg: TelefamIosChatMessage) {
        burnTask?.cancel()
        burnTask = nil
        audio.stopPlayback()
        playingMessageId = nil
        activeViewOnceMsg = nil
        if !msg.mediaLocalPath.isEmpty { try? FileManager.default.removeItem(atPath: msg.mediaLocalPath) }
        allMsgs.removeValue(forKey: msg.messageId)
        refreshPreview(msg.conversationId)
        saveCache()
        publishLists()
        if activeConversationId == msg.conversationId { publishActiveMessages(msg.conversationId) }
        Task {
            let enc = msg.messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? msg.messageId
            let req = authorizedRequest(path: "v1/chat/messages/\(enc)/view-once/consume", method: "POST", body: Data("{}".utf8))
            _ = await perform(req)
        }
    }

    private func handleSystemScreenshotDetected() async {
        guard let cid = activeConversationId, let conv = allConvs[cid] else { return }
        if activeViewOnceMsg != nil {
            await recordFlag(convId: cid, msgId: activeViewOnceMsg?.messageId ?? "", type: "view_once_capture_attempt", details: "Screen capture detected during view-once voice playback")
        } else if !conv.permissions.canScreenshot {
            await recordFlag(convId: cid, msgId: "", type: "screenshot_disallowed", details: "Screenshot taken while screenshots are restricted")
        }
    }

    func recordFlag(convId: String, msgId: String, type: String, details: String) async {
        let fid = UUID().uuidString
        let flag = TelefamIosChatSecurityFlag(
            flagId: fid,
            conversationId: convId,
            messageId: msgId,
            actorUserId: attachedUserId,
            targetUserId: allConvs[convId]?.peerUserId ?? "",
            flagType: type,
            details: details,
            isSynced: false,
            createdAtMs: Int64(Date().timeIntervalSince1970 * 1000)
        )
        allFlags[fid] = flag
        saveCache()
        if activeConversationId == convId { publishActiveMessages(convId) }
        let enc = convId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? convId
        if let body = try? JSONSerialization.data(withJSONObject: ["message_id": msgId, "flag_type": type, "details": details]) {
            let req = authorizedRequest(path: "v1/chat/conversations/\(enc)/flags", method: "POST", body: body)
            if let (_, h) = await perform(req), (200...299).contains(h.statusCode) {
                allFlags[fid]?.isSynced = true
                saveCache()
            }
        }
    }

    func runGlobalSearch(_ query: String) {
        let clean = query.trimmingCharacters(in: .whitespacesAndNewlines)
        searchConvs = allConvs.values.filter { !$0.isDeletedLocally }.filter {
            clean.isEmpty || $0.peerFullName.localizedCaseInsensitiveContains(clean) ||
                $0.peerUsername.localizedCaseInsensitiveContains(clean) ||
                $0.lastMessagePreviewPlain.localizedCaseInsensitiveContains(clean)
        }.sorted { $0.lastMessageAtMs > $1.lastMessageAtMs }
        searchMessages = clean.isEmpty ? [] : allMsgs.values.filter {
            $0.plainText.localizedCaseInsensitiveContains(clean)
        }.sorted { $0.createdAtMs > $1.createdAtMs }.prefix(25).map { $0 }

        guard !clean.isEmpty else { searchPeople = []; return }
        let qEnc = clean.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? clean
        Task {
            let req = authorizedRequest(path: "v1/chat/search", method: "GET", query: "q=\(qEnc)")
            guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
                  let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let people = root["people"] as? [[String: Any]] else { return }
            searchPeople = people.map { p in
                TelefamIosChatSearchPerson(
                    userId: p["user_id"] as? String ?? "",
                    fullName: p["full_name"] as? String ?? "",
                    username: p["username"] as? String ?? "",
                    avatarUrl: p["avatar_url"] as? String ?? "",
                    location: p["location"] as? String ?? "",
                    conversationId: p["conversation_id"] as? String ?? ""
                )
            }
        }
    }

    func searchGifly(type: String, query: String, page: Int) async -> ([TelefamIosGiflyItem], Bool) {
        let qEnc = query.trimmingCharacters(in: .whitespacesAndNewlines)
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let req = authorizedRequest(path: "v1/chat/stickers/search", method: "GET", query: "type=\(type)&q=\(qEnc)&page=\(page)&per_page=24")
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return ([], false)
        }
        let arr = (root["items"] as? [[String: Any]] ?? []).map(parseGifly)
        let hasNext = root["has_next"] as? Bool ?? false
        return (arr, hasNext)
    }

    func fetchGreetingSticker(peerUserId: String) async {
        let qEnc = peerUserId.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let req = authorizedRequest(path: "v1/chat/stickers/greeting", method: "GET", query: "peer_id=\(qEnc)")
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let item = root["item"] as? [String: Any] else { return }
        greetingSticker = parseGifly(item)
    }

    func sendTypingWs(convId: String, mode: String) {
        guard let json = try? JSONSerialization.data(withJSONObject: ["event": "typing", "conversation_id": convId, "typing_mode": mode]),
              let str = String(data: json, encoding: .utf8) else { return }
        wsTask?.send(.string(str)) { _ in }
    }

    func sendCallSignalWs(callId: String, payload: [String: Any]) {
        var envelope = payload
        envelope["event"] = "call_signal"
        envelope["call_id"] = callId
        guard let json = try? JSONSerialization.data(withJSONObject: envelope),
              let str = String(data: json, encoding: .utf8) else { return }
        wsTask?.send(.string(str)) { _ in }
    }

    func refreshConversationFromCall(_ convId: String) async {
        guard !convId.isEmpty, !isDetached else { return }
        await syncMessages(convId: convId, markAsRead: (activeConversationId == convId))
        await refreshConversations(archived: false)
    }

    func showToast(_ msg: String) {
        toastMessage = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in
            if self?.toastMessage == msg { self?.toastMessage = nil }
        }
    }

    private func connectWebSocket() {
        wsTask?.cancel(with: .goingAway, reason: nil)
        wsTask = nil
        let tok = tokenProvider()
        guard !isDetached, !attachedUserId.isEmpty, !tok.isEmpty else { return }
        guard var comps = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else { return }
        comps.scheme = (baseURL.scheme == "https") ? "wss" : "ws"
        comps.path = "/v1/chat/ws"
        guard let wsUrl = comps.url else { return }
        var req = URLRequest(url: wsUrl)
        req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization")
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        let task = URLSession.shared.webSocketTask(with: req)
        wsTask = task
        task.resume()
        receiveNextWsFrame(task)
    }

    private func receiveNextWsFrame(_ task: URLSessionWebSocketTask) {
        task.receive { [weak self] res in
            guard let self else { return }
            switch res {
            case .success(let msg):
                if case .string(let text) = msg, let data = text.data(using: .utf8),
                   let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    Task { @MainActor in await self.handleWsDict(obj) }
                }
                self.receiveNextWsFrame(task)
            case .failure(let err):
                Task { @MainActor in
                    guard !self.isDetached, !self.tokenProvider().isEmpty, !self.attachedUserId.isEmpty else { return }
                    let closeRaw = task.closeCode.rawValue
                    let desc = err.localizedDescription
                    if closeRaw == 4001 || closeRaw == 4401 || closeRaw == 4403 || desc.contains("401") || desc.contains("403") {
                        return
                    }
                    try? await Task.sleep(nanoseconds: 4_000_000_000)
                    guard !self.isDetached, !self.tokenProvider().isEmpty, !self.attachedUserId.isEmpty else { return }
                    self.connectWebSocket()
                }
            }
        }
    }

    private func handleWsDict(_ obj: [String: Any]) async {
        guard !isDetached, !attachedUserId.isEmpty else { return }
        let ev = obj["event"] as? String ?? ""
        let cid = obj["conversation_id"] as? String ?? ""
        let uid = obj["user_id"] as? String ?? ""
        let mid = obj["message_id"] as? String ?? ""
        switch ev {
        case "typing":
            if !cid.isEmpty && uid != attachedUserId {
                let mode = obj["typing_mode"] as? String ?? ""
                typingModeByConv[cid] = mode
                if mode == "typing" {
                    let muted = allConvs[cid]?.isMuted == true
                    audio.playTone(freqStart: 420, freqEnd: 480, durationMs: 32, gain: 0.08, enabled: soundsEnabledProvider() && !muted)
                }
                Task {
                    try? await Task.sleep(nanoseconds: 4_000_000_000)
                    if typingModeByConv[cid] == mode { typingModeByConv.removeValue(forKey: cid) }
                }
            }
        case "presence":
            for (k, var c) in allConvs where c.peerUserId == uid {
                c.isOnline = obj["is_online"] as? Bool ?? false
                c.lastSeenLabel = obj["last_seen_label"] as? String ?? c.lastSeenLabel
                allConvs[k] = c
            }
            publishLists()
        case "new_message", "message_new", "message_edited", "conversation_updated":
            if !cid.isEmpty { await syncMessages(convId: cid, markAsRead: (activeConversationId == cid)) }
            await refreshConversations(archived: false)
        case "message_unsent", "view_once_burned", "view_once_consumed":
            if let old = allMsgs.removeValue(forKey: mid), !old.mediaLocalPath.isEmpty {
                try? FileManager.default.removeItem(atPath: old.mediaLocalPath)
            }
            if !cid.isEmpty {
                refreshPreview(cid)
                TelefamIosNotificationManager.shared.cancelConversationNotifications(conversationId: cid)
            }
            saveCache()
            publishLists()
            if activeConversationId == cid { publishActiveMessages(cid) }
        case "message_receipt":
            if var m = allMsgs[mid], let st = obj["status"] as? String {
                m.deliveryStatus = st
                allMsgs[mid] = m
                if var c = allConvs[m.conversationId], c.lastMessageId == mid {
                    c.lastDeliveryStatus = st
                    allConvs[m.conversationId] = c
                }
                saveCache()
                publishLists()
                if activeConversationId == m.conversationId { publishActiveMessages(m.conversationId) }
            }
        case "call_incoming", "call_ringing", "call_accepted", "call_signal", "call_ended":
            onCallWsEvent?(ev, obj)
            if ev == "call_ended" {
                if !cid.isEmpty { await syncMessages(convId: cid, markAsRead: (activeConversationId == cid)) }
                await refreshConversations(archived: false)
            }
        default: break
        }
    }

    private func startMaintenanceLoop() {
        maintenanceTask?.cancel()
        maintenanceTask = Task {
            while !Task.isCancelled && !isDetached {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                guard !isDetached, !tokenProvider().isEmpty else { break }
                await flushOutbox()
                if let cid = activeConversationId { await syncMessages(convId: cid, markAsRead: true) }
            }
        }
    }

    private func flushOutbox() async {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        for op in pendingOps.values where op.nextRetryAtMs <= now {
            let ok: Bool
            switch op.opKind {
            case "send":
                if let m = allMsgs[op.targetMessageId] { ok = await tryDeliver(m) } else { ok = true }
            case "edit":
                if let body = op.payloadJson.data(using: .utf8) {
                    let enc = op.targetMessageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? op.targetMessageId
                    let req = authorizedRequest(path: "v1/chat/messages/\(enc)", method: "PATCH", body: body)
                    if let (_, h) = await perform(req), (200...299).contains(h.statusCode) { ok = true } else { ok = false }
                } else { ok = true }
            default:
                ok = true
            }
            if ok {
                pendingOps.removeValue(forKey: op.opId)
            } else {
                var next = op
                next.retryCount += 1
                next.nextRetryAtMs = now + min(30_000, Int64(2000 * next.retryCount))
                pendingOps[op.opId] = next
            }
        }
        saveCache()
    }

    private func ensureKeysUploaded() async {
        guard let engine = e2ee, !engine.keysUploaded,
              let body = try? JSONSerialization.data(withJSONObject: engine.exportUploadBody()) else { return }
        let req = authorizedRequest(path: "v1/chat/e2ee/keys", method: "PUT", body: body)
        if let (_, h) = await perform(req), (200...299).contains(h.statusCode) { engine.markUploaded() }
    }

    private func fetchPeerKey(peerUserId: String) async -> String? {
        if let k = peerKeys[peerUserId], !k.isEmpty { return k }
        let enc = peerUserId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? peerUserId
        let req = authorizedRequest(path: "v1/chat/e2ee/keys/\(enc)", method: "GET")
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let m = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        let spk = m["signed_prekey_pub_b64"] as? String ?? ""
        let idp = m["identity_pub_b64"] as? String ?? ""
        let chosen = spk.isEmpty ? idp : spk
        if !chosen.isEmpty { peerKeys[peerUserId] = chosen }
        return chosen.isEmpty ? nil : chosen
    }

    func refreshConversations(archived: Bool) async {
        let req = authorizedRequest(path: "v1/chat/conversations", method: "GET", query: "archived=\(archived)")
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let list = root["conversations"] as? [[String: Any]] else { return }
        for item in list {
            var c = parseConversation(item)
            if let prev = allConvs[c.conversationId] {
                if c.lastMessagePreviewPlain.isEmpty { c.lastMessagePreviewPlain = prev.lastMessagePreviewPlain }
                c.isDeletedLocally = prev.isDeletedLocally && c.lastMessageAtMs <= prev.lastMessageAtMs
            }
            if !c.conversationId.isEmpty { allConvs[c.conversationId] = c }
        }
        saveCache()
        publishLists()
    }

    private func syncMessages(convId: String, markAsRead: Bool = false) async {
        guard !isDetached, !attachedUserId.isEmpty else { return }
        let sinceMs = allMsgs.values.filter { $0.conversationId == convId && $0.syncState == "synced" }
            .map { max($0.createdAtMs, $0.editedAtMs) }.max() ?? 0
        let enc = convId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? convId
        let req = authorizedRequest(path: "v1/chat/conversations/\(enc)/messages", method: "GET", query: sinceMs > 0 ? "since_ms=\(sinceMs)" : nil)
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        if let cObj = root["conversation"] as? [String: Any] {
            var parsed = parseConversation(cObj)
            if !parsed.conversationId.isEmpty {
                if markAsRead {
                    parsed.unreadCount = 0
                } else if parsed.lastMessagePreviewPlain.isEmpty, let prev = allConvs[parsed.conversationId] {
                    parsed.lastMessagePreviewPlain = prev.lastMessagePreviewPlain
                }
                allConvs[parsed.conversationId] = parsed
            }
        }
        var newIncoming = false
        var ackDeliveredIds: [String] = []
        var ackSeenIds: [String] = []

        for item in (root["messages"] as? [[String: Any]] ?? []) {
            let serverId = item["message_id"] as? String ?? ""
            let clientOpId = item["client_op_id"] as? String ?? ""
            let pendingMatch = clientOpId.isEmpty ? nil : allMsgs.values.first {
                $0.messageId != serverId && ($0.messageId == clientOpId || $0.clientOpId == clientOpId)
            }
            let existing = allMsgs[serverId] ?? pendingMatch
            let msg = parseMessage(
                item,
                fallbackPlain: existing?.plainText ?? "",
                localPath: existing?.mediaLocalPath ?? ""
            )
            if !msg.messageId.isEmpty {
                if let pm = pendingMatch {
                    allMsgs.removeValue(forKey: pm.messageId)
                }
                if existing == nil && msg.senderUserId != attachedUserId { newIncoming = true }
                allMsgs[msg.messageId] = msg
                if msg.senderUserId != attachedUserId && msg.deliveryStatus != "seen" {
                    if markAsRead {
                        ackSeenIds.append(msg.messageId)
                    } else if msg.deliveryStatus == "sent" {
                        ackDeliveredIds.append(msg.messageId)
                    }
                }
            }
        }

        if markAsRead {
            let showReceipts = allConvs[convId]?.permissions.showReadReceipts ?? true
            let rawTargets: [String]
            if showReceipts {
                rawTargets = ackSeenIds + allMsgs.values.filter {
                    $0.conversationId == convId && $0.senderUserId != attachedUserId && $0.deliveryStatus != "seen"
                }.map(\.messageId)
            } else {
                rawTargets = ackDeliveredIds + allMsgs.values.filter {
                    $0.conversationId == convId && $0.senderUserId != attachedUserId && $0.deliveryStatus == "sent"
                }.map(\.messageId)
            }
            let desired = showReceipts ? "seen" : "delivered"
            var seenSet = Set<String>()
            for mid in rawTargets where seenSet.insert(mid).inserted {
                if let body = try? JSONSerialization.data(withJSONObject: ["status": desired]) {
                    let mEnc = mid.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? mid
                    if let (rData, rh) = await perform(authorizedRequest(path: "v1/chat/messages/\(mEnc)/receipt", method: "POST", body: body)),
                       (200...299).contains(rh.statusCode) {
                        let st = ((try? JSONSerialization.jsonObject(with: rData) as? [String: Any])?["delivery_status"] as? String) ?? desired
                        allMsgs[mid]?.deliveryStatus = st
                    }
                }
            }
            if !showReceipts, let body = try? JSONSerialization.data(withJSONObject: ["conversation_ids": [convId], "action": "mark_read"]) {
                _ = await perform(authorizedRequest(path: "v1/chat/conversations/batch-action", method: "POST", body: body))
            }
        } else if !ackDeliveredIds.isEmpty {
            var seenSet = Set<String>()
            for mid in ackDeliveredIds where seenSet.insert(mid).inserted {
                if let body = try? JSONSerialization.data(withJSONObject: ["status": "delivered"]) {
                    let mEnc = mid.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? mid
                    if let (rData, rh) = await perform(authorizedRequest(path: "v1/chat/messages/\(mEnc)/receipt", method: "POST", body: body)),
                       (200...299).contains(rh.statusCode) {
                        let st = ((try? JSONSerialization.jsonObject(with: rData) as? [String: Any])?["delivery_status"] as? String) ?? "delivered"
                        allMsgs[mid]?.deliveryStatus = st
                    }
                }
            }
        }

        for fItem in (root["security_flags"] as? [[String: Any]] ?? []) {
            let flag = TelefamIosChatSecurityFlag(
                flagId: fItem["flag_id"] as? String ?? "",
                conversationId: fItem["conversation_id"] as? String ?? convId,
                messageId: fItem["message_id"] as? String ?? "",
                actorUserId: fItem["actor_user_id"] as? String ?? "",
                targetUserId: fItem["target_user_id"] as? String ?? "",
                flagType: fItem["flag_type"] as? String ?? "",
                details: fItem["details"] as? String ?? "",
                isSynced: true,
                createdAtMs: (fItem["created_at_ms"] as? NSNumber)?.int64Value ?? 0
            )
            if !flag.flagId.isEmpty { allFlags[flag.flagId] = flag }
        }
        refreshPreview(convId)
        if markAsRead, var c = allConvs[convId] {
            c.unreadCount = 0
            allConvs[convId] = c
        }
        saveCache()
        publishLists()
        if activeConversationId == convId { publishActiveMessages(convId) }
        var newestIncomingMsg: TelefamIosChatMessage? = nil
        for item in (root["messages"] as? [[String: Any]] ?? []) {
            let serverId = item["message_id"] as? String ?? ""
            if let m = allMsgs[serverId], m.senderUserId != attachedUserId {
                if newestIncomingMsg == nil || m.createdAtMs > newestIncomingMsg!.createdAtMs {
                    newestIncomingMsg = m
                }
            }
        }
        if newIncoming {
            let muted = allConvs[convId]?.isMuted == true
            audio.playTone(freqStart: 580, freqEnd: 820, durationMs: 95, gain: 0.24, enabled: soundsEnabledProvider() && !muted)
            if !markAsRead && activeConversationId != convId, let latest = newestIncomingMsg {
                let conv = allConvs[convId]
                let senderLabel = (conv?.peerFullName.isEmpty == false ? conv?.peerFullName : conv?.peerUsername) ?? ""
                TelefamIosNotificationManager.shared.notifyIncomingChatMessage(
                    conversationId: convId,
                    message: latest,
                    senderName: senderLabel,
                    isMuted: muted
                )
            }
        }
    }

    // MARK: - Notification Quick Actions & Remote Push E2EE Decryption

    func markConversationReadFromNotification(conversationId: String) async {
        attachIfNeeded()
        guard !isDetached, !attachedUserId.isEmpty else { return }
        if var c = allConvs[conversationId], c.unreadCount > 0 {
            c.unreadCount = 0
            allConvs[conversationId] = c
            saveCache()
            publishLists()
        }
        await syncMessages(convId: conversationId, markAsRead: true)
    }

    func sendQuickReplyFromNotification(conversationId: String, peerUserId: String, text: String) async {
        attachIfNeeded()
        guard !isDetached, !attachedUserId.isEmpty else { return }
        await markConversationReadFromNotification(conversationId: conversationId)
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        sendText(convId: conversationId, peerUserId: peerUserId, text: clean)
    }

    func decryptRemotePushEnvelope(
        conversationId: String,
        messageId: String,
        senderUserId: String,
        senderName: String,
        senderUsername: String,
        messageType: String,
        compressionCodec: String,
        ciphertextB64: String,
        isViewOnce: Bool,
        createdAtMs: Int64
    ) async -> (preview: String, isMuted: Bool, canReply: Bool) {
        attachIfNeeded()
        let conv = allConvs[conversationId]
        let isMuted = conv?.isMuted == true
        let canReply = !isViewOnce && !(conv?.permissions.isBlocked == true)

        if !messageId.isEmpty, let body = try? JSONSerialization.data(withJSONObject: ["status": "delivered"]) {
            let mEnc = messageId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? messageId
            _ = await perform(authorizedRequest(path: "v1/chat/messages/\(mEnc)/receipt", method: "POST", body: body))
        }

        if isViewOnce || messageType == "voice_once" {
            return ("Sent a view-once voice message", isMuted, false)
        }

        let decryptedPlain: String
        switch messageType {
        case "voice": decryptedPlain = "Sent a voice message"
        case "sticker": decryptedPlain = "Sent a sticker"
        case "gif": decryptedPlain = "Sent a GIF"
        case "clip": decryptedPlain = "Sent a clip"
        default:
            if !ciphertextB64.isEmpty,
               let plain = e2ee?.decryptAndDecompress(isOwn: false, ciphertextB64: ciphertextB64, senderCiphertextB64: "", codec: compressionCodec),
               !plain.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                decryptedPlain = plain
            } else {
                decryptedPlain = "New encrypted message"
            }
        }

        if !conversationId.isEmpty && !messageId.isEmpty && allMsgs[messageId] == nil {
            let cached = TelefamIosChatMessage(
                messageId: messageId,
                conversationId: conversationId,
                senderUserId: senderUserId,
                recipientUserId: attachedUserId,
                messageType: messageType,
                compressionCodec: compressionCodec,
                ciphertextB64: ciphertextB64,
                plainText: decryptedPlain,
                deliveryStatus: "delivered",
                syncState: "synced",
                createdAtMs: createdAtMs
            )
            allMsgs[messageId] = cached
            if var c = allConvs[conversationId] {
                c.lastMessageId = messageId
                c.lastMessageType = messageType
                c.lastSenderId = senderUserId
                c.lastMessagePreviewPlain = decryptedPlain
                c.lastDeliveryStatus = "delivered"
                c.lastMessageAtMs = createdAtMs
                c.unreadCount += 1
                allConvs[conversationId] = c
            }
            saveCache()
            publishLists()
        }
        return (decryptedPlain, isMuted, canReply)
    }

    func handleRemoteMessageCancel(conversationId: String, messageId: String) async {
        if !messageId.isEmpty, let old = allMsgs.removeValue(forKey: messageId), !old.mediaLocalPath.isEmpty {
            try? FileManager.default.removeItem(atPath: old.mediaLocalPath)
        }
        if !conversationId.isEmpty {
            refreshPreview(conversationId)
            saveCache()
            publishLists()
            if activeConversationId == conversationId {
                publishActiveMessages(conversationId)
            }
        }
    }

    private func uploadVoiceMultipart(convId: String, durationMs: Int64, viewOnce: Bool, bytes: Data, filename: String) async -> (mediaId: String, mediaUrl: String)? {
        let boundary = "TelefamIosVoice\(UUID().uuidString.replacingOccurrences(of: "-", with: ""))"
        var body = Data()
        func addField(_ name: String, _ val: String) {
            body.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(name)\"\r\n\r\n\(val)\r\n".data(using: .utf8)!)
        }
        addField("conversation_id", convId)
        addField("duration_ms", String(durationMs))
        addField("view_once", viewOnce ? "true" : "false")
        body.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\nContent-Type: audio/mp4\r\n\r\n".data(using: .utf8)!)
        body.append(bytes)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        let req = authorizedRequest(path: "v1/chat/media/voice", method: "POST", contentType: "multipart/form-data; boundary=\(boundary)", body: body, compressJson: false)
        guard let (data, h) = await perform(req), (200...299).contains(h.statusCode),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let mUrl = obj["media_url"] as? String, !mUrl.isEmpty else { return nil }
        return (obj["media_id"] as? String ?? "", mUrl)
    }

    private func ensureLocalVoiceUrl(_ msg: TelefamIosChatMessage) async -> URL? {
        if !msg.mediaLocalPath.isEmpty && FileManager.default.fileExists(atPath: msg.mediaLocalPath) {
            return URL(fileURLWithPath: msg.mediaLocalPath)
        }
        guard !msg.mediaUrl.isEmpty,
              let resolved = URL(string: msg.mediaUrl, relativeTo: baseURL) else { return nil }
        var req = URLRequest(url: resolved)
        let tok = tokenProvider()
        if !tok.isEmpty { req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization") }
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        guard let (data, h) = try? await URLSession.shared.data(for: req),
              let http = h as? HTTPURLResponse, (200...299).contains(http.statusCode) else { return nil }
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first ?? FileManager.default.temporaryDirectory
        let dir = base.appendingPathComponent("TelefamVoices", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let isRiff = data.count >= 4 && data.prefix(4) == Data("RIFF".utf8)
        let ext = isRiff ? "wav" : "m4a"
        let dest = dir.appendingPathComponent("dl_\(msg.messageId).\(ext)")
        try? data.write(to: dest)
        allMsgs[msg.messageId]?.mediaLocalPath = dest.path
        saveCache()
        return dest
    }

    private func authorizedRequest(path: String, method: String, query: String? = nil, contentType: String = "application/json", body: Data? = nil, compressJson: Bool = true) -> URLRequest {
        var url = baseURL.appendingPathComponent(path)
        if let query, !query.isEmpty, var comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            comps.percentEncodedQuery = query
            if let u = comps.url { url = u }
        }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 20)
        req.httpMethod = method
        req.setValue("gzip", forHTTPHeaderField: "Accept-Encoding")
        let tok = tokenProvider()
        if !tok.isEmpty { req.setValue("Bearer \(tok)", forHTTPHeaderField: "Authorization") }
        req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        if let body {
            req.setValue(contentType, forHTTPHeaderField: "Content-Type")
            if compressJson && contentType.contains("json") && body.count >= 256 {
                let gz = TelefamIosCompression.gzipCompress(body)
                if gz.count < body.count {
                    req.setValue("gzip", forHTTPHeaderField: "Content-Encoding")
                    req.httpBody = gz
                    return req
                }
            }
            req.httpBody = body
        }
        return req
    }

    private func perform(_ req: URLRequest) async -> (Data, HTTPURLResponse)? {
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              let http = resp as? HTTPURLResponse else { return nil }
        return (data, http)
    }

    private func parseConversation(_ m: [String: Any]) -> TelefamIosChatConversation {
        let lastMsgId = m["last_message_id"] as? String ?? ""
        let sender = m["last_sender_id"] as? String ?? ""
        let cB64 = m["last_ciphertext_b64"] as? String ?? ""
        let sB64 = m["last_sender_ciphertext_b64"] as? String ?? ""
        let cachedPlain = (!lastMsgId.isEmpty ? allMsgs[lastMsgId]?.plainText : nil)
            .flatMap { ($0.isEmpty || $0 == "[Encrypted message]") ? nil : $0 }
        let lastType = m["last_message_type"] as? String ?? ""
        let rawPlain = e2ee?.decryptAndDecompress(isOwn: sender == attachedUserId, ciphertextB64: cB64, senderCiphertextB64: sB64, codec: "zlib")
            ?? cachedPlain
            ?? defaultTypeLabel(lastType)
        let plain = (lastType == "call_log" || lastType == "call_request")
            ? TelefamIosCallMetaParser.summaryPreview(plainText: rawPlain, messageType: lastType)
            : rawPlain
        var perms = TelefamIosChatCapabilities()
        if let pDict = m["permissions"] as? [String: Any],
           let pData = try? JSONSerialization.data(withJSONObject: pDict),
           let decoded = try? JSONDecoder().decode(TelefamIosChatCapabilities.self, from: pData) {
            perms = decoded
        }
        return TelefamIosChatConversation(
            conversationId: m["conversation_id"] as? String ?? "",
            peerUserId: m["peer_user_id"] as? String ?? "",
            peerFullName: m["peer_full_name"] as? String ?? "",
            peerUsername: m["peer_username"] as? String ?? "",
            peerAvatarUrl: m["peer_avatar_url"] as? String ?? "",
            lastMessageId: m["last_message_id"] as? String ?? "",
            lastMessageType: m["last_message_type"] as? String ?? "",
            lastSenderId: sender,
            lastMessagePreviewPlain: plain,
            lastDeliveryStatus: m["last_delivery_status"] as? String ?? "sent",
            lastMessageAtMs: (m["last_message_at_ms"] as? NSNumber)?.int64Value ?? 0,
            unreadCount: (m["unread_count"] as? NSNumber)?.intValue ?? 0,
            isArchived: m["is_archived"] as? Bool ?? false,
            isMuted: m["is_muted"] as? Bool ?? false,
            isOnline: m["is_online"] as? Bool ?? false,
            lastSeenLabel: m["last_seen_label"] as? String ?? "last seen recently",
            permissions: perms,
            updatedAtMs: (m["updated_at_ms"] as? NSNumber)?.int64Value ?? 0
        )
    }

    private func parseMessage(_ m: [String: Any], fallbackPlain: String, localPath: String) -> TelefamIosChatMessage {
        let sender = m["sender_user_id"] as? String ?? ""
        let cB64 = m["ciphertext_b64"] as? String ?? ""
        let sB64 = m["sender_ciphertext_b64"] as? String ?? ""
        let codec = m["compression_codec"] as? String ?? "zlib"
        let plain = e2ee?.decryptAndDecompress(isOwn: sender == attachedUserId, ciphertextB64: cB64, senderCiphertextB64: sB64, codec: codec)
            ?? (fallbackPlain.isEmpty ? "[Encrypted message]" : fallbackPlain)
        return TelefamIosChatMessage(
            messageId: m["message_id"] as? String ?? "",
            conversationId: m["conversation_id"] as? String ?? "",
            senderUserId: sender,
            recipientUserId: m["recipient_user_id"] as? String ?? "",
            clientOpId: m["client_op_id"] as? String ?? "",
            messageType: m["message_type"] as? String ?? "text",
            compressionCodec: codec,
            encryptionScheme: m["encryption_scheme"] as? String ?? "telefam_ecdh_v1",
            ciphertextB64: cB64,
            senderCiphertextB64: sB64,
            signalMsgType: (m["signal_msg_type"] as? NSNumber)?.intValue ?? 3,
            plainText: plain,
            mediaUrl: m["media_url"] as? String ?? "",
            mediaLocalPath: localPath,
            mediaId: m["media_id"] as? String ?? "",
            voiceDurationMs: (m["voice_duration_ms"] as? NSNumber)?.int64Value ?? 0,
            viewOnceTtlSec: (m["view_once_ttl_sec"] as? NSNumber)?.intValue ?? 0,
            viewOnceOpenedAtMs: (m["view_once_opened_at_ms"] as? NSNumber)?.int64Value ?? 0,
            viewOnceExpiresAtMs: (m["view_once_expires_at_ms"] as? NSNumber)?.int64Value ?? 0,
            forwardedFromUserId: m["forwarded_from_user_id"] as? String ?? "",
            deliveryStatus: m["delivery_status"] as? String ?? "sent",
            syncState: "synced",
            editedAtMs: (m["edited_at_ms"] as? NSNumber)?.int64Value ?? 0,
            createdAtMs: (m["created_at_ms"] as? NSNumber)?.int64Value ?? 0
        )
    }

    private func parseGifly(_ m: [String: Any]) -> TelefamIosGiflyItem {
        TelefamIosGiflyItem(
            id: (m["id"] as? NSNumber)?.int64Value ?? 0,
            slug: m["slug"] as? String ?? "",
            title: m["title"] as? String ?? "",
            type: m["type"] as? String ?? "sticker",
            preview: m["preview"] as? String ?? "",
            previewWebp: m["preview_webp"] as? String ?? "",
            url: m["url"] as? String ?? "",
            urlHd: m["url_hd"] as? String ?? ""
        )
    }

    private func previewText(for msg: TelefamIosChatMessage) -> String {
        switch msg.messageType {
        case "sticker": return "Sticker"
        case "gif": return "GIF"
        case "clip": return "Clip"
        case "voice": return "Voice message"
        case "voice_once": return "View-once voice"
        case "call_log", "call_request":
            return TelefamIosCallMetaParser.summaryPreview(plainText: msg.plainText, messageType: msg.messageType)
        default: return msg.plainText
        }
    }

    private func defaultTypeLabel(_ type: String) -> String {
        switch type {
        case "voice", "voice_once": return "Voice message"
        case "sticker": return "Sticker"
        case "gif": return "GIF"
        case "clip": return "Clip"
        case "call_log": return "Call"
        case "call_request": return "Call request"
        default: return ""
        }
    }

    private func refreshPreview(_ convId: String) {
        guard var c = allConvs[convId] else { return }
        if let latest = allMsgs.values.filter({ $0.conversationId == convId }).max(by: { $0.createdAtMs < $1.createdAtMs }) {
            c.lastMessageId = latest.messageId
            c.lastMessageType = latest.messageType
            c.lastSenderId = latest.senderUserId
            c.lastMessagePreviewPlain = previewText(for: latest)
            c.lastDeliveryStatus = latest.deliveryStatus
            c.lastMessageAtMs = latest.createdAtMs
        } else {
            c.lastMessageId = ""
            c.lastMessagePreviewPlain = ""
        }
        allConvs[convId] = c
    }

    private func publishLists() {
        let sorted = allConvs.values.filter { !$0.isDeletedLocally }.sorted { max($0.lastMessageAtMs, $0.updatedAtMs) > max($1.lastMessageAtMs, $1.updatedAtMs) }
        conversations = sorted.filter { !$0.isArchived }
        archivedConversations = sorted.filter { $0.isArchived }
    }

    private func publishActiveMessages(_ convId: String) {
        activeMessages = allMsgs.values.filter { $0.conversationId == convId }.sorted { $0.createdAtMs < $1.createdAtMs }
        activeSecurityFlags = allFlags.values.filter { $0.conversationId == convId }.sorted { $0.createdAtMs > $1.createdAtMs }
    }

    private func saveCache() {
        guard !attachedUserId.isEmpty else { return }
        let snap = CacheSnapshot(
            convs: Array(allConvs.values),
            msgs: Array(allMsgs.values),
            flags: Array(allFlags.values),
            ops: Array(pendingOps.values)
        )
        TelefamIosChatVault.writeEncrypted(snap, file: "chat_cache_\(attachedUserId).bin", uid: attachedUserId)
    }
}
