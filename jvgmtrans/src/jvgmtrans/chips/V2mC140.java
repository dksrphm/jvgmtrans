package jvgmtrans.chips;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import jvgmtrans.Utils;
import jvgmtrans.VGMFileInfo;
import jvgmtrans.VGMStatus;

public class V2mC140 {
	String ChipName = "C140";
	// 同時発声数24
	int Channels = 24;
	// ただしSMFは16チャンネルしかないため、C140の8ボイスずつを3つのSMFに出力することとする
	// (SMFの10chはドラムトラック専用で、スキップさせるのが面倒なので)
	// C140のボイスchとMIDIのchは以下の対応とする
	// pp ch C140    midi
	//  0  0 voice1  ch1-0
	//  0  1 voice2  ch1-1
	//  0  2 voice3  ch1-2
	//  :
	//  0  7 voice8  ch1-7
	//  0  8 voice9  ch2-0
	//  0  9 voice10 ch2-1
	//  0  A voice11 ch2-2
	//  :
	//  0  E voice15 ch2-6
	//  0  F voice16 ch2-7
	//  1  0 voice17 ch3-0
	//  1  1 voice18 ch3-1
	//  :
	//  1  6 voice23 ch3-6
	//  1  7 voice24 ch3-7

	int[] volume_right = new int[Channels];
	int[] volume_left = new int[Channels];
	int[] frequency_msb = new int[Channels];
	int[] frequency_lsb = new int[Channels];
	int[] bank = new int[Channels];
	int[] mode = new int[Channels];
	int[] start_msb = new int[Channels];
	int[] start_lsb = new int[Channels];
	int[] end_msb = new int[Channels];
	int[] end_lsb = new int[Channels];
	int[] loop_msb = new int[Channels];
	int[] loop_lsb = new int[Channels];
	
	int[] freq = new int[Channels];

	Track[] tracks = new Track[Channels];
	// MIDI出力するかフラグ(v2mが呼ばれたら立つ)
	boolean midiOutFlg;

	// MIDIシーケンス
	Sequence seq;

	public V2mC140() {
		// TODO 自動生成されたコンストラクター・スタブ
	}
	public void v2m(byte cmd, byte pp, byte aa, byte dd, VGMStatus vgmStatus, VGMFileInfo vgmFileInfo){
		int midino;
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
		if (pp == (byte)0x01 && aa == (byte)0xf8){
			// 0x01f8
			if (vgmStatus.getDebug()){
				System.out.printf("%s pp:%02x aa:%02x dd:%02x timer_interval:%02x\n", ChipName, pp, aa, dd, dd);
			}
		} else if (pp == (byte)0x01 && aa == (byte)0xfa){
			// 0x01fa
			if (vgmStatus.getDebug()){
				System.out.printf("%s pp:%02x aa:%02x dd:%02x irq_ack:%02x\n", ChipName, pp, aa, dd, dd);
			}
		} else if (pp == (byte)0x01 && aa == (byte)0xfe){
			// 0x01fe
			if (vgmStatus.getDebug()){
				System.out.printf("%s pp:%02x aa:%02x dd:%02x timer_switch:%02x\n", ChipName, pp, aa, dd, dd);
			}
		} else {
			ch = pp * 16 + ((aa & 0xF0) >> 4);
			switch (aa & 0x0F){
			case 0x00:
				// volume_right
				volume_right[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d volume_right[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, volume_right[ch]);
				}
				break;
			case 0x01:
				// volume_left
				volume_left[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d volume_left[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, volume_left[ch]);
				}
				break;
			case 0x02:
				// frequency_msb
				frequency_msb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d frequency_msb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, frequency_msb[ch]);
				}
				break;
			case 0x03:
				// frequency_lsb
				frequency_lsb[ch] = dd & 0xff;
				freq[ch] = frequency_msb[ch] * 256 + frequency_lsb[ch];
				double outHz = hzC140(vgmFileInfo.getC140Clock(), freq[ch]);
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d frequency_lsb[%d]:%02x freq[%d]:%d outHz:%f\n", ChipName, pp, aa, dd, ch, ch, frequency_lsb[ch], ch, freq[ch], outHz);
				}
				break;
			case 0x04:
				// bank
				bank[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d bank[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, bank[ch]);
				}
				break;
			case 0x05:
				// mode
				mode[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d mode[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, mode[ch]);
				}
				break;
			case 0x06:
				// start_msb
				start_msb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d start_msb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, start_msb[ch]);
				}
				break;
			case 0x07:
				// start_lsb
				start_lsb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d start_lsb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, start_lsb[ch]);
				}
				break;
			case 0x08:
				// end_msb
				end_msb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d end_msb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, end_msb[ch]);
				}
				break;
			case 0x09:
				// end_lsb
				end_lsb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d end_lsb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, end_lsb[ch]);
				}
				break;
			case 0x0A:
				// loop_msb
				loop_msb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d loop_msb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, loop_msb[ch]);
				}
				break;
			case 0x0B:
				// loop_lsb
				loop_lsb[ch] = dd & 0xff;
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02d loop_lsb[%d]:%02x\n", ChipName, pp, aa, dd, ch, ch, loop_lsb[ch]);
				}
				break;
			default:
				if (vgmStatus.getDebug()){
					System.out.printf("%s pp:%02x aa:%02x dd:%02x ch:%02x Not implemented\n", ChipName, pp, aa, dd, ch);
				}
			}
		}
	}
	private double hzC140(int fsamC140, int freq){
		/* Delta =  frequency * ((8MHz/374)*2 / sample rate) */
		/* 80000/374 = 21390 = fsamC140*/
		//return (double)freq * ((8000000.0/374.0) * 2.0 / ((double)fsamC140));
		//return (double)freq * ((8000000.0) * 2.0 / ((double)fsamC140 * 384.0));
		//return (double)freq * ((double)fsamC140 * 2.0 / (8000000.0/374.0));
		//return (double)freq * ((double)fsamC140 * 2.0 / (double)fsamC140);
		return (double)freq * ((double)fsamC140 * 2.0 / (44100.0));
		//return (double)freq * (21390 * 2.0 / (21.4*1000));
	}
}
