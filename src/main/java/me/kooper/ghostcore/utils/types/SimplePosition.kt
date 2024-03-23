package me.kooper.ghostcore.utils.types

import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector

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

    fun toVector(): Vector {
        return Vector(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun distanceSquared(other: SimplePosition): Double {
        return toVector().distanceSquared(other.toVector())
    }

    override fun toString(): String {
        return "SimplePosition(x=$x, y=$y, z=$z)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as SimplePosition

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    companion object {
        fun from(x: Int, y: Int, z: Int): SimplePosition {
            return SimplePosition(x, y, z)
        }
    }

}

fun Vector.toSimplePosition(): SimplePosition {
    return SimplePosition(x, y, z)
}