# Gamelin
A Gameboy emulator written in Kotlin with LibGDX and VisUI.

## Status
The emulator is in an early, but usable state. At the moment, the following is implemented and works:

##### Console
- Full SM83 CPU instruction set (All blargg cpu_instr tests pass)
- All of the PPU except the window and some finer details
- Most of the timers as well as PPU interrupts
- Working keyboard input, mapped to arrow keys, Enter, Space, X and Z
- Full APU/Sound emulation (7/12 blargg dmg_sound tests pass)
- Most MBC1 cartridge mapper behavior (MBC3/5 still missing)

##### Emulator
- Reset and ROM changing support
- Instruction Set Viewer
- Tileset and BG Map Viewers
- Cartridge Info Viewer
- Debugger with support for:
    - PC and write breakpoints
    - Memory, register and stack view
    - Emulator halting / Line-by-line debugging
    - Per-instruction CPU/register state logging

### Working/Tested games
##### Perfect with no observable issues
- Tic-Tac-Toe (1996, Norman Nithman)
- Dr. Mario (Global version, 1990, Nintendo)

##### Minor, barely noticeable issues that do not affect gameplay
- Tetris (JP and Global versions, 1989, Nintendo)
    - The "fail" sound effect played while the playing field is filled does not play

##### Playable with some issues
- Pokemon Blue (Global, 1996, Game Freak)
    - Game display some garbage before copyrights/GF logo/intro
    - All big sprites are black squares (decompression routine broken?)
    - Game has a lot of flickering lines
    - Gameplay seems to work fine (tested until getting the Pokedex)

##### Somewhat playable with big issues
- Super Mario Land (Global, Nintendo)
    - Game does not update the background X scroll, making it almost impossible
    to properly tell where Mario is because the displayed map is incorrect

- Donkey Kong (Global, 1994, Nintendo)
    - Game display some garbage before copyright/intro
    - Works well until starting first level, most of map is invisible and
    sprites are cut off (still playable though)
    - Game has a lot of flickering lines

##### Does not reach gameplay/not playable
- Tetris DX (Global, 1998, Nintendo)
    - Game briefly displays garbage on boot
    - Title screen only allows selecting "Entry", pressing A/Start loads an empty
    tetris field and hangs (although reset button combo still works)

- Kirby's Dream Land (Global, Nintendo/HAL)
    - Title screen works with buggy Kirby sprite (missing 8x16-sprites support)
    - Starting gameplay hangs on empty first level

- The Legends of Zelda: Link's Awakening (Global, 1993, Nintendo)
    - Intro cutscene and title screen work with graphical glitches
    - Game hangs after pressing START on title screen

- Donkey Kong Land (Global, 1995, Nintendo/RARE)
    - Softlocks on map selection screen
    - Graphics are glitchy (wrong palettes, incorrect tiles)

- Yoshi (Global, 1992, Nintendo)
    - Only shows white screen

## Build
``` bash
# Run on desktop
./gradlew desktop:run

# Create jarfile
./gradlew desktop:dist
```

## Testing
Blargg ROMs can be run automatically using gradle:
```bash
gradle test
```

#### Blargg test results
- `cpu_instrs`: 11/11 pass
- `dmg_sound`: 7/12 pass (01, 07, 09, 10, 12 fail)
- Others: Not tested yet

## Thanks To
- [Imran Nazar, for their series of blog posts on GB emulation](http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-The-CPU)
- [Michael Steil, for The Ultimate Game Boy Talk](https://media.ccc.de/v/33c3-8029-the_ultimate_game_boy_talk)
- [kotcrab, for creating the xgbc emulator I often used to confirm/understand fine behavior as well as VisUI](https://github.com/kotcrab/xgbc)
- [trekawek, for coffee-gb, which I abridged the sound implementation from](https://github.com/trekawek/coffee-gb)
- [Megan Sullivan, for her list of GB opcodes](https://meganesulli.com/blog/game-boy-opcodes)
- [gbdev.io for a list of useful resources](https://gbdev.io)
- blargg and Gekkio for their test ROMs and retrio for hosting blargg's ROMs
- You, for reading this :)
