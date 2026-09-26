package com.example.loderunner

import com.example.loderunner.game.audio.SoundFxEngine
import com.example.loderunner.game.core.GameEngine
import com.example.loderunner.game.levels.ClassicLevels
import com.example.loderunner.game.model.Direction
import com.example.loderunner.game.model.EntityState
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
        assertEquals(3, state.enemies.size)
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

    @Test
    fun testLadderClimbingAndSmoothDismount() {
        // Level layout:
        // Row 12: Ladder at col 10 (H), platform to the right at col 11..13 (#), walk surface row 11
        // Row 13: Ladder at col 10 (H)
        // Row 14: Floor at col 8..15 (#), runner starts at col 9 (x = 9.3)
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
            E         H###             E
            E         H                E
            E        &H                E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """.trimIndent()
        val level = LevelData.fromAscii(1, "Test Ladder", ascii)
        engine.startLevel(level)

        // Runner is at (9, 14). Ladder is at col 10 (rows 12 and 13).
        // Move towards ladder and press UP:
        engine.state.runner.x = 9.6f // Approaching ladder horizontally
        engine.setInputDirection(Direction.UP)

        for (i in 0 until 10) {
            engine.tick()
        }

        // Runner should magnetically snap to column 10 and start climbing UP
        assertEquals(10, engine.state.runner.gridX)
        assertTrue("Runner should be climbing or climbing up", engine.state.runner.y < 14.0f)

        // Climb all the way up to row 11.0 (top of ladder, standing on top rung level with row 11 walking surface)
        var climbTicks = 0
        while (engine.state.runner.y > 11.05f && climbTicks++ < 200) {
            engine.tick()
        }

        // Runner should reach exactly y = 11.0f (yMin) and remain stable without falling
        assertEquals(11.0f, engine.state.runner.y, 0.05f)
        assertFalse("Runner must NOT fall when standing at top of ladder", engine.state.runner.state == EntityState.FALLING)

        // Dismount right onto platform at col 11 (platform bricks at row 12, empty at row 11)
        engine.setInputDirection(Direction.RIGHT)
        for (i in 0 until 20) {
            engine.tick()
        }

        assertTrue("Runner should step right onto platform", engine.state.runner.x > 10.5f)
        assertEquals(11, engine.state.runner.gridY)
        assertFalse("Runner must NOT fall on platform", engine.state.runner.state == EntityState.FALLING)

        // Now walk back LEFT towards ladder and climb DOWN
        engine.setInputDirection(Direction.LEFT)
        for (i in 0 until 20) {
            engine.tick()
        }

        // Press DOWN to climb back down the ladder
        engine.setInputDirection(Direction.DOWN)
        var downTicks = 0
        while (engine.state.runner.y < 13.9f && downTicks++ < 200) {
            engine.tick()
        }

        assertEquals(10, engine.state.runner.gridX)
        assertTrue("Runner should have climbed back down ladder towards floor", engine.state.runner.y > 13.5f)
        assertFalse("Runner must NOT be falling down ladder", engine.state.runner.state == EntityState.FALLING)
    }
}
