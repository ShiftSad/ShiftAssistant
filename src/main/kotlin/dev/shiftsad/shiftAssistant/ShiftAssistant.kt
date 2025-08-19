package dev.shiftsad.shiftAssistant

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.shiftsad.shiftAssistant.commands.AdminCommand
import dev.shiftsad.shiftAssistant.commands.UserCommand
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import dev.shiftsad.shiftAssistant.holder.RateLimitHolder
import dev.shiftsad.shiftAssistant.service.CompletionService
import dev.shiftsad.shiftAssistant.service.RateLimitService
import dev.shiftsad.shiftAssistant.store.MessageHistoryStore
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File

class ShiftAssistant : JavaPlugin() {

    private lateinit var retrievalController: RetrievalController
    private lateinit var completionService: CompletionService
    private lateinit var rateLimitCleanupTask: BukkitTask

    override fun onEnable() {
        CommandAPI.onEnable()

        dataFolder.mkdirs()
        saveDefaultConfig()
        val config = YamlConfigLoader.load(File(dataFolder, "config.yml").toPath())
        ConfigHolder.set(config = config)
        OpenAIHolder.initFromConfig(config = config)

        val rateLimitService = RateLimitService()
        RateLimitHolder.set(rateLimitService)

        rateLimitCleanupTask = server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            rateLimitService.cleanup()
        }, 20L * 60L * 5L, 20L * 60L * 5L)

        val indexDir = dataFolder.toPath().resolve("embeddings")
        indexDir.toFile().mkdirs()
        retrievalController = RetrievalController(indexDir)

        completionService = CompletionService(
            history = MessageHistoryStore(),
            retrievalController = retrievalController
        )

        val adminCommand = AdminCommand(retrievalController, dataFolder)
        adminCommand.get().register(this)
        val userCommand = UserCommand(completionService)
        userCommand.get().register(this)

        logger.info("ShiftAssistant enabled successfully!")
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this))
    }

    override fun onDisable() {
        if (::rateLimitCleanupTask.isInitialized) {
            rateLimitCleanupTask.cancel()
        }

        if (::retrievalController.isInitialized) {
            retrievalController.close()
        }
        CommandAPI.onDisable()
        logger.info("ShiftAssistant disabled successfully!")
    }
}
