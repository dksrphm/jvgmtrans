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

public class V2mSegaPCM {
	String ChipName = "SegaPCM";
	// 同時発声数16
	int Channels = 16;
	
	int[] volumeLeft = new int[Channels];
	int[] volumeRight = new int[Channels];
	int[] loopAddrU = new int[Channels];
	int[] loopAddrL = new int[Channels];
	int[] endAddr = new int[Channels];
	int[] addrDelta = new int[Channels];

	int[] curAddrU = new int[Channels];
	int[] curAddrL = new int[Channels];
	int[] chDisable = new int[Channels];
	int[] loopDisable = new int[Channels];
	int[] bank = new int[Channels];

	// 音データ数　32個
	int[] smpIdArray = new int[32];
	int smpIdNo = 0;
	
	// Midiトラックを10トラック作成(全ての音は10チャンネルに出力される)
	Track[] tracks = new Track[10];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;

	// MIDIシーケンス
	Sequence seq;

	public V2mSegaPCM() {
		// TODO 自動生成されたコンストラクター・スタブ
		midiOutFlg = false;
	}
	
	public void v2m(byte cmd, byte aa1, byte aa2, byte dd, VGMStatus vgmStatus){
		//SegaPCM MIDIトラックは10
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
		
		// SegaPCMは
		// aa1が0x01-0x07はch1、0x08-0x0Fはch2、0x10-0x17はch3
		// aa1が0x81-0x87はch1、0x88-0x8Fはch2、0x90-0x97はch3
		// という構造の模様なので、aa1のアドレスからどのチャンネルなのかを計算する
		// aa1 & 0xffでunsignedで扱えるようにする
		// /8して0-15に割り当てる
		// %8して0-8に割り当てる
		int ch = ((aa1 & 0xff) / 8) % 16;
//		if (vgmStatus.getDebug()){
//			System.out.printf("%s ch:%d\n", ChipName, ch);
//		}
		// aa1を0x00-0x07、0x80-0x87の値へ変換し、aa1xとして扱う
		byte aa1x = (byte) (aa1 - 8 * (byte)(ch & 0xff));
//		if (vgmStatus.getDebug()){
//			System.out.printf("%s aa1x:%02x\n", ChipName, aa1x);
//		}

		switch (aa1x & 0xff){
		case 0x00:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x01:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x02:
			// SegaPCMは、この0x02と0x03の値により、チャンネル毎に左右の音量を変えることができる模様。
			// しかしMIDIではドラム類は10トラックにまとめて出力することから、チャンネル単位のパンをトラック上の各音に
			// 反映することはできない(全ての音のパンが変わってしまう)
			// よってv2mでは、トータルの音量を計算するためにのみ使う。パンのためには使わない。
			// なお0x02、0x03の値は00～3Fの模様。
			volumeLeft[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d volumeLeft[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, volumeLeft[ch]);
			}
			break;
		case 0x03:
			volumeRight[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d volumeRight[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, volumeRight[ch]);
			}
			break;
		case 0x04:
			loopAddrU[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d loopAddrU[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, loopAddrU[ch]);
			}
			break;
		case 0x05:
			loopAddrL[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d loopAddrL[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, loopAddrL[ch]);
			}
			break;
		case 0x06:
			endAddr[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d endAddr[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, endAddr[ch]);
			}
			break;
		case 0x07:
			addrDelta[ch] = dd & 0xff;
			if (vgmStatus.getDebug()){
				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d addrDelta[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, addrDelta[ch]);
			}
			break;
		case 0x80:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x81:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x82:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x83:
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d ?\n", ChipName, aa1, aa2, dd, ch);
//			}
			break;
		case 0x84:
			curAddrU[ch] = dd & 0xff;
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d curAddrU[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, curAddrU[ch]);
//			}
			break;
		case 0x85:
			curAddrL[ch] = dd & 0xff;
//			if (vgmStatus.getDebug()){
//				System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d curAddrL[%02d]:%02x\n", ChipName, aa1, aa2, dd, ch, ch, curAddrL[ch]);
//			}
			break;
		case 0x86:
			chDisable[ch] = dd & 0x01;
			loopDisable[ch] = (dd & 0x02) >> 1;
			bank[ch] = (dd & 0xfc) >> 2;
			int smpNo = -1;
			int smpIdWk = (loopAddrU[ch] << 24) + (loopAddrL[ch] << 16) + (bank[ch] << 8) + addrDelta[ch];
			if ( 0 != smpIdWk ){
				// 既に配列に登録済みのループアドレス・バンクかチェックし、存在すればsmpNoにする
				for (int i = 0; i < smpIdNo; i++){
					if (smpIdWk == smpIdArray[i]){
						smpNo = i;
						break;
					}
				}
				// 配列に登録されていない場合は配列に追加し、その配列の要素番号をsmpNoにする
				if (smpNo == -1){
					smpIdArray[smpIdNo] = smpIdWk;
					smpNo = smpIdNo;
					smpIdNo++;
				}
				// 音量をvolumeLeftとvolumeRightから作る
				int volume;
				// 普通に足して127 / 0x80すると音が小さすぎるので、64-127に納まるようにしてみる
				//volume = (volumeLeft[ch] + volumeRight[ch]) * 127 / 0x80;
				volume = (volumeLeft[ch] + volumeRight[ch]) * 64 / 0x80 + 64;
				if (vgmStatus.getDebug()){
					System.out.printf("%s aa1:%02x aa2:%02x dd:%02x ch:%02d chDisable[%02d]:%1x loopDisable[%02d]:%1x bank[%02d]:%02x "
							, ChipName, aa1, aa2, dd, ch, ch, chDisable[ch], ch, loopDisable[ch], ch, bank[ch]);
					System.out.printf("loopAddrU[%02d]:%02x loopAddrL[%02d]:%02x smpNo:%02d\n"
							, ch, loopAddrU[ch], ch, loopAddrL[ch], smpNo);
								}
				if (chDisable[ch] == 0) {
					try {
						message = new ShortMessage();
						// シーケンサでの修正を考えて、ノート番号70から音を配置する
						message.setMessage(ShortMessage.NOTE_ON, 10 - 1, smpNo + 70, volume);
						tracks[10 - 1].add(new MidiEvent(message, tick));
		
						message = new ShortMessage();
						message.setMessage(ShortMessage.NOTE_OFF, 10 - 1, smpNo + 70, 0);
						tracks[10 - 1].add(new MidiEvent(message, tick + 60));
					} catch (Exception e){
						e.printStackTrace();
					}
				}
			}
			break;
		case 0x87:
			break;
		default:
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

//reg           function
//------------------------------------------------
//0xx0	0xx8	?
//0xx1	0xx9	?
//0xx2	0xxA	volume left
//0xx3	0xxB	volume right
//0xx4	0xxC	loop address (08-15)
//0xx5	0xxD	loop address (16-23)
//0xx6	0xxE	end address
//0xx7	0xxF	address delta

//0xX0	0xX8	?
//0xX1	0xX9	?
//0xX2	0xXA	?
//0xX3	0xXB	?
//0xX4	0xXC	current address (08-15), 00-07 is internal?
//0xX5	0xXD	current address (16-23)
//0xX6	0xXE	bit 0: channel disable?
//				bit 1: loop disable
//				other bits: bank
//0xX7	0xXF	?



//reg      function
//------------------------------------------------
//0x00     ?
//0x01     ?
//0x02     volume left
//0x03     volume right
//0x04     loop address (08-15)
//0x05     loop address (16-23)
//0x06     end address
//0x07     address delta

//0x80     ?
//0x81     ?
//0x82     ?
//0x83     ?
//0x84     current address (08-15), 00-07 is internal?
//0x85     current address (16-23)
//0x86     bit 0: channel disable?
//       bit 1: loop disable
//       other bits: bank
//0x87     ?
