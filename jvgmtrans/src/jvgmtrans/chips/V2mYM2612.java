package jvgmtrans.chips;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import jvgmtrans.Utils;
import jvgmtrans.VGMFileInfo;
import jvgmtrans.VGMStatus;

public class V2mYM2612 {
	String ChipName = "YM2612";
	// 同時発声数6
	int Channels = 6;
	Track[] tracks = new Track[Channels];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;

	// オクターブ
	int[] octave = new int[Channels];
	// Frequency LSB/MSB
	int[] freqL = new int[Channels];
	int[] freqM = new int[Channels];
	int[] freq = new int[Channels];
	// Left/Right Output
	int[] outL = new int[Channels];
	int[] outR = new int[Channels];
	// panpot
	int[] panpot = new int[Channels];
	// Program No
	int[] programNo = new int[Channels];
	// algorithm
	int[] algorithm = new int[Channels];
	// Total level(op)
	int[][] Tl = new int[Channels][4];
	int[] volume = new int[Channels];
	double[] lastMidiNoteInf = new double[Channels];
	int[] midiNoteNo = new int[Channels];
	int[] midiPitchBend = new int[Channels];
	int[] lastNoteNo = new int[Channels];

	String[] toneStr = {"C", "C+", "D", "D+", "E", "F", "F+", "G", "G+", "A", "A+", "B"};
	// KeyOn -> KeyOn や KeyOff -> KeyOffの影響を除外するために必要
	int[] keyOnFlg = new int[Channels];
	
	// MIDIシーケンス
	Sequence seq;

	// 周波数の範囲と音階を変換するための配列
	// ただし周波数はfreqL、freqMから計算する方式としているので、実際には使ってない。
	// smspowerとかに書かれている値もあっているのかよく分からない
	// 計算の都合上、double型とする
	//                          >b    c    c+   d    d+   e    f    f+   g    g+    a     a+    b     <c    <c+
	double[] noteFreqRange = {568, 599, 635, 672, 713, 755, 800, 848, 898, 952, 1008, 1068, 1132, 1198, 1268, 1340};
	
