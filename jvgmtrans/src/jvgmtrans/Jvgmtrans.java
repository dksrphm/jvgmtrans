package jvgmtrans;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.midi.*;

import jvgmtrans.chips.V2mC140;
import jvgmtrans.chips.V2mOKIM6295;
import jvgmtrans.chips.V2mPSG;
import jvgmtrans.chips.V2mSegaPCM;
import jvgmtrans.chips.V2mYM2151;
import jvgmtrans.chips.V2mYM2203;
import jvgmtrans.chips.V2mYM2612;
import jvgmtrans.VGMFileInfo;
import jvgmtrans.VGMStatus;

public class Jvgmtrans {

	public Jvgmtrans() {
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public static void main(String[] args){
		// TODO Auto-generated method stub
		VGMStatus vgmStatus = new VGMStatus();
		VGMFileInfo vgmFileInfo = new VGMFileInfo();
		
		// Debug mode(-DDEBUG=1) check
		System.out.println(System.getProperty("DEBUG"));
		if (System.getProperty("DEBUG") == null){
			vgmStatus.setDebug(false);
			//vgmStatus.setDebug(true);
		} else {
			vgmStatus.setDebug(true);
		}
		if (args.length == 1){
			vgmStatus.setVgmFileName(args[0]);
		} else {
			vgmStatus.setVgmFileName("vgmfile.vgm");
		}
		if (vgmStatus.getDebug()){
			System.out.printf("VGM File Name: %s\n", vgmStatus.getVgmFileName());
		}
		
		try {
			// 変換後のMIDIシーケンス
			// 分解能は初期値480
			Sequence seq = new Sequence(Sequence.PPQ, vgmStatus.getMidiResolution());
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
			
			// 全トラック作成する
			// ドラムトラックはVGMStatusに格納
			
			// open vgm file (random access)
			RandomAccessFile vgmFile = new RandomAccessFile(vgmStatus.getVgmFileName(), "r");
			//RandomAccessFile vgmFile = new RandomAccessFile("vgmfile.vgm", "r");
			vgmFileInfo.readHeader(vgmFile);
			
			// seek to to VGM data stream
			if (vgmFileInfo.getVersion() >= 0x0150){
				vgmFile.seek(0x34 + vgmFileInfo.getVgmDataOffset());
			} else {
				// For versions prior to 1.50, it should be 0 and the VGM data must start at offset 0x40.
				vgmFile.seek(0x40);
			}
			if (vgmStatus.getDebug()){
				System.out.println("EofOffset:" + Integer.toHexString(vgmFileInfo.getEofOffset()));
				System.out.println("Version = " + Integer.toHexString(vgmFileInfo.getVersion()));
				System.out.println("VGMDataOffset = " + Integer.toHexString(vgmFileInfo.getVgmDataOffset()));
				System.out.println("SN76489Clock = " + vgmFileInfo.getSN76489Clock());
				System.out.println("YM2612Clock = " + vgmFileInfo.getYM2612Clock());
				System.out.println("YM2151Clock = " + vgmFileInfo.getYM2151Clock());
				System.out.println("SegaPCMclock = " + vgmFileInfo.getSegaPcmClock());
				System.out.println("SPCMIF = " + vgmFileInfo.getSPCMIF());
				System.out.println("OKIM6295Clock = " + vgmFileInfo.getOKIM6295Clock());
				System.out.println("PWMClock = " + vgmFileInfo.getPWMClock());
				System.out.println("C140Type = " + vgmFileInfo.getC140Type());
				System.out.println("C140Clock = " + vgmFileInfo.getC140Clock());
				System.out.printf("FilePointer: %x\n", vgmFile.getFilePointer());
			}
			
			// convert VGM data to MIDI events
			byte cmd = 0;
			byte aa = 0, dd = 0;
			byte aa1 = 0, aa2 = 0;
			byte[] buf2 = new byte[2];
			byte[] buf4 = new byte[4];
			Boolean eosd = new Boolean(false);
			long address = 0;

			//YM2612
			V2mYM2612 v2mYM2612 = new V2mYM2612();
			//YM2151
			V2mYM2151 v2mYM2151 = new V2mYM2151();
			//YM2151203
			V2mYM2203 v2mYM2203 = new V2mYM2203();
			//PSG
			V2mPSG v2mPSG = new V2mPSG();
			//OKIM6295
			V2mOKIM6295 v2mOKI6295 = new V2mOKIM6295();
			//SegaPCM
			V2mSegaPCM v2mSegaPCM = new V2mSegaPCM();
			//C140
			V2mC140 v2mC140 = new V2mC140();

			while (vgmFile.length() > vgmFile.getFilePointer() && eosd != true /*&& vgmFile.getFilePointer() < 200000/**/){
				cmd = vgmFile.readByte();
				switch (cmd){
				case 0x4F:
					//Game Gear PSG stereo, write dd to port 0x06
					address = vgmFile.getFilePointer() - 1;
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x dd:%02x\n", address, cmd, dd);
					}
					break;
				case 0x50:
					//PSG (SN76489/SN76496) write value dd
					address = vgmFile.getFilePointer() - 1;
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x dd:%02x\n", address, cmd, dd);
					}
					v2mPSG.v2m(cmd, dd, vgmStatus, vgmFileInfo);
					break;
				case 0x52:
					//YM2612 port 0, write value dd to register aa
				case 0x53:
					//YM2612 port 1, write value dd to register aa
					address = vgmFile.getFilePointer() - 1;
					aa = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x aa:%02x dd:%02x\n", address, cmd, (aa & 0xff), dd);
					}
					v2mYM2612.v2m(cmd, aa, dd, vgmStatus, vgmFileInfo);
					break;
				case 0x54:
					//YM2151, write value dd to register aa
					address = vgmFile.getFilePointer() - 1;
					aa = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x aa:%02x dd:%02x\n", address, cmd, (aa & 0xff), dd);
					}
					//v2mYM2151.v2m(cmd, aa, dd, vgmStatus, seq);
					v2mYM2151.v2m(cmd, aa, dd, vgmStatus);
					break;
				case 0x55:
					//YM2203, write value dd to register aa
					address = vgmFile.getFilePointer() - 1;
					aa = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x aa:%02x dd:%02x\n", address, cmd, (aa & 0xff), dd);
					}
					//v2mYM2203.v2m(cmd, aa, dd, vgmStatus, seq);
					v2mYM2203.v2m(cmd, aa, dd, vgmStatus, vgmFileInfo);
					break;
				case 0x61:
					//Wait n samples, n can range from 0 to 65535 
					address = vgmFile.getFilePointer() - 1;
					vgmFile.read(buf2);
					// signedの16ビット値(-32768～32767)となってしまうので、 & 0xffff をつけてintに変換する
					int waitsmp = (int)(ByteBuffer.wrap(buf2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff);
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x nn:%02x nn:%02x\n", address, cmd, buf2[0], buf2[1]);
						System.out.printf("VGM Wait n samples(%d)\n", waitsmp);
					}
					vgmStatus.addSamples(waitsmp);
					break;
				case 0x62:
					//wait 735 samples 
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x\n", vgmFile.getFilePointer() - 1, cmd);
					}
					vgmStatus.addSamples(735);
					break;
				case 0x63:
					//wait 882 samples
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x\n", vgmFile.getFilePointer() - 1, cmd);
					}
					vgmStatus.addSamples(882);
					break;
				case 0x66:
					//end of sound data
					if (vgmStatus.getDebug()){
						System.out.printf("address: %x cmd: %02x\n", vgmFile.getFilePointer() - 1, cmd);
					}
					eosd = true;
					break;
				case 0x67:
					//data block
					address = vgmFile.getFilePointer() - 1;
					//read 0x66
					vgmFile.readByte();
					//read data type
					byte dataType = vgmFile.readByte();
					//read size of data
					vgmFile.read(buf4);
					int dataSize = ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%04x DataBlock DataType:%02x DataSize:%04x\n", address, dataType, dataSize);
						//System.out.println("DataType:" + dataType + " DataSize:" + dataSize);
					}
					vgmFile.seek(vgmFile.getFilePointer() + dataSize);
					break;
				case 0x70:
				case 0x71:
				case 0x72:
				case 0x73:
				case 0x74:
				case 0x75:
				case 0x76:
				case 0x77:
				case 0x78:
				case 0x79:
				case 0x7a:
				case 0x7b:
				case 0x7c:
				case 0x7d:
				case 0x7e:
				case 0x7f:
					// wait n+1 samples
					vgmStatus.addSamples(cmd - 0x70 + 1);
					//System.out.printf("cmd: %1x\n", cmd);
					//System.out.printf("Samples: %d\n", vgmStatus.getSamples());
					break;
				case (byte) 0xE0:
					// seek to offset dddddddd (Intel byte order) in PCM data bank
					//System.out.printf("address: %x seek to offset: %d\n", vgmFile.getFilePointer() - 1, ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt());
					vgmFile.read(buf4);
					break;
				case (byte) 0x80:
				case (byte) 0x81:
				case (byte) 0x82:
				case (byte) 0x83:
				case (byte) 0x84:
				case (byte) 0x85:
				case (byte) 0x86:
				case (byte) 0x87:
				case (byte) 0x88:
				case (byte) 0x89:
				case (byte) 0x8a:
				case (byte) 0x8b:
				case (byte) 0x8c:
				case (byte) 0x8d:
				case (byte) 0x8e:
				case (byte) 0x8f:
					//YM2612 port 0 address 2A write from the data bank, then wait n samples
					if (vgmStatus.getDebug()){
						// output too much.
						//System.out.printf("address:%x cmd:%02x writeYM2612andWait:%02x\n", vgmFile.getFilePointer(), cmd, (cmd & 0xdd) - 0x80);
					}
					vgmStatus.addSamples((byte)(cmd - 0x80));
					break;
				case (byte) 0xB8:
					//OKIM6295, write value dd to register aa
					address = vgmFile.getFilePointer() - 1;
					aa = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x aa:%02x dd:%02x\n", address, cmd, (aa & 0xff), dd);
					}
					v2mOKI6295.v2m(cmd, aa, dd, vgmStatus);
					break;
				case (byte) 0xC0:
					//0xC0 aaaa dd : Sega PCM, write value dd to memory offset aa2aa1
					address = vgmFile.getFilePointer() - 1;
					aa1 = vgmFile.readByte();
					aa2 = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x aa1:%02x aa2:%02x dd:%02x\n", address, cmd, (aa1 & 0xff), (aa2 & 0xff), dd);
					}
					v2mSegaPCM.v2m(cmd, aa1, aa2, dd, vgmStatus);
					break;
				case (byte) 0xD4:
					//0xD4 pp aa dd : C140 write value dd to register ppaa
					address = vgmFile.getFilePointer() - 1;
					aa1 = vgmFile.readByte();
					aa2 = vgmFile.readByte();
					dd = vgmFile.readByte();
					if (vgmStatus.getDebug()){
						System.out.printf("address:%x cmd:%02x pp:%02x aa:%02x dd:%02x\n", address, cmd, (aa1 & 0xff), (aa2 & 0xff), dd);
					}
					v2mC140.v2m(cmd, aa1, aa2, dd, vgmStatus, vgmFileInfo);
					break;

				default:
					System.out.printf("address:%x command:%x NotImplemented\n", vgmFile.getFilePointer() - 1, cmd);
				}
				
			}
			System.out.println("FilePointer: " + vgmFile.getFilePointer());
			System.out.println("Length: " + vgmFile.length());
			System.out.printf("Samples: %d\n", vgmStatus.getSamples());
			System.out.printf("VGM Play time: %d(s)\n", vgmStatus.getSamples() / vgmStatus.getVgmhz());
			//write to file
			//MidiSystem.write(seq, 1,new java.io.File("jvgmtrans.mid"));
			//MidiSystem.write(seq, 1,new java.io.File(vgmStatus.getMidiFileName() + "_vgm.mid"));
			vgmFile.close();
			v2mYM2151.writeMidiFile(vgmStatus);
			v2mYM2203.writeMidiFile(vgmStatus);
			v2mYM2612.writeMidiFile(vgmStatus);
			v2mOKI6295.writeMidiFile(vgmStatus);
			v2mSegaPCM.writeMidiFile(vgmStatus);
			v2mPSG.writeMidiFile(vgmStatus);
			
			// read vgm file header
			// 256bytes(VGM spec 1.70)
