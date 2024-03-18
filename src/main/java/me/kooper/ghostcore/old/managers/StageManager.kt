package me.kooper.ghostcore.old.managers

import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.old.events.DeleteStageEvent
import me.kooper.ghostcore.old.events.SpectateStageEvent
import me.kooper.ghostcore.old.events.StageCreateEvent
import me.kooper.ghostcore.old.models.Stage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class StageManager {
    val stages: ConcurrentHashMap<String, Stage> = ConcurrentHashMap()
    val spectatorPrevStages: HashMap<UUID, ArrayList<Stage>> = HashMap()
    val spectators: HashMap<UUID, Stage> = HashMap()

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
        Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable { run { StageCreateEvent(stage).callEvent() } })
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
        Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable { run { DeleteStageEvent(stage).callEvent() } })
        for (viewer in stage.getViewers()) {
            if (spectators[viewer.uniqueId] != null) {
                toggleSpectate(viewer, stage)
                viewer.sendMessage(Component.text("The player you were spectating has left.").color(TextColor.color(233, 233, 233)))
            }
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

    fun toggleSpectate(player: Player, stage: Stage) {
        if (spectatorPrevStages[player.uniqueId] != null && !stage.audience.contains(player.uniqueId)) {
            player.sendMessage(
                Component.text("You are already spectating a player.").color(TextColor.color(233, 233, 233))
            )
            return
        }
        if (stage.audience.contains(player.uniqueId)) {
            stage.removePlayer(player)
            Bukkit.getScheduler().runTaskLater(GhostCore.getInstance(), Runnable {
                spectatorPrevStages[player.uniqueId]?.forEach {
                    it.addPlayer(player)
                }
                spectatorPrevStages.remove(player.uniqueId)
                spectators.remove(player.uniqueId)
            }, 20L)
            SpectateStageEvent(player, stage, false).callEvent()
        } else {
            spectatorPrevStages[player.uniqueId] = ArrayList(getStages(player))
            spectators[player.uniqueId] = stage
            spectatorPrevStages[player.uniqueId]?.forEach {
                it.removePlayer(player)
            }
            Bukkit.getScheduler().runTaskLater(GhostCore.getInstance(), Runnable {
                stage.addPlayer(player)
            }, 20L)
            SpectateStageEvent(player, stage, true).callEvent()
        }
    }

}