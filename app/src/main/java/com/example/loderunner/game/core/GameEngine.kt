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

import com.example.loderunner.game.model.GameSpeed

class GameEngine(
    val soundFx: SoundFxEngine = SoundFxEngine(),
    var onLevelComplete: (() -> Unit)? = null,
    var onGameOver: (() -> Unit)? = null
) {
    var state: GameState = GameState()
        private set

    var gameSpeed: GameSpeed = GameSpeed.NORMAL

    companion object {
        const val BASE_RUNNER_SPEED = 0.075f
        const val BASE_ENEMY_SPEED = 0.048f
        const val BASE_FALL_SPEED = 0.110f
        const val BASE_CLIMB_SPEED = 0.055f
        const val BASE_HOLE_TOTAL_TICKS = 360 // ~6 seconds at 60 FPS
        const val BASE_ENEMY_TRAPPED_TICKS = 180 // ~3 seconds in hole before climbing out
        const val BASE_ENEMY_RESPAWN_DELAY = 120 // ~2 seconds after crushed
        const val BASE_DIG_ANIM_TICKS = 18

        const val HOLE_TOTAL_TICKS = BASE_HOLE_TOTAL_TICKS
        const val ENEMY_TRAPPED_TICKS = BASE_ENEMY_TRAPPED_TICKS
        const val ENEMY_RESPAWN_DELAY = BASE_ENEMY_RESPAWN_DELAY
        const val DIG_ANIM_TICKS = BASE_DIG_ANIM_TICKS
        const val RUNNER_SPEED = BASE_RUNNER_SPEED
        const val ENEMY_SPEED = BASE_ENEMY_SPEED
        const val FALL_SPEED = BASE_FALL_SPEED
        const val CLIMB_SPEED = BASE_CLIMB_SPEED
    }

    val runnerSpeed: Float get() = BASE_RUNNER_SPEED * gameSpeed.multiplier
    val enemySpeed: Float get() = BASE_ENEMY_SPEED * gameSpeed.multiplier
    val fallSpeed: Float get() = BASE_FALL_SPEED * gameSpeed.multiplier
    val climbSpeed: Float get() = BASE_CLIMB_SPEED * gameSpeed.multiplier
    val holeTotalTicks: Int get() = (BASE_HOLE_TOTAL_TICKS / gameSpeed.multiplier).toInt()
    val enemyTrappedTicks: Int get() = (BASE_ENEMY_TRAPPED_TICKS / gameSpeed.multiplier).toInt()
    val enemyRespawnDelay: Int get() = (BASE_ENEMY_RESPAWN_DELAY / gameSpeed.multiplier).toInt()
    val digAnimTicks: Int get() = (BASE_DIG_ANIM_TICKS / gameSpeed.multiplier).toInt()

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
            state.dugHoles.add(DugHole(x = targetX, y = targetY, remainingTicks = holeTotalTicks, totalTicks = holeTotalTicks))

            runner.digTimer = digAnimTicks
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
                        enemy.respawnTicks = enemyRespawnDelay
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
        val groundTile = getTile(gx, (gy + 1).coerceAtMost(LevelData.ROWS - 1))
        val isOnRope = currentTile == TileType.ROPE
        val isEnemyBelow = isTrappedEnemyAt(gx, gy + 1)

        val activeTrack = getActiveLadderTrack(rx, ry, hTolerance = 0.5f)
        val inLadderTrack = activeTrack != null
        val isSupportedOnLadderTop = activeTrack != null && abs(ry - activeTrack.yMin) < 0.35f

        // Solid ground or ladder rungs directly beneath runner (e.g. standing on top of ladder)
        val hasSolidGround = isSolid(groundTile) || isSupportedOnLadderTop || isEnemyBelow

        // 1. Gravity / Falling
        // Runner only falls if NOT on solid ground, NOT in a ladder vertical track, and NOT on rope
        if (!hasSolidGround && !inLadderTrack && !isOnRope) {
            runner.state = EntityState.FALLING
            runner.y += fallSpeed
            // Snap X to column center while falling
            runner.x = alignToCenter(runner.x, gx)

            // Landing check
            val newGy = runner.gridY
            val newGround = getTile(gx, (newGy + 1).coerceAtMost(LevelData.ROWS - 1))
            val newTrack = getActiveLadderTrack(runner.x, runner.y, hTolerance = 0.5f)
            val newOnRope = getTile(gx, newGy) == TileType.ROPE
            val newEnemyBelow = isTrappedEnemyAt(gx, newGy + 1)

            if (isSolid(newGround) || newTrack != null || newOnRope || newEnemyBelow) {
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
                if (targetCol >= 0) {
                    val currentTrack = getActiveLadderTrack(rx, ry, hTolerance = 0.5f)
                    if (currentTrack != null) {
                        val dismountRow = findAccessibleDismountRow(targetCol, ry)
                        if (dismountRow != null) {
                            runner.y = alignToCenter(runner.y, dismountRow)
                            if (abs(runner.y - dismountRow) < 0.15f) {
                                runner.y = dismountRow.toFloat()
                            }
                            runner.x -= runnerSpeed
                            runner.state = if (getTile(targetCol, dismountRow) == TileType.ROPE) EntityState.HANGING else EntityState.RUNNING
                            runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                        } else if (!isSolid(getTile(targetCol, kotlin.math.round(ry).toInt()))) {
                            runner.x -= runnerSpeed
                            runner.state = EntityState.RUNNING
                            runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                        } else {
                            runner.state = EntityState.IDLE
                        }
                    } else if (canMoveHorizontal(targetCol, gy)) {
                        runner.x -= runnerSpeed
                        runner.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                        runner.y = gy.toFloat()
                        runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                    } else {
                        runner.x = gx.toFloat()
                        runner.state = EntityState.IDLE
                    }
                } else {
                    runner.state = EntityState.IDLE
                }
            }
            Direction.RIGHT -> {
                runner.facing = Direction.RIGHT
                val targetCol = gx + 1
                if (targetCol < LevelData.COLS) {
                    val currentTrack = getActiveLadderTrack(rx, ry, hTolerance = 0.5f)
                    if (currentTrack != null) {
                        val dismountRow = findAccessibleDismountRow(targetCol, ry)
                        if (dismountRow != null) {
                            runner.y = alignToCenter(runner.y, dismountRow)
                            if (abs(runner.y - dismountRow) < 0.15f) {
                                runner.y = dismountRow.toFloat()
                            }
                            runner.x += runnerSpeed
                            runner.state = if (getTile(targetCol, dismountRow) == TileType.ROPE) EntityState.HANGING else EntityState.RUNNING
                            runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                        } else if (!isSolid(getTile(targetCol, kotlin.math.round(ry).toInt()))) {
                            runner.x += runnerSpeed
                            runner.state = EntityState.RUNNING
                            runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                        } else {
                            runner.state = EntityState.IDLE
                        }
                    } else if (canMoveHorizontal(targetCol, gy)) {
                        runner.x += runnerSpeed
                        runner.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                        runner.y = gy.toFloat()
                        runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                    } else {
                        runner.x = gx.toFloat()
                        runner.state = EntityState.IDLE
                    }
                } else {
                    runner.state = EntityState.IDLE
                }
            }
            Direction.UP -> {
                val nearbyTrack = findNearbyLadderTrack(rx, ry, hTolerance = 0.85f, forClimbingUp = true)
                if (nearbyTrack != null) {
                    val col = nearbyTrack.col
                    val xDiff = col - rx

                    // Smooth horizontal steering towards ladder center
                    if (abs(xDiff) > 0.04f) {
                        runner.x += (if (xDiff > 0) 1f else -1f) * minOf(runnerSpeed, abs(xDiff))
                        runner.facing = if (xDiff > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        runner.x = col.toFloat()
                    }

                    // Vertical climbing if within 0.45 tiles of ladder center
                    if (abs(rx - col) <= 0.45f) {
                        if (ry > nearbyTrack.yMin) {
                            runner.y = maxOf(nearbyTrack.yMin, ry - climbSpeed)
                            runner.state = EntityState.CLIMBING
                            runner.animFrame = ((state.tickCount / 6) % 2).toInt()
                        } else {
                            runner.y = nearbyTrack.yMin
                            runner.state = EntityState.IDLE
                        }
                    } else {
                        runner.state = EntityState.RUNNING
                        runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                    }
                }
            }
            Direction.DOWN -> {
                val nearbyTrack = findNearbyLadderTrack(rx, ry, hTolerance = 0.85f, forClimbingUp = false)
                if (nearbyTrack != null) {
                    val col = nearbyTrack.col
                    val xDiff = col - rx

                    // Smooth horizontal steering towards ladder center
                    if (abs(xDiff) > 0.04f) {
                        runner.x += (if (xDiff > 0) 1f else -1f) * minOf(runnerSpeed, abs(xDiff))
                        runner.facing = if (xDiff > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        runner.x = col.toFloat()
                    }

                    // Vertical climbing if within 0.45 tiles of ladder center
                    if (abs(rx - col) <= 0.45f) {
                        if (ry < nearbyTrack.yMax) {
                            runner.y = minOf(nearbyTrack.yMax, ry + climbSpeed)
                            runner.state = EntityState.CLIMBING
                            runner.animFrame = ((state.tickCount / 6) % 2).toInt()
                        } else {
                            // Reached the bottom of the ladder
                            val bottomRow = kotlin.math.round(nearbyTrack.yMax).toInt()
                            val floorBelowSolid = bottomRow < LevelData.ROWS - 1 && isSolid(getTile(col, bottomRow + 1))
                            val enemyBelow = isTrappedEnemyAt(col, bottomRow + 1)
                            if (floorBelowSolid || enemyBelow) {
                                runner.y = nearbyTrack.yMax
                                runner.state = EntityState.IDLE
                            } else {
                                // Hanging ladder: drop off into air
                                runner.y += fallSpeed
                                runner.state = EntityState.FALLING
                                soundFx.playFall()
                            }
                        }
                    } else {
                        runner.state = EntityState.RUNNING
                        runner.animFrame = ((state.tickCount / 6) % 4).toInt()
                    }
                } else if (isOnRope) {
                    // Drop down from monkey bar
                    runner.y += fallSpeed
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
                enemy.trappedTicks = enemyTrappedTicks
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
                enemy.y += fallSpeed
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
                    enemy.y += if (moveUp) -climbSpeed else climbSpeed
                    enemy.x = egx.toFloat()
                    enemy.state = EntityState.CLIMBING
                    enemy.animFrame = ((state.tickCount / 8) % 2).toInt()
                    continue
                }
            }

            // If at ladder top and player is below, climb down
            if (isAtLadderTop && diffY > 0.8f && abs(diffX) < 0.5f) {
                enemy.y += climbSpeed
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
                    enemy.x += if (moveRight) enemySpeed else -enemySpeed
                    enemy.state = if (isOnRope) EntityState.HANGING else EntityState.RUNNING
                    enemy.y = egy.toFloat()
                    enemy.animFrame = ((state.tickCount / 7) % 4).toInt()
                } else {
                    // Blocked, check if there's a ladder to take
                    if (isOnLadder) {
                        val moveUp = diffY < 0
                        enemy.y += if (moveUp) -climbSpeed else climbSpeed
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

    fun isLadder(col: Int, row: Int): Boolean {
        if (col !in 0 until LevelData.COLS || row !in 0 until LevelData.ROWS) return false
        val tile = state.activeGrid[row][col]
        return tile == TileType.LADDER.id || (tile == TileType.ESCAPE_LADDER.id && state.escapeLaddersRevealed)
    }

    data class LadderTrack(val col: Int, val yMin: Float, val yMax: Float)

    fun getLadderTrackAt(col: Int, row: Int): LadderTrack? {
        if (col !in 0 until LevelData.COLS) return null
        val candidateRows = listOf(row, row + 1, row - 1).filter { r ->
            r in 0 until LevelData.ROWS && isLadder(col, r)
        }
        val ladderRow = candidateRows.firstOrNull() ?: return null

        var top = ladderRow
        while (top > 0 && isLadder(col, top - 1)) {
            top--
        }
        var bottom = ladderRow
        while (bottom < LevelData.ROWS - 1 && isLadder(col, bottom + 1)) {
            bottom++
        }

        // Top-most accessible Y: standing on top rung (y = top - 1) if space above isn't solid
        val yMin = if (top > 0 && !isSolid(getTile(col, top - 1))) {
            (top - 1).toFloat()
        } else {
            top.toFloat()
        }

        val yMax = bottom.toFloat()
        return LadderTrack(col, yMin, yMax)
    }

    fun getActiveLadderTrack(x: Float, y: Float, hTolerance: Float = 0.5f): LadderTrack? {
        val gx = kotlin.math.round(x).toInt().coerceIn(0, LevelData.COLS - 1)
        val cols = listOf(gx, (gx - 1).coerceAtLeast(0), (gx + 1).coerceAtMost(LevelData.COLS - 1)).distinct()
        for (c in cols) {
            if (abs(x - c) <= hTolerance) {
                val candidateRows = listOf(
                    kotlin.math.round(y).toInt(),
                    kotlin.math.floor(y).toInt(),
                    kotlin.math.ceil(y).toInt()
                ).distinct()
                for (r in candidateRows) {
                    val track = getLadderTrackAt(c, r)
                    if (track != null && y >= track.yMin - 0.15f && y <= track.yMax + 0.15f) {
                        return track
                    }
                }
            }
        }
        return null
    }

    fun findNearbyLadderTrack(x: Float, y: Float, hTolerance: Float = 0.85f, forClimbingUp: Boolean): LadderTrack? {
        val gx = kotlin.math.round(x).toInt().coerceIn(0, LevelData.COLS - 1)
        val cols = listOf(gx, (gx - 1).coerceAtLeast(0), (gx + 1).coerceAtMost(LevelData.COLS - 1)).distinct()
            .sortedBy { abs(x - it) }

        for (c in cols) {
            if (abs(x - c) <= hTolerance) {
                val candidateRows = listOf(
                    kotlin.math.round(y).toInt(),
                    kotlin.math.floor(y).toInt(),
                    kotlin.math.ceil(y).toInt(),
                    kotlin.math.round(y).toInt() - 1,
                    kotlin.math.round(y).toInt() + 1
                ).distinct()

                for (r in candidateRows) {
                    val track = getLadderTrackAt(c, r)
                    if (track != null) {
                        if (forClimbingUp) {
                            if (y > track.yMin - 0.1f && y <= track.yMax + 1.1f) {
                                return track
                            }
                        } else {
                            if (y >= track.yMin - 0.5f && y < track.yMax + 0.15f) {
                                return track
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun findAccessibleDismountRow(targetCol: Int, currentY: Float): Int? {
        if (targetCol !in 0 until LevelData.COLS) return null
        val gy = kotlin.math.round(currentY).toInt().coerceIn(0, LevelData.ROWS - 1)
        val floorY = kotlin.math.floor(currentY).toInt().coerceIn(0, LevelData.ROWS - 1)
        val ceilY = kotlin.math.ceil(currentY).toInt().coerceIn(0, LevelData.ROWS - 1)
        val candidateRows = listOf(gy, floorY, ceilY).distinct().sortedBy { abs(currentY - it) }

        for (row in candidateRows) {
            if (abs(currentY - row) > 0.48f) continue
            val tileAtTarget = getTile(targetCol, row)
            if (isSolid(tileAtTarget)) continue

            val groundAtTarget = getTile(targetCol, (row + 1).coerceAtMost(LevelData.ROWS - 1))
            val isSupportedPlatform = isSolid(groundAtTarget) || 
                                      isLadder(targetCol, row + 1) || 
                                      tileAtTarget == TileType.ROPE ||
                                      isLadder(targetCol, row) ||
                                      isTrappedEnemyAt(targetCol, row + 1)

            if (isSupportedPlatform) {
                return row
            }
        }
        return null
    }

    private fun alignToCenter(current: Float, targetCell: Int): Float {
        val target = targetCell.toFloat()
        val diff = target - current
        return if (abs(diff) < 0.08f) target else current + diff * 0.35f
    }
}
