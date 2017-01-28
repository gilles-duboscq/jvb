package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public abstract class VSUChannel implements WriteOnlyMemory {
    private static final int PLAY_CONTROL_START = 0x00;
    private static final int VOLUME_START = 0x04;
    private static final int FREQUENCY_LOW_START = 0x08;
    private static final int FREQUENCY_HIGH_START = 0x0C;
    private static final int ENVELOPE_DATA_START = 0x10;
    private static final int ENVELOPE_CONTROL_START = 0x14;

    private static final int VOLUME_LEN = 4;

    private static final int CONTROL_ENABLE_POS = 7;
    private static final int CONTROL_USE_DURATION_POS = 5;
    private static final int CONTROL_DURATION_POS = 0;
    private static final int CONTROL_DURATION_LEN = 5;

    private final int start;

    private byte volumeLeft;
    private byte volumeRight;
    private byte duration;
    private boolean enable;
    private boolean useDuration;

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
                volumeRight = (byte) (value & mask(VOLUME_LEN));
                volumeLeft = (byte) ((value >> VOLUME_LEN) & mask(VOLUME_LEN));
                return;
        }
        throw new RuntimeException(String.format("Unmapped address! 0x%08x", address));
    }

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

    public byte getDuration() {
        return duration;
    }
}