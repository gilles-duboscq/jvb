package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.InputProvider;
import gd.twohundred.jvb.components.interfaces.InputProvider.Inputs;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.InterruptSource;

import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;

public class GamePad implements ExactlyEmulable, InterruptSource {
    private static final int CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS = 7;
    private static final int CONTROL_LATCH_GAME_PAD_SIGNAL_POS = 5;
    private static final int CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS = 4;
    private static final int CONTROL_INITIATE_HARDWARE_READ_POS = 2;
    private static final int CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS = 1;
    private static final int CONTROL_ABORT_HARDWARE_READ_POS = 0;

    private static final int HARDWARE_READ_CYCLES_PER_BIT = 4;

    private static final int INTERRUPT_STATUS_MASK = ~intBits(Inputs.A.offset(), Inputs.B.offset(), Inputs.One.offset());

    private final InputProvider provider;
    private final Logger logger;
    private byte status;
    private short input;

    private boolean interruptEnabled;
    private boolean interruptRaised;
    private int hardwareReadBit;

    public GamePad(InputProvider inputProvider, Logger logger) {
        this.provider = inputProvider;
        this.logger = logger;
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
        } else if (latchInput || sendBit) {
            // TODO latch? sendBit?
            logger.warning(Logger.Component.GamePad, "Ignoring game pad control with latch: %b sendBit: %b", latchInput, sendBit);
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
        interruptRaised = false;
    }

    @Override
    public void tickExact(long cycles) {
        for (long i = 0; i < cycles / HARDWARE_READ_CYCLES_PER_BIT; i++) {
            if (hardwareReadBit >= 0) {
                input = (short) insert(provider.read(Inputs.get(hardwareReadBit)), hardwareReadBit, input);
                hardwareReadBit--;
                if (hardwareReadBit < 0) {
                    status &= ~intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
                    if (interruptEnabled && (status & INTERRUPT_STATUS_MASK) != 0) {
                        interruptRaised = true;
                    }
                }
            }
        }
    }

    @Override
    public Interrupt raised() {
        if (interruptRaised) {
            return new SimpleInterrupt(Interrupt.InterruptType.GamePad);
        }
        return null;
    }
}
