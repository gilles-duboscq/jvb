package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;

import static gd.twohundred.jvb.Utils.toBinary;

public class GamePad implements ExactlyEmulable {
    private static final int CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS = 7;
    private static final int CONTROL_LATCH_GAME_PAD_SIGNAL_POS = 5;
    private static final int CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS = 4;
    private static final int CONTROL_INITIATE_HARDWARE_READ_POS = 2;
    private static final int CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS = 1;
    private static final int CONTROL_ABORT_HARDWARE_READ_POS = 0;

    private byte status;
    private short input;

    private static final boolean DEBUG_GAME_PAD = true;

    public void setControl(byte value) {
        if (DEBUG_GAME_PAD) {
            System.out.println("Ignoring game pad control: 0b" + toBinary(value & 0xff, Byte.SIZE));
        }
    }

    public int getControl() {
        return status & 0xff;
    }

    public int getInputLow() {
        return input & 0xff;
    }

    public int getInputHigh() {
        return (input << 8) & 0xff;
    }

    @Override
    public void reset() {
        setControl((byte) 0x04);
    }

    @Override
    public void tickExact(int cycles) {
        // ?
    }
}
