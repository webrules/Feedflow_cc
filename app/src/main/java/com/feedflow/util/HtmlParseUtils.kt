package com.feedflow.util

import com.feedflow.data.model.User
import org.jsoup.nodes.Element

object HtmlParseUtils {

    private const val MAX_CACHE_SIZE = 100

    fun Element.selectFirstText(selector: String): String {
        return selectFirst(selector)?.text()?.trim() ?: ""
    }

    fun Element.selectFirstAttr(selector: String, attr: String): String {
        return selectFirst(selector)?.attr(attr)?.trim() ?: ""
    }

    fun resolveUrl(url: String, baseUrl: String): String {
        if (url.isBlank()) return ""
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            !url.startsWith("http") -> "$baseUrl/$url"
            else -> url
        }
    }

    fun parseAvatarUrl(avatarUrl: String, baseUrl: String): String {
        if (avatarUrl.isBlank()) return ""
        return when {
            avatarUrl.startsWith("//") -> "https:$avatarUrl"
            avatarUrl.startsWith("/") -> "$baseUrl$avatarUrl"
            !avatarUrl.startsWith("http") -> "$baseUrl/$avatarUrl"
            else -> avatarUrl
        }
    }

    fun extractThreadIdFromUrl(url: String, pattern: Regex): String? {
        return pattern.find(url)?.groupValues?.getOrNull(1)
    }

    fun parseNumberFromText(text: String): Int {
        return text.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }

    fun Element.parseAuthorInfo(
        authorSelector: String,
        avatarSelector: String,
        baseUrl: String
    ): User {
        val authorName = selectFirstText(authorSelector).ifEmpty { "Anonymous" }
        val avatarUrl = parseAvatarUrl(selectFirstAttr(avatarSelector, "src"), baseUrl)
        return User(
            id = authorName,
            username = authorName,
            avatar = avatarUrl
        )
    }

    fun cleanContent(content: String): String {
        return content
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun <K, V> MutableMap<K, V>.putWithMaxSize(key: K, value: V, maxSize: Int = MAX_CACHE_SIZE) {
        if (size >= maxSize) {
            keys.firstOrNull()?.let { remove(it) }
        }
        put(key, value)
    }
}
