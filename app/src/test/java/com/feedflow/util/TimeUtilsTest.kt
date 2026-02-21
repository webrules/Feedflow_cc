package com.feedflow.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date
import java.util.concurrent.TimeUnit

class TimeUtilsTest {

    @Test
    fun calculateTimeAgo_seconds() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.SECONDS.toMillis(30))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("30s", result)
    }

    @Test
    fun calculateTimeAgo_minutes() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.MINUTES.toMillis(5))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("5m", result)
    }

    @Test
    fun calculateTimeAgo_hours() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.HOURS.toMillis(3))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("3h", result)
    }

    @Test
    fun calculateTimeAgo_days() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.DAYS.toMillis(2))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("2d", result)
    }

    @Test
    fun calculateTimeAgo_weeks() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.DAYS.toMillis(14))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("2w", result)
    }

    @Test
    fun calculateTimeAgo_months() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.DAYS.toMillis(60))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("2mo", result)
    }

    @Test
    fun calculateTimeAgo_years() {
        val now = System.currentTimeMillis()
        val date = Date(now - TimeUnit.DAYS.toMillis(400))
        val result = TimeUtils.calculateTimeAgo(date)
        assertEquals("1y", result)
    }

    @Test
    fun calculateTimeAgo_timestamp() {
        val now = System.currentTimeMillis()
        val timestamp = (now - TimeUnit.MINUTES.toMillis(10)) / 1000
        val result = TimeUtils.calculateTimeAgo(timestamp)
        assertEquals("10m", result)
    }

    @Test
    fun parseDate_iso8601Format() {
        val dateString = "2024-01-15T10:30:00.000Z"
        val result = TimeUtils.parseDate(dateString)
        assertNotNull(result)
    }

    @Test
    fun parseDate_iso8601AltFormat() {
        val dateString = "2024-01-15T10:30:00Z"
        val result = TimeUtils.parseDate(dateString)
        assertNotNull(result)
    }

    @Test
    fun parseDate_rfc822Format() {
        val dateString = "Mon, 15 Jan 2024 10:30:00 +0000"
        val result = TimeUtils.parseDate(dateString)
        assertNotNull(result)
    }

    @Test
    fun parseDate_invalidFormat_returnsNull() {
        val dateString = "invalid-date"
        val result = TimeUtils.parseDate(dateString)
        assertNull(result)
    }

    @Test
    fun calculateTimeAgo_stringInput() {
        val result = TimeUtils.calculateTimeAgo("invalid-date")
        assertEquals("Recent", result)
    }

    @Test
    fun parseUnixTimestamp() {
        val timestamp = 1705315800
        val result = TimeUtils.parseUnixTimestamp(timestamp)
        assertNotNull(result)
    }

    @Test
    fun formatDate_defaultPattern() {
        val date = Date(1705315800000L)
        val result = TimeUtils.formatDate(date)
        assertNotNull(result)
        assert(result.isNotEmpty())
    }

    @Test
    fun formatDate_customPattern() {
        val date = Date(1705315800000L)
        val result = TimeUtils.formatDate(date, "yyyy-MM-dd")
        assertNotNull(result)
        assert(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}
