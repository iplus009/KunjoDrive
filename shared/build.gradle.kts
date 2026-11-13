import java.io.File
import java.util.Properties

// Generate Secrets.kt from the gitignored root .env file so credentials never live in source.
// See .env.example for the keys: KUNJO_BOT_TOKEN, KUNJO_ADMIN_CHAT_ID, KUNJO_SUPABASE_URL, KUNJO_SUPABASE_KEY.
// Note: everything here is compiled into the APK — only client-safe values belong here.
val generateSecrets = tasks.register("generateSecrets") {
    val outDir = layout.buildDirectory.dir("generated/secrets-kotlin")
    val appVersionValue = libs.versions.appVersion.get()
    outputs.dir(outDir)
    // No declared inputs, so force a rerun; otherwise an edited .env would be ignored.
    outputs.upToDateWhen { false }
    doLast {
        val props = Properties()
        val envFile: File = rootProject.file(".env")
        if (envFile.exists()) envFile.inputStream().use { props.load(it) }

        fun prop(key: String): String = System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: props.getProperty(key, "").trim()

        val dir: File = outDir.get().asFile
        dir.mkdirs()
        File(dir, "Secrets.kt").writeText(
            """package com.novo.drive.core.network

internal object Secrets {
    const val BOT_TOKEN: String = "${prop("KUNJO_BOT_TOKEN")}"
    const val ADMIN_CHAT_ID: String = "${prop("KUNJO_ADMIN_CHAT_ID")}"
    const val SUPABASE_URL: String = "${prop("KUNJO_SUPABASE_URL")}"
    const val SUPABASE_KEY: String = "${prop("KUNJO_SUPABASE_KEY")}"
    const val APP_VERSION: String = "v$appVersionValue"
}
"""
        )
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.novo.drive.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                api(libs.androidx.paging.common)
                api(libs.androidx.room.runtime)
                api(libs.androidx.room.paging)
                api(libs.androidx.sqlite.bundled)
                api(libs.androidx.datastore.preferences.core)
            }
        }
        val jvmCommonMain = create("jvmCommonMain") {
            dependsOn(commonMain.get())
        }
        val jvmCommonTest = create("jvmCommonTest") {
            dependsOn(commonTest.get())
        }
        named("androidMain") {
            dependsOn(jvmCommonMain)
        }
        named("desktopMain") {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.tdlight.java)
                // Pick the native for the host so cross-platform packages do not
                // carry a second, unused native set (Windows: Linux natives, and
                // vice versa). TDLight loads the matching library at runtime.
                // TDLight publishes macos_arm64 only, so Intel Macs have no build.
                val hostOs = System.getProperty("os.name").lowercase()
                val nativesClassifier = when {
                    hostOs.contains("win") -> "windows_amd64"
                    hostOs.contains("mac") -> "macos_arm64"
                    else -> "linux_amd64_gnu_ssl3"
                }
                implementation("it.tdlight:tdlight-natives:${libs.versions.tdlightNatives.get()}:$nativesClassifier")
            }
        }
        named("desktopTest") {
            dependsOn(jvmCommonTest)
            dependencies {
                implementation(libs.junit)
            }
        }
        named("jvmCommonMain") {
            kotlin.srcDir("build/generated/secrets-kotlin")
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.matching {
    it.name in listOf("compileAndroidMain", "compileKotlinDesktop", "compileJvmCommonMainKotlinMetadata", "kspAndroidMain", "kspDesktopMain", "kspKotlinDesktop")
}.configureEach {
    dependsOn(generateSecrets)
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}
