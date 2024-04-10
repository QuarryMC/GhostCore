package me.kooper.ghostcore.utils.types

import org.bukkit.block.data.BlockData

class GhostBlockData(block: BlockData) {

    companion object {
        val blockData: HashMap<Short, BlockData> = HashMap()
        var nextId: Short = 0
    }

    private var id: Short = 0

    val block: BlockData
    get() {return blockData[id]!!}

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
