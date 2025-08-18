package dev.shiftsad.shiftAssistant.service

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import dev.shiftsad.shiftAssistant.gateway.OpenAIGateway
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import dev.shiftsad.shiftAssistant.store.MessageHistoryStore
import dev.shiftsad.shiftAssistant.util.Placeholders
import org.bukkit.entity.Player

class CompletionService(
    private val history: MessageHistoryStore,
    private val retrievalController: RetrievalController
) {
    suspend fun complete(
        player: Player,
        message: String,
    ): String {
        val config = ConfigHolder.get()

        val gateway = OpenAIGateway(
            config = config.openAI,
            client = OpenAIHolder.get()
        )

        val knowledge = retrievalController.search(query = message)
        val knowledgeText = knowledge.joinToString("\n") { it.text }

        val systemPrompt = config.prompt.basePrompt
            .replace("{{knowledge}}", knowledgeText)
            .replace("{{extra_prompt}}", Placeholders.apply(player, template = config.prompt.extraPrompt))

        val system = ChatMessage(role = ChatRole.System, content = systemPrompt)
        val prior = history.get(player.uniqueId)
        val userMessage = ChatMessage(role = ChatRole.User, content = message)

        val messages = buildList {
            add(system)
            addAll(prior)
            add(userMessage)
        }

        val result = gateway.chatCompletion(messages)

        val assistantText =
            result.text.ifBlank { "Desculpe, não sei responder." }.trim()

        history.append(
            playerId = player.uniqueId,
            message = userMessage
        )
        history.append(
            playerId = player.uniqueId,
            message = ChatMessage(role = ChatRole.Assistant, content = assistantText)
        )

        return assistantText
    }

    fun clearHistory(player: Player) {
        history.clear(player.uniqueId)
    }
}