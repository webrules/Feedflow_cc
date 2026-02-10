package com.feedflow.domain.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

data class OPMLFeed(
    val title: String,
    val url: String
)

@Singleton
class OPMLParser @Inject constructor() {

    fun parse(opml: String): List<OPMLFeed> {
        val feeds = mutableListOf<OPMLFeed>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(opml))

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "outline") {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")

                    if (!xmlUrl.isNullOrBlank()) {
                        val title = parser.getAttributeValue(null, "text")
                            ?: parser.getAttributeValue(null, "title")
                            ?: xmlUrl

                        feeds.add(OPMLFeed(title = title, url = xmlUrl))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Return whatever we parsed so far
        }

        return feeds
    }
}
