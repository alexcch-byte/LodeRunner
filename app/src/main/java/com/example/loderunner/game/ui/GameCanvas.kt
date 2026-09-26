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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
fun GameCanvas(
    gameState: GameState,
    palette: GamePalette,
    tickCount: Long = 0L,
    showCrtScanlines: Boolean = true,
    showMiniMap: Boolean = true,
    modifier: Modifier = Modifier
) {
    var smoothCamX by remember { mutableFloatStateOf(gameState.runner.x) }
    var smoothCamY by remember { mutableFloatStateOf(gameState.runner.y) }

    // Re-center on level start or death reset
    LaunchedEffect(gameState.levelNumber, gameState.lives) {
        smoothCamX = gameState.runner.x
        smoothCamY = gameState.runner.y
    }

    // Smooth camera tracking towards runner center
    val targetX = gameState.runner.x + 0.5f
    val targetY = gameState.runner.y + 0.5f
    smoothCamX += (targetX - smoothCamX) * 0.16f
    smoothCamY += (targetY - smoothCamY) * 0.16f

    Canvas(modifier = modifier.fillMaxSize()) {
        clipRect(0f, 0f, size.width, size.height) {
            val totalCols = LevelData.COLS
            val totalRows = LevelData.ROWS

            // Make sprites 2.2x larger: show ~13 columns instead of 28!
            val targetVisibleCols = 13.0f
            val cellSize = size.width / targetVisibleCols

            val visibleCols = size.width / cellSize
            val visibleRows = size.height / cellSize

            val halfVisCols = visibleCols / 2f
            val halfVisRows = visibleRows / 2f

            // Clamp camera within 28x16 world boundaries
            val camX = if (totalCols <= visibleCols) {
                totalCols / 2f
            } else {
                smoothCamX.coerceIn(halfVisCols, totalCols - halfVisCols)
            }

            val camY = if (totalRows <= visibleRows) {
                totalRows / 2f
            } else {
                smoothCamY.coerceIn(halfVisRows, totalRows - halfVisRows)
            }

            // World-to-screen origin offset
            val originX = size.width / 2f - camX * cellSize
            val originY = size.height / 2f - camY * cellSize

            // 1. Draw Playfield Background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.background,
                        Color(0xFF0C101A),
                        palette.background
                    ),
                    startY = 0f,
                    endY = size.height
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height)
            )

            // 2. Draw Visible Tiles with bounds culling
            val minCol = ((0f - originX) / cellSize).toInt().coerceIn(0, totalCols - 1)
            val maxCol = (((size.width - originX) / cellSize) + 1).toInt().coerceIn(0, totalCols - 1)
            val minRow = ((0f - originY) / cellSize).toInt().coerceIn(0, totalRows - 1)
            val maxRow = (((size.height - originY) / cellSize) + 1).toInt().coerceIn(0, totalRows - 1)

            for (r in minRow..maxRow) {
                for (c in minCol..maxCol) {
                    val tileId = gameState.activeGrid[r][c]
                    val tile = TileType.fromId(tileId)
                    val tileLeft = originX + c * cellSize
                    val tileTop = originY + r * cellSize

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
                val holeLeft = originX + hole.x * cellSize
                val holeTop = originY + hole.y * cellSize
                if (holeLeft + cellSize >= 0 && holeLeft <= size.width && holeTop + cellSize >= 0 && holeTop <= size.height) {
                    drawEnhancedDugHole(hole, holeLeft, holeTop, cellSize, palette, tickCount)
                }
            }

            // 4. Draw Bungeling Guards
            for (enemy in gameState.enemies) {
                if (!enemy.isDead) {
                    val enemyLeft = originX + enemy.x * cellSize
                    val enemyTop = originY + enemy.y * cellSize
                    if (enemyLeft + cellSize * 1.5f >= 0 && enemyLeft <= size.width && enemyTop + cellSize * 1.5f >= 0 && enemyTop <= size.height) {
                        drawEnhancedEnemy(enemy, enemyLeft, enemyTop, cellSize, palette, tickCount)
                    }
                }
            }

            // 5. Draw Runner (Player)
            val runner = gameState.runner
            val runnerLeft = originX + runner.x * cellSize
            val runnerTop = originY + runner.y * cellSize
            drawEnhancedRunner(runner, runnerLeft, runnerTop, cellSize, palette, tickCount)

            // 6. Draw CRT Scanlines & Screen Vignette
            if (showCrtScanlines) {
                drawEnhancedCrtEffects(0f, 0f, size.width, size.height)
            }

            // 7. Outer Screen Bezel
            drawRoundRect(
                color = palette.buttonBorder.copy(alpha = 0.85f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 3f)
            )

            // 8. Draw Mini-Map / Radar
            if (showMiniMap) {
                drawMiniMap(
                    gameState = gameState,
                    palette = palette,
                    camX = camX,
                    camY = camY,
                    visCols = visibleCols,
                    visRows = visibleRows
                )
            }
        }
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
        TileType.BRICK, TileType.FALSE_BRICK -> {
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
            // High-Definition 3D Ornate Treasure Chest
            val chestW = cellSize * 0.76f
            val chestH = cellSize * 0.58f
            val gLeft = left + (cellSize - chestW) / 2f
            val gTop = top + (cellSize - chestH) / 2f + cellSize * 0.14f

            // 1. Soft golden treasure ambient aura on floor
            val auraPulse = (sin(tickCount * 0.12) * 0.15 + 0.35).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.gold.copy(alpha = auraPulse), Color.Transparent),
                    center = Offset(gLeft + chestW / 2f, gTop + chestH * 0.6f),
                    radius = chestW * 0.75f
                ),
                radius = chestW * 0.75f,
                center = Offset(gLeft + chestW / 2f, gTop + chestH * 0.6f)
            )

            // 2. Chest drop shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(gLeft + 1.5f, gTop + 3f),
                size = Size(chestW, chestH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 3. Wooden Chest Body (Rich mahogany/oak wood grain with plank groove)
            val woodDark = Color(0xFF6A3408)
            val woodMid = Color(0xFF8B4513)
            val woodLight = Color(0xFFA55A20)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(woodLight, woodMid, woodDark),
                    startY = gTop,
                    endY = gTop + chestH
                ),
                topLeft = Offset(gLeft, gTop),
                size = Size(chestW, chestH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Chest horizontal plank separator groove
            val lidSplitY = gTop + chestH * 0.38f
            drawLine(Color.Black.copy(alpha = 0.6f), Offset(gLeft, lidSplitY), Offset(gLeft + chestW, lidSplitY), 1.5f)
            drawLine(woodLight.copy(alpha = 0.6f), Offset(gLeft, lidSplitY + 1.5f), Offset(gLeft + chestW, lidSplitY + 1.5f), 1f)

            // 4. Heavy Brass/Gold Reinforcement Bands (Vertical & Horizontal)
            val bandWidth = cellSize * 0.08f
            val brassHighlight = palette.goldHighlight
            val brassBase = palette.gold
            val brassShadow = Color(0xFF805A00)

            // Left vertical band
            val band1X = gLeft + chestW * 0.18f
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(brassHighlight, brassBase, brassShadow), startX = band1X, endX = band1X + bandWidth),
                topLeft = Offset(band1X, gTop),
                size = Size(bandWidth, chestH),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )

            // Right vertical band
            val band2X = gLeft + chestW * 0.82f - bandWidth
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(brassHighlight, brassBase, brassShadow), startX = band2X, endX = band2X + bandWidth),
                topLeft = Offset(band2X, gTop),
                size = Size(bandWidth, chestH),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )

            // Top arched lid rim band
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(brassHighlight, brassBase, brassShadow), startY = gTop, endY = gTop + bandWidth),
                topLeft = Offset(gLeft, gTop),
                size = Size(chestW, bandWidth * 0.85f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Bottom base reinforcement band
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(brassHighlight, brassBase, brassShadow), startY = gTop + chestH - bandWidth * 0.7f, endY = gTop + chestH),
                topLeft = Offset(gLeft, gTop + chestH - bandWidth * 0.7f),
                size = Size(chestW, bandWidth * 0.7f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 5. Studded Brass Rivets on bands
            val rivetRadius = 1.3f
            val rivetColor = Color(0xFFFFF4C0)
            val rivetShadow = Color(0xFF402800)
            fun DrawScope.drawRivet(cx: Float, cy: Float) {
                drawCircle(rivetShadow, rivetRadius + 0.6f, Offset(cx + 0.5f, cy + 0.5f))
                drawCircle(rivetColor, rivetRadius, Offset(cx, cy))
            }
            drawRivet(band1X + bandWidth / 2f, gTop + bandWidth * 0.45f)
            drawRivet(band1X + bandWidth / 2f, lidSplitY + bandWidth * 0.6f)
            drawRivet(band1X + bandWidth / 2f, gTop + chestH - bandWidth * 0.35f)
            drawRivet(band2X + bandWidth / 2f, gTop + bandWidth * 0.45f)
            drawRivet(band2X + bandWidth / 2f, lidSplitY + bandWidth * 0.6f)
            drawRivet(band2X + bandWidth / 2f, gTop + chestH - bandWidth * 0.35f)

            // 6. Ornate Center Keyhole Escutcheon Clasp with Inset Gemstone
            val claspW = chestW * 0.24f
            val claspH = chestH * 0.38f
            val claspX = gLeft + (chestW - claspW) / 2f
            val claspY = lidSplitY - claspH * 0.32f

            // Clasp shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(claspX + 1f, claspY + 1.5f),
                size = Size(claspW, claspH),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Clasp gold plate
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF0A0), palette.gold, Color(0xFF704500)),
                    center = Offset(claspX + claspW / 2f, claspY + claspH / 2f),
                    radius = claspW * 0.7f
                ),
                topLeft = Offset(claspX, claspY),
                size = Size(claspW, claspH),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Inset Ruby / Red Gem on clasp top
            val gemY = claspY + claspH * 0.28f
            drawCircle(Color(0xFF880000), 2.2f, Offset(claspX + claspW / 2f, gemY + 0.5f))
            drawCircle(Color(0xFFFF2222), 1.8f, Offset(claspX + claspW / 2f, gemY))
            drawCircle(Color.White, 0.7f, Offset(claspX + claspW / 2f - 0.5f, gemY - 0.5f))

            // Keyhole slot
            val keyholeY = claspY + claspH * 0.62f
            drawCircle(Color(0xFF1A1005), 1.8f, Offset(claspX + claspW / 2f, keyholeY))
            val keyPath = Path().apply {
                moveTo(claspX + claspW / 2f - 1.2f, keyholeY)
                lineTo(claspX + claspW / 2f + 1.2f, keyholeY)
                lineTo(claspX + claspW / 2f + 1.6f, keyholeY + claspH * 0.22f)
                lineTo(claspX + claspW / 2f - 1.6f, keyholeY + claspH * 0.22f)
                close()
            }
            drawPath(keyPath, Color(0xFF1A1005))

            // 7. Periodic Animated 4-Point Diamond Sparkle Glint
            val sparklePhase = ((tickCount + (left * 19).toLong()) % 80).toInt()
            if (sparklePhase in 0..14) {
                val sparkleProgress = sparklePhase / 14f
                val sparkleScale = (sin(sparkleProgress * PI) * cellSize * 0.16f).toFloat()
                val sparkleCenter = Offset(gLeft + chestW * 0.82f, gTop + chestH * 0.18f)

                // Diamond glint flare
                val starPath = Path().apply {
                    moveTo(sparkleCenter.x, sparkleCenter.y - sparkleScale)
                    lineTo(sparkleCenter.x + sparkleScale * 0.28f, sparkleCenter.y - sparkleScale * 0.28f)
                    lineTo(sparkleCenter.x + sparkleScale, sparkleCenter.y)
                    lineTo(sparkleCenter.x + sparkleScale * 0.28f, sparkleCenter.y + sparkleScale * 0.28f)
                    lineTo(sparkleCenter.x, sparkleCenter.y + sparkleScale)
                    lineTo(sparkleCenter.x - sparkleScale * 0.28f, sparkleCenter.y + sparkleScale * 0.28f)
                    lineTo(sparkleCenter.x - sparkleScale, sparkleCenter.y)
                    lineTo(sparkleCenter.x - sparkleScale * 0.28f, sparkleCenter.y - sparkleScale * 0.28f)
                    close()
                }
                drawPath(starPath, Color.White)
                drawCircle(palette.goldHighlight, sparkleScale * 0.45f, sparkleCenter)
                drawCircle(Color.White, sparkleScale * 0.2f, sparkleCenter)
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

    // Scale constants for high-definition 2.2x zoomed viewport
    val charH = cellSize * 0.84f
    val charTop = top + cellSize * 0.12f
    val charBottom = charTop + charH

    // Adventurer Color Palette
    val skinTone = palette.runnerHead
    val skinShadow = Color(0xFFC88A60)
    val hairColor = Color(0xFF4A2A14)
    val hatCrownColor = Color(0xFFB88438)
    val hatBrimColor = Color(0xFFA07028)
    val hatBandColor = Color(0xFF28180A)
    val jacketMain = palette.runnerBody
    val jacketShadow = if (palette.runnerBody == Color.White) Color(0xFFB0B8C8) else palette.runnerBody.copy(alpha = 0.7f)
    val shirtColor = Color(0xFFE8E0D0)
    val beltColor = Color(0xFF3A2010)
    val buckleColor = palette.goldHighlight
    val bootColor = Color(0xFF381C08)
    val bootSoleColor = Color(0xFF180C04)
    val gloveColor = Color(0xFF6B4226)

    // ==========================================
    // 1. STATE: DIGGING (Kneeling with Sci-Fi Plasma Blaster)
    // ==========================================
    if (runner.state == EntityState.DIGGING) {
        val digLeft = runner.digDirection == Direction.LEFT
        val digSign = if (digLeft) -1f else 1f
        val blastTargetX = if (digLeft) left - cellSize * 0.55f else left + cellSize * 1.55f
        val blastTargetY = top + cellSize * 1.35f

        val crouchY = charTop + cellSize * 0.10f
        val torsoCenterY = crouchY + cellSize * 0.38f

        // Kneeling legs (one knee on ground, front knee bent up)
        val rearFootX = centerX - digSign * cellSize * 0.22f
        val frontKneeX = centerX + digSign * cellSize * 0.18f
        // Rear knee on floor
        drawRoundRect(bootColor, Offset(rearFootX - 4f, charBottom - cellSize * 0.12f), Size(cellSize * 0.26f, cellSize * 0.10f), CornerRadius(3f, 3f))
        // Front bent knee
        val kneePath = Path().apply {
            moveTo(centerX, torsoCenterY + cellSize * 0.12f)
            lineTo(frontKneeX, charBottom - cellSize * 0.22f)
            lineTo(frontKneeX + digSign * cellSize * 0.14f, charBottom - 2f)
            lineTo(frontKneeX + digSign * cellSize * 0.04f, charBottom - 2f)
            lineTo(frontKneeX - digSign * cellSize * 0.08f, charBottom - cellSize * 0.18f)
            close()
        }
        drawPath(kneePath, jacketMain)
        // Front boot
        drawRoundRect(bootColor, Offset(frontKneeX + digSign * cellSize * 0.02f, charBottom - cellSize * 0.12f), Size(cellSize * 0.24f, cellSize * 0.12f), CornerRadius(3f, 3f))

        // Torso leaning forward toward digging direction
        val torsoW = cellSize * 0.26f
        val torsoH = cellSize * 0.28f
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(jacketMain, jacketShadow)),
            topLeft = Offset(centerX - torsoW / 2f + digSign * cellSize * 0.06f, crouchY + cellSize * 0.22f),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Belt
        drawRect(beltColor, Offset(centerX - torsoW / 2f + digSign * cellSize * 0.06f, crouchY + cellSize * 0.46f), Size(torsoW, cellSize * 0.045f))
        drawRect(buckleColor, Offset(centerX - 2.5f + digSign * cellSize * 0.06f, crouchY + cellSize * 0.46f), Size(5f, cellSize * 0.045f))

        // Head looking down at blast
        val headR = cellSize * 0.14f
        val headX = centerX + digSign * cellSize * 0.12f
        val headY = crouchY + cellSize * 0.16f
        // Hair
        drawCircle(hairColor, headR * 1.05f, Offset(headX - digSign * 2f, headY + 1f))
        // Face
        drawCircle(skinTone, headR, Offset(headX, headY))
        // Determined squint eye
        drawCircle(Color.White, 2.2f, Offset(headX + digSign * headR * 0.5f, headY + headR * 0.2f))
        drawCircle(Color.Black, 1.4f, Offset(headX + digSign * headR * 0.58f, headY + headR * 0.2f))
        drawLine(Color.Black, Offset(headX + digSign * headR * 0.2f, headY), Offset(headX + digSign * headR * 0.75f, headY + headR * 0.15f), 1.6f)

        // Fedora Hat (angled down)
        val brimL = headX - cellSize * 0.22f
        val brimT = headY - headR * 0.9f
        drawRoundRect(hatBrimColor, Offset(brimL, brimT + digSign * 3f), Size(cellSize * 0.44f, cellSize * 0.06f), CornerRadius(2f, 2f))
        drawRoundRect(hatCrownColor, Offset(headX - cellSize * 0.13f, brimT - cellSize * 0.10f + digSign * 3f), Size(cellSize * 0.26f, cellSize * 0.11f), CornerRadius(3f, 3f))
        drawRect(hatBandColor, Offset(headX - cellSize * 0.13f, brimT - cellSize * 0.02f + digSign * 3f), Size(cellSize * 0.26f, cellSize * 0.03f))

        // Sci-Fi Plasma Blaster Rifle
        val rifleStockX = centerX - digSign * cellSize * 0.06f
        val rifleStockY = crouchY + cellSize * 0.32f
        val rifleMuzzleX = centerX + digSign * cellSize * 0.42f
        val rifleMuzzleY = crouchY + cellSize * 0.48f

        // Heavy rifle body
        drawLine(Color(0xFF303644), Offset(rifleStockX, rifleStockY), Offset(rifleMuzzleX, rifleMuzzleY), cellSize * 0.10f, StrokeCap.Round)
        // Glowing cyan energy cell chamber on rifle
        val energyCoreX = (rifleStockX + rifleMuzzleX) / 2f
        val energyCoreY = (rifleStockY + rifleMuzzleY) / 2f
        drawCircle(Color(0xFF00E5FF), cellSize * 0.06f, Offset(energyCoreX, energyCoreY))
        drawCircle(Color.White, cellSize * 0.03f, Offset(energyCoreX, energyCoreY))
        // Extended emitter nozzle
        drawLine(Color(0xFF708090), Offset(rifleMuzzleX, rifleMuzzleY), Offset(rifleMuzzleX + digSign * cellSize * 0.10f, rifleMuzzleY + cellSize * 0.06f), 3.5f, StrokeCap.Square)

        // Gloved hands holding rifle
        drawCircle(gloveColor, cellSize * 0.045f, Offset(rifleStockX + digSign * cellSize * 0.10f, rifleStockY + cellSize * 0.03f))
        drawCircle(gloveColor, cellSize * 0.045f, Offset(rifleMuzzleX - digSign * cellSize * 0.04f, rifleMuzzleY - cellSize * 0.02f))

        // Plasma Laser Beam with expanding energy rings & disintegration sparks
        val muzzleTip = Offset(rifleMuzzleX + digSign * cellSize * 0.10f, rifleMuzzleY + cellSize * 0.06f)
        val targetPt = Offset(blastTargetX, blastTargetY)

        // Outer neon aura
        val beamFlicker = ((tickCount % 3) * 0.8f)
        drawLine(Color(0x6000E5FF), muzzleTip, targetPt, 8f + beamFlicker, StrokeCap.Round)
        // Electric blue laser beam core
        drawLine(Color(0xFF00E5FF), muzzleTip, targetPt, 3.5f + beamFlicker * 0.5f, StrokeCap.Round)
        // White-hot plasma core line
        drawLine(Color.White, muzzleTip, targetPt, 1.8f, StrokeCap.Round)

        // Expanding concentric plasma energy rings down the beam
        val ringCount = 3
        for (i in 1..ringCount) {
            val ringProgress = ((tickCount * 0.15f + i / ringCount.toFloat()) % 1f)
            val ringPos = Offset(
                muzzleTip.x + (targetPt.x - muzzleTip.x) * ringProgress,
                muzzleTip.y + (targetPt.y - muzzleTip.y) * ringProgress
            )
            drawCircle(Color(0x8000FFFF), 3f + ringProgress * 4f, ringPos, style = Stroke(width = 1.2f))
        }

        // Brick impact explosion: sparks and disintegrating rubble
        drawCircle(Color(0x9000E5FF), 9f + beamFlicker * 2f, targetPt)
        drawCircle(Color(0xFFFFF0A0), 5f, targetPt)
        drawCircle(Color.White, 3f, targetPt)

        // Disintegrating brick particles
        for (p in 0..4) {
            val sparkAngle = (p * 45f + (tickCount * 18f) % 360f) * (PI / 180f)
            val sparkDist = 4f + ((tickCount + p * 7) % 10) * 1.2f
            val sx = targetPt.x + cos(sparkAngle).toFloat() * sparkDist
            val sy = targetPt.y - sin(sparkAngle).toFloat() * sparkDist
            drawCircle(if (p % 2 == 0) palette.brickHighlight else Color(0xFFFFD700), 1.6f, Offset(sx, sy))
        }
        return
    }

    // ==========================================
    // 2. STATE: HANGING ON ROPE / MONKEY BAR
    // ==========================================
    if (runner.state == EntityState.HANGING) {
        val ropeY = top + cellSize * 0.30f
        val hangingY = ropeY + cellSize * 0.08f

        // Head looking slightly forward
        val headR = cellSize * 0.13f
        val headCenter = Offset(centerX, hangingY + cellSize * 0.15f)
        drawCircle(hairColor, headR * 1.05f, Offset(headCenter.x - dirSign * 2f, headCenter.y + 1f))
        drawCircle(skinTone, headR, headCenter)
        // Eyes looking forward
        drawCircle(Color.White, 2.2f, Offset(headCenter.x + dirSign * 3f, headCenter.y))
        drawCircle(Color.Black, 1.3f, Offset(headCenter.x + dirSign * 3.8f, headCenter.y))

        // Fedora hat
        val brimT = headCenter.y - headR * 0.88f
        drawRoundRect(hatBrimColor, Offset(headCenter.x - cellSize * 0.22f, brimT), Size(cellSize * 0.44f, cellSize * 0.05f), CornerRadius(2f, 2f))
        drawRoundRect(hatCrownColor, Offset(headCenter.x - cellSize * 0.13f, brimT - cellSize * 0.10f), Size(cellSize * 0.26f, cellSize * 0.11f), CornerRadius(3f, 3f))
        drawRect(hatBandColor, Offset(headCenter.x - cellSize * 0.13f, brimT - cellSize * 0.02f), Size(cellSize * 0.26f, cellSize * 0.03f))

        // Arms reaching straight UP gripping the rope bar
        val leftGripX = centerX - cellSize * 0.16f
        val rightGripX = centerX + cellSize * 0.16f
        val torsoTop = headCenter.y + headR * 0.9f
        val torsoBottom = torsoTop + cellSize * 0.26f

        // Shoulders & arms pulled up
        drawLine(jacketMain, Offset(centerX - cellSize * 0.10f, torsoTop), Offset(leftGripX, ropeY + 2f), cellSize * 0.07f, StrokeCap.Round)
        drawLine(jacketMain, Offset(centerX + cellSize * 0.10f, torsoTop), Offset(rightGripX, ropeY + 2f), cellSize * 0.07f, StrokeCap.Round)

        // Gloved hands clasping rope bar
        drawRoundRect(gloveColor, Offset(leftGripX - 3.5f, ropeY - 2.5f), Size(7f, 6f), CornerRadius(2f, 2f))
        drawRoundRect(gloveColor, Offset(rightGripX - 3.5f, ropeY - 2.5f), Size(7f, 6f), CornerRadius(2f, 2f))

        // Torso
        val torsoW = cellSize * 0.24f
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(jacketMain, jacketShadow)),
            topLeft = Offset(centerX - torsoW / 2f, torsoTop),
            size = Size(torsoW, cellSize * 0.26f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Belt
        drawRect(beltColor, Offset(centerX - torsoW / 2f, torsoBottom - cellSize * 0.04f), Size(torsoW, cellSize * 0.04f))
        drawRect(buckleColor, Offset(centerX - 2f, torsoBottom - cellSize * 0.04f), Size(4f, cellSize * 0.04f))

        // Legs swinging smoothly with pendular physics
        val legSwing = sin(tickCount * 0.28).toFloat() * cellSize * 0.14f
        val legW = cellSize * 0.075f
        val legBottomY = torsoBottom + cellSize * 0.28f

        // Back leg
        drawLine(jacketShadow, Offset(centerX - cellSize * 0.06f, torsoBottom), Offset(centerX - cellSize * 0.06f + legSwing * 0.8f, legBottomY - 3f), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX - cellSize * 0.06f + legSwing * 0.8f - 3f, legBottomY - 4f), Size(cellSize * 0.16f, cellSize * 0.08f), CornerRadius(2f, 2f))

        // Front leg
        drawLine(jacketMain, Offset(centerX + cellSize * 0.06f, torsoBottom), Offset(centerX + cellSize * 0.06f + legSwing, legBottomY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX + cellSize * 0.06f + legSwing - 3f, legBottomY), Size(cellSize * 0.16f, cellSize * 0.08f), CornerRadius(2f, 2f))
        return
    }

    // ==========================================
    // 3. STATE: CLIMBING LADDER
    // ==========================================
    if (runner.state == EntityState.CLIMBING) {
        val climbStep = (runner.animFrame % 2 == 0)
        val headR = cellSize * 0.13f
        val headCenter = Offset(centerX, charTop + cellSize * 0.16f)

        // Explorer canvas backpack on back!
        val packW = cellSize * 0.22f
        val packH = cellSize * 0.24f
        val packX = centerX - packW / 2f
        val packY = headCenter.y + headR * 0.8f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF8B5A2B), Color(0xFF5A3818))),
            topLeft = Offset(packX, packY),
            size = Size(packW, packH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Backpack flap & buckle
        drawRoundRect(Color(0xFF6B4226), Offset(packX, packY), Size(packW, packH * 0.45f), CornerRadius(3f, 3f))
        drawCircle(palette.goldHighlight, 2f, Offset(centerX, packY + packH * 0.45f))

        // Torso
        val torsoTop = headCenter.y + headR * 0.7f
        val torsoBottom = torsoTop + cellSize * 0.28f

        // Head (back/three-quarter view)
        drawCircle(hairColor, headR * 1.05f, Offset(headCenter.x, headCenter.y + 1f))
        drawCircle(skinTone, headR, headCenter)

        // Fedora from behind
        val brimT = headCenter.y - headR * 0.88f
        drawRoundRect(hatBrimColor, Offset(centerX - cellSize * 0.22f, brimT), Size(cellSize * 0.44f, cellSize * 0.05f), CornerRadius(2f, 2f))
        drawRoundRect(hatCrownColor, Offset(centerX - cellSize * 0.13f, brimT - cellSize * 0.10f), Size(cellSize * 0.26f, cellSize * 0.11f), CornerRadius(3f, 3f))
        drawRect(hatBandColor, Offset(centerX - cellSize * 0.13f, brimT - cellSize * 0.02f), Size(cellSize * 0.26f, cellSize * 0.03f))

        // Alternating climbing limbs reaching for rungs
        val armW = cellSize * 0.07f
        val leftHandY = if (climbStep) charTop + cellSize * 0.08f else charTop + cellSize * 0.32f
        val rightHandY = if (climbStep) charTop + cellSize * 0.32f else charTop + cellSize * 0.08f
        val ladderRungX = cellSize * 0.25f

        // Left arm & gloved hand
        drawLine(jacketMain, Offset(centerX - cellSize * 0.10f, torsoTop + 4f), Offset(centerX - ladderRungX, leftHandY), armW, StrokeCap.Round)
        drawRoundRect(gloveColor, Offset(centerX - ladderRungX - 3.5f, leftHandY - 3f), Size(7f, 6f), CornerRadius(2f, 2f))

        // Right arm & gloved hand
        drawLine(jacketMain, Offset(centerX + cellSize * 0.10f, torsoTop + 4f), Offset(centerX + ladderRungX, rightHandY), armW, StrokeCap.Round)
        drawRoundRect(gloveColor, Offset(centerX + ladderRungX - 3.5f, rightHandY - 3f), Size(7f, 6f), CornerRadius(2f, 2f))

        // Alternating climbing legs
        val legW = cellSize * 0.08f
        val leftFootY = if (climbStep) charBottom - cellSize * 0.05f else charBottom - cellSize * 0.18f
        val rightFootY = if (climbStep) charBottom - cellSize * 0.18f else charBottom - cellSize * 0.05f

        // Left leg & boot
        drawLine(jacketShadow, Offset(centerX - cellSize * 0.07f, torsoBottom), Offset(centerX - ladderRungX * 0.8f, leftFootY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX - ladderRungX * 0.8f - 4f, leftFootY - 2f), Size(cellSize * 0.16f, cellSize * 0.08f), CornerRadius(2f, 2f))

        // Right leg & boot
        drawLine(jacketMain, Offset(centerX + cellSize * 0.07f, torsoBottom), Offset(centerX + ladderRungX * 0.8f, rightFootY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX + ladderRungX * 0.8f - 4f, rightFootY - 2f), Size(cellSize * 0.16f, cellSize * 0.08f), CornerRadius(2f, 2f))
        return
    }

    // ==========================================
    // 4. STATE: FALLING (Panic Spread-Eagle Freefall)
    // ==========================================
    if (runner.state == EntityState.FALLING) {
        val headR = cellSize * 0.13f
        val headCenter = Offset(centerX, charTop + cellSize * 0.20f)

        // Hat flying off head slightly in the wind!
        val hatDriftY = charTop + cellSize * 0.05f
        drawRoundRect(hatBrimColor, Offset(centerX - cellSize * 0.22f, hatDriftY + cellSize * 0.07f), Size(cellSize * 0.44f, cellSize * 0.05f), CornerRadius(2f, 2f))
        drawRoundRect(hatCrownColor, Offset(centerX - cellSize * 0.13f, hatDriftY), Size(cellSize * 0.26f, cellSize * 0.09f), CornerRadius(3f, 3f))
        drawRect(hatBandColor, Offset(centerX - cellSize * 0.13f, hatDriftY + cellSize * 0.06f), Size(cellSize * 0.26f, cellSize * 0.02f))

        // Wild disheveled hair
        drawCircle(hairColor, headR * 1.1f, Offset(headCenter.x, headCenter.y - 1f))
        // Face
        drawCircle(skinTone, headR, headCenter)

        // Comical panicked wide shocked eyes & open mouth
        drawCircle(Color.White, 3f, Offset(headCenter.x - 3.5f, headCenter.y - 1.5f))
        drawCircle(Color.White, 3f, Offset(headCenter.x + 3.5f, headCenter.y - 1.5f))
        drawCircle(Color.Black, 1.6f, Offset(headCenter.x - 3.5f, headCenter.y - 1.5f))
        drawCircle(Color.Black, 1.6f, Offset(headCenter.x + 3.5f, headCenter.y - 1.5f))
        // Wide shocked O mouth
        drawCircle(Color(0xFF3A1010), 2.8f, Offset(headCenter.x, headCenter.y + 3.5f))

        // Torso
        val torsoTop = headCenter.y + headR * 0.85f
        val torsoBottom = torsoTop + cellSize * 0.26f
        val torsoW = cellSize * 0.26f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(jacketMain, jacketShadow)),
            topLeft = Offset(centerX - torsoW / 2f, torsoTop),
            size = Size(torsoW, cellSize * 0.26f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Arms flailing high in panic
        val armW = cellSize * 0.07f
        val flail = sin(tickCount * 0.5).toFloat() * 3f
        drawLine(jacketMain, Offset(centerX - torsoW * 0.4f, torsoTop + 2f), Offset(centerX - cellSize * 0.28f, charTop + cellSize * 0.06f + flail), armW, StrokeCap.Round)
        drawCircle(gloveColor, cellSize * 0.045f, Offset(centerX - cellSize * 0.28f, charTop + cellSize * 0.06f + flail))

        drawLine(jacketMain, Offset(centerX + torsoW * 0.4f, torsoTop + 2f), Offset(centerX + cellSize * 0.28f, charTop + cellSize * 0.06f - flail), armW, StrokeCap.Round)
        drawCircle(gloveColor, cellSize * 0.045f, Offset(centerX + cellSize * 0.28f, charTop + cellSize * 0.06f - flail))

        // Splayed legs kicking
        val legW = cellSize * 0.08f
        drawLine(jacketMain, Offset(centerX - cellSize * 0.06f, torsoBottom), Offset(centerX - cellSize * 0.24f, charBottom - cellSize * 0.05f), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX - cellSize * 0.32f, charBottom - cellSize * 0.08f), Size(cellSize * 0.18f, cellSize * 0.09f), CornerRadius(2f, 2f))

        drawLine(jacketMain, Offset(centerX + cellSize * 0.06f, torsoBottom), Offset(centerX + cellSize * 0.24f, charBottom - cellSize * 0.05f), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX + cellSize * 0.16f, charBottom - cellSize * 0.08f), Size(cellSize * 0.18f, cellSize * 0.09f), CornerRadius(2f, 2f))
        return
    }

    // ==========================================
    // 5. STATE: RUNNING / IDLE (High-Definition Adventurer Character)
    // ==========================================
    val isRunning = runner.state == EntityState.RUNNING

    // 4-frame dynamic running stride calculations
    val animPhase = if (isRunning) runner.animFrame % 4 else 0
    val strideProgress = when (animPhase) {
        0 -> 1.0f    // Full stride extension: front leg forward, back leg trailing
        1 -> 0.35f   // Passing pose: body raised
        2 -> -1.0f   // Opposite full stride extension
        else -> -0.35f
    }

    val torsoBob = if (isRunning) (if (animPhase % 2 == 1) -cellSize * 0.03f else 0f) else sin(tickCount * 0.08).toFloat() * 1.5f
    val torsoLean = if (isRunning) dirSign * cellSize * 0.05f else 0f
    val effectiveCharTop = charTop + torsoBob

    // A. Head & Fedora Hat
    val headR = cellSize * 0.135f
    val headX = centerX + torsoLean * 1.1f
    val headY = effectiveCharTop + cellSize * 0.16f

    // Hair under hat
    drawCircle(hairColor, headR * 1.05f, Offset(headX - dirSign * 2.5f, headY + 1f))
    // Sideburns / fringe
    drawRoundRect(hairColor, Offset(headX - dirSign * headR * 0.2f, headY - 1f), Size(cellSize * 0.07f, cellSize * 0.11f), CornerRadius(2f, 2f))

    // Face
    drawCircle(skinTone, headR, Offset(headX, headY))

    // Expressive Eye looking in facing direction
    val eyeX = headX + dirSign * headR * 0.42f
    val eyeY = headY - headR * 0.05f
    // Sclera (white)
    drawRoundRect(Color.White, Offset(eyeX - 3f, eyeY - 2.5f), Size(6f, 5.5f), CornerRadius(2f, 2f))
    // Iris (blue/dark)
    drawCircle(Color(0xFF183060), 1.8f, Offset(eyeX + dirSign * 0.8f, eyeY))
    // Specular highlight dot
    drawCircle(Color.White, 0.8f, Offset(eyeX + dirSign * 0.8f - 0.5f, eyeY - 0.8f))
    // Eyebrow
    drawLine(hairColor, Offset(eyeX - 3.5f, eyeY - 4.5f), Offset(eyeX + 3.5f, eyeY - 3.5f + dirSign * 0.5f), 1.6f, StrokeCap.Round)

    // Nose & mouth
    drawCircle(skinShadow, 1.0f, Offset(headX + dirSign * headR * 0.85f, headY + 1.5f))
    drawLine(skinShadow, Offset(headX + dirSign * headR * 0.3f, headY + headR * 0.55f), Offset(headX + dirSign * headR * 0.65f, headY + headR * 0.55f), 1.2f, StrokeCap.Round)

    // Fedora Hat
    val hatBrimW = cellSize * 0.44f
    val hatBrimH = cellSize * 0.055f
    val hatCrownW = cellSize * 0.26f
    val hatCrownH = cellSize * 0.11f
    val hatTilt = if (isRunning) dirSign * 2f else 0f
    val brimY = headY - headR * 0.88f

    // Hat brim with 3D highlight and curve
    drawRoundRect(hatBrimColor, Offset(headX - hatBrimW / 2f + dirSign * cellSize * 0.02f, brimY + hatTilt), Size(hatBrimW, hatBrimH), CornerRadius(2.5f, 2.5f))
    drawLine(Color(0xFFDEC080), Offset(headX - hatBrimW / 2f + 3f, brimY + hatTilt + 0.8f), Offset(headX + hatBrimW / 2f - 3f, brimY + hatTilt + 0.8f), 1f)

    // Hat Crown (creased adventurer fedora top)
    val crownX = headX - hatCrownW / 2f + dirSign * cellSize * 0.01f
    val crownY = brimY - hatCrownH + hatTilt
    val hatCrownPath = Path().apply {
        moveTo(crownX, crownY + hatCrownH)
        lineTo(crownX + hatCrownW * 0.1f, crownY + hatCrownH * 0.2f)
        lineTo(crownX + hatCrownW * 0.35f, crownY + hatCrownH * 0.35f) // Crease indent
        lineTo(crownX + hatCrownW * 0.5f, crownY + hatCrownH * 0.25f)
        lineTo(crownX + hatCrownW * 0.65f, crownY + hatCrownH * 0.35f) // Crease indent
        lineTo(crownX + hatCrownW * 0.9f, crownY + hatCrownH * 0.2f)
        lineTo(crownX + hatCrownW, crownY + hatCrownH)
        close()
    }
    drawPath(hatCrownPath, Brush.verticalGradient(listOf(Color(0xFFDEC080), hatCrownColor)))
    // Dark brown hat ribbon band
    drawRect(hatBandColor, Offset(crownX + 1f, brimY - cellSize * 0.032f + hatTilt), Size(hatCrownW - 2f, cellSize * 0.032f))

    // B. Torso & Explorer Outfit
    val torsoTop = headY + headR * 0.80f
    val torsoH = cellSize * 0.27f
    val torsoBottom = torsoTop + torsoH
    val torsoW = cellSize * 0.25f
    val torsoX = centerX - torsoW / 2f + torsoLean

    // Adventurer Jacket/Vest
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(jacketMain, jacketShadow),
            startX = torsoX,
            endX = torsoX + torsoW
        ),
        topLeft = Offset(torsoX, torsoTop),
        size = Size(torsoW, torsoH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Shirt collar visible at neck
    val collarPath = Path().apply {
        moveTo(headX - 4f, torsoTop)
        lineTo(headX, torsoTop + cellSize * 0.09f)
        lineTo(headX + 4f, torsoTop)
        close()
    }
    drawPath(collarPath, shirtColor)

    // Cross-body leather explorer harness strap
    drawLine(Color(0xFF6B3A14), Offset(headX - dirSign * torsoW * 0.35f, torsoTop + 2f), Offset(headX + dirSign * torsoW * 0.35f, torsoBottom - 4f), 2.2f)
    drawCircle(palette.goldHighlight, 1.8f, Offset(headX, torsoTop + torsoH * 0.5f))

    // Adventurer Utility Belt & Buckle
    val beltH = cellSize * 0.045f
    val beltY = torsoBottom - beltH
    drawRect(beltColor, Offset(torsoX, beltY), Size(torsoW, beltH))
    drawRect(buckleColor, Offset(headX - 2.5f, beltY), Size(5f, beltH))
    // Equipment pouch on hip
    drawRoundRect(Color(0xFF5A3010), Offset(torsoX - dirSign * 2f, beltY - 2f), Size(cellSize * 0.08f, cellSize * 0.07f), CornerRadius(1.5f, 1.5f))

    // C. Articulated Legs & Boots (Realistic Running Stride)
    val legW = cellSize * 0.08f
    val maxStride = cellSize * 0.24f
    val frontStride = dirSign * strideProgress * maxStride
    val rearStride = -dirSign * strideProgress * maxStride

    val frontFootY = charBottom - (if (frontStride * dirSign < 0) cellSize * 0.06f else 0f)
    val rearFootY = charBottom - (if (rearStride * dirSign < 0) cellSize * 0.06f else 0f)

    // Rear leg (drawn behind)
    val rearKneeX = centerX + torsoLean + rearStride * 0.6f
    val rearKneeY = torsoBottom + cellSize * 0.14f
    val rearFootX = centerX + torsoLean + rearStride
    drawLine(jacketShadow, Offset(centerX + torsoLean, torsoBottom), Offset(rearKneeX, rearKneeY), legW, StrokeCap.Round)
    drawLine(jacketShadow, Offset(rearKneeX, rearKneeY), Offset(rearFootX, rearFootY), legW, StrokeCap.Round)
    // Rear boot
    drawRoundRect(bootColor, Offset(rearFootX - cellSize * 0.06f + dirSign * cellSize * 0.04f, rearFootY - cellSize * 0.07f), Size(cellSize * 0.18f, cellSize * 0.08f), CornerRadius(2f, 2f))
    drawRect(bootSoleColor, Offset(rearFootX - cellSize * 0.06f + dirSign * cellSize * 0.04f, rearFootY - cellSize * 0.015f), Size(cellSize * 0.18f, cellSize * 0.02f))

    // Front leg (drawn in front)
    val frontKneeX = centerX + torsoLean + frontStride * 0.6f
    val frontKneeY = torsoBottom + cellSize * 0.14f
    val frontFootX = centerX + torsoLean + frontStride
    drawLine(jacketMain, Offset(centerX + torsoLean, torsoBottom), Offset(frontKneeX, frontKneeY), legW, StrokeCap.Round)
    drawLine(jacketMain, Offset(frontKneeX, frontKneeY), Offset(frontFootX, frontFootY), legW, StrokeCap.Round)
    // Front boot
    drawRoundRect(bootColor, Offset(frontFootX - cellSize * 0.06f + dirSign * cellSize * 0.04f, frontFootY - cellSize * 0.07f), Size(cellSize * 0.18f, cellSize * 0.08f), CornerRadius(2f, 2f))
    drawRect(bootSoleColor, Offset(frontFootX - cellSize * 0.06f + dirSign * cellSize * 0.04f, frontFootY - cellSize * 0.015f), Size(cellSize * 0.18f, cellSize * 0.02f))

    // D. Articulated Arms (Swinging in opposition to stride)
    val armW = cellSize * 0.07f
    val armSwing = if (isRunning) -frontStride * 0.85f else dirSign * cellSize * 0.06f

    // Rear arm
    val rearHandX = headX - armSwing
    val rearHandY = torsoTop + cellSize * 0.18f
    drawLine(jacketShadow, Offset(headX - dirSign * torsoW * 0.35f, torsoTop + 3f), Offset(rearHandX, rearHandY), armW, StrokeCap.Round)
    drawCircle(gloveColor, cellSize * 0.04f, Offset(rearHandX, rearHandY))

    // Front arm
    val frontHandX = headX + armSwing
    val frontHandY = torsoTop + cellSize * 0.18f
    drawLine(jacketMain, Offset(headX + dirSign * torsoW * 0.35f, torsoTop + 3f), Offset(frontHandX, frontHandY), armW, StrokeCap.Round)
    drawCircle(gloveColor, cellSize * 0.04f, Offset(frontHandX, frontHandY))
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

    val charH = cellSize * 0.84f
    val charTop = top + cellSize * 0.12f
    val charBottom = charTop + charH

    // Bungeling Guard Dark Color Scheme
    val cowlMain = palette.guardBody
    val cowlShadow = Color(0xFF6B0808)
    val cowlHighlight = Color(0xFFFF4848)
    val armorSteel = Color(0xFF282E38)
    val armorHighlight = Color(0xFF505A6E)
    val visorVoid = Color(0xFF100202)
    val opticGlow = Color(0xFFFFD700)
    val opticCore = Color(0xFFFFFFFF)
    val gauntletColor = Color(0xFF381414)
    val bootColor = Color(0xFF201010)

    // ==========================================
    // 1. STATE: TRAPPED IN DUG PIT (Frantic Struggling Animation)
    // ==========================================
    if (enemy.isTrapped) {
        val pitRimY = top + cellSize * 0.06f
        val trapBob = sin(tickCount * 0.45).toFloat() * 2f
        val trapHeadY = top + cellSize * 0.38f + trapBob

        // Guard half submerged inside the pit!
        // Shoulder / cowl base trapped in pit
        val cowW = cellSize * 0.46f
        val cowH = cellSize * 0.32f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(cowlHighlight, cowlMain, cowlShadow)),
            topLeft = Offset(centerX - cowW / 2f, trapHeadY - cowH * 0.2f),
            size = Size(cowW, cowH),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Sinister Cowl Head
        val headR = cellSize * 0.18f
        drawCircle(
            brush = Brush.radialGradient(listOf(cowlHighlight, cowlMain, cowlShadow), center = Offset(centerX, trapHeadY), radius = headR),
            radius = headR,
            center = Offset(centerX, trapHeadY)
        )

        // Shadowed Visor Opening
        drawRoundRect(visorVoid, Offset(centerX - headR * 0.7f, trapHeadY - headR * 0.35f), Size(headR * 1.4f, headR * 0.75f), CornerRadius(3f, 3f))

        // Frantic Stunned Spinning / Flashing Panic Eyes
        val spinAngle = (tickCount * 25f) % 360f
        val rad = spinAngle * (PI / 180f)
        val eyeOffset = 4f
        // Left eye dizzy star
        val lEyeCenter = Offset(centerX - headR * 0.35f, trapHeadY)
        drawLine(opticGlow, Offset(lEyeCenter.x - cos(rad).toFloat() * eyeOffset, lEyeCenter.y - sin(rad).toFloat() * eyeOffset), Offset(lEyeCenter.x + cos(rad).toFloat() * eyeOffset, lEyeCenter.y + sin(rad).toFloat() * eyeOffset), 1.8f)
        drawLine(opticGlow, Offset(lEyeCenter.x - sin(rad).toFloat() * eyeOffset, lEyeCenter.y + cos(rad).toFloat() * eyeOffset), Offset(lEyeCenter.x + sin(rad).toFloat() * eyeOffset, lEyeCenter.y - cos(rad).toFloat() * eyeOffset), 1.8f)
        drawCircle(opticCore, 1.2f, lEyeCenter)

        // Right eye dizzy star
        val rEyeCenter = Offset(centerX + headR * 0.35f, trapHeadY)
        drawLine(opticGlow, Offset(rEyeCenter.x - cos(rad).toFloat() * eyeOffset, rEyeCenter.y - sin(rad).toFloat() * eyeOffset), Offset(rEyeCenter.x + cos(rad).toFloat() * eyeOffset, rEyeCenter.y + sin(rad).toFloat() * eyeOffset), 1.8f)
        drawLine(opticGlow, Offset(rEyeCenter.x - sin(rad).toFloat() * eyeOffset, rEyeCenter.y + cos(rad).toFloat() * eyeOffset), Offset(rEyeCenter.x + sin(rad).toFloat() * eyeOffset, rEyeCenter.y - cos(rad).toFloat() * eyeOffset), 1.8f)
        drawCircle(opticCore, 1.2f, rEyeCenter)

        // Frantic clawing gauntlets scrabbling at the pit rims!
        val clawSwing = sin(tickCount * 0.5).toFloat() * 3f
        val leftClaw = Offset(left + cellSize * 0.08f, pitRimY + 3f + clawSwing)
        val rightClaw = Offset(left + cellSize * 0.92f, pitRimY + 3f - clawSwing)

        // Left armored arm & claw
        drawLine(cowlMain, Offset(centerX - headR * 0.8f, trapHeadY), leftClaw, cellSize * 0.08f, StrokeCap.Round)
        drawRoundRect(gauntletColor, Offset(leftClaw.x - 4f, leftClaw.y - 3f), Size(9f, 7f), CornerRadius(2f, 2f))
        // Claw fingers gripping ledge
        drawLine(Color(0xFFE0E0E0), Offset(leftClaw.x - 3f, leftClaw.y - 3f), Offset(leftClaw.x - 3f, leftClaw.y + 2f), 1.5f)
        drawLine(Color(0xFFE0E0E0), Offset(leftClaw.x + 1f, leftClaw.y - 3f), Offset(leftClaw.x + 1f, leftClaw.y + 2f), 1.5f)

        // Right armored arm & claw
        drawLine(cowlMain, Offset(centerX + headR * 0.8f, trapHeadY), rightClaw, cellSize * 0.08f, StrokeCap.Round)
        drawRoundRect(gauntletColor, Offset(rightClaw.x - 5f, rightClaw.y - 3f), Size(9f, 7f), CornerRadius(2f, 2f))
        // Claw fingers gripping ledge
        drawLine(Color(0xFFE0E0E0), Offset(rightClaw.x - 1f, rightClaw.y - 3f), Offset(rightClaw.x - 1f, rightClaw.y + 2f), 1.5f)
        drawLine(Color(0xFFE0E0E0), Offset(rightClaw.x + 3f, rightClaw.y - 3f), Offset(rightClaw.x + 3f, rightClaw.y + 2f), 1.5f)

        // Comic steam puff particles rising from the trap
        val steamY = trapHeadY - headR * 1.2f - ((tickCount * 2) % 15).toFloat()
        drawCircle(Color(0x90FFFFFF), 2.5f, Offset(centerX + sin(tickCount * 0.3).toFloat() * 4f, steamY))
        return
    }

    // ==========================================
    // 2. ACTIVE PATROLLING / CHASING BUNGELING GUARD
    // ==========================================
    val isRunning = enemy.state == EntityState.RUNNING
    val isClimbing = enemy.state == EntityState.CLIMBING
    val isHanging = enemy.state == EntityState.HANGING

    // Stride calculations
    val stridePhase = if (isRunning) enemy.animFrame % 4 else 0
    val strideProgress = when (stridePhase) {
        0 -> 1.0f
        1 -> 0.3f
        2 -> -1.0f
        else -> -0.3f
    }
    val marchStride = if (isRunning) dirSign * strideProgress * cellSize * 0.22f else 0f
    val marchBob = if (isRunning && stridePhase % 2 == 1) -cellSize * 0.02f else 0f
    val effectiveTop = charTop + marchBob

    // A. Head & Crimson Cowl
    val headR = cellSize * 0.15f
    val headCenter = Offset(centerX, effectiveTop + cellSize * 0.18f)

    // Deep crimson cowl hood with pointed peak
    val cowlPath = Path().apply {
        moveTo(headCenter.x - headR * 1.2f, headCenter.y + headR * 0.9f)
        lineTo(headCenter.x - headR * 1.1f, headCenter.y - headR * 0.4f)
        lineTo(headCenter.x, headCenter.y - headR * 1.35f) // Pointed hood peak
        lineTo(headCenter.x + headR * 1.1f, headCenter.y - headR * 0.4f)
        lineTo(headCenter.x + headR * 1.2f, headCenter.y + headR * 0.9f)
        close()
    }
    drawPath(cowlPath, Brush.radialGradient(listOf(cowlHighlight, cowlMain, cowlShadow), center = headCenter, radius = headR * 1.4f))

    // Shadowed Face Visor Void
    val visorW = headR * 1.4f
    val visorH = headR * 0.75f
    drawRoundRect(visorVoid, Offset(headCenter.x - visorW / 2f + dirSign * 1.5f, headCenter.y - visorH / 2f), Size(visorW, visorH), CornerRadius(3f, 3f))

    // Menacing Cybernetic Optic Visor (Pulsing glowing amber/red with inner laser core)
    val pulseAlpha = (sin(tickCount * 0.22) * 0.2 + 0.8).toFloat()
    val opticY = headCenter.y - 1.5f
    val opticX1 = headCenter.x - headR * 0.38f + dirSign * 2f
    val opticX2 = headCenter.x + headR * 0.12f + dirSign * 2f
    val opticW = headR * 0.32f

    // Slanted aggressive eye slits
    fun DrawScope.drawSlantedEye(x: Float) {
        val eyePath = Path().apply {
            moveTo(x, opticY - 1f)
            lineTo(x + opticW, opticY + dirSign * 1f)
            lineTo(x + opticW - 1f, opticY + 3.5f + dirSign * 1f)
            lineTo(x - 1f, opticY + 2.5f)
            close()
        }
        drawPath(eyePath, opticGlow.copy(alpha = pulseAlpha))
        // Inner white-hot laser core
        drawLine(opticCore, Offset(x + 1f, opticY + 1f), Offset(x + opticW - 1f, opticY + 1.8f), 1.2f)
    }
    drawSlantedEye(opticX1)
    drawSlantedEye(opticX2)

    // Cybernetic respirator / mouth grille
    val grilleY = headCenter.y + headR * 0.42f
    drawLine(Color(0xFF404856), Offset(headCenter.x - 4f, grilleY), Offset(headCenter.x + 4f, grilleY), 1.5f)
    drawLine(Color(0xFF404856), Offset(headCenter.x - 3f, grilleY + 2.5f), Offset(headCenter.x + 3f, grilleY + 2.5f), 1.2f)

    // B. Torso & Armored Cuirass with Heavy Pauldrons
    val torsoTop = headCenter.y + headR * 0.80f
    val torsoH = cellSize * 0.28f
    val torsoBottom = torsoTop + torsoH
    val torsoW = cellSize * 0.27f
    val torsoX = centerX - torsoW / 2f

    // Crimson Combat Tunic Underlayer
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(cowlMain, cowlShadow)),
        topLeft = Offset(torsoX, torsoTop),
        size = Size(torsoW, torsoH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Steel Armored Breastplate
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(armorHighlight, armorSteel)),
        topLeft = Offset(torsoX + 2f, torsoTop + 2f),
        size = Size(torsoW - 4f, torsoH * 0.65f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Heavy Flared Shoulder Pauldrons (Left & Right)
    val pauldronW = cellSize * 0.13f
    val pauldronH = cellSize * 0.12f
    // Left pauldron
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(cowlHighlight, cowlShadow)),
        topLeft = Offset(torsoX - pauldronW * 0.5f, torsoTop - 1f),
        size = Size(pauldronW, pauldronH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawLine(Color(0xFFE0A0A0), Offset(torsoX - pauldronW * 0.5f, torsoTop - 1f), Offset(torsoX + pauldronW * 0.5f, torsoTop - 1f), 1f)

    // Right pauldron
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(cowlHighlight, cowlShadow)),
        topLeft = Offset(torsoX + torsoW - pauldronW * 0.5f, torsoTop - 1f),
        size = Size(pauldronW, pauldronH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawLine(Color(0xFFE0A0A0), Offset(torsoX + torsoW - pauldronW * 0.5f, torsoTop - 1f), Offset(torsoX + torsoW + pauldronW * 0.5f, torsoTop - 1f), 1f)

    // Combat Utility Belt with power canisters
    val beltY = torsoBottom - cellSize * 0.05f
    val beltH = cellSize * 0.05f
    drawRect(Color(0xFF181C24), Offset(torsoX, beltY), Size(torsoW, beltH))
    drawRect(palette.guardBody, Offset(centerX - 3f, beltY), Size(6f, beltH))
    drawCircle(Color(0xFF88A0C0), 1.8f, Offset(torsoX + 4f, beltY + beltH / 2f))
    drawCircle(Color(0xFF88A0C0), 1.8f, Offset(torsoX + torsoW - 4f, beltY + beltH / 2f))

    // C. Armored Legs & Heavy Greaves
    val legW = cellSize * 0.09f
    if (isClimbing) {
        val climbStep = (enemy.animFrame % 2 == 0)
        val leftFootY = if (climbStep) charBottom - cellSize * 0.06f else charBottom - cellSize * 0.18f
        val rightFootY = if (climbStep) charBottom - cellSize * 0.18f else charBottom - cellSize * 0.06f
        drawLine(cowlShadow, Offset(centerX - cellSize * 0.07f, torsoBottom), Offset(centerX - cellSize * 0.14f, leftFootY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX - cellSize * 0.18f, leftFootY - 3f), Size(cellSize * 0.16f, cellSize * 0.09f), CornerRadius(2f, 2f))
        drawLine(cowlMain, Offset(centerX + cellSize * 0.07f, torsoBottom), Offset(centerX + cellSize * 0.14f, rightFootY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(centerX + cellSize * 0.06f, rightFootY - 3f), Size(cellSize * 0.16f, cellSize * 0.09f), CornerRadius(2f, 2f))
    } else {
        val frontFootX = centerX + marchStride
        val rearFootX = centerX - marchStride
        val frontFootY = charBottom - (if (marchStride * dirSign < 0) cellSize * 0.05f else 0f)
        val rearFootY = charBottom - (if (-marchStride * dirSign < 0) cellSize * 0.05f else 0f)

        // Rear leg
        drawLine(cowlShadow, Offset(centerX, torsoBottom), Offset(rearFootX, rearFootY), legW, StrokeCap.Round)
        drawRoundRect(bootColor, Offset(rearFootX - cellSize * 0.06f + dirSign * cellSize * 0.03f, rearFootY - cellSize * 0.07f), Size(cellSize * 0.18f, cellSize * 0.08f), CornerRadius(2f, 2f))

        // Front leg
        drawLine(cowlMain, Offset(centerX, torsoBottom), Offset(frontFootX, frontFootY), legW, StrokeCap.Round)
        // Knee armor plate
        drawCircle(armorSteel, 2.5f, Offset((centerX + frontFootX) / 2f, (torsoBottom + frontFootY) / 2f))
        drawRoundRect(bootColor, Offset(frontFootX - cellSize * 0.06f + dirSign * cellSize * 0.03f, frontFootY - cellSize * 0.07f), Size(cellSize * 0.18f, cellSize * 0.08f), CornerRadius(2f, 2f))
    }

    // D. Armored Gauntlets
    val armW = cellSize * 0.075f
    if (isClimbing) {
        val climbStep = (enemy.animFrame % 2 == 0)
        val leftHandY = if (climbStep) effectiveTop + cellSize * 0.06f else effectiveTop + cellSize * 0.28f
        val rightHandY = if (climbStep) effectiveTop + cellSize * 0.28f else effectiveTop + cellSize * 0.06f
        drawLine(cowlShadow, Offset(torsoX + 2f, torsoTop + 4f), Offset(centerX - cellSize * 0.24f, leftHandY), armW, StrokeCap.Round)
        drawRoundRect(gauntletColor, Offset(centerX - cellSize * 0.24f - 4f, leftHandY - 3f), Size(8f, 7f), CornerRadius(2f, 2f))
        drawLine(cowlMain, Offset(torsoX + torsoW - 2f, torsoTop + 4f), Offset(centerX + cellSize * 0.24f, rightHandY), armW, StrokeCap.Round)
        drawRoundRect(gauntletColor, Offset(centerX + cellSize * 0.24f - 4f, rightHandY - 3f), Size(8f, 7f), CornerRadius(2f, 2f))
    } else {
        val armSwing = if (isRunning) -marchStride * 0.8f else dirSign * cellSize * 0.10f
        // Rear arm
        val rHandX = centerX - armSwing
        val rHandY = torsoTop + cellSize * 0.18f
        drawLine(cowlShadow, Offset(centerX - dirSign * torsoW * 0.35f, torsoTop + 4f), Offset(rHandX, rHandY), armW, StrokeCap.Round)
        drawCircle(gauntletColor, cellSize * 0.045f, Offset(rHandX, rHandY))

        // Front arm reaching aggressively forward
        val fHandX = centerX + armSwing + dirSign * cellSize * 0.06f
        val fHandY = torsoTop + cellSize * 0.18f
        drawLine(cowlMain, Offset(centerX + dirSign * torsoW * 0.35f, torsoTop + 4f), Offset(fHandX, fHandY), armW, StrokeCap.Round)
        drawCircle(gauntletColor, cellSize * 0.045f, Offset(fHandX, fHandY))
        // Gauntlet spike / knuckle reinforcement
        drawCircle(Color(0xFFE0E0E0), 1.5f, Offset(fHandX + dirSign * 2f, fHandY))
    }

    // E. Stolen Treasure Chest Strapped on Back!
    if (enemy.hasGold) {
        val chestW = cellSize * 0.46f
        val chestH = cellSize * 0.36f
        val chestX = centerX - dirSign * cellSize * 0.28f - chestW / 2f
        val chestY = torsoTop + cellSize * 0.02f

        // Drop shadow on back
        drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(chestX + 1f, chestY + 2f), Size(chestW, chestH), CornerRadius(3f, 3f))

        // 3D Chest Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFA55A20), Color(0xFF8B4513), Color(0xFF5A2A08))),
            topLeft = Offset(chestX, chestY),
            size = Size(chestW, chestH),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Golden reinforcement bands
        val bW = chestW * 0.14f
        drawRect(palette.gold, Offset(chestX + chestW * 0.2f, chestY), Size(bW, chestH))
        drawRect(palette.gold, Offset(chestX + chestW * 0.8f - bW, chestY), Size(bW, chestH))
        drawCircle(palette.goldHighlight, 1.8f, Offset(chestX + chestW / 2f, chestY + chestH * 0.5f))

        // Leather harness straps securing chest to guard's torso
        drawLine(Color(0xFF382010), Offset(chestX + chestW / 2f, chestY + 2f), Offset(centerX, torsoTop + 4f), 2f)
        drawLine(Color(0xFF382010), Offset(chestX + chestW / 2f, chestY + chestH - 2f), Offset(centerX, beltY), 2f)

        // Sparkling golden glint
        val glintPhase = ((tickCount * 4) % 60).toInt()
        if (glintPhase in 0..10) {
            val glintX = chestX + chestW * 0.75f
            val glintY = chestY + chestH * 0.25f
            drawLine(Color.White, Offset(glintX - 3f, glintY), Offset(glintX + 3f, glintY), 1.2f)
            drawLine(Color.White, Offset(glintX, glintY - 3f), Offset(glintX, glintY + 3f), 1.2f)
        }
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

private fun DrawScope.drawMiniMap(
    gameState: GameState,
    palette: GamePalette,
    camX: Float,
    camY: Float,
    visCols: Float,
    visRows: Float
) {
    val mapW = 100f
    val mapH = (mapW * (LevelData.ROWS.toFloat() / LevelData.COLS.toFloat())) // ~57f
    val mapMargin = 10f
    val mapLeft = size.width - mapW - mapMargin
    val mapTop = mapMargin

    // Semi-transparent radar background
    drawRoundRect(
        color = Color(0xDD0A0E18),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapW, mapH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = palette.buttonBorder.copy(alpha = 0.5f),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapW, mapH),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1f)
    )

    val scaleX = mapW / LevelData.COLS
    val scaleY = mapH / LevelData.ROWS

    // Draw gold nuggets as golden dots
    for (r in 0 until LevelData.ROWS) {
        for (c in 0 until LevelData.COLS) {
            if (gameState.activeGrid[r][c] == TileType.GOLD.id) {
                drawCircle(palette.gold, 1.4f, Offset(mapLeft + (c + 0.5f) * scaleX, mapTop + (r + 0.5f) * scaleY))
            }
        }
    }

    // Draw enemies as red dots
    for (enemy in gameState.enemies) {
        if (!enemy.isDead) {
            drawCircle(palette.guardBody, 2f, Offset(mapLeft + (enemy.x + 0.5f) * scaleX, mapTop + (enemy.y + 0.5f) * scaleY))
        }
    }

    // Draw runner as bright white dot
    drawCircle(Color.White, 2.5f, Offset(mapLeft + (gameState.runner.x + 0.5f) * scaleX, mapTop + (gameState.runner.y + 0.5f) * scaleY))

    // Draw current camera viewport box
    val boxLeft = mapLeft + (camX - visCols / 2f) * scaleX
    val boxTop = mapTop + (camY - visRows / 2f) * scaleY
    val boxW = visCols * scaleX
    val boxH = visRows * scaleY
    drawRect(
        color = palette.escapeLadder.copy(alpha = 0.8f),
        topLeft = Offset(boxLeft.coerceIn(mapLeft, mapLeft + mapW), boxTop.coerceIn(mapTop, mapTop + mapH)),
        size = Size(boxW.coerceAtMost(mapW), boxH.coerceAtMost(mapH)),
        style = Stroke(width = 1.2f)
    )
}

