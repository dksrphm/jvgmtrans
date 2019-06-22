package jvgmtrans;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class VGMFileInfo {
	//0x00
	private String IdentStr;
	private Integer EofOffset;
	private Integer Version;
	private Integer SN76489Clock;
	//0x10
	private Integer YM2413Clock;
	private Integer GD3Offset;
	private Integer TotalSamples;
	private Integer LoopOffset;
	//0x20
	private Integer LoopSamples;
	private Integer Rate;
	private Short SNFB;
	private byte SNW;
	private byte SF;
	private Integer YM2612Clock;
	//0x30
	private Integer YM2151Clock;
	private Integer VgmDataOffset;
	private Integer SegaPcmClock;
	private Integer SPCMIF;
	//0x40
	private Integer RF5C68Clock;
	private Integer YM2203Clock;
	private Integer YM2608Clock;
	private Integer YM2610bClock;
	//0x50
	private Integer YM3812Clock;
	private Integer YM3526Clock;
	private Integer Y8950Clock;
	private Integer YMF262Clock;
	//0x60
	private Integer YMF278BClock;
	private Integer YMF271Clock;
	private Integer YMZ280BClock;
	private Integer RF5C164Clock;
	//0x70
	private Integer PWMClock;
	private Integer AY8910Clock;
	private byte AYT;
	private byte AYFlags1;
	private byte AYFlags2;
	private byte AYFlags3;
	private byte VM;
	//0x7D reserved
	private byte LB;
	private byte LM;
	//0x80
	private Integer GBDMGClock;
	private Integer NESAPUClock;
	private Integer MultiPCMClock;
	private Integer uPD7759Clock;
	//0x90 
	private Integer OKIM6258Clock;
	private byte OKIM6258Flags;
	private byte K054539Flags;
	private byte C140Type;
	//0x97 reserved
	private Integer OKIM6295Clock;
	private Integer K051649Clock;
	//0xA0
	private Integer K054539Clock;
	private Integer HuC6280Clock;
	private Integer C140Clock;
	private Integer K053260Clock;
	//0xB0
	private Integer PokeyClock;
	private Integer QSoundClock;
	//0xB8-BB reserved
	private Integer ExtraHdrOfs;
	
	public VGMFileInfo() {
		super();
		// TODO 自動生成されたコンストラクター・スタブ
		//0x00
		IdentStr = "";
		EofOffset = 0;
		Version = 0;
		SN76489Clock = 0;
		//0x10
		YM2413Clock = 0;
		GD3Offset = 0;
		TotalSamples = 0;
		LoopOffset = 0;
		//0x20
		LoopSamples = 0;
		Rate = 0;
		SNFB = 0;
		SNW = 0;
		SF = 0;
		YM2612Clock = 0;
		//0x30
		YM2151Clock = 0;
		VgmDataOffset = 0;
		SegaPcmClock = 0;
		SPCMIF = 0;
		//0x40
		RF5C68Clock = 0;
		YM2203Clock = 0;
		YM2608Clock = 0;
		YM2610bClock = 0;
		//0x50
		YM3812Clock = 0;
		YM3526Clock = 0;
		Y8950Clock = 0;
		YMF262Clock = 0;
		//0x60
		YMF278BClock = 0;
		YMF271Clock = 0;
		YMZ280BClock = 0;
		RF5C164Clock = 0;
		//0x70
		PWMClock = 0;
		AY8910Clock = 0;
		AYT = 0;
		AYFlags1 = 0;
		AYFlags2 = 0;
		AYFlags3 = 0;
		VM = 0;
		//0x7D reserved
		LB = 0;
		LM = 0;
		//0x80
		GBDMGClock = 0;
		NESAPUClock = 0;
		MultiPCMClock = 0;
		uPD7759Clock = 0;
		//0x90 
		OKIM6258Clock = 0;
		OKIM6258Flags = 0;
		K054539Flags = 0;
		C140Type = 0;
		//0x97 reserved
		OKIM6295Clock = 0;
		K051649Clock = 0;
		//0xA0
		K054539Clock = 0;
		HuC6280Clock = 0;
		C140Clock = 0;
		K053260Clock = 0;
		//0xB0
		PokeyClock = 0;
		QSoundClock = 0;
		//0xB8-BB reserved
		ExtraHdrOfs = 0;
	}
	public void readHeader(RandomAccessFile f){
		// ファイルヘッダを読み込む
		byte[] buf1 = new byte[1];
		byte[] buf2 = new byte[2];
		byte[] buf4 = new byte[4];
		try{
			//0x00
			// read file identification "Vgm "
			f.read(buf4);
			IdentStr = new String(buf4, 0, 4, Charset.forName("UTF-8"));
			System.out.println("Identstr = #" + IdentStr + "#");
			if (IdentStr.equals("Vgm ") ){
				System.out.println("VGMFileInfo: File is VGM file.");
			} else {
				System.out.println("VGMFileInfo: File is not VGM file.");
			}
			// Eof offset (32 bits)
			f.read(buf4);
			EofOffset = ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			// Version number (32 bits)
			f.read(buf4);
			Version = ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			// SN76489 clock (32 bits)
			f.read(buf4);
			SN76489Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			//0x10
			// YM2413 clock (32 bits)
			f.read(buf4);
			YM2413Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			// GD3 offset (32 bits)
			f.read(buf4);
			GD3Offset = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			// Total # samples (32 bits)
			f.read(buf4);
			TotalSamples = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			// Loop offset (32 bits)
			f.read(buf4);
			LoopOffset = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			
			//0x20
			// Loop # samples (32 bits)
			f.read(buf4);
			LoopSamples = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0100);
			// [VGM 1.01 additions:]
			// Rate (32 bits)
			f.read(buf4);
			Rate = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0101);
			// [VGM 1.10 additions:]
			// SN76489 feedback (16 bits)
			f.read(buf2);
			SNFB = setShortValByVer(ByteBuffer.wrap(buf2).order(ByteOrder.LITTLE_ENDIAN).getShort(), 0x0110);
			// SN76489 shift register width (8 bits)
			f.read(buf1);
			SNW = setbyteValByVer(buf1[0], 0x0110);
			// SN76489 Flags (8 bits)
			f.read(buf1);
			SF = setbyteValByVer(buf1[0], 0x0151);
			// [VGM 1.10 additions:]
			// YM2612 clock (32 bits)
			f.read(buf4);
			YM2612Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0110);

			//0x30
			// YM2151 clock (32 bits)
			f.read(buf4);
			YM2151Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0110);
			//[VGM 1.50 additions:]
			// VGM data offset (32 bits)
			// For versions prior to 1.50, it should be 0 and the VGM data must start at offset 0x40.
			f.read(buf4);
			if (Version >= 0x0150){
				VgmDataOffset = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0150);
			} else {
				VgmDataOffset = 0;
			}

			// [VGM 1.51 additions:]
			// Sega PCM clock (32 bits)
			f.read(buf4);
			SegaPcmClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// Sega PCM interface register (32 bits)
			f.read(buf4);
			SPCMIF = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);

			//0x40
			// RF5C68 clock (32 bits)
			f.read(buf4);
			RF5C68Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YM2203 clock (32 bits)
			f.read(buf4);
			YM2203Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YM2608 clock (32 bits)
			f.read(buf4);
			YM2608Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YM2610/YM2610B clock (32 bits)
			f.read(buf4);
			YM2610bClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);

			//0x50
			// YM3812 clock (32 bits)
			f.read(buf4);
			YM3812Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YM3526 clock (32 bits)
			f.read(buf4);
			YM3526Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// Y8950 clock (32 bits)
			f.read(buf4);
			Y8950Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YMF262 clock (32 bits)
			f.read(buf4);
			YMF262Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);

			//0x60
			// YMF278B clock (32 bits)
			f.read(buf4);
			YMF278BClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YMF271 clock (32 bits)
			f.read(buf4);
			YMF271Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// YMZ280B clock (32 bits)
			f.read(buf4);
			YMZ280BClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// RF5C164 clock (32 bits)
			f.read(buf4);
			RF5C164Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);

			//0x70
			// PWM clock (32 bits)
			f.read(buf4);
			PWMClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// AY8910 clock (32 bits)
			f.read(buf4);
			AY8910Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0151);
			// AY8910 Chip Type (8 bits)
			f.read(buf1);
			AYT = setbyteValByVer(buf1[0], 0x0151);
			// AY8910 Flags (8 bits)
			f.read(buf1);
			AYFlags1 = setbyteValByVer(buf1[0], 0x0151);
			// YM2203/AY8910 Flags (8 bits)
			f.read(buf1);
			AYFlags2 = setbyteValByVer(buf1[0], 0x0151);
			// YM2608/AY8910 Flags (8 bits)
			f.read(buf1);
			AYFlags3 = setbyteValByVer(buf1[0], 0x0151);
			// [VGM 1.60 additions:]
			// Volume Modifier (8 bits)
			f.read(buf1);
			VM = setbyteValByVer(buf1[0], 0x0160);
			//0x7D reserved (8 bits)
			f.read(buf1);
			// Loop Base (8 bits)
			f.read(buf1);
			LB = setbyteValByVer(buf1[0], 0x0160);
			// [VGM 1.51 additions:]
			// Loop Modifier (8 bits)
			f.read(buf1);
			LM = setbyteValByVer(buf1[0], 0x0151);

			//0x80
			// [VGM 1.61 additions:]
			// GameBoy DMG clock (32 bits)
			f.read(buf4);
			GBDMGClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// NES APU clock (32 bits)
			f.read(buf4);
			NESAPUClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// MultiPCM clock (32 bits)
			f.read(buf4);
			MultiPCMClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// uPD7759 clock (32 bits)
			f.read(buf4);
			uPD7759Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);

			//0x90
			// OKIM6258 clock (32 bits)
			f.read(buf4);
			OKIM6258Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// OKIM6258 Flags (8 bits)
			f.read(buf1);
			OKIM6258Flags = setbyteValByVer(buf1[0], 0x0161);
			// K054539 Flags (8 bits)
			f.read(buf1);
			K054539Flags = setbyteValByVer(buf1[0], 0x0161);
			// C140 Chip Type (8 bits)
			f.read(buf1);
			C140Type = setbyteValByVer(buf1[0], 0x0161);
			//0x97 reserved (8 bits)
			f.read(buf1);
			// OKIM6295 clock (32 bits)
			f.read(buf4);
			OKIM6295Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// K051649 clock (32 bits)
			f.read(buf4);
			K051649Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);

			//0xA0
			// K054539 clock (32 bits)
			f.read(buf4);
			K054539Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// HuC6280 clock (32 bits)
			f.read(buf4);
			HuC6280Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// C140 clock (32 bits)
			f.read(buf4);
			C140Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// K053260 clock (32 bits)
			f.read(buf4);
			K053260Clock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);

			//0xB0
			// Pokey clock (32 bits)
			f.read(buf4);
			PokeyClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			// QSound clock (32 bits)
			f.read(buf4);
			QSoundClock = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0161);
			//0xB8 reserved (32 bits)
			f.read(buf4);
			// [VGM 1.70 additions:]
			// Extra Header Offset
			f.read(buf4);
			ExtraHdrOfs = setIntValByVer(ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).getInt(), 0x0170);
			// 0xC0: reserved
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private Integer setIntValByVer(Integer Value, Integer Ver){
		if (Version >= Ver){
			return Value;
		} else {
			return 0;
		}
	}
	private Short setShortValByVer(Short Value, Integer Ver){
		if (Version >= Ver){
			return Value;
		} else {
			return 0;
		}
	}
	private byte setbyteValByVer(byte Value, Integer Ver){
		if (Version >= Ver){
			return Value;
		} else {
			return 0;
		}
	}
