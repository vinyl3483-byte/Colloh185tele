#pragma once

#include <cstdint>
#include <mutex>
#include <string>
#include <vector>

namespace telefam::company {

enum class NetworkQuality {
    OFFLINE = 0,
    WEAK = 1,
    GOOD = 2
};

struct OfflineAction {
    std::string client_op_id;
    std::string room_id;
    std::string kind;
    std::string payload;
    int64_t created_at_ms;
    int32_t retry_count = 0;
    int64_t next_retry_at_ms = 0;
};

class OfflineSyncEngine {
public:
    void enqueueAction(const OfflineAction& action);
    std::vector<OfflineAction> dequeueReadyBatch(
        NetworkQuality quality,
        int64_t now_ms
    );
    void markAcked(const std::vector<std::string>& client_op_ids);
    void markFailed(const std::string& client_op_id, int64_t now_ms, NetworkQuality quality);
    std::size_t pendingCount();

    static int64_t computeBackoffMs(int32_t retry_count, NetworkQuality quality);

private:
    std::mutex mu_;
    std::vector<OfflineAction> queue_;
};

} // namespace telefam::company
