cmake_minimum_required(VERSION 3.22)
project(telefam_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(telefam_core STATIC
    src/OtpRateLimiter.cpp
    src/FingerprintEngine.cpp
    src/OfflineSyncEngine.cpp
    src/ErrorSanitizer.cpp
    src/InputSanitizer.cpp
    src/TelefamEngine.cpp
    bridge/TelefamCBridge.cpp
)

target_include_directories(telefam_core PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/bridge
)

if(ANDROID OR TELEFAM_BUILD_JNI)
    add_library(telefam_jni SHARED
        bridge/TelefamJniBridge.cpp
    )
    target_link_libraries(telefam_jni PRIVATE telefam_core)
    if(ANDROID)
        find_library(log-lib log)
        target_link_libraries(telefam_jni PRIVATE ${log-lib})
    endif()
endif()
