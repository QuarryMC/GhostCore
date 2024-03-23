package me.kooper.ghostcore.models

import me.kooper.ghostcore.utils.PatternData
import me.kooper.ghostcore.utils.SimpleBound
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.Material
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.concurrent.ConcurrentHashMap

data class ChunkedView(
    val name: String,
    var patternData: PatternData,
    var damageable: Boolean,
    var bound: SimpleBound,
    /**
     * ConcurrentHashMap<Chunk Position, ConcurrentHashMap<Block Position, Block Data>>
     */
    val blocks: ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>> = ConcurrentHashMap()
) : View {

    /**
     * Used to track recently changed blocks, this will be sent to players and cleared on an interval.
     */
//    val changedBlocks: ConcurrentHashMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()

    override fun isBreakable(): Boolean {
        return this.damageable
    }

    override fun setBreakable(breakable: Boolean) {
        this.damageable = breakable
    }

    fun getBlocksInChunk(chunkPosition: SimplePosition): ConcurrentHashMap<SimplePosition, GhostBlockData> {
        return blocks.getOrDefault(chunkPosition, ConcurrentHashMap())
    }

    fun getAllBlocksInBound(): Map<SimplePosition, GhostBlockData> {
        // This will loop blocks within the bound
        val allBlocks = ConcurrentHashMap<SimplePosition, GhostBlockData>()
        val min = bound.min()
        val max = bound.max()
        val airBlockData = GhostBlockData(Material.AIR.createBlockData())
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    val position = SimplePosition(x, y, z)
                    val blockData = getBlock(position)
                    allBlocks[position] = blockData ?: airBlockData
                }
            }
        }
        return allBlocks
    }

    override fun getAllBlocks(): Map<SimplePosition, GhostBlockData> {
        val allBlocks = ConcurrentHashMap<SimplePosition, GhostBlockData>()
        blocks.forEach { (_, blockData) -> allBlocks.putAll(blockData) }
        return allBlocks
    }

    override fun getBlock(position: SimplePosition): GhostBlockData? {
        if (!bound.contains(position)) {
            return null
        }
        val blockMap = blocks.getOrDefault(position.getChunk(), ConcurrentHashMap())
        return blockMap[position]
    }

    override fun setBlock(position: SimplePosition, blockData: GhostBlockData) {
        if (!bound.contains(position)) return
        blocks.getOrPut(position.getChunk()) { ConcurrentHashMap() }[position] = blockData
//        changedBlocks[position] = blockData
    }

    override fun setBlocks(blocks: Map<SimplePosition, GhostBlockData>) {
        val chunkedBlocks = blocks.keys.groupBy { it.getChunk() }
        chunkedBlocks.forEach { (chunk, _) ->
            val chunkBlocks = blocks.filterKeys { it.getChunk() == chunk && bound.contains(it) }
            this.blocks.getOrPut(chunk) { ConcurrentHashMap() }.putAll(chunkBlocks)
        }
//        changedBlocks.putAll(blocks)
    }

    override fun removeBlock(position: SimplePosition) {
        if (!bound.contains(position)) return
        blocks.getOrDefault(position.getChunk(), ConcurrentHashMap()).remove(position)
//        changedBlocks.remove(position)
    }

    override fun removeBlocks(blocks: Set<SimplePosition>) {
        blocks.forEach { removeBlock(it) }
//        changedBlocks.keys.removeAll(blocks)
    }

    override fun hasBlock(position: SimplePosition): Boolean {
        if (!bound.contains(position)) return false
        return blocks.getOrDefault(position.getChunk(), ConcurrentHashMap())
            .containsKey(position)
    }

    fun hasChunk(chunkPosition: SimplePosition): Boolean {
        return blocks.containsKey(chunkPosition)
    }

}