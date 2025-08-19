package dev.shiftsad.shiftAssistant.service

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import dev.shiftsad.shiftAssistant.gateway.OpenAIGateway
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import dev.shiftsad.shiftAssistant.store.MessageHistoryStore
import dev.shiftsad.shiftAssistant.util.Placeholders
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CompletionService(
    private val history: MessageHistoryStore,
    private val retrievalController: RetrievalController
) {
    suspend fun complete(
        sender: CommandSender,
        message: String,
    ): String {
        val config = ConfigHolder.get()
        val player = sender as? Player
        val gateway = OpenAIGateway(
            config = config.openai,
            client = OpenAIHolder.get()
        )

        val knowledge = retrievalController.search(query = message)
        val knowledgeText = knowledge.joinToString("\n") { it.text }

        val systemPrompt = if (player != null)
            config.prompt.basePrompt
                .replace("{{knowledge}}", knowledgeText)
                .replace("{{extra_prompt}}", Placeholders.apply(player, template = config.prompt.extraPrompt))
        else config.prompt.basePrompt.replace("{{knowledge}}", knowledgeText)

        val system = ChatMessage(role = ChatRole.System, content = systemPrompt)
        val userMessage = ChatMessage(role = ChatRole.User, content = message)

        val messages = buildList {
            add(system)
            if (player != null) addAll(history.get(player.uniqueId))
            add(userMessage)
        }

        val result = gateway.chatCompletion(messages)

        val assistantText =
            result.text.ifBlank { "Desculpe, não sei responder." }.trim()

        if (player != null) {
            history.append(
                playerId = player.uniqueId,
                message = userMessage
            )
            history.append(
                playerId = player.uniqueId,
                message = ChatMessage(role = ChatRole.Assistant, content = assistantText)
            )
        }

        return assistantText
    }

    fun clearHistory(player: Player) {
        history.clear(player.uniqueId)
    }
}