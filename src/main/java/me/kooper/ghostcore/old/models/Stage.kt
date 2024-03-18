package me.kooper.ghostcore.old.models

import io.papermc.paper.math.Position
import me.kooper.ghostcore.GhostCore
import me.kooper.ghostcore.old.data.ChunkedViewData
import me.kooper.ghostcore.old.data.PatternData
import me.kooper.ghostcore.utils.SimplePosition
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
    val views: HashMap<String, ChunkedViewData>,
    val chunks: HashMap<SimplePosition, ConcurrentHashMap<SimplePosition, BlockData>>
) {

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(GhostCore.getInstance(), Runnable {
            run {
                views.forEach {
                    showView(it.key)
                }
            }
        }, 0L, 600L)
    }

    constructor(world: World, name: String, audience: ArrayList<UUID>) : this(
        world,
        name,
        audience,
        HashMap(),
        HashMap()
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
    fun getViewFromPos(position: SimplePosition): ChunkedViewData? {
        return views.values.firstOrNull { it.hasBlock(position) }
    }

    /**
     * Shows a specific view to all the viewers.
     * @param name The name of the view to be shown.
     */
    fun showView(name: String) {
        Bukkit.broadcastMessage("Showing view $name")
        sendBlocks(views[name]!!.getAllBlocks())
    }

    /**
     * Hides a specific view from all the viewers by replacing its blocks with air.
     * @param name The name of the view to be hidden.
     */
    fun hideView(name: String) {
        val blocks: ConcurrentHashMap<SimplePosition, BlockData> = getBlocks(name)
        val airBlockData = Material.AIR.createBlockData()
        blocks.keys.forEach { pos -> blocks[pos] = airBlockData }
        sendBlocks(blocks)
    }

    /**
     * Checks whether a given position is within the view identified by the specified name.
     *
     * @param name The name of the view to check.
     * @param position The position to check within the view.
     * @return `true` if the position is within the view, `false` otherwise.
     * @throws NoSuchElementException if the specified view name is not found in the 'views' map.
     */
    fun isWithinView(name: String, position: SimplePosition): Boolean {
        val view: ChunkedViewData = views[name]!!
        return view.blocks[position] != null
    }

    /**
     * Updates the view of a specific chunk for all the viewers.
     * @param name The name of the view to be updated.
     * @param chunk The key of the chunk to be updated.
     */
    fun updateChunkView(name: String, chunk: SimplePosition) {
        sendBlocks(chunks[chunk]!!)
    }

    /**
     * Updates blocks by sending a multi block change (RUN ASYNC)
     * @param blocks The blocks to be updated.
     */
    fun sendBlocks(blocks: Map<SimplePosition, BlockData>) {
        for (viewer in getViewers()) {
            if (viewer.world != world) continue
            viewer.sendMessage("Sending blocks to ${viewer.name} in world ${viewer.world.name} with ${blocks.size} blocks.")
            viewer.sendMultiBlockChange(blocks.mapKeys { it.key.toBlockPosition() })
        }
    }

    /**
     * Sets a block in a specific view to air, updating the viewers.
     * @param name The name of the view containing the block.
     * @param position The position of the block to be set to air.
     * @param update Whether to update the view on the next tick.
     */
    fun setAirBlock(name: String, position: SimplePosition, update: Boolean) {
        val view: ChunkedViewData = views[name]!!
        setBlock(view.name, position, Material.AIR.createBlockData(), update)
    }

    /**
     * Sets multiple blocks in a specific view to air, updating the viewers. (CALL ASYNC)
     * @param name The name of the view containing the blocks.
     * @param positions The set of positions of the blocks to be set to air.
     */
    fun setAirBlocks(name: String, positions: Set<SimplePosition>) {
        val view: ChunkedViewData = views[name]!!
        val blocksToAdd = HashMap<SimplePosition, BlockData>()

        val airBlockData = Material.AIR.createBlockData()
        positions.forEach {
            if (view.blocks.containsKey(it)) {
                blocksToAdd[it] = airBlockData
            }
            setAirBlock(name, it, false)
        }

        view.blocksChange.putAll(blocksToAdd)
    }

    /**
     * Gets the highest altitude from an x-coord and z-coord
     * @param name The name of the view.
     * @param x The x-coordinate.
     * @param z The z-coordinate
     * @return Returns the highest block at that position.
     */
    fun getHighestPosition(name: String, x: Int, z: Int) : SimplePosition? {
        val view: ChunkedViewData = views[name]!!
        for (y in 256 downTo 0) {
            val block = view.getBlock(SimplePosition.from(x, y, z))
            if (block != null && block.material != Material.AIR) {
                return SimplePosition.from(x, y, z)
            }
        }
        return null
    }

    /**
     * Resets all blocks in a specific view to their original pattern, updating the viewers. (CALL ASYNC)
     * @param name The name of the view to be reset.
     */
    fun resetBlocks(name: String) {
        val view: ChunkedViewData = views[name]!!
        val blocksToAdd = HashMap<SimplePosition, BlockData>()

        view.getAllBlocks().forEach {
            val blockData = view.patternData.getRandomBlockData()
            resetBlock(view.name, it.key, blockData, false)
            blocksToAdd[it.key] = blockData
        }

        view.blocksChange.putAll(blocksToAdd)
    }

    /**
     * Resets all solid blocks in a specific view to their original pattern, updating the viewers. (CALL ASYNC)
     * @param name The name of the view to be reset.
     */
    fun resetSolidBlocks(name: String) {
        val view: ChunkedViewData = views[name]!!
        val blocksToAdd = HashMap<SimplePosition, BlockData>()

        getSolidBlocks(name).forEach {
            val blockData = view.patternData.getRandomBlockData()
            resetBlock(view.name, it.key, blockData, false)
            blocksToAdd[it.key] = blockData
        }

        view.blocksChange.putAll(blocksToAdd)
    }

    /**
     * Resets a block in a specific view to their original pattern, updating the viewers.
     * @param name The name of the view to be reset.
     * @param position The position of the view to be reset.
     * @param update Whether to update the view on the next tick.
     */
    fun resetBlock(name: String, position: SimplePosition, blockData: BlockData, update: Boolean) {
        val view: ChunkedViewData = views[name]!!

        chunks[position.getChunk()]!![position] = blockData
        view.setBlock(position, blockData)
        if (update) view.blocksChange[position] = blockData
    }

    /**
     * Retrieves only the solid blocks from a specific view.
     * @param name The name of the view to get solid blocks from.
     * @return A map of solid blocks in the specified view.
     */
    fun getSolidBlocks(name: String): Map<SimplePosition, BlockData> {
        val blocks = ConcurrentHashMap(getBlocks(name))
        blocks.entries.removeIf { it.value.material == Material.AIR }
        return blocks
    }

    /**
     * Retrieves all blocks from a specific view.
     * @param name The name of the view to get blocks from.
     * @return A map of all blocks in the specified view.
     */
    fun getBlocks(name: String): ConcurrentHashMap<SimplePosition, BlockData> {
        return views[name]?.getAllBlocks() ?: ConcurrentHashMap()
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
    fun getBlock(name: String, position: SimplePosition): BlockData? {
        return views[name]?.getBlock(position)
    }

    /**
     * Changes the pattern of all blocks in a specific view and updates the blocks to the audience. (CALL ASYNC)
     * @param name The name of the view to change the pattern of.
     * @param newPatternData The new pattern for the blocks.
     */
    fun changePattern(name: String, newPatternData: PatternData) {
        views[name]!!.patternData = newPatternData
        resetSolidBlocks(name)
    }

    /**
     * Adds a set of blocks to a specific view, updating the viewers. (CALL ASYNC)
     * @param name The name of the view to add blocks to.
     * @param positions The set of positions to be added.
     */
    fun addBlocks(name: String, positions: Set<SimplePosition>) {
        val view = views[name]!!
        val chunkBlocks = HashMap<SimplePosition, HashMap<SimplePosition, BlockData>>()
        val blocksToAdd = HashMap<SimplePosition, BlockData>()

        for (position in positions) {
            if (view.blocks[position] != null) continue
            val blockData = view.patternData.getRandomBlockData()
            chunkBlocks.computeIfAbsent(position.getChunk()) { HashMap() }[position] = blockData
            view.setBlock(position, blockData)
            blocksToAdd[position] = blockData
        }

        view.blocksChange.putAll(blocksToAdd)

        chunkBlocks.forEach { (chunk, data) ->
//            if (chunks[chunk] != null) {
//                chunks[chunk][name] = ConcurrentHashMap(data)
//            } else {
//                chunks[chunk] = hashMapOf(Pair(name, ConcurrentHashMap(data)))
//            }
            if (chunks[chunk] == null) {
                chunks[chunk] = ConcurrentHashMap()
            } else {
                chunks[chunk]!!
            }
        }
    }

    /**
     * Sets blocks in a specified view based on the provided positions, pattern, and update flag. (CALL ASYNC)
     *
     * @param name The name of the view.
     * @param positions The list of positions where blocks should be set.
     * @param pattern The pattern data used to determine the block data.
     */
    fun setBlocks(name: String, positions: List<SimplePosition>, pattern: PatternData) {
        val view: ChunkedViewData = views[name]!!
        val blocksToAdd = HashMap<SimplePosition, BlockData>()

        positions.forEach {
            val blockData = pattern.getRandomBlockData()
            setBlock(view.name, it, blockData, false)
            blocksToAdd[it] = blockData
        }

        view.blocksChange.putAll(blocksToAdd)
    }

    /**
     * Sets a block in a specified view at the given position based on the provided pattern and update flag.
     *
     * @param name The name of the view.
     * @param position The position where the block should be set.
     * @param pattern The pattern data used to determine the block data.
     * @param update Whether to update the view on the next tick.
     */
    fun setBlock(name: String, position: SimplePosition, pattern: PatternData, update: Boolean) {
        val view: ChunkedViewData = views[name]!!
        val blockData = pattern.getRandomBlockData()
        if (!view.blocks.containsKey(position)) {
            return
        }
        view.setBlock(position, blockData)
        if (update) view.blocksChange[position] = blockData
        chunks[position.getChunk()]!![position] = blockData
    }

    /**
     * Sets a block in a specified view at the given position based on the provided block data and update flag.
     *
     * @param name The name of the view.
     * @param position The position where the block should be set.
     * @param blockData The block data used for the block.
     * @param update Whether to update the view on the next tick.
     */
    fun setBlock(name: String, position: SimplePosition, blockData: BlockData, update: Boolean) {
        val view: ChunkedViewData = views[name]!!
        if (!view.blocks.containsKey(position)) {
            return
        }
        view.setBlock(position, blockData)
        if (update) view.blocksChange[position] = blockData
        chunks[position.getChunk()]!![position] = blockData
    }

    /**
     * Removes a set of blocks from a specific view, updating the viewers. (CALL ASYNC)
     * @param name The name of the view to remove blocks from.
     * @param blocks The set of positions of the blocks to be removed.
     */
    fun removeBlocks(name: String, blocks: Set<SimplePosition>) {
        val view: ChunkedViewData = views[name]!!
        val blocksToUpdate = HashMap<SimplePosition, BlockData>()
        view.removeBlocks(blocks)

        blocks.forEach {
            blocksToUpdate[it] = world.getBlockData(it.toLocation(world))
            val chunk = chunks[it.getChunk()]
            if (chunk != null && chunk[it] != null) {
                chunk.remove(it)
                if (chunk.isEmpty()) {
                    chunks.remove(it.getChunk())
                }
            }
        }

        view.blocksChange.putAll(blocksToUpdate)
    }

    /**
     * Creates a new view with the specified name, blocks, positions, and block pattern. (CALL ASYNC)
     * @param name The name of the new view.
     * @param positions The set of positions to be included in the view.
     * @param pattern The block pattern for the blocks in the view.
     * @return The newly created ViewData object, or null if a view with the same name already exists.
     */
    fun createView(
        name: String,
        positions: HashSet<SimplePosition>,
        pattern: PatternData,
        isBreakable: Boolean
    ): ChunkedViewData? {
        if (views.containsKey(name)) {
            Bukkit.getLogger().severe("View with $name already exists for stage ${this.name}!")
            return null
        }
        val view = ChunkedViewData(name, ConcurrentHashMap(), ConcurrentHashMap(), pattern, isBreakable)
        views[name] = view
        addBlocks(name, positions)
        return view
    }

    /**
     * Adds a player to the audience of the stage, updating the player's view. (CALL ASYNC)
     * @param player The player to be added to the audience.
     */
    fun addPlayer(player: Player) {
        audience.add(player.uniqueId)

        JoinStageEvent(player, audience, this).callEvent()

        if (player.world != world) return
        for (view in views.keys) {
            player.sendMultiBlockChange(getSolidBlocks(view).mapKeys { it.key.toBlockPosition() })
        }
    }

    /**
     * Removes a player from the audience of the stage, updating the player's view. (CALL ASYNC)
     * @param player The player to be removed from the audience.
     */
    fun removePlayer(player: Player) {
        audience.remove(player.uniqueId)

        LeaveStageEvent(player, audience, this).callEvent()

        for (view in views.keys) {
            val blocks = getBlocks(view).toMutableMap()
            val airBlockData = Material.AIR.createBlockData()
            blocks.keys.forEach { pos ->
                blocks[pos] = airBlockData
            }
            if (player.world != world) return
            player.sendMultiBlockChange(blocks.mapKeys { it.key.toBlockPosition() })
        }
    }


    /**
     * Converts a Position object to a corresponding chunk key.
     * @param position The position to get the chunk key from.
     * @return The chunk key corresponding to the given position.
     */
    fun getChunkFromPos(position: SimplePosition): Long {
        return Chunk.getChunkKey(position.x shr 4, position.z shr 4)
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