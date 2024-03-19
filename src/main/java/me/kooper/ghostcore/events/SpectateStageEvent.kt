package me.kooper.ghostcore.events

import me.kooper.ghostcore.models.Stage
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SpectateStageEvent(val spectator: Player, val stage: Stage, val isSpectating: Boolean) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        private fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS

}