package me.kooper.ghostcore.listeners

import me.kooper.ghostcore.GhostCore
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

}