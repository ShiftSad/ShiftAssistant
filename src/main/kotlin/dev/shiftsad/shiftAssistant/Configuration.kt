package dev.shiftsad.shiftAssistant

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val openAI: OpenAIConfig,
    val retrieval: RetrievalConfig,
    val prompt: PromptConfig,
    val rateLimits: RateLimitsConfig
)

@Serializable
data class OpenAIConfig(
    val model: String,
    val embeddingsModel: String,
    val baseUrl: String,
    val apiKey: String,
    val timeoutMs: Int
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
    val livePrompt: String
)

@Serializable
data class RateLimitsConfig(
    val perPlayerQpm: Int,
    val perServerQpm: Int
)