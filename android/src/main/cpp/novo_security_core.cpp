#include <jni.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

namespace {

// Standalone FIPS 180-4 SHA-256 & RFC 2104 HMAC-SHA256 inside native .so
static const uint32_t K256[64] = {
    0x428a2f98UL, 0x71374491UL, 0xb5c0fbcfUL, 0xe9b5dba5UL,
    0x3956c25bUL, 0x59f111f1UL, 0x923f82a4UL, 0xab1c5ed5UL,
    0xd807aa98UL, 0x12835b01UL, 0x243185beUL, 0x550c7dc3UL,
    0x72be5d74UL, 0x80deb1feUL, 0x9bdc06a7UL, 0xc19bf174UL,
    0xe49b69c1UL, 0xefbe4786UL, 0x0fc19dc6UL, 0x240ca1ccUL,
    0x2de92c6fUL, 0x4a7484aaUL, 0x5cb0a9dcUL, 0x76f988daUL,
    0x983e5152UL, 0xa831c66dUL, 0xb00327c8UL, 0xbf597fc7UL,
    0xc6e00bf3UL, 0xd5a79147UL, 0x06ca6351UL, 0x14292967UL,
    0x27b70a85UL, 0x2e1b2138UL, 0x4d2c6dfcUL, 0x53380d13UL,
    0x650a7354UL, 0x766a0abbUL, 0x81c2c92eUL, 0x92722c85UL,
    0xa2bfe8a1UL, 0xa81a664bUL, 0xc24b8b70UL, 0xc76c51a3UL,
    0xd192e819UL, 0xd6990624UL, 0xf40e3585UL, 0x106aa070UL,
    0x19a4c116UL, 0x1e376c08UL, 0x2748774cUL, 0x34b0bcb5UL,
    0x391c0cb3UL, 0x4ed8aa4aUL, 0x5b9cca4fUL, 0x682e6ff3UL,
    0x748f82eeUL, 0x78a5636fUL, 0x84c87814UL, 0x8cc70208UL,
    0x90befffaUL, 0xa4506cebUL, 0xbef9a3f7UL, 0xc67178f2UL
};

inline uint32_t rotr(uint32_t x, uint32_t n) {
    return (x >> n) | (x << (32 - n));
}

struct Sha256Ctx {
    uint32_t state[8];
    uint64_t bitlen;
    uint8_t data[64];
    size_t datalen;
};

void sha256_transform(Sha256Ctx* ctx, const uint8_t data[64]) {
    uint32_t m[64];
    for (int i = 0, j = 0; i < 16; ++i, j += 4) {
        m[i] = (static_cast<uint32_t>(data[j]) << 24) |
               (static_cast<uint32_t>(data[j + 1]) << 16) |
               (static_cast<uint32_t>(data[j + 2]) << 8) |
               (static_cast<uint32_t>(data[j + 3]));
    }
    for (int i = 16; i < 64; ++i) {
        uint32_t s0 = rotr(m[i - 15], 7) ^ rotr(m[i - 15], 18) ^ (m[i - 15] >> 3);
        uint32_t s1 = rotr(m[i - 2], 17) ^ rotr(m[i - 2], 19) ^ (m[i - 2] >> 10);
        m[i] = m[i - 16] + s0 + m[i - 7] + s1;
    }
    uint32_t a = ctx->state[0];
    uint32_t b = ctx->state[1];
    uint32_t c = ctx->state[2];
    uint32_t d = ctx->state[3];
    uint32_t e = ctx->state[4];
    uint32_t f = ctx->state[5];
    uint32_t g = ctx->state[6];
    uint32_t h = ctx->state[7];

    for (int i = 0; i < 64; ++i) {
        uint32_t S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
        uint32_t ch = (e & f) ^ ((~e) & g);
        uint32_t temp1 = h + S1 + ch + K256[i] + m[i];
        uint32_t S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
        uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
        uint32_t temp2 = S0 + maj;

        h = g;
        g = f;
        f = e;
        e = d + temp1;
        d = c;
        c = b;
        b = a;
        a = temp1 + temp2;
    }

    ctx->state[0] += a;
    ctx->state[1] += b;
    ctx->state[2] += c;
    ctx->state[3] += d;
    ctx->state[4] += e;
    ctx->state[5] += f;
    ctx->state[6] += g;
    ctx->state[7] += h;
}

void sha256_init(Sha256Ctx* ctx) {
    ctx->datalen = 0;
    ctx->bitlen = 0;
    ctx->state[0] = 0x6a09e667UL;
    ctx->state[1] = 0xbb67ae85UL;
    ctx->state[2] = 0x3c6ef372UL;
    ctx->state[3] = 0xa54ff53aUL;
    ctx->state[4] = 0x510e527fUL;
    ctx->state[5] = 0x9b05688cUL;
    ctx->state[6] = 0x1f83d9abUL;
    ctx->state[7] = 0x5be0cd19UL;
}

void sha256_update(Sha256Ctx* ctx, const uint8_t* data, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        ctx->data[ctx->datalen++] = data[i];
        if (ctx->datalen == 64) {
            sha256_transform(ctx, ctx->data);
            ctx->bitlen += 512;
            ctx->datalen = 0;
        }
    }
}

