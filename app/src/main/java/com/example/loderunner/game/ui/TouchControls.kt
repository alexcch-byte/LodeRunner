package com.example.loderunner.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.ui.theme.GamePalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Virtual Analog Thumbstick for smooth, responsive 4-way arcade directional control.
 * Tracking isolated by pointer ID to ensure multi-touch with Dig buttons never sticks.
 * Snaps back to center and resets direction immediately upon release.
 */
@Composable
fun TabletThumbstick(
    onDirectionChange: (Direction) -> Unit,
    palette: GamePalette,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val currentOnDirectionChange by rememberUpdatedState(onDirectionChange)
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val knobSize = size * 0.44f
    val knobSizePx = with(density) { knobSize.toPx() }
    val maxRadiusPx = (sizePx - knobSizePx) / 2f
    val centerPx = Offset(sizePx / 2f, sizePx / 2f)

    var knobOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var activeDirection by remember { mutableStateOf(Direction.NONE) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isDragging = true

                    fun updateDirection(pos: Offset) {
                        val delta = pos - centerPx
                        val dist = delta.getDistance()
                        val clampedOffset = if (dist > maxRadiusPx) {
                            delta * (maxRadiusPx / dist)
                        } else {
                            delta
                        }
                        knobOffsetPx = clampedOffset

                        val deadZone = maxRadiusPx * 0.20f
                        val newDir = if (dist < deadZone) {
                            Direction.NONE
                        } else {
                            val angle = atan2(delta.y, delta.x) * (180f / PI.toFloat())
                            val h = 12f // 12-degree hysteresis prevents jitter between adjacent quadrants
                            when (activeDirection) {
                                Direction.UP -> when {
                                    angle in (-135f - h)..(-45f + h) -> Direction.UP
                                    angle in (-45f + h)..(45f) -> Direction.RIGHT
                                    angle in (45f)..(135f) -> Direction.DOWN
                                    else -> Direction.LEFT
                                }
                                Direction.DOWN -> when {
                                    angle in (45f - h)..(135f + h) -> Direction.DOWN
                                    angle in (-45f)..(45f - h) -> Direction.RIGHT
                                    angle in (-135f)..(-45f) -> Direction.UP
                                    else -> Direction.LEFT
                                }
                                Direction.LEFT -> when {
                                    angle < (-135f + h) || angle > (135f - h) -> Direction.LEFT
                                    angle in (-135f + h)..(-45f) -> Direction.UP
                                    angle in (45f)..(135f - h) -> Direction.DOWN
                                    else -> Direction.RIGHT
                                }
                                Direction.RIGHT -> when {
                                    angle in (-45f - h)..(45f + h) -> Direction.RIGHT
                                    angle in (-135f)..(-45f - h) -> Direction.UP
                                    angle in (45f + h)..(135f) -> Direction.DOWN
                                    else -> Direction.LEFT
                                }
                                else -> when {
                                    angle in -45f..45f -> Direction.RIGHT
                                    angle in 45f..135f -> Direction.DOWN
                                    angle in -135f..-45f -> Direction.UP
                                    else -> Direction.LEFT
                                }
                            }
                        }
                        if (newDir != activeDirection) {
                            activeDirection = newDir
                            currentOnDirectionChange(newDir)
                        }
                    }

                    updateDirection(down.position)

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            updateDirection(change.position)
                        }
                    } finally {
                        isDragging = false
                        knobOffsetPx = Offset.Zero
                        if (activeDirection != Direction.NONE) {
                            activeDirection = Direction.NONE
                            currentOnDirectionChange(Direction.NONE)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Base plate Canvas (recessed housing, guide notches, active direction glows)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val radius = w / 2f

            // 1. Outer Bezel Housing Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.bezelBackground,
                        Color(0xFF141A26),
                        Color(0xFF080C14)
                    ),
                    center = Offset(radius, radius),
                    radius = radius
                ),
                radius = radius,
                center = Offset(radius, radius)
            )

            // 2. Beveled Outer Rim Border
            drawCircle(
                color = palette.buttonBorder.copy(alpha = 0.7f),
                radius = radius - 1.5f,
                center = Offset(radius, radius),
                style = Stroke(width = 3f)
            )

            // 3. Inner Recessed Track Groove
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = maxRadiusPx,
                center = Offset(radius, radius),
                style = Stroke(width = 2f)
            )

            // 4. Directional Indicators
            val notchDistance = radius * 0.72f
            val chevronColorInactive = palette.buttonBorder.copy(alpha = 0.45f)
            val activeGlow = palette.goldHighlight

            // UP Indicator
            drawDirectionalArrow(
                center = Offset(radius, radius - notchDistance),
                angle = -90f,
                isActive = activeDirection == Direction.UP,
                activeColor = activeGlow,
                inactiveColor = chevronColorInactive
            )

            // DOWN Indicator
            drawDirectionalArrow(
                center = Offset(radius, radius + notchDistance),
                angle = 90f,
                isActive = activeDirection == Direction.DOWN,
                activeColor = activeGlow,
                inactiveColor = chevronColorInactive
            )

            // LEFT Indicator
            drawDirectionalArrow(
                center = Offset(radius - notchDistance, radius),
                angle = 180f,
                isActive = activeDirection == Direction.LEFT,
                activeColor = activeGlow,
                inactiveColor = chevronColorInactive
            )

            // RIGHT Indicator
            drawDirectionalArrow(
                center = Offset(radius + notchDistance, radius),
                angle = 0f,
                isActive = activeDirection == Direction.RIGHT,
                activeColor = activeGlow,
                inactiveColor = chevronColorInactive
            )

            // Subdued crosshair guide lines
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(radius, radius - maxRadiusPx),
                end = Offset(radius, radius + maxRadiusPx),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(radius - maxRadiusPx, radius),
                end = Offset(radius + maxRadiusPx, radius),
                strokeWidth = 1.5f
            )
        }

        // Movable Center Thumb Knob
        val knobOffsetDpX = with(density) { knobOffsetPx.x.toDp() }
        val knobOffsetDpY = with(density) { knobOffsetPx.y.toDp() }

        Box(
            modifier = Modifier
                .offset(x = knobOffsetDpX, y = knobOffsetDpY)
                .size(knobSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isDragging) {
                            listOf(
                                palette.buttonBorder.copy(alpha = 0.6f),
                                Color(0xFF384660),
                                Color(0xFF1E2838)
                            )
                        } else {
                            listOf(
                                Color(0xFF3E4E68),
                                Color(0xFF263244),
                                Color(0xFF141C28)
                            )
                        }
                    )
                )
                .border(
                    width = 2.5.dp,
                    color = if (isDragging) Color.White else palette.buttonBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Concentric grip ridges & center cap
            Canvas(modifier = Modifier.fillMaxSize()) {
                val kr = this.size.width / 2f
                // Inner grip ring
                drawCircle(
                    color = Color.White.copy(alpha = if (isDragging) 0.35f else 0.18f),
                    radius = kr * 0.65f,
                    style = Stroke(width = 1.5f)
                )
                // Center jewel pip
                drawCircle(
                    color = if (isDragging) palette.goldHighlight else palette.buttonBorder.copy(alpha = 0.7f),
                    radius = kr * 0.28f
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = kr * 0.12f
                )
            }
        }
    }
}

