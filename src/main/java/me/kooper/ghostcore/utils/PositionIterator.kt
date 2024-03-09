import io.papermc.paper.math.Position
import kotlin.math.abs
import kotlin.math.min

@Suppress("UnstableApiUsage")
class PositionIterator(
    x1: Int,
    y1: Int,
    z1: Int,
    x2: Int,
    y2: Int,
    z2: Int
) :
    Iterator<Position> {
    private var x: Int
    private var y: Int
    private var z: Int
    private var baseX: Int
    private var baseY: Int
    private var baseZ: Int
    private val sizeX: Int
    private val sizeY: Int
    private val sizeZ: Int

    init {
        baseX = min(x1, x2)
        baseY = min(y1, y2)
        baseZ = min(z1, z2)
        sizeX = (abs((x2 - x1).toDouble()) + 1).toInt()
        sizeY = (abs((y2 - y1).toDouble()) + 1).toInt()
        sizeZ = (abs((z2 - z1).toDouble()) + 1).toInt()
        y = 0
        x = 0
        z = 0
    }

    override fun hasNext(): Boolean {
        return x < sizeX && y < sizeY && z < sizeZ
    }

    override fun next(): Position {
        val position = Position.block(baseX + x, baseY + y, baseZ + z)
        if (++x >= sizeX) {
            x = 0
            if (++y >= sizeY) {
                y = 0
                ++z
            }
        }
        return position
    }
}