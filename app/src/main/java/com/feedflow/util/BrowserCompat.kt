package com.feedflow.util

/**
 * Shared browser-compatible constants so that WebView and OkHttp present
 * identical fingerprints to Cloudflare and other bot-detection systems.
 */
object BrowserCompat {

    /** Full Chrome Mobile User-Agent — must match what the login WebView sends. */
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7a) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /** Standard browser headers that Cloudflare expects on every request. */
    val BROWSER_HEADERS: Map<String, String> = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
        "Accept-Encoding" to "gzip, deflate, br",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "same-origin",
        "Sec-Fetch-User" to "?1",
    )
}
