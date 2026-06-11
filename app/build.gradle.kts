plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
}

android {
    namespace = "tv.cinepilot.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "tv.cinepilot.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    // Native subtitle decoders (LibASS / PGS) are only built when the Android NDK
    // is available on the build machine. Missing NDK falls back to the Java default
    // subtitle path; the native wrappers guard System.loadLibrary with try/catch.
    val ndkDir = (findProperty("android.ndkDirectory") as? String)
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: ""
    val ndkAvailable = ndkDir.isNotBlank() && file(ndkDir).isDirectory
    if (ndkAvailable) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
        defaultConfig {
            externalNativeBuild {
                cmake {
                    cppFlags += "-std=c++17 -fexceptions"
                    abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                    arguments += listOf("-DANDROID_STL=c++_shared")
                }
            }
        }
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
        ndkVersion = "27.0.12077973"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        abortOnError = true
        disable += setOf(
            "MissingTranslation",
            "HardcodedText",
            "ContentDescription",
            "UseSwitchCompatOrMaterialCode",
            "IconDuplicatesConfig",
            "UnsafeOptInUsageError",
        )
        enable += setOf(
            "NewApi",
            "SyntheticAccessor",
            "LintError",
        )
        fatal += setOf("NewApi")
        checkReleaseBuilds = false
        checkTestSources = false
        checkGeneratedSources = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":plugin-spi"))
    runtimeOnly(project(":plugins:bangumi"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.workmanager)
    implementation(libs.juniversalchardet)
    implementation(libs.androidx.tvprovider)
    implementation(libs.androidx.work.runtime.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(project(":core"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.7")
}

detekt {
    buildUponDefaultConfig = false
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        md.required.set(true)
        txt.required.set(true)
    }
}
