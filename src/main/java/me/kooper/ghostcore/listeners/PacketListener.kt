package me.kooper.ghostcore.listeners

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.events.GhostBreakEvent
import me.kooper.ghostcore.events.GhostInteractEvent
import me.kooper.ghostcore.models.ChunkedStage
import me.kooper.ghostcore.models.ChunkedView
import me.kooper.ghostcore.utils.callEventSync
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.Bukkit
import org.bukkit.GameMode
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
                    stage.getViewFromPos(blockPosition)!!.hasBlock(blockPosition) && stage.world == player.world
                }

                event.isCancelled = isCancelled
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val digging = WrapperPlayClientPlayerDigging(event)
                val actionType = digging.action

                val player = event.player as Player
                val diggingPosition = SimplePosition.from(digging.blockPosition.x, digging.blockPosition.y, digging.blockPosition.z)

                val block = GhostCore.getInstance().stageManager.getStages(player)
                    .asSequence().filter {
                        it.getViewFromPos(diggingPosition) != null
                    }
                    .mapNotNull { stage ->
                        stage.getViewFromPos(diggingPosition)!!.getBlock(diggingPosition)
                    }
                    .firstOrNull() ?: return

                val stage: ChunkedStage = GhostCore.getInstance().stageManager.getStages(player)
                    .asSequence().filter {
                        it.getViewFromPos(diggingPosition) != null
                    }
                    .mapNotNull {
                        it.getViewFromPos(diggingPosition) as ChunkedStage
                    }
                    .firstOrNull() ?: return
                val view = stage.getViewFromPos(diggingPosition)!!

                when (actionType) {
                    DiggingAction.START_DIGGING -> {
                        GhostInteractEvent(player, diggingPosition, block.getBlockData(), view, stage).callEventSync()

                        if (
                            player.gameMode == GameMode.CREATIVE ||
                            GhostCore.getInstance().instaBreak.contains(block.getBlockData().material) ||
                            ((block.getBlockData().getDestroySpeed(player.inventory.itemInMainHand, true) >= block.getBlockData().material.hardness * 30) && !player.isFlying || (block.getBlockData().getDestroySpeed(player.inventory.itemInMainHand, true) >= block.getBlockData().material.hardness * 150) && player.isFlying)
                        ) {
                            if (!view.isBreakable()) {
                                player.sendBlockChange(diggingPosition.toLocation(stage.world), block.getBlockData())
                                return
                            }
                            Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable {
                                run {
                                    val ghostBreakEvent = GhostBreakEvent(player, diggingPosition, block.getBlockData(), view, stage)
                                    ghostBreakEvent.callEventSync()
                                    if (!ghostBreakEvent.isCancelled) {
                                        player.sendBlockChange(diggingPosition.toLocation(stage.world), block.getBlockData())
                                        view.setBlock(diggingPosition, block)
                                    }
                                }
                            })
                        }
                    }
                    else -> {}
                }
            }

            else -> {}
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onPacketPlaySend(event: PacketPlaySendEvent?) {
        if (event == null) return
        when (event.packetType) {

            PacketType.Play.Server.CHUNK_DATA -> {
                val player = event.player as Player
                val chunkData = WrapperPlayServerChunkData(event)
                val world = player.world
                val chunkX = chunkData.column.x
                val chunkZ = chunkData.column.z
                val stage = GhostCore.getInstance().stageManager.getStages(player).firstOrNull { it.world == world } ?: return

                val chunkPositions = mutableSetOf<SimplePosition>()
                for (y in world.minHeight until world.maxHeight step 16) {
                    chunkPositions.add(SimplePosition(chunkX * 16, y, chunkZ * 16).getChunk())
                }

                val chunkedViews = stage.views.filter {
                    chunkPositions.any { pos -> it.value.hasBlock(pos) }
                }.values as Collection<ChunkedView>

                chunkedViews.forEach { view ->
                    val chunkBlocks = view.getBlocksInChunk(SimplePosition(chunkX, 0, chunkZ))
                    if (chunkBlocks.isNotEmpty()) {
                        player.sendMultiBlockChange(chunkBlocks.mapValues { it.value.getBlockData() }.mapKeys { it.key.toBlockPosition() })
                    }
                }
            }

            else -> {}
        }
    }

}