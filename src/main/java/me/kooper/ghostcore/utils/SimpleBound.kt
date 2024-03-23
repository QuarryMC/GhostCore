package me.kooper.ghostcore.utils

import me.kooper.ghostcore.utils.types.SimplePosition

class SimpleBound(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
) {

    constructor(
        min: SimplePosition,
        max: SimplePosition
    ) : this(
        min.x,
        min.y,
        min.z,
        max.x,
        max.y,
        max.z
    )

    fun contains(position: SimplePosition): Boolean {
        return position.x in minX..maxX && position.y in minY..maxY && position.z in minZ..maxZ
    }

    fun min(): SimplePosition {
        return SimplePosition(minX, minY, minZ)
    }

    fun max(): SimplePosition {
        return SimplePosition(maxX, maxY, maxZ)
    }

}