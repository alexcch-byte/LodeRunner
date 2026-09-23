package com.example.loderunner.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.model.GameState
import com.example.loderunner.game.ui.theme.GamePalette

@Composable
fun GameHud(
    gameState: GameState,
    highScore: Int,
    palette: GamePalette,
    onPauseClick: () -> Unit,
    onRestartClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.bezelBackground)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Score and High Score
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudField(label = "SCORE", value = String.format("%06d", gameState.score), palette = palette)
            HudField(label = "HIGH", value = String.format("%06d", maxOf(highScore, gameState.score)), palette = palette)
        }

        // Center: Level & Gold Counter
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudField(label = "LEVEL", value = String.format("%02d", gameState.levelNumber), palette = palette)
            HudField(
                label = "GOLD",
                value = "${gameState.goldTotal - gameState.goldRemaining}/${gameState.goldTotal}",
                palette = palette
            )
            HudField(label = "LIVES", value = "${gameState.lives}", palette = palette)
        }

        // Right side: Controls (Pause, Restart, Exit)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudButton(label = "RESTART", palette = palette, onClick = onRestartClick)
            HudButton(label = "PAUSE", palette = palette, onClick = onPauseClick)
            HudButton(label = "MENU", palette = palette, onClick = onExitClick)
        }
    }
}

@Composable
private fun HudField(label: String, value: String, palette: GamePalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = palette.hudLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = palette.hudText,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HudButton(label: String, palette: GamePalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.buttonBackground)
            .border(1.dp, palette.buttonBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = palette.buttonText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
