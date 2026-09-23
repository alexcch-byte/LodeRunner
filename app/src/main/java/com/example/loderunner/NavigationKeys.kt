package com.example.loderunner

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MainMenuKey : NavKey

@Serializable
data class GameKey(val levelIndex: Int = 1, val isCustom: Boolean = false) : NavKey

@Serializable
data class LevelEditorKey(val levelIndex: Int = -1) : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object HowToPlayKey : NavKey
