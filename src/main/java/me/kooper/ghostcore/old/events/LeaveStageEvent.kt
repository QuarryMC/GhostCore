package me.kooper.ghostcore.old.events

import me.kooper.ghostcore.old.models.Stage
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.*

class LeaveStageEvent(val player: Player, val audience: ArrayList<UUID>, val stage: Stage) : Event() {

    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        private fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = LeaveStageEvent.HANDLERS

}