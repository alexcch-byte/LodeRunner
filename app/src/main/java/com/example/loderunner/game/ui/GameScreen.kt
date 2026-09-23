package com.example.loderunner.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.example.loderunner.game.core.GameEngine
import com.example.loderunner.game.levels.LevelManager
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.model.GameStatus
import com.example.loderunner.game.ui.theme.GamePalette
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    levelIndex: Int,
    isCustom: Boolean,
    levelManager: LevelManager,
    palette: GamePalette,
    showCrtScanlines: Boolean,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevelIdx by remember { mutableStateOf(levelIndex) }
    val highScore = remember(currentLevelIdx) { levelManager.getHighScore() }
    val focusRequester = remember { FocusRequester() }

    val engine = remember {
        GameEngine()
    }

    // Initialize level on start or level change
    LaunchedEffect(currentLevelIdx, isCustom) {
        val levelData = if (isCustom) {
            levelManager.getCustomLevel(currentLevelIdx) ?: levelManager.getClassicLevel(1)
        } else {
            levelManager.getClassicLevel(currentLevelIdx)
        }
        engine.startLevel(levelData, preserveScoreAndLives = currentLevelIdx > 1 && !isCustom)
    }

    // 60 FPS Game Loop
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            engine.tick()
            delay(16L) // ~60 FPS
        }
    }

    // Update high score whenever score changes
    LaunchedEffect(engine.state.score) {
        levelManager.saveHighScore(engine.state.score)
    }

    val state = engine.state

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bezelBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft, Key.A -> {
                            engine.setInputDirection(Direction.LEFT)
                            true
                        }
                        Key.DirectionRight, Key.D -> {
                            engine.setInputDirection(Direction.RIGHT)
                            true
                        }
                        Key.DirectionUp, Key.W -> {
                            engine.setInputDirection(Direction.UP)
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            engine.setInputDirection(Direction.DOWN)
                            true
                        }
                        Key.Z, Key.Q, Key.J -> {
                            engine.triggerDig(Direction.LEFT)
                            true
                        }
                        Key.X, Key.E, Key.K -> {
                            engine.triggerDig(Direction.RIGHT)
                            true
                        }
                        Key.Spacebar, Key.P -> {
                            engine.togglePause()
                            true
                        }
                        Key.R -> {
                            engine.restartCurrentLevel()
                            true
                        }
                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.DirectionLeft, Key.A,
                        Key.DirectionRight, Key.D,
                        Key.DirectionUp, Key.W,
                        Key.DirectionDown, Key.S -> {
                            engine.setInputDirection(Direction.NONE)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HUD at top
            GameHud(
                gameState = state,
                highScore = highScore,
                palette = palette,
                onPauseClick = { engine.togglePause() },
                onRestartClick = { engine.restartCurrentLevel() },
                onExitClick = onBackToMenu
            )

            // Center game row: Left Dpad, Center Canvas, Right Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Bezel: D-Pad
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TabletDpad(
                        onDirectionChange = { dir -> engine.setInputDirection(dir) },
                        palette = palette
                    )
                }

                // Center: Retro Canvas (28x16 grid)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    GameCanvas(
                        gameState = state,
                        palette = palette,
                        showCrtScanlines = showCrtScanlines
                    )
                }

                // Right Bezel: Dig L & Dig R Buttons
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TabletActionButtons(
                        onDigLeft = { engine.triggerDig(Direction.LEFT) },
                        onDigRight = { engine.triggerDig(Direction.RIGHT) },
                        palette = palette
                    )
                }
            }
        }

        // Overlay states
        if (state.status == GameStatus.PAUSED) {
            PauseOverlay(
                palette = palette,
                onResume = { engine.togglePause() },
                onRestart = { engine.restartCurrentLevel() },
                onMainMenu = onBackToMenu
            )
        }

        if (state.status == GameStatus.LEVEL_CLEARED) {
            LevelClearedOverlay(
                levelNumber = state.levelNumber,
                score = state.score,
                palette = palette,
                onNextLevel = {
                    val maxClassic = levelManager.getClassicLevelCount()
                    if (currentLevelIdx < maxClassic && !isCustom) {
                        currentLevelIdx++
                    } else {
                        onBackToMenu()
                    }
                }
            )
        }

        if (state.status == GameStatus.GAME_OVER) {
            GameOverOverlay(
                score = state.score,
                highScore = highScore,
                palette = palette,
                onRestart = {
                    val lvl = if (isCustom) {
                        levelManager.getCustomLevel(currentLevelIdx) ?: levelManager.getClassicLevel(1)
                    } else {
                        levelManager.getClassicLevel(currentLevelIdx)
                    }
                    engine.startLevel(lvl, preserveScoreAndLives = false)
                },
                onMainMenu = onBackToMenu
            )
        }
    }
}
