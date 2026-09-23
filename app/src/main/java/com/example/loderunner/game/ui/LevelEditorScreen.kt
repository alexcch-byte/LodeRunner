package com.example.loderunner.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.levels.LevelManager
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.TileType
import com.example.loderunner.game.ui.theme.GamePalette

enum class EditorBrush(val label: String, val charSymbol: Char, val previewColor: Color) {
    BRICK("Brick", '#', Color(0xFFC04818)),
    SOLID_ROCK("Rock", '@', Color(0xFF303888)),
    LADDER("Ladder", 'H', Color(0xFFEEEEEE)),
    ROPE("Rope", '-', Color(0xFF38D8D8)),
    GOLD("Gold", 'G', Color(0xFFFFD700)),
    RUNNER("Runner", '&', Color(0xFFFFFFFF)),
    ENEMY("Guard", 'M', Color(0xFFE02020)),
    ESCAPE_LADDER("Exit Lad", 'E', Color(0xFF40FF40)),
    ERASER("Eraser", ' ', Color(0xFF222222))
}

@Composable
fun LevelEditorScreen(
    levelIndex: Int,
    levelManager: LevelManager,
    palette: GamePalette,
    onPlaytestLevel: (LevelData) -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialLevel = remember(levelIndex) {
        if (levelIndex >= 0) {
            levelManager.getCustomLevel(levelIndex)
        } else null
    }

    var levelName by remember { mutableStateOf(initialLevel?.name ?: "Custom Level") }
    var selectedBrush by remember { mutableStateOf(EditorBrush.BRICK) }

    // Grid state
    val grid = remember {
        val g = Array(LevelData.ROWS) { r ->
            IntArray(LevelData.COLS) { c ->
                initialLevel?.grid?.get(r)?.get(c) ?: if (r == LevelData.ROWS - 1) TileType.SOLID_ROCK.id else TileType.EMPTY.id
            }
        }
        mutableStateOf(g)
    }

    var runnerX by remember { mutableIntStateOf(initialLevel?.runnerStartX ?: 14) }
    var runnerY by remember { mutableIntStateOf(initialLevel?.runnerStartY ?: 14) }
    val enemySpawns = remember {
        val spawns = initialLevel?.enemySpawns?.toMutableList() ?: mutableListOf(Pair(5, 12), Pair(22, 12))
        mutableStateOf(spawns)
    }

    var feedbackMsg by remember { mutableStateOf("") }

    fun paintCell(c: Int, r: Int) {
        if (c !in 0 until LevelData.COLS || r !in 0 until LevelData.ROWS) return

        when (selectedBrush) {
            EditorBrush.RUNNER -> {
                runnerX = c
                runnerY = r
                grid.value[r][c] = TileType.EMPTY.id
            }
            EditorBrush.ENEMY -> {
                val existing = enemySpawns.value.indexOfFirst { it.first == c && it.second == r }
                if (existing < 0) {
                    enemySpawns.value = (enemySpawns.value + Pair(c, r)).toMutableList()
                    grid.value[r][c] = TileType.EMPTY.id
                }
            }
            EditorBrush.ERASER -> {
                grid.value[r][c] = TileType.EMPTY.id
                enemySpawns.value = enemySpawns.value.filterNot { it.first == c && it.second == r }.toMutableList()
            }
            else -> {
                val tile = TileType.fromChar(selectedBrush.charSymbol)
                grid.value[r][c] = tile.id
                enemySpawns.value = enemySpawns.value.filterNot { it.first == c && it.second == r }.toMutableList()
            }
        }
        // Force recomposition
        grid.value = Array(LevelData.ROWS) { row -> grid.value[row].copyOf() }
    }

    fun buildCurrentLevelData(): LevelData {
        return LevelData(
            id = initialLevel?.id ?: 1000,
            name = levelName.trim().ifEmpty { "Custom Level" },
            grid = grid.value,
            runnerStartX = runnerX,
            runnerStartY = runnerY,
            enemySpawns = enemySpawns.value.toList()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bezelBackground)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorButton(label = "◀ MENU", palette = palette, onClick = onBackToMenu)
                Text(
                    text = "LEVEL DESIGNER",
                    color = palette.buttonBorder,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            OutlinedTextField(
                value = levelName,
                onValueChange = { levelName = it },
                singleLine = true,
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = palette.buttonBorder,
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorButton(
                    label = "CLEAR",
                    palette = palette,
                    color = Color.Red,
                    onClick = {
                        grid.value = Array(LevelData.ROWS) { r ->
                            IntArray(LevelData.COLS) { c ->
                                if (r == LevelData.ROWS - 1) TileType.SOLID_ROCK.id else TileType.EMPTY.id
                            }
                        }
                        enemySpawns.value = mutableListOf()
                        feedbackMsg = "Grid cleared"
                    }
                )

                EditorButton(
                    label = "SAVE",
                    palette = palette,
                    color = palette.escapeLadder,
                    onClick = {
                        val lvl = buildCurrentLevelData()
                        levelManager.saveCustomLevel(lvl)
                        feedbackMsg = "Level '${lvl.name}' Saved!"
                    }
                )

                EditorButton(
                    label = "PLAYTEST ▶",
                    palette = palette,
                    color = palette.gold,
                    onClick = {
                        val lvl = buildCurrentLevelData()
                        onPlaytestLevel(lvl)
                    }
                )
            }
        }

        if (feedbackMsg.isNotEmpty()) {
            Text(
                text = feedbackMsg,
                color = palette.goldHighlight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Center: Palette on Left, Canvas in Center
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Brush Palette Column
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorBrush.entries.forEach { brush ->
                    val isSelected = selectedBrush == brush
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) brush.previewColor.copy(alpha = 0.35f) else palette.buttonBackground)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) brush.previewColor else palette.buttonBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedBrush = brush }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(brush.previewColor)
                                    .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = brush.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Editor Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedBrush) {
                            detectTapGestures { offset ->
                                val scaleX = size.width / LevelData.COLS
                                val scaleY = size.height / LevelData.ROWS
                                val cellSize = minOf(scaleX, scaleY)
                                val playW = cellSize * LevelData.COLS
                                val playH = cellSize * LevelData.ROWS
                                val ox = (size.width - playW) / 2f
                                val oy = (size.height - playH) / 2f

                                val c = ((offset.x - ox) / cellSize).toInt()
                                val r = ((offset.y - oy) / cellSize).toInt()
                                paintCell(c, r)
                            }
                        }
                        .pointerInput(selectedBrush) {
                            detectDragGestures { change, _ ->
                                val scaleX = size.width / LevelData.COLS
                                val scaleY = size.height / LevelData.ROWS
                                val cellSize = minOf(scaleX, scaleY)
                                val playW = cellSize * LevelData.COLS
                                val playH = cellSize * LevelData.ROWS
                                val ox = (size.width - playW) / 2f
                                val oy = (size.height - playH) / 2f

                                val c = ((change.position.x - ox) / cellSize).toInt()
                                val r = ((change.position.y - oy) / cellSize).toInt()
                                paintCell(c, r)
                            }
                        }
                ) {
                    val scaleX = size.width / LevelData.COLS
                    val scaleY = size.height / LevelData.ROWS
                    val cellSize = minOf(scaleX, scaleY)
                    val playW = cellSize * LevelData.COLS
                    val playH = cellSize * LevelData.ROWS
                    val ox = (size.width - playW) / 2f
                    val oy = (size.height - playH) / 2f

                    // Background
                    drawRect(palette.background, Offset(ox, oy), Size(playW, playH))

                    // Draw Grid Tiles
                    for (r in 0 until LevelData.ROWS) {
                        for (c in 0 until LevelData.COLS) {
                            val tileId = grid.value[r][c]
                            val tile = TileType.fromId(tileId)
                            val tLeft = ox + c * cellSize
                            val tTop = oy + r * cellSize

                            val color = when (tile) {
                                TileType.BRICK -> palette.brickMain
                                TileType.SOLID_ROCK -> palette.solidRockMain
                                TileType.LADDER -> palette.ladder
                                TileType.ROPE -> palette.rope
                                TileType.GOLD -> palette.gold
                                TileType.ESCAPE_LADDER -> palette.escapeLadder
                                else -> Color.Transparent
                            }

                            if (color != Color.Transparent) {
                                drawRect(color, Offset(tLeft, tTop), Size(cellSize, cellSize))
                            }

                            // Subtle grid line
                            drawRect(
                                color = Color.DarkGray.copy(alpha = 0.35f),
                                topLeft = Offset(tLeft, tTop),
                                size = Size(cellSize, cellSize),
                                style = Stroke(width = 0.5f)
                            )
                        }
                    }

                    // Draw Runner start
                    val runnerLeft = ox + runnerX * cellSize
                    val runnerTop = oy + runnerY * cellSize
                    drawCircle(palette.runnerBody, cellSize * 0.35f, Offset(runnerLeft + cellSize / 2f, runnerTop + cellSize / 2f))

                    // Draw Enemy spawns
                    for (sp in enemySpawns.value) {
                        val spLeft = ox + sp.first * cellSize
                        val spTop = oy + sp.second * cellSize
                        drawCircle(palette.guardBody, cellSize * 0.35f, Offset(spLeft + cellSize / 2f, spTop + cellSize / 2f))
                    }

                    // Playfield border
                    drawRect(
                        color = palette.buttonBorder,
                        topLeft = Offset(ox, oy),
                        size = Size(playW, playH),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorButton(
    label: String,
    palette: GamePalette,
    color: Color = palette.buttonText,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.buttonBackground)
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
