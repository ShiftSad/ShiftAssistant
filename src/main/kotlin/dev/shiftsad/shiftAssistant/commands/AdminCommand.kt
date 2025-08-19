package dev.shiftsad.shiftAssistant.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.shiftsad.shiftAssistant.YamlConfigLoader
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.bukkit.command.CommandSender
import java.io.File

class AdminCommand(private val retrievalController: RetrievalController, private val dataFolder: File) {

    companion object {
        private const val PERMISSION_ADMIN = "shiftassistant.admin"
        private val coroutineScope = CoroutineScope(Dispatchers.IO)
    }

    fun get(): CommandAPICommand {
        return CommandAPICommand("shiftassistant")
            .withPermission(PERMISSION_ADMIN)
            .withSubcommands(
                buildEmbeddingsCommand(),
                buildReloadCommand(),
            )
    }

    private fun buildEmbeddingsCommand(): CommandAPICommand {
        return CommandAPICommand("embeddings")
            .withSubcommands(
                CommandAPICommand("generate")
                    .executes(CommandExecutor { sender, _ ->
                        generateEmbeddings(sender)
                    }),

                CommandAPICommand("search")
                    .withArguments(TextArgument("query"))
                    .withOptionalArguments(IntegerArgument("limit", 1, 20))
                    .executes(CommandExecutor { sender, args ->
                        val query = args.get("query") as String
                        val limit = args.getOptional("limit").orElse(5) as Int
                        searchEmbeddings(sender, query, limit)
                    }),

                CommandAPICommand("clear")
                    .executes(CommandExecutor { sender, _ ->
                        retrievalController.clearIndex()
                        sender.sendMessage("Embeddings index cleared.")
                    })
            )
    }

    private fun buildReloadCommand(): CommandAPICommand {
        return CommandAPICommand("reload")
            .executes(CommandExecutor { sender, _ ->
                reloadConfig(sender)
            })
    }

    private fun generateEmbeddings(sender: CommandSender) {
        val docs = dataFolder.resolve("docs")
        docs.mkdirs()
        val files = docs.listFiles() ?: return

        coroutineScope.launch {
            val semaphore = Semaphore(5)
            var totalChunks = 0

            val jobs = files.map { file ->
                async {
                    semaphore.withPermit {
                        try {
                            val text = file.readText()
                            val chunks = retrievalController.ingest(
                                sourceId = file.nameWithoutExtension,
                                text = text
                            )
                            synchronized(this) {
                                totalChunks += chunks
                            }
                            sender.sendMessage("✅ Processed: ${file.name} ($chunks chunks)")
                        } catch (e: Exception) {
                            sender.sendMessage("⚠️ Error processing ${file.name}: ${e.message}")
                        }
                    }
                }
            }

            jobs.awaitAll()
            sender.sendMessage(
                "✨ Embeddings for ${files.size} files generated, amount of chunks: $totalChunks"
            )
        }
    }

    private fun searchEmbeddings(sender: CommandSender, query: String, limit: Int) {
        coroutineScope.launch {
            val results = retrievalController.search(query).sortedByDescending { it.score }
            if (results.isEmpty()) {
                sender.sendMessage("No results found for query: $query")
                return@launch
            }

            sender.sendMessage("Search results for '$query':")
            for (i in results.indices.take(limit)) {
                val result = results[i]
                sender.sendMessage("${i + 1}. [${result.score}] ${result.text} (ID: ${result.id})")
            }
        }
    }

    private fun reloadConfig(sender: CommandSender) {
        try {
            val configFile = File(dataFolder, "config.yml")
            if (configFile.exists()) {
                val config = YamlConfigLoader.load(configFile.toPath())
                ConfigHolder.set(config)
                OpenAIHolder.initFromConfig(config)
                sender.sendMessage("Configuration reloaded successfully.")
            } else sender.sendMessage("Configuration file not found.")
        } catch (e: Exception) {
            sender.sendMessage("Error reloading configuration: ${e.message}")
        }
    }
}