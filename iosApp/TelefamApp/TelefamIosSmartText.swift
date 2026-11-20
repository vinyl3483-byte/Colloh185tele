import Contacts
import ContactsUI
import Foundation
import SwiftUI
import UIKit
@preconcurrency import WebKit

// MARK: - Smart Text Entity Detection Engine

struct SmartPhonePayload: Identifiable, Equatable {
    let id = UUID()
    let rawText: String
    let dialNumber: String
    let formattedDisplay: String
}

struct SmartEmailPayload: Identifiable, Equatable {
    let id = UUID()
    let rawText: String
    let emailAddress: String
}

struct SmartUrlPayload: Identifiable, Equatable {
    let id = UUID()
    let rawText: String
    let normalizedUrl: String
    let domain: String
}

struct SmartOtpPayload: Identifiable, Equatable {
    let id = UUID()
    let rawText: String
    let code: String
}

enum SmartDetectedKind: Equatable {
    case email(SmartEmailPayload)
    case url(SmartUrlPayload)
    case otp(SmartOtpPayload)
    case phone(SmartPhonePayload)
}

struct SmartDetectedEntity: Identifiable, Equatable {
    let id: String
    let startOffset: Int // UTF-16 offset in NSString
    let endOffset: Int
    let rawText: String
    let kind: SmartDetectedKind
}

struct SmartTextAnalysisResult: Equatable {
    let text: String
    let entities: [SmartDetectedEntity]

    var urls: [SmartUrlPayload] {
        entities.compactMap {
            if case .url(let u) = $0.kind { return u }
            return nil
        }
    }

    var otpCodes: [SmartOtpPayload] {
        entities.compactMap {
            if case .otp(let o) = $0.kind { return o }
            return nil
        }
    }
}

enum SmartTextEngine {
    private static let maxCacheEntries = 256
    private static var cacheOrder: [String] = []
    private static var cacheMap: [String: SmartTextAnalysisResult] = [:]

    private static let dangerousSchemes = [
        "javascript:", "data:", "file:", "vbscript:", "about:",
        "content:", "intent:", "blob:", "ftp:", "tel:", "mailto:"
    ]

    private static let recognizedTlds: Set<String> = [
        "com", "org", "net", "edu", "gov", "io", "co", "app", "dev", "me", "tv", "ai",
        "gg", "ly", "be", "to", "ke", "uk", "us", "ca", "de", "fr", "jp", "cn", "in",
        "au", "nz", "za", "ng", "ug", "tz", "rw", "et", "gh", "eg", "ma", "info", "biz",
        "xyz", "tech", "online", "site", "store", "cloud", "news", "media", "studio",
        "company", "pro", "live", "page", "link", "click", "wiki", "blog", "shop", "club",
        "cc", "fm", "so", "it", "es", "nl", "ch", "se", "no", "dk", "fi", "pl", "ua",
        "br", "mx", "ar", "cl", "sg", "hk", "tw", "kr", "id", "my", "ph", "th", "vn",
        "ae", "sa", "il", "tr", "ac", "int", "mil", "name", "mobi", "africa"
    ]

    private static let unsafeSchemeRegex = try! NSRegularExpression(
        pattern: #"(?i)\b(?:javascript|data|file|vbscript|about|content|intent|blob):[^\s]*"#
    )
    private static let emailRegex = try! NSRegularExpression(
        pattern: #"(?<![A-Za-z0-9._%+\-])([A-Za-z0-9][A-Za-z0-9._%+\-]{0,63}@[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)+)"#
    )
    private static let urlRegex = try! NSRegularExpression(
        pattern: #"(?i)(?<![A-Za-z0-9@._\-/])((?:https?://)?(?:www\.)?(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,24}(?::\d{2,5})?(?:/[^\s<>"{}|\\^`\[\]]*)?(?:\?[^\s<>"{}|\\^`\[\]]*)?(?:#[^\s<>"{}|\\^`\[\]]*)?)"#
    )
    private static let otpPrefixRegex = try! NSRegularExpression(
        pattern: #"(?i)(?:verification\s*code|login\s*code|security\s*code|one[-\s]*time\s*(?:password|passcode|pin|code)|auth(?:entication)?\s*code|confirmation\s*code|access\s*code|reset\s*code|2fa\s*code|mfa\s*code|backup\s*code|passcode|otp|pin(?:\s*code)?|code)\s*(?:is|=|:|-)?\s*([0-9]{4,8})\b"#
    )
    private static let otpSuffixRegex = try! NSRegularExpression(
        pattern: #"(?i)(?:use|enter|input|type|is)\s+([0-9]{4,8})\s+(?:to\s+(?:verify|confirm|log\s*in|login|sign\s*in|authenticate|complete|unlock|activate)|as\s+(?:your\s+)?(?:otp|code|verification|passcode))"#
    )
    private static let phoneRegex = try! NSRegularExpression(
        pattern: #"(?<![A-Za-z0-9+.\-])(\+[1-9](?:[\s\-().]*\d){6,14}|\b0[17](?:[\s\-]*\d){8}\b|\b\(?[0-9]{3}\)?[\s\-][0-9]{3}[\s\-][0-9]{4}\b)"#
    )
    private static let currencyPrefixRegex = try! NSRegularExpression(
        pattern: #"(?i)(?:[$€£¥]|kes|ksh|usd|eur|gbp|ugx|tzs|ngn|zar)\s*$"#
    )
    private static let ipv4Regex = try! NSRegularExpression(
        pattern: #"^\d{1,3}(?:\.\d{1,3}){3}$"#
    )

    static func analyze(_ text: String) -> SmartTextAnalysisResult {
        if text.isEmpty {
            return SmartTextAnalysisResult(text: text, entities: [])
        }
        if let hit = cacheMap[text] {
            return hit
        }

        let nsText = text as NSString
        let fullRange = NSRange(location: 0, length: nsText.length)
        var taken: [Range<Int>] = []
        var entities: [SmartDetectedEntity] = []

        func overlaps(_ s: Int, _ e: Int) -> Bool {
            taken.contains { s < $0.upperBound && e > $0.lowerBound }
        }
        func claim(_ s: Int, _ e: Int) {
            if e > s { taken.append(s..<e) }
        }

        for m in unsafeSchemeRegex.matches(in: text, range: fullRange) {
            claim(m.range.location, m.range.location + m.range.length)
        }

        // 1. Emails
        for m in emailRegex.matches(in: text, range: fullRange) {
            let r = m.range(at: 1)
            guard r.location != NSNotFound else { continue }
            let raw = trimTrailingSentencePunctuation(nsText.substring(with: r))
            let start = r.location
            let end = start + (raw as NSString).length
            if end > start && !overlaps(start, end) && isValidEmail(raw) {
                claim(start, end)
                entities.append(
                    SmartDetectedEntity(
                        id: "email_\(start)_\(raw)",
                        startOffset: start,
                        endOffset: end,
                        rawText: raw,
                        kind: .email(SmartEmailPayload(rawText: raw, emailAddress: raw))
                    )
                )
            }
        }

        // 2. URLs
        for m in urlRegex.matches(in: text, range: fullRange) {
            let r = m.range(at: 1)
            guard r.location != NSNotFound else { continue }
            let start = r.location
            if start > 0 && nsText.substring(with: NSRange(location: start - 1, length: 1)) == "@" {
                continue
            }
            let rawCandidate = nsText.substring(with: r)
            let trimmed = trimUrlTrailingPunctuation(rawCandidate)
            let end = start + (trimmed as NSString).length
            if end <= start || overlaps(start, end) { continue }

            let lower = trimmed.lowercased()
            let hasScheme = lower.hasPrefix("http://") || lower.hasPrefix("https://")
            guard let normalized = normalizeSafeHttpUrl(trimmed, requireKnownTld: !hasScheme) else { continue }
            let domain = extractCleanDomain(normalized)
            if domain.isEmpty { continue }

            claim(start, end)
            entities.append(
                SmartDetectedEntity(
                    id: "url_\(start)_\(normalized)",
                    startOffset: start,
                    endOffset: end,
                    rawText: trimmed,
                    kind: .url(SmartUrlPayload(rawText: trimmed, normalizedUrl: normalized, domain: domain))
                )
            )
        }

        // 3. Context-aware OTP codes
        var otpRanges: [(NSRange, String)] = []
        for m in otpPrefixRegex.matches(in: text, range: fullRange) {
            let r = m.range(at: 1)
            if r.location != NSNotFound {
                otpRanges.append((r, nsText.substring(with: r)))
            }
        }
        for m in otpSuffixRegex.matches(in: text, range: fullRange) {
            let r = m.range(at: 1)
            if r.location != NSNotFound {
                otpRanges.append((r, nsText.substring(with: r)))
            }
        }
        for (r, digits) in otpRanges {
            let start = r.location
            let end = r.location + r.length
            guard !overlaps(start, end), isPlausibleOtpContext(nsText: nsText, start: start, end: end, digits: digits) else {
                continue
            }
            claim(start, end)
            entities.append(
                SmartDetectedEntity(
                    id: "otp_\(start)_\(digits)",
                    startOffset: start,
                    endOffset: end,
                    rawText: digits,
                    kind: .otp(SmartOtpPayload(rawText: digits, code: digits))
                )
            )
        }

        // 4. Phone Numbers
        for m in phoneRegex.matches(in: text, range: fullRange) {
            let r = m.range(at: 1)
            guard r.location != NSNotFound else { continue }
            let raw = trimTrailingSentencePunctuation(nsText.substring(with: r))
            let start = r.location
            let end = start + (raw as NSString).length
            guard end > start, !overlaps(start, end),
                  let dial = normalizeDialPhone(raw),
                  !looksLikePriceOrDateFragment(nsText: nsText, start: start, end: end) else {
                continue
            }
            claim(start, end)
            entities.append(
                SmartDetectedEntity(
                    id: "phone_\(start)_\(dial)",
                    startOffset: start,
                    endOffset: end,
                    rawText: raw,
                    kind: .phone(SmartPhonePayload(rawText: raw, dialNumber: dial, formattedDisplay: formatPhoneForDisplay(raw)))
                )
            )
        }

        let sorted = entities.sorted { $0.startOffset < $1.startOffset }
        let result = SmartTextAnalysisResult(text: text, entities: sorted)
        if cacheOrder.count >= maxCacheEntries, let oldest = cacheOrder.first {
            cacheOrder.removeFirst()
            cacheMap.removeValue(forKey: oldest)
        }
        cacheOrder.append(text)
        cacheMap[text] = result
        return result
    }

