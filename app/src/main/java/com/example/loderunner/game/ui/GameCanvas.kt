package com.example.loderunner.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    gameState: GameState,
    palette: GamePalette,
    tickCount: Long = 0L,
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

        // 1. Draw Playfield Background with subtle ambient gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.background,
                    Color(0xFF0D121D),
                    palette.background
                ),
                startY = offsetY,
                endY = offsetY + playfieldHeight
            ),
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

                drawEnhancedTile(
                    tile = tile,
                    left = tileLeft,
                    top = tileTop,
                    cellSize = cellSize,
                    palette = palette,
                    escapeLaddersRevealed = gameState.escapeLaddersRevealed,
                    tickCount = tickCount
                )
            }
        }

        // 3. Draw Dug Holes
        for (hole in gameState.dugHoles) {
            val holeLeft = offsetX + hole.x * cellSize
            val holeTop = offsetY + hole.y * cellSize
            drawEnhancedDugHole(hole, holeLeft, holeTop, cellSize, palette, tickCount)
        }

        // 4. Draw Bungeling Guards
        for (enemy in gameState.enemies) {
            if (!enemy.isDead) {
                val enemyLeft = offsetX + enemy.x * cellSize
                val enemyTop = offsetY + enemy.y * cellSize
                drawEnhancedEnemy(enemy, enemyLeft, enemyTop, cellSize, palette, tickCount)
            }
        }

        // 5. Draw Runner (Player)
        val runner = gameState.runner
        val runnerLeft = offsetX + runner.x * cellSize
        val runnerTop = offsetY + runner.y * cellSize
        drawEnhancedRunner(runner, runnerLeft, runnerTop, cellSize, palette, tickCount)

        // 6. Draw CRT Scanlines & Screen Vignette
        if (showCrtScanlines) {
            drawEnhancedCrtEffects(offsetX, offsetY, playfieldWidth, playfieldHeight)
        }

        // 7. Outer Arcade Playfield Frame
        drawRoundRect(
            color = palette.buttonBorder.copy(alpha = 0.8f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(playfieldWidth, playfieldHeight),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2.5f)
        )
    }
}

