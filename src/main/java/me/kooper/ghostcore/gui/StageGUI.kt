package me.kooper.ghostcore.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.PaginatedGui
import me.kooper.ghostcore.GhostCore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player


class StageGUI(private val player: Player) {
    init {
        val gui: PaginatedGui =
            Gui.paginated().title(Component.text("Ghost Core ✦ Stages")).rows(6).pageSize(45).disableAllInteractions()
                .create()
        gui.setItem(
            6,
            3,
            ItemBuilder.from(Material.PAPER).name(Component.text("Previous").decoration(TextDecoration.ITALIC, false))
                .asGuiItem { gui.previous() })
        gui.setItem(
            6,
            7,
            ItemBuilder.from(Material.PAPER).name(Component.text("Next").decoration(TextDecoration.ITALIC, false))
                .asGuiItem { gui.next() })
        gui.setItem(
            listOf(0, 9, 18, 27, 36, 45, 46, 48, 49, 50, 52, 53, 44, 35, 26, 17, 8, 7, 6, 5, 4, 3, 2, 1),
            ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(
                Component.text("")
            ).asGuiItem()
        )

        for ((name, stage) in GhostCore.getInstance().stageManager.stages) {
            val material = if (stage.audience.size == 0) Material.BARRIER else Material.NOTE_BLOCK
            gui.addItem(
                ItemBuilder.from(material).name(
                    Component.text(name).color(TextColor.color(197, 205, 216)).decoration(TextDecoration.ITALIC, false)
                ).asGuiItem { ViewGUI(player, stage) })
        }
        gui.open(player)
    }

}