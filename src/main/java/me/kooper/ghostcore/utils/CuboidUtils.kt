package me.kooper.ghostcore.utils

import io.papermc.paper.math.Position
import me.kooper.ghostcore.GhostCore
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.concurrent.CompletableFuture

object CuboidUtils {

    @Suppress("UnstableApiUsage")
    fun getPositionsBetween(loc1: Location, loc2: Location): CompletableFuture<Set<Position>> {
        val future = CompletableFuture<Set<Position>>()

        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                val positions: MutableSet<Position> = HashSet()

                val topBlockX = Integer.max(loc1.blockX, loc2.blockX)
                val bottomBlockX = Integer.min(loc1.blockX, loc2.blockX)

                val topBlockY = Integer.max(loc1.blockY, loc2.blockY)
                val bottomBlockY = Integer.min(loc1.blockY, loc2.blockY)

                val topBlockZ = Integer.max(loc1.blockZ, loc2.blockZ)
                val bottomBlockZ = Integer.min(loc1.blockZ, loc2.blockZ)

                for (x in bottomBlockX..topBlockX) {
                    for (z in bottomBlockZ..topBlockZ) {
                        for (y in bottomBlockY..topBlockY) {
                            positions.add(Position.block(x, y, z))
                        }
                    }
                }

                future.complete(positions)
            }
        })

        return future
    }

}