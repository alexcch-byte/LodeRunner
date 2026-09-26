package com.example.loderunner

import com.example.loderunner.game.levels.ClassicLevels
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.TileType
import org.junit.Test
import java.util.ArrayDeque

class LevelReachabilityTest {

    private val report = StringBuilder()

    @Test
    fun analyzeAllClassicLevels() {
        report.appendLine("==================================================")
        report.appendLine("STARTING FULL LEVEL REACHABILITY & BRICK DIGGING ANALYSIS")
        report.appendLine("==================================================")

        for ((index, level) in ClassicLevels.ALL_LEVELS.withIndex()) {
            report.appendLine("\n--- ANALYZING LEVEL ${index + 1}: ${level.name} ---")
            analyzeLevel(level)
        }

        val reportFile = java.io.File("build/reachability_report.txt")
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report.toString())
        println(report.toString())
    }

    private fun analyzeLevel(level: LevelData) {
        val rows = LevelData.ROWS
        val cols = LevelData.COLS

        val goldLocations = mutableListOf<Pair<Int, Int>>()
        val brickLocations = mutableListOf<Pair<Int, Int>>()

        for (y in 0 until rows) {
            for (x in 0 until cols) {
                when (level.grid[y][x]) {
                    TileType.GOLD.id -> goldLocations.add(Pair(x, y))
                    TileType.BRICK.id, TileType.FALSE_BRICK.id -> brickLocations.add(Pair(x, y))
                }
            }
        }

        report.appendLine("Level Dimensions: ${cols}x${rows}")
        report.appendLine("Runner Start: (${level.runnerStartX}, ${level.runnerStartY})")
        report.appendLine("Total Gold: ${goldLocations.size}")
        report.appendLine("Total Bricks: ${brickLocations.size}")

        report.appendLine("Grid Dump:")
        for (r in 0 until rows) {
            val sb = StringBuilder()
            for (c in 0 until cols) {
                val t = level.grid[r][c]
                val ch = TileType.fromId(t).charSymbol
                sb.append(ch)
            }
            report.appendLine(String.format("%2d: %s", r, sb.toString()))
        }

        // We run a multi-pass reachable + diggable simulation:
        // When a brick is reachable and diggable, we allow it to be dug to reveal deeper areas.
        val currentGrid = Array(rows) { r -> level.grid[r].copyOf() }
        val visited = Array(rows) { BooleanArray(cols) }
        val dugBricks = mutableSetOf<Pair<Int, Int>>()

        fun isSolid(x: Int, y: Int): Boolean {
            if (x !in 0 until cols || y !in 0 until rows) return true
            val t = currentGrid[y][x]
            return t == TileType.BRICK.id || t == TileType.SOLID_ROCK.id
        }

        fun isLadder(x: Int, y: Int): Boolean {
            if (x !in 0 until cols || y !in 0 until rows) return false
            return currentGrid[y][x] == TileType.LADDER.id
        }

        fun isRope(x: Int, y: Int): Boolean {
            if (x !in 0 until cols || y !in 0 until rows) return false
            return currentGrid[y][x] == TileType.ROPE.id
        }

        var newAction = true
        while (newAction) {
            newAction = false

            // Standard BFS exploration
            val queue = ArrayDeque<Pair<Int, Int>>()
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if (visited[y][x]) {
                        queue.add(Pair(x, y))
                    }
                }
            }
            if (queue.isEmpty()) {
                val start = Pair(level.runnerStartX, level.runnerStartY)
                visited[start.second][start.first] = true
                queue.add(start)
            }

            while (queue.isNotEmpty()) {
                val (x, y) = queue.poll()!!

                val onLadder = isLadder(x, y)
                val atLadderTop = isLadder(x, y + 1)
                val onRope = isRope(x, y)
                val groundSolid = isSolid(x, y + 1)
                val isSupported = groundSolid || onLadder || atLadderTop || onRope

                if (!isSupported) {
                    val nextY = y + 1
                    if (nextY < rows && !isSolid(x, nextY)) {
                        if (!visited[nextY][x]) {
                            visited[nextY][x] = true
                            queue.add(Pair(x, nextY))
                            newAction = true
                        }
                    }
                    continue
                }

                // Move Left
                val leftX = x - 1
                if (leftX >= 0 && !isSolid(leftX, y)) {
                    if (!visited[y][leftX]) {
                        visited[y][leftX] = true
                        queue.add(Pair(leftX, y))
                        newAction = true
                    }
                }

                // Move Right
                val rightX = x + 1
                if (rightX < cols && !isSolid(rightX, y)) {
                    if (!visited[y][rightX]) {
                        visited[y][rightX] = true
                        queue.add(Pair(rightX, y))
                        newAction = true
                    }
                }

                // Climb Up
                if (onLadder || atLadderTop) {
                    val upY = y - 1
                    if (upY >= 0 && !isSolid(x, upY)) {
                        if (!visited[upY][x]) {
                            visited[upY][x] = true
                            queue.add(Pair(x, upY))
                            newAction = true
                        }
                    }
                }

                // Climb / Drop Down
                if (onLadder || atLadderTop || onRope) {
                    val downY = y + 1
                    if (downY < rows && !isSolid(x, downY)) {
                        if (!visited[downY][x]) {
                            visited[downY][x] = true
                            queue.add(Pair(x, downY))
                            newAction = true
                        }
                    }
                }
            }

            // Check if any visited position can dig an adjacent brick
            for (brick in brickLocations) {
                if (dugBricks.contains(brick)) continue
                val (bx, by) = brick

                // Dig from right: standing at (bx + 1, by - 1)
                val digFromRight = by > 0 && bx < cols - 1 && visited[by - 1][bx + 1] && !isSolid(bx, by - 1)
                // Dig from left: standing at (bx - 1, by - 1)
                val digFromLeft = by > 0 && bx > 0 && visited[by - 1][bx - 1] && !isSolid(bx, by - 1)

                if (digFromRight || digFromLeft) {
                    dugBricks.add(brick)
                    currentGrid[by][bx] = TileType.EMPTY.id
                    newAction = true
                }
            }
        }

        report.appendLine("Visited Grid:")
        for (r in 0 until rows) {
            val sb = StringBuilder()
            for (c in 0 until cols) {
                sb.append(if (visited[r][c]) '.' else 'X')
            }
            report.appendLine(String.format("%2d: %s", r, sb.toString()))
        }

        val unreachableGold = goldLocations.filter { !visited[it.second][it.first] }
        report.appendLine("Accessible Gold: ${goldLocations.size - unreachableGold.size} / ${goldLocations.size}")
        if (unreachableGold.isNotEmpty()) {
            report.appendLine("WARNING: Unreachable Gold at: $unreachableGold")
        }

        val accessibleBricks = mutableListOf<Pair<Int, Int>>()
        val unreachableBricks = mutableListOf<Pair<Int, Int>>()

        for (brick in brickLocations) {
            val bx = brick.first
            val by = brick.second

            val standOnTop = by > 0 && visited[by - 1][bx]
            val standLeft = bx > 0 && visited[by][bx - 1]
            val standRight = bx < cols - 1 && visited[by][bx + 1]
            val canDig = dugBricks.contains(brick)

            if (standOnTop || standLeft || standRight || canDig) {
                accessibleBricks.add(brick)
            } else {
                unreachableBricks.add(brick)
            }
        }

        report.appendLine("Accessible Bricks: ${accessibleBricks.size} / ${brickLocations.size}")
        if (unreachableBricks.isNotEmpty()) {
            report.appendLine("ALERT: ${unreachableBricks.size} UNREACHABLE BRICKS FOUND!")
            for (ub in unreachableBricks) {
                report.appendLine("   Unreachable Brick at (${ub.first}, ${ub.second})")
            }
        } else {
            report.appendLine("SUCCESS: ALL BRICKS ARE ACCESSIBLE!")
        }
    }
}
