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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.example.loderunner.game.ui.theme.GamePalette

@Composable
fun LevelClearedOverlay(
    levelNumber: Int,
    score: Int,
    palette: GamePalette,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(palette.bezelBackground)
                .border(3.dp, palette.escapeLadder, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "STAGE CLEARED!",
                color = palette.escapeLadder,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Level $levelNumber Completed",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Score: $score",
                color = palette.gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            DialogActionButton(
                label = "NEXT LEVEL ▶",
                palette = palette,
                color = palette.escapeLadder,
                onClick = onNextLevel
            )
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    highScore: Int,
    palette: GamePalette,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(palette.bezelBackground)
                .border(3.dp, Color(0xFFE02020), RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "GAME OVER",
                color = Color(0xFFE02020),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Final Score: $score",
                color = palette.hudText,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace
            )

            if (score >= highScore && score > 0) {
                Text(
                    text = "★ NEW HIGH SCORE! ★",
                    color = palette.gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DialogActionButton(
                    label = "PLAY AGAIN",
                    palette = palette,
                    color = palette.buttonBorder,
                    onClick = onRestart
                )
                DialogActionButton(
                    label = "MAIN MENU",
                    palette = palette,
                    color = Color.LightGray,
                    onClick = onMainMenu
                )
            }
        }
    }
}

@Composable
fun PauseOverlay(
    palette: GamePalette,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(palette.bezelBackground)
                .border(2.dp, palette.buttonBorder, RoundedCornerShape(16.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "PAUSED",
                color = palette.buttonBorder,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            DialogActionButton(
                label = "RESUME",
                palette = palette,
                color = palette.buttonBorder,
                onClick = onResume
            )

            DialogActionButton(
                label = "RESTART LEVEL",
                palette = palette,
                color = Color.Yellow,
                onClick = onRestart
            )

            DialogActionButton(
                label = "MAIN MENU",
                palette = palette,
                color = Color.LightGray,
                onClick = onMainMenu
            )
        }
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    palette: GamePalette,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.buttonBackground)
            .border(2.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
