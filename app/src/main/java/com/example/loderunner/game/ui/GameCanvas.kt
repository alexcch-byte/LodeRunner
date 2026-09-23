package com.example.loderunner.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.model.DugHole
import com.example.loderunner.game.model.Enemy
import com.example.loderunner.game.model.EntityState
import com.example.loderunner.game.model.GameState
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.Runner
import com.example.loderunner.game.model.TileType
import com.example.loderunner.game.ui.theme.GamePalette

@Composable
fun GameCanvas(
    gameState: GameState,
    palette: GamePalette,
    showCrtScanlines: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val totalCols = LevelData.COLS
        val totalRows = LevelData.ROWS

        // Maintain 28:16 aspect ratio centered in canvas area
        val scaleX = size.width / totalCols
        val scaleY = size.height / totalRows
        val cellSize = minOf(scaleX, scaleY)

        val playfieldWidth = cellSize * totalCols
        val playfieldHeight = cellSize * totalRows
        val offsetX = (size.width - playfieldWidth) / 2f
        val offsetY = (size.height - playfieldHeight) / 2f

        // 1. Draw Playfield Background
        drawRect(
            color = palette.background,
            topLeft = Offset(offsetX, offsetY),
            size = Size(playfieldWidth, playfieldHeight)
        )

        // 2. Draw Tiles
        for (r in 0 until totalRows) {
            for (c in 0 until totalCols) {
                val tileId = gameState.activeGrid[r][c]
                val tile = TileType.fromId(tileId)
                val tileLeft = offsetX + c * cellSize
                val tileTop = offsetY + r * cellSize

                drawTile(
                    tile = tile,
                    left = tileLeft,
                    top = tileTop,
                    cellSize = cellSize,
                    palette = palette,
                    escapeLaddersRevealed = gameState.escapeLaddersRevealed
                )
            }
        }

        // 3. Draw Dug Holes (including crumbling / warning phase)
        for (hole in gameState.dugHoles) {
            val holeLeft = offsetX + hole.x * cellSize
            val holeTop = offsetY + hole.y * cellSize
            drawDugHole(hole, holeLeft, holeTop, cellSize, palette, gameState.tickCount)
        }

        // 4. Draw Enemies (Bungeling Guards)
        for (enemy in gameState.enemies) {
            if (!enemy.isDead) {
                val enemyLeft = offsetX + enemy.x * cellSize
                val enemyTop = offsetY + enemy.y * cellSize
                drawEnemy(enemy, enemyLeft, enemyTop, cellSize, palette)
            }
        }

        // 5. Draw Player (Runner)
        val runner = gameState.runner
        val runnerLeft = offsetX + runner.x * cellSize
        val runnerTop = offsetY + runner.y * cellSize
        drawRunner(runner, runnerLeft, runnerTop, cellSize, palette)

        // 6. Draw CRT Scanlines (optional retro filter)
        if (showCrtScanlines) {
            drawScanlines(offsetX, offsetY, playfieldWidth, playfieldHeight)
        }

        // 7. Draw Playfield Border
        drawRect(
            color = palette.buttonBorder.copy(alpha = 0.6f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(playfieldWidth, playfieldHeight),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawTile(
    tile: TileType,
    left: Float,
    top: Float,
    cellSize: Float,
    palette: GamePalette,
    escapeLaddersRevealed: Boolean
) {
    when (tile) {
        TileType.BRICK -> {
            // Authentic brick pattern: 3 rows of bricks with mortar lines
            drawRect(
                color = palette.brickMain,
                topLeft = Offset(left, top),
                size = Size(cellSize, cellSize)
            )
            // Brick horizontal mortar lines
            val rowH = cellSize / 3f
            drawLine(
                color = palette.brickMortar,
                start = Offset(left, top + rowH),
                end = Offset(left + cellSize, top + rowH),
                strokeWidth = 1.5f
            )
            drawLine(
                color = palette.brickMortar,
                start = Offset(left, top + rowH * 2),
                end = Offset(left + cellSize, top + rowH * 2),
                strokeWidth = 1.5f
            )
            // Vertical mortar joints
            drawLine(palette.brickMortar, Offset(left + cellSize * 0.5f, top), Offset(left + cellSize * 0.5f, top + rowH), 1.5f)
            drawLine(palette.brickMortar, Offset(left + cellSize * 0.25f, top + rowH), Offset(left + cellSize * 0.25f, top + rowH * 2), 1.5f)
            drawLine(palette.brickMortar, Offset(left + cellSize * 0.75f, top + rowH), Offset(left + cellSize * 0.75f, top + rowH * 2), 1.5f)
            drawLine(palette.brickMortar, Offset(left + cellSize * 0.5f, top + rowH * 2), Offset(left + cellSize * 0.5f, top + cellSize), 1.5f)

            // Top highlight
            drawLine(palette.brickHighlight, Offset(left, top), Offset(left + cellSize, top), 1f)
        }

        TileType.SOLID_ROCK -> {
            // Indestructible stone
            drawRect(
                color = palette.solidRockMain,
                topLeft = Offset(left, top),
                size = Size(cellSize, cellSize)
            )
            // 3D beveled stone outline
            drawLine(palette.solidRockHighlight, Offset(left, top), Offset(left + cellSize, top), 2f)
            drawLine(palette.solidRockHighlight, Offset(left, top), Offset(left, top + cellSize), 2f)
            drawLine(Color.Black.copy(alpha = 0.5f), Offset(left + cellSize, top), Offset(left + cellSize, top + cellSize), 2f)
            drawLine(Color.Black.copy(alpha = 0.5f), Offset(left, top + cellSize), Offset(left + cellSize, top + cellSize), 2f)
            // Inner stone pattern
            drawRect(
                color = palette.solidRockHighlight.copy(alpha = 0.3f),
                topLeft = Offset(left + cellSize * 0.2f, top + cellSize * 0.2f),
                size = Size(cellSize * 0.6f, cellSize * 0.6f)
            )
        }

        TileType.LADDER -> {
            drawLadder(left, top, cellSize, palette.ladder, palette.ladderRung)
        }

        TileType.ESCAPE_LADDER -> {
            if (escapeLaddersRevealed) {
                drawLadder(left, top, cellSize, palette.escapeLadder, palette.escapeLadder.copy(alpha = 0.8f))
            }
        }

        TileType.ROPE -> {
            // Suspended hand-to-hand horizontal bar
            val barY = top + cellSize * 0.3f
            drawLine(
                color = palette.rope,
                start = Offset(left, barY),
                end = Offset(left + cellSize, barY),
                strokeWidth = 3f
            )
            // Notch / bar texture
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 1.5f,
                center = Offset(left + cellSize * 0.5f, barY)
            )
        }

        TileType.GOLD -> {
            // Sparkling Gold Chest / Nugget
            val goldW = cellSize * 0.65f
            val goldH = cellSize * 0.5f
            val gLeft = left + (cellSize - goldW) / 2f
            val gTop = top + (cellSize - goldH) / 2f + cellSize * 0.15f

            // Golden Chest body
            drawRect(
                color = palette.gold,
                topLeft = Offset(gLeft, gTop),
                size = Size(goldW, goldH)
            )
            // Highlight band
            drawRect(
                color = palette.goldHighlight,
                topLeft = Offset(gLeft + 2f, gTop + 2f),
                size = Size(goldW - 4f, goldH * 0.35f)
            )
            // Clasp / lock
            drawCircle(
                color = Color(0xFF6B4500),
                radius = goldW * 0.12f,
                center = Offset(gLeft + goldW / 2f, gTop + goldH * 0.55f)
            )
        }

        TileType.FALSE_BRICK -> {
            // Looks like brick with faint crack
            drawRect(palette.brickMain, Offset(left, top), Size(cellSize, cellSize))
            drawLine(Color.Black.copy(alpha = 0.4f), Offset(left + cellSize * 0.3f, top), Offset(left + cellSize * 0.7f, top + cellSize), 1f)
        }

        TileType.EMPTY -> {
            // Nothing to draw
        }
    }
}

private fun DrawScope.drawLadder(left: Float, top: Float, cellSize: Float, railColor: Color, rungColor: Color) {
    val railInset = cellSize * 0.2f
    val railWidth = 2.5f

    // Left and Right rails
    drawLine(railColor, Offset(left + railInset, top), Offset(left + railInset, top + cellSize), railWidth)
    drawLine(railColor, Offset(left + cellSize - railInset, top), Offset(left + cellSize - railInset, top + cellSize), railWidth)

    // 4 horizontal rungs
    val rungs = 4
    for (i in 0 until rungs) {
        val rungY = top + (i + 0.5f) * (cellSize / rungs)
        drawLine(
            rungColor,
            Offset(left + railInset, rungY),
            Offset(left + cellSize - railInset, rungY),
            2f
        )
    }
}

private fun DrawScope.drawDugHole(hole: DugHole, left: Float, top: Float, cellSize: Float, palette: GamePalette, tickCount: Long) {
    if (hole.isCrumbling) {
        // Warning: crumbling / flashing
        val flash = (tickCount / 6) % 2 == 0L
        if (flash) {
            drawRect(
                color = palette.brickMain.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = Size(cellSize, cellSize)
            )
        }
        // Debris / crack lines
        drawLine(palette.brickHighlight, Offset(left + 2f, top + 2f), Offset(left + cellSize - 2f, top + cellSize - 2f), 2f)
        drawLine(palette.brickHighlight, Offset(left + cellSize - 2f, top + 2f), Offset(left + 2f, top + cellSize - 2f), 2f)
    } else {
        // Open hole
        drawRect(
            color = palette.background,
            topLeft = Offset(left, top),
            size = Size(cellSize, cellSize)
        )
        // Hole edge lip
        drawRect(
            color = Color.Black.copy(alpha = 0.8f),
            topLeft = Offset(left + 2f, top + 2f),
            size = Size(cellSize - 4f, cellSize - 4f)
        )
    }
}

private fun DrawScope.drawRunner(runner: Runner, left: Float, top: Float, cellSize: Float, palette: GamePalette) {
    val centerX = left + cellSize / 2f
    val headRadius = cellSize * 0.2f
    val headCenterY = top + cellSize * 0.28f

    // Draw Head
    drawCircle(palette.runnerHead, headRadius, Offset(centerX, headCenterY))

    // Torso
    val torsoTopY = headCenterY + headRadius
    val torsoBottomY = top + cellSize * 0.72f
    drawLine(palette.runnerBody, Offset(centerX, torsoTopY), Offset(centerX, torsoBottomY), 4f)

    // Limbs based on state
    when (runner.state) {
        EntityState.HANGING -> {
            // Hands reaching up to rope bar
            val ropeY = top + cellSize * 0.3f
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.2f, ropeY), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.8f, ropeY), 3f)
            // Hanging legs swinging
            val legSwing = if (runner.animFrame % 2 == 0) -cellSize * 0.15f else cellSize * 0.15f
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(centerX + legSwing, top + cellSize * 0.95f), 3f)
        }

        EntityState.CLIMBING -> {
            // Alternating limbs climbing ladder
            val alt = (runner.animFrame % 2 == 0)
            val leftHandY = if (alt) top + cellSize * 0.25f else top + cellSize * 0.5f
            val rightHandY = if (alt) top + cellSize * 0.5f else top + cellSize * 0.25f
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.2f, leftHandY), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.8f, rightHandY), 3f)

            val leftFootY = if (alt) top + cellSize * 0.95f else top + cellSize * 0.8f
            val rightFootY = if (alt) top + cellSize * 0.8f else top + cellSize * 0.95f
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.3f, leftFootY), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.7f, rightFootY), 3f)
        }

        EntityState.DIGGING -> {
            // Digging laser / blast beam
            val digLeft = runner.digDirection == Direction.LEFT
            val targetX = if (digLeft) left - cellSize * 0.4f else left + cellSize * 1.4f
            val targetY = top + cellSize * 1.2f

            // Arm pointed down-diagonal
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 4f), Offset(if (digLeft) left else left + cellSize, top + cellSize * 0.6f), 3f)
            // Bright energy beam into the brick
            drawLine(Color(0xFF00FFFF), Offset(if (digLeft) left else left + cellSize, top + cellSize * 0.6f), Offset(targetX, targetY), 3f)
            drawCircle(Color.White, 3f, Offset(targetX, targetY))

            // Legs planted
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.3f, top + cellSize * 0.95f), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.7f, top + cellSize * 0.95f), 3f)
        }

        EntityState.FALLING -> {
            // Arms flailing upward
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.15f, top + cellSize * 0.15f), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(left + cellSize * 0.85f, top + cellSize * 0.15f), 3f)
            // Legs dangling
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.4f, top + cellSize * 0.98f), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(left + cellSize * 0.6f, top + cellSize * 0.98f), 3f)
        }

        else -> {
            // Running or Idle
            val facingLeft = runner.facing == Direction.LEFT
            val legStride = if (runner.state == EntityState.RUNNING) {
                when (runner.animFrame) {
                    0 -> cellSize * 0.25f
                    1 -> cellSize * 0.12f
                    2 -> -cellSize * 0.12f
                    else -> -cellSize * 0.25f
                }
            } else 0f

            // Arms
            val armDir = if (facingLeft) -1 else 1
            drawLine(palette.runnerBody, Offset(centerX, torsoTopY + 2f), Offset(centerX + armDir * cellSize * 0.25f, torsoBottomY - 2f), 3f)

            // Legs
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(centerX - legStride, top + cellSize * 0.95f), 3f)
            drawLine(palette.runnerBody, Offset(centerX, torsoBottomY), Offset(centerX + legStride, top + cellSize * 0.95f), 3f)
        }
    }
}

