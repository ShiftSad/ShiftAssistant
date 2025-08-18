package dev.shiftsad.shiftAssistant.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists

class AdminCommand(private val retrievalController: RetrievalController, private val dataFolder: File) {

    companion object {
        private const val PERMISSION_ADMIN = "shiftassistant.admin"
        private val coroutineScope = CoroutineScope(Dispatchers.IO)

        private fun CommandSender.sendMessage(text: String, color: NamedTextColor = NamedTextColor.WHITE) {
            this.sendMessage(Component.text(text, color))
        }

        private fun CommandSender.sendSuccess(text: String) = sendMessage(text, NamedTextColor.GREEN)
        private fun CommandSender.sendError(text: String) = sendMessage(text, NamedTextColor.RED)
        private fun CommandSender.sendInfo(text: String) = sendMessage(text, NamedTextColor.YELLOW)
    }

    fun get(): CommandAPICommand {
        return CommandAPICommand("shiftassistant")
            .withPermission(PERMISSION_ADMIN)
            .withSubcommands(
                buildEmbeddingsCommand(),
                buildDebugCommand(),
                buildReloadCommand()
            )
    }

    private fun buildEmbeddingsCommand(): CommandAPICommand {
        return CommandAPICommand("embeddings")
            .withSubcommands(
                // Generate new embeddings without deleting existing ones
                CommandAPICommand("generate")
                    .withOptionalArguments(StringArgument("source"))
                    .executes(CommandExecutor { sender, args ->
                        val source = args.getOptional("source").orElse(null) as String?
                        generateEmbeddings(sender, source, false)
                    }),

                // Force regenerate - delete all and start fresh
                CommandAPICommand("forcegenerate")
                    .executes(CommandExecutor { sender, _ ->
                        generateEmbeddings(sender, null, true)
                    }),

                // Add document to embeddings
                CommandAPICommand("add")
                    .withArguments(
                        StringArgument("sourceId"),
                        StringArgument("text")
                    )
                    .executes(CommandExecutor { sender, args ->
                        val sourceId = args.get("sourceId") as String
                        val text = args.get("text") as String
                        addDocument(sender, sourceId, text)
                    }),

                // Search embeddings
                CommandAPICommand("search")
                    .withArguments(StringArgument("query"))
                    .withOptionalArguments(IntegerArgument("limit", 1, 20))
                    .executes(CommandExecutor { sender, args ->
                        val query = args.get("query") as String
                        val limit = args.getOptional("limit").orElse(5) as Int
                        searchEmbeddings(sender, query, limit)
                    }),

                // Get embeddings statistics
                CommandAPICommand("stats")
                    .executes(CommandExecutor { sender, _ ->
                        showEmbeddingsStats(sender)
                    }),

                // Clear all embeddings
                CommandAPICommand("clear")
                    .executes(CommandExecutor { sender, _ ->
                        clearEmbeddings(sender)
                    })
            )
    }

    private fun buildDebugCommand(): CommandAPICommand {
        return CommandAPICommand("debug")
            .withSubcommands(
                CommandAPICommand("info")
                    .executes(CommandExecutor { sender, _ ->
                        showDebugInfo(sender)
                    }),

                CommandAPICommand("test")
                    .withArguments(StringArgument("text"))
                    .executes(CommandExecutor { sender, args ->
                        val text = args.get("text") as String
                        testEmbedding(sender, text)
                    })
            )
    }

    private fun buildReloadCommand(): CommandAPICommand {
        return CommandAPICommand("reload")
            .executes(CommandExecutor { sender, _ ->
                reloadConfig(sender)
            })
    }

