package jvgmtrans;

import javax.sound.midi.Track;

public class VGMStatus {
	boolean debug;
	int samples;
	int vgmhz;
	// 今MIDIデータ上何tick目か
	long midiTicks;
	// 曲データ開始までに遅延させる拍数(初期化用の時間)
	int midiDelay;
	int midiTempo;
	int midiResolution;
	Track midiDrumTrack;
	int midiPitchBend;
	// VGMファイルのファイル名
	String vgmFileName;
	// Midiファイルのファイル名(～_xxxx.midの～の部分)
	String midiFileName;
	
	public VGMStatus() {
		super();
		debug = true;
		samples = 0;
		vgmhz = 44100;
		midiTicks = 0;
		midiDelay = 4;
		midiTempo = 120;
		midiResolution = 480;
		midiDrumTrack = null;
		midiPitchBend = 12;
		vgmFileName = "vgmfile.vgm";
		vgmFileName = "vgmfile";
	}

	public boolean getDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getSamples() {
		return samples;
	}

	public void setSamples(int samples) {
		this.samples = samples;
	}

	public void addSamples(int samples) {
		this.samples += samples;
	}

	public int getVgmhz() {
		return vgmhz;
	}

	public void setVgmhz(int vgmhz) {
		this.vgmhz = vgmhz;
	}

	public int getMidiTempo() {
		return midiTempo;
	}

	public void setMidiTempo(int midiTempo) {
		this.midiTempo = midiTempo;
	}

	public int getMidiResolution() {
		return midiResolution;
	}

	public void setMidiResolution(int midiResolution) {
		this.midiResolution = midiResolution;
	}

	public int getMidiPitchBend() {
		return midiPitchBend;
	}

	public void setMidiPitchBend(int midiPitchBend) {
		this.midiPitchBend = midiPitchBend;
	}

	public int getMidiDelay() {
		return midiDelay;
	}

	public void setMidiDelay(int midiDelay) {
		this.midiDelay = midiDelay;
	}

	public long getMidiTicks() {
		double dMidiTicks = ((double)this.getSamples() * (double)this.getMidiResolution() / (double)this.getVgmhz() * (double)this.getMidiTempo() / 60.0)
				+ ((double)this.midiDelay * (double)this.getMidiResolution());
		return (long)dMidiTicks;
	}

	public void setMidiTicks(long midiTicks) {
		this.midiTicks = midiTicks;
	}

	public Track getMidiDrumTrack() {
		return midiDrumTrack;
	}

	public void setMidiDrumTrack(Track midiDrumTrack) {
		this.midiDrumTrack = midiDrumTrack;
	}

	public String getVgmFileName() {
		return vgmFileName;
	}

	public void setVgmFileName(String vgmFileName) {
		this.vgmFileName = vgmFileName;
		// midiFileNameも更新する
		int point = vgmFileName.lastIndexOf(".");
	    if (point != -1) {
	    	midiFileName = vgmFileName.substring(0, point);
	    } else {
	    	//midiFileName = "jvgmtrans.mid";
	    	midiFileName = vgmFileName;
	    }
	}

	public String getMidiFileName() {
		return midiFileName;
	}

	public void setMidiFileName(String midiFileName) {
		this.midiFileName = midiFileName;
	}

}
