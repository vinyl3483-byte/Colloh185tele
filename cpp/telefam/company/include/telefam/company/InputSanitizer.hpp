#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace telefam::company {

class InputSanitizer {
public:
    static std::string sanitizePlainText(const std::string& raw, std::size_t max_len);
    static bool isValidUsername(const std::string& username);
    static bool isAtLeast13YearsOld(
        int32_t birth_year, int32_t birth_month, int32_t birth_day,
        int32_t now_year, int32_t now_month, int32_t now_day
    );
    static std::string computeMediaDHashHex(const std::vector<uint8_t>& luma_9x8);
};

} // namespace telefam::company
