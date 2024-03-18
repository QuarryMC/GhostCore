package me.kooper.ghostcore

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.kooper.ghostcore.old.commands.GhostCommand
import me.kooper.ghostcore.old.listeners.StageListeners
import me.kooper.ghostcore.old.managers.StageManager
import me.kooper.ghostcore.old.packets.PacketListener
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin


class GhostCore : JavaPlugin() {

    lateinit var stageManager: StageManager
    lateinit var instaBreak: List<Material>

    companion object {
        fun getInstance(): GhostCore {
            return getPlugin(GhostCore::class.java)
        }
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(false)
            .checkForUpdates(true)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        saveDefaultConfig()
        instaBreak = config.getStringList("instabreak").map { s -> Material.valueOf(s) }
        stageManager = StageManager()
        PacketEvents.getAPI().eventManager.registerListener(PacketListener())
        PacketEvents.getAPI().init()
        server.getPluginCommand("ghost")!!.setExecutor(GhostCommand())
        server.pluginManager.registerEvents(StageListeners(), this)
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }
}
