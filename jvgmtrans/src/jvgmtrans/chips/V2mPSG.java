package jvgmtrans.chips;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
//import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import jvgmtrans.Utils;
import jvgmtrans.VGMFileInfo;
import jvgmtrans.VGMStatus;

// SN76489の方のPSG
// AY-3-8910ではない

public class V2mPSG {
	String ChipName = "PSG";
	// 同時発声数4(トーン3+ノイズ1)
	int Channels = 4;
	Track[] tracks = new Track[Channels];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;

	int[] freq = new int[Channels];
	int[] freqH = new int[Channels];
	int[] freqL = new int[Channels];
	int[] volume = new int[Channels];
	int[] keyOnFlg = new int[Channels];
	int[] lastNoteNo = new int[Channels];
	double[] lastMidiNoteInf = new double[Channels];
	// 2バイトのコマンド用。直前で何のコマンドを拾ってたか。enumを使ってみる
	int lastCmd;
	int ch;

	int midiNoteNo[] = new int[Channels];
	int midiPitchBend[] = new int[Channels];
	
	// MIDIシーケンス
	Sequence seq;

	public V2mPSG() {
		super();
		// TODO 自動生成されたコンストラクター・スタブ
		midiOutFlg = false;
		for (int i = 0; i < Channels; i++){
			freq[i] = 0;
			freqH[i] = 0;
			freqL[i] = 0;
			volume[i] = 0;
			keyOnFlg[i] = 0;
			lastNoteNo[i] = 0;
		}
		ch = 0;
	}
	public void v2m(byte cmd, byte dd, VGMStatus vgmStatus, VGMFileInfo vgmFileInfo){
		//MetaMessage mmsg;
		ShortMessage smsg;

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

		// unsigned -> signed
		int udd = dd & 0xff;
		if ((udd & 0x80) != 0){
			switch (dd & 0xf0){
			case 0x80:
				// ch0 tone freqL
			case 0xa0:
				// ch1 tone freqL
			case 0xc0:
				// ch2 tone freqL
				// ch=lastCmd
				// 0x80: lastCmd=0 0xa0: lastCmd=1 0xc0: lastCmd=2
				lastCmd = ((dd & 0xf0) - 0x80) / 0x20;
				freqL[lastCmd] = udd & 0x0f;
				if (vgmStatus.getDebug()){
					System.out.printf("PSG freqL[%d]:%d\n", lastCmd, freqL[lastCmd]);
				}
				break;
			case 0x90:
				// ch0 volume
			case 0xb0:
				// ch1 volume
			case 0xd0:
				// ch2 volume
				// PSGではノートオン時のベロシティではなく、トラックボリュームを変更する
				// 0x90: ch=0 0xb0: ch=1 0xd0: ch=2
				ch = ((dd & 0xf0) - 0x90) / 0x20;
				volume[ch] = udd & 0x0f;
				if (vgmStatus.getDebug()){
					System.out.printf("PSG volume[%d]:%d\n", ch, volume[ch]);
				}
				Utils.addVolumeCC(tracks[ch], ch, vgmStatus.getMidiTicks(), calcPSGvol2MidiVol(volume[ch]));
				
				if (volume[ch] == 0x0f){
					Utils.addNoteOff(tracks[ch], ch, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
					lastNoteNo[ch] = 0;
					keyOnFlg[ch] = 0;
				} else {
					if (keyOnFlg[ch] == 0){
						//Key off -> Key on
						//set pitchbend
						Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
						//add note on
						// PSGではノートオン時はベロシティ127固定とする。音量はトラックボリュームで変更する。
						Utils.addNoteOn(tracks[ch], ch, vgmStatus.getMidiTicks(), midiNoteNo[ch], 127);
						keyOnFlg[ch] = 1;
						lastNoteNo[ch] = midiNoteNo[ch];
					} else {
						//Key on -> Key on
						//set pitchbend
						Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
					}

				}
				break;
			case 0xe0:
				// noise control
				if (vgmStatus.getDebug()){
					System.out.printf("PSG noiseCtrl:%02x\n", dd);
				}
				break;
			case 0xf0:
				// noise volume
				ch = 3;
				volume[ch] = udd & 0x0f;
				Utils.addVolumeCC(tracks[ch], 9, vgmStatus.getMidiTicks(), calcPSGvol2MidiVol(volume[ch]));
				// ハイハット音に変換
				// VRDXのReplayでの利用状況から、25以下ならノートオフとする
				if (calcPSGvol2MidiVol(volume[3]) <= 25){
					// Note off
					if (keyOnFlg[3] != 0){
						try {
							smsg = new ShortMessage();
							smsg.setMessage(ShortMessage.NOTE_OFF, 9, 44, 0);
							tracks[3].add(new MidiEvent(smsg, vgmStatus.getMidiTicks()));
						} catch (Exception e){
							e.printStackTrace();
						}
					}
					keyOnFlg[3] = 0;
				} else {
					if (keyOnFlg[3] == 0){
						try {
							// Note on
							smsg = new ShortMessage();
							smsg.setMessage(ShortMessage.NOTE_ON, 9, 44, 127);
							tracks[3].add(new MidiEvent(smsg, vgmStatus.getMidiTicks()));
						} catch (Exception e) {
							e.printStackTrace();
						}
						keyOnFlg[3] = 1;
					}
				}
				if (vgmStatus.getDebug()){
					System.out.printf("PSG noiseVol:%02x\n", dd);
				}
				break;
			default:
				break;
			}
		} else {
			switch (lastCmd){
			case 0:
			case 1:
			case 2:
				//freq H
				ch = lastCmd;
				freqH[ch] = udd & 0x3f;
				freq[ch] = calcFreq(freqH[ch], freqL[ch]);

				double psgHz = calcPSGHz(freq[ch], vgmFileInfo.getSN76489Clock());
				double midiNoteInf = Utils.hz2MidiNote(psgHz);
				//PSGの場合は、前回のmidiNoteInf(lastMidiNoteInf)とある程度離れたら、ノートオフにする
				//大きすぎるとピッチベンドで繋がってしまい、小さくしすぎるとノートオフ・ノートオンで繋がってしまう微妙な値
				if (Math.abs(lastMidiNoteInf[ch] - midiNoteInf) > 0.8){
					Utils.addNoteOff(tracks[ch], lastCmd, vgmStatus.getMidiTicks(), lastNoteNo[ch]);
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
					System.out.printf("PSG freq[%d]:%02x midiNoteInf:%f midiNoteNo[%d]:%d midiPitchBend[%d]=%d keyOnFlg[%d]:%d\n", ch, freq[ch], midiNoteInf, ch, midiNoteNo[ch], ch, midiPitchBend[ch], ch, keyOnFlg[ch]);
				}

				if (keyOnFlg[ch] == 0){
					//Key off
					//set pitchbend
					Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
//					//add note on
					Utils.addNoteOn(tracks[ch], ch, vgmStatus.getMidiTicks(), midiNoteNo[ch], 127);
					keyOnFlg[ch] = 1;
					lastNoteNo[ch] = midiNoteNo[ch];
				} else {
					//Key on
					//set pitchbend
					Utils.addPitchBend(tracks[ch], ch, vgmStatus.getMidiTicks(), midiPitchBend[ch]);
				}
				break;
			default:
				break;
			}
		}
	}
	private int calcFreq(int freqH, int freqL){
		return (freqH << 4) + freqL;
	}
	private double calcPSGHz(int freq, int clockPSG){
		double hz;
		if (freq == 0){
			hz = 0;
		} else {
			hz = (clockPSG / 32) / freq;
		}
		return hz;
	}
	private int calcPSGvol2MidiVol(int volume){
		/*
		 int volume_table[16]={
				   32767, 26028, 20675, 16422, 13045, 10362,  8231,  6568,
				    5193,  4125,  3277,  2603,  2067,  1642,  1304,     0
				 };
		*/
		int[] volume_table = {127, 101, 80, 64, 50, 40, 32, 25, 
								20, 16, 12, 10,  8,  6,  5,  0};
		;
		if (volume_table.length < volume){
			return 127;
		} else {
			return volume_table[volume];
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
