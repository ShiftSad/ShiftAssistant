package dev.shiftsad.shiftAssistant.service

import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RateLimitService {

    private val playerQueries = ConcurrentHashMap<UUID, MutableList<Long>>()
    private val serverQueries = mutableListOf<Long>()
    private val serverQueryCount = AtomicInteger(0)

    /**
     * Checks if a player can make a query based on rate limits
     * @param player The player attempting to make a query
     * @return true if the player is allowed to make a query, false if rate limited
     */
    fun canMakeQuery(player: Player): Boolean {
        val config = ConfigHolder.get().rateLimits
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60_000 // 60 seconds in milliseconds

        if (!canMakeServerQuery(oneMinuteAgo, config.perServerQpm)) {
            return false
        }

        return canMakePlayerQuery(player.uniqueId, oneMinuteAgo, config.perPlayerQpm)
    }

    /**
     * Records a query for a player and the server
     * @param player The player making the query
     */
    fun recordQuery(player: Player) {
        val currentTime = System.currentTimeMillis()

        playerQueries.computeIfAbsent(player.uniqueId) { mutableListOf() }.add(currentTime)
        synchronized(serverQueries) {
            serverQueries.add(currentTime)
            serverQueryCount.incrementAndGet()
        }
    }

    /**
     * Gets the remaining queries for a player in the current minute
     * @param player The player to check
     * @return Number of queries remaining for the player
     */
    fun getRemainingPlayerQueries(player: Player): Int {
        val config = ConfigHolder.get().rateLimits
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60_000

        val playerQueryList = playerQueries[player.uniqueId] ?: return config.perPlayerQpm

        synchronized(playerQueryList) {
            // Clean old queries
            playerQueryList.removeIf { it < oneMinuteAgo }
            return maxOf(0, config.perPlayerQpm - playerQueryList.size)
        }
    }

    /**
     * Gets the remaining queries for the server in the current minute
     * @return Number of queries remaining for the server
     */
    fun getRemainingServerQueries(): Int {
        val config = ConfigHolder.get().rateLimits
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60_000

        synchronized(serverQueries) {
            serverQueries.removeIf { it < oneMinuteAgo }
            return maxOf(0, config.perServerQpm - serverQueries.size)
        }
    }

    /**
     * Gets the time until the player can make another query
     * @param player The player to check
     * @return Time in milliseconds until the player can make another query, or 0 if they can query now
     */
    fun getTimeUntilNextPlayerQuery(player: Player): Long {
        val playerQueryList = playerQueries[player.uniqueId] ?: return 0
        val config = ConfigHolder.get().rateLimits

        synchronized(playerQueryList) {
            if (playerQueryList.size < config.perPlayerQpm) {
                return 0
            }

            val oldestQuery = playerQueryList.minOrNull() ?: return 0
            val timeUntilReset = (oldestQuery + 60_000) - System.currentTimeMillis()
            return maxOf(0, timeUntilReset)
        }
    }

    /**
     * Cleans up old query records to prevent memory leaks
     * Should be called periodically
     */
    fun cleanup() {
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60_000

        playerQueries.values.forEach { queryList ->
            synchronized(queryList) {
                queryList.removeIf { it < oneMinuteAgo }
            }
        }

        playerQueries.entries.removeIf { it.value.isEmpty() }
        synchronized(serverQueries) {
            serverQueries.removeIf { it < oneMinuteAgo }
        }
    }

    private fun canMakeServerQuery(oneMinuteAgo: Long, maxQueries: Int): Boolean {
        synchronized(serverQueries) {
            serverQueries.removeIf { it < oneMinuteAgo }
            return serverQueries.size < maxQueries
        }
    }

    private fun canMakePlayerQuery(playerId: UUID, oneMinuteAgo: Long, maxQueries: Int): Boolean {
        val playerQueryList = playerQueries.computeIfAbsent(playerId) { mutableListOf() }

        synchronized(playerQueryList) {
            playerQueryList.removeIf { it < oneMinuteAgo }
            return playerQueryList.size < maxQueries
        }
    }
}