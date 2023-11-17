package me.kooper.ghostcore.models

import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.data.AudienceData
import me.kooper.ghostcore.data.ChunkData
import me.kooper.ghostcore.data.PatternData
import me.kooper.ghostcore.data.ViewData
import me.kooper.ghostcore.events.JoinStageEvent
import me.kooper.ghostcore.events.LeaveStageEvent
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


@Suppress("UnstableApiUsage")
class Stage(
    val world: World,
    val name: String,
    val audience: AudienceData,
    val views: HashMap<String, ViewData>,
    val blocks: ConcurrentHashMap<Position, BlockData>,
    val chunks: Long2ObjectOpenHashMap<HashMap<String, ChunkData>>
) {

    constructor(world: World, name: String, audience: AudienceData) : this(
        world,
        name,
        audience,
        HashMap(),
        ConcurrentHashMap(),
        Long2ObjectOpenHashMap()
    )


    /**
     * Deletes a view by hiding it and removing it from the views map.
     * @param name The name of the view to be deleted.
     */
    fun deleteView(name: String) {
        hideView(name)
        views.remove(name)
    }

    /**
     * Retrieves a set of positions between two given positions.
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return A set of positions between pos1 and pos2.
     */
    fun getPositionsBetween(pos1: Position, pos2: Position): Set<Position> {
        val positions = HashSet<Position>(
            (pos1.blockX() - pos2.blockX()).absoluteValue *
                    (pos1.blockY() - pos2.blockY()).absoluteValue *
                    (pos1.blockZ() - pos2.blockZ()).absoluteValue
        )

        val bottomBlockX = min(pos1.blockX(), pos2.blockX()).coerceIn(-64, 320)
        val topBlockX = max(pos1.blockX(), pos2.blockX()).coerceIn(-64, 320)

        val bottomBlockY = min(pos1.blockY(), pos2.blockY()).coerceIn(-64, 320)
        val topBlockY = max(pos1.blockY(), pos2.blockY()).coerceIn(-64, 320)

        val bottomBlockZ = min(pos1.blockZ(), pos2.blockZ()).coerceIn(-64, 320)
        val topBlockZ = max(pos1.blockZ(), pos2.blockZ()).coerceIn(-64, 320)

        repeat(topBlockX - bottomBlockX + 1) { x ->
            repeat(topBlockZ - bottomBlockZ + 1) { z ->
                repeat(topBlockY - bottomBlockY + 1) { y ->
                    positions.add(Position.block(bottomBlockX + x, bottomBlockY + y, bottomBlockZ + z))
                }
            }
        }

        return positions
    }

    /**
     * Calculates the percentage of non-air blocks in a given view.
     * @param name The name of the view.
     * @return The percentage of non-air blocks.
     */
    fun getPercentAir(name: String): Double {
        val view: ViewData = views[name]!!

        val nonAirCount = view.blocks.count { it.value.material != Material.AIR }
        val totalBlocks = view.blocks.size.toDouble()

        val percent = nonAirCount / totalBlocks
        return percent
    }

    /**
     * Retrieves the list of players who are viewing the stage.
     * @return The list of players viewing the stage.
     */
    fun getViewers(): List<Player> {
        return audience.viewers.mapNotNull { viewerUUID ->
            Bukkit.getPlayer(viewerUUID)
        }.toList()
    }

    /**
     * Retrieves the view that contains a given position.
     * @param position The position to search for in views.
     * @return The ViewData containing the specified position, or null if not found.
     */
    fun getViewFromPos(position: Position): ViewData? {
        return views.values.firstOrNull { it.blocks.containsKey(position) }
    }

    /**
     * Shows a specific view to all the viewers.
     * @param name The name of the view to be shown.
     */
    fun showView(name: String) {
        for (viewer in getViewers()) {
            if (viewer.world != world) continue
            viewer.sendMultiBlockChange(getSolidBlocks(name))
        }
    }

    /**
     * Hides a specific view from all the viewers by replacing its blocks with air.
     * @param name The name of the view to be hidden.
     */
    fun hideView(name: String) {
        for (viewer in getViewers()) {
            val blocks: ConcurrentHashMap<Position, BlockData> = getBlocks(name)
            blocks.keys.forEach { pos -> blocks[pos] = Material.AIR.createBlockData() }
            viewer.sendMultiBlockChange(blocks)
        }
    }

    /**
     * Checks whether a given position is within the view identified by the specified name.
     *
     * @param name The name of the view to check.
     * @param position The position to check within the view.
     * @return `true` if the position is within the view, `false` otherwise.
     * @throws NoSuchElementException if the specified view name is not found in the 'views' map.
     */
    fun isWithinView(name: String, position: Position): Boolean {
        val view: ViewData = views[name]!!
        return view.blocks[position] != null
    }

    /**
     * Updates the view of a specific chunk for all the viewers.
     * @param name The name of the view to be updated.
     * @param chunk The key of the chunk to be updated.
     */
    fun updateChunkView(name: String, chunk: Long) {
        for (viewer in getViewers()) {
            viewer.sendMultiBlockChange(chunks[chunk][name]!!.blocks)
        }
    }

    /**
     * Sets a block in a specific view to air, updating the viewers.
     * @param name The name of the view containing the block.
     * @param position The position of the block to be set to air.
     * @param update If the block should update to the audience
     */
    fun setAirBlock(name: String, position: Position, update: Boolean) {
        val view: ViewData = views[name]!!
        view.blocks[position] = Material.AIR.createBlockData()
        chunks[getChunkFromPos(position)][name]!!.blocks[position] = Material.AIR.createBlockData()
        blocks[position] = Material.AIR.createBlockData()
        if (!update) return
        getViewers().forEach { player ->
            player.sendBlockChange(
                position.toLocation(world),
                Material.AIR.createBlockData()
            )
        }
    }

    /**
     * Sets multiple blocks asynchronously in a specific view to air, updating the viewers.
     * @param name The name of the view containing the blocks.
     * @param positions The set of positions of the blocks to be set to air.
     */
    fun setAirBlocks(name: String, positions: Set<Position>) {
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                positions.forEach { pos -> setAirBlock(name, pos, false) }
                val change: HashMap<Position, BlockData> = HashMap()
                positions.forEach { pos -> change[pos] = Material.AIR.createBlockData() }
                getViewers().forEach { player ->
                    player.sendMultiBlockChange(change)
                }
            }
        })
    }

    /**
     * Resets all blocks asynchronously in a specific view to their original pattern, updating the viewers.
     * @param name The name of the view to be reset.
     */
    fun resetBlocks(name: String) {
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                val view: ViewData = views[name]!!

                blocks.forEach{
                    resetBlock(view.name, it.key, false)
                }

                showView(view.name)
            }
        })
    }

    /**
     * Resets a block in a specific view to their original pattern, updating the viewers.
     * @param name The name of the view to be reset.
     * @param position The position of the view to be reset.
     * @param update If the block should update to the audience
     */
    fun resetBlock(name: String, position: Position, update: Boolean) {
        val view: ViewData = views[name]!!

        val randomBlockData = view.patternData.getRandomBlockData()
        blocks[position] = randomBlockData
        chunks[getChunkFromPos(position)][name]!!.blocks[position] = randomBlockData
        view.blocks[position] = randomBlockData

        if (update) showView(name)
    }

    /**
     * Retrieves only the solid blocks from a specific view.
     * @param name The name of the view to get solid blocks from.
     * @return A map of solid blocks in the specified view.
     */
    fun getSolidBlocks(name: String): Map<Position, BlockData> {
        val blocks = getBlocks(name)
        blocks.entries.removeIf { it.value.material == Material.AIR }
        return blocks
    }

    /**
     * Retrieves all blocks from a specific view.
     * @param name The name of the view to get blocks from.
     * @return A map of all blocks in the specified view.
     */
    fun getBlocks(name: String): ConcurrentHashMap<Position, BlockData> {
        return views[name]?.blocks ?: ConcurrentHashMap()
    }

    /**
     * Retrieves the [BlockData] associated with the specified name and position.
     *
     * @param name The name of the view containing the desired block.
     * @param position The position of the block within the view.
     * @return The [BlockData] at the specified position, or `null` if not found.
     *
     * @throws NullPointerException if the specified name is not present in the views map.
     */
    fun getBlock(name: String, position: Position): BlockData? {
        return views[name]?.blocks?.get(position)
    }

    /**
     * Changes the pattern of all blocks in a specific view and updates the blocks to the audience.
     * @param name The name of the view to change the pattern of.
     * @param newPatternData The new pattern for the blocks.
     */
    fun changePattern(name: String, newPatternData: PatternData) {
        views[name]!!.patternData = newPatternData
        resetBlocks(name)
    }

    /**
     * Adds a set of blocks to a specific view, updating the viewers.
     * @param name The name of the view to add blocks to.
     * @param blocks The set of blocks to be added.
     */
    fun addBlocks(name: String, blocks: Set<Block>) {
        val view = views[name]!!
        val chunkBlocks = HashMap<Long, HashMap<Position, BlockData>>()

        blocks.forEach { block ->
            val blockData = view.patternData.getRandomBlockData()
            chunkBlocks.computeIfAbsent(block.chunk.chunkKey) { HashMap() }[Position.block(block.location)] = blockData
            view.blocks[Position.block(block.location)] = blockData
            this.blocks[Position.block(block.location)] = blockData
        }

        chunkBlocks.forEach { (chunk, data) ->
            if (chunks[chunk] != null) {
                chunks[chunk][name] = ChunkData(data)
            } else {
                chunks[chunk] = hashMapOf(Pair(name, ChunkData(data)))
            }
        }

        showView(name)
    }

    /**
     * Removes a set of blocks from a specific view, updating the viewers.
     * @param name The name of the view to remove blocks from.
     * @param blocks The set of positions of the blocks to be removed.
     */
    fun removeBlocks(name: String, blocks: Set<Position>) {
        val view: ViewData = views[name]!!
        view.blocks.keys.removeAll(blocks)
        showView(name)
    }

    /**
     * Creates a new view with the specified name, blocks, positions, and block pattern.
     * @param name The name of the new view.
     * @param blocks The set of blocks to be included in the view.
     * @param pos1 The first position defining the bounding box of the view.
     * @param pos2 The second position defining the bounding box of the view.
     * @param pattern The block pattern for the blocks in the view.
     * @return The newly created ViewData object, or null if a view with the same name already exists.
     */
    fun createView(
        name: String,
        blocks: HashSet<Block>,
        pos1: Position,
        pos2: Position,
        pattern: PatternData,
        isBreakable: Boolean
    ): ViewData? {
        if (views.containsKey(name)) {
            Bukkit.getLogger().severe("View with $name already exists for stage $name!")
            return null
        }
        val view = ViewData(name, ConcurrentHashMap(), pos1, pos2, pattern, isBreakable)
        views[name] = view
        addBlocks(name, blocks)
        return view
    }

    /**
     * Adds a player to the audience of the stage, updating the player's view.
     * @param player The player to be added to the audience.
     */
    fun addPlayer(player: Player) {
        audience.viewers.add(player.uniqueId)

        JoinStageEvent(player, audience).callEvent()

        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                for (view in views.keys) {
                    player.sendMultiBlockChange(getSolidBlocks(view))
                }
            }
        })
    }

    /**
     * Removes a player from the audience of the stage, updating the player's view.
     * @param player The player to be removed from the audience.
     */
    fun removePlayer(player: Player) {
        audience.viewers.remove(player.uniqueId)

        LeaveStageEvent(player, audience).callEvent()

        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                for (view in views.keys) {
                    val blocks = getBlocks(view).toMutableMap()
                    blocks.keys.forEach { pos ->
                        blocks[pos] = Material.AIR.createBlockData()
                    }
                    player.sendMultiBlockChange(blocks)
                }
            }
        })
    }

    /**
     * Converts a Position object to a corresponding chunk key.
     * @param position The position to get the chunk key from.
     * @return The chunk key corresponding to the given position.
     */
    fun getChunkFromPos(position: Position): Long {
        return Chunk.getChunkKey(position.blockX() shr 4, position.blockZ() shr 4)
    }

    /**
     * Converts a Position object to a corresponding long value.
     * @param position The position to be converted.
     * @return The long value representing the given position.
     */
    fun positionToLong(position: Position): Long {
        val x = position.x().toLong()
        val y = position.y().toLong()
        val z = position.z().toLong()

        return (x shl 40) or ((y and 0xFFFFFL) shl 20) or (z and 0xFFFFFL)
    }

    /**
     * Converts a long value to a corresponding Position object.
     * @param key The long value to be converted.
     * @return The Position object corresponding to the given long value.
     */
    fun longToPosition(key: Long): Position {
        val x = (key shr 40).toInt()
        val y = ((key shr 20) and 0xFFFFFL).toInt()
        val z = (key and 0xFFFFFL).toInt()

        return Position.block(x, y, z)
    }

}