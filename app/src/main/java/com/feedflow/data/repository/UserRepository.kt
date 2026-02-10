package com.feedflow.data.repository

import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.model.ForumSite
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val encryptionHelper: EncryptionHelper
) {
    fun isLoggedIn(siteId: String): Boolean {
        return encryptionHelper.hasCookies(siteId)
    }

    fun getLoginStatusMap(): Map<String, Boolean> {
        return ForumSite.entries
            .filter { it != ForumSite.RSS }
            .associate { it.id to isLoggedIn(it.id) }
    }

    fun saveCookies(siteId: String, cookiesJson: String) {
        encryptionHelper.saveCookies(siteId, cookiesJson)
    }

    fun getCookies(siteId: String): String? {
        return encryptionHelper.getCookies(siteId)
    }

    fun logout(siteId: String) {
        encryptionHelper.removeCookies(siteId)
    }

    fun logoutAll() {
        ForumSite.entries.forEach { site ->
            encryptionHelper.removeCookies(site.id)
        }
    }

    fun saveCredential(key: String, value: String) {
        encryptionHelper.saveCredential(key, value)
    }

    fun getCredential(key: String): String? {
        return encryptionHelper.getCredential(key)
    }

    fun removeCredential(key: String) {
        encryptionHelper.removeCredential(key)
    }
}
