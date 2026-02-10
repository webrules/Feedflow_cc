package com.feedflow.domain.parser

import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.User
import com.feedflow.util.HtmlUtils
import com.feedflow.util.TimeUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RSSParser @Inject constructor() {

    fun parse(xml: String, community: Community): List<ForumThread> {
        val threads = mutableListOf<ForumThread>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var inEntry = false
            var inAuthor = false

            var currentTitle = ""
            var currentLink = ""
            var currentDescription = ""  // summary/description
            var currentFullContent = ""  // content:encoded or content (full)
            var currentPubDate = ""
            var currentAuthor = ""
            var currentGuid = ""

            // Track element stack for nested elements like <author><name>
            val elementStack = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = parser.name.lowercase()
                        val namespace = parser.namespace ?: ""
                        elementStack.add(localName)

                        when {
                            localName == "item" -> {
                                inItem = true
                                currentTitle = ""
                                currentLink = ""
                                currentDescription = ""
                                currentFullContent = ""
                                currentPubDate = ""
                                currentAuthor = ""
                                currentGuid = ""
                            }
                            localName == "entry" -> {
                                inEntry = true
                                currentTitle = ""
                                currentLink = ""
                                currentDescription = ""
                                currentFullContent = ""
                                currentPubDate = ""
                                currentAuthor = ""
                                currentGuid = ""
                            }
                            (inItem || inEntry) && localName == "title" && !inAuthor -> {
                                currentTitle = parser.nextText() ?: ""
                            }
                            (inItem || inEntry) && localName == "link" -> {
                                // Atom uses href attribute
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) {
                                    currentLink = href
                                } else {
                                    currentLink = parser.nextText() ?: ""
                                }
                            }
                            // content:encoded (namespace-aware: localName="encoded", ns=content)
                            (inItem || inEntry) && localName == "encoded" && namespace.contains("content") -> {
                                currentFullContent = parser.nextText() ?: ""
                            }
                            // Atom <content> or RSS <content> (without namespace)
                            (inItem || inEntry) && localName == "content" && !namespace.contains("content") -> {
                                currentFullContent = parser.nextText() ?: ""
                            }
                            // description / summary — only use as fallback
                            (inItem || inEntry) && (localName == "description" || localName == "summary") -> {
                                currentDescription = parser.nextText() ?: ""
                            }
                            (inItem || inEntry) && (localName == "pubdate" || localName == "published" || localName == "updated") -> {
                                if (currentPubDate.isBlank()) {
                                    currentPubDate = parser.nextText() ?: ""
                                }
                            }
                            // <author> tag — may contain nested <name> in Atom
                            (inItem || inEntry) && localName == "author" -> {
                                inAuthor = true
                            }
                            // <name> inside <author> (Atom format)
                            inAuthor && localName == "name" -> {
                                currentAuthor = parser.nextText() ?: ""
                            }
                            // dc:creator (namespace-aware: localName="creator", ns=dc)
                            (inItem || inEntry) && localName == "creator" -> {
                                currentAuthor = parser.nextText() ?: ""
                            }
                            (inItem || inEntry) && localName == "guid" -> {
                                currentGuid = parser.nextText() ?: ""
                            }
                            (inItem || inEntry) && localName == "id" && !inAuthor -> {
                                // Atom uses id
                                currentGuid = parser.nextText() ?: ""
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val localName = parser.name.lowercase()

                        if (localName == "author") {
                            inAuthor = false
                        }

                        if (elementStack.isNotEmpty()) {
                            elementStack.removeAt(elementStack.lastIndex)
                        }

                        if ((localName == "item" && inItem) || (localName == "entry" && inEntry)) {
                            val threadId = currentLink.ifBlank { currentGuid }
                            if (threadId.isNotBlank()) {
                                // Prefer full content over description/summary
                                val rawContent = currentFullContent.ifBlank { currentDescription }
                                val cleanContent = HtmlUtils.cleanHtml(rawContent)
                                val timeAgo = if (currentPubDate.isNotBlank()) {
                                    TimeUtils.calculateTimeAgo(currentPubDate)
                                } else {
                                    "Recent"
                                }

                                val authorName = currentAuthor.trim()

                                threads.add(
                                    ForumThread(
                                        id = threadId,
                                        title = HtmlUtils.decodeHtmlEntities(currentTitle),
                                        content = cleanContent,
                                        author = User(
                                            id = authorName.ifBlank { "" },
                                            username = authorName,
                                            avatar = ""
                                        ),
                                        community = community,
                                        timeAgo = timeAgo,
                                        likeCount = 0,
                                        commentCount = 0
                                    )
                                )
                            }
                            inItem = false
                            inEntry = false
                            inAuthor = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            android.util.Log.e("RSSParser", "Parse error: ${e.message}", e)
        }

        return threads
    }
}
