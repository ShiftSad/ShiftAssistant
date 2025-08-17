package dev.shiftsad.shiftAssistant.gateway

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.Effort
import com.aallam.openai.api.model.ModelId
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder

class OpenAIGateway {

    suspend fun chatCompletion(messages: List<ChatMessage>): Result {
        val cfg = ConfigHolder.get().openAI
        val client = OpenAIHolder.get()

        val request = ChatCompletionRequest(
            model = ModelId(cfg.model),
            messages = messages,
            reasoningEffort = Effort(id = cfg.reasoningEffort),
            temperature = cfg.temperature,
            maxCompletionTokens = cfg.maxCompletionTokens,
            user = cfg.user
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