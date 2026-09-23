package com.example.loderunner.game.model

/**
 * Data representation of a Lode Runner level (28 columns x 16 rows).
 */
data class LevelData(
    val id: Int,
    val name: String,
    val grid: Array<IntArray>,
    val runnerStartX: Int,
    val runnerStartY: Int,
    val enemySpawns: List<Pair<Int, Int>>
) {
    companion object {
        const val COLS = 28
        const val ROWS = 16

        /**
         * Parses a 16-line ASCII string representation into LevelData.
         */
        fun fromAscii(id: Int, name: String, ascii: String): LevelData {
            val lines = ascii.trimIndent().lines().filter { it.isNotBlank() }
            val grid = Array(ROWS) { IntArray(COLS) { TileType.EMPTY.id } }
            var runnerX = 14
            var runnerY = 14
            val enemySpawns = mutableListOf<Pair<Int, Int>>()

            for (r in 0 until minOf(ROWS, lines.size)) {
                val line = lines[r]
                for (c in 0 until minOf(COLS, line.length)) {
                    val ch = line[c]
                    when (ch) {
                        '&' -> {
                            runnerX = c
                            runnerY = r
                            grid[r][c] = TileType.EMPTY.id
                        }
                        'M' -> {
                            enemySpawns.add(Pair(c, r))
                            grid[r][c] = TileType.EMPTY.id
                        }
                        else -> {
                            grid[r][c] = TileType.fromChar(ch).id
                        }
                    }
                }
            }

            return LevelData(
                id = id,
                name = name,
                grid = grid,
                runnerStartX = runnerX,
                runnerStartY = runnerY,
                enemySpawns = enemySpawns
            )
        }
    }

    fun clone(): LevelData {
        val newGrid = Array(ROWS) { r -> grid[r].copyOf() }
        return LevelData(
            id = id,
            name = name,
            grid = newGrid,
            runnerStartX = runnerStartX,
            runnerStartY = runnerStartY,
            enemySpawns = enemySpawns.toList()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelData
        if (id != other.id) return false
        if (name != other.name) return false
        if (!grid.contentDeepEquals(other.grid)) return false
        if (runnerStartX != other.runnerStartX) return false
        if (runnerStartY != other.runnerStartY) return false
        if (enemySpawns != other.enemySpawns) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + grid.contentDeepHashCode()
        result = 31 * result + runnerStartX
        result = 31 * result + runnerStartY
        result = 31 * result + enemySpawns.hashCode()
        return result
    }
}