private fun DrawScope.drawEnhancedTile(
    tile: TileType,
    left: Float,
    top: Float,
    cellSize: Float,
    palette: GamePalette,
    escapeLaddersRevealed: Boolean,
    tickCount: Long
) {
    when (tile) {
        TileType.BRICK -> {
            // Textured 3D Beveled Brick Platform
            // Mortar background
            drawRect(
                color = palette.brickMortar,
                topLeft = Offset(left, top),
                size = Size(cellSize, cellSize)
            )

            val rowH = (cellSize - 3f) / 3f
            val brickGap = 1.2f

            // Row 1 (2 full bricks)
            drawSingleBrick(left + 1f, top + 1f, (cellSize - 4f) / 2f, rowH, palette)
            drawSingleBrick(left + (cellSize / 2f) + 1f, top + 1f, (cellSize - 4f) / 2f, rowH, palette)

            // Row 2 (staggered: 1 half brick, 1 full, 1 half)
            val halfW = (cellSize - 5f) / 4f
            val fullW = (cellSize - 5f) / 2f
            drawSingleBrick(left + 1f, top + rowH + brickGap + 1f, halfW, rowH, palette)
            drawSingleBrick(left + halfW + brickGap + 1f, top + rowH + brickGap + 1f, fullW, rowH, palette)
            drawSingleBrick(left + halfW + fullW + brickGap * 2 + 1f, top + rowH + brickGap + 1f, halfW, rowH, palette)

            // Row 3 (2 full bricks)
            drawSingleBrick(left + 1f, top + (rowH + brickGap) * 2 + 1f, (cellSize - 4f) / 2f, rowH, palette)
            drawSingleBrick(left + (cellSize / 2f) + 1f, top + (rowH + brickGap) * 2 + 1f, (cellSize - 4f) / 2f, rowH, palette)
        }

        TileType.SOLID_ROCK -> {
            // Chiseled Bedrock / Megalith with 3D slate beveling
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(palette.solidRockHighlight, palette.solidRockMain, Color(0xFF181C30)),
                    start = Offset(left, top),
                    end = Offset(left + cellSize, top + cellSize)
                ),
                topLeft = Offset(left, top),
                size = Size(cellSize, cellSize)
            )

            // Inner chiseled border
            drawRect(
                color = palette.solidRockHighlight.copy(alpha = 0.5f),
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(cellSize - 4f, cellSize - 4f),
                style = Stroke(width = 1.5f)
            )

            // Corner rivets
            val rivetColor = Color(0xFFA0C0F0)
            drawCircle(rivetColor, 1.2f, Offset(left + 4f, top + 4f))
            drawCircle(rivetColor, 1.2f, Offset(left + cellSize - 4f, top + 4f))
            drawCircle(rivetColor, 1.2f, Offset(left + 4f, top + cellSize - 4f))
            drawCircle(rivetColor, 1.2f, Offset(left + cellSize - 4f, top + cellSize - 4f))

            // Center stone texture motif
            drawRect(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(left + cellSize * 0.35f, top + cellSize * 0.35f),
                size = Size(cellSize * 0.3f, cellSize * 0.3f)
            )
        }

        TileType.LADDER -> {
            drawEnhancedLadder(left, top, cellSize, palette.ladder, palette.ladderRung)
        }

        TileType.ESCAPE_LADDER -> {
            if (escapeLaddersRevealed) {
                // Pulsing glowing energy ladder
                val pulse = (sin(tickCount * 0.15) * 0.3 + 0.7).toFloat()
                val neonGlow = palette.escapeLadder.copy(alpha = pulse)
                drawEnhancedLadder(left, top, cellSize, neonGlow, Color.White.copy(alpha = pulse))

                // Aura glow
                drawLine(
                    color = neonGlow.copy(alpha = 0.3f),
                    start = Offset(left + cellSize * 0.5f, top),
                    end = Offset(left + cellSize * 0.5f, top + cellSize),
                    strokeWidth = cellSize * 0.6f
                )
            }
        }

        TileType.ROPE -> {
            // Braided Hand-to-Hand Suspension Bar
            val barY = top + cellSize * 0.3f

            // Shadow under bar
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(left, barY + 2f),
                end = Offset(left + cellSize, barY + 2f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Main rope rail
            drawLine(
                color = palette.rope,
                start = Offset(left, barY),
                end = Offset(left + cellSize, barY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Top highlight rail
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(left, barY - 1f),
                end = Offset(left + cellSize, barY - 1f),
                strokeWidth = 1f
            )

            // Metallic link brackets at center
            drawCircle(
                color = Color.White,
                radius = 1.8f,
                center = Offset(left + cellSize * 0.5f, barY)
            )
        }

        TileType.GOLD -> {
            // 3D Treasure Chest with sparkling glint
            val chestW = cellSize * 0.75f
            val chestH = cellSize * 0.55f
            val gLeft = left + (cellSize - chestW) / 2f
            val gTop = top + (cellSize - chestH) / 2f + cellSize * 0.15f

            // Chest drop shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(gLeft + 1f, gTop + 2f),
                size = Size(chestW, chestH),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Chest Body (golden gradient)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.goldHighlight, palette.gold, Color(0xFFC08000)),
                    startY = gTop,
                    endY = gTop + chestH
                ),
                topLeft = Offset(gLeft, gTop),
                size = Size(chestW, chestH),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Metallic iron rim / lock bands
            val bandW = 2.5f
            drawRect(Color(0xFF5A3000), Offset(gLeft + chestW * 0.25f, gTop), Size(bandW, chestH))
            drawRect(Color(0xFF5A3000), Offset(gLeft + chestW * 0.75f - bandW, gTop), Size(bandW, chestH))

            // Golden Keyhole Clasp
            drawCircle(Color.White, 2f, Offset(gLeft + chestW / 2f, gTop + chestH * 0.5f))
            drawCircle(Color.Black, 1.2f, Offset(gLeft + chestW / 2f, gTop + chestH * 0.5f))

            // Periodic Animated Sparkle Glint
            val sparklePhase = ((tickCount + (left * 17).toLong()) % 90).toInt()
            if (sparklePhase in 0..12) {
                val sparkleProgress = sparklePhase / 12f
                val sparkleSize = (sin(sparkleProgress * PI) * 5f).toFloat()
                val sparkleCenter = Offset(gLeft + chestW * 0.8f, gTop + chestH * 0.2f)

                // 4-point star sparkle
                drawLine(Color.White, Offset(sparkleCenter.x - sparkleSize, sparkleCenter.y), Offset(sparkleCenter.x + sparkleSize, sparkleCenter.y), 1.5f)
                drawLine(Color.White, Offset(sparkleCenter.x, sparkleCenter.y - sparkleSize), Offset(sparkleCenter.x, sparkleCenter.y + sparkleSize), 1.5f)
                drawCircle(Color.White, sparkleSize * 0.4f, sparkleCenter)
            }
        }

        TileType.FALSE_BRICK -> {
            drawSingleBrick(left + 1f, top + 1f, cellSize - 2f, cellSize - 2f, palette)
            // Faint crack indicating trapdoor
            drawLine(Color.Black.copy(alpha = 0.5f), Offset(left + cellSize * 0.3f, top + 2f), Offset(left + cellSize * 0.6f, top + cellSize - 2f), 1.2f)
        }

        TileType.EMPTY -> {}
    }
}

