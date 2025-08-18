package dev.shiftsad.shiftAssistant

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val openai: OpenAIConfig,
    val history: HistoryConfig,
    val retrieval: RetrievalConfig,
    val prompt: PromptConfig,
    val rateLimits: RateLimitsConfig
)

@Serializable
data class OpenAIConfig(
    val model: String,
    val embeddingsModel: String,
    val apiKey: String,
    val baseUrl: String,
    val timeoutMs: Int,
    val reasoningEffort: String, // low, medium, high
    val temperature: Double, // 0 a 1
    val maxCompletionTokens: Int,
    val user: String
)

@Serializable
data class HistoryConfig(
    val maxMessages: Int,
    val maxTokens: Int,
    val maxAgeSeconds: Long,
    val cleanupIntervalSeconds: Int
)

@Serializable
data class RetrievalConfig(
    val topK: Int,
    val minCosine: Double,
    val maxChunkChars: Int
)

@Serializable
data class PromptConfig(
    val basePrompt: String,
    val extraPrompt: String
)

@Serializable
data class RateLimitsConfig(
    val perPlayerQpm: Int,
    val perServerQpm: Int
)