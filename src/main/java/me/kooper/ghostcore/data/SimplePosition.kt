package me.kooper.ghostcore.data

import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.floor

class SimplePosition(var x: Int, var y: Int, var z: Int) {
    constructor(x: Number, y: Number, z: Number) : this(x.toInt(), y.toInt(), z.toInt())
    constructor(other: SimplePosition) : this(other.x, other.y, other.z)

    fun set(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun set(x: Number, y: Number, z: Number) {
        set(x.toInt(), y.toInt(), z.toInt())
    }

    fun toBlockPosition(): BlockPosition {
        val blockPosition = Position.block(x, y, z)
        return blockPosition
    }

    fun getChunk(): SimplePosition {
        val chunkX = x / 16
        val chunkY = y / 16
        val chunkZ = z / 16


        return SimplePosition(chunkX, chunkY, chunkZ)
    }

    fun toLocation(world: World): Location {
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    companion object {
        fun from(x: Int, y: Int, z: Int): SimplePosition {
            return SimplePosition(x, y, z)
        }
    }

}