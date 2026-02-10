package com.feedflow.ui.browser

import androidx.lifecycle.ViewModel
import com.feedflow.data.local.encryption.EncryptionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginBrowserViewModel @Inject constructor(
    private val encryptionHelper: EncryptionHelper
) : ViewModel() {

    fun saveCookies(siteId: String, cookieHeader: String) {
        encryptionHelper.saveCookies(siteId, cookieHeader)
    }
}
