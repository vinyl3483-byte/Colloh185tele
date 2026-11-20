#include "TelefamCBridge.h"
#include "telefam/company/TelefamEngine.hpp"

#include <algorithm>
#include <cstring>

struct TelefamEngineHandle {
    telefam::company::TelefamEngine engine;
};

namespace {
void copyStringToBuf(const std::string& src, char* out_buf, size_t out_buf_len) {
    if (!out_buf || out_buf_len == 0) return;
    const size_t n = std::min(src.size(), out_buf_len - 1);
    std::memcpy(out_buf, src.data(), n);
    out_buf[n] = '\0';
}
} // namespace

extern "C" {

TelefamEngineHandle* telefam_engine_create(void) {
    return new TelefamEngineHandle();
}

void telefam_engine_destroy(TelefamEngineHandle* handle) {
    delete handle;
}

int32_t telefam_check_and_record_otp_send(
    TelefamEngineHandle* handle,
    const char* email,
    const char* device_fp,
    const char* ip_fp,
    int64_t now_epoch_seconds,
    int64_t* out_cooldown_seconds
) {
    if (!handle || !email || !device_fp || !ip_fp) return 0;
    const auto acct = handle->engine.hashAccount(email);
    const auto decision = handle->engine.rateLimiter().evaluateAndRecordSend(
        acct, device_fp, ip_fp, now_epoch_seconds
    );
    if (out_cooldown_seconds) {
        *out_cooldown_seconds = decision.cooldown_seconds;
    }
    return decision.allowed ? 1 : 0;
}

int64_t telefam_remaining_cooldown_seconds(
    TelefamEngineHandle* handle,
    const char* email,
    const char* device_fp,
    int64_t now_epoch_seconds
) {
    if (!handle || !email || !device_fp) return 0;
    return handle->engine.rateLimiter().remainingCooldownSeconds(
        handle->engine.hashAccount(email), device_fp, now_epoch_seconds
    );
}

void telefam_compute_device_fingerprint(
    const char* platform,
    const char* model,
    const char* os_version,
    const char* install_id,
    char* out_buf,
    size_t out_buf_len
) {
    const std::string fp = telefam::company::FingerprintEngine::computeDeviceFingerprint(
        platform ? platform : "",
        model ? model : "",
        os_version ? os_version : "",
        install_id ? install_id : ""
    );
    copyStringToBuf(fp, out_buf, out_buf_len);
}

void telefam_sanitize_error(
    int32_t http_status,
    const char* raw_body,
    char* out_buf,
    size_t out_buf_len
) {
    const std::string sanitized = telefam::company::ErrorSanitizer::sanitizeServerError(
        http_status, raw_body ? raw_body : ""
    );
    copyStringToBuf(sanitized, out_buf, out_buf_len);
}

} // extern "C"
