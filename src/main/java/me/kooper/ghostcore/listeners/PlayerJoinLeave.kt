package me.kooper.ghostcore.listeners

import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.data.AudienceData
import me.kooper.ghostcore.models.Stage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class PlayerJoinLeave : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid: UUID = UUID.randomUUID()
        GhostCore.instance.stageManager.stages[uuid] = Stage(uuid, AudienceData(arrayListOf(event.player.uniqueId)))

        val player: Player = event.player
        val blocks: HashSet<Block> = HashSet()
        for (x in 60 downTo -60) {
            for (y in 30..120) {
                for (z in 60 downTo -60) {
                    blocks.add(player.world.getBlockAt(Location(player.world, x.toDouble(), y.toDouble(), z.toDouble())))
                }
            }
        }
        GhostCore.instance.stageManager.stages[uuid]!!.createView("diamonds", blocks, Material.DIAMOND_BLOCK)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
    }

}