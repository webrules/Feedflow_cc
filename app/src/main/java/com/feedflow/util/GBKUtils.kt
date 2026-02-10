package com.feedflow.util

import java.nio.charset.Charset

object GBKUtils {
    private val GBK_CHARSET: Charset = Charset.forName("GBK")

    /**
     * Encode string to GBK URL-encoded format.
     */
    fun encodeToGBK(text: String): String {
        val bytes = text.toByteArray(GBK_CHARSET)
        val sb = StringBuilder()

        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            if (unsigned in 0x30..0x7A) {
                // Keep alphanumeric and some special chars as-is
                sb.append(unsigned.toChar())
            } else {
                // Percent-encode the byte
                sb.append("%%%02X".format(unsigned))
            }
        }

        return sb.toString()
    }

    /**
     * Decode GBK bytes to String.
     */
    fun decodeFromGBK(bytes: ByteArray): String {
        return String(bytes, GBK_CHARSET)
    }

    /**
     * Try to decode response bytes as GBK first, then fallback to UTF-8.
     */
    fun smartDecode(bytes: ByteArray): String {
        return try {
            String(bytes, GBK_CHARSET)
        } catch (e: Exception) {
            String(bytes, Charsets.UTF_8)
        }
    }

    /**
     * Encode form data for GBK POST requests.
     */
    fun encodeFormData(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "$key=${encodeToGBK(value)}"
        }
    }
}
