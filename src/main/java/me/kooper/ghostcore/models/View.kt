package me.kooper.ghostcore.models

import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition

interface View {

    fun getAllBlocks(): Map<SimplePosition, GhostBlockData>
    fun getBlock(position: SimplePosition): GhostBlockData?
    fun setBlock(position: SimplePosition, blockData: GhostBlockData)
    fun setBlocks(blocks: Map<SimplePosition, GhostBlockData>)
    fun removeBlock(position: SimplePosition)
    fun removeBlocks(blocks: Set<SimplePosition>)
    fun hasBlock(position: SimplePosition): Boolean

}