private fun DrawScope.drawSingleBrick(x: Float, y: Float, w: Float, h: Float, palette: GamePalette) {
    // 3D Beveled Clay Brick
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.brickHighlight, palette.brickMain, Color(0xFF702008)),
            startY = y,
            endY = y + h
        ),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )

    // Top & Left highlight
    drawLine(palette.brickHighlight, Offset(x, y), Offset(x + w, y), 1f)
    drawLine(palette.brickHighlight, Offset(x, y), Offset(x, y + h), 1f)

    // Bottom shadow
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(x, y + h), Offset(x + w, y + h), 1f)
}

private fun DrawScope.drawEnhancedLadder(left: Float, top: Float, cellSize: Float, railColor: Color, rungColor: Color) {
    val railInset = cellSize * 0.18f
    val railW = 3f

    // Rail drop shadows
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(left + railInset + 1f, top), Offset(left + railInset + 1f, top + cellSize), railW)
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(left + cellSize - railInset + 1f, top), Offset(left + cellSize - railInset + 1f, top + cellSize), railW)

    // Left and Right rails with metallic gradient effect
    drawLine(railColor, Offset(left + railInset, top), Offset(left + railInset, top + cellSize), railW)
    drawLine(railColor, Offset(left + cellSize - railInset, top), Offset(left + cellSize - railInset, top + cellSize), railW)

    // 4 3D cylindrical rungs
    val rungs = 4
    for (i in 0 until rungs) {
        val rungY = top + (i + 0.5f) * (cellSize / rungs)

        // Rung shadow
        drawLine(Color.Black.copy(alpha = 0.4f), Offset(left + railInset, rungY + 1.5f), Offset(left + cellSize - railInset, rungY + 1.5f), 2.5f)
        // Main rung
        drawLine(rungColor, Offset(left + railInset, rungY), Offset(left + cellSize - railInset, rungY), 2.5f)
        // Top gleam
        drawLine(Color.White.copy(alpha = 0.7f), Offset(left + railInset + 1f, rungY - 0.5f), Offset(left + cellSize - railInset - 1f, rungY - 0.5f), 1f)
    }
}

