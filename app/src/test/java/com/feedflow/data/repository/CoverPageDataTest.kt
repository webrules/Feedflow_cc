package com.feedflow.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverPageDataTest {

    @Test
    fun parseSummaries_extractsEnglishAndChinese() {
        val combined = "---EN---\nEnglish text here.\n\n---CN---\n中文内容在这里。"

        val (en, cn) = CoverPageData.parseSummaries(combined)

        assertEquals("English text here.", en)
        assertEquals("中文内容在这里。", cn)
    }

    @Test
    fun parseSummaries_handlesChineseFirst() {
        val combined = "---CN---\n中文内容。\n\n---EN---\nEnglish content."

        val (en, cn) = CoverPageData.parseSummaries(combined)

        assertEquals("English content.", en)
        assertEquals("中文内容。", cn)
    }

    @Test
    fun parseSummaries_returnsInputWhenMarkersMissing() {
        val text = "Just some text without markers"

        val (en, cn) = CoverPageData.parseSummaries(text)

        assertEquals(text, en)
        assertEquals(text, cn)
    }

    @Test
    fun parseSiteSummary_extractsSection() {
        val text = """
            ---EN---
            ## Hacker News
            HN summary here.
            
            ## V2EX
            V2EX summary.
            
            ---CN---
            ## Hacker News
            HN 中文摘要。
            
            ## V2EX
            V2EX 中文。
        """.trimIndent()

        val (hnEn, hnCn) = CoverPageData.parseSiteSummary(text, "Hacker News")

        assertEquals("HN summary here.", hnEn)
        assertEquals("HN 中文摘要。", hnCn)
    }

    @Test
    fun parseSiteSummary_returnsEmptyWhenSectionNotFound() {
        val text = "---EN---\nSome content\n---CN---\n一些内容"

        val (en, cn) = CoverPageData.parseSiteSummary(text, "NonExistent")

        assertEquals("", en)
        assertEquals("", cn)
    }
}
