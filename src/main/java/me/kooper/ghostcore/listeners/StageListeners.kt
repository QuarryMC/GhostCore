package me.kooper.ghostcore.listeners

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.events.*
import me.kooper.ghostcore.models.ChunkedView
import me.kooper.ghostcore.utils.SchedulerAssist
import me.kooper.ghostcore.utils.blocks.blocksToSend
import me.kooper.ghostcore.utils.blocks.unchunkedMultiBlockChange
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class StageListeners : Listener {

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        if (GhostCore.getInstance().stageManager.spectatorPrevStages[event.player.uniqueId] != null) {
            GhostCore.getInstance().stageManager.spectatorPrevStages.remove(event.player.uniqueId)
        }

        if (blocksToSend[event.player.uniqueId] != null) blocksToSend.remove(event.player.uniqueId)
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        if (blocksToSend[event.player.uniqueId] != null) blocksToSend.remove(event.player.uniqueId)
    }

    @EventHandler
    private fun onTick(event: ServerTickStartEvent) {
        if (event.tickNumber % 60L == 0L) {
            for (player in Bukkit.getOnlinePlayers()) {
                // Get stage the player is viewing
                val stage = GhostCore.getInstance().stageManager.spectators[player.uniqueId] ?: continue
                val view = stage.views["mine"] ?: continue
                val chunked = view as ChunkedView

                // Resync blocks
                player.unchunkedMultiBlockChange(chunked.getAllBlocksInBound())
            }
        }
    }

}