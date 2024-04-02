package me.kooper.ghostcore.listeners

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.events.GhostBreakEvent
import me.kooper.ghostcore.models.ChunkedView
import me.kooper.ghostcore.utils.SchedulerAssist
import me.kooper.ghostcore.utils.blocks.blocksToSend
import me.kooper.ghostcore.utils.blocks.unchunkedMultiBlockChange
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import org.codehaus.plexus.util.FastMap

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
        if (event.tickNumber % 150L == 0L) {
                for (player in Bukkit.getOnlinePlayers()) {
                    if (!player.hasMetadata("broke")) {
                        continue
                    }
                    SchedulerAssist {
                        // Get stage the player is viewing
                        val stage = GhostCore.getInstance().stageManager.getStages(player).firstOrNull() ?: return@SchedulerAssist
                        val view = stage.views["mine"] ?: return@SchedulerAssist
                        val chunked = view as ChunkedView

                        // Resync blocks
                        player.unchunkedMultiBlockChange(chunked.getAllBlocksInBound())
                        player.removeMetadata("broke", GhostCore.getInstance())
                        Thread.sleep(5L)
                    }.runAsync()
                }
        }
    }

    @EventHandler
    private fun onBreak(event: GhostBreakEvent) {
        val player = event.player
        player.setMetadata("broke", FixedMetadataValue(GhostCore.getInstance(), true))
    }

}