package com.example.loderunner.game.model

/**
 * The player character (Runner).
 */
data class Runner(
    var x: Float,
    var y: Float,
    var state: EntityState = EntityState.IDLE,
    var facing: Direction = Direction.RIGHT,
    var digTimer: Int = 0, // > 0 when performing dig animation
    var digDirection: Direction = Direction.NONE,
    var animFrame: Int = 0
) {
    val gridX: Int get() = kotlin.math.round(x).toInt().coerceIn(0, 27)
    val gridY: Int get() = kotlin.math.round(y).toInt().coerceIn(0, 15)

    fun reset(startX: Int, startY: Int) {
        x = startX.toFloat()
        y = startY.toFloat()
        state = EntityState.IDLE
        facing = Direction.RIGHT
        digTimer = 0
        digDirection = Direction.NONE
        animFrame = 0
    }
}
