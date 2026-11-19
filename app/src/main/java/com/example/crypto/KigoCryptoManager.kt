package com.example.crypto

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard JCA End-to-End Encryption manager using:
 * - ECDH on NIST P-256 (secp256r1) for key agreement
 * - HKDF-SHA256 (RFC 5869) for symmetric session key derivation
 * - AES-256-GCM with 96-bit random IV and 128-bit authentication tag
 */
class KigoCryptoManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun getOrCreateKeyPair(userId: String): KeyPair {
        val pubB64 = prefs.getString("${KEY_PUB_PREFIX}$userId", null)
        val privB64 = prefs.getString("${KEY_PRIV_PREFIX}$userId", null)
        if (!pubB64.isNullOrBlank() && !privB64.isNullOrBlank()) {
            return try {
                val kf = KeyFactory.getInstance("EC")
                val pub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubB64, Base64.NO_WRAP)))
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.NO_WRAP)))
                KeyPair(pub, priv)
            } catch (_: Exception) {
                generateAndStoreKeyPair(userId)
            }
        }
        return generateAndStoreKeyPair(userId)
    }

    fun getPublicKeyBase64(userId: String): String {
        val kp = getOrCreateKeyPair(userId)
        return Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
    }

    fun getKeyFingerprint(publicKeyBase64: String): String {
        if (publicKeyBase64.isBlank()) return "Not initialized"
        val bytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.take(16).chunked(2).joinToString(" ") { pair ->
            pair.joinToString("") { "%02X".format(it) }
        }
    }

    fun encryptDirectMessage(
        myUserId: String,
        peerPublicKeyBase64: String,
        conversationId: String,
        plaintext: String
    ): EncryptedPayload? {
        if (peerPublicKeyBase64.isBlank()) return null
        return try {
            val myKeyPair = getOrCreateKeyPair(myUserId)
            val peerPublicKey = decodePublicKey(peerPublicKeyBase64)
            val aesKey = deriveSharedAesKey(myKeyPair.private, peerPublicKey, conversationId)
            val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            EncryptedPayload(
                ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decryptDirectMessage(
        myUserId: String,
        peerPublicKeyBase64: String,
        conversationId: String,
        ciphertextBase64: String,
        ivBase64: String
    ): String? {
        if (peerPublicKeyBase64.isBlank() || ciphertextBase64.isBlank() || ivBase64.isBlank()) return null
        return try {
            val myKeyPair = getOrCreateKeyPair(myUserId)
            val peerPublicKey = decodePublicKey(peerPublicKeyBase64)
            val aesKey = deriveSharedAesKey(myKeyPair.private, peerPublicKey, conversationId)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(conversationId.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun generateAndStoreKeyPair(userId: String): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val kp = kpg.generateKeyPair()
        prefs.edit()
            .putString("${KEY_PUB_PREFIX}$userId", Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
            .putString("${KEY_PRIV_PREFIX}$userId", Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
            .apply()
        return kp
    }

    private fun decodePublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    }

    private fun deriveSharedAesKey(
        myPrivateKey: PrivateKey,
        peerPublicKey: PublicKey,
        conversationId: String
    ): SecretKey {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(myPrivateKey)
        agreement.doPhase(peerPublicKey, true)
        val sharedSecret = agreement.generateSecret()

        // HKDF-SHA256 Extract + Expand (RFC 5869)
        val salt = "KIGO-CHAT-E2EE-V1".toByteArray(Charsets.UTF_8)
        val prkMac = Mac.getInstance("HmacSHA256")
        prkMac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = prkMac.doFinal(sharedSecret)

        val info = "kigo-msg:$conversationId".toByteArray(Charsets.UTF_8)
        val expandMac = Mac.getInstance("HmacSHA256")
        expandMac.init(SecretKeySpec(prk, "HmacSHA256"))
        expandMac.update(info)
        expandMac.update(0x01.toByte())
        val okm = expandMac.doFinal()
        return SecretKeySpec(okm.copyOf(32), "AES")
    }

    data class EncryptedPayload(
        val ciphertextBase64: String,
        val ivBase64: String
    )

    private companion object {
        const val PREFS_NAME = "kigo_e2ee_keystore_prefs"
        const val KEY_PUB_PREFIX = "ecdh_pub_"
        const val KEY_PRIV_PREFIX = "ecdh_priv_"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
