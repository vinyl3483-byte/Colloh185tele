#include <jni.h>
#include <string>
#include "telefam/company/TelefamEngine.hpp"

namespace {
telefam::company::TelefamEngine& globalEngine() {
    static telefam::company::TelefamEngine instance;
    return instance;
}

std::string jstringToStdString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(jstr, chars);
    return result;
}
} // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_telefam_company_core_NativeTelefamBridge_nativeDeviceFingerprint(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring platform,
    jstring model,
    jstring osVersion,
    jstring installId
) {
    const std::string fp = globalEngine().buildDeviceFingerprint(
        jstringToStdString(env, platform),
        jstringToStdString(env, model),
        jstringToStdString(env, osVersion),
        jstringToStdString(env, installId)
    );
    return env->NewStringUTF(fp.c_str());
}

JNIEXPORT jstring JNICALL
Java_telefam_company_core_NativeTelefamBridge_nativeIpFingerprint(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring ipAddress
) {
    const std::string fp = globalEngine().buildIpFingerprint(jstringToStdString(env, ipAddress));
    return env->NewStringUTF(fp.c_str());
}

JNIEXPORT jlong JNICALL
Java_telefam_company_core_NativeTelefamBridge_nativeEvaluateOtpSend(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring email,
    jstring deviceFp,
    jstring ipFp,
    jlong nowEpochSeconds
) {
    const std::string acct = globalEngine().hashAccount(jstringToStdString(env, email));
    const auto decision = globalEngine().rateLimiter().evaluateAndRecordSend(
        acct,
        jstringToStdString(env, deviceFp),
        jstringToStdString(env, ipFp),
        static_cast<int64_t>(nowEpochSeconds)
    );
    // Positive = allowed with returned cooldown seconds; negative = blocked with remaining seconds.
    return decision.allowed
        ? static_cast<jlong>(decision.cooldown_seconds)
        : -static_cast<jlong>(decision.cooldown_seconds == 0 ? 60 : decision.cooldown_seconds);
}

JNIEXPORT jstring JNICALL
Java_telefam_company_core_NativeTelefamBridge_nativeSanitizeError(
    JNIEnv* env,
    jobject /*thiz*/,
    jint httpStatus,
    jstring rawBody
) {
    const std::string clean = globalEngine().sanitizeError(
        static_cast<int32_t>(httpStatus),
        jstringToStdString(env, rawBody)
    );
    return env->NewStringUTF(clean.c_str());
}

JNIEXPORT jstring JNICALL
Java_telefam_company_core_NativeTelefamBridge_nativeComputeVideoContentId(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring joinedFrameHashes,
    jlong durationMs,
    jlong trimStartMs,
    jlong trimEndMs
) {
    const std::string contentId = globalEngine().buildVideoContentId(
        jstringToStdString(env, joinedFrameHashes),
        static_cast<int64_t>(durationMs),
        static_cast<int64_t>(trimStartMs),
        static_cast<int64_t>(trimEndMs)
    );
    return env->NewStringUTF(contentId.c_str());
}

} // extern "C"
