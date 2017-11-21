package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.AudioOut;

import static gd.twohundred.jvb.Utils.NANOS_PER_SECOND;

public class DebugChannel extends VSUChannel {
    private long t;
    private long rt0;

    public DebugChannel(int start, Logger logger) {
        super(start, logger);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int outputSample(VirtualSoundUnit.OutputChannel outputChannel) {
        if (t == 0) {
            rt0 = System.nanoTime();
        }
        if (t % AudioOut.OUTPUT_SAMPLING_HZ == 0) {
            long drt = System.nanoTime() - rt0;
            long ticksPerSecond = t * NANOS_PER_SECOND / drt;
            logger.warning(Logger.Component.VSU, "outputSample / s: %d", ticksPerSecond);
        }
        byte s = (byte) (((Math.sin(2.0 * Math.PI * t * 440 / AudioOut.OUTPUT_SAMPLING_HZ ) + 1) / 2) * 0b111_1111);
        //logger.warning(Logger.Component.VSU, "Debug sample %d", s);
        if (outputChannel == VirtualSoundUnit.OutputChannel.Left) {
            t++;
        }
        return s;
    }

    @Override
    protected byte sample() {
        return 0;
    }
}
