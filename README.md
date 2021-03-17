# Gamelin
A Gameboy emulator written in Kotlin with LibGDX and VisUI.

## Status
The emulator is in an early, but usable state. At the moment, the following is implemented and works:

##### Console
- Full SM83 CPU instruction set (All blargg cpu_instr tests pass)
- All of the PPU except the window and some finer details
- Most of the timers as well as PPU interrupts
- Working keyboard input, mapped to arrow keys, Enter, Space, X and Z
- Full APU/Sound emulation

##### Emulator
- Reset and ROM changing support
- Instruction Set Viewer
- Tileset and BG Map Viewers
- Debugger with support for:
    - PC and write breakpoints
    - Memory, register and stack view
    - Emulator halting / Line-by-line debugging
    - Per-instruction CPU/register state logging

#### Working/Tested games
##### Perfect with no observable issues
- Tic-Tac-Toe (1996, Norman Nithman)

##### Minor issues that do not affect gameplay
- Tetris (JP Version, 1989, Nintendo)
    - Music is slightly off at times, some sound effects do not play

##### Playable, with issues
- Dr. Mario (1990, Nintendo): 
    - Only one half of a pellet is actually recognized (both halves display)
    - Text of selected music in menu is invisible

##### Title screen, does not reach gameplay
- Tetris (Global Version, 1989, Nintendo)
    - Console hangs when pressing A on difficulty select

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
