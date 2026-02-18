package com.feedflow.data.remote

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Bridges Android [CookieManager] (WebView) to OkHttp's [CookieJar].
 *
 * This ensures that cookies set during a WebView login — including Cloudflare's
 * `cf_clearance` token — are automatically sent on subsequent OkHttp API calls,
 * mirroring how iOS shares cookies between WKWebView and URLSession.
 */
class WebViewCookieJar : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieManager = CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(url.toString()) ?: return emptyList()

        return cookieString.split(";").mapNotNull { raw ->
            Cookie.parse(url, raw.trim())
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookieManager = CookieManager.getInstance()
        for (cookie in cookies) {
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        cookieManager.flush()
    }
}
