package jvgmtrans;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class Utils {

	public Utils() {
		// TODO 自動生成されたコンストラクター・スタブ
	}
	public static void initMidiTrack(Track track, int trackNo, int programNo, String trackName, VGMStatus vgmStatus){
		/*
		 * MIDIトラックを初期化する。コマンドごとに少しずつずらす
		 */
		MetaMessage mmsg = new MetaMessage();
		ShortMessage smsg = new ShortMessage();

		try {
			// トラック名を指定
			mmsg = new MetaMessage();
			mmsg.setMessage(0x03 , trackName.getBytes(), trackName.length());
			MidiEvent me = new MidiEvent(mmsg, 10);
			track.add(me);
			// 音色を設定
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.PROGRAM_CHANGE, trackNo, programNo - 1, 0);
			track.add(new MidiEvent(smsg, 20));
			// ピッチベンド幅を設定
			// MSB
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.CONTROL_CHANGE, trackNo, 101, 0);
			track.add(new MidiEvent(smsg, 30));
			// LSB
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.CONTROL_CHANGE, trackNo, 100, 0);
			track.add(new MidiEvent(smsg, 35));
			// Data Entry
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.CONTROL_CHANGE, trackNo, 6, vgmStatus.getMidiPitchBend());
			track.add(new MidiEvent(smsg, 40));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void initMidiSeq(Sequence seq, VGMStatus vgmStatus){
		try {
			// テンポは初期値120
			vgmStatus.setMidiTempo(120);
			// トラック0(コンダクタートラック)にテンポを設定する
			Track track0 = seq.createTrack();
			MetaMessage mmsg = new MetaMessage();
			int ll = 60*1000000/vgmStatus.getMidiTempo();
			mmsg.setMessage(0x51,
					new byte[]{(byte)(ll/65536), (byte)(ll%65536/256), (byte)(ll%256)},
					3);
			track0.add(new MidiEvent(mmsg, 0));
		} catch (InvalidMidiDataException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
	}

	public static void createTracks(Sequence seq, Track[] tracks, int Channels, String ChipName, VGMStatus vgmStatus){
		String trackName;
		// コンダクタートラックはトラック0
		// seq.getTracks().length で、作成済みのトラック数が得られる
		if (tracks[0] == null){
			System.out.printf("tracks[0] is null\n");
		}
		//beginTrack = seq.getTracks().length;
		//System.out.println(ChipName + " beginTrack: " + beginTrack);
		for (int i = 0; i < Channels; i++){
			try{
				tracks[i] = seq.createTrack();
				// トラックを初期化
				// トラック名を設定
				MetaMessage mmsg = new MetaMessage();
				trackName = ChipName + "(ch" + (i + 1) + ")";
				mmsg.setMessage(0x03 , trackName.getBytes(), trackName.length());
				MidiEvent me = new MidiEvent(mmsg, (long)10);
				tracks[i].add(me);
				// 音色を設定
				// とりあえずsquare(音色番号81)
				// createされたトラック番号は、コンダクタートラックを考慮し seq.getTracks().length で得られるトラック数から2を引く
				ShortMessage message = new ShortMessage();
				message.setMessage(ShortMessage.PROGRAM_CHANGE, seq.getTracks().length - 2, 81 - 1, 0);
				tracks[i].add(new MidiEvent(message, 20));
				// ピッチベンド幅を12に設定
				message = new ShortMessage();
				message.setMessage(ShortMessage.CONTROL_CHANGE, seq.getTracks().length - 2, 101, 0);
				tracks[i].add(new MidiEvent(message, 30));
				message = new ShortMessage();
				message.setMessage(ShortMessage.CONTROL_CHANGE, seq.getTracks().length - 2, 100, 0);
				tracks[i].add(new MidiEvent(message, 35));
				message = new ShortMessage();
				message.setMessage(ShortMessage.CONTROL_CHANGE, seq.getTracks().length - 2, 6, vgmStatus.getMidiPitchBend());
				tracks[i].add(new MidiEvent(message, 40));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	public static void createDrumTracks(Sequence seq, Track tracks[], int Channels, String ChipName){
		String trackName;
		// コンダクタートラックはトラック0
		// seq.getTracks().length で、作成済みのトラック数が得られる
		if (tracks[0] == null){
			System.out.printf("tracks[0] is null\n");
		}
		//beginTrack = seq.getTracks().length;
		//System.out.println(ChipName + " beginTrack: " + beginTrack);
		try{
			MetaMessage mmsg;
			for (int i = 0; i < Channels - 1; i++){
				tracks[i] = seq.createTrack();
				// トラックを初期化
				// トラック名を設定
				mmsg = new MetaMessage();
				trackName = "";
				mmsg.setMessage(0x03 , trackName.getBytes(), trackName.length());
				MidiEvent me = new MidiEvent(mmsg, (long)10);
				tracks[i].add(me);
			}
			mmsg = new MetaMessage();
			tracks[9] = seq.createTrack();
			trackName = ChipName + "(ch10)";
			mmsg.setMessage(0x03 , trackName.getBytes(), trackName.length());
			MidiEvent me = new MidiEvent(mmsg, (long)10);
			tracks[9].add(me);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	public static double hz2MidiNote(double hz){
		double note;
		if (hz == 0){
			note = 0;
		} else {
			note =(Math.log(hz) - Math.log(440)) / (Math.log(Math.pow(2.0, (1.0/12.0)))) + 69.0;
		}
		return note;
	}
	public static int calcVolumeFM(int algorithm, int[] Tl){
		// algorithm
		// YM2151, YM2612
		int volume;
		if (0 <= algorithm && algorithm <=3){
			volume = 127 - Tl[3];
		} else if (algorithm == 4){
			volume = ((127 - Tl[1]) + (127 - Tl[3])) / 2;
		} else if (5 <= algorithm && algorithm <=6){
			volume = ((127 - Tl[1]) + (127 - Tl[2]) + (127 - Tl[3])) / 3;
		} else {
			volume = ((127 - Tl[0]) + (127 - Tl[1]) + (127 - Tl[2]) + (127 - Tl[3])) / 4;
		}
		return volume;
	}
	public static void addNoteOn(Track track, int ch, long tick, int noteNo, int velocity){
		ShortMessage smsg;
		try {
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.NOTE_ON, ch, noteNo, velocity);
			track.add(new MidiEvent(smsg, tick));
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	public static void addNoteOff(Track track, int ch, long tick, int lastNoteNo){
		ShortMessage smsg;
		try {
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.NOTE_OFF, ch, lastNoteNo, 0);
			track.add(new MidiEvent(smsg, tick));
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	public static void addPitchBend(Track track, int ch, long tick, int pitchBend){
		ShortMessage smsg;
		pitchBend = pitchBend + 8192;
		try{
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.PITCH_BEND, ch, pitchBend & 0x7F, (pitchBend >> 7) & 0x7F);
			track.add(new MidiEvent(smsg, tick));
		} catch (Exception e){
			e.printStackTrace();
		}

	}
	public static void addVolumeCC(Track track, int ch, long tick, int volume){
		ShortMessage smsg;
		try {
			smsg = new ShortMessage();
			smsg.setMessage(ShortMessage.CONTROL_CHANGE, ch, 7, volume);
			track.add(new MidiEvent(smsg, tick));
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
