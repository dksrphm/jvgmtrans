package jvgmtrans.chips;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import jvgmtrans.Utils;
import jvgmtrans.VGMStatus;

public class V2mYM2151 {
	String ChipName = "YM2151";
	// 同時発声数8
	int Channels = 8;
	Track[] tracks = new Track[Channels];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;
	// オクターブ
	int[] octave = new int[Channels];
	int[] noteNo = new int[Channels];
	// Frequency LSB/MSB
	int[] freqL = new int[Channels];
	int[] freqM = new int[Channels];
	int[] freq = new int[Channels];
	// Left/Right Output
	int[] outL = new int[Channels];
	int[] outR = new int[Channels];
	// panpot
	int[] panpot = new int[Channels];
	// Connection(algorithm)
	int[] Con = new int[Channels];
	// Volume
	int[] volume = new int[Channels];
	// Total Level
	// ch(8) * op(4)
	int[][] Tl = new int[Channels][4];
	// Program No
	//int[] programNo = new int[Channels];
	String[] toneStr = {"C", "C+", "D", "D+", "E", "F", "F+", "G", "G+", "A", "A+", "B"};
	// register note -> Midi note
	int[] note2Midinote = {1, 2, 3, 0, 4, 5, 6, 0, 7, 8, 9, 0, 10, 11, 12, 0};
	// NOTE: "C+", "D", "D+", "", "E", "F", "F+", "", "G", "G+", "A", "", "A+", "B", "C"
	//        0     1    2     3   4    5    6     7   8    9     A    B   C     D    E

	// KeyOn -> KeyOn や KeyOff -> KeyOffの影響を除外するために必要
	int[] keyOnFlg = new int[Channels];
	int[] curOctave = new int[Channels];
	int[] curNoteNo = new int[Channels];
	int[] curPitchBend = new int[Channels];
	
	// MIDIシーケンス
	Sequence seq;