void sha256_final(Sha256Ctx* ctx, uint8_t hash[32]) {
    size_t i = ctx->datalen;
    if (ctx->datalen < 56) {
        ctx->data[i++] = 0x80;
        while (i < 56) ctx->data[i++] = 0x00;
    } else {
        ctx->data[i++] = 0x80;
        while (i < 64) ctx->data[i++] = 0x00;
        sha256_transform(ctx, ctx->data);
        std::memset(ctx->data, 0, 56);
    }
    ctx->bitlen += ctx->datalen * 8;
    ctx->data[63] = static_cast<uint8_t>(ctx->bitlen);
    ctx->data[62] = static_cast<uint8_t>(ctx->bitlen >> 8);
    ctx->data[61] = static_cast<uint8_t>(ctx->bitlen >> 16);
    ctx->data[60] = static_cast<uint8_t>(ctx->bitlen >> 24);
    ctx->data[59] = static_cast<uint8_t>(ctx->bitlen >> 32);
    ctx->data[58] = static_cast<uint8_t>(ctx->bitlen >> 40);
    ctx->data[57] = static_cast<uint8_t>(ctx->bitlen >> 48);
    ctx->data[56] = static_cast<uint8_t>(ctx->bitlen >> 56);
    sha256_transform(ctx, ctx->data);

    for (i = 0; i < 4; ++i) {
        hash[i]      = (ctx->state[0] >> (24 - i * 8)) & 0xff;
        hash[i + 4]  = (ctx->state[1] >> (24 - i * 8)) & 0xff;
        hash[i + 8]  = (ctx->state[2] >> (24 - i * 8)) & 0xff;
        hash[i + 12] = (ctx->state[3] >> (24 - i * 8)) & 0xff;
        hash[i + 16] = (ctx->state[4] >> (24 - i * 8)) & 0xff;
        hash[i + 20] = (ctx->state[5] >> (24 - i * 8)) & 0xff;
        hash[i + 24] = (ctx->state[6] >> (24 - i * 8)) & 0xff;
        hash[i + 28] = (ctx->state[7] >> (24 - i * 8)) & 0xff;
    }
}

void hmac_sha256(
    const uint8_t* key,
    size_t key_len,
    const uint8_t* msg,
    size_t msg_len,
    uint8_t out[32]
) {
    uint8_t k_block[64];
    std::memset(k_block, 0, sizeof(k_block));
    if (key_len > 64) {
        Sha256Ctx kctx;
        sha256_init(&kctx);
        sha256_update(&kctx, key, key_len);
        sha256_final(&kctx, k_block);
    } else {
        std::memcpy(k_block, key, key_len);
    }

    uint8_t ipad[64];
    uint8_t opad[64];
    for (int i = 0; i < 64; ++i) {
        ipad[i] = k_block[i] ^ 0x36;
        opad[i] = k_block[i] ^ 0x5c;
    }

    uint8_t inner_hash[32];
    Sha256Ctx ictx;
    sha256_init(&ictx);
    sha256_update(&ictx, ipad, 64);
    sha256_update(&ictx, msg, msg_len);
    sha256_final(&ictx, inner_hash);

    Sha256Ctx octx;
    sha256_init(&octx);
    sha256_update(&octx, opad, 64);
    sha256_update(&octx, inner_hash, 32);
    sha256_final(&octx, out);

    volatile uint8_t* wipe = k_block;
    for (size_t i = 0; i < sizeof(k_block); ++i) wipe[i] = 0;
}