    static func normalizeSafeHttpUrl(_ raw: String, requireKnownTld: Bool = false) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed.count <= 2048,
              trimmed.rangeOfCharacter(from: .whitespacesAndNewlines) == nil else {
            return nil
        }
        let lower = trimmed.lowercased()
        if dangerousSchemes.contains(where: { lower.hasPrefix($0) }) { return nil }
        let hasScheme = lower.hasPrefix("http://") || lower.hasPrefix("https://")
        if !hasScheme && trimmed.contains("://") { return nil }

        let candidate = hasScheme ? trimmed : "https://\(trimmed)"
        guard let comps = URLComponents(string: candidate),
              let scheme = comps.scheme?.lowercased(),
              (scheme == "http" || scheme == "https"),
              comps.user == nil,
              comps.password == nil,
              let rawHost = comps.host?.lowercased() else {
            return nil
        }

        let host = rawHost.trimmingCharacters(in: CharacterSet(charactersIn: "."))
        if host.isEmpty || host.contains("..") || host.hasPrefix("-") || host.hasSuffix("-") { return nil }
        if host == "localhost" || host.hasSuffix(".localhost") || host.hasSuffix(".local") ||
            host.hasSuffix(".internal") || host == "metadata" || host == "metadata.google.internal" ||
            host.hasPrefix("fe80:") || host.hasPrefix("fc00:") || host.hasPrefix("fd00:") || host == "::1" {
            return nil
        }

        let hostNSRange = NSRange(location: 0, length: (host as NSString).length)
        if ipv4Regex.firstMatch(in: host, range: hostNSRange) != nil {
            if !isPublicIPv4Literal(host) { return nil }
        } else {
            let labels = host.split(separator: ".", omittingEmptySubsequences: false)
            if labels.count < 2 || labels.contains(where: { $0.isEmpty || $0.count > 63 }) { return nil }
            let tld = String(labels.last ?? "")
            if tld.count < 2 || !tld.allSatisfy({ $0.isLetter && $0.isASCII }) { return nil }
            if requireKnownTld && !recognizedTlds.contains(tld) { return nil }
        }

        var normalizedComps = comps
        normalizedComps.scheme = scheme
        normalizedComps.host = host
        return normalizedComps.string ?? candidate
    }

    static func extractCleanDomain(_ normalizedUrl: String) -> String {
        guard let comps = URLComponents(string: normalizedUrl), let host = comps.host?.lowercased() else {
            return ""
        }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    static func isValidEmail(_ raw: String) -> Bool {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let parts = trimmed.split(separator: "@", omittingEmptySubsequences: false)
        guard parts.count == 2 else { return false }
        let local = String(parts[0])
        let domain = String(parts[1]).lowercased()
        guard !local.isEmpty, local.count <= 64, domain.count <= 253 else { return false }
        if local.hasPrefix(".") || local.hasSuffix(".") || local.contains("..") { return false }
        if domain.hasPrefix(".") || domain.hasSuffix(".") || domain.contains("..") { return false }
        let labels = domain.split(separator: ".", omittingEmptySubsequences: false)
        guard labels.count >= 2, !labels.contains(where: { $0.isEmpty || $0.hasPrefix("-") || $0.hasSuffix("-") }) else {
            return false
        }
        let tld = String(labels.last ?? "")
        return (2...24).contains(tld.count) && tld.allSatisfy { $0.isLetter && $0.isASCII }
    }

    static func formatPhoneForDisplay(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasPlus = trimmed.hasPrefix("+")
        let digits = trimmed.filter { $0.isNumber && $0.isASCII }
        guard !digits.isEmpty else { return trimmed }
        if hasPlus && digits.hasPrefix("254") && digits.count == 12 {
            return "+254 \(slice(digits, 3, 6)) \(slice(digits, 6, 9)) \(slice(digits, 9, 12))"
        }
        if !hasPlus && digits.count == 10 && (digits.hasPrefix("07") || digits.hasPrefix("01")) {
            return "\(slice(digits, 0, 4)) \(slice(digits, 4, 7)) \(slice(digits, 7, 10))"
        }
        if hasPlus && digits.hasPrefix("1") && digits.count == 11 {
            return "+1 \(slice(digits, 1, 4)) \(slice(digits, 4, 7)) \(slice(digits, 7, 11))"
        }
        if hasPlus {
            let ccLen = min(max(digits.count - 4, 1), (digits.hasPrefix("1") || digits.hasPrefix("7")) ? 1 : (digits.count >= 11 ? 3 : 2))
            let cc = String(digits.prefix(ccLen))
            let rest = chunked(String(digits.dropFirst(ccLen)), size: 3).joined(separator: " ")
            return "+\(cc) \(rest)"
        }
        return chunked(digits, size: 3).joined(separator: " ")
    }

    private static func slice(_ s: String, _ lo: Int, _ hi: Int) -> String {
        let a = s.index(s.startIndex, offsetBy: lo)
        let b = s.index(s.startIndex, offsetBy: hi)
        return String(s[a..<b])
    }

    private static func chunked(_ s: String, size: Int) -> [String] {
        var out: [String] = []
        var idx = s.startIndex
        while idx < s.endIndex {
            let end = s.index(idx, offsetBy: size, limitedBy: s.endIndex) ?? s.endIndex
            out.append(String(s[idx..<end]))
            idx = end
        }
        return out
    }

    private static func normalizeDialPhone(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasPlus = trimmed.hasPrefix("+")
        let digits = trimmed.filter { $0.isNumber && $0.isASCII }
        if hasPlus && (7...15).contains(digits.count) { return "+\(digits)" }
        if !hasPlus && digits.count == 10 { return digits }
        return nil
    }

    private static func isCharDigit(_ nsText: NSString, at index: Int) -> Bool {
        guard index >= 0, index < nsText.length else { return false }
        let code = nsText.character(at: index)
        return code >= 48 && code <= 57
    }

    private static func charCode(_ nsText: NSString, at index: Int) -> unichar? {
        guard index >= 0, index < nsText.length else { return nil }
        return nsText.character(at: index)
    }

    private static func isPlausibleOtpContext(nsText: NSString, start: Int, end: Int, digits: String) -> Bool {
        guard (4...8).contains(digits.count) else { return false }
        let prevCh = charCode(nsText, at: start - 1)
        let nextCh = charCode(nsText, at: end)
        let dot: unichar = 46 // '.'
        let slash: unichar = 47 // '/'
        let dash: unichar = 45 // '-'
        let prevDecimalOrSlash = start >= 2 && (prevCh == dot || prevCh == slash) && isCharDigit(nsText, at: start - 2)
        let nextDecimalOrSlash = end + 1 < nsText.length && (nextCh == dot || nextCh == slash) && isCharDigit(nsText, at: end + 1)
        if isCharDigit(nsText, at: start - 1) || prevDecimalOrSlash { return false }
        if isCharDigit(nsText, at: end) || nextDecimalOrSlash || nextCh == dash { return false }

        let pStart = max(0, start - 20)
        let prefixWindow = nsText.substring(with: NSRange(location: pStart, length: start - pStart))
        if currencyPrefixRegex.firstMatch(in: prefixWindow, range: NSRange(location: 0, length: (prefixWindow as NSString).length)) != nil {
            return false
        }
        return true
    }

    private static func looksLikePriceOrDateFragment(nsText: NSString, start: Int, end: Int) -> Bool {
        let pStart = max(0, start - 12)
        let before = nsText.substring(with: NSRange(location: pStart, length: start - pStart))
        if currencyPrefixRegex.firstMatch(in: before, range: NSRange(location: 0, length: (before as NSString).length)) != nil {
            return true
        }
        let dot: unichar = 46
        let slash: unichar = 47
        if let prev = charCode(nsText, at: start - 1), prev == dot || prev == slash { return true }
        if let next = charCode(nsText, at: end), next == slash { return true }
        return false
    }

    private static func trimTrailingSentencePunctuation(_ raw: String) -> String {
        let trimSet = CharacterSet(charactersIn: ".,!?:;'\"…)]}")
        var s = raw
        while let last = s.unicodeScalars.last, trimSet.contains(last) {
            s.removeLast()
        }
        return s
    }

    private static func trimUrlTrailingPunctuation(_ raw: String) -> String {
        var s = raw
        let simpleTrim = CharacterSet(charactersIn: ".,!?:;'\"…")
        while let last = s.last {
            if let scalar = last.unicodeScalars.first, s.unicodeScalars.count >= 1 && simpleTrim.contains(scalar) && last.unicodeScalars.count == 1 {
                s.removeLast()
            } else if last == ")" && s.filter({ $0 == ")" }).count > s.filter({ $0 == "(" }).count {
                s.removeLast()
            } else if last == "]" && s.filter({ $0 == "]" }).count > s.filter({ $0 == "[" }).count {
                s.removeLast()
            } else if last == "}" && s.filter({ $0 == "}" }).count > s.filter({ $0 == "{" }).count {
                s.removeLast()
            } else {
                break
            }
        }
        return s
    }

    private static func isPublicIPv4Literal(_ host: String) -> Bool {
        let parts = host.split(separator: ".").compactMap { Int($0) }
        guard parts.count == 4, parts.allSatisfy({ (0...255).contains($0) }) else { return false }
        let a = parts[0], b = parts[1]
        if a == 0 || a == 10 || a == 127 { return false }
        if a == 169 && b == 254 { return false }
        if a == 172 && (16...31).contains(b) { return false }
        if a == 192 && b == 168 { return false }
        if a == 100 && (64...127).contains(b) { return false }
        return a < 224
    }
}

