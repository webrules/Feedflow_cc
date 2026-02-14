package com.feedflow.util

import android.text.Html
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist

object HtmlUtils {
    private const val MARKER_BR = "___BR___"
    private const val MARKER_P = "___P___"

    private val NEWLINE_REGEX = Regex("\\n{3,}")
    private val SPACE_NEWLINE_REGEX = Regex(" \\n")
    private val NEWLINE_SPACE_REGEX = Regex("\\n ")
    private val MENTION_NEWLINE_REGEX = Regex("(@\\S+)\\s*\\n+\\s*")

    /**
     * Clean HTML content and convert to plain text with markers for images and links.
     */
    fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""

        // 1. Parse HTML once
        val doc = Jsoup.parse(html)

        // 2. Remove scripts, styles, and other non-content elements early
        doc.select("script, style, head, meta, link").remove()

        // 3. Extract images and convert to markers
        extractImages(doc)

        // 4. Extract links and convert to markers
        extractLinks(doc)

        // 5. Handle block elements for newlines using markers
        // Replace <br> with newline marker
        doc.select("br").forEach { it.replaceWith(TextNode(MARKER_BR)) }

        // Append double newline marker to block elements
        doc.select("p, div, li, h1, h2, h3, h4, h5, h6").forEach {
             it.appendChild(TextNode(MARKER_P))
        }

        // 6. Get text (Jsoup decodes entities and normalizes whitespace)
        var text = doc.text()

        // 7. Restore newlines from markers
        text = text.replace(MARKER_BR, "\n")
        text = text.replace(MARKER_P, "\n\n")

        // 8. Post-processing regex
        // Collapse newlines after @mentions (e.g. "@username\n reply text" → "@username reply text")
        text = text.replace(MENTION_NEWLINE_REGEX, "$1 ")

        // Remove spaces around newlines
        text = text.replace(SPACE_NEWLINE_REGEX, "\n")
        text = text.replace(NEWLINE_SPACE_REGEX, "\n")

        // Normalize whitespace (collapse multiple newlines to max 2)
        text = text.replace(NEWLINE_REGEX, "\n\n")

        return text.trim()
    }

    private fun extractImages(doc: Document) {
        doc.select("img").forEach { img ->
            val src = img.attr("src").takeIf { it.isNotBlank() }
                ?: img.attr("data-src").takeIf { it.isNotBlank() }

            if (src != null && !isEmoji(src)) {
                // We want images on their own line usually
                img.replaceWith(TextNode("$MARKER_BR[IMAGE:$src]$MARKER_BR"))
            } else {
                img.remove()
            }
        }
    }

    private fun extractLinks(doc: Document) {
        doc.select("a[href]").forEach { link ->
            val href = link.attr("href")
            val text = link.text().takeIf { it.isNotBlank() } ?: href

            if (href.isNotBlank() && !href.startsWith("#") && !href.startsWith("javascript:")) {
                // Keep @mentions as inline text (e.g. V2EX @username links)
                if (text.startsWith("@") || href.contains("/member/")) {
                    link.replaceWith(TextNode(text))
                } else {
                    link.replaceWith(TextNode("[LINK:$href|$text]"))
                }
            }
        }
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

    /**
     * Decode HTML entities.
     * Note: Jsoup.parse().text() already handles this, but we keep this for legacy or non-Jsoup usage.
     */
    fun decodeHtmlEntities(text: String): String {
        return org.jsoup.parser.Parser.unescapeEntities(text, false)
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
