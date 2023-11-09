package me.kooper.ghostcore

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.kooper.ghostcore.listeners.ChunkListener
import me.kooper.ghostcore.listeners.PlayerJoinLeave
import me.kooper.ghostcore.managers.StageManager
import me.kooper.ghostcore.packets.PacketListener
import org.bukkit.plugin.java.JavaPlugin


class GhostCore : JavaPlugin() {

    lateinit var stageManager: StageManager

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
        stageManager = StageManager()
        PacketEvents.getAPI().eventManager.registerListener(PacketListener())
        PacketEvents.getAPI().init()
        server.pluginManager.registerEvents(ChunkListener(), this)
        server.pluginManager.registerEvents(PlayerJoinLeave(), this)
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }
}