	public V2mYM2151() {
		// TODO 自動生成されたコンストラクター・スタブ
		//beginTrack = 0;
		midiOutFlg = false;
		for (int i = 0; i < Channels; i++){
			octave[i] = 0;
			noteNo[i] = 0;
			freqL[i] = 0;
			freqM[i] = 0;
			freq[i] = 0;
			outL[i] = 0;
			outR[i] = 0;
			Con[i] = 0;
			panpot[i] = 64;
			keyOnFlg[i] = 0;
			curNoteNo[i] = -1;
			curPitchBend[i] = 8192;
			volume[i] = 0;
			for (int j = 0; j < 4; j++){
				Tl[i][j] = 127;
			}
		}
	}
	public void v2m(byte cmd, byte aa, byte dd, VGMStatus vgmStatus){
		//YM2151 ch -> 内部では0-7として扱い、トラック名表示は1-8
		//MIDI track -> 0-7
		int ch;
		ShortMessage message;
		long tick = vgmStatus.getMidiTicks();

		// v2mメソッドが呼ばれたなら、Midi出力フラグを有効にする
		midiOutFlg = true;

		if (seq == null){
			// Sequenceオブジェクトがなければ作る
			// MIDIシーケンスを作成する
			// 分解能は初期値480
			try {
				seq = new Sequence(Sequence.PPQ, vgmStatus.getMidiResolution());
			} catch (InvalidMidiDataException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			// 初期化する
			Utils.initMidiSeq(seq, vgmStatus);
			// トラックもチャンネル数分作成する
			Utils.createTracks(seq, tracks, Channels, ChipName, vgmStatus);
		}
		
		switch (aa){
		case 0x08:
			// Key on/off
			ch = (dd & 0x07); /* 00000111 */
			byte keyOnBit = (byte) ((dd & 0x78) >> 3);
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x ch:%02x keyOnBit:%02x noteNo[%d]:%02x\n", ChipName, aa, dd, ch, keyOnBit, ch, noteNo[ch]);
			}

			if (keyOnBit != 0){ /* 01111000 */
				// Key on
				if (keyOnFlg[ch] == 0){
					// KeyOff -> KeyOn
					keyOnFlg[ch] = 1;
					try {
						message = new ShortMessage();
						message.setMessage(ShortMessage.NOTE_ON, ch, noteNo[ch], volume[ch]);
						tracks[ch].add(new MidiEvent(message, tick));
						curOctave[ch] = octave[ch];
						curNoteNo[ch] = noteNo[ch];
					} catch (Exception e){
						e.printStackTrace();
					}
				} else {
					// KeyOn -> KeyOn
				}
			} else {
				// Key off
				if (keyOnFlg[ch] == 1){
					// KeyOn -> KeyOff
					keyOnFlg[ch] = 0;
					try {
						message = new ShortMessage();
						message.setMessage(ShortMessage.NOTE_OFF, ch, curNoteNo[ch], 0);
						tracks[ch].add(new MidiEvent(message, tick));
					} catch (Exception e){
						e.printStackTrace();
					}
					curOctave[ch] = -1;
					curNoteNo[ch] = -1;
				} else {
					//KeyOff -> KeyOff
				}
			}
			break;
		case (byte) 0x20:
		case (byte) 0x21:
		case (byte) 0x22:
		case (byte) 0x23:
		case (byte) 0x24:
		case (byte) 0x25:
		case (byte) 0x26:
		case (byte) 0x27:
			// panpot and connection algorithm
			// panpot
			ch = aa - (byte)0x20;
			outL[ch] = (dd & 0x40) >> 6;
			outR[ch] = (dd & 0x80) >> 7;
			// LRがON -> SMFではPAN 64
			// LのみON -> SMFではPAN 0
			// RのみON -> SMFではPAN 127
			if (outL[ch] == 1){
				if (outR[ch] == 1){
					panpot[ch] = 64;
				} else {
					panpot[ch] = 0;
				}
			} else {
				if (outR[ch] == 1){
					panpot[ch] = 127;
				} else {
					//no tone?
					panpot[ch] = 64;
				}
			}
			// connection algorithm
			Con[ch] = (dd & 0x07);
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x ch:%02x panpot[%d]:%02x Con[%d]:%02x\n", ChipName, aa, dd, ch, ch, panpot[ch], ch, Con[ch]);
			}
			try{
				message = new ShortMessage();
				message.setMessage(ShortMessage.CONTROL_CHANGE, ch, 0x0a, panpot[ch]);
				tracks[ch].add(new MidiEvent(message, tick));
			} catch (Exception e){
				e.printStackTrace();
			}
			break;
		case (byte) 0x28:
		case (byte) 0x29:
		case (byte) 0x2A:
		case (byte) 0x2B:
		case (byte) 0x2C:
		case (byte) 0x2D:
		case (byte) 0x2E:
		case (byte) 0x2F:
			// Key Code
			// 01234567
			//  OCT(3bit)
			//     NOTE(4bit)
			ch = aa - (byte)0x28;
			octave[ch] = (dd & 0x70) >> 4;
			noteNo[ch] = (octave[ch] + 1) * 12 + note2Midinote[(dd & 0x0F)];
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x ch:%02x octave[%d]:%02x noteNo[%d]:%02x\n", ChipName, aa, dd, ch, ch, octave[ch], ch, noteNo[ch]);
			}
			break;
		case (byte) 0x30:
		case (byte) 0x31:
		case (byte) 0x32:
		case (byte) 0x33:
		case (byte) 0x34:
		case (byte) 0x35:
		case (byte) 0x36:
		case (byte) 0x37:
			// Key Fraction
			ch = aa - (byte)0x30;
			int keyFraction = (dd & 0xFC) >> 2;
			int pitchBend = (int)((double)keyFraction / 64.0 * (8192 / vgmStatus.getMidiPitchBend())) + 8192;
			try {
				message = new ShortMessage();
				message.setMessage(ShortMessage.PITCH_BEND, ch, pitchBend & 0x7F, (pitchBend >> 7) & 0x7F);
				tracks[ch].add(new MidiEvent(message, tick));
				curPitchBend[ch] = pitchBend;
			} catch (Exception e){
				e.printStackTrace();
			}

			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x ch:%02x keyFraction:%02x pitchbend=%d\n", ChipName, aa, dd, ch, keyFraction, pitchBend);
			}
			break;
		case (byte) 0x60:
		case (byte) 0x61:
		case (byte) 0x62:
		case (byte) 0x63:
		case (byte) 0x64:
		case (byte) 0x65:
		case (byte) 0x66:
		case (byte) 0x67:
		case (byte) 0x68:
		case (byte) 0x69:
		case (byte) 0x6A:
		case (byte) 0x6B:
		case (byte) 0x6C:
		case (byte) 0x6D:
		case (byte) 0x6E:
		case (byte) 0x6F:
		case (byte) 0x70:
		case (byte) 0x71:
		case (byte) 0x72:
		case (byte) 0x73:
		case (byte) 0x74:
		case (byte) 0x75:
		case (byte) 0x76:
		case (byte) 0x77:
		case (byte) 0x78:
		case (byte) 0x79:
		case (byte) 0x7A:
		case (byte) 0x7B:
		case (byte) 0x7C:
		case (byte) 0x7D:
		case (byte) 0x7E:
		case (byte) 0x7F:
			ch = (aa - (byte)0x60) % 8;
			int op = (aa - (byte)0x60) / 8;
			Tl[ch][op] = dd & 0x7F;
			// calc volume from Tl and algorithm
			// Tl 0:max 127:min
			if (0 <= Con[ch] && Con[ch] <=3){
				volume[ch] = 127 - Tl[ch][3];
			} else if (Con[ch] == 4){
				volume[ch] = 127 - (Tl[ch][1] + Tl[ch][3]) / 2;
			} else if (5 <= Con[ch] && Con[ch] <=6){
				volume[ch] = 127 - (Tl[ch][1] + Tl[ch][2] + Tl[ch][3]) / 3;
			} else {
				volume[ch] = 127 - (Tl[ch][0] + Tl[ch][1] + Tl[ch][2] + Tl[ch][3]) / 4;
			}
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x ch:%02x op:%02x Tl[%d][%d]:%02x volume[%d]:%d\n", ChipName, aa, dd, ch, op, ch, op, Tl[ch][op], ch, volume[ch]);
			}
			break;

		case (byte) 0x14:
			// timer.
			break;
		default:
			System.out.printf("%s aa:%02x dd:%02x NotImplemented\n", ChipName, aa, dd);
			;
		}

	}

	public void writeMidiFile(VGMStatus vgmStatus){
		if (midiOutFlg){
			try {
				MidiSystem.write(seq, 1, new java.io.File(vgmStatus.getMidiFileName() + "_" + ChipName + ".mid"));
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}
}
