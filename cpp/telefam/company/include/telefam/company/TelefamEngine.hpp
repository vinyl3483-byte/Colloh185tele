#pragma once

#include "telefam/company/ErrorSanitizer.hpp"
#include "telefam/company/FingerprintEngine.hpp"
#include "telefam/company/OfflineSyncEngine.hpp"
#include "telefam/company/OtpRateLimiter.hpp"

namespace telefam::company {

class TelefamEngine {
public:
    OtpRateLimiter& rateLimiter() { return rate_limiter_; }
    OfflineSyncEngine& offlineSync() { return offline_sync_; }

    std::string buildDeviceFingerprint(
        const std::string& platform,
        const std::string& model,
        const std::string& os_version,
        const std::string& install_id
    ) const {
        return FingerprintEngine::computeDeviceFingerprint(platform, model, os_version, install_id);
    }

    std::string buildIpFingerprint(const std::string& ip) const {
        return FingerprintEngine::computeIpFingerprint(ip);
    }

    std::string hashAccount(const std::string& email) const {
        return FingerprintEngine::hashAccountEmail(email);
    }

    std::string sanitizeError(int32_t http_status, const std::string& raw_body) const {
        return ErrorSanitizer::sanitizeServerError(http_status, raw_body);
    }

    std::string buildVideoContentId(
        const std::string& joined_frame_hashes,
        int64_t duration_ms,
        int64_t trim_start_ms,
        int64_t trim_end_ms
    ) const {
        return FingerprintEngine::computeVideoContentId(
            joined_frame_hashes, duration_ms, trim_start_ms, trim_end_ms
        );
    }

private:
    OtpRateLimiter rate_limiter_;
    OfflineSyncEngine offline_sync_;
};

} // namespace telefam::company
