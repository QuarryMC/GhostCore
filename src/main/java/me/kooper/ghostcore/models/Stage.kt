package me.kooper.ghostcore.models

import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.data.PatternData
import me.kooper.ghostcore.data.ViewData
import me.kooper.ghostcore.events.JoinStageEvent
import me.kooper.ghostcore.events.LeaveStageEvent
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Suppress("UnstableApiUsage")
class Stage(
    val world: World,
    val name: String,
    val audience: ArrayList<UUID>,
    val views: HashMap<String, ViewData>,
    val blocks: ConcurrentHashMap<Position, BlockData>,
    val chunks: Long2ObjectOpenHashMap<HashMap<String, HashMap<Position, BlockData>>>
) {

    constructor(world: World, name: String, audience: ArrayList<UUID>) : this(
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
        return audience.mapNotNull { viewerUUID ->
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
        return position.toVector().isInAABB(view.pos2.toVector(), view.pos1.toVector())
    }

    /**
     * Updates the view of a specific chunk for all the viewers.
     * @param name The name of the view to be updated.
     * @param chunk The key of the chunk to be updated.
     */
    fun updateChunkView(name: String, chunk: Long) {
        for (viewer in getViewers()) {
            viewer.sendMultiBlockChange(chunks[chunk][name]!!)
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
        setBlock(view.name, position, PatternData(mapOf(Pair(Material.AIR.createBlockData(), 1.0))), update)
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

                view.blocks.forEach{
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
        chunks[getChunkFromPos(position)][name]!![position] = randomBlockData
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
        println(newPatternData)
        views[name]!!.patternData = newPatternData
        resetBlocks(name)
    }

    /**
     * Adds a set of blocks to a specific view, updating the viewers.
     * @param name The name of the view to add blocks to.
     * @param blocks The set of blocks to be added.
     */
    fun addBlocks(name: String, positions: Set<Position>) {
        val view = views[name]!!
        val chunkBlocks = HashMap<Long, HashMap<Position, BlockData>>()

        for (position in positions) {
            if (this.blocks[position] != null) continue
            val blockData = view.patternData.getRandomBlockData()
            chunkBlocks.computeIfAbsent(getChunkFromPos(position)) { HashMap() }[position] = blockData
            view.blocks[position] = blockData
            this.blocks[position] = blockData
        }

        chunkBlocks.forEach { (chunk, data) ->
            if (chunks[chunk] != null) {
                chunks[chunk][name] = data
            } else {
                chunks[chunk] = hashMapOf(Pair(name, data))
            }
        }

        showView(name)
    }

    /**
     * Sets blocks in a specified view based on the provided positions, pattern, and update flag.
     *
     * @param name The name of the view.
     * @param positions The list of positions where blocks should be set.
     * @param pattern The pattern data used to determine the block data.
     * @param update Flag indicating whether to update viewers immediately.
     */
    fun setBlocks(name: String, positions: List<Position>, pattern: PatternData, update: Boolean) {
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable {
            run {
                val view: ViewData = views[name]!!

                positions.forEach {
                    setBlock(view.name, it, pattern, update)
                }
            }
        })
    }

    /**
     * Sets a block in a specified view at the given position based on the provided pattern and update flag.
     *
     * @param name The name of the view.
     * @param position The position where the block should be set.
     * @param pattern The pattern data used to determine the block data.
     * @param update Flag indicating whether to update viewers immediately.
     */
    fun setBlock(name: String, position: Position, pattern: PatternData, update: Boolean) {
        val view: ViewData = views[name]!!
        val blockData = pattern.getRandomBlockData()
        view.blocks[position] = blockData
        chunks[getChunkFromPos(position)][name]!![position] = blockData
        blocks[position] = blockData
        if (!update) return
        getViewers().forEach { player ->
            player.sendBlockChange(position.toLocation(world), blockData)
        }
    }

    /**
     * Removes a set of blocks from a specific view, updating the viewers.
     * @param name The name of the view to remove blocks from.
     * @param blocks The set of positions of the blocks to be removed.
     */
    fun removeBlocks(name: String, blocks: Set<Position>) {
        val view: ViewData = views[name]!!
        view.blocks.keys.removeAll(blocks)
        this.blocks.keys.removeAll(blocks)

        getViewers().forEach {
            it.sendMultiBlockChange(hashMapOf<Position, BlockData>().apply {
                blocks.forEach { key ->
                    val chunk = chunks[getChunkFromPos(key)]
                    if (chunk != null && chunk[name] != null) {
                        chunk[name]!!.remove(key)
                        if (chunk[name]!!.isEmpty()) {
                            chunk.remove(name)
                        }
                    }
                    put(key, world.getBlockData(key.toLocation(world)))
                }
            })
        }
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
        positions: HashSet<Position>,
        pos1: Position,
        pos2: Position,
        pattern: PatternData,
        isBreakable: Boolean
    ): ViewData? {
        if (views.containsKey(name)) {
            Bukkit.getLogger().severe("View with $name already exists for stage ${this.name}!")
            return null
        }
        val view = ViewData(name, ConcurrentHashMap(), pos1, pos2, pattern, isBreakable)
        views[name] = view
        Bukkit.getScheduler().runTaskAsynchronously(GhostCore.instance, Runnable { run { addBlocks(name, positions) } })
        return view
    }

    /**
     * Adds a player to the audience of the stage, updating the player's view.
     * @param player The player to be added to the audience.
     */
    fun addPlayer(player: Player) {
        audience.add(player.uniqueId)

        JoinStageEvent(player, audience, this).callEvent()

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
        audience.remove(player.uniqueId)

        LeaveStageEvent(player, audience, this).callEvent()

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