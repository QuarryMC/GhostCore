package me.kooper.ghostcore.data

import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position
import org.bukkit.block.data.BlockData
import java.util.concurrent.ConcurrentHashMap

data class ViewData(val name: String, val blocks: ConcurrentHashMap<BlockPosition, BlockData>, val blocksChange: ConcurrentHashMap<BlockPosition, BlockData>, var patternData: PatternData, var isBreakable: Boolean)
