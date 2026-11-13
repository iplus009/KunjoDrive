import com.android.build.api.variant.FilterConfiguration
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.novo.drive"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.novo.drive"
        minSdk = 26
        targetSdk = 37
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersion.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
        }
    }

    val splitAbis = providers.gradleProperty("splitAbis").orNull?.toBoolean() ?: false
    splits {
        abi {
            isEnable = splitAbis
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = splitAbis
        }
    }

    val officialReleaseKeystore = rootProject.file("certs/kunjo-release.jks")
    val envProps = Properties().apply {
        val f = rootProject.file(".env")
        if (f.exists()) {
            FileInputStream(f).use { load(it) }
        }
    }
    val resolvedStorePassword = System.getenv("KEYSTORE_PASSWORD")
        ?: envProps.getProperty("KEYSTORE_PASSWORD")
        ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)

    val resolvedKeyPassword = System.getenv("KEY_PASSWORD")
        ?: envProps.getProperty("KEY_PASSWORD")
        ?: resolvedStorePassword

    val resolvedKeyAlias = System.getenv("KEY_ALIAS")
        ?: envProps.getProperty("KEY_ALIAS")
        ?: "kunjo"

    signingConfigs {
        create("release") {
            if (officialReleaseKeystore.exists()) {
                storeFile = officialReleaseKeystore
                storePassword = resolvedStorePassword ?: error("KEYSTORE_PASSWORD is not set in env or .env")
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword ?: error("KEY_PASSWORD is not set in env or .env")
                enableV1Signing = true
                enableV2Signing = true
            } else {
                error("Missing release keystore at certs/kunjo-release.jks")
            }
        }
    }
    buildTypes {
        debug {
            signingConfig = if (officialReleaseKeystore.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?: "universal"
            val extension = output.outputFileName.get().substringAfterLast('.')
            output.outputFileName.set(
                output.versionName.map { version ->
                    "DewanGram-KunjoDrive-v$version-$abi-${variant.buildType}.$extension"
                }
            )
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":ui"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.google.android.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.workmanager)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    if (providers.gradleProperty("withFfmpegDecoder").isPresent) {
        implementation(libs.androidx.media3.decoder.ffmpeg)
    }

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.telephoto.zoomable.coil)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.exifinterface)
    implementation(libs.zip4j)
    implementation(libs.zxing.core)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.tdlib)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
