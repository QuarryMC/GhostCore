package me.kooper.ghostcore.data

import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import org.bukkit.Material
import org.bukkit.block.data.BlockData

data class ViewData(val blocks: HashMap<Position, BlockData>, var material: Material)
