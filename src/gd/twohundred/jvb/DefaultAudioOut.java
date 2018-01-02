package gd.twohundred.jvb;

import gd.twohundred.jvb.audioutils.CircularSampleBuffer;
import gd.twohundred.jvb.audioutils.LinearResampler;
import gd.twohundred.jvb.components.interfaces.AudioOut;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteOrder;

import static gd.twohundred.jvb.Utils.mask;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class DefaultAudioOut implements AudioOut {
    private static final int AUDIO_LATENCY_MS = 50;

    private static final int OUT_BITS = 16;
    private final LinearResampler resampler;
    private final SourceDataLine outDataLine;
    private final Logger logger;
    private final CircularSampleBuffer buffer;
    private long startT;
    private long samplesSunk;

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
                                && format.getSampleSizeInBits() == OUT_BITS
                                && format.isBigEndian() == (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)) {
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
            resampler = new LinearResampler(AudioOut.OUTPUT_SAMPLING_DECIHZ, outRate * 10);
            try {
                outLine.open(outLine.getFormat());
                outLine.start();
                logger.warning(Logger.Component.VSU, "Line out rate: %f", outLine.getFormat().getSampleRate());
            } catch (LineUnavailableException e) {
                logger.error(Logger.Component.VSU, e, "Advertised audio line could not be open in the selected format");
                outLine = null;
            }
            buffer = new CircularSampleBuffer(ByteOrder.nativeOrder(), outRate * AUDIO_LATENCY_MS / 1000);
        } else {
            logger.error(Logger.Component.VSU, "Could not find compatible audio line");
            resampler = null;
            buffer = null;
        }
        outDataLine = outLine;
    }

    @Override
    public void update(int left, int right) {
        if (resampler == null) {
            return;
        }
        assert (mask(AudioOut.OUTPUT_BITS) & left) == left;
        assert (mask(AudioOut.OUTPUT_BITS) & right) == right;
        int scaledSampleLeft = left << (16 - AudioOut.OUTPUT_BITS - 1);
        int scaledSampleRight = right << (16 - AudioOut.OUTPUT_BITS - 1);
        resampler.in(scaledSampleLeft, scaledSampleRight, this::sink);
    }

    private void sink(int left, int right) {
        if (outDataLine == null) {
            return;
        }
        if (startT == 0) {
            startT = System.nanoTime();
        }
        samplesSunk++;
        if (buffer.writeAvailableSamples() < 1) {
            int maxSamples = outDataLine.available() / 4;
            if (maxSamples > 0) {
                int availableSamples = buffer.readAvailableSamples();
                int samples = min(maxSamples, availableSamples);
                long now = System.nanoTime();
                long dt = now - startT;
                long ssps = samplesSunk * Utils.NANOS_PER_SECOND / dt;
                //logger.debug(Logger.Component.VSU, "Writing samples: max=%d, avail=%d, samples=%d, samples sunk/s=%d", maxSamples, availableSamples, samples, ssps);
                if (dt > Utils.NANOS_PER_SECOND * 2) {
                    startT = now;
                    samplesSunk = 0;
                }
                buffer.readTo(outDataLine::write, samples);
            } else {
                //logger.warning(Logger.Component.VSU, "Buffer overflow! dropping a sample :(");
                return;
            }
        }
        buffer.writeSample((char) toSignedSample(left), (char) toSignedSample(right));
    }

    private int toSignedSample(int unsigned) {
        return unsigned /*- Utils.mask(OUT_BITS - 1)*/;
    }

}