/*	private Short setShortValByVer(Short Param, Integer Version){
		;
	}
	private byte setbyteValByVer(byte Param, Integer Version){
		;
	}*/
	
	public String getIdentStr(){
		return IdentStr;
	}
	
	public Integer getEofOffset(){
		return EofOffset;
	}

	public Integer getVersion(){
		return Version;
	}

	public Integer getSN76489Clock() {
		return SN76489Clock;
	}

	public Integer getYM2413Clock() {
		return YM2413Clock;
	}

	public Integer getGD3Offset() {
		return GD3Offset;
	}

	public Integer getTotalSamples() {
		return TotalSamples;
	}

	public Integer getLoopOffset() {
		return LoopOffset;
	}

	public Integer getLoopSamples() {
		return LoopSamples;
	}

	public Integer getRate() {
		return Rate;
	}

	public Short getSNFB() {
		return SNFB;
	}

	public byte getSNW() {
		return SNW;
	}

	public byte getSF() {
		return SF;
	}

	public Integer getYM2612Clock() {
		return YM2612Clock;
	}

	public Integer getYM2151Clock() {
		return YM2151Clock;
	}

	public Integer getVgmDataOffset() {
		return VgmDataOffset;
	}

	public Integer getSegaPcmClock() {
		return SegaPcmClock;
	}

	public Integer getSPCMIF() {
		return SPCMIF;
	}

	public Integer getRF5C68Clock() {
		return RF5C68Clock;
	}

	public Integer getYM2203Clock() {
		return YM2203Clock;
	}

	public Integer getYM2608Clock() {
		return YM2608Clock;
	}

	public Integer getYM2610bClock() {
		return YM2610bClock;
	}

	public Integer getYM3812Clock() {
		return YM3812Clock;
	}

	public Integer getYM3526Clock() {
		return YM3526Clock;
	}

	public Integer getY8950Clock() {
		return Y8950Clock;
	}

	public Integer getYMF262Clock() {
		return YMF262Clock;
	}

	public Integer getYMF278BClock() {
		return YMF278BClock;
	}

	public Integer getYMF271Clock() {
		return YMF271Clock;
	}

	public Integer getYMZ280BClock() {
		return YMZ280BClock;
	}

	public Integer getRF5C164Clock() {
		return RF5C164Clock;
	}

	public Integer getPWMClock() {
		return PWMClock;
	}

	public Integer getAY8910Clock() {
		return AY8910Clock;
	}

	public byte getAYT() {
		return AYT;
	}

	public byte getAYFlags1() {
		return AYFlags1;
	}

	public byte getAYFlags2() {
		return AYFlags2;
	}

	public byte getAYFlags3() {
		return AYFlags3;
	}

	public byte getVM() {
		return VM;
	}

	public byte getLB() {
		return LB;
	}

	public byte getLM() {
		return LM;
	}

	public Integer getGBDMGClock() {
		return GBDMGClock;
	}

	public Integer getNESAPUClock() {
		return NESAPUClock;
	}

	public Integer getMultiPCMClock() {
		return MultiPCMClock;
	}

	public Integer getuPD7759Clock() {
		return uPD7759Clock;
	}

	public Integer getOKIM6258Clock() {
		return OKIM6258Clock;
	}

	public byte getOKIM6258Flags() {
		return OKIM6258Flags;
	}

	public byte getK054539Flags() {
		return K054539Flags;
	}

	public byte getC140Type() {
		return C140Type;
	}

	public Integer getOKIM6295Clock() {
		return OKIM6295Clock;
	}

	public Integer getK051649Clock() {
		return K051649Clock;
	}

	public Integer getK054539Clock() {
		return K054539Clock;
	}

	public Integer getHuC6280Clock() {
		return HuC6280Clock;
	}

	public Integer getC140Clock() {
		return C140Clock;
	}

	public Integer getK053260Clock() {
		return K053260Clock;
	}

	public Integer getPokeyClock() {
		return PokeyClock;
	}

	public Integer getQSoundClock() {
		return QSoundClock;
	}

	public Integer getExtraHdrOfs() {
		return ExtraHdrOfs;
	}
}
/*
0x00 ["Vgm " ident   ][EoF offset     ][Version        ][SN76489 clock  ]
0x10 [YM2413 clock   ][GD3 offset     ][Total # samples][Loop offset    ]
0x20 [Loop # samples ][Rate           ][SN FB ][SNW][SF][YM2612 clock   ]
0x30 [YM2151 clock   ][VGM data offset][Sega PCM clock ][SPCM Interface ]
0x40 [RF5C68 clock   ][YM2203 clock   ][YM2608 clock   ][YM2610/B clock ]
0x50 [YM3812 clock   ][YM3526 clock   ][Y8950 clock    ][YMF262 clock   ]
0x60 [YMF278B clock  ][YMF271 clock   ][YMZ280B clock  ][RF5C164 clock  ]
0x70 [PWM clock      ][AY8910 clock   ][AYT][AY Flags  ][VM] *** [LB][LM]
0x80 [GB DMG clock   ][NES APU clock  ][MultiPCM clock ][uPD7759 clock  ]
0x90 [OKIM6258 clock ][OF][KF][CF] *** [OKIM6295 clock ][K051649 clock  ]
0xA0 [K054539 clock  ][HuC6280 clock  ][C140 clock     ][K053260 clock  ]
0xB0 [Pokey clock    ][QSound clock   ] *** *** *** *** [Extra Hdr ofs  ]
 */
