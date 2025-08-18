package dev.shiftsad.shiftAssistant.gateway

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.Effort
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.shiftsad.shiftAssistant.OpenAIConfig

class OpenAIGateway(
    private val config: OpenAIConfig,
    private val client: OpenAI
) {

    suspend fun chatCompletion(messages: List<ChatMessage>): Result {

        val request = ChatCompletionRequest(
            model = ModelId(config.model),
            messages = messages,
            reasoningEffort = Effort(id = config.reasoningEffort),
            temperature = config.temperature,
            maxCompletionTokens = config.maxCompletionTokens,
            user = config.user
        )

        val completion = client.chatCompletion(request)
        val choice = completion.choices.firstOrNull()

        return Result(
            text = choice?.message?.content?.trim().orEmpty(),
            usage = completion.usage?.let {
                Usage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens
                )
            }
        )
    }

    data class Result(
        val text: String,
        val usage: Usage?
    )

    data class Usage(
        val promptTokens: Int?,
        val completionTokens: Int?,
        val totalTokens: Int?
    )
}