package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;

public class VSUNoiseChannel extends VSUChannel {
    public VSUNoiseChannel(int start, Logger logger) {
        super(start, logger);
    }

    @Override
    protected byte sample() {
        // TODO
        return 0;
    }

    public long getCyclesPerSample() {
        return 4 * (2048 - getFrequencyData());
    }
}
