# jvgmtrans
VGM to MIDI converter in Java.

## Usage
    > jvgmtrans.bat VGMFILE.vgm  
    output: VGMFILE_ChipName.mid

example:

    > jvgmtrans.bat OutRun_MagicalSoundShoweer.vgm
    output: OutRun_MagicalSoundShoweer_YM2151.vgm, OutRun_MagicalSoundShoweer_SegaPCM.vgm

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
* You *MUST* arrenge MIDI files with MIDI sequencer to listen.  
1.Merge FMs and PCMs MIDI files.  
2.Edit Tone.  
3.Assign Drum note to original note.  

