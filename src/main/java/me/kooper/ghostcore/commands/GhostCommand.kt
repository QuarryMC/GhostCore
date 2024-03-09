
package me.kooper.ghostcore.commands

import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.gui.StageGUI
import me.kooper.ghostcore.utils.PositionUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class GhostCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return true
        if (args == null || args.isEmpty()) {
            if (!sender.hasPermission("ghostcore.admin")) {
                sender.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(255, 204, 205)))
                return true
            }
            val player: Player = sender
            StageGUI(player)
            PositionUtils.getLocationsWithin(player.location.add(Vector(15, 0, 15)), player.location.add(Vector(-15, 0, -15)))
        } else {
            if (!sender.hasPermission("ghostcore.mod")) {
                sender.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(255, 204, 205)))
                return true
            }
            if (args[0].lowercase().contentEquals("spectate")) {
                val player: Player? = Bukkit.getPlayer(args[1])
                if (player == null || !player.isOnline) {
                    sender.sendMessage(Component.text("This player is not online!").color(TextColor.color(255, 204, 205)))
                    return true
                }
                if (player == sender) {
                    sender.sendMessage(Component.text("You can't spectate yourself!").color(TextColor.color(255, 204, 205)))
                    return true
                }
                GhostCore.instance.stageManager.toggleSpectate(sender, GhostCore.instance.stageManager.getStages(player)[0])
            }
        }
        return true
    }

}