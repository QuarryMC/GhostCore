package me.kooper.ghostcore.old.data

import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import java.util.concurrent.ConcurrentHashMap

data class ChunkedViewData(
    val name: String,
    val blocks: ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, BlockData>>,
    val blocksChange: ConcurrentHashMap<SimplePosition, BlockData>,
    var patternData: PatternData,
    var isBreakable: Boolean
) {

    private fun getChunkPos(position: SimplePosition): SimplePosition {
        return position.getChunk()
    }

    private fun getChunkData(position: SimplePosition): ConcurrentHashMap<SimplePosition, BlockData> {
        return blocks.getOrDefault(getChunkPos(position), ConcurrentHashMap())
    }

    fun hasBlock(position: SimplePosition): Boolean {
        return getChunkData(position).containsKey(position)
    }

    fun getBlock(position: SimplePosition): BlockData? {
        return getChunkData(position)[position]
    }

    fun setBlock(position: SimplePosition, blockData: BlockData) {
        blocks[getChunkPos(position)]?.put(position, blockData)
    }

    fun removeBlock(position: SimplePosition) {
        getChunkData(position).remove(position)
    }

    fun removeBlocks(blocks: Set<SimplePosition>) {
        blocks.forEach { removeBlock(it) }
    }

    fun getAllBlocks(): ConcurrentHashMap<SimplePosition, BlockData> {
        val allBlocks = ConcurrentHashMap<SimplePosition, BlockData>()
        blocks.forEach { (_, blockData) -> allBlocks.putAll(blockData) }
        return allBlocks
    }

    fun getChunkBlocks(chunkPosition: SimplePosition): ConcurrentHashMap<SimplePosition, BlockData> {
        return blocks.getOrDefault(getChunkPos(chunkPosition), ConcurrentHashMap())
    }

    fun setChunkBlocks(chunkPosition: SimplePosition, chunkBlocks: ConcurrentHashMap<SimplePosition, BlockData>) {
        blocks[chunkPosition] = chunkBlocks
    }
}