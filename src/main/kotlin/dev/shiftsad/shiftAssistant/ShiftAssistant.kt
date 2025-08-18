package dev.shiftsad.shiftAssistant

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.shiftsad.shiftAssistant.commands.AdminCommand
import dev.shiftsad.shiftAssistant.controller.RetrievalController
import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ShiftAssistant : JavaPlugin() {

    private lateinit var retrievalController: RetrievalController
    private lateinit var adminCommand: AdminCommand

    override fun onEnable() {
        CommandAPI.onEnable()

        dataFolder.mkdirs()
        saveDefaultConfig()
        val config = YamlConfigLoader.load(File(dataFolder, "config.yml").toPath())
        ConfigHolder.set(config = config)
        OpenAIHolder.initFromConfig(config = config)

        val indexDir = dataFolder.toPath().resolve("embeddings")
        indexDir.toFile().mkdirs()
        retrievalController = RetrievalController(indexDir)

        // Initialize and register the admin command
        adminCommand = AdminCommand(retrievalController, dataFolder)
        adminCommand.get().register()

        logger.info("ShiftAssistant enabled successfully!")
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this))
    }

    override fun onDisable() {
        if (::retrievalController.isInitialized) {
            retrievalController.close()
        }
        CommandAPI.onDisable()
        logger.info("ShiftAssistant disabled successfully!")
    }
}
