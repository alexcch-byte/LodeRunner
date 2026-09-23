package com.example.loderunner

import com.example.loderunner.game.audio.SoundFxEngine
import com.example.loderunner.game.core.GameEngine
import com.example.loderunner.game.levels.ClassicLevels
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.model.GameStatus
import com.example.loderunner.game.model.LevelData
import com.example.loderunner.game.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var soundFx: SoundFxEngine
    private lateinit var engine: GameEngine

    @Before
    fun setup() {
        soundFx = SoundFxEngine().apply { isSoundEnabled = false }
        engine = GameEngine(soundFx)
    }

    @Test
    fun testLevel1Initialization() {
        val level1 = ClassicLevels.LEVEL_1
        engine.startLevel(level1)

        val state = engine.state
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(5, state.lives)
        assertEquals(0, state.score)
        assertTrue("Gold should be present", state.goldTotal > 0)
        assertEquals(state.goldTotal, state.goldRemaining)
        assertFalse(state.escapeLaddersRevealed)
        assertEquals(level1.runnerStartX, state.runner.gridX)
        assertEquals(level1.runnerStartY, state.runner.gridY)
        assertEquals(2, state.enemies.size)
    }

    @Test
    fun testDiggingLeftAndRegeneration() {
        // Create simple level: Runner on row 14, brick at (13, 15)
        val ascii = """
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E             &            E
            @@@@@@@@@@@@@#@@@@@@@@@@@@@@
        """.trimIndent()
        // Runner is at (14, 14). Down-left is (13, 15), which is '#'
        val level = LevelData.fromAscii(1, "Test Dig", ascii)
        engine.startLevel(level)

        assertEquals(TileType.BRICK.id, engine.state.activeGrid[15][13])

        // Dig left
        engine.triggerDig(Direction.LEFT)
        engine.tick()

        // Tile (13, 15) should now be dug out (EMPTY) and a hole should exist
        assertEquals(TileType.EMPTY.id, engine.state.activeGrid[15][13])
        assertEquals(1, engine.state.dugHoles.size)
        val hole = engine.state.dugHoles[0]
        assertEquals(13, hole.x)
        assertEquals(15, hole.y)

        // Fast forward hole ticks
        while (engine.state.dugHoles.isNotEmpty()) {
            engine.tick()
        }

        // After regeneration, brick should be restored!
        assertEquals(TileType.BRICK.id, engine.state.activeGrid[15][13])
    }

    @Test
    fun testCrushingEnemyInHoleAwardsScore() {
        // Enemy directly above diggable brick, falls in and gets crushed
        val ascii = """
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E            M             E
            E             &            E
            @@@@@@@@@@@@@#@@@@@@@@@@@@@@
        """.trimIndent()
        val level = LevelData.fromAscii(1, "Test Crush", ascii)
        engine.startLevel(level)

        // Dig left
        engine.triggerDig(Direction.LEFT)
        engine.tick()
        assertEquals(1, engine.state.dugHoles.size)

        // Tick simulation until enemy falls into hole
        for (i in 0 until 60) {
            engine.tick()
        }

        val enemy = engine.state.enemies[0]
        assertTrue("Enemy should be trapped or dead", enemy.isTrapped || enemy.isDead)

        // Tick until hole regenerates and crushes enemy
        for (i in 0 until GameEngine.HOLE_TOTAL_TICKS) {
            engine.tick()
        }

        // Enemy should be crushed and player awarded 750 points
        assertTrue(engine.state.score >= 750)
    }

    @Test
    fun testCollectingGoldRevealsEscapeLadders() {
        // Level with 1 gold nugget
        val ascii = """
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E                          E
            E            &G            E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """.trimIndent()
        val level = LevelData.fromAscii(1, "Test Gold", ascii)
        engine.startLevel(level)

        assertEquals(1, engine.state.goldRemaining)
        assertFalse(engine.state.escapeLaddersRevealed)

        // Move right into the gold
        engine.setInputDirection(Direction.RIGHT)
        for (i in 0 until 15) {
            engine.tick()
        }

        assertEquals(0, engine.state.goldRemaining)
        assertTrue("Escape ladders should be revealed when all gold is collected", engine.state.escapeLaddersRevealed)
        assertTrue(engine.state.score >= 250)
    }
}
