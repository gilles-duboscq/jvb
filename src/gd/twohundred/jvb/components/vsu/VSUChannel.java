package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.Resetable;
import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public abstract class VSUChannel implements WriteOnlyMemory, Resetable {
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

    private final int start;

    private byte volumeLeft;
    private byte volumeRight;
    private byte duration;
    private boolean enable;
    private boolean useDuration;
    private byte stepInterval;
    private Direction direction;
    private byte reloadValue;
    private boolean enableEnvelope;
    private boolean repeatEnvelope;
    private short frequencyData;

    public enum Direction {
        Decay,
        Grow
    }

    public VSUChannel(int start) {
        this.start = start;
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
                enable = testBit(value, CONTROL_ENABLE_POS);
                useDuration = testBit(value, CONTROL_USE_DURATION_POS);
                duration = (byte) extractU(value, CONTROL_DURATION_POS, CONTROL_DURATION_LEN);
                return;
            case VOLUME_START:
                volumeRight = (byte) extractU(value, VOLUME_RIGHT_POS, VOLUME_LEN);
                volumeLeft = (byte) extractU(value, VOLUME_LEFT_POS, VOLUME_LEN);
                return;
            case ENVELOPE_DATA_START:
                stepInterval = (byte) extractU(value, ENVELOPE_DATA_STEP_INTERVAL_POS, ENVELOPE_DATA_STEP_INTERVAL_LEN);
                direction = testBit(value, ENVELOPE_DATA_DIRECTION_POS) ? Direction.Grow : Direction.Decay;
                reloadValue = (byte) extractU(value, ENVELOPE_DATA_RELOAD_VALUE_POS, ENVELOPE_DATA_RELOAD_VALUE_LEN);
                return;
            case ENVELOPE_CONTROL_START:
                enableEnvelope = testBit(value, ENVELOPE_CONTROL_ENABLE_POS);
                repeatEnvelope = testBit(value, ENVELOPE_CONTROL_REPEAT_POS);
                return;
            case FREQUENCY_LOW_START:
                frequencyData = (short) insert(value, 0, 8, frequencyData);
                return;
            case FREQUENCY_HIGH_START:
                frequencyData = (short) insert(value, 8, 2, frequencyData);
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
        return enable;
    }

    public boolean useDuration() {
        return useDuration;
    }

    public int getDuration() {
        return duration;
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

    public boolean repeatEnvelope() {
        return repeatEnvelope;
    }

    protected short getFrequencyData() {
        return frequencyData;
    }

    @Override
    public void reset() {
        volumeRight = (byte) 0xde;
        volumeLeft = (byte) 0xad;
        duration = 0xb;
        enable = false;
        useDuration = true;
        stepInterval = 5;
        direction = Direction.Grow;
        reloadValue = 0xf;
        frequencyData = 0xef;
    }
}
