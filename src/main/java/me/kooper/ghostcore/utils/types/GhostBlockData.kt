package me.kooper.ghostcore.utils.types

import org.bukkit.block.data.BlockData

class GhostBlockData(val block: BlockData) {

    companion object {
        val blockData: HashMap<Byte, BlockData> = HashMap()
        var nextId: Byte = 0
    }

    private var id: Byte = 0

    init {
        if (blockData.containsValue(block)) {
            id = blockData.filterValues { it == block }.keys.first()
        } else {
            blockData[nextId] = block
            id = nextId
            nextId++
        }
    }

    fun getBlockData(): BlockData {
        return blockData[id]!!
    }
}