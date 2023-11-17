package me.kooper.ghostcore.packets

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.google.common.collect.Sets
import io.papermc.paper.math.Position
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.data.ViewData
import me.kooper.ghostcore.events.GhostBreakEvent
import me.kooper.ghostcore.models.Stage
import org.bukkit.*
import org.bukkit.block.data.BlockData
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player


@Suppress("UnstableApiUsage")
class PacketListener : SimplePacketListenerAbstract() {

    private val instaBreakable: Set<Material> = Sets.newHashSet(
        Material.GRASS,
        Material.TALL_GRASS,
        Material.FERN,
        Material.LARGE_FERN,
        Material.DANDELION,
        Material.POPPY,
        Material.BLUE_ORCHID,
        Material.ALLIUM,
        Material.PINK_PETALS,
        Material.AZURE_BLUET,
        Material.OXEYE_DAISY,
        Material.CORNFLOWER,
        Material.RED_TULIP,
        Material.ORANGE_TULIP,
        Material.WHITE_TULIP,
        Material.PINK_TULIP,
        Material.SUNFLOWER,
        Material.SWEET_BERRIES,
        Material.WHEAT,
        Material.CARROT,
        Material.POTATO,
        Material.BEETROOT,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.MELON,
        Material.PUMPKIN,
        Material.SUGAR_CANE,
        Material.CACTUS,
        Material.BAMBOO,
        Material.SEAGRASS,
        Material.SEA_PICKLE,
        Material.TUBE_CORAL_FAN,
        Material.BRAIN_CORAL_FAN,
        Material.BUBBLE_CORAL_FAN,
        Material.FIRE_CORAL_FAN,
        Material.HORN_CORAL_FAN,
        Material.TUBE_CORAL,
        Material.BRAIN_CORAL,
        Material.BUBBLE_CORAL,
        Material.FIRE_CORAL,
        Material.HORN_CORAL,
        Material.NETHER_SPROUTS,
        Material.WARPED_FUNGUS,
        Material.CRIMSON_FUNGUS,
        Material.WARPED_ROOTS,
        Material.CRIMSON_ROOTS,
        Material.WITHER_ROSE,
        Material.CHORUS_PLANT,
        Material.CHORUS_FLOWER,
        Material.FLOWERING_AZALEA
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

                event.isCancelled = true
                if (actionType == DiggingAction.START_DIGGING && (player.gameMode == GameMode.CREATIVE || instaBreakable.contains(
                        block.material
                    ) || player.inventory.itemInMainHand.getEnchantmentLevel(
                        Enchantment.DIG_SPEED
                    ) > 5) ||
                    actionType == DiggingAction.FINISHED_DIGGING
                ) {
                    val stage: Stage = GhostCore.instance.stageManager.getStages(player).firstOrNull { stage ->
                        stage.getViewFromPos(diggingPosition) != null && stage.world == player.world
                    } ?: return
                    val view: ViewData = stage.getViewFromPos(diggingPosition) ?: return
                    if (!view.isBreakable) {
                        event.isCancelled = true
                        player.sendBlockChange(diggingPosition.toLocation(stage.world), view.blocks[diggingPosition]!!.material.createBlockData())
                        player.spawnParticle(Particle.CLOUD, diggingPosition.toLocation(player.world), 1)
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