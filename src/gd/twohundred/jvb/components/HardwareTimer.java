package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;

import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;

public class HardwareTimer implements ReadWriteMemory, ExactlyEmulable, InterruptSource {
    public static final int START = 0x18;
    public static final int SIZE = 12;

    private static final double LARGE_INTERVAL_MICROS_PER_CYCLE = 100.0;
    private static final double SMALL_INTERVAL_MICROS_PER_CYCLE = 16.6;

    private static final int MICROS_PER_SECOND = 1_000_000;
    private static final int LARGE_INTERVAL_PERIOD = (int) (CPU.CLOCK_HZ * LARGE_INTERVAL_MICROS_PER_CYCLE / MICROS_PER_SECOND);
    private static final int SMALL_INTERVAL_PERIOD = (int) (CPU.CLOCK_HZ * SMALL_INTERVAL_MICROS_PER_CYCLE / MICROS_PER_SECOND);

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
    private byte status;

    private short counter;
    private short reloadValue;
    private int cycleCouter;
    private boolean interruptRaised;
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
        }
        throw new BusError(address, Unmapped);
    }

    private static final boolean DEBUG_TIMER = true;

    @Override
    public void setByte(int address, byte value) {
        switch (address) {
            case CONTROL_REGISTER:
                int effectiveValue = value & WRITE_MASK;
                if (testBit(effectiveValue, CLEAR_ZERO_POS)) {
                    effectiveValue &= ~intBits(CLEAR_ZERO_POS, ZERO_POS);
                    if (DEBUG_TIMER) {
                        System.out.println("Clearing zero status");
                    }
                }
                if (testBit(effectiveValue, ENABLE_INT_POS) && !isInterruptEnabled()) {
                    if (DEBUG_TIMER) {
                        System.out.println("Enabling timer interrupts");
                    }
                }
                if (testBit(effectiveValue, ENABLE_POS) && !isTimerEnabled()) {
                    if (DEBUG_TIMER) {
                        System.out.println("Enabling timer");
                    }
                }
                status = (byte) effectiveValue;
                return;
            case LOW_REGISTER:
                reloadValue = (short) ((reloadValue & 0xff00) | (value & 0xff));
                counter = reloadValue;
                return;
            case HIGH_REGISTER:
                reloadValue = (short) ((reloadValue & 0xff) | ((value << 8) & 0xff));
                counter = reloadValue;
                return;
        }
        throw new BusError(address, Unmapped);
    }

    private boolean isTimerEnabled() {
        return testBit(status, ENABLE_POS);
    }

    private boolean isInterruptEnabled() {
        return testBit(status, ENABLE_INT_POS);
    }

    private int getPeriod() {
        return testBit(status, INTERVAL_POS) ? SMALL_INTERVAL_PERIOD : LARGE_INTERVAL_PERIOD;
    }

    @Override
    public void reset() {
        status = 0x04;
    }

    @Override
    public void tickExact(int cycles) {
        if (isTimerEnabled()) {
            int period = getPeriod();
            int cyclesToConsume = cycles;
            while (cyclesToConsume > 0) {
                int remaining = period - cycleCouter;
                if (cyclesToConsume >= remaining) {
                    cycleCouter = 0;
                    counter++;
                    if (counter == 0) {
                        status |= intBit(ZERO_POS);
                        if (isInterruptEnabled()) {
                            interruptRaised = true;
                        }
                    }
                    cyclesToConsume -= remaining;
                } else {
                    cycleCouter += cyclesToConsume;
                    break;
                }
            }
        }
    }

    @Override
    public boolean raised() {
        return interruptRaised;
    }

    @Override
    public void clear() {
        interruptRaised = false;
    }

    @Override
    public short exceptionCode() {
        return (short) 0xfe10;
    }

    @Override
    public int handlerAddress() {
        return 0xFFFFFE10;
    }
}
