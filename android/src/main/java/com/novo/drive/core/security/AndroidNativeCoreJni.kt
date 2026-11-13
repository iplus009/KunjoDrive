package com.novo.drive.core.security

import android.content.Context

object AndroidNativeCoreJni {

    private val loaded: Boolean = runCatching {
        System.loadLibrary("novo_core")
        true
    }.getOrDefault(false)

    @JvmStatic
    external fun nativeMaxFamilyMembers(): Int

    @JvmStatic
    external fun nativeMaxFamilyFileBytes(): Long

    @JvmStatic
    external fun nativeSignFamilyGrant(payload: String): String

    @JvmStatic
    external fun nativeVerifyFamilyGrant(payload: String, signatureHex: String): Boolean

    @JvmStatic
    external fun nativeDeriveFamilySubKey(
        ownerSeed: String,
        slotIndex: Int,
        channelChatId: Long
    ): ByteArray

    @JvmStatic
    external fun nativeVerifyApkIntegrity(context: Context): Boolean

    @JvmStatic
    external fun nativeConstantTimeEquals(a: ByteArray, b: ByteArray): Boolean

    @JvmStatic
    external fun nativeWipeByteArray(target: ByteArray)

    fun verifyAppIntegrity(appContext: Context): Boolean {
        if (com.novo.drive.BuildConfig.DEBUG) return true
        val nativeOk = runCatching { nativeVerifyApkIntegrity(appContext) }.getOrDefault(false)
        if (nativeOk) return true

        // Fallback: Modern Android API 28+ (Android 9 to 15) verification using signingInfo
        return runCatching {
            val pm = appContext.packageManager
            val pkgName = appContext.packageName
            val targetSha256 = "6e4dc6c0e51bca896a421587f4353ae277b8295949fc2ff6d3ed9b6f91bc4b9d"
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkgName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo ?: return false
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(pkgName, android.content.pm.PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures
            }
            if (signatures.isNullOrEmpty()) return false
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val certDigest = md.digest(signatures[0].toByteArray())
            val hex = certDigest.joinToString("") { "%02x".format(it) }
            hex.equals(targetSha256, ignoreCase = true)
        }.getOrDefault(false)
    }

    fun install(appContext: Context) {
        val integrityOk = verifyAppIntegrity(appContext)
        com.novo.drive.core.common.SafeLog.d("AndroidNativeCoreJni", "Runtime integrity verified: $integrityOk")
        if (!loaded) {
            NativeSecurityBridge.installDelegate(
                object : NativeSecurityDelegate by NativeSecurityBridge.defaultFallback {
                    override fun isRuntimeIntegrityValid(): Boolean = integrityOk
                }
            )
            return
        }
        NativeSecurityBridge.installDelegate(
            object : NativeSecurityDelegate {
                override fun maxFamilyMembers(): Int = runCatching { nativeMaxFamilyMembers() }.getOrDefault(3)
                override fun maxFamilyFileBytes(): Long = runCatching { nativeMaxFamilyFileBytes() }.getOrDefault(2000L * 1024L * 1024L)
                override fun signFamilyGrant(canonicalPayload: String): String =
                    if (integrityOk) runCatching { nativeSignFamilyGrant(canonicalPayload) }.getOrDefault("") else ""

                override fun verifyFamilyGrant(
                    canonicalPayload: String,
                    signatureHex: String
                ): Boolean =
                    integrityOk && runCatching { nativeVerifyFamilyGrant(canonicalPayload, signatureHex) }.getOrDefault(false)

                override fun deriveFamilySubKey(
                    ownerSeed: String,
                    slotIndex: Int,
                    channelChatId: Long
                ): ByteArray =
                    if (integrityOk) {
                        runCatching { nativeDeriveFamilySubKey(ownerSeed, slotIndex, channelChatId) }.getOrDefault(ByteArray(0))
                    } else {
                        ByteArray(0)
                    }

                override fun isRuntimeIntegrityValid(): Boolean = integrityOk

                override fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
                    if (loaded) {
                        val nativeResult = runCatching { nativeConstantTimeEquals(a, b) }.getOrNull()
                        if (nativeResult != null) return nativeResult
                    }
                    if (a.size != b.size) return false
                    var diff = 0
                    for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
                    return diff == 0
                }

                override fun wipeByteArray(target: ByteArray) {
                    var wiped = false
                    if (loaded) {
                        wiped = runCatching {
                            nativeWipeByteArray(target)
                            true
                        }.getOrDefault(false)
                    }
                    if (!wiped) {
                        target.fill(0)
                    }
                }
            }
        )
    }
}
