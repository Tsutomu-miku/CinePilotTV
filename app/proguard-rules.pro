# ============================================================================
# CinePilot TV release ProGuard / R8 rules.
#
# Core constraints:
#   - Pure-Java :core module uses zero reflection for business logic, but
#     Android system APIs cross JNI boundaries (MediaCodec, Media3 PlayerView)
#     that require enumerating codec/mime types via string names.
#   - All protocol data records (MediaItemSummary, PlaybackReport, HomeRow…)
#     are accessed directly with getters, but some internal state machine
#     code in TvWorkflowController casts Object through <?> Map arrays that
#     JsonValue produces; those are pure Java and are R8-safe.
#   - Media3 ships its own consumer ProGuard rules; we only add edges for
#     the custom RemotePlayerView subclass and MediaCodec-based playback.
# ============================================================================

# --- Keep all model records used across the protocol boundary ---------------
# R8 is usually safe with public record accessors, but these types cross
# the workflow ↔ UI boundary and tests instantiate them via reflective
# constructor invocation in some QA paths. Keep constructors + class names.
-keep class tv.cinepilot.core.protocol.** { *; }
-keep class tv.cinepilot.core.tv.** { *; }
-keep class tv.cinepilot.core.AndroidCollections { *; }

# --- No reflection on JSON parser; keep package private JsonValue fields -----
# JsonValue's Parser class is package-private but accessed by other classes
# in the same package so R8 usually keeps them. Be explicit.
-keep class tv.cinepilot.core.protocol.JsonValue { *; }
-keep class tv.cinepilot.core.protocol.JsonValue$* { *; }
-keep class tv.cinepilot.core.protocol.HomeRowsSerializer { *; }
-keep class tv.cinepilot.core.protocol.MediaBrowserResponseMapper { *; }

# --- Android Collections / framework boundary --------------------------------
# Android N's desugaring + our Java 8 compat layer: keep the helper that
# wraps Arrays/Collections calls because call sites are widely inlined into
# records. Consumer rules from desugar_jdk_libs handle the rest.
-keep class tv.cinepilot.core.AndroidCollections { *; }

# --- Media3 / ExoPlayer ------------------------------------------------------
# Media3 ships consumer ProGuard rules, but we keep our subclass names since
# Media3 PlayerView and ExoPlayer internals look them up via reflection for
# track selection and view inflation in some flows.
-keep class tv.cinepilot.tv.player.** { *; }
-keep class * extends androidx.media3.common.Player
-keep class * extends androidx.media3.ui.PlayerView

# --- MediaCodec / device codec diagnostics -----------------------------------
# DeviceCodecDiagnostics enumerates mime-type strings via reflection on
# MediaCodecList; R8's class-name removal would break codec detection if it
# optimizes string constants (unlikely but belt-and-suspenders).
-keep class android.media.MediaCodecList
-keep class android.media.MediaCodecInfo { *; }
-keep class android.media.MediaCodecInfo$CodecCapabilities { *; }
-keepnames class tv.cinepilot.tv.runtime.DeviceCodecDiagnostics { *; }
-keep class tv.cinepilot.core.protocol.PlaybackDeviceProfile { *; }
-keep class tv.cinepilot.core.protocol.PlaybackSourceSelector { *; }
-keep class tv.cinepilot.core.protocol.PlaybackSubtitleDelivery { *; }

# --- Kotlin / data classes ---------------------------------------------------
# Kotlin data classes and record-like classes accessed by the Gradle test
# harness from :core (Robolectric tests do reflection on ViewModel factory).
-keep class tv.cinepilot.tv.CinePilotViewModel$Companion { *; }

# --- XML / Android view bindings (we don't use XML but ensure AppCompatActivity survives)
-keep class androidx.activity.ComponentActivity { *; }
-keep class tv.cinepilot.tv.MainActivity { *; }

# --- Subtitle style store / shared preferences --------------------------------
# SharedPreferences string-sets are fine but the Kotlin data objects that
# wrap them must survive.
-keep class tv.cinepilot.tv.playback.SubtitleStyleStore { *; }
-keep class tv.cinepilot.tv.settings.SettingsStore { *; }

# --- BitmapCache disk LRU uses Files/NIO and SHA-256 name hashing -------------
# FileHomeRowsCache is in :core and uses Java NIO; keep class names so
# stack traces are readable in crash reports on user devices.
-keepnames class tv.cinepilot.tv.runtime.BitmapCache { *; }
-keepnames class tv.cinepilot.tv.runtime.PrimaryImageLoader { *; }
-keepnames class tv.cinepilot.tv.runtime.ArtworkLoader { *; }
-keep class tv.cinepilot.core.tv.FileHomeRowsCache { *; }

# --- RecentAccountStore / Quick Connect --------------------------------------
-keep class tv.cinepilot.tv.runtime.RecentAccountStore { *; }
-keep class tv.cinepilot.tv.runtime.QuickConnectPoller { *; }
-keep class tv.cinepilot.core.protocol.QuickConnectSession { *; }

# --- Saved sessions / property files -----------------------------------------
# FileSessionRepository uses java.util.Properties; keep so saved tokens remain
# re-loadable after an app upgrade.
-keep class tv.cinepilot.core.protocol.FileSessionRepository { *; }
-keep class tv.cinepilot.core.protocol.SavedSession { *; }
-keep class tv.cinepilot.core.protocol.AuthenticatedServer { *; }

# --- LruCache (used by BitmapCache) ------------------------------------------
# Rename-safe, but keep the class literal used when debugging heap dumps.
-keepnames class tv.cinepilot.tv.runtime.BitmapCache$* { *; }

# --- Standard Android + Media3 common rules ----------------------------------
-dontwarn androidx.media3.**
-dontwarn org.checkerframework.checker.nullness.qual.**
-dontwarn org.jspecify.annotations.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Useful: keep line number tables for readable Play Store crashes ---------
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