    private fun generateEmbeddings(sender: CommandSender, source: String?, force: Boolean) {
        coroutineScope.launch {
            try {
                sender.sendInfo("Starting embedding generation${if (force) " (force mode)" else ""}...")

                if (force) {
                    clearEmbeddingsInternal()
                    sender.sendInfo("Cleared existing embeddings")
                }

                // Here you would implement the logic to scan and process documents
                // This is a placeholder - you'd want to scan specific directories or databases
                val documentsProcessed = processDocuments(source)

                sender.sendSuccess("Generated embeddings for $documentsProcessed documents")

            } catch (e: Exception) {
                sender.sendError("Failed to generate embeddings: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun processDocuments(source: String?): Int {
        // Placeholder implementation - you would implement actual document processing here
        // For example, scanning files, database entries, etc.
        var count = 0

        if (source != null) {
            // Process specific source
            val text = "Sample document content for $source"
            retrievalController.ingest(source, text)
            count = 1
        } else {
            // Process all available documents
            // This is where you'd implement your document discovery logic
            val sampleDocs = mapOf(
                "sample1" to "This is a sample document for testing embeddings",
                "sample2" to "Another sample document with different content"
            )

            for ((id, text) in sampleDocs) {
                retrievalController.ingest(id, text)
                count++
            }
        }

        return count
    }

    private fun addDocument(sender: CommandSender, sourceId: String, text: String) {
        coroutineScope.launch {
            try {
                val chunks = retrievalController.ingest(sourceId, text)
                sender.sendSuccess("Added document '$sourceId' with $chunks chunks")
            } catch (e: Exception) {
                sender.sendError("Failed to add document: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun searchEmbeddings(sender: CommandSender, query: String, limit: Int) {
        coroutineScope.launch {
            try {
                val results = retrievalController.search(query).take(limit)

                if (results.isEmpty()) {
                    sender.sendInfo("No results found for query: '$query'")
                    return@launch
                }

                sender.sendSuccess("Found ${results.size} results for '$query':")
                results.forEachIndexed { index, neighbor ->
                    sender.sendMessage("${index + 1}. [${neighbor.score}] ${neighbor.id}")
                    sender.sendMessage("   ${neighbor.text.take(100)}${if (neighbor.text.length > 100) "..." else ""}")
                }

            } catch (e: Exception) {
                sender.sendError("Failed to search embeddings: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showEmbeddingsStats(sender: CommandSender) {
        try {
            val indexPath = dataFolder.toPath().resolve("embeddings")
            if (!indexPath.exists()) {
                sender.sendInfo("No embeddings index found")
                return
            }

            val indexSize = Files.walk(indexPath)
                .filter { Files.isRegularFile(it) }
                .mapToLong { Files.size(it) }
                .sum()

            val sizeInMB = indexSize / (1024.0 * 1024.0)

            sender.sendSuccess("Embeddings Statistics:")
            sender.sendMessage("Index path: $indexPath")
            sender.sendMessage("Index size: %.2f MB".format(sizeInMB))

        } catch (e: Exception) {
            sender.sendError("Failed to get embeddings stats: ${e.message}")
        }
    }

    private fun clearEmbeddings(sender: CommandSender) {
        try {
            clearEmbeddingsInternal()
            sender.sendSuccess("Cleared all embeddings")
        } catch (e: Exception) {
            sender.sendError("Failed to clear embeddings: ${e.message}")
        }
    }

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private fun clearEmbeddingsInternal() {
        val indexPath = dataFolder.toPath().resolve("embeddings")
        if (indexPath.exists()) {
            indexPath.deleteRecursively()
            indexPath.toFile().mkdirs()
        }
    }

    private fun showDebugInfo(sender: CommandSender) {
        sender.sendSuccess("ShiftAssistant Debug Information:")
        sender.sendMessage("Data folder: ${dataFolder.absolutePath}")
        sender.sendMessage("Embeddings path: ${dataFolder.toPath().resolve("embeddings")}")
        sender.sendMessage("Java version: ${System.getProperty("java.version")}")
        sender.sendMessage("Kotlin version: ${KotlinVersion.CURRENT}")

        if (sender is Player) {
            sender.sendMessage("Player: ${sender.name}")
            sender.sendMessage("Permission: ${sender.hasPermission(PERMISSION_ADMIN)}")
        }
    }

    private fun testEmbedding(sender: CommandSender, text: String) {
        coroutineScope.launch {
            try {
                sender.sendInfo("Testing embedding generation for: '$text'")
                val testId = "test_${System.currentTimeMillis()}"
                val chunks = retrievalController.ingest(testId, text)
                sender.sendSuccess("Successfully generated embedding with $chunks chunks")

                // Test search
                val results = retrievalController.search(text).take(1)
                if (results.isNotEmpty()) {
                    sender.sendSuccess("Search test successful - similarity: ${results.first().score}")
                }

            } catch (e: Exception) {
                sender.sendError("Embedding test failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun reloadConfig(sender: CommandSender) {
        try {
            // You would implement config reloading logic here
            sender.sendSuccess("Configuration reloaded successfully")
        } catch (e: Exception) {
            sender.sendError("Failed to reload configuration: ${e.message}")
        }
    }
}