static const uint8_t MASKED_SHIELD_KEY[32] = {
    0xE9, 0xC5, 0xCB, 0xEF, 0xD7, 0x94, 0x80, 0x8A,
    0xAA, 0xE8, 0x1F, 0x79, 0x4E, 0x6F, 0x7F, 0x10,
    0x02, 0x17, 0x1E, 0x38, 0xCA, 0xD3, 0xD5, 0xE8,
    0xA5, 0xD8, 0x83, 0xCD, 0xE5, 0xEE, 0x00, 0x15
};

void unmask_shield_key(uint8_t out[32]) {
    for (int i = 0; i < 32; ++i) {
        uint8_t mask = static_cast<uint8_t>(0xA7 ^ ((i * 13) & 0xFF));
        out[i] = MASKED_SHIELD_KEY[i] ^ mask;
    }
}

void wipe_key(uint8_t buf[32]) {
    volatile uint8_t* p = buf;
    for (int i = 0; i < 32; ++i) p[i] = 0;
}

std::string to_hex(const uint8_t* data, size_t len) {
    static const char HEX[] = "0123456789abcdef";
    std::string s;
    s.reserve(len * 2);
    for (size_t i = 0; i < len; ++i) {
        s.push_back(HEX[(data[i] >> 4) & 0x0F]);
        s.push_back(HEX[data[i] & 0x0F]);
    }
    return s;
}

