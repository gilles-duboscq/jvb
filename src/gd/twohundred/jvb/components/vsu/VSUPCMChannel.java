package gd.twohundred.jvb.components.vsu;

import static gd.twohundred.jvb.Utils.extractU;

public class VSUPCMChannel extends VSUChannel {
    private static final int PCM_WAVE_START = 0x18;

    private static final int WAVE_INDEX_POS = 0;
    private static final int WAVE_INDEX_LEN = 3;

    private byte waveIndex;

    public VSUPCMChannel(int start) {
        super(start);
    }

    @Override
    public void setByte(int address, byte value) {
        if (address == PCM_WAVE_START) {
            waveIndex = (byte) extractU(value, WAVE_INDEX_POS, WAVE_INDEX_LEN);
            return;
        }
        super.setByte(address, value);
    }

    @Override
    public void reset() {
        super.reset();
        waveIndex = 1;
    }

    public int getWaveIndex() {
        return waveIndex;
    }
}
