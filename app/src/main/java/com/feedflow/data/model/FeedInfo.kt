package com.feedflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FeedInfo(
    val id: String,
    val name: String,
    val url: String,
    val description: String = ""
)

object DefaultFeeds {
    val feeds = listOf(
        FeedInfo(
            id = "hacker_podcast",
            name = "Hacker Podcast",
            url = "https://hacker-podcast.agi.li/rss.xml",
            description = "AI-powered tech podcast"
        ),
        FeedInfo(
            id = "ruanyifeng",
            name = "Ruanyifeng Blog",
            url = "https://www.ruanyifeng.com/blog/atom.xml",
            description = "Tech blog by Ruan Yifeng"
        ),
        FeedInfo(
            id = "oreilly_radar",
            name = "O'Reilly Radar",
            url = "https://www.oreilly.com/radar/feed/",
            description = "O'Reilly technology trends"
        )
    )
}
