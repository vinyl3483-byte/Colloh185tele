#pragma once

#include <cstdint>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

namespace telefam::company {

struct RateLimitDecision {
    bool allowed;
    int64_t cooldown_seconds;
    int32_t tier_index;
};

class OtpRateLimiter {
public:
    static int64_t cooldownSecondsForSendCount(int32_t send_count);

    RateLimitDecision evaluateAndRecordSend(
        const std::string& account_hash,
        const std::string& device_fp,
        const std::string& ip_fp,
        int64_t now_epoch_seconds
    );

    int64_t remainingCooldownSeconds(
        const std::string& account_hash,
        const std::string& device_fp,
        int64_t now_epoch_seconds
    );

    bool evaluateVerifyAttempt(
        const std::string& account_hash,
        const std::string& device_fp,
        const std::string& ip_fp,
        int64_t now_epoch_seconds
    );

    void resetAccountCooldown(
        const std::string& account_hash,
        const std::string& device_fp
    );

private:
    struct AccountState {
        int32_t send_count = 0;
        int64_t next_allowed_epoch = 0;
        int64_t last_updated_epoch = 0;
    };

    bool checkQuotaLocked(
        std::unordered_map<std::string, std::vector<int64_t>>& buckets,
        const std::string& key,
        int32_t max_requests,
        int64_t window_seconds,
        int64_t now_epoch_seconds
    );

    void recordQuotaLocked(
        std::unordered_map<std::string, std::vector<int64_t>>& buckets,
        const std::string& key,
        int64_t now_epoch_seconds
    );

    std::mutex mu_;
    std::unordered_map<std::string, AccountState> cooldowns_;
    std::unordered_map<std::string, std::vector<int64_t>> account_hits_;
    std::unordered_map<std::string, std::vector<int64_t>> device_hits_;
    std::unordered_map<std::string, std::vector<int64_t>> ip_hits_;
};

} // namespace telefam::company