// MARK: - Models & Smart Links Coordinator

struct TelefamSmartLinkPreview: Codable, Equatable, Identifiable {
    var normalizedUrl: String
    var domain: String
    var title: String
    var description: String
    var imageUrl: String
    var faviconUrl: String
    var status: String
    var fetchedAtMs: Int64

    var id: String { normalizedUrl }

    enum CodingKeys: String, CodingKey {
        case normalizedUrl = "normalized_url"
        case domain
        case title
        case description
        case imageUrl = "image_url"
        case faviconUrl = "favicon_url"
        case status
        case fetchedAtMs = "fetched_at_ms"
    }

    init(
        normalizedUrl: String,
        domain: String,
        title: String = "",
        description: String = "",
        imageUrl: String = "",
        faviconUrl: String = "",
        status: String = "ready",
        fetchedAtMs: Int64 = 0
    ) {
        self.normalizedUrl = normalizedUrl
        self.domain = domain
        self.title = title
        self.description = description
        self.imageUrl = imageUrl
        self.faviconUrl = faviconUrl
        self.status = status
        self.fetchedAtMs = fetchedAtMs
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        normalizedUrl = try c.decodeIfPresent(String.self, forKey: .normalizedUrl) ?? ""
        domain = try c.decodeIfPresent(String.self, forKey: .domain) ?? ""
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        imageUrl = try c.decodeIfPresent(String.self, forKey: .imageUrl) ?? ""
        faviconUrl = try c.decodeIfPresent(String.self, forKey: .faviconUrl) ?? ""
        status = try c.decodeIfPresent(String.self, forKey: .status) ?? "ready"
        fetchedAtMs = try c.decodeIfPresent(Int64.self, forKey: .fetchedAtMs) ?? Int64(Date().timeIntervalSince1970 * 1000)
    }
}

struct TelefamSavedLinkItem: Codable, Equatable, Identifiable {
    var id: String
    var url: String
    var normalizedUrl: String
    var domain: String
    var title: String
    var description: String
    var previewImageUrl: String
    var faviconUrl: String
    var source: String
    var savedAtMs: Int64

    enum CodingKeys: String, CodingKey {
        case id
        case url
        case normalizedUrl = "normalized_url"
        case domain
        case title
        case description
        case previewImageUrl = "preview_image_url"
        case faviconUrl = "favicon_url"
        case source
        case savedAtMs = "saved_at_ms"
    }

    init(
        id: String = UUID().uuidString,
        url: String,
        normalizedUrl: String,
        domain: String,
        title: String,
        description: String = "",
        previewImageUrl: String = "",
        faviconUrl: String = "",
        source: String = "browser",
        savedAtMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.url = url
        self.normalizedUrl = normalizedUrl
        self.domain = domain
        self.title = title
        self.description = description
        self.previewImageUrl = previewImageUrl
        self.faviconUrl = faviconUrl
        self.source = source
        self.savedAtMs = savedAtMs
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        url = try c.decodeIfPresent(String.self, forKey: .url) ?? ""
        let rawNorm = try c.decodeIfPresent(String.self, forKey: .normalizedUrl) ?? ""
        normalizedUrl = rawNorm.isEmpty ? (SmartTextEngine.normalizeSafeHttpUrl(url) ?? url) : rawNorm
        let rawDom = try c.decodeIfPresent(String.self, forKey: .domain) ?? ""
        domain = rawDom.isEmpty ? SmartTextEngine.extractCleanDomain(normalizedUrl) : rawDom
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        previewImageUrl = try c.decodeIfPresent(String.self, forKey: .previewImageUrl) ?? ""
        faviconUrl = try c.decodeIfPresent(String.self, forKey: .faviconUrl) ?? ""
        source = try c.decodeIfPresent(String.self, forKey: .source) ?? "browser"
        savedAtMs = try c.decodeIfPresent(Int64.self, forKey: .savedAtMs) ?? Int64(Date().timeIntervalSince1970 * 1000)
    }
}

struct TelefamBrowserSession: Identifiable, Equatable {
    let id = UUID()
    let initialUrl: String
    let titleHint: String
    let source: String
}

struct SmartContactDraft: Identifiable {
    let id = UUID()
    let phone: SmartPhonePayload
}

@MainActor
final class TelefamIosSmartLinksCoordinator: ObservableObject {
    @Published var linkPreviews: [String: TelefamSmartLinkPreview] = [:]
    @Published var savedLinks: [TelefamSavedLinkItem] = []
    @Published var isLoadingSavedLinks: Bool = false
    @Published var activeBrowserSession: TelefamBrowserSession? = nil
    @Published var activePhoneEntity: SmartPhonePayload? = nil
    @Published var activeEmailEntity: SmartEmailPayload? = nil
    @Published var contactDraftToSave: SmartContactDraft? = nil
    @Published var toastMessage: String? = nil
    @Published private(set) var hostStack: [UUID] = []

    private var baseURL: URL?
    private var userIdProvider: () -> String = { "" }
    private var tokenProvider: () -> String = { "" }
    private var deviceFingerprint: String = ""
    private var inFlightUrls: Set<String> = []

