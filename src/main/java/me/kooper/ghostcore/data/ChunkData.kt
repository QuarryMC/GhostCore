package me.kooper.ghostcore.data

import io.papermc.paper.math.Position
import org.bukkit.Material
import org.bukkit.block.data.BlockData

data class ChunkData(val blocks: HashMap<Position, BlockData>, val type: Material)

