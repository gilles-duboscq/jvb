package gd.twohundred.jvb.audioutils;

import gd.twohundred.jvb.components.interfaces.AudioOut;

import static gd.twohundred.jvb.Utils.gcd;

public class LinearResampler {
    private final int inRate;
    private final int outRate;
    private int lastSampleLeft;
    private int lastSampleRight;
    private int currentDelta;

    public LinearResampler(int inRate, int outRate) {
        int gcd = gcd(inRate, outRate);
        this.inRate = inRate / gcd;
        this.outRate = outRate / gcd;
    }

    public void in(int inLeft, int inRight, AudioOut out) {
        if (currentDelta >= 0) {
            currentDelta -= outRate;
        } else {
            while (currentDelta <= -inRate) {
                out.update(interpolate(lastSampleLeft, inLeft), interpolate(lastSampleRight, inRight));
                currentDelta += inRate;
            }
            int deltaDelta = inRate - outRate;
            out.update(interpolate(lastSampleLeft, inLeft), interpolate(lastSampleRight, inRight));
            currentDelta += deltaDelta;
        }
        lastSampleLeft = inLeft;
        lastSampleRight = inRight;
    }

    private int interpolate(int a, int b) {
        return (b * (outRate + currentDelta) - a * currentDelta) / outRate;
    }
}
