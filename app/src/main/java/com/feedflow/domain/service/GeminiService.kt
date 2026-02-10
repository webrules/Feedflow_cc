package com.feedflow.domain.service

import com.feedflow.data.local.preferences.PreferencesManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private var model: GenerativeModel? = null
    private var currentApiKey: String? = null

    private suspend fun getModel(): GenerativeModel? {
        val apiKey = preferencesManager.geminiApiKey.first()

        if (apiKey.isNullOrBlank()) {
            return null
        }

        // Recreate model if API key changed
        if (apiKey != currentApiKey) {
            currentApiKey = apiKey
            model = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 1024
                }
            )
        }

        return model
    }

    suspend fun generateSummary(content: String): String {
        val generativeModel = getModel()
            ?: throw Exception("Gemini API key not configured. Please set your API key in Settings.")

        val truncatedContent = if (content.length > 10000) {
            content.take(10000)
        } else {
            content
        }

        val prompt = """
            You are a helpful assistant. Please summarize the following forum discussion
            clearly and concisely. Identify the main topic, key arguments or points made,
            and the general sentiment if applicable.

            Content:
            $truncatedContent
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        return response.text ?: throw Exception("Failed to generate summary")
    }

    suspend fun generateDailySummary(articles: List<Pair<String, String>>): String {
        val generativeModel = getModel()
            ?: throw Exception("Gemini API key not configured. Please set your API key in Settings.")

        if (articles.isEmpty()) {
            throw Exception("No articles to summarize")
        }

        val articlesText = articles.take(30).joinToString("\n\n") { (title, snippet) ->
            "**$title**\n$snippet"
        }

        val prompt = """
            You are a helpful assistant. Please create a daily briefing summary of the following
            RSS feed articles from the last 24 hours. Organize them by topic, highlight the most
            important stories, and provide a brief overview of each.

            Format your response in Markdown.

            Articles:
            $articlesText
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        return response.text ?: throw Exception("Failed to generate daily summary")
    }

    suspend fun isConfigured(): Boolean {
        val apiKey = preferencesManager.geminiApiKey.first()
        return !apiKey.isNullOrBlank()
    }
}
