package com.feedflow.domain.service

import com.feedflow.R
import com.feedflow.data.local.encryption.EncryptionHelper
import com.feedflow.data.model.Comment
import com.feedflow.data.model.Community
import com.feedflow.data.model.ForumThread
import com.feedflow.data.model.ThreadDetailResult
import com.feedflow.data.model.User
import com.feedflow.util.GBKUtils
import com.feedflow.util.HtmlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FourD4YService @Inject constructor(
    private val client: OkHttpClient,
    private val encryptionHelper: EncryptionHelper
) : ForumService {

    override val name: String = "4D4Y"
    override val id: String = "4d4y"
    override val logo: Int = R.drawable.ic_4d4y

    private val baseUrl = "https://www.4d4y.com/forum"
    private val gbkCharset: Charset = Charset.forName("GBK")

    private var sessionId: String? = null
    private var formHash: String? = null

    override suspend fun fetchCategories(): List<Community> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/index.php"
        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val bytes = response.body?.bytes() ?: return@withContext emptyList()
        val html = GBKUtils.smartDecode(bytes)

        // Extract session ID
        extractSessionId(html)

        parseCategories(html)
    }

    override suspend fun fetchCategoryThreads(
        categoryId: String,
        communities: List<Community>,
        page: Int
    ): List<ForumThread> = withContext(Dispatchers.IO) {
        val sidParam = sessionId?.let { "&sid=$it" } ?: ""
        val url = "$baseUrl/forumdisplay.php?fid=$categoryId$sidParam&page=$page"
        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val bytes = response.body?.bytes() ?: return@withContext emptyList()
        val html = GBKUtils.smartDecode(bytes)

        // Update session ID
        extractSessionId(html)

        val community = communities.find { it.id == categoryId }
            ?: Community(categoryId, "Forum", "", id)

        parseThreadList(html, community)
    }

    override suspend fun fetchThreadDetail(threadId: String, page: Int): ThreadDetailResult =
        withContext(Dispatchers.IO) {
            val sidParam = sessionId?.let { "&sid=$it" } ?: ""
            val url = "$baseUrl/viewthread.php?tid=$threadId$sidParam&page=$page"
            val request = buildRequest(url)

            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes() ?: throw Exception("Failed to load thread")
            val html = GBKUtils.smartDecode(bytes)

            // Update session ID and form hash
            extractSessionId(html)
            extractFormHash(html)

            parseThreadDetail(html, threadId, page)
        }

    override suspend fun postComment(topicId: String, categoryId: String, content: String) =
        withContext(Dispatchers.IO) {
            val hash = formHash ?: throw Exception("Form hash not found. Please refresh the thread.")
            val sid = sessionId ?: ""

            val url = "$baseUrl/post.php?action=reply&fid=$categoryId&tid=$topicId&replysubmit=yes&sid=$sid&inajax=1"

            val encodedContent = GBKUtils.encodeToGBK(content)
            val formBody = FormBody.Builder()
                .add("formhash", hash)
                .add("message", content)
                .add("posttime", (System.currentTimeMillis() / 1000).toString())
                .add("wysiwyg", "1")
                .build()

            val request = buildRequest(url).newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to post comment: ${response.code}")
            }
        }

    override suspend fun createThread(categoryId: String, title: String, content: String) {
        throw UnsupportedOperationException("Thread creation not yet implemented")
    }

    suspend fun fetchRecentThreads(fid: String = "2"): List<ForumThread> = withContext(Dispatchers.IO) {
        val allThreads = mutableListOf<ForumThread>()
        val community = Community(fid, "Forum", "", id)

        // Fetch page 1 first to determine total page count
        val sidParam = sessionId?.let { "&sid=$it" } ?: ""
        val firstUrl = "$baseUrl/forumdisplay.php?fid=$fid$sidParam&orderby=lastpost&filter=86400&page=1"
        val firstRequest = buildRequest(firstUrl)
        val firstResponse = client.newCall(firstRequest).execute()
        val firstBytes = firstResponse.body?.bytes() ?: return@withContext emptyList()
        val firstHtml = GBKUtils.smartDecode(firstBytes)

        extractSessionId(firstHtml)
        allThreads.addAll(parseThreadList(firstHtml, community))

        // Dynamically extract total page count from pagination HTML
        val pagesDoc = Jsoup.parse(firstHtml)
        val pagesDiv = pagesDoc.selectFirst("div.pages")
        val maxPage = if (pagesDiv != null) {
            val pageNums = Regex("""page=(\d+)""").findAll(pagesDiv.html())
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .toList()
            pageNums.maxOrNull() ?: 1
        } else {
            1
        }

        // Fetch remaining pages
        for (page in 2..maxPage) {
            try {
                val sidParam2 = sessionId?.let { "&sid=$it" } ?: ""
                val url = "$baseUrl/forumdisplay.php?fid=$fid$sidParam2&orderby=lastpost&filter=86400&page=$page"
                val request = buildRequest(url)
                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: continue
                val html = GBKUtils.smartDecode(bytes)
                extractSessionId(html)
                allThreads.addAll(parseThreadList(html, community))
            } catch (e: Exception) {
                android.util.Log.e("4D4Y", "Error fetching page $page", e)
            }
        }

        android.util.Log.d("4D4Y", "fetchRecentThreads: ${allThreads.size} threads from $maxPage pages")
        allThreads
    }

    override fun getWebURL(thread: ForumThread): String {
        return "$baseUrl/viewthread.php?tid=${thread.id}"
    }

    override fun supportsPosting(): Boolean = true
    override fun requiresLogin(): Boolean = true

    private fun buildRequest(url: String): Request {
        val cookies = encryptionHelper.getCookies(id)
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .apply {
                if (!cookies.isNullOrBlank()) {
                    header("Cookie", cookies)
                }
            }
            .build()
    }

    private fun extractSessionId(html: String) {
        val sidRegex = Regex("""sid=([a-zA-Z0-9]+)""")
        sidRegex.find(html)?.let {
            sessionId = it.groupValues[1]
        }
    }

    private fun extractFormHash(html: String) {
        val hashRegex = Regex("""formhash=([a-zA-Z0-9]+)""")
        hashRegex.find(html)?.let {
            formHash = it.groupValues[1]
        }
    }

    private fun parseCategories(html: String): List<Community> {
        val doc = Jsoup.parse(html)
        val communities = mutableListOf<Community>()

        doc.select("a[href*=forumdisplay.php?fid=]").forEach { link ->
            try {
                val href = link.attr("href")
                val fid = href.substringAfter("fid=").substringBefore("&")
                val name = link.text().trim()

                if (name.isNotBlank() && fid.isNotBlank()) {
                    communities.add(
                        Community(
                            id = fid,
                            name = name,
                            description = "",
                            category = id
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }

        return communities.distinctBy { it.id }
    }

    private fun parseThreadList(html: String, community: Community): List<ForumThread> {
        val threads = mutableListOf<ForumThread>()
        val doc = Jsoup.parse(html)

        // WAP template: each thread is a <tr> containing:
        //   <td class="listcon">
        //     <p><a href="space.php?uid=...">author</a> / date</p>
        //     <a href="viewthread.php?tid=..." class="title">title</a>
        //     <p>last reply info</p>
        //   </td>
        //   <td width="70" align="right"><a class="num">replyCount</a></td>
        val rows = doc.select("td.listcon")
        for (cell in rows) {
            try {
                // Extract thread link and title
                val titleLink = cell.selectFirst("a.title[href*=viewthread.php?tid=]") ?: continue
                val href = titleLink.attr("href")
                val tid = href.substringAfter("tid=").substringBefore("&")
                val title = HtmlUtils.decodeHtmlEntities(titleLink.text().trim())
                if (tid.isBlank() || title.isBlank()) continue

                // Extract author from first <p> > <a href="space.php?uid=...">
                val firstP = cell.selectFirst("p")
                val authorLink = firstP?.selectFirst("a[href*=space.php?uid=]")
                val authorName = authorLink?.text()?.trim() ?: ""

                // Extract date from first <p> text (after author link), e.g. "/ 2026-2-9"
                val pText = firstP?.text() ?: ""
                val dateMatch = Regex("""/\s*(\d{4}-\d{1,2}-\d{1,2})""").find(pText)
                val timeAgo = dateMatch?.groupValues?.get(1) ?: ""

                // Extract reply count from sibling <td><a class="num">
                val parentRow = cell.parent()
                val replyCount = parentRow?.selectFirst("a.num")?.text()?.toIntOrNull() ?: 0

                threads.add(
                    ForumThread(
                        id = tid,
                        title = title,
                        content = "",
                        author = User(authorName, authorName, ""),
                        community = community,
                        timeAgo = timeAgo,
                        likeCount = 0,
                        commentCount = replyCount
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("4D4Y", "Error parsing thread row", e)
            }
        }

        // Fallback: if WAP template parsing found nothing, try desktop Discuz selectors
        if (threads.isEmpty()) {
            val rowPattern = Regex(
                """<tbody[^>]*id="(?:normalthread_|stickthread_)(\d+)"[^>]*>(.*?)</tbody>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val titlePattern = Regex(
                """href="viewthread\.php\?tid=\d+[^"]*"[^>]*>([^<]+)</a>""",
                RegexOption.IGNORE_CASE
            )
            val authorPattern = Regex(
                """<td\s+class="author"[^>]*>.*?<a[^>]*>([^<]+)</a>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val numsPattern = Regex(
                """<td\s+class="nums"[^>]*>.*?<strong>(\d+)</strong>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            for (match in rowPattern.findAll(html)) {
                try {
                    val threadId = match.groupValues[1]
                    val rowContent = match.groupValues[2]
                    val titleMatch = titlePattern.find(rowContent) ?: continue
                    val title = HtmlUtils.decodeHtmlEntities(titleMatch.groupValues[1].trim())
                    if (title.isBlank()) continue
                    val authorName = authorPattern.find(rowContent)?.groupValues?.get(1)?.trim() ?: ""
                    val replyCount = numsPattern.find(rowContent)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    threads.add(
                        ForumThread(
                            id = threadId,
                            title = title,
                            content = "",
                            author = User(authorName, authorName, ""),
                            community = community,
                            timeAgo = "",
                            likeCount = 0,
                            commentCount = replyCount
                        )
                    )
                } catch (e: Exception) { /* skip */ }
            }
        }

        android.util.Log.d("4D4Y", "Parsed ${threads.size} threads")
        return threads
    }

    private fun parseThreadDetail(html: String, threadId: String, page: Int): ThreadDetailResult {
        val doc = Jsoup.parse(html)

        // Parse title from <title> tag, stop at " - "
        val titleFromTag = Regex("""<title>(.*?)(?:\s-\s|</title>)""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()
        val title = titleFromTag?.let { HtmlUtils.decodeHtmlEntities(it) }
            ?: doc.selectFirst("h2")?.text()?.trim()
            ?: ""

        // ---- This forum uses a custom WAP/mobile Discuz 7.2 template ----
        // OP structure:
        //   <div class="detailcon" id="pidXXXXX">...content...</div>
        //   Author: <a href="space.php?uid=...">Name</a> before <em id="authorpostonXXXXX">
        // Reply structure:
        //   <div class="w replylist"><ul>
        //     <li id="pidXXXXX">
        //       <div class="replytop">...<a href="space.php?uid=...">Author</a>/ timestamp</div>
        //       <div class="replycon">...content...</div>
        //     </li>
        //   </ul></div>

        data class PostData(val id: String, val content: String, val author: String, val time: String)
        val posts = mutableListOf<PostData>()

        // --- Parse OP (first post) ---
        val opContent = doc.selectFirst("div.detailcon[id^=pid]")
        if (opContent != null) {
            val pid = opContent.id().removePrefix("pid")
            val content = opContent.html()

            // OP author: find <a href="space.php?uid=..."> near <em id="authorpostonXXX">
            val opAuthorRegex = Regex(
                """<a[^>]*href="space\.php\?uid=\d+"[^>]*>([^<]+)</a>\s*\n?\s*<em\s+id="authorposton""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val opAuthor = opAuthorRegex.find(html)?.groupValues?.get(1)?.trim() ?: "Anonymous"

            // OP time from <em id="authorpostonXXXXX">发表于 2026-2-10 09:45</em>
            val opTime = doc.selectFirst("em[id^=authorposton]")?.text()
                ?.removePrefix("发表于")?.trim() ?: ""

            posts.add(PostData(pid, content, opAuthor, opTime))
        }

        // --- Parse replies ---
        val replyItems = doc.select("div.replylist li[id^=pid], div.w.replylist li[id^=pid]")
        for (item in replyItems) {
            val pid = item.id().removePrefix("pid")

            // Reply content from <div class="replycon">
            val replyConEl = item.selectFirst("div.replycon") ?: continue
            val content = replyConEl.html()

            // Reply author and time from <div class="replytop">
            val replyTop = item.selectFirst("div.replytop")
            var author = "Anonymous"
            var time = ""
            if (replyTop != null) {
                val authorLink = replyTop.selectFirst("a[href*=space.php?uid=]")
                author = authorLink?.text()?.trim() ?: "Anonymous"
                // Time is the text after the author link, e.g. "/ 2026-2-10 09:48"
                val topText = replyTop.text()
                val timeMatch = Regex("""/\s*(\d{4}-\d{1,2}-\d{1,2}\s+\d{1,2}:\d{2})""").find(topText)
                time = timeMatch?.groupValues?.get(1) ?: ""
            }

            posts.add(PostData(pid, content, author, time))
        }

        // --- Fallback: try standard Discuz selectors if WAP template didn't match ---
        if (posts.isEmpty()) {
            // Try standard postmessage_ elements
            val postMessageElements = doc.select("[id^=postmessage_]")
            if (postMessageElements.isNotEmpty()) {
                for (el in postMessageElements) {
                    val pid = el.id().removePrefix("postmessage_")
                    posts.add(PostData(pid, el.html(), "Anonymous", ""))
                }
            }
            // Try t_f class
            if (posts.isEmpty()) {
                val tfElements = doc.select("td.t_f, div.t_f")
                for (el in tfElements) {
                    val pid = el.parents().firstOrNull { it.id().startsWith("pid") }
                        ?.id()?.removePrefix("pid") ?: posts.size.toString()
                    posts.add(PostData(pid, el.html(), "Anonymous", ""))
                }
            }
        }

        android.util.Log.d("4D4Y", "parseThreadDetail: ${posts.size} posts found, page=$page")

        // On page 1: first post = thread content, rest = comments
        // On page 2+: ALL posts are comments (OP is only on page 1)
        val isFirstPage = page == 1
        val opPost = if (isFirstPage) posts.firstOrNull() else null
        val commentPosts = if (isFirstPage) posts.drop(1) else posts

        val cleanedContent = opPost?.let { cleanPostContent(it.content) } ?: ""
        val firstAuthor = opPost?.author ?: ""

        val community = Community(
            id = "general",
            name = "4D4Y",
            description = "",
            category = id
        )

        val thread = ForumThread(
            id = threadId,
            title = title,
            content = cleanedContent,
            author = User(firstAuthor, firstAuthor, ""),
            community = community,
            timeAgo = opPost?.time ?: "",
            likeCount = 0,
            commentCount = commentPosts.size
        )

        // Parse comments
        val comments = commentPosts.mapNotNull { post ->
            try {
                Comment(
                    id = post.id,
                    author = User(post.author, post.author, ""),
                    content = cleanPostContent(post.content),
                    timeAgo = post.time,
                    likeCount = 0
                )
            } catch (e: Exception) {
                android.util.Log.e("4D4Y", "Error parsing comment", e)
                null
            }
        }

        // Calculate total pages from pagination links
        // WAP template: <div class="pages"> or <div class="w pages">
        // Look for page links like page=2, page=3 etc., or <strong>2</strong> for current page
        val totalPages = run {
            // Try to find max page number from any page=N parameter in the HTML
            val pageNums = Regex("""[?&]page=(\d+)""").findAll(html)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .toList()
            // Also look for <strong>N</strong> inside pages div (current page indicator)
            val strongNums = Regex("""<strong>(\d+)</strong>""").findAll(html)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .filter { it in 1..999 }
                .toList()
            val allNums = pageNums + strongNums
            val max = allNums.maxOrNull()
            android.util.Log.d("4D4Y", "parseThreadDetail: pageNums=$pageNums, strongNums=$strongNums, totalPages=$max")
            max ?: 1
        }

        android.util.Log.d("4D4Y", "parseThreadDetail: title='$title', contentLen=${cleanedContent.length}, comments=${comments.size}, totalPages=$totalPages")

        return ThreadDetailResult(
            thread = thread,
            comments = comments,
            totalPages = totalPages
        )
    }

    private fun cleanPostContent(rawHtml: String): String {
        // Remove attachments
        val noAttach = rawHtml.replace(Regex("""<div class="t_attach".*?</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        // Remove ignore_js_op blocks
        val noJs = noAttach.replace(Regex("""<ignore_js_op>.*?</ignore_js_op>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        // Remove smiley images but keep regular images
        val noSmileys = noJs.replace(Regex("""<img[^>]*smilieid[^>]*/?>""", RegexOption.IGNORE_CASE), "")
        // Remove edit notices: <i class="pstatus"> 本帖最后由 ... 编辑 </i>
        val noPstatus = noSmileys.replace(Regex("""<i\s+class="pstatus"[^>]*>.*?</i>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        // Remove back.gif links (the <a> wrapping the back.gif image)
        val noBackGif = noPstatus.replace(Regex("""<a[^>]*>\s*<img[^>]*common/back\.gif[^>]*/?\s*>\s*</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        // Remove common user signatures/footers
        val noSigs = noBackGif
            .replace(Regex("""<a[^>]*viewthread\.php\?tid=2849673[^>]*>.*?</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")  // 每日地板
            .replace(Regex("""<a[^>]*viewthread\.php\?tid=3419373[^>]*>.*?</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")  // Re:Source
            .replace(Regex("""<a[^>]*viewthread\.php\?tid=2950630[^>]*>.*?</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")  // 论坛助手
            .replace(Regex("""<a[^>]*viewthread\.php\?tid=1579403[^>]*>.*?</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")  // HiPDA·NG
            .replace(Regex("""<font[^>]*>\s*iOS fly ~\s*</font>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<font[^>]*color="#999"[^>]*>\s*.*?I love 4d4y~\s*</font>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        // Flatten reply-to blocks: <strong>回复 <a ...>27#</a> <i>username</i></strong> → 回复 27# username
        val noReplyTags = noSigs.replace(
            Regex("""<strong>\s*回复\s*<a[^>]*>(\d+#)</a>\s*<i>([^<]*)</i>\s*</strong>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            "回复 $1 $2 "
        )
        val cleaned = HtmlUtils.cleanHtml(noReplyTags)
        // Collapse newlines around "回复 XX# username" so it stays inline with content
        // e.g. "回复\n27#\nshooirin还有一个" → "回复 27# shooirin还有一个"
        return cleaned.replace(Regex("""回复\s*\n+\s*(\d+#)\s*\n+\s*"""), "回复 $1 ")
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
