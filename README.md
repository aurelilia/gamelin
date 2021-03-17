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

##### Emulator
- Reset and ROM changing support
- Instruction Set Viewer
- Tileset and BG Map Viewers
- Debugger with support for:
    - PC and write breakpoints
    - Memory, register and stack view
    - Emulator halting / Line-by-line debugging
    - Per-instruction CPU/register state logging

### Working/Tested games
##### Perfect with no observable issues
- Tic-Tac-Toe (1996, Norman Nithman)

##### Minor, barely noticeable issues that do not affect gameplay
- Tetris (JP and Global versions, 1989, Nintendo)
    - The "fail" sound effect played while the playing field is filled does
    not play, Music might be a bit off at times as well?
- Dr. Mario (Global version, 1990, Nintendo): 
    - Text of selected music in menu is invisible

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
