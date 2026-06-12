# CMake build file for CinePilotTV native subtitle decoders.
#
# This build only fires when the Android NDK is available on the build machine
# (the Gradle side gates `externalNativeBuild` on ndkAvailable, see
# /app/build.gradle.kts).  Without an NDK the app still ships and runs; the
# Kotlin side of LibassSubtitleDecoder falls back to Media3's default text
# renderer when System.loadLibrary("cinepilot_subs") throws.
#
# The library currently ships STUB implementations of the LibASS JNI surface:
# real LibASS source + fribidi + freetype + harfbuzz would be ~15 MB of vendored
# C and is out of scope for P1.  The JNI stubs below return "no frames decoded"
# which keeps the fallback path active.  To swap in a real LibASS renderer:
#
#   1. Vendor libass + its dependencies under app/src/main/cpp/third_party/.
#   2. Add the vendored sources + include dirs to add_library() below.
#   3. Replace the Java_* stub bodies in subs_decoder_jni.cpp with calls into
#      ass_set_frame_size / ass_read_file / ass_render_frame.

cmake_minimum_required(VERSION 3.22.1)
project("cinepilot_subs")

add_library(
    cinepilot_subs
    SHARED
    subs_decoder_jni.cpp
)

target_compile_options(cinepilot_subs PRIVATE
    -std=c++17
    -fexceptions
    -Wall
    -Wextra
    -Wno-unused-parameter
)

target_link_libraries(cinepilot_subs
    android
    log
    jnigraphics
)
