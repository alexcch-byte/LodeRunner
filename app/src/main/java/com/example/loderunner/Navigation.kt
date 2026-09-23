package com.example.loderunner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.loderunner.game.levels.LevelManager
import com.example.loderunner.game.ui.GameScreen
import com.example.loderunner.game.ui.HowToPlayScreen
import com.example.loderunner.game.ui.LevelEditorScreen
import com.example.loderunner.game.ui.MainMenuScreen
import com.example.loderunner.game.ui.SettingsScreen
import com.example.loderunner.game.ui.theme.GamePalette
import com.example.loderunner.game.ui.theme.PaletteType

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val levelManager = remember { LevelManager(context) }
    val backStack = rememberNavBackStack(MainMenuKey)

    var currentPaletteType by remember { mutableStateOf(PaletteType.APPLE_II) }
    var showCrtScanlines by remember { mutableStateOf(true) }
    var isSoundEnabled by remember { mutableStateOf(true) }

    val palette = remember(currentPaletteType) { GamePalette.forType(currentPaletteType) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MainMenuKey> {
                MainMenuScreen(
                    levelManager = levelManager,
                    palette = palette,
                    onStartGame = { levelIdx, isCustom ->
                        backStack.add(GameKey(levelIndex = levelIdx, isCustom = isCustom))
                    },
                    onOpenEditor = { levelIdx ->
                        backStack.add(LevelEditorKey(levelIndex = levelIdx))
                    },
                    onOpenSettings = {
                        backStack.add(SettingsKey)
                    },
                    onOpenHowToPlay = {
                        backStack.add(HowToPlayKey)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<GameKey> { key ->
                GameScreen(
                    levelIndex = key.levelIndex,
                    isCustom = key.isCustom,
                    levelManager = levelManager,
                    palette = palette,
                    showCrtScanlines = showCrtScanlines,
                    onBackToMenu = {
                        backStack.removeLastOrNull()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<LevelEditorKey> { key ->
                LevelEditorScreen(
                    levelIndex = key.levelIndex,
                    levelManager = levelManager,
                    palette = palette,
                    onPlaytestLevel = { customLevel ->
                        val savedIndex = levelManager.saveCustomLevel(customLevel)
                        backStack.add(GameKey(levelIndex = savedIndex, isCustom = true))
                    },
                    onBackToMenu = {
                        backStack.removeLastOrNull()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<SettingsKey> {
                SettingsScreen(
                    currentPaletteType = currentPaletteType,
                    onPaletteTypeChange = { currentPaletteType = it },
                    showCrtScanlines = showCrtScanlines,
                    onToggleCrtScanlines = { showCrtScanlines = it },
                    isSoundEnabled = isSoundEnabled,
                    onToggleSound = { isSoundEnabled = it },
                    palette = palette,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<HowToPlayKey> {
                HowToPlayScreen(
                    palette = palette,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
