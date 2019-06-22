package jvgmtrans.chips;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import jvgmtrans.Utils;
//import jvgmtrans.Utils;
import jvgmtrans.VGMStatus;

public class V2mOKIM6295 {
	String ChipName = "OKIM6295";
	// 同時発声数1(全ての音を10チャンネルに出力する)
	int Channels = 1;

	Track[] tracks = new Track[10];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;

	// 2バイトコマンド中の1バイト目を読み込んであるか
	int cmd1;
	// sample number to trigger
	int smpNo;

	// ボリューム変換テーブル
	// 根拠は末尾に
	int[] vol_table = {127, 87, 63, 43, 31, 23, 15, 11, 7, 0, 0, 0, 0, 0, 0, 0};
	// MIDIシーケンス
	Sequence seq;

	public V2mOKIM6295() {
		// TODO 自動生成されたコンストラクター・スタブ
		midiOutFlg = false;
		cmd1 = 0;
		smpNo = 0;
	}
	
	public void createTracks0(Sequence seq){
		String trackName;
		// ドラムはトラック10
		try{
			tracks[0] = seq.createTrack();
			// トラックを初期化
			// トラック名を設定
			MetaMessage mmsg = new MetaMessage();
			trackName = ChipName;
			mmsg.setMessage(0x03 , trackName.getBytes(), trackName.length());
			MidiEvent me = new MidiEvent(mmsg, (long)10);
			tracks[0].add(me);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public void v2m(byte cmd, byte aa, byte dd, VGMStatus vgmStatus){
		//OKIM2151 MIDIトラックは10
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
			// トラックを10チャンネル作成する(10チャンネルしか使わない)
			Utils.createDrumTracks(seq, tracks, 10, ChipName);
		}

		if (cmd1 == 0){
			if (0 != (dd & 0x80)){
				// 1コマンド目
				cmd1 = 1;
				smpNo = dd & 0x7F;
				if (vgmStatus.getDebug()){
					System.out.printf("%s aa:%02x dd:%02x smpNo:%02x\n", ChipName, aa, dd, smpNo);
				}
			} else {
				// stop playing
				cmd1 = 0;
				int bitToStop = (dd & 0x78) >> 3;
				if (vgmStatus.getDebug()){
					System.out.printf("%s aa:%02x dd:%02x bitToStop:%02x\n", ChipName, aa, dd, bitToStop);
				}
			}
			;
		} else {
			// 2コマンド目
			cmd1 = 0;
			// indicate which voice
			int bitToIndicate = (dd & 0xF0) >> 4;
			// volume
			int volume = (dd & 0x0F);
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa:%02x dd:%02x bitToIndicate:%02x volume:%02d\n", ChipName, aa, dd, bitToIndicate, volume);
			}
			try {
				message = new ShortMessage();
				//message.setMessage(ShortMessage.NOTE_ON, 10 - 1, 35 + smpNo, 100);
				message.setMessage(ShortMessage.NOTE_ON, 10 - 1, 35 + smpNo, vol_table[volume]);
				tracks[10 - 1].add(new MidiEvent(message, tick));
				//curNoteNo[ch] = noteNo[ch];
				message = new ShortMessage();
				message.setMessage(ShortMessage.NOTE_OFF, 10 - 1, 35 + smpNo, 0);
				tracks[10 - 1].add(new MidiEvent(message, tick + 120));
			} catch (Exception e){
				e.printStackTrace();
			}

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
/*
*
*  OKIM 6295 ADPCM chip:
*
*  Command bytes are sent:
*
*      1xxx xxxx = start of 2-byte command sequence, xxxxxxx is the sample number to trigger
*      abcd vvvv = second half of command; one of the abcd bits is set to indicate which voice
*                  the v bits seem to be volumed
*
*      0abc d000 = stop playing; one or more of the abcd bits is set to indicate which voice(s)
*
*  Status is read:
*
*      ???? abcd = one bit per voice, set to 0 if nothing is playing, or 1 if it is active
*
*/

/*  vgmplay/VGMPlay/chips/okim6295.c */
/* volume lookup table. The manual lists only 9 steps, ~3dB per step. Given the dB values,
that seems to map to a 5-bit volume control. Any volume parameter beyond the 9th index
results in silent playback. */
//static const int volume_table[16] =
//{
//	0x20,	//   0 dB		0x20*4-1=127
//	0x16,	//  -3.2 dB		0x16*4-1=87
//	0x10,	//  -6.0 dB		0x10*4-1=63
//	0x0b,	//  -9.2 dB		と考えてみた
//	0x08,	// -12.0 dB
//	0x06,	// -14.5 dB
//	0x04,	// -18.0 dB
//	0x03,	// -20.5 dB
//	0x02,	// -24.0 dB
//	0x00,
//	0x00,
//	0x00,
//	0x00,
//	0x00,
//	0x00,
//	0x00,
//};
