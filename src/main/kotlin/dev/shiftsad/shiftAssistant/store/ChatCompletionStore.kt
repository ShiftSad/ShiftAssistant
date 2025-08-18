package dev.shiftsad.shiftAssistant.store

import com.aallam.ktoken.Tokenizer
import com.aallam.openai.api.chat.ChatMessage
import dev.shiftsad.shiftAssistant.HistoryConfig
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MessageHistoryStore {

    private val histories = ConcurrentHashMap<UUID, ArrayDeque<ChatMessage>>()
    private val timestamps = ConcurrentHashMap<UUID, ArrayDeque<Long>>()

    init {
        val cleanupInterval = getHistoryConfig().cleanupIntervalSeconds * 1000L

        kotlin.concurrent.fixedRateTimer(
            name = "MessageHistoryCleanup",
            initialDelay = cleanupInterval,
            period = cleanupInterval
        ) {
            histories.keys.forEach { playerId ->
                cleanupOldMessages(playerId)
                kotlinx.coroutines.runBlocking {
                    cleanupHighTokenMessages(playerId)
                }
            }
        }
    }

    private fun getHistoryConfig(): HistoryConfig {
        return ConfigHolder.get().history
    }

    fun get(playerId: UUID): List<ChatMessage> {
        val q = histories[playerId] ?: return emptyList()
        cleanupOldMessages(playerId)
        return q.toList()
    }

    fun append(playerId: UUID, message: ChatMessage) {
        val q = histories.computeIfAbsent(playerId) { ArrayDeque() }
        val timeQ = timestamps.computeIfAbsent(playerId) { ArrayDeque() }
        val currentTime = System.currentTimeMillis()

        q.addLast(message)
        timeQ.addLast(currentTime)

        val config = getHistoryConfig()

        // Remove messages exceeding max count
        while (q.size > config.maxMessages) {
            q.removeFirst()
            timeQ.removeFirst()
        }
    }

    fun replaceAll(playerId: UUID, messages: List<ChatMessage>) {
        val config = getHistoryConfig()
        val messagesToKeep = messages.takeLast(config.maxMessages)
        val currentTime = System.currentTimeMillis()

        val q = ArrayDeque<ChatMessage>(messagesToKeep.size)
        val timeQ = ArrayDeque<Long>(messagesToKeep.size)

        for (m in messagesToKeep) {
            q.addLast(m)
            timeQ.addLast(currentTime)
        }

        histories[playerId] = q
        timestamps[playerId] = timeQ
    }

    private fun cleanupOldMessages(playerId: UUID) {
        val q = histories[playerId] ?: return
        val timeQ = timestamps[playerId] ?: return
        val config = getHistoryConfig()
        val cutoffTime = System.currentTimeMillis() - (config.maxAgeSeconds * 1000)

        while (timeQ.isNotEmpty() && timeQ.first() < cutoffTime) {
            q.removeFirst()
            timeQ.removeFirst()
        }
    }

    private suspend fun cleanupHighTokenMessages(playerId: UUID) {
        val q = histories[playerId] ?: return
        val timestamps = timestamps[playerId] ?: return
        val config = getHistoryConfig()
        val tokenizer = Tokenizer.of(model = "gpt-4")

        var totalTokens = 0
        while (q.isNotEmpty()) {
            val message = q.first()
            val tokenCount = tokenizer.encode(text = message.content ?: "").size

            if (totalTokens + tokenCount > config.maxTokens) {
                q.removeFirst()
                timestamps.removeFirst()
            } else totalTokens += tokenCount
        }
    }

    fun clear(playerId: UUID) {
        histories.remove(playerId)
        timestamps.remove(playerId)
    }

    fun clearAll() {
        histories.clear()
        timestamps.clear()
    }
}