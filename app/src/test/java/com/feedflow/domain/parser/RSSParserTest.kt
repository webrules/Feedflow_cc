package com.feedflow.domain.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feedflow.data.model.Community
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStreamReader

@RunWith(AndroidJUnit4::class)
class RSSParserTest {

    private val parser = RSSParser()

    private fun readResource(fileName: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("Resource not found: $fileName")
        return InputStreamReader(stream).readText()
    }

    @Test
    fun parseRssFeed_validRss_returnsCorrectThreads() {
        val xml = readResource("rss_feed.xml")
        val community = Community("test_id", "Test Feed", "Description", "rss")

        val threads = parser.parse(xml, community)

        assertEquals(1, threads.size)
        val thread = threads[0]
        assertEquals("Example entry", thread.title)
        assertTrue(thread.content.contains("Here is some text"))
        assertEquals("http://www.example.com/blog/post/1", thread.id)
        assertEquals("John Doe", thread.author.username)
        assertEquals("test_id", thread.community.id)
    }

    @Test
    fun parseAtomFeed_validAtom_returnsCorrectThreads() {
        val xml = readResource("atom_feed.xml")
        val community = Community("test_id", "Test Feed", "Description", "rss")

        val threads = parser.parse(xml, community)

        assertEquals(1, threads.size)
        val thread = threads[0]
        assertEquals("Atom-Powered Robots Run Amok", thread.title)
        assertEquals("Some text.", thread.content)
        // RSSParser uses link as ID if available
        assertEquals("http://example.org/2003/12/13/atom03", thread.id)
        assertEquals("Jane Doe", thread.author.username)
    }

    @Test
    fun parse_invalidXml_returnsEmptyList() {
        val xml = "<invalid>xml"
        val community = Community("test_id", "Test Feed", "Description", "rss")

        val threads = parser.parse(xml, community)

        assertTrue(threads.isEmpty())
    }

    @Test
    fun parse_emptyXml_returnsEmptyList() {
        val xml = ""
        val community = Community("test_id", "Test Feed", "Description", "rss")

        val threads = parser.parse(xml, community)

        assertTrue(threads.isEmpty())
    }
}
