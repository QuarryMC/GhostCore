package me.kooper.ghostcore.models

import io.papermc.paper.math.Position
import me.kooper.ghostcore.events.JoinStageEvent
import me.kooper.ghostcore.events.LeaveStageEvent
import me.kooper.ghostcore.old.data.PatternData
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

@Suppress("UNCHECKED_CAST", "UnstableApiUsage")
class ChunkedStage(
    override val world: World,
    override val name: String,
    override val audience: ArrayList<UUID>,
    override val views: HashMap<String, View>,
    override val chunks: HashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>
): Stage(world, name, audience, views, chunks) {
    override fun deleteView(name: String) {
        hideView(name)
        views.remove(name)
    }

    override fun showView(name: String) {
        val blocks = (views as HashMap<String, ChunkedView>)[name]?.getAllBlocksInBound() ?: return
        sendBlocks(blocks)
    }

    override fun hideView(name: String) {
        val blocks = (views as HashMap<String, ChunkedView>)[name]?.getAllBlocksInBound() ?: return
        sendBlocks(blocks)
    }

    override fun isWithinView(name: String, position: SimplePosition): Boolean {
        return (views as HashMap<String, ChunkedView>)[name]?.bound?.contains(position.toVector()) ?: false
    }

    override fun getViewFromPos(position: SimplePosition): View? {
        (views as HashMap<String, ChunkedView>).forEach { (_, view) ->
            if (view.bound.contains(position.toVector())) return view
        }
        return null
    }

    override fun createView(
        name: String,
        minPosition: SimplePosition,
        maxPosition: SimplePosition,
        pattern: PatternData,
        isBreakable: Boolean
    ): View? {
        if (views.containsKey(name)) return null

        val bound = BoundingBox.of(minPosition.toVector(), maxPosition.toVector())
        val view = ChunkedView(name, pattern, isBreakable, bound)
        views[name] = view
        return view
    }

    override fun sendBlocks(blocks: Map<SimplePosition, GhostBlockData>) {
        val mappedData = blocks.mapValues { it.value.getBlockData() }.mapKeys { it.key.toBlockPosition() }
        for (viewer in getViewers()) {
            viewer.sendMultiBlockChange(mappedData)
        }
    }

    override fun setAirBlock(name: String, position: SimplePosition, update: Boolean) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        view.removeBlock(position)
        if (update) sendBlocks(mapOf(position to GhostBlockData(Material.AIR.createBlockData())))
    }

    override fun setAirBlocks(name: String, positions: Set<SimplePosition>) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        view.removeBlocks(positions)
        if (positions.isNotEmpty()) sendBlocks(positions.associateWith { GhostBlockData(Material.AIR.createBlockData()) })
    }

    override fun resetBlocks(name: String) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blocksToSet: MutableMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()
        view.getAllBlocksInBound().forEach { (position, _) ->
            blocksToSet[position] = GhostBlockData(view.patternData.getRandomBlockData())
        }

        view.setBlocks(blocksToSet)
        sendBlocks(blocksToSet)
    }

    override fun resetSolidBlocks(name: String) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blocksToSet: MutableMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()

        getSolidBlocks(name).forEach { (position, _) ->
            blocksToSet[position] = GhostBlockData(view.patternData.getRandomBlockData())
        }

        view.setBlocks(blocksToSet)
    }

    override fun resetBlock(name: String, position: SimplePosition, blockData: GhostBlockData, update: Boolean) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        view.setBlock(position, blockData)
        if (update) sendBlocks(mapOf(position to blockData))
    }

    override fun getSolidBlocks(name: String): Map<SimplePosition, GhostBlockData> {
        return (views as HashMap<String, ChunkedView>)[name]?.getAllBlocksInBound()?.filter {it.value.getBlockData().material.isSolid} ?: emptyMap()
    }

    override fun getBlocks(name: String): ConcurrentHashMap<SimplePosition, GhostBlockData> {
        return (views as HashMap<String, ChunkedView>)[name]?.getAllBlocksInBound()!! as ConcurrentHashMap<SimplePosition, GhostBlockData>
    }

    override fun getBlock(name: String, position: SimplePosition): GhostBlockData? {
        return (views as HashMap<String, ChunkedView>)[name]?.getBlock(position)
    }

    override fun changePattern(name: String, newPatternData: PatternData) {
        (views as HashMap<String, ChunkedView>)[name]?.patternData = newPatternData
    }

    override fun addBlocks(name: String, positions: Set<SimplePosition>) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blocksToSet: MutableMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()
        positions.forEach { blocksToSet[it] = GhostBlockData(view.patternData.getRandomBlockData()) }
        view.setBlocks(blocksToSet)
        sendBlocks(blocksToSet)
    }

    override fun setBlocks(name: String, positions: List<SimplePosition>, pattern: PatternData) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blocksToSet: MutableMap<SimplePosition, GhostBlockData> = ConcurrentHashMap()
        positions.forEach { blocksToSet[it] = GhostBlockData(pattern.getRandomBlockData()) }
        view.setBlocks(blocksToSet)
        sendBlocks(blocksToSet)
    }

    override fun setBlock(name: String, position: SimplePosition, pattern: PatternData, update: Boolean) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blockData = GhostBlockData(pattern.getRandomBlockData())
        view.setBlock(position, blockData)
        if (update) sendBlocks(mapOf(position to blockData))
    }

    override fun setBlock(name: String, position: SimplePosition, blockData: GhostBlockData, update: Boolean) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        view.setBlock(position, blockData)
        if (update) sendBlocks(mapOf(position to blockData))
    }

    override fun removeBlocks(name: String, blocks: Set<SimplePosition>) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        view.removeBlocks(blocks)
        sendBlocks(blocks.associateWith { GhostBlockData(Material.AIR.createBlockData()) })
    }

    override fun getViewers(): List<Player> {
        return audience.mapNotNull { Bukkit.getPlayer(it) }
    }

    override fun addPlayer(player: Player) {
        audience.add(player.uniqueId)
        JoinStageEvent(player, audience, this).callEvent()

        if (player.world != world) return
        for (view in views.keys) {
            player.sendMultiBlockChange((views as HashMap<String, ChunkedView>)[view]?.getAllBlocks()?.mapValues { it.value.getBlockData() }?.mapKeys { it.key.toBlockPosition() } ?: emptyMap())
        }
    }

    override fun removePlayer(player: Player) {
        audience.remove(player.uniqueId)
        LeaveStageEvent(player, audience, this).callEvent()

        if (player.world != world) return
        for (view in views.keys) {
            val blocks = getBlocks(view).toMutableMap()
            val airBlock = GhostBlockData(Material.AIR.createBlockData())
            blocks.forEach { (position, _) -> blocks[position] = airBlock }
            player.sendMultiBlockChange(blocks.mapValues { it.value.getBlockData() }.mapKeys { it.key.toBlockPosition() })
        }
    }

    override fun getHighestPosition(name: String, x: Int, z: Int): SimplePosition? {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return null
        val min = view.bound.min.toBlockVector()
        val max = view.bound.max.toBlockVector()
        for (y in max.blockY downTo min.blockY) {
            val position = SimplePosition(x, y, z)
            if (view.hasBlock(position)) return position
        }
        return null
    }

    override fun updateChunkView(name: String, chunk: SimplePosition) {
        val view = (views as HashMap<String, ChunkedView>)[name] ?: return
        val blocks = view.getBlocksInChunk(chunk)
        sendBlocks(blocks)
    }


}