/*			byte[] buf = new byte[256];
			int len = bis.read(buf);
			System.out.println(len);
			String hex;
			// read header-data
			byte[] bufHeader = new byte[4];
			for (int i = 0; i < 4; i++){
				bufHeader[i] = buf[i];
			}
			char[] charArray = byte2char(bufHeader);
			if (String.valueOf(charArray).equals("Vgm ")){
				System.out.println("File is VGM file.");
			} else {
				System.out.println("File is not VGM file.");
			}
			System.out.println(charArray);
*/
			/*String[] strresult = new String(5);
			strresult[0] = String.valueOf((char)buf[0]);
			strresult[1] = String.valueOf((char)buf[1]);
			strresult[2] = String.valueOf((char)buf[2]);
			strresult[3] = String.valueOf((char)buf[3]);
			System.out.println(strresult);
			/*System.out.println((char)buf[0]);
			System.out.println((char)buf[1]);
			System.out.println((char)buf[2]);
			System.out.println((char)buf[3]);*/
			/*strresult[0] = Byte.toString(buf[0]);
			strresult[1] = Byte.toString(buf[1]);
			strresult[2] = Byte.toString(buf[2]);
			strresult[3] = Byte.toString(buf[3]);
			System.out.println(strresult);*/
/*			ByteBuffer byteBuf = ByteBuffer.allocate(4);
			byteBuf.put(buf[0]);
			byteBuf.put(buf[1]);
			byteBuf.put(buf[2]);
			byteBuf.put(buf[3]);*/
