package me.kooper.ghostcore.commands

import me.kooper.ghostcore.gui.StageGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GhostCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return true
        val player: Player = sender
        if (!player.hasPermission("ghostcore.admin")) {
            player.sendMessage(Component.text("You do not have permission to use this command!").color(TextColor.color(255, 204, 205)))
            return true
        }
        StageGUI(player)
        return true
    }

}