package me.kooper.ghostcore.events

import me.kooper.ghostcore.old.data.ChunkedViewData
import me.kooper.ghostcore.old.data.SimplePosition
import me.kooper.ghostcore.old.models.Stage
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

@Suppress("UnstableApiUsage")
class GhostBreakEvent(
    val player: Player,
    val position: SimplePosition,
    val blockData: BlockData,
    val view: ChunkedViewData,
    val stage: Stage
) : Event(), Cancellable {

    private var cancelled: Boolean = false

    companion object {
        val HANDLERS = HandlerList()
        @JvmStatic
        private fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}