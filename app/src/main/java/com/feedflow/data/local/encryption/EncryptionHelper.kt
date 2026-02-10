package com.feedflow.data.local.encryption

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun encrypt(plainText: String): String {
        // Store in encrypted prefs with a unique key, return base64 encoded
        val key = "enc_${System.nanoTime()}"
        encryptedPrefs.edit().putString(key, plainText).apply()
        // Return the encrypted value as base64
        return Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String? {
        return try {
            String(Base64.decode(cipherText, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCredential(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getCredential(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    fun removeCredential(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }

    fun saveCookies(siteId: String, cookiesJson: String) {
        encryptedPrefs.edit().putString("cookies_$siteId", cookiesJson).apply()
    }

    fun getCookies(siteId: String): String? {
        return encryptedPrefs.getString("cookies_$siteId", null)
    }

    fun hasCookies(siteId: String): Boolean {
        return encryptedPrefs.contains("cookies_$siteId")
    }

    fun removeCookies(siteId: String) {
        encryptedPrefs.edit().remove("cookies_$siteId").apply()
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "feedflow_encrypted_prefs"
    }
}
