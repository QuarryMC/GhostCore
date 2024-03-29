package me.kooper.ghostcore.utils.blocks

import io.papermc.paper.math.Position
import me.kooper.ghostcore.utils.SchedulerAssist
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap


val blocksToSend = ConcurrentHashMap<UUID, MutableList<ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>>>()
val tasks = ConcurrentHashMap<UUID, BukkitTask>()

var tasksWorking = true

fun Player.unchunkedMultiBlockChange(blocks: Map<SimplePosition, GhostBlockData>) {
    val chunkedData = blocks.entries
        .groupBy { it.key.getChunk() }
        .mapValues { entry -> entry.value.associate { it.key to it.value } }

    this.chunkedMultiBlockChange(chunkedData)
}

fun Player.chunkedMultiBlockChange(blocks: Map<SimplePosition, Map<SimplePosition, GhostBlockData>>) {
    val concurrentHashMap = ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>()
    blocks.forEach { (key, value) ->
        concurrentHashMap[key] = ConcurrentHashMap(value)
    }

    this.chunkedMultiBlockChangeMutable(concurrentHashMap)
}

fun Player.chunkedMultiBlockChangeMutable(blocks: ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>) {


    var total = 0
    blocks.forEach { (_, blockData) ->
        total += blockData.size
        if (total > 5000) return
    }
    if (!tasksWorking || total <= 4999) {
        this.sendMultiBlockChange(blocks.flatMap { (_, blockData) ->
            blockData.map { (blockPosition, ghostBlockData) ->
                Position.block(blockPosition.x, blockPosition.y, blockPosition.z) to ghostBlockData.block
            }
        }.toMap())
        return
    }


    if (blocksToSend[this.uniqueId] == null) {
        blocksToSend[this.uniqueId] = mutableListOf(blocks)
    } else {
        blocksToSend[this.uniqueId]!!.add(blocks)
    }

    if (tasks[this.uniqueId] != null && !tasks[this.uniqueId]!!.isCancelled) return

    var running = false
    val task = SchedulerAssist {
        if (running) return@SchedulerAssist
        running = true
        val next = blocksToSend[this.uniqueId]?.get(0)
        if (next == null) {
            blocksToSend.remove(this.uniqueId)
            return@SchedulerAssist
        }

        SchedulerAssist {
            next.entries.sortedBy {
                it.key.distanceSquared(SimplePosition(this.location.blockX/16, this.location.blockY/16, this.location.blockZ/16))
            }.forEach { (_, blockData) ->
                this.sendMultiBlockChange(blockData.map { (blockPosition, ghostBlockData) ->
                    Position.block(blockPosition.x, blockPosition.y, blockPosition.z) to ghostBlockData.block
                }.toMap())
                Thread.sleep(5)
            }
            blocksToSend[this.uniqueId]?.remove(next)
            running = false
        }.runAsync()
    }.attachedTo(this).condition { blocksToSend[this.uniqueId] != null }.runTimer(0, 1)
    tasks[this.uniqueId] = task.tasks[0]

}