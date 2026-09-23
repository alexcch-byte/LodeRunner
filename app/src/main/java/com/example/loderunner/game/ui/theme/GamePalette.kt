package com.example.loderunner.game.ui.theme

import androidx.compose.ui.graphics.Color

enum class PaletteType(val displayName: String) {
    APPLE_II("Apple II (1983 Original)"),
    COMMODORE_64("Commodore 64"),
    IBM_PC_CGA("IBM PC CGA"),
    ARCADE_RETRO("Arcade Classic")
}

data class GamePalette(
    val type: PaletteType,
    val background: Color,
    val brickMain: Color,
    val brickHighlight: Color,
    val brickMortar: Color,
    val solidRockMain: Color,
    val solidRockHighlight: Color,
    val ladder: Color,
    val ladderRung: Color,
    val rope: Color,
    val gold: Color,
    val goldHighlight: Color,
    val runnerBody: Color,
    val runnerHead: Color,
    val guardBody: Color,
    val guardHead: Color,
    val escapeLadder: Color,
    val hudText: Color,
    val hudLabel: Color,
    val bezelBackground: Color,
    val buttonBackground: Color,
    val buttonBorder: Color,
    val buttonText: Color
) {
    companion object {
        val APPLE_II = GamePalette(
            type = PaletteType.APPLE_II,
            background = Color(0xFF080808),
            brickMain = Color(0xFFC04818),
            brickHighlight = Color(0xFFE87838),
            brickMortar = Color(0xFF381408),
            solidRockMain = Color(0xFF303888),
            solidRockHighlight = Color(0xFF5860C0),
            ladder = Color(0xFFEEEEEE),
            ladderRung = Color(0xFFCCCCCC),
            rope = Color(0xFF38D8D8),
            gold = Color(0xFFFFD700),
            goldHighlight = Color(0xFFFFF080),
            runnerBody = Color(0xFFFFFFFF),
            runnerHead = Color(0xFFFFCCAA),
            guardBody = Color(0xFFE02020),
            guardHead = Color(0xFFFFB090),
            escapeLadder = Color(0xFF40FF40),
            hudText = Color(0xFF40FF40),
            hudLabel = Color(0xFF20AA20),
            bezelBackground = Color(0xFF141820),
            buttonBackground = Color(0xFF1E2838),
            buttonBorder = Color(0xFF40FF40),
            buttonText = Color(0xFFFFFFFF)
        )

        val COMMODORE_64 = GamePalette(
            type = PaletteType.COMMODORE_64,
            background = Color(0xFF000000),
            brickMain = Color(0xFF884040),
            brickHighlight = Color(0xFFB86868),
            brickMortar = Color(0xFF381818),
            solidRockMain = Color(0xFF505050),
            solidRockHighlight = Color(0xFF787878),
            ladder = Color(0xFF70C8C8),
            ladderRung = Color(0xFF90E8E8),
            rope = Color(0xFF70C8C8),
            gold = Color(0xFFE0D040),
            goldHighlight = Color(0xFFFFF070),
            runnerBody = Color(0xFFFFFFFF),
            runnerHead = Color(0xFFFFB090),
            guardBody = Color(0xFF6844FC),
            guardHead = Color(0xFFA088FC),
            escapeLadder = Color(0xFF70E870),
            hudText = Color(0xFF70C8C8),
            hudLabel = Color(0xFF488888),
            bezelBackground = Color(0xFF101018),
            buttonBackground = Color(0xFF242438),
            buttonBorder = Color(0xFF70C8C8),
            buttonText = Color(0xFFFFFFFF)
        )

        val IBM_PC_CGA = GamePalette(
            type = PaletteType.IBM_PC_CGA,
            background = Color(0xFF000000),
            brickMain = Color(0xFFAA0000),
            brickHighlight = Color(0xFFFF5555),
            brickMortar = Color(0xFF550000),
            solidRockMain = Color(0xFF00AAAA),
            solidRockHighlight = Color(0xFF55FFFF),
            ladder = Color(0xFFFFFFFF),
            ladderRung = Color(0xFF55FFFF),
            rope = Color(0xFF55FFFF),
            gold = Color(0xFFFF55FF),
            goldHighlight = Color(0xFFFFFFFF),
            runnerBody = Color(0xFFFFFFFF),
            runnerHead = Color(0xFF55FFFF),
            guardBody = Color(0xFFFF5555),
            guardHead = Color(0xFFFF55FF),
            escapeLadder = Color(0xFF55FF55),
            hudText = Color(0xFF55FFFF),
            hudLabel = Color(0xFF00AAAA),
            bezelBackground = Color(0xFF101014),
            buttonBackground = Color(0xFF202028),
            buttonBorder = Color(0xFF55FFFF),
            buttonText = Color(0xFFFFFFFF)
        )

        val ARCADE_RETRO = GamePalette(
            type = PaletteType.ARCADE_RETRO,
            background = Color(0xFF080C14),
            brickMain = Color(0xFFB84414),
            brickHighlight = Color(0xFFE86C30),
            brickMortar = Color(0xFF381404),
            solidRockMain = Color(0xFF384458),
            solidRockHighlight = Color(0xFF5C6C88),
            ladder = Color(0xFFF4F4F4),
            ladderRung = Color(0xFFC8C8C8),
            rope = Color(0xFF28C8D8),
            gold = Color(0xFFFFC800),
            goldHighlight = Color(0xFFFFF480),
            runnerBody = Color(0xFFF8F8F8),
            runnerHead = Color(0xFFFFCCAA),
            guardBody = Color(0xFFE82828),
            guardHead = Color(0xFFFF9070),
            escapeLadder = Color(0xFF30E860),
            hudText = Color(0xFFFFCC00),
            hudLabel = Color(0xFFFFAA00),
            bezelBackground = Color(0xFF121620),
            buttonBackground = Color(0xFF1E2638),
            buttonBorder = Color(0xFFFFCC00),
            buttonText = Color(0xFFFFFFFF)
        )

        fun forType(type: PaletteType): GamePalette = when (type) {
            PaletteType.APPLE_II -> APPLE_II
            PaletteType.COMMODORE_64 -> COMMODORE_64
            PaletteType.IBM_PC_CGA -> IBM_PC_CGA
            PaletteType.ARCADE_RETRO -> ARCADE_RETRO
        }
    }
}
