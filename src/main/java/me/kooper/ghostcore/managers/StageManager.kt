package me.kooper.ghostcore.managers

import me.kooper.ghostcore.models.Stage
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StageManager {
    val stages: ConcurrentHashMap<UUID, Stage> = ConcurrentHashMap()

    fun getStages(player: Player) : List<Stage> {
        return stages.values.filter { it.audience.viewers.contains(player.uniqueId) }
    }
}