    private let cacheDir: URL = {
        let appSup = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let dir = appSup.appendingPathComponent("TelefamSmartLinks", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()

    var savedNormalizedUrls: Set<String> {
        Set(savedLinks.map { $0.normalizedUrl })
    }

    func configure(
        baseURL: URL?,
        userIdProvider: @escaping () -> String,
        tokenProvider: @escaping () -> String,
        deviceFingerprint: String
    ) {
        self.baseURL = baseURL
        self.userIdProvider = userIdProvider
        self.tokenProvider = tokenProvider
        self.deviceFingerprint = deviceFingerprint

        loadCachedSavedLinks()
        Task { await refreshSavedLinks() }
    }

    func clearOnLogout() {
        let uid = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        if !uid.isEmpty {
            try? FileManager.default.removeItem(at: savedLinksFileURL(for: uid))
        }
        linkPreviews.removeAll()
        savedLinks.removeAll()
        inFlightUrls.removeAll()
        activeBrowserSession = nil
        activePhoneEntity = nil
        activeEmailEntity = nil
        contactDraftToSave = nil
        toastMessage = nil
    }

    func pushHost(_ id: UUID) {
        hostStack.removeAll { $0 == id }
        hostStack.append(id)
    }

    func popHost(_ id: UUID) {
        hostStack.removeAll { $0 == id }
    }

    func isTopHost(_ id: UUID) -> Bool {
        hostStack.last == id
    }

    func openUrl(_ rawUrl: String, source: String, titleHint: String = "") {
        guard let safeUrl = SmartTextEngine.normalizeSafeHttpUrl(rawUrl) else {
            showToast("Blocked unsafe or unsupported link")
            return
        }
        requestPreview(for: safeUrl)
        let title = titleHint.isEmpty ? (linkPreviews[safeUrl]?.title ?? "") : titleHint
        activeBrowserSession = TelefamBrowserSession(
            initialUrl: safeUrl,
            titleHint: title,
            source: source.isEmpty ? "browser" : source
        )
    }

    func copyOtp(_ code: String) {
        UIPasteboard.general.string = code
        showToast("Code copied: \(code)")
    }

    func copyText(_ raw: String, confirmation: String) {
        UIPasteboard.general.string = raw
        showToast(confirmation)
    }

    func requestPreview(for rawUrl: String) {
        guard let safeUrl = SmartTextEngine.normalizeSafeHttpUrl(rawUrl) else { return }
        if let existing = linkPreviews[safeUrl], existing.status != "failed" { return }
        guard !inFlightUrls.contains(safeUrl) else { return }
        inFlightUrls.insert(safeUrl)

        let domain = SmartTextEngine.extractCleanDomain(safeUrl)
        linkPreviews[safeUrl] = TelefamSmartLinkPreview(
            normalizedUrl: safeUrl,
            domain: domain,
            title: domain.isEmpty ? safeUrl : domain,
            status: "loading"
        )

        Task {
            defer { inFlightUrls.remove(safeUrl) }
            guard let baseURL,
                  var comps = URLComponents(url: baseURL.appendingPathComponent("v1/links/preview"), resolvingAgainstBaseURL: false) else {
                linkPreviews[safeUrl] = TelefamSmartLinkPreview(normalizedUrl: safeUrl, domain: domain, title: domain, status: "fallback")
                return
            }
            comps.queryItems = [URLQueryItem(name: "url", value: safeUrl)]
            guard let reqUrl = comps.url else { return }

            let req = authorizedRequest(url: reqUrl, method: "GET", contentType: nil)
            struct PreviewWrap: Codable {
                let success: Bool?
                let preview: TelefamSmartLinkPreview?
                let normalizedUrl: String?
                let domain: String?
                let title: String?
                let description: String?
                let imageUrl: String?
                let faviconUrl: String?
                let status: String?

                enum CodingKeys: String, CodingKey {
                    case success, preview, domain, title, description, status
                    case normalizedUrl = "normalized_url"
                    case imageUrl = "image_url"
                    case faviconUrl = "favicon_url"
                }
            }

            if let (data, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse,
               (200...299).contains(http.statusCode),
               let wrap = try? JSONDecoder().decode(PreviewWrap.self, from: data) {
                if var p = wrap.preview {
                    if p.normalizedUrl.isEmpty { p.normalizedUrl = safeUrl }
                    if p.domain.isEmpty { p.domain = domain }
                    linkPreviews[safeUrl] = p
                    return
                }
                let p = TelefamSmartLinkPreview(
                    normalizedUrl: (wrap.normalizedUrl?.isEmpty == false) ? wrap.normalizedUrl! : safeUrl,
                    domain: (wrap.domain?.isEmpty == false) ? wrap.domain! : domain,
                    title: (wrap.title?.isEmpty == false) ? wrap.title! : domain,
                    description: wrap.description ?? "",
                    imageUrl: wrap.imageUrl ?? "",
                    faviconUrl: wrap.faviconUrl ?? "",
                    status: wrap.status ?? "ready",
                    fetchedAtMs: Int64(Date().timeIntervalSince1970 * 1000)
                )
                linkPreviews[safeUrl] = p
            } else {
                linkPreviews[safeUrl] = TelefamSmartLinkPreview(
                    normalizedUrl: safeUrl,
                    domain: domain,
                    title: domain.isEmpty ? safeUrl : domain,
                    status: "fallback",
                    fetchedAtMs: Int64(Date().timeIntervalSince1970 * 1000)
                )
            }
        }
    }

    func refreshSavedLinks(query: String = "") async {
        guard let baseURL else { return }
        isLoadingSavedLinks = true
        defer { isLoadingSavedLinks = false }

        var comps = URLComponents(url: baseURL.appendingPathComponent("v1/links/saved"), resolvingAgainstBaseURL: false)
        var items: [URLQueryItem] = [URLQueryItem(name: "limit", value: "150")]
        let cleanQ = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleanQ.isEmpty {
            items.append(URLQueryItem(name: "q", value: cleanQ))
        }
        comps?.queryItems = items
        guard let reqUrl = comps?.url else { return }
        let req = authorizedRequest(url: reqUrl, method: "GET", contentType: nil)

        struct SavedListResp: Codable {
            let success: Bool
            let items: [TelefamSavedLinkItem]?
        }

        if let (data, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode),
           let parsed = try? JSONDecoder().decode(SavedListResp.self, from: data),
           parsed.success,
           let list = parsed.items {
            if cleanQ.isEmpty {
                savedLinks = list
                persistSavedLinksCache()
            }
        }
    }

    func toggleSaveLink(rawUrl: String, source: String, titleHint: String = "") {
        guard let safeUrl = SmartTextEngine.normalizeSafeHttpUrl(rawUrl) else {
            showToast("Unable to save invalid link")
            return
        }
        if let existing = savedLinks.first(where: { $0.normalizedUrl == safeUrl }) {
            Task { await deleteSavedLink(existing) }
            return
        }
        let preview = linkPreviews[safeUrl]
        let cleanDomain = SmartTextEngine.extractCleanDomain(safeUrl)
        let title = titleHint.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? (preview?.title.isEmpty == false ? preview!.title : cleanDomain)
            : titleHint

        Task {
            guard let baseURL else { return }
            let payload: [String: String] = [
                "url": safeUrl,
                "title": title,
                "description": preview?.description ?? "",
                "preview_image_url": preview?.imageUrl ?? "",
                "favicon_url": preview?.faviconUrl ?? "",
                "source": source.isEmpty ? "browser" : source
            ]
            guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return }
            let req = authorizedRequest(url: baseURL.appendingPathComponent("v1/links/saved"), method: "POST", body: body)

            struct SaveResp: Codable {
                let success: Bool
                let item: TelefamSavedLinkItem?
                let error: String?
            }

            if let (data, resp) = try? await URLSession.shared.data(for: req),
               let http = resp as? HTTPURLResponse,
               (200...299).contains(http.statusCode),
               let parsed = try? JSONDecoder().decode(SaveResp.self, from: data),
               parsed.success,
               let saved = parsed.item {
                savedLinks.removeAll { $0.normalizedUrl == saved.normalizedUrl }
                savedLinks.insert(saved, at: 0)
                persistSavedLinksCache()
                showToast("Saved to Saved Links")
            } else {
                showToast("Unable to save link right now")
            }
        }
    }

    func deleteSavedLink(_ item: TelefamSavedLinkItem) async {
        savedLinks.removeAll { $0.id == item.id && $0.normalizedUrl == item.normalizedUrl }
        persistSavedLinksCache()

        guard let baseURL else { return }
        let key = item.id.isEmpty ? item.normalizedUrl : item.id
        let url = baseURL.appendingPathComponent("v1/links/saved").appendingPathComponent(key)
        let req = authorizedRequest(url: url, method: "DELETE", contentType: nil)
        if let (_, resp) = try? await URLSession.shared.data(for: req),
           let http = resp as? HTTPURLResponse,
           (200...299).contains(http.statusCode) {
            showToast("Removed from Saved Links")
        } else {
            savedLinks.insert(item, at: 0)
            persistSavedLinksCache()
            showToast("Unable to remove saved link")
        }
    }

    func showToast(_ msg: String) {
        toastMessage = msg
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            if self?.toastMessage == msg { self?.toastMessage = nil }
        }
    }

    private func authorizedRequest(url: URL, method: String, contentType: String? = "application/json", body: Data? = nil) -> URLRequest {
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        req.httpMethod = method
        if let contentType {
            req.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        let token = tokenProvider()
        if !token.isEmpty {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if !deviceFingerprint.isEmpty {
            req.setValue(deviceFingerprint, forHTTPHeaderField: "X-Telefam-Device-FP")
        }
        req.httpBody = body
        return req
    }

    private func savedLinksFileURL(for userId: String) -> URL {
        let safe = userId.replacingOccurrences(of: "[^A-Za-z0-9._-]", with: "_", options: .regularExpression)
        return cacheDir.appendingPathComponent("saved_links_\(safe.isEmpty ? "default" : safe).json")
    }

    private func loadCachedSavedLinks() {
        let uid = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !uid.isEmpty,
              let data = try? Data(contentsOf: savedLinksFileURL(for: uid)),
              let decoded = try? JSONDecoder().decode([TelefamSavedLinkItem].self, from: data) else {
            return
        }
        savedLinks = decoded
    }

    private func persistSavedLinksCache() {
        let uid = userIdProvider().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !uid.isEmpty, let data = try? JSONEncoder().encode(savedLinks) else { return }
        try? data.write(to: savedLinksFileURL(for: uid), options: [.atomic, .completeFileProtection])
    }
}

// MARK: - Custom SVG Icons for Smart Text & Browser

private enum SmartSvgKind {
    case copy, check, globe, lockClosed, lockOpen, bookmarkFilled, bookmarkOutline, openExternal, arrowBack, arrowForward, refresh, close, trash, cloudOff
}

private struct SmartSvgIcon: View {
    let kind: SmartSvgKind
    let color: Color
    let size: CGFloat

    var body: some View {
        Canvas { ctx, sz in
            let w = sz.width
            let h = sz.height
            let stroke = StrokeStyle(lineWidth: max(1.5, w * 0.095), lineCap: .round, lineJoin: .round)

            switch kind {
            case .copy:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.32, y: h * 0.14, width: w * 0.52, height: h * 0.56), cornerRadius: w * 0.08), with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.16, y: h * 0.30, width: w * 0.52, height: h * 0.56), cornerRadius: w * 0.08), with: .color(color), style: stroke)
            case .check:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.18, y: h * 0.52))
                p.addLine(to: CGPoint(x: w * 0.42, y: h * 0.74))
                p.addLine(to: CGPoint(x: w * 0.84, y: h * 0.28))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .globe:
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.12, y: h * 0.12, width: w * 0.76, height: h * 0.76)), with: .color(color), style: stroke)
                ctx.stroke(Path(ellipseIn: CGRect(x: w * 0.33, y: h * 0.12, width: w * 0.34, height: h * 0.76)), with: .color(color), style: stroke)
                var eq = Path()
                eq.move(to: CGPoint(x: w * 0.14, y: h * 0.50))
                eq.addLine(to: CGPoint(x: w * 0.86, y: h * 0.50))
                ctx.stroke(eq, with: .color(color), style: stroke)
            case .lockClosed:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.22, y: h * 0.44, width: w * 0.56, height: h * 0.42), cornerRadius: w * 0.10), with: .color(color), style: stroke)
                var sh = Path()
                sh.move(to: CGPoint(x: w * 0.34, y: h * 0.44))
                sh.addLine(to: CGPoint(x: w * 0.34, y: h * 0.30))
                sh.addCurve(to: CGPoint(x: w * 0.66, y: h * 0.30), control1: CGPoint(x: w * 0.34, y: h * 0.14), control2: CGPoint(x: w * 0.66, y: h * 0.14))
                sh.addLine(to: CGPoint(x: w * 0.66, y: h * 0.44))
                ctx.stroke(sh, with: .color(color), style: stroke)
            case .lockOpen:
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.22, y: h * 0.44, width: w * 0.56, height: h * 0.42), cornerRadius: w * 0.10), with: .color(color), style: stroke)
                var sh = Path()
                sh.move(to: CGPoint(x: w * 0.34, y: h * 0.44))
                sh.addLine(to: CGPoint(x: w * 0.34, y: h * 0.30))
                sh.addCurve(to: CGPoint(x: w * 0.66, y: h * 0.28), control1: CGPoint(x: w * 0.34, y: h * 0.14), control2: CGPoint(x: w * 0.66, y: h * 0.14))
                ctx.stroke(sh, with: .color(color), style: stroke)
            case .bookmarkFilled, .bookmarkOutline:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.24, y: h * 0.14))
                p.addLine(to: CGPoint(x: w * 0.76, y: h * 0.14))
                p.addLine(to: CGPoint(x: w * 0.76, y: h * 0.86))
                p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.66))
                p.addLine(to: CGPoint(x: w * 0.24, y: h * 0.86))
                p.closeSubpath()
                if case .bookmarkFilled = kind {
                    ctx.fill(p, with: .color(color))
                } else {
                    ctx.stroke(p, with: .color(color), style: stroke)
                }
            case .openExternal:
                var box = Path()
                box.move(to: CGPoint(x: w * 0.50, y: h * 0.20))
                box.addLine(to: CGPoint(x: w * 0.20, y: h * 0.20))
                box.addLine(to: CGPoint(x: w * 0.20, y: h * 0.80))
                box.addLine(to: CGPoint(x: w * 0.80, y: h * 0.80))
                box.addLine(to: CGPoint(x: w * 0.80, y: h * 0.50))
                ctx.stroke(box, with: .color(color), style: stroke)
                var arr = Path()
                arr.move(to: CGPoint(x: w * 0.48, y: h * 0.52))
                arr.addLine(to: CGPoint(x: w * 0.82, y: h * 0.18))
                arr.move(to: CGPoint(x: w * 0.60, y: h * 0.18))
                arr.addLine(to: CGPoint(x: w * 0.82, y: h * 0.18))
                arr.addLine(to: CGPoint(x: w * 0.82, y: h * 0.40))
                ctx.stroke(arr, with: .color(color), style: stroke)
            case .arrowBack:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.80, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.50))
                p.move(to: CGPoint(x: w * 0.46, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.20, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.46, y: h * 0.76))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .arrowForward:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.20, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.50))
                p.move(to: CGPoint(x: w * 0.54, y: h * 0.24))
                p.addLine(to: CGPoint(x: w * 0.80, y: h * 0.50))
                p.addLine(to: CGPoint(x: w * 0.54, y: h * 0.76))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .refresh:
                var arc = Path()
                arc.addArc(center: CGPoint(x: w * 0.50, y: h * 0.50), radius: w * 0.32, startAngle: .degrees(-45), endAngle: .degrees(255), clockwise: false)
                ctx.stroke(arc, with: .color(color), style: stroke)
                var tip = Path()
                tip.move(to: CGPoint(x: w * 0.66, y: h * 0.18))
                tip.addLine(to: CGPoint(x: w * 0.82, y: h * 0.24))
                tip.addLine(to: CGPoint(x: w * 0.72, y: h * 0.38))
                ctx.stroke(tip, with: .color(color), style: stroke)
            case .close:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.22, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.78, y: h * 0.78))
                p.move(to: CGPoint(x: w * 0.78, y: h * 0.22))
                p.addLine(to: CGPoint(x: w * 0.22, y: h * 0.78))
                ctx.stroke(p, with: .color(color), style: stroke)
            case .trash:
                var p = Path()
                p.move(to: CGPoint(x: w * 0.18, y: h * 0.26))
                p.addLine(to: CGPoint(x: w * 0.82, y: h * 0.26))
                p.move(to: CGPoint(x: w * 0.38, y: h * 0.16))
                p.addLine(to: CGPoint(x: w * 0.62, y: h * 0.16))
                p.move(to: CGPoint(x: w * 0.42, y: h * 0.42))
                p.addLine(to: CGPoint(x: w * 0.42, y: h * 0.70))
                p.move(to: CGPoint(x: w * 0.58, y: h * 0.42))
                p.addLine(to: CGPoint(x: w * 0.58, y: h * 0.70))
                ctx.stroke(p, with: .color(color), style: stroke)
                ctx.stroke(Path(roundedRect: CGRect(x: w * 0.26, y: h * 0.26, width: w * 0.48, height: h * 0.58), cornerRadius: w * 0.08), with: .color(color), style: stroke)
            case .cloudOff:
                var cloud = Path()
                cloud.move(to: CGPoint(x: w * 0.28, y: h * 0.70))
                cloud.addLine(to: CGPoint(x: w * 0.74, y: h * 0.70))
                cloud.addCurve(to: CGPoint(x: w * 0.73, y: h * 0.45), control1: CGPoint(x: w * 0.88, y: h * 0.70), control2: CGPoint(x: w * 0.88, y: h * 0.46))
                cloud.addCurve(to: CGPoint(x: w * 0.37, y: h * 0.42), control1: CGPoint(x: w * 0.71, y: h * 0.26), control2: CGPoint(x: w * 0.43, y: h * 0.24))
                cloud.addCurve(to: CGPoint(x: w * 0.28, y: h * 0.70), control1: CGPoint(x: w * 0.18, y: h * 0.42), control2: CGPoint(x: w * 0.14, y: h * 0.70))
                cloud.closeSubpath()
                ctx.stroke(cloud, with: .color(color), style: stroke)
                var slash = Path()
                slash.move(to: CGPoint(x: w * 0.16, y: h * 0.16))
                slash.addLine(to: CGPoint(x: w * 0.84, y: h * 0.84))
                ctx.stroke(slash, with: .color(color), style: stroke)
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Smart Interactive Text Component

