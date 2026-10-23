package com.mizani.money.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates a random 256-bit database passphrase and wraps it with a non-exportable
 * Android Keystore key (AES-GCM). The passphrase itself never leaves the process in
 * plaintext on disk, and the Keystore key cannot be extracted from the device.
 */
object DatabaseKeyManager {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "mizani_database_key_v1"
    private const val PREFS = "mizani_database_key"
    private const val PREF_WRAPPED = "wrapped_key"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32
    private const val TAG = "MizaniKey"

    @Synchronized
    fun loadOrCreate(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_WRAPPED, null)
        if (stored != null) {
            return try {
                unwrap(stored)
            } catch (error: Exception) {
                // Never delete or regenerate silently: doing so would orphan the encrypted database.
                Log.e(TAG, "Unable to unwrap database key", error)
                throw IllegalStateException("The local database key could not be recovered", error)
            }
        }

        val raw = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val wrapped = wrap(raw)
        prefs.edit().putString(PREF_WRAPPED, wrapped).commit()
        return raw
    }

    private fun wrap(raw: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(raw)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun unwrap(stored: String): ByteArray {
        val combined = Base64.decode(stored, Base64.NO_WRAP)
        require(combined.size > IV_BYTES) { "Invalid wrapped key" }
        val iv = combined.copyOfRange(0, IV_BYTES)
        val encrypted = combined.copyOfRange(IV_BYTES, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
