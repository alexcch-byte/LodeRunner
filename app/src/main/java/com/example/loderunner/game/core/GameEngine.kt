package com.example.loderunner.game.core

import com.example.loderunner.game.audio.SoundFxEngine
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.model.DugHole
import com.example.loderunner.game.model.Enemy
import com.example.loderunner.game.model.EntityState
import com.example.loderunner.game.model.GameState
import com.example.loderunner.game.model.GameStatus
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.Runner
import com.example.loderunner.game.model.TileType
import kotlin.math.abs
import kotlin.math.roundToInt

class GameEngine(
    val soundFx: SoundFxEngine = SoundFxEngine(),
    var onLevelComplete: (() -> Unit)? = null,
    var onGameOver: (() -> Unit)? = null
) {
    var state: GameState = GameState()
        private set

    companion object {
        const val RUNNER_SPEED = 0.12f
        const val ENEMY_SPEED = 0.075f
        const val FALL_SPEED = 0.16f
        const val CLIMB_SPEED = 0.09f
        const val HOLE_TOTAL_TICKS = 360 // ~6 seconds at 60 FPS
        const val ENEMY_TRAPPED_TICKS = 180 // ~3 seconds in hole before climbing out
        const val ENEMY_RESPAWN_DELAY = 120 // ~2 seconds after crushed
        const val DIG_ANIM_TICKS = 15
    }

    private var inputDirection: Direction = Direction.NONE
    private var pendingDig: Direction = Direction.NONE
    private var runnerSpawnX: Int = 0
    private var runnerSpawnY: Int = 0

    fun setInputDirection(direction: Direction) {
        inputDirection = direction
    }

    fun triggerDig(direction: Direction) {
        if (state.status == GameStatus.PLAYING && (direction == Direction.LEFT || direction == Direction.RIGHT)) {
            pendingDig = direction
        }
    }

    fun startLevel(level: LevelData, preserveScoreAndLives: Boolean = false) {
        val score = if (preserveScoreAndLives) state.score else 0
        val lives = if (preserveScoreAndLives) state.lives else 5

        var goldCount = 0
        val gridCopy = Array(LevelData.ROWS) { r ->
            IntArray(LevelData.COLS) { c ->
                val tile = level.grid[r][c]
                if (tile == TileType.GOLD.id) {
                    goldCount++
                }
                tile
            }
        }

        val enemies = level.enemySpawns.mapIndexed { index, spawn ->
            Enemy(
                id = index + 1,
                x = spawn.first.toFloat(),
                y = spawn.second.toFloat(),
                spawnX = spawn.first.toFloat(),
                spawnY = spawn.second.toFloat()
            )
        }

        runnerSpawnX = level.runnerStartX
        runnerSpawnY = level.runnerStartY
        val runner = Runner(
            x = level.runnerStartX.toFloat(),
            y = level.runnerStartY.toFloat()
        )

        state = GameState(
            levelNumber = level.id,
            score = score,
            lives = lives,
            goldRemaining = goldCount,
            goldTotal = goldCount,
            escapeLaddersRevealed = false,
            status = GameStatus.PLAYING,
            runner = runner,
            enemies = enemies,
            dugHoles = mutableListOf(),
            activeGrid = gridCopy,
            tickCount = 0L,
            statusTimer = 0
        )
    }

    fun restartCurrentLevel() {
        if (state.lives > 0) {
            state.lives--
            if (state.lives <= 0) {
                state.status = GameStatus.GAME_OVER
                soundFx.playDeath()
                onGameOver?.invoke()
            } else {
                state.status = GameStatus.PLAYING
                resetEntitiesToSpawn()
            }
        }
    }

    private fun resetEntitiesToSpawn() {
        // Respawn at the level's start position, not where the runner died
        state.runner.reset(
            startX = runnerSpawnX,
            startY = runnerSpawnY
        )
        for (e in state.enemies) {
            e.reset()
        }
        state.dugHoles.clear()
        inputDirection = Direction.NONE
        pendingDig = Direction.NONE
    }

    fun togglePause() {
        if (state.status == GameStatus.PLAYING) {
            state.status = GameStatus.PAUSED
        } else if (state.status == GameStatus.PAUSED) {
            state.status = GameStatus.PLAYING
        }
    }

    fun tick() {
        if (state.status != GameStatus.PLAYING) {
            handleNonPlayingTicks()
            return
        }

        state.tickCount++

        // 1. Process Digging Requests
        processDigInput()

        // 2. Update Dug Holes
        updateDugHoles()

        // 3. Update Runner Physics & Movement
        updateRunner()

        // 4. Update Bungeling Enemy AI & Physics
        updateEnemies()

        // 5. Collision Checks between Runner and Enemies
        checkRunnerEnemyCollisions()

        // 6. Check Win Condition
        checkLevelCompletion()
    }

    private fun handleNonPlayingTicks() {
        if (state.status == GameStatus.RUNNER_DIED) {
            state.statusTimer++
            if (state.statusTimer > 90) { // ~1.5s delay
                state.statusTimer = 0
                state.lives--
                if (state.lives <= 0) {
                    state.status = GameStatus.GAME_OVER
                    onGameOver?.invoke()
                } else {
                    state.status = GameStatus.PLAYING
                    resetEntitiesToSpawn()
                }
            }
        } else if (state.status == GameStatus.LEVEL_CLEARED) {
            state.statusTimer++
            if (state.statusTimer > 120) { // ~2s victory pause
                state.statusTimer = 0
                onLevelComplete?.invoke()
            }
        }
    }

    private fun processDigInput() {
        if (pendingDig == Direction.NONE) return
        val runner = state.runner
        val digDir = pendingDig
        pendingDig = Direction.NONE

        if (runner.digTimer > 0 || runner.state == EntityState.FALLING) return

        val gx = runner.gridX
        val gy = runner.gridY

        // Runner must be well-aligned vertically and supported
        if (abs(runner.y - gy) > 0.3f) return

        val targetX = if (digDir == Direction.LEFT) gx - 1 else gx + 1
        val targetY = gy + 1
        val overheadY = gy

        // Bounds check
        if (targetX !in 0 until LevelData.COLS || targetY !in 0 until LevelData.ROWS) return

        // Dig condition: target must be BRICK, overhead must be empty/rope/ladder
        val targetTile = state.activeGrid[targetY][targetX]
        val overheadTile = state.activeGrid[overheadY][targetX]

        val isOverheadClear = overheadTile == TileType.EMPTY.id ||
                overheadTile == TileType.ROPE.id ||
                overheadTile == TileType.LADDER.id ||
                (overheadTile == TileType.ESCAPE_LADDER.id && state.escapeLaddersRevealed)

        val alreadyDug = state.dugHoles.any { it.x == targetX && it.y == targetY }

        if (targetTile == TileType.BRICK.id && isOverheadClear && !alreadyDug) {
            // Dig success!
            state.activeGrid[targetY][targetX] = TileType.EMPTY.id
            state.dugHoles.add(DugHole(x = targetX, y = targetY, remainingTicks = HOLE_TOTAL_TICKS))

            runner.digTimer = DIG_ANIM_TICKS
            runner.digDirection = digDir
            runner.state = EntityState.DIGGING
            soundFx.playDig()
        }
    }

    private fun updateDugHoles() {
        val iterator = state.dugHoles.iterator()
        while (iterator.hasNext()) {
            val hole = iterator.next()
            hole.remainingTicks--

            if (hole.remainingTicks <= 0) {
                // Hole regenerates to solid brick!
                state.activeGrid[hole.y][hole.x] = TileType.BRICK.id
                iterator.remove()

                // Check if runner crushed
                val rx = state.runner.gridX
                val ry = state.runner.gridY
                if (rx == hole.x && ry == hole.y) {
                    soundFx.playCrush()
                    state.status = GameStatus.RUNNER_DIED
                    soundFx.playDeath()
                }

                // Check if any enemy crushed
                for (enemy in state.enemies) {
                    if (enemy.gridX == hole.x && enemy.gridY == hole.y) {
                        // Enemy crushed!
                        enemy.trappedTicks = 0
                        enemy.respawnTicks = ENEMY_RESPAWN_DELAY
                        enemy.state = EntityState.DEAD
                        state.score += 750 // 750 points for crushing guard!
                        soundFx.playCrush()
                    }
                }
            }
        }
    }

    private fun updateRunner() {
        val runner = state.runner

        if (runner.digTimer > 0) {
            runner.digTimer--
            if (runner.digTimer <= 0) {
                runner.state = EntityState.IDLE
            }
            return
        }

        val rx = runner.x
        val ry = runner.y
        val gx = runner.gridX
        val gy = runner.gridY

        val currentTile = getTile(gx, gy)
        val groundTile = getTile(gx, gy + 1)
        val isOnLadder = currentTile == TileType.LADDER || (currentTile == TileType.ESCAPE_LADDER && state.escapeLaddersRevealed)
        val isAtLadderTop = groundTile == TileType.LADDER || (groundTile == TileType.ESCAPE_LADDER && state.escapeLaddersRevealed)
        val isOnRope = currentTile == TileType.ROPE

        // Check if supported by ground or trapped enemy below
        val isEnemyBelow = isTrappedEnemyAt(gx, gy + 1)
        val hasSolidGround = isSolid(groundTile) || (isAtLadderTop && abs(ry - gy) < 0.2f) || isEnemyBelow

        // 1. Gravity / Falling
        if (!hasSolidGround && !isOnLadder && !isOnRope) {
            runner.state = EntityState.FALLING
            runner.y += FALL_SPEED
            // Snap X to column center while falling
            runner.x = alignToCenter(runner.x, gx)

            // Landing check
            val newGy = runner.gridY
            val newGround = getTile(gx, newGy + 1)
            val newOnLadder = getTile(gx, newGy) == TileType.LADDER
            val newOnRope = getTile(gx, newGy) == TileType.ROPE
            val newEnemyBelow = isTrappedEnemyAt(gx, newGy + 1)

            if (isSolid(newGround) || newOnLadder || newOnRope || newEnemyBelow) {
                runner.y = newGy.toFloat()
                runner.state = EntityState.IDLE
            }
            return
        }

        // 2. User Input Movement
        when (inputDirection) {
            Direction.LEFT -> {
                runner.facing = Direction.LEFT
                val targetCol = gx - 1
                if (canMoveHorizontal(targetCol, gy)) {
                    runner.x -= RUNNER_SPEED
                    runner.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                    runner.y = gy.toFloat() // Keep snapped to horizontal track
                    runner.animFrame = ((state.tickCount / 4) % 4).toInt()
                } else {
                    // Blocked by wall
                    runner.x = gx.toFloat()
                    runner.state = EntityState.IDLE
                }
            }
            Direction.RIGHT -> {
                runner.facing = Direction.RIGHT
                val targetCol = gx + 1
                if (canMoveHorizontal(targetCol, gy)) {
                    runner.x += RUNNER_SPEED
                    runner.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                    runner.y = gy.toFloat()
                    runner.animFrame = ((state.tickCount / 4) % 4).toInt()
                } else {
                    runner.x = gx.toFloat()
                    runner.state = EntityState.IDLE
                }
            }
            Direction.UP -> {
                if (isOnLadder || (isAtLadderTop && abs(rx - gx) < 0.3f)) {
                    val targetRow = gy - 1
                    if (targetRow >= 0 && !isSolid(getTile(gx, targetRow))) {
                        runner.y -= CLIMB_SPEED
                        runner.x = gx.toFloat()
                        runner.state = EntityState.CLIMBING
                        runner.animFrame = ((state.tickCount / 4) % 2).toInt()
                    }
                }
            }
            Direction.DOWN -> {
                if (isOnLadder || isAtLadderTop) {
                    val targetRow = gy + 1
                    if (targetRow < LevelData.ROWS && !isSolid(getTile(gx, targetRow))) {
                        runner.y += CLIMB_SPEED
                        runner.x = gx.toFloat()
                        runner.state = EntityState.CLIMBING
                        runner.animFrame = ((state.tickCount / 4) % 2).toInt()
                    }
                } else if (isOnRope) {
                    // Drop down from rope
                    runner.y += FALL_SPEED
                    runner.state = EntityState.FALLING
                    soundFx.playFall()
                }
            }
            Direction.NONE -> {
                if (runner.state != EntityState.FALLING) {
                    runner.state = EntityState.IDLE
                }
            }
        }

        // Clamp runner within grid
        runner.x = runner.x.coerceIn(0f, (LevelData.COLS - 1).toFloat())
        runner.y = runner.y.coerceIn(0f, (LevelData.ROWS - 1).toFloat())

        // 3. Gold Pickup Check
        val currentTileAtRunner = getTile(runner.gridX, runner.gridY)
        if (currentTileAtRunner == TileType.GOLD) {
            state.activeGrid[runner.gridY][runner.gridX] = TileType.EMPTY.id
            state.goldRemaining = (state.goldRemaining - 1).coerceAtLeast(0)
            state.score += 250
            soundFx.playGold()

            if (state.goldRemaining == 0 && !state.escapeLaddersRevealed) {
                state.escapeLaddersRevealed = true
                soundFx.playVictory()
            }
        }
    }

    private fun updateEnemies() {
        val runner = state.runner
        for (enemy in state.enemies) {
            // Handle dead/respawning enemies
            if (enemy.isDead) {
                enemy.respawnTicks--
                if (enemy.respawnTicks <= 0) {
                    enemy.reset()
                }
                continue
            }

            val ex = enemy.x
            val ey = enemy.y
            val egx = enemy.gridX
            val egy = enemy.gridY

            // 1. Hole Trapping Check
            val isCurrentlyInHole = state.dugHoles.any { it.x == egx && it.y == egy }
            if (isCurrentlyInHole && !enemy.isTrapped) {
                // Enemy falls into dug hole!
                enemy.trappedTicks = ENEMY_TRAPPED_TICKS
                enemy.state = EntityState.TRAPPED
                enemy.x = egx.toFloat()
                enemy.y = egy.toFloat()
                soundFx.playTrap()

                // If carrying gold, drop it overhead
                if (enemy.hasGold) {
                    enemy.hasGold = false
                    val dropY = (egy - 1).coerceAtLeast(0)
                    if (state.activeGrid[dropY][egx] == TileType.EMPTY.id) {
                        state.activeGrid[dropY][egx] = TileType.GOLD.id
                    }
                }
                continue
            }

            if (enemy.isTrapped) {
                enemy.trappedTicks--
                if (enemy.trappedTicks <= 0) {
                    // Climb out of hole upward
                    val aboveY = (egy - 1).coerceAtLeast(0)
                    enemy.y = aboveY.toFloat()
                    enemy.state = EntityState.IDLE
                }
                continue
            }

            val curTile = getTile(egx, egy)
            val groundTile = getTile(egx, egy + 1)
            val isOnLadder = curTile == TileType.LADDER
            val isAtLadderTop = groundTile == TileType.LADDER
            val isOnRope = curTile == TileType.ROPE
            val hasSolidGround = isSolid(groundTile) || isAtLadderTop

            // 2. Enemy Gravity / Falling
            if (!hasSolidGround && !isOnLadder && !isOnRope) {
                enemy.state = EntityState.FALLING
                enemy.y += FALL_SPEED
                enemy.x = alignToCenter(enemy.x, egx)
                val newEgy = enemy.gridY
                val newGround = getTile(egx, newEgy + 1)
                if (isSolid(newGround) || getTile(egx, newEgy) == TileType.LADDER || getTile(egx, newEgy) == TileType.ROPE) {
                    enemy.y = newEgy.toFloat()
                    enemy.state = EntityState.IDLE
                }
                continue
            }

            // 3. AI Navigation towards Runner
            val diffX = runner.x - ex
            val diffY = runner.y - ey

            // If enemy is on ladder and runner is on another vertical level, prioritize vertical movement
            if (isOnLadder && abs(diffY) > 0.5f) {
                val moveUp = diffY < 0
                val targetRow = if (moveUp) egy - 1 else egy + 1
                if (targetRow in 0 until LevelData.ROWS && !isSolid(getTile(egx, targetRow))) {
                    enemy.y += if (moveUp) -CLIMB_SPEED else CLIMB_SPEED
                    enemy.x = egx.toFloat()
                    enemy.state = EntityState.CLIMBING
                    enemy.animFrame = ((state.tickCount / 6) % 2).toInt()
                    continue
                }
            }

            // If at ladder top and player is below, climb down
            if (isAtLadderTop && diffY > 0.8f && abs(diffX) < 0.5f) {
                enemy.y += CLIMB_SPEED
                enemy.x = egx.toFloat()
                enemy.state = EntityState.CLIMBING
                continue
            }

            // Otherwise move horizontally towards runner
            if (abs(diffX) > 0.2f) {
                val moveRight = diffX > 0
                val targetCol = if (moveRight) egx + 1 else egx - 1
                enemy.facing = if (moveRight) Direction.RIGHT else Direction.LEFT

                if (canMoveHorizontal(targetCol, egy)) {
                    enemy.x += if (moveRight) ENEMY_SPEED else -ENEMY_SPEED
                    enemy.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                    enemy.y = egy.toFloat()
                    enemy.animFrame = ((state.tickCount / 5) % 4).toInt()
                } else {
                    // Blocked, check if there's a ladder to take
                    if (isOnLadder) {
                        val moveUp = diffY < 0
                        enemy.y += if (moveUp) -CLIMB_SPEED else CLIMB_SPEED
                        enemy.state = EntityState.CLIMBING
                    }
                }
            } else {
                enemy.state = EntityState.IDLE
            }
        }
    }

    private fun checkRunnerEnemyCollisions() {
        val runner = state.runner
        val rx = runner.x
        val ry = runner.y

        for (enemy in state.enemies) {
            if (enemy.isDead) continue

            // If enemy is trapped in a hole and runner is above (walking over head)
            if (enemy.isTrapped) {
                // Runner walking on top of trapped enemy is safe!
                if (ry < enemy.y - 0.4f) {
                    continue
                }
            }

            val dx = abs(rx - enemy.x)
            val dy = abs(ry - enemy.y)

            // Hitbox threshold
            if (dx < 0.65f && dy < 0.65f) {
                // Runner caught!
                state.status = GameStatus.RUNNER_DIED
                runner.state = EntityState.DEAD
                soundFx.playDeath()
                break
            }
        }
    }

    private fun checkLevelCompletion() {
        if (!state.escapeLaddersRevealed) return
        val runner = state.runner
        val gx = runner.gridX
        val gy = runner.gridY

        val tile = getTile(gx, gy)
        // If runner climbed to the top row (row 0) on an escape ladder
        if (tile == TileType.ESCAPE_LADDER && gy <= 0 && runner.y <= 0.3f) {
            state.status = GameStatus.LEVEL_CLEARED
            runner.state = EntityState.ESCAPED
            state.score += 1500 // Bonus for level clear
            soundFx.playVictory()
        }
    }

    // Helper functions for collision & tile queries
    private fun getTile(col: Int, row: Int): TileType {
        if (col !in 0 until LevelData.COLS || row !in 0 until LevelData.ROWS) {
            return TileType.SOLID_ROCK
        }
        return TileType.fromId(state.activeGrid[row][col])
    }

    private fun isSolid(tile: TileType): Boolean {
        return tile == TileType.BRICK || tile == TileType.SOLID_ROCK
    }

    private fun canMoveHorizontal(targetCol: Int, row: Int): Boolean {
        if (targetCol !in 0 until LevelData.COLS) return false
        val tile = getTile(targetCol, row)
        return !isSolid(tile)
    }

    private fun isTrappedEnemyAt(gx: Int, gy: Int): Boolean {
        return state.enemies.any { it.isTrapped && it.gridX == gx && it.gridY == gy }
    }

    private fun alignToCenter(current: Float, targetCell: Int): Float {
        val target = targetCell.toFloat()
        val diff = target - current
        return if (abs(diff) < 0.08f) target else current + diff * 0.2f
    }
}
