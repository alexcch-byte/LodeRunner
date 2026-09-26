package com.example.loderunner.game.levels

import com.example.loderunner.game.model.LevelData

object ClassicLevels {
    /**
     * Level 1: The Vault (Official 1983 Apple II Lode Runner).
     * Fully connected platform tiers, ladders, ropes, and secret escape ladder.
     */
    val LEVEL_1 = LevelData.fromLines(
        id = 1,
        name = "Level 1: The Vault",
        lines = listOf(
            "                  S         ",
            "    G             S         ",
            "#######H#######   S         ",
            "       H----------S    G    ",
            "       H    ##H   #######H##",
            "       H    ##H          H  ",
            "     0 H    ##H       G0 H  ",
            "##H#####    ########H#######",
            "  H                 H       ",
            "  H           0     H       ",
            "#########H##########H       ",
            "         H          H       ",
            "       G H----------H   G   ",
            "    H######         #######H",
            "    H         &  G         H",
            "############################"
        )
    )

    /**
     * Level 2: High Wire (Official 1983 Apple II Lode Runner).
     * Suspended monkey bars, false brick traps, and bedrock chambers.
     */
    val LEVEL_2 = LevelData.fromLines(
        id = 2,
        name = "Level 2: High Wire",
        lines = listOf(
            "   G                       H",
            "H@@#@@H           G        H",
            "H     H    H#########H G   H",
            "H G 0 H    H         H####XH",
            "H#@#@#H    H         H     S",
            "H     H----H------  0H     S",
            "H     H    H     H###@@@@@@H",
            "H     H    H  G  H         H",
            "H   0 H G  H#####H         H",
            "@###@##@##@H         H###H##",
            "@###@      H         H   H  ",
            "@G  @      H   ------H   H G",
            "########H###@@@@     H  ####",
            "        H            H      ",
            "        H   &        H      ",
            "############################"
        )
    )

    /**
     * Level 3: Stepped Descent (Official 1983 Apple II Lode Runner).
     * Diagonal monkey bars, multi-floor guard chases, and central vault.
     */
    val LEVEL_3 = LevelData.fromLines(
        id = 3,
        name = "Level 3: Stepped Descent",
        lines = listOf(
            "                           S",
            "----------    G            S",
            "H G      H##########H      S",
            "#####H   H          H@@@@@@@",
            "     H 0 H     G    H       ",
            "     H######H#####H##       ",
            "  G  H      H     H  --     ",
            "####H#      H  0  H    --   ",
            "    H    H######H##      --G",
            "    H----H      H  0       #",
            "    H       H#########H     ",
            "    H       H#########H     ",
            "###H##########   G   #####H#",
            "###H########## H###H #####H#",
            "   H      &    H###H   G  H ",
            "############################"
        )
    )

    /**
     * Level 4: The Gauntlet (Official 1983 Apple II Lode Runner).
     * Symmetrical ladder grid with clustered guards and treasures.
     */
    val LEVEL_4 = LevelData.fromLines(
        id = 4,
        name = "Level 4: Gauntlet",
        lines = listOf(
            "S                           ",
            "S-----------                ",
            "H     H     # G #     H     ",
            "H G  HHH  G ##### G  HHH  G ",
            "H HH  H  HH       HH  H  HH ",
            "H H HHHHH H       H HHHHH H ",
            "H H  G0G  H   H   H  G0G  H ",
            "H  H#####H   HHH   H#####H  ",
            "H   HHHHH HH  H  HH HHHHH   ",
            "H         H HHHHH H         ",
            "H    G    H  G0G  H     G   ",
            "H######H   H#####H  H#######",
            "H      H    HHHHH   H       ",
            "H      H            H       ",
            "H      H       G  & H       ",
            "############################"
        )
    )

    /**
     * Level 5: The Pyramid / Staircase (Official 1983 Apple II Lode Runner).
     * Intricate staggered stair steps, vertical ladder shafts, and rooftop escape.
     */
    val LEVEL_5 = LevelData.fromLines(
        id = 5,
        name = "Level 5: Staircase",
        lines = listOf(
            "         S                  ",
            "         S       G      0   ",
            "##H      S      ####H#######",
            "  H#     S     ##   H       ",
            "  H##    S    ##    H       ",
            "G H###   S  G###    H   G   ",
            "##H####  S  ####H###H#######",
            "  H   ## S ##   H           ",
            "  HG0  ##H##    H     G     ",
            "H###H    H     #H##H###     ",
            "H   H              H        ",
            "H   H    G     0   H        ",
            "H   H#######H######H#####H##",
            "H           H            H  ",
            "H           H  &         H  ",
            "############################"
        )
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
