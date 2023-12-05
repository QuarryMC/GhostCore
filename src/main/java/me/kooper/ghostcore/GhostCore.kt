package me.kooper.ghostcore

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.kooper.ghostcore.commands.GhostCommand
import me.kooper.ghostcore.managers.StageManager
import me.kooper.ghostcore.packets.PacketListener
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin


class GhostCore : JavaPlugin() {

    lateinit var stageManager: StageManager
    lateinit var instaBreak: List<Material>

    companion object {
        lateinit var instance: GhostCore
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(false)
            .checkForUpdates(true)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        instaBreak = config.getStringList("instabreak").map { s -> Material.valueOf(s) }
        stageManager = StageManager()
        PacketEvents.getAPI().eventManager.registerListener(PacketListener())
        PacketEvents.getAPI().init()
        server.getPluginCommand("ghost")!!.setExecutor(GhostCommand())
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }
}
