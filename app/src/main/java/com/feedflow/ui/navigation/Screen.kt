package com.feedflow.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Cover : Screen("cover")
    object Communities : Screen("communities/{siteId}") {
        fun createRoute(siteId: String) = "communities/$siteId"
    }
    object ThreadList : Screen("threads/{siteId}/{communityId}") {
        fun createRoute(siteId: String, communityId: String) =
            "threads/$siteId/${java.net.URLEncoder.encode(communityId, "UTF-8")}"
    }
    object ThreadDetail : Screen("thread/{siteId}/{threadId}") {
        fun createRoute(siteId: String, threadId: String) =
            "thread/$siteId/${java.net.URLEncoder.encode(threadId, "UTF-8")}"
    }
    object Settings : Screen("settings")
    object Bookmarks : Screen("bookmarks")
    object Login : Screen("login")
    object LoginBrowser : Screen("login_browser/{siteId}/{url}") {
        fun createRoute(siteId: String, url: String) =
            "login_browser/$siteId/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    object CloudflareChallenge : Screen("cloudflare_challenge/{siteId}/{siteUrl}") {
        fun createRoute(siteId: String, siteUrl: String) =
            "cloudflare_challenge/$siteId/${java.net.URLEncoder.encode(siteUrl, "UTF-8")}"
    }
    object Browser : Screen("browser/{url}") {
        fun createRoute(url: String) = "browser/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    object FullScreenImage : Screen("image/{url}") {
        fun createRoute(url: String) = "image/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
}
