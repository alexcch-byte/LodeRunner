package com.example.loderunner.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.ui.theme.GamePalette

@Composable
fun TabletDpad(
    onDirectionChange: (Direction) -> Unit,
    palette: GamePalette,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 54.dp
) {
    val currentOnDirectionChange by rememberUpdatedState(onDirectionChange)
    // Directions currently held, most recent last; releasing one falls back to another still held
    val heldDirections = remember { mutableListOf<Direction>() }
    val press: (Direction) -> Unit = { dir ->
        heldDirections.remove(dir)
        heldDirections.add(dir)
        currentOnDirectionChange(dir)
    }
    val release: (Direction) -> Unit = { dir ->
        heldDirections.remove(dir)
        currentOnDirectionChange(heldDirections.lastOrNull() ?: Direction.NONE)
    }

    Column(
        modifier = modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // UP Button
        EnhancedDirectionalButton(
            label = "▲",
            palette = palette,
            size = buttonSize,
            onPress = { press(Direction.UP) },
            onRelease = { release(Direction.UP) }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // LEFT Button
            EnhancedDirectionalButton(
                label = "◀",
                palette = palette,
                size = buttonSize,
                onPress = { press(Direction.LEFT) },
                onRelease = { release(Direction.LEFT) }
            )

            // Center decorative D-pad metallic hub
            Box(
                modifier = Modifier
                    .size(buttonSize * 0.82f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2C3648), palette.bezelBackground)
                        )
                    )
                    .border(2.dp, palette.buttonBorder.copy(alpha = 0.6f), CircleShape)
            )

            // RIGHT Button
            EnhancedDirectionalButton(
                label = "▶",
                palette = palette,
                size = buttonSize,
                onPress = { press(Direction.RIGHT) },
                onRelease = { release(Direction.RIGHT) }
            )
        }

        // DOWN Button
        EnhancedDirectionalButton(
            label = "▼",
            palette = palette,
            size = buttonSize,
            onPress = { press(Direction.DOWN) },
            onRelease = { release(Direction.DOWN) }
        )
    }
}

@Composable
fun TabletActionButtons(
    onDigLeft: () -> Unit,
    onDigRight: () -> Unit,
    palette: GamePalette,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    stacked: Boolean = false
) {
    val digLeft = @Composable {
        EnhancedArcadeActionButton(
            label = "DIG L",
            subLabel = "↙",
            palette = palette,
            baseColor = Color(0xFFD42818),
            highlightColor = Color(0xFFFF5A40),
            size = buttonSize,
            onClick = onDigLeft
        )
    }
    val digRight = @Composable {
        EnhancedArcadeActionButton(
            label = "DIG R",
            subLabel = "↘",
            palette = palette,
            baseColor = Color(0xFFD42818),
            highlightColor = Color(0xFFFF5A40),
            size = buttonSize,
            onClick = onDigRight
        )
    }

    if (stacked) {
        // Narrow phone bezel: stack vertically, DIG L on top
        Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            digLeft()
            digRight()
        }
    } else {
        Row(
            modifier = modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            digLeft()
            digRight()
        }
    }
}

@Composable
private fun EnhancedDirectionalButton(
    label: String,
    palette: GamePalette,
    size: Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) {
                        listOf(palette.buttonBorder.copy(alpha = 0.4f), palette.buttonBackground)
                    } else {
                        listOf(Color(0xFF2C384C), palette.buttonBackground)
                    }
                )
            )
            .border(
                width = 2.dp,
                color = if (isPressed) Color.White else palette.buttonBorder,
                shape = RoundedCornerShape(12.dp)
            )
            // Per-pointer gesture tracking: works with other fingers on screen (multi-touch),
            // unlike pointerInteropFilter which only saw the combined MotionEvent's ACTION_DOWN/UP.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    currentOnPress()
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        isPressed = false
                        currentOnRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else palette.buttonBorder,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun EnhancedArcadeActionButton(
    label: String,
    subLabel: String,
    palette: GamePalette,
    baseColor: Color,
    highlightColor: Color,
    size: Dp,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(highlightColor, baseColor, Color(0xFF6B1008))
                )
            )
            .border(3.dp, palette.buttonBorder, CircleShape)
            // Dig fires on touch-down (not on release like clickable) for responsive arcade input
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    currentOnClick()
                    waitForUpOrCancellation()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subLabel,
                color = palette.goldHighlight,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
