package com.example.loderunner.game.ui

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.ui.theme.GamePalette

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabletDpad(
    onDirectionChange: (Direction) -> Unit,
    palette: GamePalette,
    modifier: Modifier = Modifier
) {
    val buttonSize = 56.dp

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // UP Button
        DirectionalTouchButton(
            label = "▲",
            palette = palette,
            size = buttonSize,
            onPress = { onDirectionChange(Direction.UP) },
            onRelease = { onDirectionChange(Direction.NONE) }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // LEFT Button
            DirectionalTouchButton(
                label = "◀",
                palette = palette,
                size = buttonSize,
                onPress = { onDirectionChange(Direction.LEFT) },
                onRelease = { onDirectionChange(Direction.NONE) }
            )

            // Center decorative D-pad hub
            Box(
                modifier = Modifier
                    .size(buttonSize * 0.85f)
                    .clip(CircleShape)
                    .background(palette.bezelBackground)
                    .border(2.dp, palette.buttonBorder.copy(alpha = 0.5f), CircleShape)
            )

            // RIGHT Button
            DirectionalTouchButton(
                label = "▶",
                palette = palette,
                size = buttonSize,
                onPress = { onDirectionChange(Direction.RIGHT) },
                onRelease = { onDirectionChange(Direction.NONE) }
            )
        }

        // DOWN Button
        DirectionalTouchButton(
            label = "▼",
            palette = palette,
            size = buttonSize,
            onPress = { onDirectionChange(Direction.DOWN) },
            onRelease = { onDirectionChange(Direction.NONE) }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabletActionButtons(
    onDigLeft: () -> Unit,
    onDigRight: () -> Unit,
    palette: GamePalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // DIG LEFT
        ArcadeActionButton(
            label = "DIG L",
            subLabel = "↙",
            palette = palette,
            buttonColor = Color(0xFFC83820),
            onClick = onDigLeft
        )

        // DIG RIGHT
        ArcadeActionButton(
            label = "DIG R",
            subLabel = "↘",
            palette = palette,
            buttonColor = Color(0xFFC83820),
            onClick = onDigRight
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DirectionalTouchButton(
    label: String,
    palette: GamePalette,
    size: androidx.compose.ui.unit.Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.buttonBackground)
            .border(2.dp, palette.buttonBorder, RoundedCornerShape(12.dp))
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onPress()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onRelease()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = palette.buttonBorder,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ArcadeActionButton(
    label: String,
    subLabel: String,
    palette: GamePalette,
    buttonColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .border(3.dp, palette.buttonBorder, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
