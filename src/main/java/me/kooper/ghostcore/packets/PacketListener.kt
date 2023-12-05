package me.kooper.ghostcore.packets

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import io.papermc.paper.math.Position
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.data.ViewData
import me.kooper.ghostcore.events.GhostBreakEvent
import me.kooper.ghostcore.events.GhostInteractEvent
import me.kooper.ghostcore.models.Stage
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.GameMode
import org.bukkit.block.data.BlockData
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player


@Suppress("UnstableApiUsage")
class PacketListener : SimplePacketListenerAbstract() {

    override fun onPacketPlayReceive(event: PacketPlayReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> {
                val placement = WrapperPlayClientPlayerBlockPlacement(event)
                val blockPosition = Position.block(
                    placement.blockPosition.x,
                    placement.blockPosition.y,
                    placement.blockPosition.z
                )

                val player = event.player as Player
                val isCancelled = GhostCore.instance.stageManager.getStages(player).any { stage ->
                    stage.blocks.containsKey(blockPosition) && stage.world == player.world
                }

                event.isCancelled = isCancelled
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val digging = WrapperPlayClientPlayerDigging(event)
                val actionType: DiggingAction = digging.action
                val player: Player = event.player as Player
                val diggingPosition =
                    Position.block(digging.blockPosition.x, digging.blockPosition.y, digging.blockPosition.z)
                val block: BlockData = GhostCore.instance.stageManager.getStages(player)
                    .asSequence()
                    .mapNotNull { stage ->
                        stage.blocks[diggingPosition]
                    }
                    .firstOrNull() ?: return
                val stage: Stage = GhostCore.instance.stageManager.getStages(player).firstOrNull { stage ->
                    stage.getViewFromPos(diggingPosition) != null && stage.world == player.world
                } ?: return
                val view: ViewData = stage.getViewFromPos(diggingPosition) ?: return

                if (actionType == DiggingAction.START_DIGGING) {
                    val interactEvent = GhostInteractEvent(player, diggingPosition, block, view, stage)
                    Bukkit.getScheduler().runTask(GhostCore.instance, Runnable { run { interactEvent.callEvent() } })
                }

                if (actionType == DiggingAction.START_DIGGING && (player.gameMode == GameMode.CREATIVE || GhostCore.instance.instaBreak.contains(
                        block.material
                    ) || player.inventory.itemInMainHand.getEnchantmentLevel(
                        Enchantment.DIG_SPEED
                    ) > 5) ||
                    actionType == DiggingAction.FINISHED_DIGGING
                ) {
                    if (!view.isBreakable) {
                        player.sendBlockChange(
                            diggingPosition.toLocation(stage.world),
                            view.blocks[diggingPosition]!!.material.createBlockData()
                        )
                        return
                    }
                    Bukkit.getScheduler().runTask(GhostCore.instance, Runnable {
                        run {
                            val ghostBreakEvent = GhostBreakEvent(player, diggingPosition, block, view, stage)
                            ghostBreakEvent.callEvent()
                            if (!ghostBreakEvent.isCancelled) stage.setAirBlock(view.name, diggingPosition, true)
                        }
                    })
                }
            }

            else -> {}
        }
    }

    override fun onPacketPlaySend(event: PacketPlaySendEvent?) {
        if (event == null) return
        when (event.packetType) {
            PacketType.Play.Server.CHUNK_DATA -> {
                val player = event.player as Player
                val chunkData = WrapperPlayServerChunkData(event)
                val chunkKey = Chunk.getChunkKey(chunkData.column.x, chunkData.column.z)
                for (stage in GhostCore.instance.stageManager.getStages(player)) {
                    if (stage.world != (event.player as Player).world) return
                    if (stage.chunks[chunkKey] == null) continue
                    for (chunk in stage.chunks[chunkKey].values) {
                        player.sendMultiBlockChange(chunk.blocks)
                    }
                }
            }

            else -> {}
        }
    }

}