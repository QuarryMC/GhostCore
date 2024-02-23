package me.kooper.ghostcore.data

import org.bukkit.block.data.BlockData
import org.decimal4j.util.DoubleRounder

class PatternData(val blockDataPercentages: Map<BlockData, Double>) {
    init {
        require(blockDataPercentages.values.all { it in 0.0..1.0 }) {
            "Percentage values must be in the range [0.0, 1.0]"
        }
        require(blockDataPercentages.isNotEmpty()) {
            "Pattern must contain at least one BlockData with a non-zero percentage"
        }
        require(DoubleRounder.round(blockDataPercentages.values.sum(), 5) <= 1.0) {
            "Sum of percentages must not exceed 1.0"
        }
    }

    fun getRandomBlockData(): BlockData {
        val randomValue = Math.random()
        var cumulativeProbability = 0.0

        for ((blockData, percentage) in blockDataPercentages) {
            cumulativeProbability += percentage
            if (randomValue <= cumulativeProbability) {
                return blockData
            }
        }

        return blockDataPercentages.entries.last().key
    }
}
