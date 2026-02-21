package com.feedflow.util

import com.feedflow.data.model.Comment
import com.feedflow.data.model.User
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlParsingHelper {

    fun parseHtml(html: String): org.jsoup.nodes.Document = Jsoup.parse(html)

    fun Element.getText(selector: String): String {
        return selectFirst(selector)?.text()?.trim() ?: ""
    }

    fun Element.getAttr(selector: String, attr: String): String {
        return selectFirst(selector)?.attr(attr)?.trim() ?: ""
    }

    fun Element.getAllText(): String = text().trim()

    fun Element.getHtml(): String = html()

    fun resolveUrl(url: String?, baseUrl: String): String {
        if (url.isNullOrBlank()) return ""
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            !url.startsWith("http") -> "$baseUrl/$url"
            else -> url
        }
    }

    fun parseAuthor(
        element: Element,
        authorSelector: String,
        avatarSelector: String,
        baseUrl: String
    ): User {
        val name = element.getText(authorSelector).ifEmpty { "Anonymous" }
        val avatarUrl = resolveUrl(element.getAttr(avatarSelector, "src"), baseUrl)
        return User(id = name, username = name, avatar = avatarUrl)
    }

    fun parseNumber(text: String?): Int {
        return text?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
    }

    fun Element.parseComment(
        idSelector: (Element) -> String,
        authorSelector: String,
        avatarSelector: String,
        contentSelector: String,
        timeSelector: String,
        baseUrl: String
    ): Comment? {
        val content = getText(contentSelector)
        if (content.isBlank()) return null
        
        val id = idSelector(this)
        val author = parseAuthor(this, authorSelector, avatarSelector, baseUrl)
        val timeAgo = getText(timeSelector)
        
        return Comment(
            id = id,
            author = author,
            content = HtmlUtils.cleanHtml(content),
            timeAgo = timeAgo,
            likeCount = 0
        )
    }

    fun extractIdFromUrl(url: String, prefix: String, suffix: String = ""): String {
        return url.substringAfter(prefix).substringBefore(suffix).substringBefore("#").substringBefore("?")
    }
}
