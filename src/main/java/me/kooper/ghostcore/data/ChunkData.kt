package me.kooper.ghostcore.data

import io.papermc.paper.math.Position
import org.bukkit.block.data.BlockData

@Suppress("UnstableApiUsage")
data class ChunkData(val blocks: HashMap<Position, BlockData>)

