#ifndef TELEFAM_COMPANY_C_BRIDGE_H
#define TELEFAM_COMPANY_C_BRIDGE_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct TelefamEngineHandle TelefamEngineHandle;

TelefamEngineHandle* telefam_engine_create(void);
void telefam_engine_destroy(TelefamEngineHandle* engine);

int32_t telefam_check_and_record_otp_send(
    TelefamEngineHandle* engine,
    const char* email,
    const char* device_fp,
    const char* ip_fp,
    int64_t now_epoch_seconds,
    int64_t* out_cooldown_seconds
);

int64_t telefam_remaining_cooldown_seconds(
    TelefamEngineHandle* engine,
    const char* email,
    const char* device_fp,
    int64_t now_epoch_seconds
);

void telefam_compute_device_fingerprint(
    const char* platform,
    const char* model,
    const char* os_version,
    const char* install_id,
    char* out_buf,
    size_t out_buf_len
);

void telefam_sanitize_error(
    int32_t http_status,
    const char* raw_body,
    char* out_buf,
    size_t out_buf_len
);

#ifdef __cplusplus
}
#endif

#endif // TELEFAM_COMPANY_C_BRIDGE_H
