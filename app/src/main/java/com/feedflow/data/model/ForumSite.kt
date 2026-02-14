package com.feedflow.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.feedflow.R

enum class ForumSite(
    val id: String,
    @StringRes val displayNameRes: Int,
    @DrawableRes val iconRes: Int,
    val baseUrl: String
) {
    RSS(
        id = "rss",
        displayNameRes = R.string.forum_rss,
        iconRes = R.drawable.ic_rss,
        baseUrl = ""
    ),
    HACKER_NEWS(
        id = "hackernews",
        displayNameRes = R.string.forum_hackernews,
        iconRes = R.drawable.ic_hackernews,
        baseUrl = "https://hacker-news.firebaseio.com/v0"
    ),
    FOUR_D4Y(
        id = "4d4y",
        displayNameRes = R.string.forum_4d4y,
        iconRes = R.drawable.ic_4d4y,
        baseUrl = "https://www.4d4y.com/forum"
    ),
    V2EX(
        id = "v2ex",
        displayNameRes = R.string.forum_v2ex,
        iconRes = R.drawable.ic_v2ex,
        baseUrl = "https://v2ex.com"
    ),
    LINUX_DO(
        id = "linux_do",
        displayNameRes = R.string.forum_linuxdo,
        iconRes = R.drawable.ic_linuxdo,
        baseUrl = "https://linux.do"
    ),
    ZHIHU(
        id = "zhihu",
        displayNameRes = R.string.forum_zhihu,
        iconRes = R.drawable.ic_zhihu,
        baseUrl = "https://www.zhihu.com"
    ),
    NODE_SEEK(
        id = "nodeseek",
        displayNameRes = R.string.forum_nodeseek,
        iconRes = R.drawable.ic_nodeseek,
        baseUrl = "https://www.nodeseek.com"
    ),
    TWO_LIBRA(
        id = "2libra",
        displayNameRes = R.string.forum_2libra,
        iconRes = R.drawable.ic_2libra,
        baseUrl = "https://2libra.com"
    );

    companion object {
        fun fromId(id: String): ForumSite? = entries.find { it.id == id }
    }
}
