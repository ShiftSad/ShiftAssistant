package dev.shiftsad.shiftAssistant.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.shiftsad.shiftAssistant.holder.RateLimitHolder
import dev.shiftsad.shiftAssistant.service.CompletionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class UserCommand(
    private val completionService: CompletionService
) {

    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.IO)
    }

    fun get(): CommandAPICommand {
        val rateLimitService = RateLimitHolder.get()
        return CommandAPICommand("ask")
            .withArguments(TextArgument("query"))
            .executes(CommandExecutor { sender, args ->
                var canRunCommand = false
                if (sender is Player &&
                    rateLimitService.canMakeQuery(player = sender)
                ) canRunCommand = true

                if (!canRunCommand) {
                    val player = sender as? Player
                    if (player == null) {
                        sender.sendMessage("This command is on server wide cooldown.")
                        return@CommandExecutor
                    }

                    sender.sendMessage("You are on cooldown. You may only make another query in " +
                        "${rateLimitService.getTimeUntilNextPlayerQuery(player)} seconds.")
                    return@CommandExecutor
                }

                val query = args.get("query") as String
                rateLimitService.recordQuery(player = sender as Player)
                handleQuery(query, sender)
            })
    }

    private fun handleQuery(query: String, sender: CommandSender) {
        coroutineScope.launch {
            val response = completionService.complete(sender, query)
            sender.sendMessage("Assistant: $response")
        }
    }
}