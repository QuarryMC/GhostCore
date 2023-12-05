package me.kooper.ghostcore.events

import me.kooper.ghostcore.data.AudienceData
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class LeaveStageEvent(player: Player, audience: AudienceData) : Event() {

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        private fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = LeaveStageEvent.HANDLERS

}