private fun DrawScope.drawEnemy(enemy: Enemy, left: Float, top: Float, cellSize: Float, palette: GamePalette) {
    val centerX = left + cellSize / 2f

    if (enemy.isTrapped) {
        // Trapped in hole: head popping up with confused eyes
        val headRadius = cellSize * 0.22f
        val headCenterY = top + cellSize * 0.35f
        drawCircle(palette.guardBody, headRadius, Offset(centerX, headCenterY))
        drawCircle(palette.guardHead, headRadius * 0.6f, Offset(centerX, headCenterY))
        // Stunned X eyes
        val eyeSize = 3f
        drawLine(Color.Yellow, Offset(centerX - 4f, headCenterY - 2f), Offset(centerX - 1f, headCenterY + 1f), 1.5f)
        drawLine(Color.Yellow, Offset(centerX - 1f, headCenterY - 2f), Offset(centerX - 4f, headCenterY + 1f), 1.5f)
        drawLine(Color.Yellow, Offset(centerX + 1f, headCenterY - 2f), Offset(centerX + 4f, headCenterY + 1f), 1.5f)
        drawLine(Color.Yellow, Offset(centerX + 4f, headCenterY - 2f), Offset(centerX + 1f, headCenterY + 1f), 1.5f)
        return
    }

    val headRadius = cellSize * 0.2f
    val headCenterY = top + cellSize * 0.28f

    // Guard Hood/Head
    drawCircle(palette.guardBody, headRadius, Offset(centerX, headCenterY))
    drawCircle(palette.guardHead, headRadius * 0.5f, Offset(centerX, headCenterY + 1f))

    // Torso
    val torsoTopY = headCenterY + headRadius
    val torsoBottomY = top + cellSize * 0.72f
    drawLine(palette.guardBody, Offset(centerX, torsoTopY), Offset(centerX, torsoBottomY), 4.5f)

    // Arms & Legs
    val legStride = if (enemy.state == EntityState.RUNNING) {
        when (enemy.animFrame) {
            0 -> cellSize * 0.2f
            1 -> cellSize * 0.1f
            2 -> -cellSize * 0.1f
            else -> -cellSize * 0.2f
        }
    } else 0f

    drawLine(palette.guardBody, Offset(centerX, torsoBottomY), Offset(centerX - legStride, top + cellSize * 0.95f), 3.5f)
    drawLine(palette.guardBody, Offset(centerX, torsoBottomY), Offset(centerX + legStride, top + cellSize * 0.95f), 3.5f)

    // If carrying gold, draw gold icon over head
    if (enemy.hasGold) {
        val gSize = cellSize * 0.35f
        drawRect(palette.gold, Offset(centerX - gSize / 2f, top - gSize), Size(gSize, gSize))
    }
}

private fun DrawScope.drawScanlines(left: Float, top: Float, width: Float, height: Float) {
    val scanlineStep = 4f
    var y = top
    while (y < top + height) {
        drawLine(
            color = Color.Black.copy(alpha = 0.18f),
            start = Offset(left, y),
            end = Offset(left + width, y),
            strokeWidth = 1.2f
        )
        y += scanlineStep
    }
}
