package com.example.loderunner.game.model

/**
 * Bungeling Guard enemy.
 */
data class Enemy(
    val id: Int,
    var x: Float,
    var y: Float,
    val spawnX: Float,
    val spawnY: Float,
    var state: EntityState = EntityState.IDLE,
    var facing: Direction = Direction.LEFT,
    var trappedTicks: Int = 0,
    var hasGold: Boolean = false,
    var respawnTicks: Int = 0,
    var animFrame: Int = 0
) {
    val gridX: Int get() = kotlin.math.round(x).toInt().coerceIn(0, 27)
    val gridY: Int get() = kotlin.math.round(y).toInt().coerceIn(0, 15)

    val isTrapped: Boolean get() = trappedTicks > 0
    val isDead: Boolean get() = respawnTicks > 0

    fun reset() {
        x = spawnX
        y = spawnY
        state = EntityState.IDLE
        facing = Direction.LEFT
        trappedTicks = 0
        hasGold = false
        respawnTicks = 0
        animFrame = 0
    }
}
