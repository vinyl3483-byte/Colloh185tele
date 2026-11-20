#include "telefam/company/ErrorSanitizer.hpp"

namespace telefam::company {

std::string ErrorSanitizer::sanitizeServerError(int32_t http_status, const std::string& /*raw_body*/) {
    if (http_status == 429) {
        return "An error has occurred. Please try again later.";
    }
    return "An error has occurred. Please try again.";
}

} // namespace telefam::company
