package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public class VSUNoiseChannel extends VSUChannel {
    private static final int NOISE_CONTROL_START = 0x14;

    private static final int TAP_LOCATION_POS = 4;
    private static final int TAP_LOCATION_LEN = 3;

    private static final int STATE_BITS = 15;
    private static final int[] TAP_BITS = new int[]{14, 10, 13, 4, 8, 6, 9, 11};

    private int tapBit;
    private short state;

    public VSUNoiseChannel(int start, Logger logger) {
        super(start, logger);
    }

    @Override
    public void setByte(int address, byte value) {
        super.setByte(address, value);
        if (address == NOISE_CONTROL_START) {
            int tapLocation = extractU(value, TAP_LOCATION_POS, TAP_LOCATION_LEN);
            tapBit = TAP_BITS[tapLocation];
        }
    }

    @Override
    protected byte sample() {
        boolean temp = testBit(state, 7) ^ testBit(state, tapBit);
        state = (short) (((state << 1) | (temp ? 1 : 0)) & mask(STATE_BITS));
        return (byte) (temp ? 0 : 63);
    }

    public long getCyclesPerSample() {
        return 4 * (2048 - getFrequencyData());
    }

    @Override
    public void reset() {
        super.reset();
        tapBit = 8;
        state = (short) mask(STATE_BITS);
    }
}
