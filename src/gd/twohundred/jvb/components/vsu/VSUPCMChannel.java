package gd.twohundred.jvb.components.vsu;

public class VSUPCMChannel extends VSUChannel {
    private static final int PCM_WAVE_START = 0x18;
    public VSUPCMChannel(int start) {
        super(start);
    }
}
