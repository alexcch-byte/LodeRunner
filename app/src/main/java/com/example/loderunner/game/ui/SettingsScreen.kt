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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.model.GameSpeed
import com.example.loderunner.game.ui.theme.GamePalette
import com.example.loderunner.game.ui.theme.PaletteType

@Composable
fun SettingsScreen(
    currentPaletteType: PaletteType,
    onPaletteTypeChange: (PaletteType) -> Unit,
    currentGameSpeed: GameSpeed,
    onGameSpeedChange: (GameSpeed) -> Unit,
    showCrtScanlines: Boolean,
    onToggleCrtScanlines: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    palette: GamePalette,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bezelBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.background)
                .border(2.dp, palette.buttonBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SETTINGS & OPTIONS",
                color = palette.buttonBorder,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            // Game Speed Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GAME SPEED",
                    color = palette.hudLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameSpeed.entries.forEach { speed ->
                        val isSelected = currentGameSpeed == speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) palette.buttonBorder.copy(alpha = 0.25f) else palette.buttonBackground)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) palette.buttonBorder else Color.DarkGray,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onGameSpeedChange(speed) }
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = speed.displayName,
                                    color = if (isSelected) palette.buttonBorder else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${speed.multiplier}x",
                                    color = if (isSelected) palette.buttonBorder else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Retro Palette Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RETRO COLOR THEME",
                    color = palette.hudLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                PaletteType.entries.forEach { type ->
                    val isSelected = currentPaletteType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) palette.buttonBorder.copy(alpha = 0.25f) else palette.buttonBackground)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) palette.buttonBorder else Color.DarkGray,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onPaletteTypeChange(type) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            color = if (isSelected) palette.buttonBorder else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // CRT Scanlines Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRT SCANLINE EFFECT",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = showCrtScanlines,
                    onCheckedChange = onToggleCrtScanlines,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.buttonBorder,
                        checkedTrackColor = palette.buttonBackground
                    )
                )
            }

            // Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "8-BIT SOUND FX",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = onToggleSound,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.buttonBorder,
                        checkedTrackColor = palette.buttonBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.buttonBackground)
                    .border(2.dp, palette.buttonBorder, RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "BACK TO MENU",
                    color = palette.buttonBorder,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