/*			int i;
			for (i = 0; i < 4; i++){
				hex = String.format("%1$x", buf[i]);
				System.out.println(hex + " ");
			}
			bis.close();
*/
		} catch (Exception e){
			e.printStackTrace();
		}
		
		try {
			/*
	    	 * SequenceとTrackの作成
	    	 * 24tick=四分音符
	    	 * 戻る
	    	 */ 
			Sequence sequence = new Sequence(Sequence.PPQ, 24);
			Track track = sequence.createTrack();
			//Track track2 = sequence.createTrack();
			/*
			 * チャンネル:0, 音の高さ:48 音の強さ:127 音色番号:6
			 */
			int channel = 0;
			int pitch = 48;
			int velocity = 127;
			int instrument = 6;
			/*
			 * テンポの設定 四分音符の長さをμsecで指定し3バイトに分解する
			 * ここでは20 四分音符/分
			 * 戻る
			 */
			MetaMessage mmessage = new MetaMessage();
			int tempo = 60;
			int l = 60*1000000/tempo;
			mmessage.setMessage(0x51,
					new byte[]{(byte)(l/65536), (byte)(l%65536/256), (byte)(l%256)},
					3);
			track.add(new MidiEvent(mmessage, 0));
			/*
			 * 音色の指定 音色番号:6
			 * 戻る
			 */
			//set instrument
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.PROGRAM_CHANGE, channel, instrument, 0);
			track.add(new MidiEvent(message, 0));
			/*
			 * 音を鳴らす
			 * 戻る
			 */
			// Note on
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
			track.add(new MidiEvent(message, 0));
			// Note off after quater (24tick)
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, channel, pitch, velocity);
			track.add(new MidiEvent(message, 24));
			
			// Note on
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, channel+1, pitch+2, velocity);
			track.add(new MidiEvent(message, 24));
			// Note off after quater (24tick)
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, channel+1, pitch+2, velocity);
			track.add(new MidiEvent(message, 48));
			
			// Note on
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, channel, pitch+4, velocity);
			track.add(new MidiEvent(message, 48));
			// Note off after quater (24tick)
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, channel, pitch+4, velocity);
			track.add(new MidiEvent(message, 72));
			
			// Note on
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, channel, pitch+5, velocity);
			track.add(new MidiEvent(message, 72));
			// Note off after quater (24tick)
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, channel, pitch+5, velocity);
			track.add(new MidiEvent(message, 96));
			
			// Note on
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, channel, pitch+7, velocity);
			track.add(new MidiEvent(message, 96));
			// Note off after quater (24tick)
			message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, channel, pitch+7, velocity);
			track.add(new MidiEvent(message, 120));
			
			/*
			 * ファイルに書き出す。
			 * 戻る
			 */
			//write to file
			MidiSystem.write(sequence, 1,new java.io.File("hello.mid"));
		} catch(Exception e){
			e.printStackTrace();
		}

	}
	public static char[] byte2char(byte[] byteArray) {
		char[] charArray = new char[byteArray.length];
		for (int i = 0; i < charArray.length; i++) {
			charArray[i] = (char) byteArray[i];
		}
		return charArray;
	}

}
