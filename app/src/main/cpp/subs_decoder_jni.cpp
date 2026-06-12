// JNI bridge between LibassSubtitleDecoder.kt and a (currently stubbed) native
// LibASS renderer.  See the sibling CMakeLists.txt for notes on swapping in a
// real LibASS build; for P1 the stubs below are intentionally no-ops so the
// decoder's nativeAvailable() check still succeeds but the Kotlin layer keeps
// the Media3 default text renderer as the visible fallback.

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <vector>

#define LOG_TAG "CinePilotSubs"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Fake renderer "state".  A real implementation would store an ASS_Tracker*
// allocated by ass_library_init + ass_renderer_init; for the stub we just
// return a non-zero sentinel so nativeAvailable() -> true and Kotlin can
// distinguish "library loaded OK" from "NDK build didn't happen".
struct DecoderHandle {
    int width;
    int height;
    uint64_t next_frame_id;
};

extern "C" JNIEXPORT jlong JNICALL
Java_tv_cinepilot_tv_subtitles_LibassSubtitleDecoder_nativeInit(
    JNIEnv* /*env*/, jobject /*thiz*/, jint width, jint height) {
    auto* handle = new (std::nothrow) DecoderHandle{
        .width = width,
        .height = height,
        .next_frame_id = 0,
    };
    if (!handle) return 0;
    LOGD("LibASS stub initialized (%dx%d)", width, height);
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_tv_cinepilot_tv_subtitles_LibassSubtitleDecoder_nativeRelease(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    if (!handle) return;
    delete reinterpret_cast<DecoderHandle*>(handle);
}

// Build a zero-size LibassFrame[] (no cues) — the Kotlin side interprets an
// empty array as "author styling is unavailable" and lets the default text
// renderer paint fallback cues using the user's CaptionStyleCompat.
extern "C" JNIEXPORT jobjectArray JNICALL
Java_tv_cinepilot_tv_subtitles_LibassSubtitleDecoder_nativeDecodeAss(
    JNIEnv* env, jobject /*thiz*/, jlong handle,
    jbyteArray /*bytes*/, jint /*size*/, jlong /*ptsUs*/) {
    if (!handle) return nullptr;
    // Stub: return null to signal "no composited frames produced".
    // Callers are contractually required to treat a null return as a graceful
    // degradation rather than an error state.
    return nullptr;
}

// Returns JNI_FALSE for the stub renderer — Kotlin's nativeAvailable() uses
// this to fall back to Media3's default SSA/ASS text decoder when no real
// LibASS implementation is linked in.
extern "C" JNIEXPORT jboolean JNICALL
Java_tv_cinepilot_tv_subtitles_LibassSubtitleDecoder_nativeHasAssRenderer(
    JNIEnv* /*env*/, jclass /*cls*/) {
    return JNI_FALSE;
}

jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    LOGD("cinepilot_subs JNI_OnLoad — stub renderer loaded");
    return JNI_VERSION_1_6;
}
