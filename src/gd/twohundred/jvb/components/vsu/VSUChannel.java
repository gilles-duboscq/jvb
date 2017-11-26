package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public abstract class VSUChannel implements WriteOnlyMemory, ExactlyEmulable {
    private static final int PLAY_CONTROL_START = 0x00;
    private static final int VOLUME_START = 0x04;
    private static final int FREQUENCY_LOW_START = 0x08;
    private static final int FREQUENCY_HIGH_START = 0x0C;
    private static final int ENVELOPE_DATA_START = 0x10;
    protected static final int ENVELOPE_CONTROL_START = 0x14;

    private static final int VOLUME_LEN = 4;
    private static final int VOLUME_RIGHT_POS = 0;
    private static final int VOLUME_LEFT_POS = 4;

    private static final int CONTROL_ENABLE_POS = 7;
    private static final int CONTROL_USE_DURATION_POS = 5;
    private static final int CONTROL_DURATION_POS = 0;
    private static final int CONTROL_DURATION_LEN = 5;

    public static final int ENVELOPE_DATA_STEP_INTERVAL_POS = 0;
    public static final int ENVELOPE_DATA_STEP_INTERVAL_LEN = 3;
    public static final int ENVELOPE_DATA_DIRECTION_POS = 3;
    public static final int ENVELOPE_DATA_RELOAD_VALUE_POS = 4;
    public static final int ENVELOPE_DATA_RELOAD_VALUE_LEN = 4;

    public static final int ENVELOPE_CONTROL_ENABLE_POS = 0;
    public static final int ENVELOPE_CONTROL_REPEAT_POS = 1;

    public static final long ENVELOPE_STEP_UNIT_FREQUENCY_DECIHZ = 651;
    public static final long ENVELOPE_STEP_UNIT_CYCLES = (CPU.CLOCK_HZ * 10L) / ENVELOPE_STEP_UNIT_FREQUENCY_DECIHZ;

    public static final long DURATION_BASE_UNIT_DECIHZ = 2604;
    public static final long DURATION_BASE_UNIT_CYCLES = (CPU.CLOCK_HZ * 10L) / DURATION_BASE_UNIT_DECIHZ;

    private final int start;
    protected final Logger logger;

    private byte volumeLeft;
    private byte volumeRight;
    private byte duration;
    private boolean enabled;
    private boolean useDuration;
    private byte stepInterval;
    private Direction direction;
    private byte reloadValue;
    private boolean enableEnvelope;
    private boolean repeatEnvelope;
    private short frequencyData;

    private byte currentSample;
    private byte currentEnvelope;
    private long cyclesSinceLastSample;
    private long cyclesSinceLastEnvelopeStep;
    private long durationCyclesRemaining;

    public enum Direction {
        Decay,
        Grow;

        public int delta() {
            switch (this) {
                case Grow:
                    return 1;
                case Decay:
                    return -1;
                default:
                    throw new RuntimeException();
            }
        }
    }

    public VSUChannel(int start, Logger logger) {
        this.start = start;
        this.logger = logger;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getSize() {
        return 0x18;
    }

    @Override
    public void setByte(int address, byte value) {
        switch (address) {
            case PLAY_CONTROL_START:
                if (testBit(value, CONTROL_ENABLE_POS) != enabled) {
                    logger.debug(Logger.Component.VSU, "%s @ %#08x %s", this.getClass().getSimpleName(), start, testBit(value, CONTROL_ENABLE_POS) ? "enabled" : "disabled");
                }
                enabled = testBit(value, CONTROL_ENABLE_POS);
                if (!enabled) {
                    currentSample = 0;
                }
                useDuration = testBit(value, CONTROL_USE_DURATION_POS);
                duration = (byte) extractU(value, CONTROL_DURATION_POS, CONTROL_DURATION_LEN);
                if (enabled && useDuration) {
                    durationCyclesRemaining = getDurationCycles();
                }
                return;
            case VOLUME_START:
                volumeRight = (byte) extractU(value, VOLUME_RIGHT_POS, VOLUME_LEN);
                volumeLeft = (byte) extractU(value, VOLUME_LEFT_POS, VOLUME_LEN);
                return;
            case ENVELOPE_DATA_START:
                stepInterval = (byte) extractU(value, ENVELOPE_DATA_STEP_INTERVAL_POS, ENVELOPE_DATA_STEP_INTERVAL_LEN);
                direction = testBit(value, ENVELOPE_DATA_DIRECTION_POS) ? Direction.Grow : Direction.Decay;
                reloadValue = (byte) extractU(value, ENVELOPE_DATA_RELOAD_VALUE_POS, ENVELOPE_DATA_RELOAD_VALUE_LEN);
                currentEnvelope = reloadValue;
                return;
            case ENVELOPE_CONTROL_START:
                enableEnvelope = testBit(value, ENVELOPE_CONTROL_ENABLE_POS);
                repeatEnvelope = testBit(value, ENVELOPE_CONTROL_REPEAT_POS);
                return;
            case FREQUENCY_LOW_START:
                frequencyData = (short) insert(value, 0, 8, frequencyData);
                assert frequencyData >= 0 : value;
                return;
            case FREQUENCY_HIGH_START:
                frequencyData = (short) insert(value, 8, 3, frequencyData);
                assert frequencyData >= 0;
                return;
        }
        if (IGNORE_UNMAPPED_WRITES) {
            return;
        }
        throw new BusError(address, BusError.Reason.Unmapped, this.getClass().getName());
    }

    private static final boolean IGNORE_UNMAPPED_WRITES = true;

    public int getVolumeLeft() {
        return volumeLeft & 0xff;
    }

    public int getVolumeRight() {
        return volumeRight & 0xff;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean useDuration() {
        return useDuration;
    }

    public int getDuration() {
        return duration;
    }

    public long getDurationCycles() {
        return (duration + 1L) * DURATION_BASE_UNIT_CYCLES;
    }

    public int getStepInterval() {
        return stepInterval;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getReloadValue() {
        return reloadValue;
    }

    public boolean enableEnvelope() {
        return enableEnvelope;
    }

    public boolean isEnvelopeEnabled() {
        return enableEnvelope;
    }

    public boolean repeatEnvelope() {
        return repeatEnvelope;
    }

    public short getFrequencyData() {
        return frequencyData;
    }

    protected void addFrequencyDelta(short delta) {
        this.frequencyData = (short) ((frequencyData + delta) & mask(11));
    }

    public int outputSample(VirtualSoundUnit.OutputChannel outputChannel) {
        assert (mask(6) & (currentSample & 0xff)) == (currentSample & 0xff);
        int sample = getVolume(outputChannel) * (currentSample & 0xff);
        sample >>= 1;
        return sample;
    }

    private int getVolume(VirtualSoundUnit.OutputChannel outputChannel) {
        int volume;
        switch (outputChannel) {
            case Left:
                volume = volumeLeft;
                break;
            case Right:
                volume = volumeRight;
                break;
            default:
                throw new RuntimeException();
        }
        volume *= getEnvelope();
        volume >>= 3;
        if (volume != 0) {
            volume += 1;
        }
        return volume;
    }

    private int getEnvelope() {
        return currentEnvelope;
    }

    @Override
    public void tickExact(long cycles) {
        if (enableEnvelope()) {
            cyclesSinceLastEnvelopeStep += cycles;
            long cyclesPerEnvelopeStep = getCyclesPerEnvelopeStep();
            while (cyclesSinceLastEnvelopeStep > cyclesPerEnvelopeStep) {
                if (currentEnvelope == 0) {
                    if (repeatEnvelope) {
                        currentEnvelope = reloadValue;
                    }
                } else {
                    currentEnvelope = (byte) ((currentEnvelope + direction.delta()) & mask(ENVELOPE_DATA_RELOAD_VALUE_LEN));
                }
                cyclesSinceLastEnvelopeStep -= cyclesPerEnvelopeStep;
            }
        }
        if (isEnabled()) {
            cyclesSinceLastSample += cycles;
            long cyclesPerSample = getCyclesPerSample();
            while (cyclesSinceLastSample > cyclesPerSample) {
                currentSample = sample();
                cyclesSinceLastSample -= cyclesPerSample;
            }
            if (useDuration()) {
                durationCyclesRemaining -= cycles;
                if (durationCyclesRemaining <= 0) {
                    enabled = false;
                }
            }
        }
    }

    protected abstract byte sample();

    public long getCyclesPerSample() {
        return 4L * (2048L - frequencyData);
    }

    public long getCyclesPerEnvelopeStep() {
        return ENVELOPE_STEP_UNIT_CYCLES * (stepInterval + 1L);
    }

    @Override
    public void reset() {
        volumeRight = (byte) 0xde;
        volumeLeft = (byte) 0xad;
        duration = 0xb;
        enabled = false;
        useDuration = true;
        stepInterval = 5;
        direction = Direction.Grow;
        reloadValue = 0xf;
        frequencyData = 0xef;
        currentSample = 0;
        cyclesSinceLastSample = 0;
        cyclesSinceLastEnvelopeStep = 0;
    }
}
