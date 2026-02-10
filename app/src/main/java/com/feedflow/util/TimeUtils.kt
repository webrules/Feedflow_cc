package com.feedflow.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val iso8601FormatAlt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val rfc822Format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    private val rfc822FormatAlt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)

    fun calculateTimeAgo(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "${seconds}s"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            days < 30 -> "${days / 7}w"
            days < 365 -> "${days / 30}mo"
            else -> "${days / 365}y"
        }
    }

    fun calculateTimeAgo(timestamp: Long): String {
        return calculateTimeAgo(Date(timestamp * 1000))
    }

    fun calculateTimeAgo(dateString: String): String {
        val date = parseDate(dateString) ?: return "Recent"
        return calculateTimeAgo(date)
    }

    fun parseDate(dateString: String): Date? {
        return try {
            iso8601Format.parse(dateString)
        } catch (e: Exception) {
            try {
                iso8601FormatAlt.parse(dateString)
            } catch (e: Exception) {
                try {
                    rfc822Format.parse(dateString)
                } catch (e: Exception) {
                    try {
                        rfc822FormatAlt.parse(dateString)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    fun parseUnixTimestamp(timestamp: Int): Date {
        return Date(timestamp * 1000L)
    }

    fun formatDate(date: Date, pattern: String = "MMM d, yyyy"): String {
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(date)
    }
}