private fun DrawScope.drawEnhancedDugHole(hole: DugHole, left: Float, top: Float, cellSize: Float, palette: GamePalette, tickCount: Long) {
    if (hole.isCrumbling) {
        // Warning: brick is cracking and regenerating
        val flash = (tickCount / 5) % 2 == 0L
        val baseColor = if (flash) palette.brickHighlight.copy(alpha = 0.6f) else palette.brickMain.copy(alpha = 0.35f)

        drawRoundRect(
            color = baseColor,
            topLeft = Offset(left, top),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Jagged glowing energy fissures
        val crackColor = Color(0xFFFF9900)
        drawLine(crackColor, Offset(left + cellSize * 0.2f, top + 2f), Offset(left + cellSize * 0.5f, top + cellSize * 0.5f), 2f)
        drawLine(crackColor, Offset(left + cellSize * 0.5f, top + cellSize * 0.5f), Offset(left + cellSize * 0.8f, top + cellSize - 2f), 2f)
        drawLine(crackColor, Offset(left + cellSize * 0.8f, top + 2f), Offset(left + cellSize * 0.2f, top + cellSize - 2f), 1.5f)
    } else {
        // Open hole: deep recessed pit
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black, Color(0xFF10141E)),
                center = Offset(left + cellSize / 2f, top + cellSize / 2f),
                radius = cellSize * 0.7f
            ),
            topLeft = Offset(left, top),
            size = Size(cellSize, cellSize)
        )

        // Pit lip / rubble edge
        drawRect(
            color = palette.brickHighlight.copy(alpha = 0.5f),
            topLeft = Offset(left + 1f, top),
            size = Size(cellSize - 2f, 2f)
        )
        // Rubble stones at pit floor
        drawCircle(palette.brickMain, 2f, Offset(left + cellSize * 0.25f, top + cellSize - 3f))
        drawCircle(palette.brickHighlight, 1.5f, Offset(left + cellSize * 0.7f, top + cellSize - 2.5f))
    }
}

