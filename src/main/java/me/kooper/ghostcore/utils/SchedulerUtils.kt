package me.kooper.ghostcore.utils

import me.kooper.ghostcore.GhostCore
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/*
Want:

SchedulerAssist(true/false) {
 // Code
}
 */
open class SchedulerAssist(private val function: () -> Unit) {

    val tasks = mutableListOf<BukkitTask>()
    private var attachedTo: LivingEntity? = null
    private var condition: (() -> Boolean)? = { true }

    private fun getRunnable(function: () -> Unit): Runnable = Runnable {
        if (condition != null && !condition!!()) {
            tasks.forEach { if (!it.isCancelled) it.cancel() }
            return@Runnable
        }

        if (attachedTo != null) {
            if (attachedTo is Player) {
                if (!(attachedTo as Player).isOnline) {
                    tasks.forEach { if (!it.isCancelled) it.cancel() }
                }
            } else {
                if (attachedTo!!.isDead) {
                    tasks.forEach { if (!it.isCancelled) it.cancel() }
                }
            }
        }
        function()
    }

    fun runSync(): SchedulerAssist {
        val task = Bukkit.getScheduler().runTask(GhostCore.getInstance(), getRunnable(function))
        tasks.add(task)
        return this
    }

    fun runAsync(): SchedulerAssist {
        val task = Bukkit.getScheduler().runTaskAsynchronously(GhostCore.getInstance(), getRunnable(function))
        tasks.add(task)
        return this
    }

    fun runLater(ticks: Long): SchedulerAssist {
        val task = Bukkit.getScheduler().runTaskLater(GhostCore.getInstance(), getRunnable(function), ticks)
        tasks.add(task)
        return this
    }

    fun runTimer(delay: Long, period: Long): SchedulerAssist {
        val task = Bukkit.getScheduler().runTaskTimer(GhostCore.getInstance(), getRunnable(function), delay, period)
        tasks.add(task)
        return this
    }

    fun runLaterAsync(ticks: Long): SchedulerAssist {
        val task = Bukkit.getScheduler().runTaskLaterAsynchronously(GhostCore.getInstance(), getRunnable(function), ticks)
        tasks.add(task)
        return this
    }

    fun runTimerAsync(delay: Long, period: Long): SchedulerAssist {
        val task = Bukkit.getScheduler().runTaskTimerAsynchronously(GhostCore.getInstance(), getRunnable(function), delay, period)
        tasks.add(task)
        return this
    }

    fun attachedTo(entity: LivingEntity): SchedulerAssist {
        if (attachedTo != null) {
            Bukkit.getLogger().warning("There was already an entity attached to a SchedulerAssist, but it was overwritten.")
        }

        this.attachedTo = entity
        return this
    }

    fun condition(condition: () -> Boolean): SchedulerAssist {
        this.condition = condition
        return this
    }
}

operator fun <T> (() -> T).not(): BukkitTask {
    return Bukkit.getScheduler().runTaskAsynchronously(GhostCore.getInstance(), Runnable { this() })
}