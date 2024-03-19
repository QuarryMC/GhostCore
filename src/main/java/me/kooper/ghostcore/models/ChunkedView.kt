package me.kooper.ghostcore.models

import me.kooper.ghostcore.old.data.PatternData
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.Material
import org.bukkit.util.BoundingBox
import java.util.concurrent.ConcurrentHashMap

data class ChunkedView(
    val name: String,
    var patternData: PatternData,
    var isBreakable: Boolean,
    var bound: BoundingBox,
    /**
     * ConcurrentHashMap<Chunk Position, ConcurrentHashMap<Block Position, Block Data>>
     */
    val blocks: ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>> = ConcurrentHashMap()
): View {

    /**
     * Used to track recently changed blocks, this will be sent to players and cleared on an interval.
     */
    val changedBlocks: ConcurrentHashMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()

    override fun isBreakable(): Boolean {
        return isBreakable;
    }

    override fun setBreakable(breakable: Boolean) {
        isBreakable = breakable
    }

    fun getBlocksInChunk(chunkPosition: SimplePosition): ConcurrentHashMap<SimplePosition, GhostBlockData> {
        return blocks.getOrDefault(chunkPosition, ConcurrentHashMap())
    }

    fun getAllBlocksInBound(): Map<SimplePosition, GhostBlockData> {
        // This will loop blocks within the bound
        val allBlocks = ConcurrentHashMap<SimplePosition, GhostBlockData>()
        val min = bound.min.toBlockVector()
        val max = bound.max.toBlockVector()
        val airBlockData = GhostBlockData(Material.AIR.createBlockData())
        for (x in min.blockX..max.blockX) {
            for (y in min.blockY..max.blockY) {
                for (z in min.blockZ..max.blockZ) {
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
        if (!bound.contains(position.toVector())) return null
        return blocks.getOrDefault(position.getChunk(), ConcurrentHashMap())[position]
    }

    override fun setBlock(position: SimplePosition, blockData: GhostBlockData) {
        if (!bound.contains(position.toVector())) return
        blocks.getOrPut(position.getChunk()) { ConcurrentHashMap() }[position] = blockData
        changedBlocks[position] = blockData
    }

    override fun setBlocks(blocks: Map<SimplePosition, GhostBlockData>) {
        blocks.forEach { (position, blockData) -> setBlock(position, blockData) }
        changedBlocks.putAll(blocks)
    }

    override fun removeBlock(position: SimplePosition) {
        if (!bound.contains(position.toVector())) return
        blocks.getOrDefault(position.getChunk(), ConcurrentHashMap()).remove(position)
        changedBlocks.remove(position)
    }

    override fun removeBlocks(blocks: Set<SimplePosition>) {
        blocks.forEach { removeBlock(it) }
        changedBlocks.keys.removeAll(blocks)
    }

    override fun hasBlock(position: SimplePosition): Boolean {
        if (!bound.contains(position.toVector())) return false
        return changedBlocks.contains(position) || blocks.getOrDefault(position.getChunk(), ConcurrentHashMap()).containsKey(position)
    }

}