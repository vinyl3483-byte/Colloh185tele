import CoreLocation
import CoreTelephony
import Foundation
import Network
import SwiftUI
import UIKit

struct TelefamIosDeviceContextSnapshot: Codable, Equatable {
    let deviceName: String
    let platform: String
    let hardwareModel: String
    let carrierCountryIso: String
    let simCountryIso: String
    let gpsCountryIso: String
    let gpsCity: String
    let localeCountryIso: String
    let timezone: String
    let latBucket: Double
    let lonBucket: Double
    let signalSource: String
    let capturedAtMs: Int64

    enum CodingKeys: String, CodingKey {
        case deviceName = "device_name"
        case platform
        case hardwareModel = "hardware_model"
        case carrierCountryIso = "carrier_country_iso"
        case simCountryIso = "sim_country_iso"
        case gpsCountryIso = "gps_country_iso"
        case gpsCity = "gps_city"
        case localeCountryIso = "locale_country_iso"
        case timezone
        case latBucket = "lat_bucket"
        case lonBucket = "lon_bucket"
        case signalSource = "signal_source"
        case capturedAtMs = "captured_at_ms"
    }
}

struct TelefamIosDeviceApprovalWaitState: Equatable, Identifiable {
    let email: String
    let requestId: String
    let pollToken: String
    let targetDeviceName: String
    let targetLocation: String
    let requestTimeMs: Int64
    var remainingSeconds: Int
    var isDeniedBlocked: Bool = false

    var id: String { requestId }
}

struct TelefamIosIncomingLoginRequest: Codable, Equatable, Identifiable {
    let requestId: String
    let deviceName: String
    let location: String
    let ipAddressMasked: String
    let requestedAtMs: Int64
    let expiresAtMs: Int64
    var remainingSeconds: Int

    var id: String { requestId }

    enum CodingKeys: String, CodingKey {
        case requestId = "request_id"
        case deviceName = "device_name"
        case location
        case ipAddressMasked = "ip_address_masked"
        case requestedAtMs = "requested_at_ms"
        case expiresAtMs = "expires_at_ms"
        case remainingSeconds = "remaining_seconds"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        requestId = try c.decodeIfPresent(String.self, forKey: .requestId) ?? ""
        deviceName = try c.decodeIfPresent(String.self, forKey: .deviceName) ?? "New Device"
        location = try c.decodeIfPresent(String.self, forKey: .location) ?? "Unknown location"
        ipAddressMasked = try c.decodeIfPresent(String.self, forKey: .ipAddressMasked) ?? "Hidden"
        requestedAtMs = try c.decodeIfPresent(Int64.self, forKey: .requestedAtMs) ?? 0
        expiresAtMs = try c.decodeIfPresent(Int64.self, forKey: .expiresAtMs) ?? 0
        remainingSeconds = try c.decodeIfPresent(Int.self, forKey: .remainingSeconds) ?? 60
    }
}

struct TelefamIosLoginApprovedState: Equatable {
    let deviceName: String
    let location: String
    let requestedAtMs: Int64
}

final class TelefamIosDeviceContextCollector: NSObject {
    static let shared = TelefamIosDeviceContextCollector()

