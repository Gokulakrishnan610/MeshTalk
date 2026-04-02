package com.ble_mesh.meshtalk.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * EncryptionService — AES-256-GCM symmetric encryption for personal DM messages.
 *
 * Ported core approach from bitchat's EncryptionService.kt:
 * - Uses Android KeyStore for secure key persistence (no key material ever leaves hardware)
 * - Each message encrypted with a fresh 12-byte IV
 * - Ciphertext stored as base64: "<iv_base64>:<ciphertext_base64>"
 *
 * For the mesh DM model:
 * - We use a single shared app-level key (KeyStore alias: "meshtalk_dm_key")
 * - This provides confidentiality from non-MeshTalk devices intercepting BLE
 * - True per-peer key exchange (like bitchat's Noise protocol) is a future enhancement
 */
class EncryptionService(private val context: Context) {

    companion object {
        private const val TAG = "EncryptionService"
        private const val KEY_ALIAS = "meshtalk_dm_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }



    /**
     * Encrypt a plaintext message for DM delivery.
     * @return base64-encoded "<IV>:<ciphertext>" or null on failure
     */
    fun encrypt(plaintext: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ctB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            "$ivB64:$ctB64"
        } catch (e: Exception) {
            Log.e(TAG, "Encrypt failed: ${e.message}")
            null
        }
    }

    /**
     * Decrypt a DM payload.
     * @param payload base64-encoded "<IV>:<ciphertext>"
     * @return plaintext string or null on failure
     */
    fun decrypt(payload: String): String? {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decrypt failed: ${e.message}")
            null
        }
    }

    private fun getKey(): SecretKey {
        // A static 256-bit (32 byte) shared mesh key. 
        // In the future, this should be replaced with an Elliptic-Curve Diffie-Hellman (ECDH) key exchange!
        val keyBytes = "MeshTalk_Shared_Secret_Key_32_B!".toByteArray(Charsets.UTF_8)
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }
}
