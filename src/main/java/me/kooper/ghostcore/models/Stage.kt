package me.kooper.ghostcore.models

import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.kooper.ghostcore.data.AudienceData
import me.kooper.ghostcore.data.ChunkData
import me.kooper.ghostcore.data.ViewData
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.*


@Suppress("UnstableApiUsage")
class Stage(val uuid: UUID, val audience: AudienceData, val views: HashMap<String, ViewData>, val blocks: HashMap<Position, BlockData>, val chunks: Long2ObjectOpenHashMap<ChunkData>) {

    constructor(uuid: UUID, audience: AudienceData) : this(uuid, audience, HashMap(), HashMap(), Long2ObjectOpenHashMap())

    fun deleteView(name: String) {
        hideView(name)
        views.remove(name.lowercase())
    }

    fun getViewers() : ArrayList<Player> {
        val players: ArrayList<Player> = ArrayList()
        for (viewer in audience.viewers) {
            val player: Player? = Bukkit.getPlayer(viewer)
            if (player == null) {
                audience.viewers.remove(viewer)
                continue
            }
            players.add(player)
        }
        return players
    }

    fun showView(name: String) {
        for (viewer in getViewers()) {
            viewer.sendMultiBlockChange(getSolidBlocks(name))
        }
    }

    fun hideView(name: String) {
        for (viewer in getViewers()) {
            val blocks: HashMap<Position, BlockData> = getBlocks(name)
            blocks.keys.forEach{ pos -> blocks[pos] = Material.AIR.createBlockData() }
            viewer.sendMultiBlockChange(blocks)
        }
    }

    fun setAirBlocks(name: String, positions: Set<Position>) {
        positions.forEach { pos -> setAirBlock(name, pos) }
    }

    fun updateChunkView(name: String, chunk: Long) {
        for (viewer in getViewers()) {
            viewer.sendMultiBlockChange(chunks[chunk].blocks)
        }
    }

    fun setAirBlock(name: String, position: Position) {
        val view: ViewData = views[name.lowercase()]!!
        view.blocks[position] = Material.AIR.createBlockData()
        chunks[getChunkFromPos(position)].blocks[position] = Material.AIR.createBlockData()
    }

    fun resetAirBlocks(name: String) {
        val view: ViewData = views[name.lowercase()]!!
        view.blocks.keys.forEach{ pos -> view.blocks[pos] = view.material.createBlockData() }
        chunks.values.forEach{ chunk -> chunk.blocks.keys.forEach{ block -> chunk.blocks[block] = chunk.type.createBlockData() } }
    }

    fun getSolidBlocks(name: String) : Map<Position, BlockData> {
        val blocks = getBlocks(name)
        val iterator = blocks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.material == Material.AIR) {
                iterator.remove()
            }
        }
        return blocks
    }

    fun getBlocks(name: String) : HashMap<Position, BlockData> {
        val view: ViewData = views[name.lowercase()]!!
        return view.blocks
    }

    fun changeType(name: String, newType: Material, isSolid: Boolean) : Map<Position, BlockData> {
        views[name.lowercase()]!!.material = newType
        return if (isSolid) getSolidBlocks(name) else getBlocks(name)
    }

    fun addBlocks(name: String, blocks: Set<Block>) {
        val view: ViewData = views[name.lowercase()]!!
        val chunkBlocks: MutableMap<Long, HashMap<Position, BlockData>> = HashMap()
        blocks.forEach { block ->
            chunkBlocks.computeIfAbsent(block.chunk.chunkKey) { HashMap() }
            chunkBlocks[block.chunk.chunkKey]!![Position.block(block.location)] = view.material.createBlockData()
            view.blocks[Position.block(block.location)] = view.material.createBlockData()
            this.blocks[Position.block(block.location)] = view.material.createBlockData()
        }
        for (chunk in chunkBlocks.keys) {
            val chunkData = ChunkData(chunkBlocks[chunk]!!, view.material)
            chunks.put(chunk, chunkData)
        }
        showView(name.lowercase())
    }

    fun removeBlocks(name: String, blocks: Set<Position>) {
        val view: ViewData = views[name.lowercase()]!!
        view.blocks.keys.removeAll(blocks)
        showView(name.lowercase())
    }

    fun createView(name: String, blocks: HashSet<Block>, type: Material) : ViewData? {
        if (views.containsKey(name.lowercase())) {
            Throwable("View with $name already exists for stage $uuid!")
            return null
        }
        val view = ViewData(HashMap(), type)
        views[name.lowercase()] = view
        addBlocks(name.lowercase(), blocks)
        return view
    }

    fun addPlayer(player: Player) {
        audience.viewers.add(player.uniqueId)
        for (view in views.keys) {
            player.sendMultiBlockChange(getSolidBlocks(view))
        }
    }

    fun removePlayer(player: Player) {
        audience.viewers.remove(player.uniqueId)
        for (view in views.keys) {
            player.sendMultiBlockChange(changeType(view, Material.AIR, true))
        }
    }

    fun getChunkFromPos(position: Position) : Long {
        return Chunk.getChunkKey(position.blockX() shr 4, position.blockZ() shr 4)
    }

    fun positionToLong(position: Position): Long {
        val x = position.x().toLong()
        val y = position.y().toLong()
        val z = position.z().toLong()

        return (x shl 40) or ((y and 0xFFFFFL) shl 20) or (z and 0xFFFFFL)
    }

    fun longToPosition(key: Long): Position {
        val x = (key shr 40).toInt()
        val y = ((key shr 20) and 0xFFFFFL).toInt()
        val z = (key and 0xFFFFFL).toInt()

        return Position.block(x, y, z)
    }

}