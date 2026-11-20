#include "telefam/company/OtpRateLimiter.hpp"

#include <algorithm>
#include <array>

namespace telefam::company {

namespace {
constexpr std::array<int64_t, 6> kEscalatingScheduleSeconds = {
    60,     // 1st send/resend: 1 minute
    60,     // 2nd resend: 1 minute
    60,     // 3rd resend: 1 minute
    900,    // 4th resend: 15 minutes
    3600,   // 5th resend: 1 hour
    86400   // 6th+ resend: 1 day
};
constexpr int64_t kDailyWindowSeconds = 86400;
constexpr int64_t kVerifyWindowSeconds = 900;
constexpr int32_t kMaxAccountPerDay = 12;
constexpr int32_t kMaxDevicePerDay = 20;
constexpr int32_t kMaxIpPerDay = 40;
constexpr int32_t kMaxVerifyPer15Min = 15;
} // namespace

int64_t OtpRateLimiter::cooldownSecondsForSendCount(int32_t send_count) {
    if (send_count <= 0) {
        return kEscalatingScheduleSeconds[0];
    }
    const std::size_t idx = std::min<std::size_t>(
        static_cast<std::size_t>(send_count - 1),
        kEscalatingScheduleSeconds.size() - 1
    );
    return kEscalatingScheduleSeconds[idx];
}

RateLimitDecision OtpRateLimiter::evaluateAndRecordSend(
    const std::string& account_hash,
    const std::string& device_fp,
    const std::string& ip_fp,
    int64_t now_epoch_seconds
) {
    std::lock_guard<std::mutex> lock(mu_);
    const std::string key = account_hash + ":" + device_fp;

    auto it = cooldowns_.find(key);
    if (it != cooldowns_.end()) {
        if (now_epoch_seconds - it->second.last_updated_epoch >= kDailyWindowSeconds) {
            cooldowns_.erase(it);
        } else if (now_epoch_seconds < it->second.next_allowed_epoch) {
            const int64_t rem = std::max<int64_t>(1, it->second.next_allowed_epoch - now_epoch_seconds);
            return RateLimitDecision{false, rem, it->second.send_count};
        }
    }

    if (!checkQuotaLocked(account_hits_, account_hash, kMaxAccountPerDay, kDailyWindowSeconds, now_epoch_seconds) ||
        !checkQuotaLocked(device_hits_, device_fp, kMaxDevicePerDay, kDailyWindowSeconds, now_epoch_seconds) ||
        !checkQuotaLocked(ip_hits_, ip_fp, kMaxIpPerDay, kDailyWindowSeconds, now_epoch_seconds)) {
        return RateLimitDecision{false, 0, -1};
    }

    recordQuotaLocked(account_hits_, account_hash, now_epoch_seconds);
    recordQuotaLocked(device_hits_, device_fp, now_epoch_seconds);
    recordQuotaLocked(ip_hits_, ip_fp, now_epoch_seconds);

    auto& st = cooldowns_[key];
    st.send_count += 1;
    const int64_t cooldown = cooldownSecondsForSendCount(st.send_count);
    st.next_allowed_epoch = now_epoch_seconds + cooldown;
    st.last_updated_epoch = now_epoch_seconds;

    return RateLimitDecision{true, cooldown, st.send_count};
}

int64_t OtpRateLimiter::remainingCooldownSeconds(
    const std::string& account_hash,
    const std::string& device_fp,
    int64_t now_epoch_seconds
) {
    std::lock_guard<std::mutex> lock(mu_);
    const auto it = cooldowns_.find(account_hash + ":" + device_fp);
    if (it == cooldowns_.end()) {
        return 0;
    }
    if (now_epoch_seconds >= it->second.next_allowed_epoch) {
        return 0;
    }
    return it->second.next_allowed_epoch - now_epoch_seconds;
}

bool OtpRateLimiter::evaluateVerifyAttempt(
    const std::string& account_hash,
    const std::string& device_fp,
    const std::string& ip_fp,
    int64_t now_epoch_seconds
) {
    std::lock_guard<std::mutex> lock(mu_);
    const std::string acc_k = "v:" + account_hash;
    const std::string dev_k = "v:" + device_fp;
    const std::string ip_k = "v:" + ip_fp;

    if (!checkQuotaLocked(account_hits_, acc_k, kMaxVerifyPer15Min, kVerifyWindowSeconds, now_epoch_seconds) ||
        !checkQuotaLocked(device_hits_, dev_k, kMaxVerifyPer15Min, kVerifyWindowSeconds, now_epoch_seconds) ||
        !checkQuotaLocked(ip_hits_, ip_k, kMaxVerifyPer15Min, kVerifyWindowSeconds, now_epoch_seconds)) {
        return false;
    }

    recordQuotaLocked(account_hits_, acc_k, now_epoch_seconds);
    recordQuotaLocked(device_hits_, dev_k, now_epoch_seconds);
    recordQuotaLocked(ip_hits_, ip_k, now_epoch_seconds);
    return true;
}

void OtpRateLimiter::resetAccountCooldown(
    const std::string& account_hash,
    const std::string& device_fp
) {
    std::lock_guard<std::mutex> lock(mu_);
    cooldowns_.erase(account_hash + ":" + device_fp);
}

bool OtpRateLimiter::checkQuotaLocked(
    std::unordered_map<std::string, std::vector<int64_t>>& buckets,
    const std::string& key,
    int32_t max_requests,
    int64_t window_seconds,
    int64_t now_epoch_seconds
) {
    auto it = buckets.find(key);
    if (it == buckets.end()) {
        return true;
    }
    const int64_t cutoff = now_epoch_seconds - window_seconds;
    auto& hits = it->second;
    hits.erase(
        std::remove_if(hits.begin(), hits.end(), [cutoff](int64_t ts) { return ts <= cutoff; }),
        hits.end()
    );
    return static_cast<int32_t>(hits.size()) < max_requests;
}

void OtpRateLimiter::recordQuotaLocked(
    std::unordered_map<std::string, std::vector<int64_t>>& buckets,
    const std::string& key,
    int64_t now_epoch_seconds
) {
    buckets[key].push_back(now_epoch_seconds);
}

} // namespace telefam::company