struct SmartInteractiveTextView: View {
    @EnvironmentObject private var coordinator: TelefamIosSmartLinksCoordinator

    let text: String
    let source: String
    var font: Font = .body
    var textColor: Color = .primary
    var linkColor: Color = TelefamPalette.crimson
    var otpHighlightColor: Color? = nil
    var lineLimit: Int? = nil
    var textAlignment: TextAlignment = .leading
    var showOtpPills: Bool = true

    @State private var copiedOtpKey: String? = nil

    var body: some View {
        let analysis = SmartTextEngine.analyze(text)
        let attributed = buildAttributedString(from: analysis)

        VStack(alignment: textAlignment == .center ? .center : .leading, spacing: 6) {
            Text(attributed)
                .font(font)
                .foregroundColor(textColor)
                .multilineTextAlignment(textAlignment)
                .lineLimit(lineLimit)
                .environment(\.openURL, OpenURLAction { internalUrl in
                    handleSmartTap(internalUrl, analysis: analysis)
                    return .handled
                })

            if showOtpPills && !analysis.otpCodes.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(analysis.otpCodes.enumerated()), id: \.offset) { idx, otp in
                            let key = "\(idx)_\(otp.code)"
                            let isCopied = copiedOtpKey == key
                            let green = Color(red: 0.086, green: 0.639, blue: 0.29)
                            Button {
                                coordinator.copyOtp(otp.code)
                                copiedOtpKey = key
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                                    if copiedOtpKey == key { copiedOtpKey = nil }
                                }
                            } label: {
                                HStack(spacing: 6) {
                                    SmartSvgIcon(
                                        kind: isCopied ? .check : .copy,
                                        color: isCopied ? green : linkColor,
                                        size: 14
                                    )
                                    Text(isCopied ? "Copied" : otp.code)
                                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                                        .foregroundColor(isCopied ? green : textColor)
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 7)
                                .background(isCopied ? green.opacity(0.14) : linkColor.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(isCopied ? green.opacity(0.45) : linkColor.opacity(0.35), lineWidth: 1)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private func buildAttributedString(from analysis: SmartTextAnalysisResult) -> AttributedString {
        let nsText = analysis.text as NSString
        var out = AttributedString()
        var cursor = 0
        let bg = otpHighlightColor ?? linkColor.opacity(0.14)

        for entity in analysis.entities {
            if entity.startOffset > cursor {
                let plain = nsText.substring(with: NSRange(location: cursor, length: entity.startOffset - cursor))
                out.append(AttributedString(plain))
            }

            let displaySegment: String
            switch entity.kind {
            case .phone(let p):
                displaySegment = p.formattedDisplay
            default:
                displaySegment = nsText.substring(with: NSRange(location: entity.startOffset, length: entity.endOffset - entity.startOffset))
            }

            var attrSeg = AttributedString(displaySegment)
            let token = entity.id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? entity.id
            if let tapUrl = URL(string: "telefam-smart://tap?id=\(token)") {
                attrSeg.link = tapUrl
            }

            switch entity.kind {
            case .url, .email, .phone:
                attrSeg.foregroundColor = linkColor
                attrSeg.underlineStyle = .single
            case .otp:
                attrSeg.foregroundColor = linkColor
                attrSeg.backgroundColor = bg
                attrSeg.font = .system(.body, design: .monospaced).bold()
            }

            out.append(attrSeg)
            cursor = entity.endOffset
        }

        if cursor < nsText.length {
            out.append(AttributedString(nsText.substring(from: cursor)))
        }
        return out
    }

    private func handleSmartTap(_ url: URL, analysis: SmartTextAnalysisResult) {
        guard url.scheme == "telefam-smart",
              let comps = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let idParam = comps.queryItems?.first(where: { $0.name == "id" })?.value,
              let matched = analysis.entities.first(where: { $0.id == idParam }) else {
            return
        }
        switch matched.kind {
        case .url(let u):
            coordinator.openUrl(u.normalizedUrl, source: source)
        case .email(let e):
            coordinator.activeEmailEntity = e
        case .phone(let p):
            coordinator.activePhoneEntity = p
        case .otp(let o):
            coordinator.copyOtp(o.code)
        }
    }
}

// MARK: - Link Preview Card Stack

struct SmartLinkPreviewCardStack: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var coordinator: TelefamIosSmartLinksCoordinator

    let text: String
    let source: String
    var isDarkBubble: Bool = false

    var body: some View {
        let urls = deduplicatedUrls()
        if !urls.isEmpty {
            VStack(spacing: 6) {
                ForEach(urls, id: \.normalizedUrl) { entity in
                    let preview = coordinator.linkPreviews[entity.normalizedUrl]
                    let isSaved = coordinator.savedNormalizedUrls.contains(entity.normalizedUrl)
                    SmartLinkPreviewCardView(
                        urlEntity: entity,
                        preview: preview,
                        isSaved: isSaved,
                        isDarkBubble: isDarkBubble,
                        onOpen: {
                            coordinator.openUrl(entity.normalizedUrl, source: source, titleHint: preview?.title ?? "")
                        },
                        onToggleSave: {
                            coordinator.toggleSaveLink(rawUrl: entity.normalizedUrl, source: source, titleHint: preview?.title ?? "")
                        }
                    )
                    .task(id: entity.normalizedUrl) {
                        coordinator.requestPreview(for: entity.normalizedUrl)
                    }
                }
            }
        }
    }

    private func deduplicatedUrls() -> [SmartUrlPayload] {
        var seen: Set<String> = []
        var out: [SmartUrlPayload] = []
        for u in SmartTextEngine.analyze(text).urls {
            if seen.insert(u.normalizedUrl).inserted {
                out.append(u)
                if out.count >= 3 { break }
            }
        }
        return out
    }
}

private struct SmartLinkPreviewCardView: View {
    @Environment(\.colorScheme) private var colorScheme

    let urlEntity: SmartUrlPayload
    let preview: TelefamSmartLinkPreview?
    let isSaved: Bool
    let isDarkBubble: Bool
    let onOpen: () -> Void
    let onToggleSave: () -> Void

    var body: some View {
        let cardBg = isDarkBubble ? Color.white.opacity(0.14) : TelefamPalette.surface(colorScheme)
        let border = isDarkBubble ? Color.white.opacity(0.24) : TelefamPalette.border(colorScheme)
        let primaryInk = isDarkBubble ? Color.white : TelefamPalette.textPrimary(colorScheme)
        let mutedInk = isDarkBubble ? Color.white.opacity(0.78) : TelefamPalette.textMuted(colorScheme)
        let accent = isDarkBubble ? Color.white : TelefamPalette.crimson

        Button(action: onOpen) {
            Group {
                if preview == nil || preview?.status == "loading" {
                    VStack(alignment: .leading, spacing: 7) {
                        HStack(spacing: 6) {
                            Circle()
                                .fill(mutedInk.opacity(0.35))
                                .frame(width: 14, height: 14)
                            Text(urlEntity.domain)
                                .font(.system(size: 11))
                                .foregroundColor(mutedInk)
                                .lineLimit(1)
                        }
                        RoundedRectangle(cornerRadius: 4)
                            .fill(mutedInk.opacity(0.32))
                            .frame(height: 12)
                            .frame(maxWidth: 190)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(mutedInk.opacity(0.22))
                            .frame(height: 10)
                            .frame(maxWidth: 130)
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                } else if let preview, preview.status == "failed" || (preview.status == "fallback" && preview.title.isEmpty) {
                    HStack(spacing: 10) {
                        SmartSvgIcon(kind: .globe, color: accent, size: 18)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(urlEntity.domain.isEmpty ? urlEntity.normalizedUrl : urlEntity.domain)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(primaryInk)
                                .lineLimit(1)
                            Text(urlEntity.normalizedUrl)
                                .font(.system(size: 11))
                                .foregroundColor(mutedInk)
                                .lineLimit(1)
                        }
                        Spacer()
                        Button(action: onToggleSave) {
                            SmartSvgIcon(kind: isSaved ? .bookmarkFilled : .bookmarkOutline, color: accent, size: 16)
                                .frame(width: 32, height: 32)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                } else if let preview {
                    VStack(alignment: .leading, spacing: 0) {
                        if let imgUrl = SmartTextEngine.normalizeSafeHttpUrl(preview.imageUrl).flatMap({ URL(string: $0) }) {
                            AsyncImage(url: imgUrl) { phase in
                                if case .success(let image) = phase {
                                    image
                                        .resizable()
                                        .scaledToFill()
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 130)
                                        .clipped()
                                }
                            }
                        }

                        HStack(alignment: .top, spacing: 8) {
                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 5) {
                                    if let favUrl = SmartTextEngine.normalizeSafeHttpUrl(preview.faviconUrl).flatMap({ URL(string: $0) }) {
                                        AsyncImage(url: favUrl) { phase in
                                            if case .success(let img) = phase {
                                                img.resizable().scaledToFill().frame(width: 14, height: 14).clipShape(Circle())
                                            } else {
                                                SmartSvgIcon(kind: .globe, color: mutedInk, size: 12)
                                            }
                                        }
                                    } else {
                                        SmartSvgIcon(kind: .globe, color: mutedInk, size: 12)
                                    }
                                    Text(preview.domain.isEmpty ? urlEntity.domain : preview.domain)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(mutedInk)
                                        .lineLimit(1)
                                }

                                Text(preview.title.isEmpty ? preview.domain : preview.title)
                                    .font(.system(size: 13.5, weight: .bold))
                                    .foregroundColor(primaryInk)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)

                                if !preview.description.isEmpty {
                                    Text(preview.description)
                                        .font(.system(size: 12))
                                        .foregroundColor(mutedInk)
                                        .lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                }
                            }

                            Spacer(minLength: 4)

                            Button(action: onToggleSave) {
                                SmartSvgIcon(
                                    kind: isSaved ? .bookmarkFilled : .bookmarkOutline,
                                    color: isSaved ? accent : mutedInk,
                                    size: 16
                                )
                                .frame(width: 32, height: 32)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                    }
                }
            }
            .background(cardBg)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - In-App Browser (WKWebView + Pull to Refresh + Kinetic Animation)

struct TelefamIosInAppBrowserView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var coordinator: TelefamIosSmartLinksCoordinator

    let session: TelefamBrowserSession
    let onClose: () -> Void

    @State private var currentUrl: String = ""
    @State private var pageTitle: String = ""
    @State private var progress: Double = 0.0
    @State private var isLoading: Bool = true
    @State private var canGoBack: Bool = false
    @State private var canGoForward: Bool = false
    @State private var hasError: Bool = false
    @State private var webCommand: BrowserWebCommand? = nil

    private enum BrowserWebCommand: Equatable {
        case back(Int)
        case forward(Int)
        case reload(Int)
        case stop(Int)
    }

    @State private var commandTick: Int = 0

    var body: some View {
        let safeInitial = SmartTextEngine.normalizeSafeHttpUrl(session.initialUrl) ?? "about:blank"
        let activeUrl = currentUrl.isEmpty ? safeInitial : currentUrl
        let isHttps = activeUrl.lowercased().hasPrefix("https://")
        let domain = SmartTextEngine.extractCleanDomain(activeUrl)
        let isSaved = SmartTextEngine.normalizeSafeHttpUrl(activeUrl).map { coordinator.savedNormalizedUrls.contains($0) } ?? false

        VStack(spacing: 0) {
            // Top Toolbar
            HStack(spacing: 10) {
                Button(action: onClose) {
                    SmartSvgIcon(kind: .close, color: TelefamPalette.textPrimary(colorScheme), size: 17)
                        .frame(width: 40, height: 40)
                        .background(TelefamPalette.surface(colorScheme))
                        .clipShape(Circle())
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(pageTitle.isEmpty ? (session.titleHint.isEmpty ? domain : session.titleHint) : pageTitle)
                        .font(.system(size: 14.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .lineLimit(1)

                    HStack(spacing: 4) {
                        SmartSvgIcon(
                            kind: isHttps ? .lockClosed : .lockOpen,
                            color: isHttps ? Color(red: 0.086, green: 0.639, blue: 0.29) : Color.orange,
                            size: 11.5
                        )
                        Text(domain.isEmpty ? activeUrl : domain)
                            .font(.system(size: 11.5))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button {
                    coordinator.toggleSaveLink(rawUrl: activeUrl, source: session.source, titleHint: pageTitle)
                } label: {
                    SmartSvgIcon(
                        kind: isSaved ? .bookmarkFilled : .bookmarkOutline,
                        color: isSaved ? TelefamPalette.crimson : TelefamPalette.textPrimary(colorScheme),
                        size: 17
                    )
                    .frame(width: 40, height: 40)
                }

                Button {
                    if let safe = SmartTextEngine.normalizeSafeHttpUrl(activeUrl), let u = URL(string: safe) {
                        UIApplication.shared.open(u)
                    }
                } label: {
                    SmartSvgIcon(kind: .openExternal, color: TelefamPalette.textPrimary(colorScheme), size: 17)
                        .frame(width: 40, height: 40)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(TelefamPalette.surface(colorScheme))

            if isLoading {
                ProgressView(value: min(max(progress, 0.08), 1.0))
                    .tint(TelefamPalette.crimson)
                    .progressViewStyle(.linear)
            } else {
                Divider()
            }

            // WebView Content + Error + Kinetic Loader
            ZStack {
                if hasError {
                    VStack(spacing: 14) {
                        SmartSvgIcon(kind: .cloudOff, color: TelefamPalette.crimson, size: 44)
                        Text("Unable to load page")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Text(domain.isEmpty ? activeUrl : domain)
                            .font(.system(size: 13))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                            .lineLimit(2)
                        HStack(spacing: 10) {
                            Button {
                                hasError = false
                                isLoading = true
                                commandTick += 1
                                webCommand = .reload(commandTick)
                            } label: {
                                Text("Retry")
                                    .font(.system(size: 14.5, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 10)
                                    .background(TelefamPalette.crimson)
                                    .clipShape(Capsule())
                            }

                            Button {
                                if let safe = SmartTextEngine.normalizeSafeHttpUrl(activeUrl), let u = URL(string: safe) {
                                    UIApplication.shared.open(u)
                                }
                            } label: {
                                Text("Open externally")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(TelefamPalette.surface(colorScheme))
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(TelefamPalette.border(colorScheme)))
                            }
                        }
                    }
                    .padding(28)
                } else {
                    TelefamWKWebViewRepresentable(
                        initialUrlString: safeInitial,
                        command: webCommand,
                        onTitleChange: { t in if !t.isEmpty { pageTitle = t } },
                        onUrlChange: { u in currentUrl = u },
                        onProgressChange: { p in progress = p },
                        onLoadingChange: { l in isLoading = l },
                        onNavStateChange: { b, f in
                            canGoBack = b
                            canGoForward = f
                        },
                        onErrorChange: { err in hasError = err }
                    )
                }

                if isLoading && !hasError {
                    TelefamIosBrowserReconnectAnimation()
                        .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            Divider()

            // Bottom Navigation Toolbar
            HStack {
                Spacer()
                Button {
                    commandTick += 1
                    webCommand = .back(commandTick)
                } label: {
                    SmartSvgIcon(
                        kind: .arrowBack,
                        color: canGoBack ? TelefamPalette.textPrimary(colorScheme) : TelefamPalette.textMuted(colorScheme).opacity(0.4),
                        size: 19
                    )
                    .frame(width: 48, height: 44)
                }
                .disabled(!canGoBack)

                Spacer()
                Button {
                    commandTick += 1
                    webCommand = .forward(commandTick)
                } label: {
                    SmartSvgIcon(
                        kind: .arrowForward,
                        color: canGoForward ? TelefamPalette.textPrimary(colorScheme) : TelefamPalette.textMuted(colorScheme).opacity(0.4),
                        size: 19
                    )
                    .frame(width: 48, height: 44)
                }
                .disabled(!canGoForward)

                Spacer()
                Button {
                    commandTick += 1
                    if isLoading {
                        webCommand = .stop(commandTick)
                        isLoading = false
                    } else {
                        hasError = false
                        isLoading = true
                        webCommand = .reload(commandTick)
                    }
                } label: {
                    SmartSvgIcon(
                        kind: isLoading ? .close : .refresh,
                        color: TelefamPalette.textPrimary(colorScheme),
                        size: 18
                    )
                    .frame(width: 48, height: 44)
                }
                Spacer()
            }
            .background(TelefamPalette.surface(colorScheme))
        }
        .background(TelefamPalette.background(colorScheme).ignoresSafeArea())
        .onAppear {
            currentUrl = safeInitial
            pageTitle = session.titleHint.isEmpty ? SmartTextEngine.extractCleanDomain(safeInitial) : session.titleHint
        }
    }

    private struct TelefamWKWebViewRepresentable: UIViewRepresentable {
        let initialUrlString: String
        let command: BrowserWebCommand?
        let onTitleChange: (String) -> Void
        let onUrlChange: (String) -> Void
        let onProgressChange: (Double) -> Void
        let onLoadingChange: (Bool) -> Void
        let onNavStateChange: (Bool, Bool) -> Void
        let onErrorChange: (Bool) -> Void

        func makeCoordinator() -> Coordinator {
            Coordinator(parent: self)
        }

        func makeUIView(context: Context) -> WKWebView {
            let cfg = WKWebViewConfiguration()
            cfg.defaultWebpagePreferences.allowsContentJavaScript = true
            let webView = WKWebView(frame: .zero, configuration: cfg)
            webView.navigationDelegate = context.coordinator
            webView.allowsBackForwardNavigationGestures = true

            let refreshControl = UIRefreshControl()
            refreshControl.addTarget(context.coordinator, action: #selector(Coordinator.handlePullToRefresh(_:)), for: .valueChanged)
            webView.scrollView.addSubview(refreshControl)
            context.coordinator.refreshControl = refreshControl
            context.coordinator.observe(webView: webView)

            if let safe = SmartTextEngine.normalizeSafeHttpUrl(initialUrlString), let url = URL(string: safe) {
                webView.load(URLRequest(url: url))
            } else {
                onErrorChange(true)
                onLoadingChange(false)
            }
            return webView
        }

        func updateUIView(_ webView: WKWebView, context: Context) {
            if let cmd = command, cmd != context.coordinator.lastCommand {
                context.coordinator.lastCommand = cmd
                switch cmd {
                case .back:
                    if webView.canGoBack { webView.goBack() }
                case .forward:
                    if webView.canGoForward { webView.goForward() }
                case .reload:
                    webView.reload()
                case .stop:
                    webView.stopLoading()
                }
            }
        }

        final class Coordinator: NSObject, WKNavigationDelegate {
            let parent: TelefamWKWebViewRepresentable
            weak var webView: WKWebView?
            weak var refreshControl: UIRefreshControl?
            var lastCommand: BrowserWebCommand?
            private var observations: [NSKeyValueObservation] = []

            init(parent: TelefamWKWebViewRepresentable) {
                self.parent = parent
            }

            func observe(webView: WKWebView) {
                self.webView = webView
                observations = [
                    webView.observe(\.estimatedProgress, options: [.new]) { [weak self] wv, _ in
                        Task { @MainActor in self?.parent.onProgressChange(wv.estimatedProgress) }
                    },
                    webView.observe(\.title, options: [.new]) { [weak self] wv, _ in
                        Task { @MainActor in self?.parent.onTitleChange(wv.title ?? "") }
                    },
                    webView.observe(\.url, options: [.new]) { [weak self] wv, _ in
                        Task { @MainActor in
                            if let u = wv.url?.absoluteString { self?.parent.onUrlChange(u) }
                            self?.parent.onNavStateChange(wv.canGoBack, wv.canGoForward)
                        }
                    }
                ]
            }

            @objc func handlePullToRefresh(_ sender: UIRefreshControl) {
                webView?.reload()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    sender.endRefreshing()
                }
            }

            func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
                guard let urlStr = navigationAction.request.url?.absoluteString,
                      SmartTextEngine.normalizeSafeHttpUrl(urlStr) != nil else {
                    decisionHandler(.cancel)
                    return
                }
                decisionHandler(.allow)
            }

            func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
                parent.onLoadingChange(true)
                parent.onErrorChange(false)
            }

            func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
                parent.onLoadingChange(false)
                parent.onNavStateChange(webView.canGoBack, webView.canGoForward)
                refreshControl?.endRefreshing()
            }

            func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
                if (error as NSError).code != NSURLErrorCancelled {
                    parent.onLoadingChange(false)
                    parent.onErrorChange(true)
                }
                refreshControl?.endRefreshing()
            }

            func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
                if (error as NSError).code != NSURLErrorCancelled {
                    parent.onLoadingChange(false)
                    parent.onErrorChange(true)
                }
                refreshControl?.endRefreshing()
            }
        }
    }
}

private struct TelefamIosBrowserReconnectAnimation: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var phase: CGFloat = 0

    private let telefamLetters = ["T", "e", "l", "e", "f", "a", "m"]
    private let browserLetters = ["B", "r", "o", "w", "s", "e", "r"]

    var body: some View {
        HStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(TelefamPalette.crimson.opacity(0.14))
                    .frame(width: 28, height: 28)
                SmartSvgIcon(kind: .globe, color: TelefamPalette.crimson, size: 15)
            }

            Spacer().frame(width: 6 + phase * 10)

            HStack(spacing: 0) {
                ForEach(Array(telefamLetters.enumerated()), id: \.offset) { idx, ch in
                    let dx = CGFloat(idx - 3) * phase * 2.2
                    let dy = CGFloat(sin(Double(idx) * 1.1 + Double(phase) * 3.14)) * phase * 4.0
                    Text(ch)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(TelefamPalette.crimson)
                        .offset(x: dx, y: dy)
                        .opacity(1.0 - Double(phase) * 0.25)
                }
            }

            Spacer().frame(width: 6 + phase * 14)

            HStack(spacing: 0) {
                ForEach(Array(browserLetters.enumerated()), id: \.offset) { idx, ch in
                    let dx = CGFloat(idx - 3) * phase * 2.2
                    let dy = -CGFloat(sin(Double(idx) * 1.1 + Double(phase) * 3.14)) * phase * 4.0
                    Text(ch)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        .offset(x: dx, y: dy)
                        .opacity(1.0 - Double(phase) * 0.25)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(TelefamPalette.surface(colorScheme).opacity(0.95))
        .clipShape(Capsule())
        .overlay(Capsule().stroke(TelefamPalette.crimson.opacity(0.35), lineWidth: 1))
        .shadow(color: .black.opacity(0.18), radius: 8, y: 4)
        .onAppear {
            withAnimation(.easeInOut(duration: 1.45).repeatForever(autoreverses: true)) {
                phase = 1.0
            }
        }
    }
}

// MARK: - Native Save Contact Sheet Bridge

struct TelefamIosSaveContactSheet: UIViewControllerRepresentable {
    let phoneNumber: String
    @Environment(\.dismiss) private var dismiss

    func makeCoordinator() -> Coordinator {
        Coordinator(dismiss: dismiss)
    }

    func makeUIViewController(context: Context) -> UINavigationController {
        let contact = CNMutableContact()
        contact.phoneNumbers = [CNLabeledValue(label: CNLabelPhoneNumberMobile, value: CNPhoneNumber(stringValue: phoneNumber))]
        let vc = CNContactViewController(forNewContact: contact)
        vc.delegate = context.coordinator
        return UINavigationController(rootViewController: vc)
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {}

    final class Coordinator: NSObject, CNContactViewControllerDelegate {
        let dismiss: DismissAction
        init(dismiss: DismissAction) { self.dismiss = dismiss }
        func contactViewController(_ viewController: CNContactViewController, didCompleteWith contact: CNContact?) {
            dismiss()
        }
    }
}

// MARK: - Saved Links Screen

struct TelefamIosSavedLinksView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var coordinator: TelefamIosSmartLinksCoordinator
    @State private var searchQuery: String = ""

    private var filtered: [TelefamSavedLinkItem] {
        let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if q.isEmpty { return coordinator.savedLinks }
        return coordinator.savedLinks.filter {
            $0.title.lowercased().contains(q) ||
            $0.domain.lowercased().contains(q) ||
            $0.url.lowercased().contains(q) ||
            $0.source.lowercased().contains(q)
        }
    }

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                SmartSvgIcon(kind: .globe, color: TelefamPalette.textMuted(colorScheme), size: 16)
                TextField("Search saved links", text: $searchQuery)
                    .font(.system(size: 14.5))
                    .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                if !searchQuery.isEmpty {
                    Button { searchQuery = "" } label: {
                        SmartSvgIcon(kind: .close, color: TelefamPalette.textMuted(colorScheme), size: 14)
                    }
                }
            }
            .padding(.horizontal, 14)
            .frame(height: 44)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
            .padding(.horizontal, 16)
            .padding(.top, 10)

            if filtered.isEmpty {
                VStack(spacing: 10) {
                    SmartSvgIcon(kind: .bookmarkOutline, color: TelefamPalette.textMuted(colorScheme), size: 34)
                    Text(searchQuery.isEmpty ? "No saved links yet" : "No matching links")
                        .font(.system(size: 15.5, weight: .bold))
                        .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                    Text("Bookmark links from chats, feeds, or the Telefam Browser.")
                        .font(.system(size: 13))
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 48)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(filtered) { item in
                            Button {
                                coordinator.openUrl(
                                    item.normalizedUrl.isEmpty ? item.url : item.normalizedUrl,
                                    source: item.source,
                                    titleHint: item.title
                                )
                            } label: {
                                HStack(spacing: 12) {
                                    if let imgUrl = SmartTextEngine.normalizeSafeHttpUrl(item.previewImageUrl).flatMap({ URL(string: $0) }) {
                                        AsyncImage(url: imgUrl) { phase in
                                            if case .success(let img) = phase {
                                                img.resizable().scaledToFill()
                                            } else {
                                                fallbackIcon
                                            }
                                        }
                                        .frame(width: 56, height: 56)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                    } else {
                                        fallbackIcon
                                    }

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(item.title.isEmpty ? item.domain : item.title)
                                            .font(.system(size: 14.5, weight: .bold))
                                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                                            .lineLimit(2)
                                            .multilineTextAlignment(.leading)
                                        Text(item.domain.isEmpty ? item.normalizedUrl : item.domain)
                                            .font(.system(size: 12))
                                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                            .lineLimit(1)
                                        HStack(spacing: 8) {
                                            Text(sourceLabel(item.source))
                                                .font(.system(size: 10.5, weight: .semibold))
                                                .foregroundColor(TelefamPalette.crimson)
                                                .padding(.horizontal, 6)
                                                .padding(.vertical, 2)
                                                .background(TelefamPalette.crimson.opacity(0.10))
                                                .clipShape(RoundedRectangle(cornerRadius: 6))
                                            if item.savedAtMs > 0 {
                                                Text(formatSavedDate(item.savedAtMs))
                                                    .font(.system(size: 11))
                                                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                                            }
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                    Button {
                                        Task { await coordinator.deleteSavedLink(item) }
                                    } label: {
                                        SmartSvgIcon(kind: .trash, color: TelefamPalette.textMuted(colorScheme), size: 17)
                                            .frame(width: 40, height: 40)
                                    }
                                    .buttonStyle(.plain)
                                }
                                .padding(12)
                                .background(TelefamPalette.surface(colorScheme))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(TelefamPalette.border(colorScheme), lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
            }
        }
        .task {
            await coordinator.refreshSavedLinks()
        }
    }

    private var fallbackIcon: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(TelefamPalette.crimson.opacity(0.12))
            SmartSvgIcon(kind: .globe, color: TelefamPalette.crimson, size: 22)
        }
        .frame(width: 56, height: 56)
    }

    private func sourceLabel(_ raw: String) -> String {
        switch raw.lowercased() {
        case "chat": return "Chat"
        case "feed": return "Feed"
        case "profile": return "Profile"
        default: return "Browser"
        }
    }

    private func formatSavedDate(_ ms: Int64) -> String {
        let d = Date(timeIntervalSince1970: Double(ms) / 1000.0)
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .none
        return f.string(from: d)
    }
}

// MARK: - Top-Most Host Overlay Presenter

private struct TelefamSmartOverlayHostModifier: ViewModifier {
    @EnvironmentObject private var coordinator: TelefamIosSmartLinksCoordinator
    let preferredColorScheme: ColorScheme?
    let showLocalToast: Bool

    @State private var hostId = UUID()

    private var isTop: Bool {
        coordinator.isTopHost(hostId)
    }

    func body(content: Content) -> some View {
        ZStack(alignment: .bottom) {
            content

            if showLocalToast, isTop, let toast = coordinator.toastMessage {
                Text(toast)
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.84))
                    .clipShape(Capsule())
                    .padding(.bottom, 34)
                    .transition(.opacity)
                    .zIndex(200)
            }
        }
        .onAppear {
            coordinator.pushHost(hostId)
        }
        .onDisappear {
            coordinator.popHost(hostId)
        }
        .fullScreenCover(
            item: Binding(
                get: { isTop ? coordinator.activeBrowserSession : nil },
                set: { if $0 == nil && isTop { coordinator.activeBrowserSession = nil } }
            )
        ) { session in
            TelefamIosInAppBrowserView(session: session) {
                coordinator.activeBrowserSession = nil
            }
            .environmentObject(coordinator)
            .preferredColorScheme(preferredColorScheme)
        }
        .confirmationDialog(
            coordinator.activePhoneEntity?.formattedDisplay ?? "Phone number",
            isPresented: Binding(
                get: { isTop && coordinator.activePhoneEntity != nil },
                set: { if !$0 && isTop { coordinator.activePhoneEntity = nil } }
            ),
            titleVisibility: .visible
        ) {
            if let phone = coordinator.activePhoneEntity {
                Button("Call \(phone.formattedDisplay)") {
                    coordinator.activePhoneEntity = nil
                    let encoded = phone.dialNumber.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? phone.dialNumber
                    if let url = URL(string: "tel://\(encoded)") {
                        UIApplication.shared.open(url)
                    }
                }
                Button("Copy") {
                    coordinator.activePhoneEntity = nil
                    coordinator.copyText(phone.dialNumber, confirmation: "Phone number copied")
                }
                Button("Save contact") {
                    coordinator.activePhoneEntity = nil
                    coordinator.contactDraftToSave = SmartContactDraft(phone: phone)
                }
                Button("Send message") {
                    coordinator.activePhoneEntity = nil
                    let encoded = phone.dialNumber.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? phone.dialNumber
                    if let url = URL(string: "sms:\(encoded)") {
                        UIApplication.shared.open(url)
                    }
                }
            }
            Button("Cancel", role: .cancel) {
                if isTop { coordinator.activePhoneEntity = nil }
            }
        }
        .confirmationDialog(
            coordinator.activeEmailEntity?.emailAddress ?? "Email",
            isPresented: Binding(
                get: { isTop && coordinator.activeEmailEntity != nil },
                set: { if !$0 && isTop { coordinator.activeEmailEntity = nil } }
            ),
            titleVisibility: .visible
        ) {
            if let email = coordinator.activeEmailEntity {
                Button("Send email") {
                    coordinator.activeEmailEntity = nil
                    let encoded = email.emailAddress.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? email.emailAddress
                    if let url = URL(string: "mailto:\(encoded)") {
                        UIApplication.shared.open(url)
                    }
                }
                Button("Copy") {
                    coordinator.activeEmailEntity = nil
                    coordinator.copyText(email.emailAddress, confirmation: "Email copied")
                }
            }
            Button("Cancel", role: .cancel) {
                if isTop { coordinator.activeEmailEntity = nil }
            }
        }
        .sheet(
            item: Binding(
                get: { isTop ? coordinator.contactDraftToSave : nil },
                set: { if $0 == nil && isTop { coordinator.contactDraftToSave = nil } }
            )
        ) { draft in
            TelefamIosSaveContactSheet(phoneNumber: draft.phone.dialNumber)
                .preferredColorScheme(preferredColorScheme)
        }
    }
}

extension View {
    func telefamSmartOverlayHost(
        preferredColorScheme: ColorScheme?,
        showLocalToast: Bool = true
    ) -> some View {
        modifier(
            TelefamSmartOverlayHostModifier(
                preferredColorScheme: preferredColorScheme,
                showLocalToast: showLocalToast
            )
        )
    }
}
