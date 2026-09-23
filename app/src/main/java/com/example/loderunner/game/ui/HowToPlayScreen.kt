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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun HowToPlayScreen(
    palette: GamePalette,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bezelBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(620.dp)
                .fillMaxSize(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.background)
                .border(2.dp, palette.buttonBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "HOW TO PLAY LODE RUNNER",
                color = palette.gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            SectionBlock(
                title = "1. OBJECTIVE",
                text = "Collect every chest of Bungeling gold scattered across each sector. Once all gold is collected, hidden escape ladders appear leading to the ceiling. Climb to the top row to advance!",
                palette = palette
            )

            SectionBlock(
                title = "2. DIGGING & HOLES",
                text = "You cannot jump! Instead, you carry a laser digging blaster. Tap DIG L to dig a hole down-left, or DIG R to dig down-right into brick. Holes trap Bungeling guards. After several seconds, bricks regenerate and fill back in. Trapped guards or runners inside when it closes are CRUSHED (+750 pts)!",
                palette = palette
            )

            SectionBlock(
                title = "3. RUNNING ON HEADS",
                text = "When a guard is trapped inside a hole, you can safely walk across the guard's head as if it were a solid bridge! Trapped guards drop any gold they are carrying.",
                palette = palette
            )

            SectionBlock(
                title = "4. CONTROLS",
                text = "• On-screen: Left D-pad for directions, Right buttons for DIG L / DIG R.\n• Tablet Keyboard: Arrow keys or WASD to move, Z / X (or J / K, Q / E) to dig left/right, Space to pause, R to restart level.",
                palette = palette
            )

            SectionBlock(
                title = "5. TABLET LEVEL DESIGNER",
                text = "Use the built-in Level Editor to construct your own retro puzzle stages. Tap or drag tiles onto the 28x16 canvas, then tap PLAYTEST to immediately play your creations!",
                palette = palette
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.buttonBackground)
                    .border(2.dp, palette.buttonBorder, RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "BACK TO MENU",
                    color = palette.buttonBorder,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    text: String,
    palette: GamePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.bezelBackground.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = palette.buttonBorder,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = text,
            color = Color(0xFFDDDDDD),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
