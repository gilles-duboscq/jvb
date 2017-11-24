package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.log2;
import static gd.twohundred.jvb.Utils.mask;

public class VSUPCMChannel extends VSUChannel {
    private static final int PCM_WAVE_START = 0x18;

    private static final int WAVE_INDEX_POS = 0;
    private static final int WAVE_INDEX_LEN = 3;
    private final PCMWaveTable[] waveTables;
    private byte waveIndex;
    private int currentSampleIndex;

    public VSUPCMChannel(int start, PCMWaveTable[] waveTables, Logger logger) {
        super(start, logger);
        this.waveTables = waveTables;
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
    protected byte sample() {
        int waveIndex = getWaveIndex();
        if (waveIndex >= waveTables.length) {
            return 0;
        }
        PCMWaveTable waveTable = waveTables[waveIndex];
        byte out = waveTable.getSample(currentSampleIndex);
        currentSampleIndex++;
        currentSampleIndex &= mask(log2(PCMWaveTable.SAMPLE_COUNT));
        return out;
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
