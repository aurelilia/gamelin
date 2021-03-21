# Gamelin
A Gameboy (Color) emulator written in Kotlin, able to run on desktop (LibGDX + VisUI) and in browser (KORGE).

## Status
The emulator is in an early, but usable state. At the moment, the following is implemented and works:

##### Console
- Full SM83 CPU instruction set (All blargg cpu_instr tests pass)
- All of the PPU
- Most of the timers as well as PPU interrupts
- Working keyboard input, mapped to arrow keys, Enter, Space, X and Z
- Full APU/Sound emulation (7/12 blargg dmg_sound tests pass)
- Most MBC1 cartridge mapper behavior (MBC3/5 still missing)
- Game saving support (to `.sav` file on desktop, into local browser storage on web)
- Game Boy Color:
    - Detection of GBC games, support for running them in GBC mode
    - Additional WRAM/VRAM banks
    - Most of the additional PPU features, full color support

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
- Tetris (JP and Global versions, 1989, Nintendo)
- Super Mario Land (Global, Nintendo)
    - Played until 2-1
- Super Mario Land 2 (Global, 1992, Nintendo)
    - Played the first level
- The Legend of Zelda: Link's Awakening (Global, 1993, Nintendo)
    - Played until shortly after obtaining your sword at the shore
- Donkey Kong* (Global, 1994, Nintendo)
- Yoshi (Global, 1992, Nintendo)

##### Barely noticeable issues, fully playable
- Pokemon Blue* (Global, 1996, Game Freak)
    - Menu screen shows a line of garbage before sliding in Pokemon Blue logo
    - "Defeated pokemon" noise does not play
    - Tested until Viridian Forest
- Donkey Kong Land* (Global, 1995, Nintendo/RARE)
    - Palette on title screen is wrong
    - Tested for the first 2 levels: perfect

##### Fully playable but with obvious issues
- Tetris DX* (Global, 1998, Nintendo)
    - Some parts of the UI are blacked out
- Pokemon Silver ** (Global, 2000, Game Freak)
    - Some Pokemon sprites have a few incorrect/garbage tiles
    - Map flickers for a second when dialogue box is first displayed (also applies to menu)
        - Might be related to unimplemented CGB features? Did not occur pre-CGB support 
    - Otherwise works, played until after meeting Mr. Pokemon/Prof. Oak

##### Does not reach gameplay/not playable
- Kirby's Dream Land (Global, Nintendo/HAL)
    - Starting gameplay hangs on empty first level or white screen, inconsistent (interrupt timing issue?)

\* briefly displays garbage on boot
\** Works in GBC mode

## Build
``` bash
# Run on desktop
./gradlew desktop:run

# Create jarfile
./gradlew desktop:dist
```

## Testing
Blargg and mooneye ROMs can be run automatically using gradle:
```bash
gradle jvmTest
```

#### Blargg test results
- `cpu_instrs`: 11/11 pass
- `dmg_sound`: 7/12 pass (01, 07, 09, 10, 12 fail)
- `mem_timing`/`mem_timing-2`: 2/3 pass (03-modify fails)
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
