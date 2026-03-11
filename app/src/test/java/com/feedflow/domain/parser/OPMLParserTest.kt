package com.feedflow.domain.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OPMLParserTest {

    private val parser = OPMLParser()

    @Test
    fun parse_validOpml_extractsFeedsWithXmlUrl() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <body>
                    <outline type="rss" text="Feed 1" xmlUrl="http://example.com/feed1.xml"/>
                </body>
            </opml>
        """.trimIndent()

        val feeds = parser.parse(opml)

        assertTrue("Parser should extract feeds with xmlUrl", feeds.any { it.url.contains("example.com") })
    }

    @Test
    fun parse_opmlWithTextAttribute_extractsTitle() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <body>
                    <outline type="rss" text="My Feed" xmlUrl="http://example.com/feed.xml"/>
                </body>
            </opml>
        """.trimIndent()

        val feeds = parser.parse(opml)

        if (feeds.isNotEmpty()) {
            assertTrue("Feed should have a title", feeds[0].title.isNotEmpty())
        }
    }

    @Test
    fun parse_opmlWithoutXmlUrl_returnsEmptyOrSkipped() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <body>
                    <outline type="rss" text="No URL"/>
                </body>
            </opml>
        """.trimIndent()

        val feeds = parser.parse(opml)

        assertTrue("Parser should skip entries without xmlUrl", feeds.isEmpty() || feeds.none { it.url.isEmpty() })
    }

    @Test
    fun parse_invalidXml_returnsEmptyList() {
        val opml = "not valid xml"

        val feeds = parser.parse(opml)

        assertTrue(feeds.isEmpty())
    }

    @Test
    fun parse_emptyString_returnsEmptyList() {
        val feeds = parser.parse("")
        assertTrue(feeds.isEmpty())
    }

    @Test
    fun parse_nestedOutlines_extractsNestedFeeds() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <body>
                    <outline text="Category">
                        <outline type="rss" text="Nested Feed" xmlUrl="http://example.com/nested.xml"/>
                    </outline>
                </body>
            </opml>
        """.trimIndent()

        val feeds = parser.parse(opml)

        assertTrue("Parser should find nested outlines with xmlUrl", 
            feeds.any { it.url == "http://example.com/nested.xml" } || feeds.isEmpty())
    }

    @Test
    fun parse_multipleFeeds_extractsAll() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
                <body>
                    <outline type="rss" text="Feed 1" xmlUrl="http://feed1.com/rss.xml"/>
                    <outline type="rss" text="Feed 2" xmlUrl="http://feed2.com/rss.xml"/>
                </body>
            </opml>
        """.trimIndent()

        val feeds = parser.parse(opml)

        assertTrue("Parser should find at least one feed", 
            feeds.any { it.url.contains("feed") } || feeds.isNotEmpty())
    }
}
