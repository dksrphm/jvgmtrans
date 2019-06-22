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

public class V2mYM2203 {
	String ChipName = "YM2203";
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
	// Midi note and pitchbend from register
	// スペハリとかで、ノートONの最中に周波数変更->即ノートOFF->ノートON、という変なシーケンスがあるので追加
	// ノートOFFになった場合、この値に基づいてmidiNoteNo、midiPitchBendを再計算する
	// その場合、音の終端に変なピッチベンドが生じることになるが、仕方が無い
	int[] midiNoteNoR = new int[Channels];
	int[] midiPitchBendR = new int[Channels];
	// Midi note and pitchbend for midi sequence
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
	
	public V2mYM2203() {
		super();
		// TODO 自動生成されたコンストラクター・スタブ
		for (int i = 0; i < Channels; i++){
			octave[i] = 0;
			freqL[i] = 0;	// FM:freqL SSG:FineTune
			freqM[i] = 0;	// FM:freqM SSG:Coarse Tune
			freq[i] = 0;
			outL[i] = 0;
			outR[i] = 0;
			panpot[i] = 64;
			programNo[i] = 81;
			algorithm[i] = 0;
			volume[i] = 0;
			lastMidiNoteInf[i] = 0;
			midiNoteNo[i] = 0;
			midiPitchBend[i] = 0;
			keyOnFlg[i] = 0;
			for (int j = 0; j < 4; j++){
				Tl[i][j]=0;
			}
		}
	}
	public void v2m(byte cmd, byte aa, byte dd, VGMStatus vgmStatus, VGMFileInfo vgmFileInfo){
		//YM2203 ch -> 内部では0-5として扱い、トラック名表示は1-6
		//MIDI track -> 0-5
		// FM -> 0-2(Track 1-3)
		// SSG-> 3-5(Track 4-6)
		int ch;
		int op;
		ShortMessage message;
		long tick = vgmStatus.getMidiTicks();
		double midiNoteInf;
		double psgHz;

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
		case 0x00:
		case 0x02:
		case 0x04:
			// SSG
			// Fine tune
			ch = aa / 2 + 3;
			freqL[ch] = (dd & 0xff);
			//

			//SSGの場合、Coarse　tune、Fine tuneのどちらが変更になった場合でも処理は同じ。
			freq[ch] = (freqM[ch] << 8) + freqL[ch];
			System.out.printf("### freqM[%d]:%x freqL[%d]:%x freq[%d]:%x ###\n", 
					ch, freqM[ch], ch, freqL[ch], ch, freq[ch]);
			psgHz = calcPSGHz(freq[ch], vgmFileInfo.getYM2203Clock());
			midiNoteInf = Utils.hz2MidiNote(psgHz);
			//PSGの場合は、前回のmidiNoteInf(lastMidiNoteInf)とある程度離れたら、ノートオフにする
			//大きすぎるとピッチベンドで繋がってしまい、小さくしすぎるとノートオフ・ノートオンで繋がってしまう微妙な値
			if (Math.abs(lastMidiNoteInf[ch] - midiNoteInf) > 0.8){
				Utils.addNoteOff(tracks[ch], ch, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
				lastNoteNo[ch] = 0;
				keyOnFlg[ch] = 0;
			}
			lastMidiNoteInf[ch] = midiNoteInf;
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
				System.out.printf("%s freq[%d]:%02x midiNoteInf:%f midiNoteNo[%d]:%d midiPitchBend[%d]=%d keyOnFlg[%d]:%d\n"
						, ChipName, ch, freq[ch], midiNoteInf, ch, midiNoteNo[ch], ch, midiPitchBend[ch], ch, keyOnFlg[ch]);
			}

			if (keyOnFlg[ch] == 0){
				//Key off
				//set pitchbend
				Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
//				//add note on
				Utils.addNoteOn(tracks[ch], ch, vgmStatus.getMidiTicks(), midiNoteNo[ch], 127);
				keyOnFlg[ch] = 1;
				lastNoteNo[ch] = midiNoteNo[ch];
			} else {
				//Key on
				//set pitchbend
				Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
			}

			break;
		case 0x01:
		case 0x03:
		case 0x05:
			// SSG
			// Coarse tune
			ch = (aa - 1) / 2 + 3;
			freqM[ch] = (dd & 0x0f);
			//
			
			freq[ch] = (freqM[ch] << 8) + freqL[ch];
			System.out.printf("### freqM[%d]:%x freqL[%d]:%x freq[%d]:%x ###\n", 
					ch, freqM[ch], ch, freqL[ch], ch, freq[ch]);
			psgHz = calcPSGHz(freq[ch], vgmFileInfo.getYM2203Clock());
			midiNoteInf = Utils.hz2MidiNote(psgHz);
			//PSGの場合は、前回のmidiNoteInf(lastMidiNoteInf)とある程度離れたら、ノートオフにする
			//大きすぎるとピッチベンドで繋がってしまい、小さくしすぎるとノートオフ・ノートオンで繋がってしまう微妙な値
			if (Math.abs(lastMidiNoteInf[ch] - midiNoteInf) > 0.8){
				Utils.addNoteOff(tracks[ch], ch, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
				lastNoteNo[ch] = 0;
				keyOnFlg[ch] = 0;
			}
			lastMidiNoteInf[ch] = midiNoteInf;
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
				System.out.printf("%s freq[%d]:%02x midiNoteInf:%f midiNoteNo[%d]:%d midiPitchBend[%d]=%d keyOnFlg[%d]:%d\n"
						, ChipName, ch, freq[ch], midiNoteInf, ch, midiNoteNo[ch], ch, midiPitchBend[ch], ch, keyOnFlg[ch]);
			}

			if (keyOnFlg[ch] == 0){
				//Key off
				//set pitchbend
				Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
//				//add note on
				Utils.addNoteOn(tracks[ch], ch, vgmStatus.getMidiTicks(), midiNoteNo[ch], 127);
				keyOnFlg[ch] = 1;
				lastNoteNo[ch] = midiNoteNo[ch];
			} else {
				//Key on
				//set pitchbend
				Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
			}
			break;
		case 0x07:
			// SSG
			// In/Out, #Noise, #Tone(key on/off)
			int toneCh[] = new int[6];
			toneCh[3] = (dd & 0x01);
			toneCh[4] = (dd & 0x02);
			toneCh[5] = (dd & 0x04);
			//ch = (dd & 0x07);
			for (int i = 3; i <= 5; i++) {
				if (keyOnFlg[i] == 0) {
					if (toneCh[i] == 0) {	// 0 -> KeyOn 1
						// KeyOff -> KeyOn
						keyOnFlg[i] = 1;
						if (vgmStatus.getDebug()){
							System.out.printf("%s aa:%02x dd:%02x KeyOnch:%d freqL[%d]:%02x freqM[%d]:%02x\n"
									, ChipName, aa, dd, i, i, freqL[i], i, freqM[i]);
						}
						Utils.addNoteOn(tracks[i], i, vgmStatus.getMidiTicks(), midiNoteNo[i], volume[i]);
						lastNoteNo[i] = midiNoteNo[i];
					}
				} else {
					if (toneCh[i] != 0) {
						// KeyOn -> KeyOff
						keyOnFlg[i] = 0;
						if (vgmStatus.getDebug()){
							System.out.printf("%s aa:%02x dd:%02x KeyOnch:%d freqL[%d]:%02x freqM[%d]:%02x\n"
									, ChipName, aa, dd, i, i, freqL[i], i, freqM[i]);
						}
						Utils.addNoteOff(tracks[i], i, vgmStatus.getMidiTicks(), lastNoteNo[i]);
						lastNoteNo[i] = 0;
					}
				}
				;
			}
			break;
		case 0x08:
		case 0x09:
		case 0x0a:
			// SSG
			// Volume
			ch = (aa - 0x08) + 3;
			volume[ch] = (dd & 0x0f) * 8;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x Volume[%d]:%d\n"
						, ChipName, aa, dd, ch, volume[ch]);
			}
			// volume=0 -> KeyOff 
			if (volume[ch] < 1) {
				Utils.addNoteOff(tracks[ch], ch, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
				lastNoteNo[ch] = 0;
			}
			break;
		case 0x28:
			// FM
			// Key on/off
			ch = (dd & 0x03); /* 00000011 */
			byte keyOnBit = (byte) ((dd & 0xf0) >> 4);
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x KeyOnCh:%d midiPitchBend[%d]:%d midiNoteNo[%d]:%d\n"
						, ChipName, aa, dd, ch, ch, midiPitchBend[ch], ch, midiNoteNo[ch]);
			}
			if (keyOnBit != 0){ /* 11110000 */
				// Key on
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
				//スペハリ対応
				//キーOFF時は最新のmidiNoteNoR、midiPitchBendRでリセット
				midiNoteNo[ch] = midiNoteNoR[ch];
				midiPitchBend[ch] = midiPitchBendR[ch];
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
			// FM
			// OP total level
			ch = (aa - (byte)0x40) % 0x04;
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
			ch = (aa - (byte)0xB0);
			algorithm[ch] = dd & 0x07;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x algorithm[%d]:%02x\n", ChipName, aa, dd, ch, algorithm[ch]);
			}
			break;
		case (byte) 0xA0:
		case (byte) 0xA1:
		case (byte) 0xA2:
			// Frequency number LSB
			// KeyOn中はピッチベンド
			// KeyOff中は値を変更するだけ
			ch = (aa - (byte)0xA0);
			// signed -> unsigned
			freqL[ch] = dd & 0xff;

			// freqM、freqLから周波数を計算
			freq[ch] = freqM[ch] * 256 + freqL[ch];

			// The frequency is a 14-bit number that should be set high byte, low byte (e.g. A4H then A0H).
			// よってA4Hに設定された場合は何もせず、A0H側に設定された時に始めて処理をする
			double fmHz = hzYM2203(vgmFileInfo.getYM2203Clock() / 72, freq[ch], octave[ch]);
			midiNoteInf = Utils.hz2MidiNote(fmHz);
			midiNoteNo[ch] = (int)Math.floor(midiNoteInf);
			midiPitchBend[ch] = (int)((midiNoteInf - midiNoteNo[ch]) * 8192 / vgmStatus.getMidiPitchBend());
			//MIDIノート、ピッチベンドを、MIDIノートの近い方の値に揃える
			if ((8192 / vgmStatus.getMidiPitchBend() / 2) < midiPitchBend[ch]){
				midiNoteNo[ch]++;
				midiPitchBend[ch] = -1 * (8192 / vgmStatus.getMidiPitchBend() - midiPitchBend[ch]);
			}
			//スペハリ対応で、MSB、LSBから得られるノートNO、ピッチベンドをmidiNoteR、midiPitchBendRとして保存
			midiNoteNoR[ch] = midiNoteNo[ch];
			midiPitchBendR[ch] = midiPitchBend[ch];
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
			ch = (aa - (byte)0xA4);
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

	private double calcPSGHz(int freq, int clockYM2203){
		double hz;
		if (freq == 0){
			hz = 0;
		} else {
			hz = clockYM2203 / (64 * freq);
		}
		System.out.printf("freq:%d, clock:%d hz:%f\n", freq, clockYM2203, hz);
		return hz;
	}

	private double hzYM2203(double fsam2203, double freq, double octave){
		return freq * fsam2203 * Math.pow(2, octave - 21);
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