	public V2mYM2612() {
		super();
		// TODO 自動生成されたコンストラクター・スタブ
		for (int i = 0; i < Channels; i++){
			octave[i] = 0;
			freqL[i] = 0;
			freqM[i] = 0;
			freq[i] = 0;
			outL[i] = 0;
			outR[i] = 0;
			panpot[i] = 64;
			programNo[i] = 81;
			algorithm[i] = 0;
			volume[i] = 0;
			lastMidiNoteInf[i] = 0;
			midiNoteNo[i] = 0;
			keyOnFlg[i] = 0;
			for (int j = 0; j < 4; j++){
				Tl[i][j]=0;
			}
		}
	}
	public void v2m(byte cmd, byte aa, byte dd, VGMStatus vgmStatus, VGMFileInfo vgmFileInfo){
		//YM2612 ch -> 内部では0-5として扱い、トラック名表示は1-6
		//MIDI track -> 0-5
		int ch;
		int op;
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
		case 0x28:
			// Key on/off
			ch = (dd & 0x07); /* 00000111 */
			if (ch >= 4)ch--;
			/*
			 * D2 D1 D0  masked ch      tracks
			 *  0  0  0  0      0       0
			 *  0  0  1  1      1       1
			 *  0  1  0  2      2       2
			 *  0  1  1  3      (not use)
			 *  1  0  0  4      3       3 
			 *  1  0  1  5      4       4
			 *  1  1  0  6      5       5
			 */
			byte keyOnBit = (byte) ((dd & 0xf0) >> 4);
			if (keyOnBit != 0){ /* 11110000 */
				// Key on
				if (vgmStatus.getDebug()){
					System.out.printf("%s aa:%02x dd:%02x KeyOnCh:%d midiPitchBend[%d]:%d midiNoteNo[%d]:%d\n"
							, ChipName, aa, dd, ch, ch, midiPitchBend[ch], ch, midiNoteNo[ch]);
				}
				// Note on
				if (keyOnFlg[ch] == 0){
					// KeyOff -> KeyOn
					keyOnFlg[ch] = 1;
					Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
					Utils.addNoteOn(tracks[ch], ch, vgmStatus.getMidiTicks(), midiNoteNo[ch], volume[ch]);
					lastNoteNo[ch] = midiNoteNo[ch];
				} else {
					// KeyOn -> KeyOn
					Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
				}
			} else {
				// Key off
				if (keyOnFlg[ch] == 1){
					// KeyOn -> KeyOff
					keyOnFlg[ch] = 0;
					Utils.addNoteOff(tracks[ch], ch, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
					lastNoteNo[ch] = 0;
				} else {
					//KeyOff -> KeyOff
				}
			}
			break;
		case (byte) 0x2A:
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x tick:%d DACdata:%d\n", ChipName, aa, dd, vgmStatus.getMidiTicks(), (dd & 0xff) - 127);
			}
			break;
		case (byte) 0x2B:
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x DACen:%d\n", ChipName, aa, dd, (dd & 0x80) >> 7);
			}
			break;
		case (byte) 0x40:
		case (byte) 0x41:
		case (byte) 0x42:
		case (byte) 0x44:
		case (byte) 0x45:
		case (byte) 0x46:
		case (byte) 0x48:
		case (byte) 0x49:
		case (byte) 0x4A:
		case (byte) 0x4C:
		case (byte) 0x4D:
		case (byte) 0x4E:
			// OP total level
			ch = 3 * (cmd - 0x52) + (aa - (byte)0x40) % 0x04;
			op = (aa - (byte)0x40) / 0x04;
			Tl[ch][op] = (dd & 0x7f) & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x volume[%d]:%02x\n", ChipName, aa, dd, ch, volume[ch]);
			}
			volume[ch] = Utils.calcVolumeFM(algorithm[ch], Tl[ch]);
			break;
		case (byte) 0xB0:
		case (byte) 0xB1:
		case (byte) 0xB2:
			// feedback and algorithm
			ch = 3 * (cmd - 0x52) + (aa - (byte)0xB0);
			algorithm[ch] = dd & 0x07;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x algorithm[%d]:%02x\n", ChipName, aa, dd, ch, algorithm[ch]);
			}
			break;
		case (byte) 0xB4:
		case (byte) 0xB5:
		case (byte) 0xB6:
			// panpot
			ch = 3 * (cmd - 0x52) + (aa - (byte)0xB4);
			outL[ch] = (dd & 0x80) >> 7;
			outR[ch] = (dd & 0x40) >> 6;
			// LRがON -> SMFではPAN 64
			// LのみON -> SMFではPAN 0
			// RのみON -> SMFではPAN 127
			int[][] panArray = {{64,127}, //outL=0 
								{0,64}};  //outL=1
			panpot[ch] = panArray[outL[ch]][outR[ch]];
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x panpot[%d]:%02x\n", ChipName, aa, dd, ch, panpot[ch]);
			}
			try{
				message = new ShortMessage();
				message.setMessage(ShortMessage.CONTROL_CHANGE, ch, 0x0a, panpot[ch]);
				tracks[ch].add(new MidiEvent(message, tick));
			} catch (Exception e){
				e.printStackTrace();
			}
			break;
		case (byte) 0xA0:
		case (byte) 0xA1:
		case (byte) 0xA2:
			// Frequency number LSB
			// KeyOn中はピッチベンド
			// KeyOff中は値を変更するだけ
			ch = 3 * (cmd - 0x52) + (aa - (byte)0xA0);
			// signed -> unsigned
			freqL[ch] = dd & 0xff;

			// freqM、freqLから周波数を計算
			freq[ch] = freqM[ch] * 256 + freqL[ch];

			// The frequency is a 14-bit number that should be set high byte, low byte (e.g. A4H then A0H).
			// よってA4Hに設定された場合は何もせず、A0H側に設定された時に始めて処理をする
			double fmHz = hzYM2612(vgmFileInfo.getYM2612Clock() / 72, freq[ch], octave[ch]);
			double midiNoteInf = Utils.hz2MidiNote(fmHz);
			midiNoteNo[ch] = (int)Math.floor(midiNoteInf);
			midiPitchBend[ch] = (int)((midiNoteInf - midiNoteNo[ch]) * 8192 / vgmStatus.getMidiPitchBend());
			//MIDIノート、ピッチベンドを、MIDIノートの近い方の値に揃える
			if ((8192 / vgmStatus.getMidiPitchBend() / 2) < midiPitchBend[ch]){
				midiNoteNo[ch]++;
				midiPitchBend[ch] = -1 * (8192 / vgmStatus.getMidiPitchBend() - midiPitchBend[ch]);
			}
			//ピッチベンドについて、現在発声中の音を基準に再計算
			if (lastNoteNo[ch] != 0){
				if (lastNoteNo[ch] != midiNoteNo[ch]){
					int noteNoDiff = midiNoteNo[ch] - lastNoteNo[ch];
					midiPitchBend[ch] = (8192 / vgmStatus.getMidiPitchBend()) * noteNoDiff + midiPitchBend[ch];
				}
			}
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x freqL[%d]:%02x freqM[%d]:%02x midiNoteInf:%f midiNoteNo[%d]:%d midiPitchBend[%d]=%d keyOnFlg[%d]:%d\n"
						, ChipName, aa, dd, ch, freqL[ch], ch, freqM[ch], midiNoteInf, ch, midiNoteNo[ch], ch, midiPitchBend[ch], ch, keyOnFlg[ch]);
			}
			//
			
			if (keyOnFlg[ch] == 1){
				// key on中ならピッチベンド
				Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
			} else {
				// 何もしない
			}
			break;
		case (byte) 0xA4:
		case (byte) 0xA5:
		case (byte) 0xA6:
			// Block, Frequency number MSB
			ch = 3 * (cmd - 0x52) + (aa - (byte)0xA4);
			octave[ch] = (byte) (dd & 0x38) >> 3;  /* 00111000 */
			freqM[ch] = (dd & 0x07); /* 00000111 */
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x freqM[%d]:%02x\n", ChipName, aa, dd, ch, freqM[ch]);
			}
			//ここではfreqMを設定するだけで終了。音はLSB側が設定されたら出す
			break;
		default:
			System.out.printf("%s aa:%02x dd:%02x NotImplemented\n", ChipName, aa, dd);
			;
		}
	}
	private double hzYM2612(double fsam2612, double freq, double octave){
		return freq * fsam2612 * Math.pow(2, octave - 22);
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
