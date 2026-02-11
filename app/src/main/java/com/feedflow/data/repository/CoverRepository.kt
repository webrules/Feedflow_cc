package com.feedflow.data.repository

import com.feedflow.data.local.db.dao.CoverSummaryDao
import com.feedflow.data.local.db.entity.CoverSummaryEntity
import com.feedflow.data.model.ForumThread
import com.feedflow.data.remote.api.HackerNewsApi
import com.feedflow.domain.service.FourD4YService
import com.feedflow.domain.service.GeminiService
import com.feedflow.domain.service.V2EXService
import com.feedflow.data.model.Community
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CoverRepository"

data class CoverPageData(
    val summary: String,
    val summaryEn: String,
    val summaryCn: String,
    val hnSummaryEn: String,
    val hnSummaryCn: String,
    val v2exSummaryEn: String,
    val v2exSummaryCn: String,
    val fourD4ySummaryEn: String,
    val fourD4ySummaryCn: String,
    val hnThreads: List<ForumThread>,
    val v2exThreads: List<ForumThread>,
    val fourD4yThreads: List<ForumThread>,
    val createdAt: Long,
    val fromCache: Boolean = false
) {
    companion object {
        fun parseSummaries(combined: String): Pair<String, String> {
            val enMarker = "---EN---"
            val cnMarker = "---CN---"
            val enIdx = combined.indexOf(enMarker)
            val cnIdx = combined.indexOf(cnMarker)
            if (enIdx == -1 || cnIdx == -1) {
                return combined to combined
            }
            val en: String
            val cn: String
            if (enIdx < cnIdx) {
                en = combined.substring(enIdx + enMarker.length, cnIdx).trim()
                cn = combined.substring(cnIdx + cnMarker.length).trim()
            } else {
                cn = combined.substring(cnIdx + cnMarker.length, enIdx).trim()
                en = combined.substring(enIdx + enMarker.length).trim()
            }
            return en to cn
        }

        fun parseSiteSummary(combined: String, siteName: String): Pair<String, String> {
            val (en, cn) = parseSummaries(combined)
            fun extractSection(text: String, name: String): String {
                val header = "## $name"
                val idx = text.indexOf(header)
                if (idx == -1) return ""
                val start = idx + header.length
                val nextSection = text.indexOf("\n## ", start)
                val section = if (nextSection == -1) text.substring(start) else text.substring(start, nextSection)
                return section.trim()
            }
            return extractSection(en, siteName) to extractSection(cn, siteName)
        }
    }
}

