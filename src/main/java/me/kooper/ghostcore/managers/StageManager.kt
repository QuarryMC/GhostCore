package me.kooper.ghostcore.managers

import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.events.DeleteStageEvent
import me.kooper.ghostcore.events.StageCreateEvent
import me.kooper.ghostcore.models.Stage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class StageManager {
    val stages: ConcurrentHashMap<String, Stage> = ConcurrentHashMap()
    private val toBeDeleted: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(GhostCore.instance, Runnable {
            run {
                for ((name, stage) in stages) {
                    if (stage.getViewers().isNotEmpty() && !toBeDeleted.containsKey(name)) continue
                    if (toBeDeleted.containsKey(name)) {
                        if (stage.getViewers().isNotEmpty()) {
                            toBeDeleted.remove(name)
                            continue
                        }
                        if (System.currentTimeMillis() < toBeDeleted[name]!!) continue
                        toBeDeleted.remove(name)
                        deleteStage(stage)
                    } else {
                        toBeDeleted[name] = System.currentTimeMillis() + 60000
                    }
                }
            }
        }, 0L, 1200L)
    }

    /**
     * Get a list of stages that a player is currently viewing.
     *
     * @param player The player for which to retrieve stages.
     * @return A list of stages that the player is currently viewing.
     */
    fun getStages(player: Player) : List<Stage> {
        return stages.values.filter { it.audience.contains(player.uniqueId) }
    }

    /**
     * Create a new stage with the given name.
     *
     * @param stage The stage object to create and cache.
     * @return The created stage
     */
    fun createStage(stage: Stage) : Stage? {
        if (stages.containsKey(stage.name)) return null
        stages[stage.name] = stage
        StageCreateEvent(stage).callEvent()
        return stage
    }

    /**
     * Delete a stage by name. This function will log an error if the stage does not exist.
     *
     * @param name The name of the stage to delete.
     */
    fun deleteStage(name: String) {
        if (stages[name] == null) return
        deleteStage(stages[name]!!)
    }

    /**
     * Delete a stage. This function will remove all viewers from the stage and delete it.
     *
     * @param stage The stage to delete.
     */
    fun deleteStage(stage: Stage) {
        Bukkit.getScheduler().runTask(GhostCore.instance, Runnable { run { DeleteStageEvent(stage).callEvent() } })
        for (viewer in stage.getViewers()) {
            stage.removePlayer(viewer)
        }
        stages.remove(stage.name)
    }

    /**
     * Get a stage by name.
     *
     * @param name The name of the stage to retrieve.
     * @return The stage object with the given name, or null if it doesn't exist.
     */
    fun getStage(name: String) : Stage? {
        return stages[name]
    }

}