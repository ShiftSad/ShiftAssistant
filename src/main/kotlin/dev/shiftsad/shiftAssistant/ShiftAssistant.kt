package dev.shiftsad.shiftAssistant

import dev.shiftsad.shiftAssistant.holder.ConfigHolder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ShiftAssistant : JavaPlugin() {

    override fun onEnable() {
        dataFolder.mkdirs()
        saveDefaultConfig()
        ConfigHolder.set(newConfig = YamlConfigLoader.load(File(dataFolder, "config.yml").toPath()))
    }
}
