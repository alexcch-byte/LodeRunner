package com.example.loderunner.game.levels

import com.example.loderunner.game.model.LevelData

object ClassicLevels {
    /**
     * Level 1: The Classic Introductory Level.
     * Features multi-tiered brick platforms, ladders, rope traverses, 5 gold chests, and 2 guards.
     */
    val LEVEL_1 = LevelData.fromAscii(
        id = 1,
        name = "Level 1: The Vault",
        ascii = """
            E                          E
            E                          E
            E                          E
            E##########################E
            E         H     G    H     E
            E         H   #####  H     E
            E-------  H          H     E
            E      #  H    G     H     E
            E      #  H  #####   H     E
            EG     #  H          H    GE
            E###   #  H          H  ###E
            E      #  H---G------H     E
            E  M   #  H   #   #  H   M E
            E#######  H   #   #  H#####E
            E &       H   G   #  H     E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """
    )

    /**
     * Level 2: The Rope Maze.
     * High suspended bars, falling traps, tricky guard paths.
     */
    val LEVEL_2 = LevelData.fromAscii(
        id = 2,
        name = "Level 2: High Wire",
        ascii = """
            E                          E
            E                          E
            E                          E
            E--------------------------E
            E    H         H           E
            E    H   G     H    G      E
            E#####   ###   H   ###     E
            E        H     H     H     E
            E  G     H     H     H  G  E
            E#####   H   #####   H#####E
            E        H     H     H     E
            E  M     H  G  H  G  H   M E
            E#####   H#####H#####H#####E
            E        H     H     H     E
            E   &    H     G     H     E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """
    )

    /**
     * Level 3: The Pyramid.
     * A central stepped pyramid with gold caches guarded by relentless Bungelings.
     */
    val LEVEL_3 = LevelData.fromAscii(
        id = 3,
        name = "Level 3: The Pyramid",
        ascii = """
            E             H            E
            E             H            E
            E            H#H           E
            E           H###H          E
            E          H#####H         E
            E         H#######H        E
            E        H##  G  ##H       E
            E       H###  G  ###H      E
            E      H#####G G#####H     E
            E     H###############H    E
            E    H  -           -  H   E
            E   H   -   G   G   -   H  E
            E  H    -  #######  -    H E
            E H  M  -           -  M  HE
            E&H#####-           -#####HE
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """
    )

    /**
     * Level 4: The Ladder Gauntlet.
     * Vertical agility required, digging fast to trap guards.
     */
    val LEVEL_4 = LevelData.fromAscii(
        id = 4,
        name = "Level 4: Gauntlet",
        ascii = """
            E    H        H        H   E
            E    H        H        H   E
            E    H        H        H   E
            E    H########H########H   E
            E    H   G    H   G    H   E
            E    H  ####  H  ####  H   E
            E----H--------H--------H---E
            E    H        H        H   E
            E G  H   G    H   G    H  GE
            E####H########H########H###E
            E    H        H        H   E
            E    H   M    H    M   H   E
            E    H########H########H   E
            E    H        H        H   E
            E  & H        G        H   E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """
    )

    /**
     * Level 5: The Fortress.
     * Heavy solid rocks and intricate diggable chambers.
     */
    val LEVEL_5 = LevelData.fromAscii(
        id = 5,
        name = "Level 5: The Fortress",
        ascii = """
            E              E           E
            E              E           E
            E              E           E
            E@@@@@@@@@@@@@@@@@@@@@@@@@@E
            E  G   H     H     H   G   E
            E ###  H  G  H  G  H  ###  E
            E      H#####H#####H       E
            E  M   H     H     H   M   E
            E######H     H     H#######E
            E      H-----H-----H       E
            E  G   H     H     H   G   E
            E #### H  G  H  G  H ####  E
            E      H#####H#####H       E
            E      H     H     H       E
            E  &   H           H       E
            @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        """
    )

    val ALL_LEVELS: List<LevelData> = listOf(
        LEVEL_1,
        LEVEL_2,
        LEVEL_3,
        LEVEL_4,
        LEVEL_5
    )

    fun getLevel(number: Int): LevelData {
        val index = (number - 1).coerceIn(0, ALL_LEVELS.size - 1)
        return ALL_LEVELS[index].clone()
    }
}
