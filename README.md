# Gamelin
A Gameboy (Color) emulator written in Kotlin, able to run on desktop (LibGDX + VisUI) and in browser (KORGE).

## Status
The emulator is in an early, but usable state. Original GameBoy (DMA) emulation is accurate enough for
almost all games, and while GameBoy Color (CGB) emulation still unfinished, many games already work.
The emulator itself still does not have many comfort features like fast-forward or save states though.

At the moment, the following is implemented and works:

##### Console
- Full SM83 CPU instruction set (All blargg cpu_instr tests pass)
- All of the PPU
- Full DIV/timer, all interrupts
- DMA transfer
- Working keyboard input, mapped to arrow keys, Enter, Space, X and Z
- Full APU/Sound emulation (8/12 blargg dmg_sound tests pass)
- Full MBC1, MBC2, MBC3 and MBC5 (RTC still missing)
- Game saving support (to `.sav` file on desktop, into local browser storage on web)
- Game Boy Color:
    - Automatic detection of GBC games
    - Additional WRAM/VRAM banks
    - All PPU changes/differences
    - GDMA & HDMA
    - Double Speed Mode

##### Desktop Version
- Reset and ROM changing support
- Basic save states (save/load entire console state at any point)
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
- Pokemon Blue* (Global, 1996, Game Freak)
    - Tested until Viridian Forest
- Pokemon Silver ** (Global, 2000, Game Freak)
    - Played until first GYM, including Bellsprout Tower
- Dr. Mario (Global version, 1990, Nintendo)
- Tetris (JP and Global versions, 1989, Nintendo)
- Tetris DX ** (Global, 1998, Nintendo)
- Pokemon Pinball ** (Global, 1999, Jupiter)
- Super Mario Land (Global, Nintendo)
    - Played until 2-1
- Super Mario Land 2 (Global, 1992, Nintendo)
    - Played the first level
- The Legend of Zelda: Link's Awakening (Global, 1993, Nintendo)
    - Played until shortly after obtaining your sword at the shore
- Kirby's Dream Land (Global, Nintendo/HAL)
    - Played until middle of stage 2
- Donkey Kong* (Global, 1994, Nintendo)
- Yoshi (Global, 1992, Nintendo)
- Tic-Tac-Toe (1996, Norman Nithman)

##### Small issues, fully playable
- Donkey Kong Land* (Global, 1995, Nintendo/RARE)
    - Palette on title screen is wrong
    - Tested for the first 2 levels
- The Legend of Zelda: Link's Awakening DX ** (Global, 1998, Nintendo)
    - Overworld palettes do not get restored properly after opening start menu, fixed by changing screen
    - Played until finding the toadstool in the mysterious woods
- Pokemon Yellow ** (Global, Game Freak)
    - None of the Pikachu sound effects actually play
    - Played until first rival fight
- Super Mario Bros. Deluxe (Global, 1999, Nintendo)
    - Bottom right of big mario sprite missing
    - Played until 1-3, other modes also briefly tested
    
##### Mostly playable but with big issues
- Pokemon Crystal ** (Global, 2001, Game Freak)
    - Opening any submenu on the START menu and closing it again often results
    in weird behavior: 
        - A white screen, BG map shows textbox "No windows available for popping.", [Related glitchcity entry](https://glitchcity.wiki/Event_data_debugging_messages)
        - Hanging for a few seconds before rebooting and identifying the console as DMG (with the "works only on GBC" message)
    - Played until after meeting Mr. Pokemon

##### Not really playable
- The Legend of Zelda: Oracle of Ages ** (Global, 2001, Nintendo)
    - Game runs at half speed (missing CGB double speed mode)
    - Some sprites in intro broken
    - Heavy graphical issues in-game
- Donkey Kong Country ** (Global, 2000, RARE)
    - Weird palette glitches on title screen
    - In-Game: Too slow, map is completely glitched and impossible to make out

##### Does not reach gameplay/not playable
- Wario Land 3 ** (Global, 2000, Nintendo)
    - Game hangs after selecting language

\* briefly displays garbage on boot
\** Tested in GBC mode

## Build
``` bash
# Run on desktop
./gradlew desktop:run

# Create jarfile
./gradlew desktop:dist
```

## Testing
Blargg, mooneye and acid2 ROMs can be run automatically using gradle:
```bash
gradle jvmTest
```

#### Blargg test results
- `cpu_instrs`, `mem_timing`, `mem_timing-2`: All pass
- `dmg_sound`: 8/12 pass (07, 09, 10, 12 fail)
- `cgb_sound`: 7/12 pass (07, 08, 09, 11, 12 fail)
- `instr_timing`: Both pass
- `interrupt_time`: Fails
- `oam_bug`: Everything but 3 and 7 fail (OAM bug unimplemented)

#### Mooneye test results
- `acceptance`: 26 out of 75 pass
- `emulator-only`: All pass (except MBC1 multicart; will not be supported)
- Others untested

#### Acid2
Both the `dmg-acid2` and `cgb-acid2` test written by Matt Currie pass (CGB in DMG mode untested).
Thank you to [@mattcurrie](https://github.com/mattcurrie) for creating them!

## Thanks To
- [Imran Nazar, for their series of blog posts on GB emulation](http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-The-CPU)
- [Michael Steil, for The Ultimate Game Boy Talk](https://media.ccc.de/v/33c3-8029-the_ultimate_game_boy_talk)
- [kotcrab, for creating the xgbc emulator I often used to confirm/understand fine behavior as well as VisUI](https://github.com/kotcrab/xgbc)
- [stan-roelofs, for their emulator, which I abridged the sound implementation from](https://github.com/stan-roelofs/Kotlin-Gameboy-Emulator)
- [Megan Sullivan, for her list of GB opcodes](https://meganesulli.com/blog/game-boy-opcodes)
- [gbdev.io for a list of useful resources](https://gbdev.io)
- blargg, Gekkio and mattcurie for their test ROMs and retrio for hosting blargg's ROMs
- You, for reading this :)
