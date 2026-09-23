package com.example.loderunner.game.model

/**
 * Represents a hole dug in a brick tile.
 *
 * @param x Column index (0..27)
 * @param y Row index (0..15)
 * @param remainingTicks Number of simulation ticks until the hole closes completely
 * @param totalTicks Total initial ticks of the hole lifetime
 */
data class DugHole(
    val x: Int,
    val y: Int,
    var remainingTicks: Int,
    val totalTicks: Int = 360, // ~6 seconds at 60 FPS
    var trappedEnemyId: Int? = null
) {
    val progress: Float
        get() = remainingTicks.toFloat() / totalTicks.toFloat()

    /** True when the hole is in its final warning phase (crumbling / flashing) */
    val isCrumbling: Boolean
        get() = remainingTicks < (totalTicks * 0.25f)
}
