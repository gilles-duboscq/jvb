package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;

import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;

public class HardwareTimer implements ReadWriteMemory, ExactlyEmulable, InterruptSource {
    public static final int START = 0x18;
    public static final int SIZE = 12;

    private static final long LARGE_INTERVAL_MICROS = 100;
    private static final long SMALL_INTERVAL_MICROS = 20;

    private static final int MICROS_PER_SECOND = 1_000_000;
    private static final long LARGE_INTERVAL_PERIOD = CPU.CLOCK_HZ * LARGE_INTERVAL_MICROS / MICROS_PER_SECOND;
    private static final long SMALL_INTERVAL_PERIOD = CPU.CLOCK_HZ * SMALL_INTERVAL_MICROS / MICROS_PER_SECOND;

    private static final int LOW_REGISTER = 0;
    private static final int HIGH_REGISTER = 4;
    private static final int CONTROL_REGISTER = 8;

    private static final int ENABLE_POS = 0;
    private static final int ZERO_POS = 1;
    private static final int CLEAR_ZERO_POS = 2;
    private static final int ENABLE_INT_POS = 3;
    private static final int INTERVAL_POS = 4;
    private static final int READ_MASK = intBits(ENABLE_POS, ZERO_POS, ENABLE_INT_POS, INTERVAL_POS);
    private static final int WRITE_MASK = intBits(ENABLE_POS, CLEAR_ZERO_POS, ENABLE_INT_POS, INTERVAL_POS);
    private final Logger logger;
    private byte status;

    private char counter;
    private char reloadValue;
    private long cycleCounter;
    private boolean interruptRaised;

    public HardwareTimer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public int getByte(int address) {
        switch (address) {
            case CONTROL_REGISTER:
                return status & READ_MASK;
            case LOW_REGISTER:
                return reloadValue & 0xff;
            case HIGH_REGISTER:
                return (reloadValue >> 8) & 0xff;
        }
        throw new BusError(address, Unmapped);
    }

    @Override
    public void setByte(int address, byte value) {
        switch (address) {
            case CONTROL_REGISTER:
                int effectiveValue = (value & WRITE_MASK) | (status & ~WRITE_MASK);
                if (testBit(effectiveValue, CLEAR_ZERO_POS)) {
                    effectiveValue &= ~intBits(CLEAR_ZERO_POS, ZERO_POS);
                    logger.debug(Logger.Component.Timer, "Clearing zero status");
                }
                if (testBit(effectiveValue, ENABLE_INT_POS) != isInterruptEnabled()) {
                    logger.debug(Logger.Component.Timer, "%s timer interrupts", testBit(effectiveValue, ENABLE_INT_POS) ? "Enabling" : "Disabling");
                }
                if (testBit(effectiveValue, ENABLE_POS) != isTimerEnabled()) {
                    logger.debug(Logger.Component.Timer, "%s timer", testBit(effectiveValue, ENABLE_POS) ? "Enabling" : "Disabling");
                }
                if (testBit(effectiveValue, INTERVAL_POS) != testBit(status, INTERVAL_POS) ) {
                    logger.debug(Logger.Component.Timer, "Period:", testBit(effectiveValue, INTERVAL_POS) ? "Small" : "Large");
                }
                status = (byte) effectiveValue;
                if (!isInterruptEnabled() || !hasZeroStatus()) {
                    interruptRaised = false;
                }
                return;
            case LOW_REGISTER:
                reloadValue = (char) ((reloadValue & 0xff00) | (value & 0xff));
                logger.debug(Logger.Component.Timer, "new reload value: 0x%04x", (int) reloadValue);
                counter = reloadValue;
                return;
            case HIGH_REGISTER:
                reloadValue = (char) ((reloadValue & 0xff) | ((value << 8) & 0xff));
                logger.debug(Logger.Component.Timer, "new reload value: 0x%04x", (int) reloadValue);
                counter = reloadValue;
                return;
        }
        throw new BusError(address, Unmapped);
    }

    public boolean isTimerEnabled() {
        return testBit(status, ENABLE_POS);
    }

    public boolean isInterruptEnabled() {
        return testBit(status, ENABLE_INT_POS);
    }

    public long getPeriod() {
        return testBit(status, INTERVAL_POS) ? SMALL_INTERVAL_PERIOD : LARGE_INTERVAL_PERIOD;
    }

    public char getReloadValue() {
        return reloadValue;
    }

    public boolean hasZeroStatus() {
        return testBit(status, ZERO_POS);
    }

    @Override
    public void reset() {
        status = 0x04;
        counter = 0xffff;
        reloadValue = 0x0000;
        interruptRaised = false;
    }

    @Override
    public void tickExact(long cycles) {
        if (isTimerEnabled()) {
            long period = getPeriod();
            cycleCounter += cycles;
            while (cycleCounter > period) {
                if (counter == 0) {
                    status |= intBit(ZERO_POS);
                    logger.debug(Logger.Component.Timer, "Timer zero!!");
                    if (isInterruptEnabled()) {
                        logger.debug(Logger.Component.Timer, "Raising timer interrupt");
                        interruptRaised = true;
                    }
                    counter = reloadValue;
                }
                counter--;
                cycleCounter -= period;
            }
        }
    }

    @Override
    public Interrupt raised() {
        if (interruptRaised) {
            return new SimpleInterrupt(Interrupt.InterruptType.TimerZero);
        }
        return null;
    }
}
