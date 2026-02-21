package com.feedflow.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GBKUtilsTest {

    @Test
    fun encodeToGBK_asciiChars_preserved() {
        val input = "hello123"
        val result = GBKUtils.encodeToGBK(input)
        assertEquals("hello123", result)
    }

    @Test
    fun encodeToGBK_specialChars_percentEncoded() {
        val input = "hello world"
        val result = GBKUtils.encodeToGBK(input)
        assertTrue(result.contains("%20"))
    }

    @Test
    fun encodeToGBK_chineseChars_encoded() {
        val input = "你好"
        val result = GBKUtils.encodeToGBK(input)
        assertTrue(result.contains("%"))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun decodeFromGBK_validBytes_returnsString() {
        val bytes = byteArrayOf(0x68, 0x65, 0x6c, 0x6c, 0x6f)
        val result = GBKUtils.decodeFromGBK(bytes)
        assertEquals("hello", result)
    }

    @Test
    fun smartDecode_gbkBytes_returnsGbkString() {
        val gbkHello = byteArrayOf(0xC4.toByte(), 0xE3.toByte(), 0xBA.toByte(), 0xC3.toByte())
        val result = GBKUtils.smartDecode(gbkHello)
        assertEquals("你好", result)
    }

    @Test
    fun smartDecode_utf8Bytes_returnsUtf8String() {
        val utf8Hello = "hello".toByteArray(Charsets.UTF_8)
        val result = GBKUtils.smartDecode(utf8Hello)
        assertEquals("hello", result)
    }

    @Test
    fun encodeFormData_singleParam_encoded() {
        val params = mapOf("key" to "value")
        val result = GBKUtils.encodeFormData(params)
        assertEquals("key=value", result)
    }

    @Test
    fun encodeFormData_multipleParams_joinedWithAmpersand() {
        val params = mapOf("key1" to "value1", "key2" to "value2")
        val result = GBKUtils.encodeFormData(params)
        assertTrue(result.contains("key1=value1"))
        assertTrue(result.contains("key2=value2"))
        assertTrue(result.contains("&"))
    }

    @Test
    fun encodeFormData_chineseValue_encoded() {
        val params = mapOf("content" to "测试")
        val result = GBKUtils.encodeFormData(params)
        assertTrue(result.startsWith("content="))
        assertTrue(result.contains("%"))
    }

    @Test
    fun encodeToGBK_emptyString_returnsEmpty() {
        val result = GBKUtils.encodeToGBK("")
        assertEquals("", result)
    }

    @Test
    fun encodeFormData_emptyParams_returnsEmpty() {
        val result = GBKUtils.encodeFormData(emptyMap())
        assertEquals("", result)
    }
}
