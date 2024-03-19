package me.kooper.ghostcore.old.packets

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.old.data.ChunkedViewData
import me.kooper.ghostcore.utils.types.SimplePosition
import me.kooper.ghostcore.events.GhostBreakEvent
import me.kooper.ghostcore.events.GhostInteractEvent
import me.kooper.ghostcore.models.Stage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player


@Suppress("UnstableApiUsage")
class PacketListener : SimplePacketListenerAbstract() {

    override fun onPacketPlayReceive(event: PacketPlayReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> {
                val placement = WrapperPlayClientPlayerBlockPlacement(event)
                val blockPosition = SimplePosition.from(
                    placement.blockPosition.x,
                    placement.blockPosition.y,
                    placement.blockPosition.z
                )

                val player = event.player as Player
                val isCancelled = GhostCore.getInstance().stageManager.getStages(player).filter { it.getViewFromPos(blockPosition) != null }.any { stage ->
                    stage.getViewFromPos(blockPosition)!!.blocks.containsKey(blockPosition) && stage.world == player.world
                }

                event.isCancelled = isCancelled
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val digging = WrapperPlayClientPlayerDigging(event)
                val actionType: DiggingAction = digging.action
                val player: Player = event.player as Player
                val diggingPosition =
                    SimplePosition.from(digging.blockPosition.x, digging.blockPosition.y, digging.blockPosition.z)
//                val block: BlockData = GhostCore.getInstance().stageManager.getStages(player)
//                    .asSequence().filter {
//                        it.getViewFromPos(diggingPosition) != null
//                    }
//                    .mapNotNull { stage ->
//                        stage.getViewFromPos(diggingPosition)!!.blocks[diggingPosition]
//                    }
//                    .firstOrNull() ?: return
                val block: BlockData = GhostCore.getInstance().stageManager.getStages(player)
                    .asSequence().filter {
                        it.getViewFromPos(diggingPosition) != null
                    }
                    .mapNotNull {
                        it.getViewFromPos(diggingPosition)!!.getBlock(diggingPosition)
                    }
                    .firstOrNull() ?: return
                val stage: Stage = GhostCore.getInstance().stageManager.getStages(player).firstOrNull { stage ->
                    stage.getViewFromPos(diggingPosition) != null && stage.world == player.world
                } ?: return
                val view: ChunkedViewData = stage.getViewFromPos(diggingPosition) ?: return

                if (actionType == DiggingAction.START_DIGGING) {
                    val interactEvent = GhostInteractEvent(player, diggingPosition, block, view, stage)
                    Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable { run { interactEvent.callEvent() } })
                }

                if (actionType == DiggingAction.START_DIGGING && (player.gameMode == GameMode.CREATIVE || GhostCore.getInstance().instaBreak.contains(
                        block.material
                ) || (block.getDestroySpeed(player.inventory.itemInMainHand, true) >= block.material.hardness * 30) && !player.isFlying || (block.getDestroySpeed(player.inventory.itemInMainHand, true) >= block.material.hardness * 150) && player.isFlying) ||
                    actionType == DiggingAction.FINISHED_DIGGING
                ) {
                    if (!view.isBreakable) {
                        player.sendBlockChange(
                            diggingPosition.toLocation(stage.world),
                            view.getBlock(diggingPosition)!!.material.createBlockData()
                        )
                        return
                    }
                    Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable {
                        run {
                            val ghostBreakEvent = GhostBreakEvent(player, diggingPosition, block, view, stage)
                            ghostBreakEvent.callEvent()
                            if (!ghostBreakEvent.isCancelled) {
                                player.sendBlockChange(diggingPosition.toLocation(stage.world), Material.AIR.createBlockData())
                                stage.setBlock(view.name, diggingPosition, Material.AIR.createBlockData(), false)
                            }
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
                val world = player.world
//                val chunkKey = Chunk.getChunkKey(chunkData.column.x, chunkData.column.z)
//                for (stage in GhostCore.getInstance().stageManager.getStages(player)) {
//                    if (stage.world != (event.player as Player).world) return
//                    if (stage.chunks[chunkKey] == null) continue
//                    for (chunk in stage.chunks[chunkKey].values) {
//                        player.sendMultiBlockChange(chunk)
//                    }
//                }
                // New code for ChunkedViewData instead
                val chunkPositions = mutableSetOf<SimplePosition>()
                for (y in world.minHeight until world.maxHeight step 16) {
                    chunkPositions.add(SimplePosition.from(chunkData.column.x, y, chunkData.column.z).getChunk())
                }

                for (stage in GhostCore.getInstance().stageManager.getStages(player)) {
                    if (stage.world != world) return
                    for (chunkPosition in chunkPositions) {
                        if (stage.chunks[chunkPosition] == null) continue
                        player.sendMultiBlockChange(stage.chunks[chunkPosition]!!.mapKeys { it.key.toLocation(world) })
                    }
                }
            }

            else -> {}
        }
    }

}