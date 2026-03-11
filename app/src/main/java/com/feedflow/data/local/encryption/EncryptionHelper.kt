package com.feedflow.data.local.encryption

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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
        const val KEY_GEMINI_API_KEY = "gemini_api_key_encrypted"
    }
}