private fun DrawScope.drawEnhancedRunner(
    runner: Runner,
    left: Float,
    top: Float,
    cellSize: Float,
    palette: GamePalette,
    tickCount: Long
) {
    val centerX = left + cellSize / 2f
    val facingLeft = runner.facing == Direction.LEFT
    val dirSign = if (facingLeft) -1f else 1f

    // Character scale
    val headRadius = cellSize * 0.22f
    val headCenter = Offset(centerX, top + cellSize * 0.24f)

    // 1. Digging State: kneeling with laser blaster emitting glowing plasma beam
    if (runner.state == EntityState.DIGGING) {
        val digLeft = runner.digDirection == Direction.LEFT
        val blastTargetX = if (digLeft) left - cellSize * 0.6f else left + cellSize * 1.6f
        val blastTargetY = top + cellSize * 1.3f

        // Head looking down at blast
        drawCircle(palette.runnerHead, headRadius, headCenter)
        // Explorer hat / hair
        drawArc(
            color = Color(0xFFD4A040),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius - 1f),
            size = Size(headRadius * 2f, headRadius * 1.8f)
        )

        // Tunic Torso
        val torsoTop = headCenter.y + headRadius
        val torsoBottom = top + cellSize * 0.68f
        drawLine(palette.runnerBody, Offset(centerX, torsoTop), Offset(centerX, torsoBottom), 5f, cap = StrokeCap.Round)

        // Kneeling legs
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX - dirSign * cellSize * 0.2f, top + cellSize * 0.95f), 4f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX + dirSign * cellSize * 0.15f, top + cellSize * 0.85f), 4f, cap = StrokeCap.Round)

        // Futuristic Laser Blaster
        val blasterMuzzle = Offset(if (digLeft) left - 2f else left + cellSize + 2f, top + cellSize * 0.55f)
        drawLine(Color(0xFFCCCCCC), Offset(centerX, torsoTop + 3f), blasterMuzzle, 3.5f, cap = StrokeCap.Round)

        // Intense Laser Beam with energy sparks
        val beamPulse = ((tickCount % 4) * 0.5f)
        // Cyan outer energy glow
        drawLine(Color(0x8000FFFF), blasterMuzzle, Offset(blastTargetX, blastTargetY), 6f + beamPulse, cap = StrokeCap.Round)
        // White-hot plasma core
        drawLine(Color.White, blasterMuzzle, Offset(blastTargetX, blastTargetY), 2.5f, cap = StrokeCap.Round)

        // Impact spark explosion on brick
        drawCircle(Color(0xFF00FFFF), 4.5f + beamPulse, Offset(blastTargetX, blastTargetY))
        drawCircle(Color.White, 2.5f, Offset(blastTargetX, blastTargetY))
        return
    }

    // 2. Hanging on Rope
    if (runner.state == EntityState.HANGING) {
        val ropeY = top + cellSize * 0.3f
        // Head
        drawCircle(palette.runnerHead, headRadius, Offset(centerX, top + cellSize * 0.35f))
        // Torso
        val torsoY = top + cellSize * 0.45f
        drawLine(palette.runnerBody, Offset(centerX, torsoY), Offset(centerX, top + cellSize * 0.72f), 5f, cap = StrokeCap.Round)
        // Arms reaching up gripping bar
        drawLine(palette.runnerBody, Offset(centerX, torsoY), Offset(centerX - cellSize * 0.22f, ropeY), 3.5f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoY), Offset(centerX + cellSize * 0.22f, ropeY), 3.5f, cap = StrokeCap.Round)

        // Swinging legs
        val swing = sin(tickCount * 0.3).toFloat() * cellSize * 0.18f
        drawLine(palette.runnerBody, Offset(centerX, top + cellSize * 0.72f), Offset(centerX + swing, top + cellSize * 0.95f), 3.5f, cap = StrokeCap.Round)
        return
    }

    // 3. Climbing on Ladder
    if (runner.state == EntityState.CLIMBING) {
        drawCircle(palette.runnerHead, headRadius, headCenter)
        // Torso
        val torsoTop = headCenter.y + headRadius
        val torsoBottom = top + cellSize * 0.7f
        drawLine(palette.runnerBody, Offset(centerX, torsoTop), Offset(centerX, torsoBottom), 5f, cap = StrokeCap.Round)

        // Alternating climbing limbs
        val step = (runner.animFrame % 2 == 0)
        val leftHandY = if (step) top + cellSize * 0.2f else top + cellSize * 0.48f
        val rightHandY = if (step) top + cellSize * 0.48f else top + cellSize * 0.2f
        drawLine(palette.runnerBody, Offset(centerX, torsoTop + 2f), Offset(centerX - cellSize * 0.25f, leftHandY), 3.5f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoTop + 2f), Offset(centerX + cellSize * 0.25f, rightHandY), 3.5f, cap = StrokeCap.Round)

        val leftFootY = if (step) top + cellSize * 0.95f else top + cellSize * 0.8f
        val rightFootY = if (step) top + cellSize * 0.8f else top + cellSize * 0.95f
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX - cellSize * 0.2f, leftFootY), 3.5f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX + cellSize * 0.2f, rightFootY), 3.5f, cap = StrokeCap.Round)
        return
    }

    // 4. Falling
    if (runner.state == EntityState.FALLING) {
        drawCircle(palette.runnerHead, headRadius, headCenter)
        // Shocked expression
        drawCircle(Color.Black, 1.2f, Offset(headCenter.x - 2f, headCenter.y))
        drawCircle(Color.Black, 1.2f, Offset(headCenter.x + 2f, headCenter.y))

        // Torso
        val torsoTop = headCenter.y + headRadius
        val torsoBottom = top + cellSize * 0.68f
        drawLine(palette.runnerBody, Offset(centerX, torsoTop), Offset(centerX, torsoBottom), 5f, cap = StrokeCap.Round)

        // Flailing arms up
        drawLine(palette.runnerBody, Offset(centerX, torsoTop + 2f), Offset(centerX - cellSize * 0.3f, top + cellSize * 0.12f), 3.5f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoTop + 2f), Offset(centerX + cellSize * 0.3f, top + cellSize * 0.12f), 3.5f, cap = StrokeCap.Round)

        // Dangling legs
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX - cellSize * 0.15f, top + cellSize * 0.95f), 3.5f, cap = StrokeCap.Round)
        drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(centerX + cellSize * 0.15f, top + cellSize * 0.95f), 3.5f, cap = StrokeCap.Round)
        return
    }

    // 5. Normal Running / Idle
    // Head
    drawCircle(palette.runnerHead, headRadius, headCenter)
    // Explorer hair/hat
    drawArc(
        color = Color(0xFFD4A040),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius - 1f),
        size = Size(headRadius * 2f, headRadius * 1.8f)
    )
    // Eye looking forward
    val eyeX = headCenter.x + dirSign * 2f
    drawCircle(Color.Black, 1.2f, Offset(eyeX, headCenter.y))

    // Torso (Adventurer Tunic)
    val torsoTop = headCenter.y + headRadius
    val torsoBottom = top + cellSize * 0.68f
    drawLine(palette.runnerBody, Offset(centerX, torsoTop), Offset(centerX, torsoBottom), 5f, cap = StrokeCap.Round)

    // Golden Belt buckle
    drawRect(Color(0xFFFFD700), Offset(centerX - 2f, torsoBottom - 2f), Size(4f, 2.5f))

    // Animated Run Stride
    val stride = if (runner.state == EntityState.RUNNING) {
        when (runner.animFrame) {
            0 -> cellSize * 0.28f
            1 -> cellSize * 0.14f
            2 -> -cellSize * 0.14f
            else -> -cellSize * 0.28f
        }
    } else 0f

    // Arms swinging with stride
    val armSwing = if (runner.state == EntityState.RUNNING) -stride * 0.7f else dirSign * cellSize * 0.15f
    drawLine(palette.runnerBody, Offset(centerX, torsoTop + 2f), Offset(centerX + armSwing, torsoBottom - 1f), 3.5f, cap = StrokeCap.Round)

    // Legs
    val legFrontX = centerX + dirSign * stride
    val legBackX = centerX - dirSign * stride
    drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(legFrontX, top + cellSize * 0.95f), 3.8f, cap = StrokeCap.Round)
    drawLine(palette.runnerBody, Offset(centerX, torsoBottom), Offset(legBackX, top + cellSize * 0.95f), 3.8f, cap = StrokeCap.Round)

    // Boots
    val bootColor = Color(0xFF382010)
    drawRect(bootColor, Offset(legFrontX - 2f, top + cellSize * 0.92f), Size(4.5f, 3f))
    drawRect(bootColor, Offset(legBackX - 2f, top + cellSize * 0.92f), Size(4.5f, 3f))
}

