package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.InputProvider;

import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.testBit;

public class GamePad implements ExactlyEmulable {
    private static final int CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS = 7;
    private static final int CONTROL_LATCH_GAME_PAD_SIGNAL_POS = 5;
    private static final int CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS = 4;
    private static final int CONTROL_INITIATE_HARDWARE_READ_POS = 2;
    private static final int CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS = 1;
    private static final int CONTROL_ABORT_HARDWARE_READ_POS = 0;

    private final InputProvider provider;
    private byte status;
    private short input;

    private boolean interruptEnabled;
    private int hardwareReadBit;

    private static final boolean DEBUG_GAME_PAD = false;

    public GamePad(InputProvider inputProvider) {
        this.provider = inputProvider;
    }

    public void setControl(byte value) {
        interruptEnabled = !testBit(value, CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS);
        boolean latchInput = testBit(value, CONTROL_LATCH_GAME_PAD_SIGNAL_POS);
        boolean sendBit = !testBit(value, CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS);
        boolean hardwareRead = testBit(value, CONTROL_INITIATE_HARDWARE_READ_POS);
        boolean abortRead = testBit(value, CONTROL_ABORT_HARDWARE_READ_POS);

        if (abortRead) {
            hardwareReadBit = -1;
            status &= ~intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
        } else if (hardwareRead) {
            hardwareReadBit = 15;
            status |= intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
        } else {
            // TODO latch? sendBit?
            if (DEBUG_GAME_PAD) {
                System.out.printf("Ignoring game pad control with latch: %b sendBit: %b%n", latchInput, sendBit);
            }
        }
        status |= intBit(CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS, !sendBit);
        status |= intBit(CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS, interruptEnabled);
        status |= intBit(CONTROL_LATCH_GAME_PAD_SIGNAL_POS, latchInput);
    }

    public int getControl() {
        return status & 0xff;
    }

    public int getInputLow() {
        return input & 0xff;
    }

    public int getInputHigh() {
        return (input >> 8) & 0xff;
    }

    @Override
    public void reset() {
        setControl((byte) 0x04);
        hardwareReadBit = -1;
    }

    @Override
    public void tickExact(int cycles) {
        if (hardwareReadBit >= 0) {
            input = (short) insert(provider.read(InputProvider.Inputs.values()[hardwareReadBit]), hardwareReadBit, input);
            hardwareReadBit--;
            if (hardwareReadBit < 0) {
                status &= ~intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
            }
        }
    }
}
