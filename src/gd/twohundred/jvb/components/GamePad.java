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
import static gd.twohundred.jvb.Utils.toBinary;

public class GamePad implements ExactlyEmulable, InterruptSource {
    private static final int CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS = 7;
    private static final int CONTROL_LATCH_GAME_PAD_SIGNAL_POS = 5;
    private static final int CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS = 4;
    private static final int CONTROL_INITIATE_HARDWARE_READ_POS = 2;
    private static final int CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS = 1;
    private static final int CONTROL_ABORT_HARDWARE_READ_POS = 0;

    private static final int HARDWARE_READ_CYCLES_PER_BIT = 4;

    private static final int INTERRUPT_INPUT_MASK = ~intBits(Inputs.A.offset(), Inputs.B.offset(), Inputs.One.offset());

    private final InputProvider provider;
    private final Logger logger;
    private byte status;
    private short input;

    private boolean interruptEnabled;
    private boolean interruptRaised;
    private int hardwareReadBit;
    private long hardwareReadCycles;
    private int softwareReadBit;
    private boolean softwareReadLatched;
    private boolean softwareReadBitArmed;

    public GamePad(InputProvider inputProvider, Logger logger) {
        this.provider = inputProvider;
        this.logger = logger;
    }

    public void setControl(byte value) {
        logger.debug(Logger.Component.GamePad, "Control %s", toBinary(value & 0xff, 8));
        interruptEnabled = !testBit(value, CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS);
        boolean latchInput = testBit(value, CONTROL_LATCH_GAME_PAD_SIGNAL_POS);
        boolean clock = testBit(value, CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS);
        boolean hardwareRead = testBit(value, CONTROL_INITIATE_HARDWARE_READ_POS);
        boolean abortRead = testBit(value, CONTROL_ABORT_HARDWARE_READ_POS);

        if (abortRead) {
            hardwareReadBit = -1;
            status &= ~intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
            softwareReadBit = -1;
            softwareReadLatched = false;
            softwareReadBitArmed = false;
            logger.debug(Logger.Component.GamePad, "Abort read");
        } else if (latchInput) {
            softwareReadBit = 0;
            softwareReadLatched = true;
            softwareReadBitArmed = false;
            logger.debug(Logger.Component.GamePad, "Latch input");
        } else if (hardwareRead) {
            hardwareReadBit = 15;
            status |= intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
            softwareReadBit = -1;
            softwareReadLatched = false;
            softwareReadBitArmed = false;
            hardwareReadCycles = 0;
            logger.debug(Logger.Component.GamePad, "Start hardware read");
        } else if (clock) {
            if (softwareReadBitArmed && softwareReadBit >= 0) {
                logger.debug(Logger.Component.GamePad, "Reading bit %d", softwareReadBit);
                input = (short) insert(provider.read(Inputs.get(softwareReadBit)), softwareReadBit, input);
                softwareReadBitArmed = false;
                softwareReadBit++;
                if (softwareReadBit > 15) {
                    softwareReadBit = -1;
                }
            } else {
                logger.warning(Logger.Component.GamePad, "Send bit in wrong state for bit %d, armed: %s", softwareReadBit, softwareReadBitArmed);
            }
        } else {
            if (softwareReadBit > 0 || softwareReadLatched) {
                logger.debug(Logger.Component.GamePad, "Arming for bit %d", softwareReadBit);
                softwareReadBitArmed = true;
                softwareReadLatched = false;
            }
        }

        status |= intBit(CONTROL_SOFTWARE_INPUT_CLOCK_SIGNAL_POS, clock);
        status |= intBit(CONTROL_GAME_PAD_INTERRUPT_DISABLE_POS, interruptEnabled);
        status |= intBit(CONTROL_LATCH_GAME_PAD_SIGNAL_POS, latchInput);
    }

    public int getControl() {
        return (status & 0xff) | intBit(CONTROL_INITIATE_HARDWARE_READ_POS);
    }

    public int getInputLow() {
        return input & 0xff;
    }

    public int getInputHigh() {
        return (input >> 8) & 0xff;
    }

    @Override
    public void reset() {
        setControl((byte) 0x01); // 04??
        interruptRaised = false;
        hardwareReadCycles = 0;
        softwareReadBit = -1;
        softwareReadBitArmed = false;
        softwareReadLatched = false;
    }

    @Override
    public void tickExact(long cycles) {
        if (isHardwareReadInProgress()) {
            hardwareReadCycles += cycles;
            while (hardwareReadCycles >= HARDWARE_READ_CYCLES_PER_BIT) {
                input = (short) insert(provider.read(Inputs.get(hardwareReadBit)), hardwareReadBit, input);
                hardwareReadBit--;
                hardwareReadCycles -= HARDWARE_READ_CYCLES_PER_BIT;
                if (hardwareReadBit < 0) {
                    status &= ~intBit(CONTROL_HARDWARE_INPUT_IN_PROGRESS_POS);
                    if (interruptEnabled && (input & INTERRUPT_INPUT_MASK) != 0) {
                        interruptRaised = true;
                    }
                    hardwareReadCycles = 0;
                    logger.debug(Logger.Component.GamePad, "Start hardware finished");
                    break;
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

    public boolean isInterruptEnabled() {
        return interruptEnabled;
    }

    public boolean isHardwareReadInProgress() {
        return hardwareReadBit >= 0;
    }

    public boolean isSoftwareReadInProgress() {
        return softwareReadBit >= 0;
    }
}
