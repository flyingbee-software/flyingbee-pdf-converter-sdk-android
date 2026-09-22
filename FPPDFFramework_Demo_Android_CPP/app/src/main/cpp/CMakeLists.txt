# ---------------------------------------------------------------------------
# FPPDFFramework_jni - JNI bridge between the Kotlin UI and the
# FPPDFFramework C++ SDK (the C++ API demo's own bridge)
#
# Builds libFPPDFFramework_jni.so, which links against the self-contained
# libFPPDFFramework.so shipped in FPPDFFramework.aar.
#
# The SDK is consumed as a plain imported shared library rather than through
# Prefab: libFPPDFFramework.so is a shared library with a privately,
# statically linked libc++ (self-contained delivery), a shape AGP's Prefab
# integration refuses to hand to any STL-using consumer.  The Gradle task
# :app:unpackFppdfSdk unzips the AAR into build/fppdf-sdk and passes that
# path here as -DFPPDF_SDK_DIR; CMake then imports the .so for the current
# ABI and adds the three public headers to the include path.
#
# At runtime the APK carries both libFPPDFFramework_jni.so and
# libFPPDFFramework.so (AGP merges the AAR's jni/<abi>/ libraries), and the
# Android dynamic linker resolves libFPPDFFramework.so by SONAME - no extra
# setup required.
#
# The bridge code deliberately avoids the C++ STL (C types and PODs only), so
# it also compiles when the app is configured with -DANDROID_STL=system.
# ---------------------------------------------------------------------------

cmake_minimum_required(VERSION 3.22.1)
project(FPPDFFramework_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT DEFINED FPPDF_SDK_DIR)
    message(FATAL_ERROR
        "FPPDF_SDK_DIR is not set. It is passed by :app:unpackFppdfSdk in "
        "app/build.gradle.kts; make sure the AAR is unpacked before "
        "configuring this project.")
endif()

# The ABI CMake is currently building for (android-<abi> directory naming,
# same convention the AAR's prefab/ folder uses).
set(FPPDF_ABI_DIR "${CMAKE_ANDROID_ARCH_ABI}")

add_library(FPPDFFramework::FPPDFFramework SHARED IMPORTED)
set_target_properties(FPPDFFramework::FPPDFFramework PROPERTIES
    IMPORTED_LOCATION "${FPPDF_SDK_DIR}/jni/${FPPDF_ABI_DIR}/libFPPDFFramework.so"
    INTERFACE_INCLUDE_DIRECTORIES "${FPPDF_SDK_DIR}/prefab/modules/FPPDFFramework/include"
)

add_library(FPPDFFramework_jni SHARED FPPDFFramework_jni.cpp)

target_link_libraries(FPPDFFramework_jni PRIVATE
    FPPDFFramework::FPPDFFramework
    android
    log
)
