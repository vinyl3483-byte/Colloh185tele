#include "telefam/company/InputSanitizer.hpp"

#include <cctype>
#include <iomanip>
#include <sstream>

namespace telefam::company {

std::string InputSanitizer::sanitizePlainText(const std::string& raw, std::size_t max_len) {
    std::string out;
    out.reserve(raw.size());
    bool in_tag = false;
    for (unsigned char c : raw) {
        if (c == '<') { in_tag = true; continue; }
        if (c == '>') { in_tag = false; continue; }
        if (in_tag) continue;
        if (c < 0x20 || c == 0x7F) continue;
        out.push_back(static_cast<char>(c));
        if (max_len > 0 && out.size() >= max_len) break;
    }
    while (!out.empty() && std::isspace(static_cast<unsigned char>(out.front()))) out.erase(out.begin());
    while (!out.empty() && std::isspace(static_cast<unsigned char>(out.back()))) out.pop_back();
    return out;
}

bool InputSanitizer::isValidUsername(const std::string& username) {
    if (username.size() < 3 || username.size() > 24) return false;
    for (unsigned char c : username) {
        const bool ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
        if (!ok) return false;
    }
    return true;
}

bool InputSanitizer::isAtLeast13YearsOld(
    int32_t birth_year, int32_t birth_month, int32_t birth_day,
    int32_t now_year, int32_t now_month, int32_t now_day
) {
    if (birth_year < 1900 || birth_month < 1 || birth_month > 12 || birth_day < 1 || birth_day > 31) {
        return false;
    }
    int32_t age = now_year - birth_year;
    if (now_month < birth_month || (now_month == birth_month && now_day < birth_day)) {
        age -= 1;
    }
    return age >= 13;
}

std::string InputSanitizer::computeMediaDHashHex(const std::vector<uint8_t>& luma_9x8) {
    if (luma_9x8.size() < 72) return "0000000000000000";
    uint64_t hash = 0;
    int bit = 0;
    for (int y = 0; y < 8; ++y) {
        for (int x = 0; x < 8; ++x) {
            const uint8_t left = luma_9x8[static_cast<std::size_t>(y * 9 + x)];
            const uint8_t right = luma_9x8[static_cast<std::size_t>(y * 9 + x + 1)];
            if (left > right) {
                hash |= (1ULL << bit);
            }
            ++bit;
        }
    }
    std::ostringstream oss;
    oss << std::hex << std::setw(16) << std::setfill('0') << hash;
    return oss.str();
}

} // namespace telefam::company
