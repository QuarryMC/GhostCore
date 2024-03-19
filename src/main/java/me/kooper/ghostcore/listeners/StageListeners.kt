package me.kooper.ghostcore.listeners

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.models.ChunkedView
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class StageListeners : Listener {

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        if (GhostCore.getInstance().stageManager.spectatorPrevStages[event.player.uniqueId] != null) {
            GhostCore.getInstance().stageManager.spectatorPrevStages.remove(event.player.uniqueId)
        }
    }

    @EventHandler
    fun onServerTick(event: ServerTickStartEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.getInstance(), Runnable {
            run {
                for (stage in GhostCore.getInstance().stageManager.stages.values) {
                    for (view in stage.views) {
                        val chunkedView = view.value as ChunkedView
                        if (chunkedView.changedBlocks.isNotEmpty()) {
                            stage.sendBlocks(chunkedView.changedBlocks)
                            chunkedView.changedBlocks.clear()
                        }
                    }
                }
            }
        })
    }

}