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
        concurrentHashMap[key] = ConcurrentHashMap(value.toMutableMap())
    }

    this.chunkedMultiBlockChangeMutable(concurrentHashMap)
}

fun Player.chunkedMultiBlockChangeMutable(blocks: ConcurrentHashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>) {


    if (!tasksWorking || blocks.values.sumOf { it.size } <= 4500) {
        blocks.forEach { (_, blockData) ->
            blockData.forEach { (blockPosition, ghostBlockData) ->
                this.sendMultiBlockChange(
                    mapOf(
                        Position.block(
                            blockPosition.x,
                            blockPosition.y,
                            blockPosition.z
                        ) to ghostBlockData.block
                    )
                )
            }
        }
        // Add it to the task anyway
        if (tasksWorking) {
            if (blocksToSend[this.uniqueId] == null) {
                blocksToSend[this.uniqueId] = mutableListOf(blocks)
            } else {
                // Recreate the list with the old stuff included as well
                try {
                    blocksToSend[this.uniqueId]?.add(blocks) ?: mutableListOf(blocks)
                } catch (e: ArrayIndexOutOfBoundsException) {
                    println("Error adding blocks to list, recreating list instead: ${blocksToSend[this.uniqueId]?.size ?: 0}")
                    blocksToSend[this.uniqueId] = mutableListOf(blocks)
                }
            }
        }
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
        var next = blocksToSend[this.uniqueId]?.getOrNull(0)
        if (next == null) {
            blocksToSend.remove(this.uniqueId)
            return@SchedulerAssist
        }

        while (next?.entries?.sumOf { it.value.size } == 1) {
            val singleBlock = next.entries.first()
            this.sendBlockChange(Position.block(singleBlock.key.x, singleBlock.key.y, singleBlock.key.z).toLocation(world), singleBlock.value.entries.first().value.block)
            blocksToSend[this.uniqueId]?.removeAt(0)
            next = blocksToSend[this.uniqueId]?.getOrNull(0)
        }

        SchedulerAssist {
            next?.entries?.sortedBy {
                it.key.distanceSquared(SimplePosition(this.location.blockX/16, this.location.blockY/16, this.location.blockZ/16))
            }?.forEach { (_, blockData) ->
                this.sendMultiBlockChange(blockData.map { (blockPosition, ghostBlockData) ->
                    Position.block(blockPosition.x, blockPosition.y, blockPosition.z) to ghostBlockData.block
                }.toMap())
                Thread.sleep(2)
            }
            blocksToSend[this.uniqueId]?.remove(next)
            running = false
        }.runAsync()
    }.attachedTo(this).condition { blocksToSend[this.uniqueId] != null }.runTimer(0, 1)
    tasks[this.uniqueId] = task.tasks[0]

}