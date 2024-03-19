package me.kooper.ghostcore.utils

import io.papermc.paper.math.Position
import me.kooper.ghostcore.utils.types.PositionIterator

@Suppress("UnstableApiUsage")
object PositionUtils {

    /**
     * Gets the positions within position 1 and position 2. (CALL ASYNC)
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return A set of positions.
     */
    fun getLocationsWithin(pos1: Position, pos2: Position): Set<Position> {
        val positions = HashSet<Position>()
        PositionIterator(
            pos1.blockX(),
            pos1.blockY(),
            pos1.blockZ(),
            pos2.blockX(),
            pos2.blockY(),
            pos2.blockZ()
        ).forEach {
            positions.add(it)
        }
        return positions
    }

    /**
     * Gets the positions within a radius around the center. (CALL ASYNC)
     * @param center The center position.
     * @param radius The radius to get positions.
     * @return A set of positions.
     */
    fun getLocationsInRadius(center: Position, radius: Int): Set<Position> {
        val positions = HashSet<Position>()

        val cx = center.blockX()
        val cy = center.blockY()
        val cz = center.blockZ()

        for (x in cx - radius..cx + radius) {
            for (y in cy - radius..cy + radius) {
                for (z in cz - radius..cz + radius) {
                    val distance: Double = ((cx - x) * (cx - x) + (cz - z) * (cz - z) + (cy - y) * (cy - y)).toDouble()
                    if (distance < radius * radius && distance < (radius - 1) * (radius - 1)) {
                        positions.add(Position.block(x, y, z))
                    }
                }
            }
        }

        return positions
    }

}