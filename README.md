# jvgmtrans
VGM to MIDI converter in Java.  
You can convert .vgm file(YM2151, YM2203, OKIM6259, etc...) to MIDI file.

## How to build:
    You need JDK 1.8 and Apache ant in your PC.

	Setup them and
    > ant

## Usage:
    > jvgmtrans.bat VGMFILE.vgm  
    output: VGMFILE_ChipName.mid

example:

    > jvgmtrans.bat OutRun_MagicalSoundShoweer.vgm
    output: OutRun_MagicalSoundShoweer_YM2151.mid, OutRun_MagicalSoundShoweer_SegaPCM.mid

    (SEGA OutRun uses YM2151 and SegaPCM)


## Chips
* YM2151
* YM2203
* YM2612
* PSG
* OKIM6295
* SegaPCM

## Convert specification
* Tempo: 120bpm, MIDI 1ch
* FMs:  
FM 1ch -&gt; MIDI 2ch, FM 2ch -&gt; MIDI 3ch,...  
Tone: Square Wave  
* PCMs:  
Convert to MIDI 11ch  
Map to note no 70-

## Limitation
* Notes in converted MIDI files will be correct, but tones would be incorrect, and drums would be silly.  
You *MUST* arrenge MIDI files with MIDI sequencer to listen correctly.  
1.Merge FMs and PCMs MIDI files into 1 MIDI file.  
2.Edit Tone.  
3.Assign Drum note to original note.  