    private let pendingDefaultsKey = "telefam_ios_pending_device_context_json"
    private let lastCountryDefaultsKey = "telefam_ios_last_synced_country_iso"
    private let cachedGeoCountryKey = "telefam_ios_cached_gps_country_iso"
    private let cachedGeoCityKey = "telefam_ios_cached_gps_city"
    private let cachedGeoBucketKey = "telefam_ios_cached_gps_bucket"

    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "company.telefam.network.telemetry")
    private var monitorStarted = false
    private(set) var isOnline: Bool = true
    var onNetworkRestored: (() -> Void)?

    private lazy var locationManager: CLLocationManager = {
        let mgr = CLLocationManager()
        mgr.desiredAccuracy = kCLLocationAccuracyKilometer
        return mgr
    }()

    override private init() {
        super.init()
        startNetworkMonitorIfNeeded()
    }

    func startNetworkMonitorIfNeeded() {
        guard !monitorStarted else { return }
        monitorStarted = true
        pathMonitor.pathUpdateHandler = { [weak self] path in
            guard let self else { return }
            let satisfied = (path.status == .satisfied)
            let becameOnline = !self.isOnline && satisfied
            self.isOnline = satisfied
            if becameOnline || satisfied {
                self.onNetworkRestored?()
            }
        }
        pathMonitor.start(queue: monitorQueue)
    }

    static func machineIdentifier() -> String {
        var sysInfo = utsname()
        uname(&sysInfo)
        let raw = withUnsafePointer(to: &sysInfo.machine) {
            $0.withMemoryRebound(to: CChar.self, capacity: 1) { ptr in
                String(cString: ptr)
            }
        }
        return raw.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func formatRealDeviceName() -> String {
        let identifier = machineIdentifier()
        if let mapped = friendlyModelMap[identifier], !mapped.isEmpty {
            return String(mapped.prefix(64))
        }
        let uiName = UIDevice.current.name.trimmingCharacters(in: .whitespacesAndNewlines)
        if !uiName.isEmpty && !uiName.caseInsensitiveCompare("iPhone").rawValue.description.isEmpty && uiName != "iPhone" && uiName != "iPad" {
            return String(uiName.prefix(64))
        }
        let model = UIDevice.current.model.trimmingCharacters(in: .whitespacesAndNewlines)
        return String((model.isEmpty ? "Apple Device" : model).prefix(64))
    }

    func collectSnapshot() async -> TelefamIosDeviceContextSnapshot {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let deviceName = Self.formatRealDeviceName()
        let hwModel = Self.machineIdentifier().ifEmpty("iOS")
        let localeIso = (Locale.current.region?.identifier ?? "").uppercased()
        let tz = TimeZone.current.identifier

        var carrierIso = ""
        let telephony = CTTelephonyNetworkInfo()
        if let providers = telephony.serviceSubscriberCellularProviders {
            if let dataId = telephony.dataServiceIdentifier,
               let activeCarrier = providers[dataId]?.isoCountryCode?.uppercased(),
               activeCarrier.count == 2,
               activeCarrier != "ZZ" {
                carrierIso = activeCarrier
            } else if let anyCarrier = providers.values.compactMap({ $0.isoCountryCode?.uppercased() }).first(where: { $0.count == 2 && $0 != "ZZ" }) {
                carrierIso = anyCarrier
            }
        }
        let simIso = carrierIso

        // Passive GPS country attribution ONLY if permission was already granted by the user elsewhere (no prompts).
        var gpsIso = UserDefaults.standard.string(forKey: cachedGeoCountryKey) ?? ""
        var gpsCity = UserDefaults.standard.string(forKey: cachedGeoCityKey) ?? ""
        var latBucket = 0.0
        var lonBucket = 0.0

        let authStatus = locationManager.authorizationStatus
        if authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways,
           let loc = locationManager.location {
            latBucket = (loc.coordinate.latitude * 10.0).rounded() / 10.0
            lonBucket = (loc.coordinate.longitude * 10.0).rounded() / 10.0
            let bucketKey = String(format: "%.1f,%.1f", latBucket, lonBucket)
            let lastBucket = UserDefaults.standard.string(forKey: cachedGeoBucketKey) ?? ""
            if bucketKey != lastBucket || gpsIso.isEmpty {
                if let placemark = try? await CLGeocoder().reverseGeocodeLocation(loc).first {
                    let cIso = (placemark.isoCountryCode ?? "").uppercased()
                    let cCity = (placemark.locality ?? placemark.administrativeArea ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                    if cIso.count == 2 {
                        gpsIso = cIso
                        UserDefaults.standard.set(cIso, forKey: cachedGeoCountryKey)
                    }
                    if !cCity.isEmpty {
                        gpsCity = cCity
                        UserDefaults.standard.set(cCity, forKey: cachedGeoCityKey)
                    }
                    UserDefaults.standard.set(bucketKey, forKey: cachedGeoBucketKey)
                }
            }
        }

        let source: String
        if gpsIso.count == 2 {
            source = "ios_gps_passive"
        } else if carrierIso.count == 2 {
            source = "ios_carrier"
        } else {
            source = "ios_locale"
        }

        return TelefamIosDeviceContextSnapshot(
            deviceName: deviceName,
            platform: "ios",
            hardwareModel: hwModel,
            carrierCountryIso: carrierIso,
            simCountryIso: simIso,
            gpsCountryIso: gpsIso,
            gpsCity: gpsCity,
            localeCountryIso: localeIso.count == 2 ? localeIso : "",
            timezone: tz,
            latBucket: latBucket,
            lonBucket: lonBucket,
            signalSource: source,
            capturedAtMs: nowMs
        )
    }

    func applyTelemetryHeaders(to request: inout URLRequest) {
        let deviceName = Self.formatRealDeviceName()
        let hwModel = Self.machineIdentifier().ifEmpty("iOS")
        let localeIso = (Locale.current.region?.identifier ?? "").uppercased()
        let tz = TimeZone.current.identifier
        var carrierIso = ""
        let telephony = CTTelephonyNetworkInfo()
        if let providers = telephony.serviceSubscriberCellularProviders,
           let code = providers.values.compactMap({ $0.isoCountryCode?.uppercased() }).first(where: { $0.count == 2 && $0 != "ZZ" }) {
            carrierIso = code
        }

        request.setValue(deviceName, forHTTPHeaderField: "X-Telefam-Device-Name")
        request.setValue("ios", forHTTPHeaderField: "X-Telefam-Device-Platform")
        request.setValue(hwModel, forHTTPHeaderField: "X-Telefam-Hardware-Model")
        if carrierIso.count == 2 {
            request.setValue(carrierIso, forHTTPHeaderField: "X-Telefam-Carrier-Country")
        }
        if localeIso.count == 2 {
            request.setValue(localeIso, forHTTPHeaderField: "X-Telefam-Locale-Country")
        }
        if !tz.isEmpty {
            request.setValue(tz, forHTTPHeaderField: "X-Telefam-Timezone")
        }
    }

    func savePendingSnapshot(_ snapshot: TelefamIosDeviceContextSnapshot) {
        if let data = try? JSONEncoder().encode(snapshot) {
            UserDefaults.standard.set(data, forKey: pendingDefaultsKey)
        }
    }

    func loadPendingSnapshot() -> TelefamIosDeviceContextSnapshot? {
        guard let data = UserDefaults.standard.data(forKey: pendingDefaultsKey) else { return nil }
        return try? JSONDecoder().decode(TelefamIosDeviceContextSnapshot.self, from: data)
    }

    func clearPendingSnapshot() {
        UserDefaults.standard.removeObject(forKey: pendingDefaultsKey)
    }

    func saveLastSyncedCountry(_ iso: String) {
        let clean = iso.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard clean.count == 2 else { return }
        UserDefaults.standard.set(clean, forKey: lastCountryDefaultsKey)
    }

    func loadLastSyncedCountry() -> String {
        UserDefaults.standard.string(forKey: lastCountryDefaultsKey) ?? ""
    }

    private static let friendlyModelMap: [String: String] = [
        "iPhone15,2": "iPhone 14 Pro",
        "iPhone15,3": "iPhone 14 Pro Max",
        "iPhone15,4": "iPhone 15",
        "iPhone15,5": "iPhone 15 Plus",
        "iPhone16,1": "iPhone 15 Pro",
        "iPhone16,2": "iPhone 15 Pro Max",
        "iPhone17,1": "iPhone 16 Pro",
        "iPhone17,2": "iPhone 16 Pro Max",
        "iPhone17,3": "iPhone 16",
        "iPhone17,4": "iPhone 16 Plus",
        "iPad14,3": "iPad Pro 11",
        "iPad14,5": "iPad Pro 12.9",
        "iPad16,3": "iPad Pro 11 (M4)",
        "iPad16,5": "iPad Pro 13 (M4)"
    ]
}

private extension String {
    func ifEmpty(_ fallback: String) -> String {
        isEmpty ? fallback : self
    }
}
