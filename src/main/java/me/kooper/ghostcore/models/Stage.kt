package me.kooper.ghostcore.models

import me.kooper.ghostcore.utils.PatternData
import me.kooper.ghostcore.utils.types.GhostBlockData
import me.kooper.ghostcore.utils.types.SimplePosition
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class Stage(
    open val world: World,
    open val name: String,
    open val audience: ArrayList<UUID>,
    open val views: HashMap<String, View>, // ? extends View in Kotlin: https://kotlinlang.org/docs/generics.html#use-site-variance
    open val chunks: HashMap<SimplePosition, ConcurrentHashMap<SimplePosition, GhostBlockData>>
) {

    // Constructors and initialization code remains the same

    // **** View Management Functions ****

    /**
     * Deletes a view by hiding it and removing it from the 'views' map.
     *
     * @param name The name of the view to be deleted.
     */
    abstract fun deleteView(name: String)

    /**
     * Shows a specific view to all the viewers of the stage.
     *
     * @param name The name of the view to be shown.
     */
    abstract fun showView(name: String)

    /**
     * Hides a specific view from all the viewers of the stage.
     *
     * @param name The name of the view to be hidden.
     */
    abstract fun hideView(name: String)

    /**
     * Checks whether a given position is within a specified view.
     *
     * @param name The name of the view to check.
     * @param position The position to check within the view.
     * @return `true` if the position is within the view, `false` otherwise.
     */
    abstract fun isWithinView(name: String, position: SimplePosition): Boolean

    /**
     * Retrieves the view that contains a given position.
     *
     * @param position The position to search for in views.
     * @return The `View` containing the specified position, or `null` if not found.
     */
    abstract fun getViewFromPos(position: SimplePosition): View?

    /**
     * Creates a new view with the specified name, blocks, positions, and block pattern. (CALL ASYNC)
     *
     * @param name The name of the new view.
     * @param positions The set of positions to be included in the view.
     * @param pattern The block pattern for the blocks in the view.
     * @param isBreakable Whether blocks in the view can be broken.
     * @return The newly created `View` object, or `null` if a view with the same name already exists.
     */
    abstract fun createView(name: String, minPosition: SimplePosition, maxPosition: SimplePosition, pattern: PatternData, isBreakable: Boolean): View?

    // **** Block Manipulation Functions ****

    /**
     * Updates blocks by sending a multi block change (RUN ASYNC)
     *
     * @param blocks The blocks to be updated, with positions as keys and GhostBlockData as values.
     */
    abstract fun sendBlocks(blocks: Map<SimplePosition, GhostBlockData>)

    /**
     * Sets a block in a specific view to air, updating the viewers.
     *
     * @param name The name of the view containing the block.
     * @param position The position of the block to be set to air.
     * @param update Whether to update the view immediately.
     */
    abstract fun setAirBlock(name: String, position: SimplePosition, update: Boolean)

    /**
     * Sets multiple blocks in a specific view to air, updating the viewers (CALL ASYNC)
     *
     * @param name The name of the view containing the blocks.
     * @param positions The set of positions of the blocks to be set to air.
     */
    abstract fun setAirBlocks(name: String, positions: Set<SimplePosition>)

    /**
     * Resets all blocks in a specific view to their original pattern, updating the viewers (CALL ASYNC)
     *
     * @param name The name of the view to be reset.
     */
    abstract fun resetBlocks(name: String)

    /**
     * Resets all solid blocks in a specific view to their original pattern, updating the viewers (CALL ASYNC)
     *
     * @param name The name of the view to be reset.
     */
    abstract fun resetSolidBlocks(name: String)

    /**
     * Resets a block in a specific view to its original pattern, updating the viewers.
     *
     * @param name The name of the view containing the block.
     * @param position The position of the block to be reset.
     * @param blockData The new block data for the block.
     * @param update Whether to update the view immediately.
     */
    abstract fun resetBlock(name: String, position: SimplePosition, blockData: GhostBlockData, update: Boolean)

    /**
     * Retrieves only the solid blocks from a specific view.
     *
     * @param name The name of the view to get solid blocks from.
     * @return A map of solid blocks with their positions in the specified view.
     */
    abstract fun getSolidBlocks(name: String): Map<SimplePosition, GhostBlockData>

    /**
     * Retrieves all blocks from a specific view.
     *
     * @param name The name of the view to get blocks from.
     * @return A map of all blocks with their positions in the specified view.
     */
    abstract fun getBlocks(name: String): ConcurrentHashMap<SimplePosition, GhostBlockData>

    /**
     * Retrieves the block data associated with the specified name and position.
     *
     * @param name The name of the view containing the desired block.
     * @param position The position of the block within the view.
     * @return The GhostBlockData at the specified position, or `null` if not found.
     */
    abstract fun getBlock(name: String, position: SimplePosition): GhostBlockData?

    /**
     * Changes the pattern of all blocks in a specific view and updates the blocks to the audience (ASYNC)
     *
     * @param name The name of the view to change the pattern of.
     * @param newPatternData The new pattern for the blocks.
     */
    abstract fun changePattern(name: String, newPatternData: PatternData)

    /**
     * Adds a set of blocks to a specific view, updating the viewers (ASYNC)
     *
     * @param name The name of the view to add blocks to.
     * @param positions The set of positions to be added.
     */
    abstract fun addBlocks(name: String, positions: Set<SimplePosition>)

    /**
     * Sets blocks in a specified view based on the provided positions, pattern, and update flag. (ASYNC)
     *
     * @param name The name of the view.
     * @param positions The list of positions where blocks should be set.
     * @param pattern The pattern data used to determine the block data.
     */
    abstract fun setBlocks(name: String, positions: List<SimplePosition>, pattern: PatternData)

    /**
     * Sets a block in a specified view at the given position based on the provided pattern and update flag.
     *
     * @param name The name of the view.
     * @param position The position where the block should be set.
     * @param pattern The pattern data used to determine the block data.
     * @param update Whether to update the view immediately.
     */
    abstract fun setBlock(name: String, position: SimplePosition, pattern: PatternData, update: Boolean)

    /**
     * Sets a block in a specified view at the given position based on the provided block data and update flag.
     *
     * @param name The name of the view.
     * @param position The position where the block should be set.
     * @param blockData The block data used for the block.
     * @param update Whether to update the view immediately.
     */
    abstract fun setBlock(name: String, position: SimplePosition, blockData: GhostBlockData, update: Boolean)

    /**
     * Removes a set of blocks from a specific view, updating the viewers (ASYNC)
     *
     * @param name The name of the view to remove blocks from.
     * @param blocks The set of positions of the blocks to be removed.
     */
    abstract fun removeBlocks(name: String, blocks: Set<SimplePosition>)

    // **** Player Management Functions ****

    /**
     * Retrieves the list of players who are viewing the stage.
     *
     * @return The list of players viewing the stage.
     */
    abstract fun getViewers(): List<Player>

    /**
     * Adds a player to the audience of the stage, updating the player's view (ASYNC)
     *
     * @param player The player to be added to the audience.
     */
    abstract fun addPlayer(player: Player)

    /**
     * Removes a player from the audience of the stage, updating the player's view (ASYNC)
     *
     * @param player The player to be removed from the audience.
     */
    abstract fun removePlayer(player: Player)

    // **** Utility Functions ****

    /**
     * Gets the highest altitude from an x-coord and z-coord
     *
     * @param name The name of the view.
     * @param x The x-coordinate.
     * @param z The z-coordinate
     * @return The highest block position at that location, or `null` if no blocks exist.
     */
    abstract fun getHighestPosition(name: String, x: Int, z: Int): SimplePosition?

    /**
     * Updates all blocks within a specific chunk of a view. (CALL ASYNC)
     *
     * @param name The name of the view to update
     * @param chunk The chunk key of the chunk to be updated.
     */
    abstract fun updateChunkView(name: String, chunk: SimplePosition)
}