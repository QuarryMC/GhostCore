@file:Suppress("UnstableApiUsage")

package me.kooper.ghostcore.packets

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.google.common.collect.Sets
import io.papermc.paper.math.Position
import me.kooper.ghostcore.GhostCore
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player


class PacketListener : SimplePacketListenerAbstract() {

    private val INSTA_BREAKABLE: Set<Material> = Sets.newHashSet<Material>(
        Material.POTATO, Material.CARROT, Material.SUGAR_CANE, Material.FLOWERING_AZALEA, Material.CORNFLOWER, Material.SUNFLOWER, Material.TORCHFLOWER, Material.POPPY, Material.BLUE_ORCHID, Material.ORANGE_TULIP, Material.PINK_TULIP, Material.RED_TULIP, Material.WHITE_TULIP,
        Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.ROSE_BUSH,
        Material.CHORUS_FLOWER, Material.DEAD_BUSH, Material.NETHER_WART, Material.TALL_GRASS, Material.TRIPWIRE_HOOK
    )

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
                    stage.blocks.containsKey(blockPosition)
                }

                event.isCancelled = isCancelled
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val digging = WrapperPlayClientPlayerDigging(event)
                val actionType: DiggingAction = digging.action
                val player: Player = event.player as Player
                val diggingPosition = Position.block(digging.blockPosition.x, digging.blockPosition.y, digging.blockPosition.z)
                val block: BlockData? = GhostCore.instance.stageManager.getStages(player)
                    .asSequence()
                    .mapNotNull { stage ->
                        stage.blocks[diggingPosition]
                    }
                    .firstOrNull()

                if (actionType == DiggingAction.START_DIGGING && (player.gameMode == GameMode.CREATIVE || INSTA_BREAKABLE.contains(block!!.material)) ||
                    actionType == DiggingAction.FINISHED_DIGGING) {
                    val time = System.currentTimeMillis()
                    for (stage in GhostCore.instance.stageManager.getStages(player)) {
                        for (view in stage.views.keys) {
                            stage.setAirBlock(view, diggingPosition)
                            stage.updateChunkView(view, stage.getChunkFromPos(diggingPosition))
                        }
                    }
                    println(System.currentTimeMillis() - time)
                }
            }

            else -> {}
        }
    }
}