private fun DrawScope.drawDirectionalArrow(
    center: Offset,
    angle: Float,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    val arrowSize = 8f
    val rad = angle * (PI.toFloat() / 180f)
    val cosA = cos(rad)
    val sinA = sin(rad)

    val pTip = Offset(center.x + cosA * arrowSize, center.y + sinA * arrowSize)
    val pLeft = Offset(center.x - cosA * (arrowSize * 0.6f) - sinA * arrowSize, center.y - sinA * (arrowSize * 0.6f) + cosA * arrowSize)
    val pRight = Offset(center.x - cosA * (arrowSize * 0.6f) + sinA * arrowSize, center.y - sinA * (arrowSize * 0.6f) - cosA * arrowSize)

    val path = Path().apply {
        moveTo(pTip.x, pTip.y)
        lineTo(pLeft.x, pLeft.y)
        lineTo(pRight.x, pRight.y)
        close()
    }

    if (isActive) {
        drawCircle(
            color = activeColor.copy(alpha = 0.4f),
            radius = arrowSize * 2.2f,
            center = center
        )
    }

    drawPath(
        path = path,
        color = if (isActive) activeColor else inactiveColor
    )
}

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
            // isolating down.id so other fingers touching the screen never cause buttons to stick.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isPressed = true
                    currentOnPress()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) break
                        }
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