@Singleton
class CoverRepository @Inject constructor(
    private val coverSummaryDao: CoverSummaryDao,
    private val hackerNewsApi: HackerNewsApi,
    private val v2exService: V2EXService,
    private val fourD4YService: FourD4YService,
    private val geminiService: GeminiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCoverPage(forceRefresh: Boolean = false): CoverPageData {
        if (!forceRefresh) {
            val cached = coverSummaryDao.getLatest()
            if (cached != null && cached.isFresh()) {
                // Treat empty or very short summaries as failed — don't serve stale failures
                if (cached.summary.length < 50) {
                    Log.w(TAG, "getCoverPage: cached summary too short (${cached.summary.length} chars), regenerating")
                } else {
                    Log.d(TAG, "getCoverPage: serving from cache (age=${(System.currentTimeMillis() - cached.createdAt) / 1000}s, summary=${cached.summary.length} chars)")
                    return entityToData(cached, fromCache = true)
                }
            }
        } else {
            Log.d(TAG, "getCoverPage: forceRefresh=true, generating fresh")
        }
        return generateFreshCover()
    }

    suspend fun generateFreshCover(): CoverPageData = coroutineScope {
        val hnDeferred = async { fetchHNPosts() }
        val v2exDeferred = async { fetchV2EXPosts() }
        val fourD4yDeferred = async { fetchFourD4YPosts() }

        val hnThreads = try { hnDeferred.await() } catch (e: Exception) {
            Log.e(TAG, "fetchHNPosts failed", e)
            emptyList()
        }
        val v2exThreads = try { v2exDeferred.await() } catch (e: Exception) {
            Log.e(TAG, "fetchV2EXPosts failed", e)
            emptyList()
        }
        val fourD4yThreads = try { fourD4yDeferred.await() } catch (e: Exception) {
            Log.e(TAG, "fetchFourD4YPosts failed", e)
            emptyList()
        }

        Log.d(TAG, "Thread counts — HN: ${hnThreads.size}, V2EX: ${v2exThreads.size}, 4D4Y: ${fourD4yThreads.size}")

        val summary = try {
            val hnPairs = hnThreads.map { it.title to "${it.likeCount} points, ${it.commentCount} comments" }
            val v2exPairs = v2exThreads.map { it.title to "${it.commentCount} replies" }
            val fourD4yPairs = fourD4yThreads.map { it.title to "${it.commentCount} replies" }

            val hnSummaryDeferred = async {
                if (hnPairs.isNotEmpty()) geminiService.generateSiteSummary("Hacker News", hnPairs) else ""
            }
            val v2exSummaryDeferred = async {
                if (v2exPairs.isNotEmpty()) geminiService.generateSiteSummary("V2EX", v2exPairs) else ""
            }
            val fourD4ySummaryDeferred = async {
                if (fourD4yPairs.isNotEmpty()) geminiService.generateSiteSummary("4D4Y", fourD4yPairs) else ""
            }

            val hnSummary = try { hnSummaryDeferred.await() } catch (e: Exception) {
                Log.e(TAG, "Gemini HN summary failed", e); ""
            }
            val v2exSummary = try { v2exSummaryDeferred.await() } catch (e: Exception) {
                Log.e(TAG, "Gemini V2EX summary failed", e); ""
            }
            val fourD4ySummary = try { fourD4ySummaryDeferred.await() } catch (e: Exception) {
                Log.e(TAG, "Gemini 4D4Y summary failed", e); ""
            }

            val result = combineSiteSummaries(hnSummary, v2exSummary, fourD4ySummary)
            Log.d(TAG, "Gemini summary generated successfully (${result.length} chars)")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generateCoverSummary FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            buildFallbackSummary(hnThreads, v2exThreads, fourD4yThreads)
        }

        val allThreads = hnThreads + v2exThreads + fourD4yThreads
        val postsJson = json.encodeToString(allThreads)
        val now = System.currentTimeMillis()

        val entity = CoverSummaryEntity(
            summary = summary,
            postsJson = postsJson,
            createdAt = now,
            hnCount = hnThreads.size,
            v2exCount = v2exThreads.size,
            fourD4yCount = fourD4yThreads.size
        )
        coverSummaryDao.insert(entity)

        // Clean up covers older than 30 days
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        coverSummaryDao.deleteOlderThan(thirtyDaysAgo)

        val (en, cn) = CoverPageData.parseSummaries(summary)
        val (hnEn, hnCn) = CoverPageData.parseSiteSummary(summary, "Hacker News")
        val (v2exEn, v2exCn) = CoverPageData.parseSiteSummary(summary, "V2EX")
        val (fourD4yEn, fourD4yCn) = CoverPageData.parseSiteSummary(summary, "4D4Y")

        CoverPageData(
            summary = summary,
            summaryEn = en,
            summaryCn = cn,
            hnSummaryEn = hnEn,
            hnSummaryCn = hnCn,
            v2exSummaryEn = v2exEn,
            v2exSummaryCn = v2exCn,
            fourD4ySummaryEn = fourD4yEn,
            fourD4ySummaryCn = fourD4yCn,
            hnThreads = hnThreads,
            v2exThreads = v2exThreads,
            fourD4yThreads = fourD4yThreads,
            createdAt = now
        )
    }

    private suspend fun fetchHNPosts(): List<ForumThread> = coroutineScope {
        val storyIds = hackerNewsApi.getBestStories()
        val cutoff = (System.currentTimeMillis() / 1000) - 86400

        val items = storyIds.take(100).map { id ->
            async {
                try {
                    hackerNewsApi.getItem(id)
                } catch (e: Exception) {
                    null
                }
            }
        }.map { it.await() }.filterNotNull()

        val community = Community("beststories", "Best", "Best stories", "hackernews")

        items.filter { it.time.toLong() > cutoff }
            .sortedByDescending { it.descendants ?: 0 }
            .take(10)
            .map { item ->
                ForumThread(
                    id = item.id.toString(),
                    title = item.title ?: "",
                    content = "",
                    author = com.feedflow.data.model.User(
                        item.by ?: "unknown",
                        item.by ?: "unknown",
                        ""
                    ),
                    community = community,
                    timeAgo = com.feedflow.util.TimeUtils.calculateTimeAgo(item.time.toLong()),
                    likeCount = item.score ?: 0,
                    commentCount = item.descendants ?: 0
                )
            }
    }

    private suspend fun fetchV2EXPosts(): List<ForumThread> {
        val community = Community("hot", "Hot", "Hot topics", "v2ex")
        val threads = v2exService.fetchCategoryThreads("hot", listOf(community))
        return threads.sortedByDescending { it.commentCount }.take(10)
    }

    private suspend fun fetchFourD4YPosts(): List<ForumThread> {
        val threads = fourD4YService.fetchRecentThreads(fid = "2")
        return threads.sortedByDescending { it.commentCount }.take(10)
    }

    private fun buildFallbackSummary(
        hn: List<ForumThread>,
        v2ex: List<ForumThread>,
        fourD4y: List<ForumThread>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# Today's Hot Discussions / 今日热门讨论")
        sb.appendLine()
        if (hn.isNotEmpty()) {
            sb.appendLine("## Hacker News")
            hn.forEach { sb.appendLine("- **${it.title}** (${it.likeCount} pts, ${it.commentCount} comments)") }
            sb.appendLine()
        }
        if (v2ex.isNotEmpty()) {
            sb.appendLine("## V2EX")
            v2ex.forEach { sb.appendLine("- **${it.title}** (${it.commentCount} replies)") }
            sb.appendLine()
        }
        if (fourD4y.isNotEmpty()) {
            sb.appendLine("## 4D4Y")
            fourD4y.forEach { sb.appendLine("- **${it.title}** (${it.commentCount} replies)") }
        }
        return sb.toString()
    }

    private fun combineSiteSummaries(
        hnSummary: String,
        v2exSummary: String,
        fourD4ySummary: String
    ): String {
        fun extractParts(raw: String): Pair<String, String> {
            val (en, cn) = CoverPageData.parseSummaries(raw)
            return en to cn
        }

        val sites = listOf(
            "Hacker News" to hnSummary,
            "V2EX" to v2exSummary,
            "4D4Y" to fourD4ySummary
        ).filter { it.second.isNotBlank() }

        val enParts = StringBuilder()
        val cnParts = StringBuilder()
        for ((name, raw) in sites) {
            val (en, cn) = extractParts(raw)
            enParts.appendLine("## $name")
            enParts.appendLine(en)
            enParts.appendLine()
            cnParts.appendLine("## $name")
            cnParts.appendLine(cn)
            cnParts.appendLine()
        }

        return "---EN---\n${enParts.toString().trim()}\n\n---CN---\n${cnParts.toString().trim()}"
    }

    private fun entityToData(entity: CoverSummaryEntity, fromCache: Boolean): CoverPageData {
        val allThreads = try {
            json.decodeFromString<List<ForumThread>>(entity.postsJson)
        } catch (e: Exception) {
            emptyList()
        }

        val hnThreads = allThreads.filter { it.community.category == "hackernews" }
        val v2exThreads = allThreads.filter { it.community.category == "v2ex" }
        val fourD4yThreads = allThreads.filter { it.community.category == "4d4y" }

        val (en, cn) = CoverPageData.parseSummaries(entity.summary)
        val (hnEn, hnCn) = CoverPageData.parseSiteSummary(entity.summary, "Hacker News")
        val (v2exEn, v2exCn) = CoverPageData.parseSiteSummary(entity.summary, "V2EX")
        val (fourD4yEn, fourD4yCn) = CoverPageData.parseSiteSummary(entity.summary, "4D4Y")

        return CoverPageData(
            summary = entity.summary,
            summaryEn = en,
            summaryCn = cn,
            hnSummaryEn = hnEn,
            hnSummaryCn = hnCn,
            v2exSummaryEn = v2exEn,
            v2exSummaryCn = v2exCn,
            fourD4ySummaryEn = fourD4yEn,
            fourD4ySummaryCn = fourD4yCn,
            hnThreads = hnThreads,
            v2exThreads = v2exThreads,
            fourD4yThreads = fourD4yThreads,
            createdAt = entity.createdAt,
            fromCache = fromCache
        )
    }

    fun getSavedCoversFlow(): Flow<List<CoverSummaryEntity>> = coverSummaryDao.getAllFlow()

    suspend fun deleteCover(id: Long) = coverSummaryDao.delete(id)

    suspend fun deleteOlderThanOneWeek(): Int {
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        coverSummaryDao.deleteOlderThan(oneWeekAgo)
        return 1 // signal success
    }

    suspend fun loadSavedCover(entity: CoverSummaryEntity): CoverPageData {
        return entityToData(entity, fromCache = true)
    }
}
