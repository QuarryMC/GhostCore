package me.kooper.ghostcore.old.events

import me.kooper.ghostcore.old.models.Stage
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class StageCreateEvent(val stage: Stage) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        private fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = StageCreateEvent.HANDLERS

}