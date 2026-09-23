package com.example.loderunner.game.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.loderunner.game.model.LevelData
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import com.example.loderunner.game.model.GameStatus
import com.example.loderunner.game.ui.theme.GamePalette

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
    var frameTick by remember { mutableLongStateOf(0L) }
    val highScore = remember(currentLevelIdx) { levelManager.getHighScore() }
    val focusRequester = remember { FocusRequester() }

    val engine = remember {
        GameEngine()
    }

    // Immersive mode while playing: hide status/nav bars to give the playfield the full screen height.
    // Bars can be revealed temporarily with an edge swipe; restored when leaving the game.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Initialize level on start or level change
    LaunchedEffect(currentLevelIdx, isCustom) {
        val levelData = if (isCustom) {
            levelManager.getCustomLevel(currentLevelIdx) ?: levelManager.getClassicLevel(1)
        } else {
            levelManager.getClassicLevel(currentLevelIdx)
        }
        engine.startLevel(levelData, preserveScoreAndLives = currentLevelIdx > 1 && !isCustom)
        frameTick++
    }

    // Fixed-timestep 60 Hz game loop, independent of display refresh rate (e.g. 120 Hz Pixel Pro).
    // frameTick advances every step in every game status, so pause/death/clear overlays still recompose.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val stepNanos = 1_000_000_000L / 60
        var lastFrameNanos = 0L
        var accumulator = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    // Cap catch-up after long stalls (backgrounding, GC) to avoid a burst of ticks
                    accumulator += (now - lastFrameNanos).coerceAtMost(stepNanos * 5)
                }
                lastFrameNanos = now
                while (accumulator >= stepNanos) {
                    engine.tick()
                    frameTick++
                    accumulator -= stepNanos
                }
            }
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
        // Keep HUD and controls clear of status/nav bars and the camera cutout (app is edge-to-edge)
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // HUD at top
            GameHud(
                gameState = state,
                highScore = highScore,
                palette = palette,
                onPauseClick = { engine.togglePause() },
                onRestartClick = { engine.restartCurrentLevel() },
                onExitClick = onBackToMenu
            )

            // Center game row: the playfield takes as much of the row as its 28:16 aspect allows,
            // keeping just enough side bezel for thumb-sized controls. Controls scale to the space left.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val minSideWidth = (maxWidth * 0.16f).coerceAtLeast(140.dp)
                val playfieldAspect = LevelData.COLS.toFloat() / LevelData.ROWS
                val canvasWidth = minOf(maxWidth - minSideWidth * 2, maxHeight * playfieldAspect)
                val sideWidth = (maxWidth - canvasWidth) / 2
                // D-pad is ~3 buttons wide plus padding; dig buttons stack vertically when narrow
                val dpadButtonSize = ((sideWidth - 16.dp) / 3).coerceIn(40.dp, 64.dp)
                val digButtonSize = (sideWidth - 20.dp).coerceIn(56.dp, 76.dp)
                val stackDigButtons = sideWidth < digButtonSize * 2 + 40.dp

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Bezel: D-Pad
                    Box(
                        modifier = Modifier
                            .width(sideWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Opt the D-pad out of the edge back-swipe gesture so presses aren't stolen
                        TabletDpad(
                            onDirectionChange = { dir -> engine.setInputDirection(dir) },
                            palette = palette,
                            buttonSize = dpadButtonSize,
                            modifier = Modifier.systemGestureExclusion()
                        )
                    }

                    // Center: Retro Canvas (28x16 grid)
                    Box(
                        modifier = Modifier
                            .width(canvasWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        GameCanvas(
                            gameState = state,
                            palette = palette,
                            tickCount = frameTick,
                            showCrtScanlines = showCrtScanlines
                        )
                    }

                    // Right Bezel: Dig L & Dig R Buttons
                    Box(
                        modifier = Modifier
                            .width(sideWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        TabletActionButtons(
                            onDigLeft = { engine.triggerDig(Direction.LEFT) },
                            onDigRight = { engine.triggerDig(Direction.RIGHT) },
                            palette = palette,
                            buttonSize = digButtonSize,
                            stacked = stackDigButtons,
                            modifier = Modifier.systemGestureExclusion()
                        )
                    }
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