private fun DrawScope.drawEnhancedEnemy(
    enemy: Enemy,
    left: Float,
    top: Float,
    cellSize: Float,
    palette: GamePalette,
    tickCount: Long
) {
    val centerX = left + cellSize / 2f
    val facingLeft = enemy.facing == Direction.LEFT
    val dirSign = if (facingLeft) -1f else 1f

    // 1. Trapped Enemy in Hole
    if (enemy.isTrapped) {
        val headRadius = cellSize * 0.25f
        val headCenterY = top + cellSize * 0.38f

        // Hooded Guard Head popping up
        drawCircle(palette.guardBody, headRadius, Offset(centerX, headCenterY))
        drawCircle(Color(0xFF280808), headRadius * 0.65f, Offset(centerX, headCenterY))

        // Flashing Stunned X Eyes
        val eyeTick = (tickCount / 4) % 2 == 0L
        val eyeColor = if (eyeTick) Color.Yellow else Color(0xFFFF4040)
        val eyeSize = 3.5f

        // Left eye X
        drawLine(eyeColor, Offset(centerX - 5f, headCenterY - 2.5f), Offset(centerX - 1.5f, headCenterY + 1.5f), 1.8f)
        drawLine(eyeColor, Offset(centerX - 1.5f, headCenterY - 2.5f), Offset(centerX - 5f, headCenterY + 1.5f), 1.8f)

        // Right eye X
        drawLine(eyeColor, Offset(centerX + 1.5f, headCenterY - 2.5f), Offset(centerX + 5f, headCenterY + 1.5f), 1.8f)
        drawLine(eyeColor, Offset(centerX + 5f, headCenterY - 2.5f), Offset(centerX + 1.5f, headCenterY + 1.5f), 1.8f)

        // Flailing hands clawing at hole rim
        val handSwing = sin(tickCount * 0.4).toFloat() * 2f
        drawCircle(palette.guardBody, 2.5f, Offset(left + 3f, top + 1f + handSwing))
        drawCircle(palette.guardBody, 2.5f, Offset(left + cellSize - 3f, top + 1f - handSwing))
        return
    }

    // 2. Active Bungeling Guard (Menacing Red Cloak & Glowing Visor)
    val headRadius = cellSize * 0.22f
    val headCenter = Offset(centerX, top + cellSize * 0.24f)

    // Crimson Hood
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.guardBody, Color(0xFF881010)),
            center = headCenter,
            radius = headRadius
        ),
        radius = headRadius,
        center = headCenter
    )

    // Dark Shadowed Visor Opening
    drawCircle(Color(0xFF140404), headRadius * 0.6f, Offset(headCenter.x + dirSign * 1.5f, headCenter.y + 1f))

    // Menacing Glowing Optic / Eyes
    val opticGlow = Color(0xFFFFEA00)
    drawCircle(opticGlow, 1.8f, Offset(headCenter.x + dirSign * 2.5f, headCenter.y + 1f))
    drawCircle(Color.White, 0.9f, Offset(headCenter.x + dirSign * 2.5f, headCenter.y + 1f))

    // Flowing Robed Torso
    val torsoTop = headCenter.y + headRadius
    val torsoBottom = top + cellSize * 0.7f
    drawLine(palette.guardBody, Offset(centerX, torsoTop), Offset(centerX, torsoBottom), 5.5f, cap = StrokeCap.Round)

    // Guard Shoulder Mantle
    drawLine(Color(0xFF700C0C), Offset(centerX - cellSize * 0.2f, torsoTop + 2f), Offset(centerX + cellSize * 0.2f, torsoTop + 2f), 3f)

    // Running Stride
    val stride = if (enemy.state == EntityState.RUNNING) {
        when (enemy.animFrame) {
            0 -> cellSize * 0.24f
            1 -> cellSize * 0.12f
            2 -> -cellSize * 0.12f
            else -> -cellSize * 0.24f
        }
    } else 0f

    // Cloaked Limbs
    drawLine(palette.guardBody, Offset(centerX, torsoBottom), Offset(centerX + dirSign * stride, top + cellSize * 0.95f), 4f, cap = StrokeCap.Round)
    drawLine(palette.guardBody, Offset(centerX, torsoBottom), Offset(centerX - dirSign * stride, top + cellSize * 0.95f), 4f, cap = StrokeCap.Round)

    // If carrying stolen gold, draw glowing treasure chest on back!
    if (enemy.hasGold) {
        val chestSize = cellSize * 0.4f
        val chestCenter = Offset(centerX - dirSign * cellSize * 0.25f, top + cellSize * 0.15f)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.goldHighlight, palette.gold),
                startY = chestCenter.y - chestSize / 2f,
                endY = chestCenter.y + chestSize / 2f
            ),
            topLeft = Offset(chestCenter.x - chestSize / 2f, chestCenter.y - chestSize / 2f),
            size = Size(chestSize, chestSize),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawCircle(Color.White, 1.5f, Offset(chestCenter.x, chestCenter.y))
    }
}

private fun DrawScope.drawEnhancedCrtEffects(left: Float, top: Float, width: Float, height: Float) {
    // 1. Scanlines
    val scanlineStep = 4f
    var y = top
    while (y < top + height) {
        drawLine(
            color = Color.Black.copy(alpha = 0.20f),
            start = Offset(left, y),
            end = Offset(left + width, y),
            strokeWidth = 1.3f
        )
        y += scanlineStep
    }

    // 2. Subtle CRT Screen Corner Vignette
    val cornerRadius = 24f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            center = Offset(left + width / 2f, top + height / 2f),
            radius = maxOf(width, height) * 0.65f
        ),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
}
