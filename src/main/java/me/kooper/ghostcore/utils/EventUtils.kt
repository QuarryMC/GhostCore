package me.kooper.ghostcore.utils

import me.kooper.ghostcore.GhostCore
import org.bukkit.Bukkit
import org.bukkit.event.Event

fun Event.callEventSync() {
    Bukkit.getScheduler().runTask(GhostCore.getInstance(), Runnable {
        this.callEvent()
    })
}