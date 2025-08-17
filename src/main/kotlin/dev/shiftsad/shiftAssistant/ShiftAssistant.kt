package dev.shiftsad.shiftAssistant

import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import dev.shiftsad.shiftAssistant.holder.OpenAIHolder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ShiftAssistant : JavaPlugin() {

    override fun onEnable() {
        dataFolder.mkdirs()
        saveDefaultConfig()
        val config = YamlConfigLoader.load(File(dataFolder, "config.yml").toPath())
        ConfigHolder.set(config = config)
        OpenAIHolder.initFromConfig(config = config)
    }
}
