package com.example.loderunner.game.model

/**
 * Game speed settings controlling movement velocities and physics timers.
 */
enum class GameSpeed(
    val displayName: String,
    val description: String,
    val multiplier: Float
) {
    SLOW(
        displayName = "Slow / Relaxed",
        description = "0.75× speed – deliberate, forgiving puzzle pacing",
        multiplier = 0.75f
    ),
    NORMAL(
        displayName = "Normal / Classic",
        description = "1.0× speed – authentic 1980s PC pacing",
        multiplier = 1.0f
    ),
    FAST(
        displayName = "Fast",
        description = "1.35× speed – brisk arcade challenge",
        multiplier = 1.35f
    );

    companion object {
        fun fromName(name: String?): GameSpeed {
            return entries.find { it.name == name } ?: NORMAL
        }
    }
}
