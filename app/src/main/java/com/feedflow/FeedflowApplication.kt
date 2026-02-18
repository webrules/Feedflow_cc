package com.feedflow

import android.app.Application
import android.webkit.CookieManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FeedflowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize CookieManager early so WebViewCookieJar can read cookies
        // even before any WebView is created in the UI.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }
    }
}
