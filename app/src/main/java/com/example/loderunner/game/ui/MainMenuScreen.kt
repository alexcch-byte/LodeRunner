package com.example.loderunner.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.levels.LevelManager
import com.example.loderunner.game.ui.theme.GamePalette

@Composable
fun MainMenuScreen(
    levelManager: LevelManager,
    palette: GamePalette,
    onStartGame: (Int, Boolean) -> Unit,
    onOpenEditor: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowToPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLevelSelect by remember { mutableStateOf(false) }
    var showCustomSelect by remember { mutableStateOf(false) }
    val highScore = remember { levelManager.getHighScore() }
    val customLevels = remember { levelManager.getCustomLevels() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bezelBackground),
        contentAlignment = Alignment.Center
    ) {
        if (showLevelSelect) {
            LevelSelectModal(
                levelCount = levelManager.getClassicLevelCount(),
                palette = palette,
                onSelectLevel = { lvl ->
                    showLevelSelect = false
                    onStartGame(lvl, false)
                },
                onClose = { showLevelSelect = false }
            )
        } else if (showCustomSelect) {
            CustomLevelSelectModal(
                customLevels = customLevels,
                palette = palette,
                onSelectLevel = { idx ->
                    showCustomSelect = false
                    onStartGame(idx, true)
                },
                onEditLevel = { idx ->
                    showCustomSelect = false
                    onOpenEditor(idx)
                },
                onCreateNew = {
                    showCustomSelect = false
                    onOpenEditor(-1)
                },
                onClose = { showCustomSelect = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.background)
                    .border(3.dp, palette.buttonBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Banner
                Text(
                    text = "★ LODE RUNNER ★",
                    color = palette.gold,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "1980s PC RETRO TABLET EDITION",
                    color = palette.hudText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "HIGH SCORE: ${String.format("%06d", highScore)}",
                    color = palette.goldHighlight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Menu Buttons
                MenuButton(
                    label = "START GAME (LEVEL 1)",
                    color = palette.escapeLadder,
                    palette = palette,
                    onClick = { onStartGame(1, false) }
                )

                MenuButton(
                    label = "SELECT LEVEL",
                    color = palette.buttonBorder,
                    palette = palette,
                    onClick = { showLevelSelect = true }
                )

                MenuButton(
                    label = "CUSTOM LEVELS & EDITOR",
                    color = palette.gold,
                    palette = palette,
                    onClick = { showCustomSelect = true }
                )

                MenuButton(
                    label = "HOW TO PLAY",
                    color = Color.LightGray,
                    palette = palette,
                    onClick = onOpenHowToPlay
                )

                MenuButton(
                    label = "SETTINGS & THEMES",
                    color = Color.Cyan,
                    palette = palette,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun LevelSelectModal(
    levelCount: Int,
    palette: GamePalette,
    onSelectLevel: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.background)
            .border(2.dp, palette.buttonBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SELECT STAGE",
            color = palette.hudText,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (lvl in 1..levelCount) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.buttonBackground)
                        .border(2.dp, palette.buttonBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectLevel(lvl) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LVL $lvl",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.DarkGray)
                .clickable { onClose() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "CANCEL", color = Color.White, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CustomLevelSelectModal(
    customLevels: List<com.example.loderunner.game.model.LevelData>,
    palette: GamePalette,
    onSelectLevel: (Int) -> Unit,
    onEditLevel: (Int) -> Unit,
    onCreateNew: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.background)
            .border(2.dp, palette.gold, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CUSTOM LEVELS",
            color = palette.gold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )

        if (customLevels.isEmpty()) {
            Text(
                text = "No custom levels found. Create one in the designer!",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                customLevels.forEachIndexed { idx, lvl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.buttonBackground)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lvl.name, color = Color.White, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PLAY ▶",
                                color = palette.escapeLadder,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable { onSelectLevel(idx) }
                            )
                            Text(
                                text = "EDIT ✎",
                                color = palette.gold,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable { onEditLevel(idx) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(label = "+ NEW LEVEL", color = palette.gold, palette = palette, onClick = onCreateNew)
            MenuButton(label = "BACK", color = Color.Gray, palette = palette, onClick = onClose)
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    color: Color,
    palette: GamePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.buttonBackground)
            .border(2.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
