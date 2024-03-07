package me.kooper.ghostcore.listeners

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kooper.ghostcore.GhostCore
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class StageListeners : Listener {

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        if (GhostCore.instance.stageManager.spectatorPrevStages[event.player.uniqueId] != null) {
            GhostCore.instance.stageManager.spectatorPrevStages.remove(event.player.uniqueId)
        }
    }

    @EventHandler
    fun onServerTick(event: ServerTickStartEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                for (stage in GhostCore.instance.stageManager.stages.values) {
                    for (view in stage.views) {
                        stage.sendBlocks(view.value.blocksChange)
                        view.value.blocksChange.clear()
                    }
                }
            }
        })
    }

}