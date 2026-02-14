package com.feedflow.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HtmlUtilsTest {

    @Test
    fun cleanHtml_removesTagsAndPreservesText() {
        val html = "<p>Hello <b>World</b>!</p>"
        // Expect paragraph to add newlines, but trailing are trimmed
        val expected = "Hello World!"
        // Wait, <p>Hello World!</p> -> Hello World!\n\n -> trimmed -> Hello World!
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }

    @Test
    fun cleanHtml_convertsImages() {
        val html = "<p>Check out this <img src=\"http://example.com/image.jpg\" alt=\"img\"> image.</p>"
        // Expect newlines around image
        val expected = "Check out this\n[IMAGE:http://example.com/image.jpg]\nimage."
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }

    @Test
    fun cleanHtml_convertsLinks() {
        val html = "<a href=\"http://example.com\">Link</a>"
        val expected = "[LINK:http://example.com|Link]"
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }

    @Test
    fun cleanHtml_preservesMentions() {
        val html = "<a href=\"/member/user\">@user</a>"
        val expected = "@user"
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }

    @Test
    fun cleanHtml_decodesEntities() {
        val html = "Tom &amp; Jerry"
        val expected = "Tom & Jerry"
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }

    @Test
    fun cleanHtml_preservesParagraphs() {
        val html = "<p>Para 1</p><p>Para 2</p>"
        // <p>Para 1</p> -> Para 1\n\n
        // <p>Para 2</p> -> Para 2\n\n
        // Concatenated -> Para 1\n\n Para 2\n\n (space from text()?)
        // Trimmed -> Para 1\n\nPara 2

        // Wait, does text() add space between Para 1 and Para 2?
        // Jsoup text() usually adds space between block elements.
        // So Para 1\n\n Para 2\n\n.
        // My cleanup:
        // text.replace(Regex(" \\n"), "\n")
        // text.replace(Regex("\\n "), "\n")

        // So Para 1\n\nPara 2\n\n.
        // Trimmed -> Para 1\n\nPara 2.

        val expected = "Para 1\n\nPara 2"
        assertEquals(expected, HtmlUtils.cleanHtml(html))
    }
}
