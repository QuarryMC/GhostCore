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
import me.kooper.ghostcore.utils.blocks.chunkedMultiBlockChange
import me.kooper.ghostcore.utils.blocks.unchunkedMultiBlockChange
import me.kooper.ghostcore.utils.callEventSync
import me.kooper.ghostcore.utils.types.SimplePosition
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

@Suppress("UnstableApiUsage")
class PacketListener : SimplePacketListenerAbstract() {

    val invalidBreaksLastSecond = mutableMapOf<UUID, Long>()

    override fun onPacketPlayReceive(event: PacketPlayReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> {
                val placement = WrapperPlayClientPlayerBlockPlacement(event)
                val blockPosition = SimplePosition.from(
                    placement.blockPosition.x + placement.face.modX,
                    placement.blockPosition.y + placement.face.modY,
                    placement.blockPosition.z + placement.face.modZ
                )

                val player = event.player as Player
                val isCancelled = GhostCore.getInstance().stageManager.getStages(player)
                    .filter { it.getViewFromPos(blockPosition) != null }.any { stage ->
                    stage.getViewFromPos(blockPosition)!!.hasBlock(blockPosition) && stage.world == player.world
                }

                if (isCancelled && PlainTextComponentSerializer.plainText().serialize(player.inventory.itemInMainHand.displayName()).contains("Kraken Crate", true)) {
                    player.sendMessage(Component.text("You can't place these in the mine.", NamedTextColor.RED))
                }

                event.isCancelled = isCancelled
//                if (!player.inventory.itemInMainHand.type.isBlock && !player.inventory.itemInOffHand.type.isBlock) return
//                val blockAtLoc = player.world.getBlockAt(blockPosition.toLocation(player.world))
//                player.sendBlockChange(blockPosition.toLocation(player.world), blockAtLoc.blockData)
//                player.inventory.setItemInMainHand(player.inventory.itemInMainHand)
//                player.inventory.setItemInOffHand(player.inventory.itemInOffHand)
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val digging = WrapperPlayClientPlayerDigging(event)
                val actionType = digging.action


                val player = event.player as Player
                val diggingPosition =
                    SimplePosition.from(digging.blockPosition.x, digging.blockPosition.y, digging.blockPosition.z)

                val stages = GhostCore.getInstance().stageManager.getStages(player)
                val block = stages.map {
                    it.getViewFromPos(diggingPosition)
                }.filterNotNull().map {
                    it.getBlock(diggingPosition)
                }.firstOrNull()

                if (block == null) {
                    val blockAt = player.world.getBlockAt(diggingPosition.toLocation(player.world))
                    player.sendBlockChange(diggingPosition.toLocation(player.world), blockAt.blockData)

                    val view = stages.firstNotNullOfOrNull {
                        it.getViewFromPos(diggingPosition)
                    } as? ChunkedView ?: return

                    if (blockAt.type.isAir) {
                        val newTotal = invalidBreaksLastSecond.getOrDefault(player.uniqueId, 0) + 1
                        invalidBreaksLastSecond[player.uniqueId] = newTotal
                        if (newTotal >= 10) {
                            player.unchunkedMultiBlockChange(view.getAllBlocksInBound())
                            invalidBreaksLastSecond.remove(player.uniqueId)
                        }
                        Bukkit.getScheduler().runTaskLater(GhostCore.getInstance(), Runnable {
                            run {
                                if (invalidBreaksLastSecond[player.uniqueId] == null) return@run

                                invalidBreaksLastSecond[player.uniqueId] = invalidBreaksLastSecond[player.uniqueId]!! - 1

                                if (invalidBreaksLastSecond[player.uniqueId]!! <= 0)
                                    invalidBreaksLastSecond.remove(player.uniqueId)
                            }
                        }, 20L)
//                        player.unchunkedMultiBlockChange(view.getBlocksInChunkWithAir(stages.find { it.views.contains(view.name)  }!! as ChunkedStage, diggingPosition.getChunk()))
                    }

                    return
                }


                val stage: ChunkedStage = GhostCore.getInstance().stageManager.getStages(player)
                    .asSequence().filter {
                        it.getViewFromPos(diggingPosition) != null
                    }
                    .mapNotNull {
                        it as? ChunkedStage
                    }
                    .firstOrNull() ?: return
                val view = stage.getViewFromPos(diggingPosition)!!

                when (actionType) {
                    DiggingAction.START_DIGGING -> {
                        GhostInteractEvent(player, diggingPosition, block.getBlockData(), view, stage).callEventSync()

                        if (
                            player.gameMode == GameMode.CREATIVE ||
                            GhostCore.getInstance().instaBreak.contains(block.getBlockData().material) ||
                            ((block.getBlockData().getDestroySpeed(
                                player.inventory.itemInMainHand,
                                true
                            ) >= block.getBlockData().material.hardness * 30) && !player.isFlying || (block.getBlockData()
                                .getDestroySpeed(
                                    player.inventory.itemInMainHand,
                                    true
                                ) >= block.getBlockData().material.hardness * 150) && player.isFlying)
                        ) {
                            if (!view.isBreakable()) {
                                player.sendBlockChange(diggingPosition.toLocation(stage.world), block.getBlockData())
                                return
                            }
                            Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable {
                                run {
                                    val ghostBreakEvent =
                                        GhostBreakEvent(player, diggingPosition, block.getBlockData(), view, stage)
                                    ghostBreakEvent.callEvent()
                                    if (!ghostBreakEvent.isCancelled) {
                                        player.sendBlockChange(
                                            diggingPosition.toLocation(stage.world),
                                            Material.AIR.createBlockData()
                                        )
                                        view.removeBlock(diggingPosition)
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
                var stage =
                    GhostCore.getInstance().stageManager.getStages(player).firstOrNull { it.world == world } ?: return
                stage = stage as ChunkedStage

                val chunkPositions = mutableSetOf<SimplePosition>()
                val minHeight = stage.views.values.sumOf { (it as ChunkedView).bound.minY }
                val maxHeight = stage.views.values.sumOf { (it as ChunkedView).bound.maxY }
                for (y in minHeight until maxHeight step 16) {
                    chunkPositions.add(SimplePosition(chunkX * 16, y, chunkZ * 16).getChunk())
                }

                val chunkedViews = stage.views.filter {
                    val view = it.value as ChunkedView
                    chunkPositions.any { pos -> view.hasChunk(pos) }
                }.values as Collection<ChunkedView>

                chunkedViews.forEach { view ->
                    val allChunksInXZ = chunkPositions.filter { view.hasChunk(it) }
                    allChunksInXZ.forEach { chunkPos ->
                        val chunkBlocks = view.getBlocksInChunk(chunkPos)
                        if (chunkBlocks.isNotEmpty()) {
                            player.sendMultiBlockChange(chunkBlocks.mapValues { it.value.getBlockData() }
                                .mapKeys { it.key.toBlockPosition() })
                        }
                    }
                }
            }

            else -> {}
        }
    }

}