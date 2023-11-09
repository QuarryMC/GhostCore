package me.kooper.ghostcore.listeners

import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import me.kooper.ghostcore.GhostCore
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ChunkListener : Listener {

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        val chunk: Chunk = event.chunk
        val player: Player = event.player
        for (stage in GhostCore.instance.stageManager.getStages(player)) {
            val chunkData = stage.chunks.get(chunk.chunkKey)
            if (chunkData != null) {
                player.sendMultiBlockChange(chunkData.blocks)
            }
        }
    }

}