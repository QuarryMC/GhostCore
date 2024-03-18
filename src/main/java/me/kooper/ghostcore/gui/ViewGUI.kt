package me.kooper.ghostcore.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import dev.triumphteam.gui.guis.PaginatedGui
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.old.models.Stage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class ViewGUI(player: Player, stage: Stage) {

    private val gui: PaginatedGui =
        Gui.paginated().title(Component.text("Stage ✦ ${stage.name}")).rows(6).pageSize(45)
            .disableAllInteractions().create()

    init {
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
            listOf(27, 36, 45, 46, 48, 49, 50, 52, 53, 35, 44) + (0..26).toList(), ItemBuilder.from(
                Material.GRAY_STAINED_GLASS_PANE
            ).name(
                Component.text("")
            ).asGuiItem()
        )

        val getAudienceLore: () -> List<Component> = {
            var audienceLore: ArrayList<Component> = ArrayList()
            for (player in stage.getViewers()) {
                audienceLore.add(Component.text(" ➥ ${player.name}"))
            }
            audienceLore = audienceLore.map { line ->
                line.decoration(TextDecoration.ITALIC, false).color(TextColor.color(233, 233, 233))
            } as ArrayList<Component>
            audienceLore
        }
        val audienceItem = ItemBuilder.from(Material.PLAYER_HEAD).name(
            Component.text("Stage Audience").color(
                TextColor.color(113, 113, 113)
            ).decoration(TextDecoration.ITALIC, false)
        ).lore(
            getAudienceLore.invoke()
        )
        gui.setItem(14, audienceItem.asGuiItem())

        gui.setItem(12, createItem(
            ItemBuilder.from(Material.ENDER_EYE).name(
                Component.text("Spectate").color(
                    TextColor.color(113, 113, 113)
                ).decoration(TextDecoration.ITALIC, false)
            ),
            {
                GhostCore.getInstance().stageManager.toggleSpectate(player, stage)
                gui.updateItem(14, audienceItem.lore(getAudienceLore.invoke()).asGuiItem())
            },
            {
                listOf(
                    Component.text(""),
                    Component.text(if (stage.audience.contains(player.uniqueId)) "Click to Hide" else "Click to Spectate")
                        .color(TextColor.color(233, 233, 233)).decoration(TextDecoration.ITALIC, false)
                )
            }
        ))

        for ((name, view) in stage.views) {
            gui.addItem(createItem(
                ItemBuilder.from(Material.SPYGLASS).name(
                    Component.text(name).color(
                        TextColor.color(113, 113, 113)
                    ).decoration(TextDecoration.ITALIC, false)
                ),
                {
                    view.isBreakable = !view.isBreakable
                },
                {
                    var viewLore: ArrayList<Component> = arrayListOf(
                        Component.text(""),
                        Component.text("Blocks: ${view.blocks.size}"),
                        Component.text("Breakable: ${view.isBreakable}"),
                        Component.text(""),
                        Component.text("Pattern:")
                    )
                    for ((data, chance) in view.patternData.blockDataPercentages) {
                        viewLore.add(Component.text(" ➥ ${data.material.name}: ${chance}%"))
                    }
                    viewLore.addAll(listOf(Component.text(""), Component.text("Click to toggle breakability")))
                    viewLore = viewLore.map { line ->
                        line.decoration(TextDecoration.ITALIC, false).color(TextColor.color(233, 233, 233))
                    } as ArrayList<Component>
                    viewLore
                }
            ))
        }

        gui.setCloseGuiAction {
            Bukkit.getScheduler().runTaskLater(GhostCore.getInstance(), Runnable { run { StageGUI(player) } }, 1L)
        }

        gui.open(player)
    }

    private fun createItem(item: ItemBuilder, action: Runnable, lore: () -> List<Component>): GuiItem {
        return item.lore(lore.invoke()).asGuiItem {
            action.run()
            gui.updateItem(it.slot, createItem(item, action, lore))
        }
    }

}