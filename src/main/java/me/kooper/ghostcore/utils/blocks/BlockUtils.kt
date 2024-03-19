package me.kooper.ghostcore.utils.blocks

import io.papermc.paper.math.Position
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player

class BlockUtils {
    companion object {
        fun showBlocks(player: Player, blocks: Map<SimplePosition, GhostBlockData>) {
            val blocksToSend: Map<Position, BlockData> =
                blocks.mapKeys { it.key.toBlockPosition() }.mapValues { it.value.block }
            player.sendMultiBlockChange(blocksToSend)
        }

        fun showBlocks(viewers: List<Player>, world: World, blocks: Map<SimplePosition, GhostBlockData>) {
            if (viewers.isEmpty()) return
            viewers.forEach { if (it.world.uid == world.uid) showBlocks(it, blocks) }
        }
    }
}