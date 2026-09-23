# LodeRunner

A remake of the 1983 puzzle-platformer *Lode Runner* for Android tablets. It's written in Kotlin with Jetpack Compose.

Collect every piece of gold, dig holes to trap the guards, then climb the hidden escape ladder to the top of the screen.

## Features

- **5 built-in levels:** The Vault, High Wire, The Pyramid, Gauntlet and The Fortress. Each is a 28 × 16 tile grid.
- **Classic mechanics:** you can't jump, so you dig down-left or down-right into brick instead.
  - Holes refill after about 6 seconds, crushing anything still inside.
  - Trapped guards drop their gold, and you can walk across their heads.
  - Once all the gold is collected, the escape ladders appear.
- **Level editor:** paint tiles onto the grid, name your level, save it, and playtest it right away. Your levels are saved on the device.
- **Retro color themes:** Apple II (the 1983 original), Commodore 64, IBM PC CGA and Arcade Classic.
- **CRT scanline effect:** can be turned on or off.
- **8-bit sound effects:** generated in code with `AudioTrack`, so the app has no audio files.
- **Touch and keyboard controls:** an on-screen D-pad with dig buttons, plus full support for a hardware keyboard.
- **High score:** saved on the device.

## Controls

| Action     | Touch        | Keyboard          |
|------------|--------------|-------------------|
| Move       | Left D-pad   | Arrow keys / WASD |
| Dig left   | `DIG L`      | Z / Q / J         |
| Dig right  | `DIG R`      | X / E / K         |
| Pause      | —            | Space / P         |
| Restart    | —            | R                 |

## Scoring

| Event                    | Points |
|--------------------------|--------|
| Collect gold             | 250    |
| Crush a guard in a hole  | 750    |
| Clear a level            | 1500   |

You start with 5 lives.

## Tile legend (level ASCII format)

Levels are written as 16 lines of ASCII text and parsed with `LevelData.fromAscii`. See [ClassicLevels.kt](app/src/main/java/com/example/loderunner/game/levels/ClassicLevels.kt).

| Char        | Tile                                         |
|-------------|----------------------------------------------|
| ` `         | Empty                                        |
| `#`         | Brick (can be dug)                           |
| `@`         | Solid rock (can't be dug)                    |
| `H`         | Ladder                                       |
| `-`         | Rope                                         |
| `G` `$` `*` | Gold                                         |
| `F`         | False brick (you fall through it)            |
| `E`         | Escape ladder (appears after all gold is collected) |
| `&`         | Runner start position                        |
| `M`         | Guard spawn point                            |

## Project structure

```
app/src/main/java/com/example/loderunner/
├── MainActivity.kt          # Entry point
├── Navigation.kt            # Navigation 3 routes
├── NavigationKeys.kt        # Menu, Game, LevelEditor, Settings, HowToPlay keys
├── theme/                   # Material 3 app theme
└── game/
    ├── core/GameEngine.kt   # Tick-based simulation: movement, digging, guard AI, collisions
    ├── model/               # TileType, LevelData, GameState, Runner, Enemy, DugHole
    ├── levels/              # ClassicLevels + LevelManager (custom levels & high score storage)
    ├── audio/SoundFxEngine.kt
    └── ui/                  # Compose screens: menu, game canvas, HUD, touch controls,
                             # dialogs, level editor, settings, how-to-play, palettes
```

The game loop runs inside `GameScreen` using `withFrameNanos`, and each frame advances `GameEngine.tick()` by one step. The engine is written as if frames arrive at about 60 FPS: hole and trap timers count in frames. `GameCanvas` draws the state with a Compose `Canvas`.

## Tech stack

- Kotlin 2.3, JVM toolchain 17
- Jetpack Compose (BOM 2026.03.01), Material 3
- AndroidX Navigation 3
- Android Gradle Plugin 9.0
- `minSdk` 24, `targetSdk` / `compileSdk` 36

## Building & running

Open the project in Android Studio and run the `app` configuration. You can also use the command line:

```bash
./gradlew assembleDebug
```

```bash
./gradlew installDebug
```

## Tests

- **Unit tests:** in `app/src/test`. `GameEngineTest` covers the game engine rules.
- **Instrumented UI tests:** in `app/src/androidTest`.

```bash
./gradlew test
```

```bash
./gradlew connectedAndroidTest
```
