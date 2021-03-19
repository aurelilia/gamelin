# Gamelin
A Gameboy emulator written in Kotlin, able to run on desktop (LibGDX + VisUI) and in browser (KORGE).

## Status
The emulator is in an early, but usable state. At the moment, the following is implemented and works:

##### Console
- Full SM83 CPU instruction set (All blargg cpu_instr tests pass)
- All of the PPU except the window and some finer details
- Most of the timers as well as PPU interrupts
- Working keyboard input, mapped to arrow keys, Enter, Space, X and Z
- Full APU/Sound emulation (7/12 blargg dmg_sound tests pass)
- Most MBC1 cartridge mapper behavior (MBC3/5 still missing)
- Game saving support (to `.sav` file on desktop, into local browser storage on web)

##### Desktop Version
- Reset and ROM changing support
- Instruction Set Viewer
- Tileset and BG Map Viewers
- Cartridge Info Viewer
- Debugger with support for:
    - PC and write breakpoints
    - Memory, register and stack view
    - Emulator halting / Line-by-line debugging
    - Per-instruction CPU/register state logging

##### Web Version
- Underlying emulator works fully in browser
- Everything except sound working fully, sound is choppy

### Working/Tested games
##### Perfect with no observable issues
- Tic-Tac-Toe (1996, Norman Nithman)
- Dr. Mario (Global version, 1990, Nintendo)
- The Legend of Zelda: Link's Awakening (Global, 1993, Nintendo)
    - Played until shortly after obtaining your sword at the shore

##### Small sound/bootup issues, otherwise perfect
- Tetris (JP and Global versions, 1989, Nintendo)
    - The "fail" sound effect played while the playing field is filled does not play
- Super Mario Land (Global, Nintendo)
    - Background music is off at times, missing some effects in it
- Pokemon Blue (Global, 1996, Game Freak)
    - Game display some garbage before copyrights/GF logo/intro
    - Gameplay seems to work fine (tested until getting the Pokedex)
    - Some music has missing sound effects
    - Menu screen is a bit buggy on first load
- Donkey Kong Land (Global, 1995, Nintendo/RARE)
    - Game displays garbage on boot
    - Tested for the first 2 levels: perfect

##### Playable with some small other issues
- Tetris DX (Global, 1998, Nintendo)
    - Game briefly displays garbage on boot
    - Some parts of the UI are blacked out
    - Some sound effects missing

##### Somewhat playable with big issues
- Donkey Kong (Global, 1994, Nintendo)
    - Game displays some garbage before copyright/intro
    - Works well until starting first level, most of map is invisible and
    sprites are cut off (still playable though)

##### Does not reach gameplay/not playable
- Kirby's Dream Land (Global, Nintendo/HAL)
    - Title screen works with buggy Kirby sprite (missing 8x16-sprites support)
    - Starting gameplay hangs on empty first level or white screen, inconsistent (interrupt timing issue?)

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
