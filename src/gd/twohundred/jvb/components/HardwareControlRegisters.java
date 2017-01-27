package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;

public class HardwareControlRegisters implements Resetable, ReadWriteMemory {
    private static final int WAIT_CONTROL_REGISTER = 0x24;
    private static final int TIMER_LOW_REGISTER = 0x18;
    private static final int TIMER_HIGH_REGISTER = 0x1c;
    private static final int TIMER_CONTROL_REGISTER = 0x20;
    private static final int WAIT_ROM_POS = 0;
    private static final int WAIT_EXTENSION_POS = 1;
    public static final int START = 0x02000000;
    public static final int MAPPED_SIZE = 0x01000000;

    private byte waitControl;
    private final HardwareTimer timer;

    public HardwareControlRegisters(HardwareTimer timer) {
        this.timer = timer;
    }

    @Override
    public void reset() {
        // TODO
        waitControl = 0x00;
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return 0x28;
    }

    @Override
    public int getByte(int address) {
        switch (address) {
            case WAIT_CONTROL_REGISTER:
                return waitControl;
        }
        throw new BusError(address, Unimplemented);
    }

    private static final boolean DEBUG_WAIT_CONTROL = false;

    @Override
    public void setByte(int address, byte value) {
        try {
            switch (address) {
                case WAIT_CONTROL_REGISTER:
                    waitControl = (byte) (value & intBits(WAIT_ROM_POS, WAIT_EXTENSION_POS));
                    if (DEBUG_WAIT_CONTROL) {
                        System.out.println("Ignoring wait control registers:" + (testBit(value, WAIT_ROM_POS) ? " wait ROM" : "") + (testBit(value, WAIT_EXTENSION_POS) ? " wait extension" : ""));
                    }
                    return;
                case TIMER_LOW_REGISTER:
                case TIMER_HIGH_REGISTER:
                case TIMER_CONTROL_REGISTER:
                    timer.setByte(address - timer.getStart(), value);
                    return;
            }
        } catch (BusError be) {
            throw new BusError(address, Unmapped, be);
        }
        throw new BusError(address, Unimplemented);
    }
}
