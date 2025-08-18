package dev.shiftsad.shiftAssistant.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Placeholders {

    private val placeholderApiEnabled: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
    }

    fun apply(player: Player, template: String): String {
        if (!placeholderApiEnabled) return template
        return PlaceholderAPI.setPlaceholders(player, template)
    }
}