bool constant_time_eq(const std::string& a, const std::string& b) {
    if (a.size() != b.size()) return false;
    uint8_t diff = 0;
    for (size_t i = 0; i < a.size(); ++i) {
        char ca = (a[i] >= 'A' && a[i] <= 'F') ? static_cast<char>(a[i] + 32) : a[i];
        char cb = (b[i] >= 'A' && b[i] <= 'F') ? static_cast<char>(b[i] + 32) : b[i];
        diff |= static_cast<uint8_t>(ca ^ cb);
    }
    return diff == 0;
}

} // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeMaxFamilyMembers(
    JNIEnv* /*env*/,
    jclass /*clazz*/
) {
    return 3;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeMaxFamilyFileBytes(
    JNIEnv* /*env*/,
    jclass /*clazz*/
) {
    // Hardcoded 4000 * 512 KiB (~2 GB) single-file protocol cap for family members
    return 4000LL * 512LL * 1024LL;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeSignFamilyGrant(
    JNIEnv* env,
    jclass /*clazz*/,
    jstring payload
) {
    if (payload == nullptr) return env->NewStringUTF("");
    const char* utf = env->GetStringUTFChars(payload, nullptr);
    if (utf == nullptr) return env->NewStringUTF("");
    size_t len = std::strlen(utf);

    uint8_t key[32];
    uint8_t mac[32];
    unmask_shield_key(key);
    hmac_sha256(key, 32, reinterpret_cast<const uint8_t*>(utf), len, mac);
    wipe_key(key);
    env->ReleaseStringUTFChars(payload, utf);

    std::string hex = to_hex(mac, 32);
    return env->NewStringUTF(hex.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeVerifyFamilyGrant(
    JNIEnv* env,
    jclass /*clazz*/,
    jstring payload,
    jstring signatureHex
) {
    if (payload == nullptr || signatureHex == nullptr) return JNI_FALSE;
    const char* pUtf = env->GetStringUTFChars(payload, nullptr);
    const char* sUtf = env->GetStringUTFChars(signatureHex, nullptr);
    if (pUtf == nullptr || sUtf == nullptr) {
        if (pUtf) env->ReleaseStringUTFChars(payload, pUtf);
        if (sUtf) env->ReleaseStringUTFChars(signatureHex, sUtf);
        return JNI_FALSE;
    }

    uint8_t key[32];
    uint8_t mac[32];
    unmask_shield_key(key);
    hmac_sha256(key, 32, reinterpret_cast<const uint8_t*>(pUtf), std::strlen(pUtf), mac);
    wipe_key(key);

    std::string expected = to_hex(mac, 32);
    std::string provided(sUtf);

    env->ReleaseStringUTFChars(payload, pUtf);
    env->ReleaseStringUTFChars(signatureHex, sUtf);

    return constant_time_eq(expected, provided) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeDeriveFamilySubKey(
    JNIEnv* env,
    jclass /*clazz*/,
    jstring ownerSeed,
    jint slotIndex,
    jlong channelChatId
) {
    if (slotIndex < 1 || slotIndex > 3 || ownerSeed == nullptr) {
        return env->NewByteArray(0);
    }
    const char* seedUtf = env->GetStringUTFChars(ownerSeed, nullptr);
    if (seedUtf == nullptr) return env->NewByteArray(0);

    std::string info = std::string("novo-family-subkey|") + seedUtf +
                       "|slot=" + std::to_string(slotIndex) +
                       "|chat=" + std::to_string(channelChatId);
    env->ReleaseStringUTFChars(ownerSeed, seedUtf);

    uint8_t key[32];
    uint8_t derived[32];
    unmask_shield_key(key);
    hmac_sha256(
        key,
        32,
        reinterpret_cast<const uint8_t*>(info.data()),
        info.size(),
        derived
    );
    wipe_key(key);

    jbyteArray result = env->NewByteArray(32);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<const jbyte*>(derived));
    }
    wipe_key(derived);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeVerifyApkIntegrity(
    JNIEnv* env,
    jclass /*clazz*/,
    jobject context
) {
    if (context == nullptr) return JNI_FALSE;
    jclass ctxClass = env->GetObjectClass(context);
    if (ctxClass == nullptr) return JNI_FALSE;

    jmethodID getPkgName = env->GetMethodID(ctxClass, "getPackageName", "()Ljava/lang/String;");
    if (getPkgName == nullptr) return JNI_FALSE;

    auto pkgStr = static_cast<jstring>(env->CallObjectMethod(context, getPkgName));
    if (pkgStr == nullptr) return JNI_FALSE;

    const char* pkgUtf = env->GetStringUTFChars(pkgStr, nullptr);
    if (pkgUtf == nullptr) return JNI_FALSE;
    bool validPkg = (std::strcmp(pkgUtf, "com.novo.drive") == 0);
    env->ReleaseStringUTFChars(pkgStr, pkgUtf);
    if (!validPkg) return JNI_FALSE;

    // Verify APK has a valid signing certificate attached via PackageManager
    jmethodID getPm = env->GetMethodID(
        ctxClass,
        "getPackageManager",
        "()Landroid/content/pm/PackageManager;"
    );
    if (getPm == nullptr) return JNI_FALSE;
    jobject pm = env->CallObjectMethod(context, getPm);
    if (pm == nullptr) return JNI_FALSE;

    jclass pmClass = env->GetObjectClass(pm);
    jmethodID getPkgInfo = env->GetMethodID(
        pmClass,
        "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"
    );
    if (getPkgInfo == nullptr) return JNI_FALSE;

    // GET_SIGNATURES = 64 works across API 26..37 for presence & non-empty X.509 cert digest check
    jobject pkgInfo = env->CallObjectMethod(pm, getPkgInfo, pkgStr, 64);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return JNI_FALSE;
    }
    if (pkgInfo == nullptr) return JNI_FALSE;

    jclass piClass = env->GetObjectClass(pkgInfo);
    jfieldID sigsField = env->GetFieldID(
        piClass,
        "signatures",
        "[Landroid/content/pm/Signature;"
    );
    if (sigsField == nullptr) return JNI_FALSE;

    auto sigsArray = static_cast<jobjectArray>(env->GetObjectField(pkgInfo, sigsField));
    if (sigsArray == nullptr || env->GetArrayLength(sigsArray) <= 0) return JNI_FALSE;

    jobject firstSig = env->GetObjectArrayElement(sigsArray, 0);
    if (firstSig == nullptr) return JNI_FALSE;

    jclass sigClass = env->GetObjectClass(firstSig);
    jmethodID toByteArray = env->GetMethodID(sigClass, "toByteArray", "()[B");
    if (toByteArray == nullptr) return JNI_FALSE;

    auto certBytes = static_cast<jbyteArray>(env->CallObjectMethod(firstSig, toByteArray));
    if (certBytes == nullptr || env->GetArrayLength(certBytes) < 64) return JNI_FALSE;

    // Cryptographic Certificate SHA-256 Pinning
    Sha256Ctx certSha;
    sha256_init(&certSha);
    jbyte* rawCert = env->GetByteArrayElements(certBytes, nullptr);
    if (rawCert == nullptr) return JNI_FALSE;
    sha256_update(&certSha, reinterpret_cast<const uint8_t*>(rawCert), static_cast<size_t>(env->GetArrayLength(certBytes)));
    uint8_t certDigest[32];
    sha256_final(&certSha, certDigest);
    env->ReleaseByteArrayElements(certBytes, rawCert, JNI_ABORT);

    // Official Kunjo Release Key Fingerprint
    // SHA-256: 6E:4D:C6:C0:E5:1B:CA:89:6A:42:15:87:F4:35:3A:E2:77:B8:29:59:49:FC:2F:F6:D3:ED:9B:6F:91:BC:4B:9D
    static const uint8_t KUNJO_RELEASE_FINGERPRINT[32] = {
        0x6E, 0x4D, 0xC6, 0xC0, 0xE5, 0x1B, 0xCA, 0x89,
        0x6A, 0x42, 0x15, 0x87, 0xF4, 0x35, 0x3A, 0xE2,
        0x77, 0xB8, 0x29, 0x59, 0x49, 0xFC, 0x2F, 0xF6,
        0xD3, 0xED, 0x9B, 0x6F, 0x91, 0xBC, 0x4B, 0x9D
    };

    uint8_t diff = 0;
    for (int i = 0; i < 32; ++i) {
        diff |= static_cast<uint8_t>(certDigest[i] ^ KUNJO_RELEASE_FINGERPRINT[i]);
    }
    return (diff == 0) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeConstantTimeEquals(
    JNIEnv* env,
    jclass /*clazz*/,
    jbyteArray a,
    jbyteArray b
) {
    if (a == nullptr || b == nullptr) return JNI_FALSE;
    jsize lenA = env->GetArrayLength(a);
    jsize lenB = env->GetArrayLength(b);
    if (lenA != lenB) return JNI_FALSE;

    jbyte* bytesA = env->GetByteArrayElements(a, nullptr);
    jbyte* bytesB = env->GetByteArrayElements(b, nullptr);
    if (bytesA == nullptr || bytesB == nullptr) {
        if (bytesA) env->ReleaseByteArrayElements(a, bytesA, JNI_ABORT);
        if (bytesB) env->ReleaseByteArrayElements(b, bytesB, JNI_ABORT);
        return JNI_FALSE;
    }

    volatile uint8_t diff = 0;
    for (jsize i = 0; i < lenA; ++i) {
        diff |= static_cast<uint8_t>(bytesA[i] ^ bytesB[i]);
    }

    env->ReleaseByteArrayElements(a, bytesA, JNI_ABORT);
    env->ReleaseByteArrayElements(b, bytesB, JNI_ABORT);

    return (diff == 0) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_novo_drive_core_security_AndroidNativeCoreJni_nativeWipeByteArray(
    JNIEnv* env,
    jclass /*clazz*/,
    jbyteArray target
) {
    if (target == nullptr) return;
    jsize len = env->GetArrayLength(target);
    jbyte* bytes = env->GetByteArrayElements(target, nullptr);
    if (bytes != nullptr) {
        volatile uint8_t* p = reinterpret_cast<volatile uint8_t*>(bytes);
        for (jsize i = 0; i < len; ++i) {
            p[i] = 0;
        }
        env->ReleaseByteArrayElements(target, bytes, 0);
    }
}

