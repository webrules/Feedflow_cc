package com.feedflow.util

import android.text.Html
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

object HtmlUtils {
    /**
     * Clean HTML content and convert to plain text with markers for images and links.
     */
    fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""

        var processed = html

        // Extract images and convert to markers
        processed = extractImages(processed)

        // Extract links and convert to markers
        processed = extractLinks(processed)

        // Parse remaining HTML and get text
        val doc = Jsoup.parse(processed)

        // Remove scripts, styles, and other non-content elements
        doc.select("script, style, head, meta, link").remove()

        // Convert line breaks
        doc.select("br").append("\n")
        doc.select("p, div, li").append("\n\n")

        // Get text
        var text = doc.text()

        // Decode HTML entities
        text = decodeHtmlEntities(text)

        // Collapse newlines after @mentions (e.g. "@username\n reply text" → "@username reply text")
        text = text.replace(Regex("(@\\S+)\\s*\\n+\\s*"), "$1 ")

        // Normalize whitespace
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        text = text.replace(Regex(" +"), " ")

        return text.trim()
    }

    private fun extractImages(html: String): String {
        val doc = Jsoup.parse(html)
        doc.select("img").forEach { img ->
            val src = img.attr("src").takeIf { it.isNotBlank() }
                ?: img.attr("data-src").takeIf { it.isNotBlank() }

            if (src != null && !isEmoji(src)) {
                img.replaceWith(org.jsoup.nodes.TextNode("\n[IMAGE:$src]\n"))
            } else {
                img.remove()
            }
        }
        return doc.html()
    }

    private fun extractLinks(html: String): String {
        val doc = Jsoup.parse(html)
        doc.select("a[href]").forEach { link ->
            val href = link.attr("href")
            val text = link.text().takeIf { it.isNotBlank() } ?: href

            if (href.isNotBlank() && !href.startsWith("#") && !href.startsWith("javascript:")) {
                // Keep @mentions as inline text (e.g. V2EX @username links)
                if (text.startsWith("@") || href.contains("/member/")) {
                    link.replaceWith(org.jsoup.nodes.TextNode(text))
                } else {
                    link.replaceWith(org.jsoup.nodes.TextNode("[LINK:$href|$text]"))
                }
            }
        }
        return doc.html()
    }

    private fun isEmoji(src: String): Boolean {
        return src.contains("emoji") ||
                src.contains("smiley") ||
                src.contains("smilies") ||
                src.contains("static/img") ||
                src.contains("images/common") ||
                src.contains("common/back.gif") ||
                src.contains("images/default") ||
                src.startsWith("data:")
    }

    fun decodeHtmlEntities(text: String): String {
        var result = text

        // Common HTML entities
        result = result.replace("&quot;", "\"")
        result = result.replace("&apos;", "'")
        result = result.replace("&amp;", "&")
        result = result.replace("&lt;", "<")
        result = result.replace("&gt;", ">")
        result = result.replace("&nbsp;", " ")
        result = result.replace("&#x27;", "'")
        result = result.replace("&#39;", "'")
        result = result.replace("&#8203;", "") // Zero-width space

        // Numeric entities
        result = result.replace(Regex("&#(\\d+);")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        // Hex entities
        result = result.replace(Regex("&#x([0-9A-Fa-f]+);")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        return result
    }

    /**
     * Strip all HTML tags from text.
     */
    fun stripTags(html: String): String {
        return Jsoup.clean(html, Safelist.none())
    }

    /**
     * Extract plain text from HTML using Android's Html class.
     */
    fun fromHtml(html: String): String {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }
}
