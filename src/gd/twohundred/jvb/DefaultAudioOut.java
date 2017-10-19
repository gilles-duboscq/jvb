package gd.twohundred.jvb;

import gd.twohundred.jvb.components.interfaces.AudioOut;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static gd.twohundred.jvb.Utils.gcd;

public class DefaultAudioOut implements AudioOut {
    private static final int OUT_BITS = 16;
    private final LinearResampler resampler;
    private final SourceDataLine outDataLine;
    private final Logger logger;

    public DefaultAudioOut(Logger logger) {
        this.logger = logger;
        Mixer mixer = AudioSystem.getMixer(null);
        SourceDataLine outLine = null;
        for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
            if (lineInfo instanceof DataLine.Info) {
                DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                if (SourceDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                    for (AudioFormat format : dataLineInfo.getFormats()) {
                        if (format.getChannels() == 2
                                && format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                                && format.getSampleSizeInBits() == OUT_BITS
                                && format.isBigEndian() == (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ) {
                            try {
                                outLine = (SourceDataLine) mixer.getLine(dataLineInfo);
                                logger.debug(Logger.Component.VSU, "Selecting out line: channel: %d, encoding: %s, sample bits: %d, big-endian: %s",
                                        format.getChannels(),
                                        format.getEncoding(),
                                        format.getSampleSizeInBits(),
                                        format.isBigEndian());
                                break;
                            } catch (LineUnavailableException e) {
                                logger.error(Logger.Component.VSU, e, "Advertised audio line was unavailable");
                            }
                        }
                    }
                }
            }
        }
        if (outLine != null) {
            int outRate = (int) outLine.getFormat().getSampleRate();
            assert outRate != AudioSystem.NOT_SPECIFIED && outRate > 0;
            resampler = new LinearResampler(AudioOut.OUTPUT_SAMPLING_HZ, outRate);
            try {
                outLine.open(outLine.getFormat());
                outLine.start();
            } catch (LineUnavailableException e) {
                logger.error(Logger.Component.VSU, e, "Advertised audio line could not be open in the selected format");
                outLine = null;
            }
        } else {
            logger.error(Logger.Component.VSU, "Could not find compatible audio line");
            resampler = null;
        }
        outDataLine = outLine;
    }

    @Override
    public void update(int left, int right) {
        if (resampler == null) {
            return;
        }
        int scaledSampleLeft = left << (16 - AudioOut.OUTPUT_BITS - 1);
        int scaledSampleRight = right << (16 - AudioOut.OUTPUT_BITS - 1);
        resampler.in(scaledSampleLeft, scaledSampleRight, this::sink);
    }

    private void sink(int left, int right) {
        if (outDataLine == null) {
            return;
        }
        byte[] frame = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(frame);
        bb.order(ByteOrder.nativeOrder());
        bb.putChar((char) toSignedSample(left));
        bb.putChar((char) toSignedSample(right));
        outDataLine.write(frame, 0, frame.length);
    }

    private int toSignedSample(int unsigned) {
        return unsigned /*- Utils.mask(OUT_BITS - 1)*/;
    }

    private static class LinearResampler {
        private final int inRate;
        private final int outRate;
        int lastSampleLeft;
        int lastSampleRight;
        int currentDelta;

        LinearResampler(int inRate, int outRate) {
            int gcd = gcd(inRate, outRate);
            this.inRate = inRate / gcd;
            this.outRate = outRate / gcd;
        }

        void in(int inLeft, int inRight, AudioOut out) {
            if (currentDelta > 0) {
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
            return (a * (outRate + currentDelta) - b * currentDelta) / outRate;
        }
    }
}
