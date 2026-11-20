#pragma once

#include <cstdint>
#include <string>

namespace telefam::company {

class ErrorSanitizer {
public:
    static std::string sanitizeServerError(int32_t http_status, const std::string& raw_body);
};

} // namespace telefam::company
