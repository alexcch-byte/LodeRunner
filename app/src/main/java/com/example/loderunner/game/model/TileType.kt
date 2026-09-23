package com.example.loderunner.game.model

/**
 * Tile types for the 28x16 Lode Runner grid.
 */
enum class TileType(val id: Int, val charSymbol: Char) {
    EMPTY(0, ' '),
    BRICK(1, '#'),
    SOLID_ROCK(2, '@'),
    LADDER(3, 'H'),
    ROPE(4, '-'),
    GOLD(5, 'G'),
    FALSE_BRICK(6, 'F'),
    ESCAPE_LADDER(7, 'E');

    companion object {
        fun fromId(id: Int): TileType = entries.find { it.id == id } ?: EMPTY
        fun fromChar(char: Char): TileType = when (char) {
            'G', '$', '*' -> GOLD
            else -> entries.find { it.charSymbol == char } ?: EMPTY
        }
    }
}
