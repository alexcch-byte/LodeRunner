package com.example.loderunner.game.model

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    LEVEL_CLEARED,
    RUNNER_DIED,
    GAME_OVER
}

data class GameState(
    val levelNumber: Int = 1,
    var score: Int = 0,
    var lives: Int = 5,
    var goldRemaining: Int = 0,
    var goldTotal: Int = 0,
    var escapeLaddersRevealed: Boolean = false,
    var status: GameStatus = GameStatus.READY,
    var runner: Runner = Runner(14f, 14f),
    var enemies: List<Enemy> = emptyList(),
    var dugHoles: MutableList<DugHole> = mutableListOf(),
    var activeGrid: Array<IntArray> = Array(16) { IntArray(28) },
    var tickCount: Long = 0L,
    var statusTimer: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState
        if (levelNumber != other.levelNumber) return false
        if (score != other.score) return false
        if (lives != other.lives) return false
        if (goldRemaining != other.goldRemaining) return false
        if (goldTotal != other.goldTotal) return false
        if (escapeLaddersRevealed != other.escapeLaddersRevealed) return false
        if (status != other.status) return false
        if (runner != other.runner) return false
        if (enemies != other.enemies) return false
        if (dugHoles != other.dugHoles) return false
        if (!activeGrid.contentDeepEquals(other.activeGrid)) return false
        if (tickCount != other.tickCount) return false
        if (statusTimer != other.statusTimer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = levelNumber
        result = 31 * result + score
        result = 31 * result + lives
        result = 31 * result + goldRemaining
        result = 31 * result + goldTotal
        result = 31 * result + escapeLaddersRevealed.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + runner.hashCode()
        result = 31 * result + enemies.hashCode()
        result = 31 * result + dugHoles.hashCode()
        result = 31 * result + activeGrid.contentDeepHashCode()
        result = 31 * result + tickCount.hashCode()
        result = 31 * result + statusTimer
        return result
    }
}
