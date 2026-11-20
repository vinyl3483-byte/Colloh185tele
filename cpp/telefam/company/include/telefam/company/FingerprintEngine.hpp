#pragma once

#include <string>

namespace telefam::company {

class FingerprintEngine {
public:
    static std::string computeDeviceFingerprint(
        const std::string& platform,
        const std::string& model,
        const std::string& os_version,
        const std::string& install_id
    );

    static std::string computeIpFingerprint(const std::string& ip_address);

    static std::string hashAccountEmail(const std::string& email);

    static std::string computeVideoContentId(
        const std::string& joined_frame_hashes,
        int64_t duration_ms,
        int64_t trim_start_ms,
        int64_t trim_end_ms
    );

private:
    static std::string sha256Hex(const std::string& input);
};

} // namespace telefam::company
