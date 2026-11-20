#include "telefam/company/OfflineSyncEngine.hpp"

#include <algorithm>

namespace telefam::company {

void OfflineSyncEngine::enqueueAction(const OfflineAction& action) {
    std::lock_guard<std::mutex> lock(mu_);
    for (const auto& existing : queue_) {
        if (existing.client_op_id == action.client_op_id) {
            return;
        }
    }
    queue_.push_back(action);
}

std::vector<OfflineAction> OfflineSyncEngine::dequeueReadyBatch(
    NetworkQuality quality,
    int64_t now_ms
) {
    std::lock_guard<std::mutex> lock(mu_);
    if (quality == NetworkQuality::OFFLINE) {
        return {};
    }
    // Smaller batches under weak internet so payloads succeed within short radio windows.
    const std::size_t max_batch = (quality == NetworkQuality::WEAK) ? 3 : 20;
    std::vector<OfflineAction> batch;
    for (const auto& item : queue_) {
        if (item.next_retry_at_ms <= now_ms) {
            batch.push_back(item);
            if (batch.size() >= max_batch) {
                break;
            }
        }
    }
    return batch;
}

void OfflineSyncEngine::markAcked(const std::vector<std::string>& client_op_ids) {
    std::lock_guard<std::mutex> lock(mu_);
    queue_.erase(
        std::remove_if(queue_.begin(), queue_.end(), [&](const OfflineAction& item) {
            return std::find(client_op_ids.begin(), client_op_ids.end(), item.client_op_id) != client_op_ids.end();
        }),
        queue_.end()
    );
}

void OfflineSyncEngine::markFailed(const std::string& client_op_id, int64_t now_ms, NetworkQuality quality) {
    std::lock_guard<std::mutex> lock(mu_);
    for (auto& item : queue_) {
        if (item.client_op_id == client_op_id) {
            item.retry_count += 1;
            item.next_retry_at_ms = now_ms + computeBackoffMs(item.retry_count, quality);
            break;
        }
    }
}

std::size_t OfflineSyncEngine::pendingCount() {
    std::lock_guard<std::mutex> lock(mu_);
    return queue_.size();
}

int64_t OfflineSyncEngine::computeBackoffMs(int32_t retry_count, NetworkQuality quality) {
    const int32_t clamped = std::clamp(retry_count, 0, 7);
    const int64_t base_ms = (quality == NetworkQuality::WEAK) ? 2500LL : 1000LL;
    return std::min<int64_t>(base_ms << clamped, 120000LL);
}

} // namespace telefam::company
