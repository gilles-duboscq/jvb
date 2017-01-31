package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.Utils.toBinary;

public class HardwareControlRegisters implements Resetable, ReadWriteMemory {
    private static final int LINK_CONTROL_REGISTER = 0x0;
    private static final int LINK_AUX_REGISTER = 0x4;
    private static final int LINK_TX_REGISTER = 0x8;
    private static final int LINK_RX_REGISTER = 0xc;
    private static final int GAME_PAD_LOW_REGISTER = 0x10;
    private static final int GAME_PAD_HIGH_REGISTER = 0x14;
    private static final int TIMER_LOW_REGISTER = 0x18;
    private static final int TIMER_HIGH_REGISTER = 0x1c;
    private static final int TIMER_CONTROL_REGISTER = 0x20;
    private static final int WAIT_CONTROL_REGISTER = 0x24;
    private static final int GAME_PAD_CONTROL_REGISTER = 0x28;
    private static final int WAIT_ROM_POS = 0;
    private static final int WAIT_EXTENSION_POS = 1;
    public static final int START = 0x02000000;
    public static final int MAPPED_SIZE = 0x01000000;

    private byte waitControl;
    private final HardwareTimer timer;
    private final GamePad gamePad;

    public HardwareControlRegisters(HardwareTimer timer, GamePad gamePad) {
        this.timer = timer;
        this.gamePad = gamePad;
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
        if ((address & 0b11) != 0) {
            // the regs are all 4 bytes aligned and 8 bit wide.
            return 0x00; // ignore reads to other addresses.
        }
        switch (address) {
            case WAIT_CONTROL_REGISTER:
                return waitControl;
            case GAME_PAD_CONTROL_REGISTER:
                return gamePad.getControl();
            case GAME_PAD_HIGH_REGISTER:
                return gamePad.getInputHigh();
            case GAME_PAD_LOW_REGISTER:
                return gamePad.getInputLow();
            case LINK_RX_REGISTER:
            case LINK_CONTROL_REGISTER:
            case LINK_AUX_REGISTER:
                if (DEBUG_LINK_CONTROL) {
                    System.out.printf("Ignoring link control registers read @ 0x%08x%n", address);
                }
                return 0;
            case TIMER_LOW_REGISTER:
            case TIMER_HIGH_REGISTER:
            case TIMER_CONTROL_REGISTER:
                return timer.getByte(address - timer.getStart());
        }
        throw new BusError(address, Unimplemented);
    }

    private static final boolean DEBUG_WAIT_CONTROL = true;
    private static final boolean DEBUG_LINK_CONTROL = true;

    @Override
    public void setByte(int address, byte value) {
        if ((address & 0b11) != 0) {
            // the regs are all 4 bytes aligned and 8 bit wide.
            return; // ignore writes to other addresses.
        }
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
                case LINK_RX_REGISTER:
                case GAME_PAD_LOW_REGISTER:
                case GAME_PAD_HIGH_REGISTER:
                    return; // read-only
                case LINK_CONTROL_REGISTER:
                case LINK_AUX_REGISTER:
                case LINK_TX_REGISTER:
                    if (DEBUG_LINK_CONTROL) {
                        System.out.printf("Ignoring link control registers: 0b%s @ 0x%08x%n", toBinary(value & 0xff, Byte.SIZE), address);
                    }
                    return;
                case GAME_PAD_CONTROL_REGISTER:
                    gamePad.setControl(value);
                    return;

            }
        } catch (BusError be) {
            throw new BusError(address, Unmapped, be);
        }
        throw new BusError(address, Unimplemented);
    }
}
