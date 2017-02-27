package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.Instructions.AccessWidth;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.utils.WarningMemory;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;

import static gd.twohundred.jvb.BusError.Reason.Unmapped;

public class Bus extends MappedModules {
    public static final int BUS_SIZE = 0x0800_0000;
    private final CartridgeROM rom;
    private final CartridgeRAM ram;
    private final VirtualImageProcessor vip;
    private final HardwareControlRegisters controlRegisters;
    private final VirtualSoundUnit vsu;
    private final Logger logger;
    private final SystemWRAM wram;
    private final WarningMemory cartridgeExtension;
    private Debugger debugger;

    public Bus(CartridgeROM rom, CartridgeRAM ram, VirtualImageProcessor vip, HardwareControlRegisters controlRegisters, VirtualSoundUnit vsu, Logger logger) {
        this.rom = rom;
        this.ram = ram;
        this.vip = vip;
        this.controlRegisters = controlRegisters;
        this.vsu = vsu;
        this.logger = logger;
        this.wram = new SystemWRAM();
        cartridgeExtension = new WarningMemory("Cartridge Expansion", 0x04000000, 0x01000000, logger);
    }

    public static final boolean TRACE_BUS = false;

    protected MappedMemory getMappedModule(int address) {
        if (TRACE_BUS) {
            CPU.debugInstOut.printf("access 0x%08x%n", address);
        }
        if (address >= CartridgeROM.START) {
            return rom;
        }
        if (address >= CartridgeRAM.START) {
            return ram;
        }
        if (address >= SystemWRAM.START) {
            return wram;
        }
        if (address >= 0x04000000) {
            return cartridgeExtension; // TODO ? Cartridge Expansion
        }
        if (address >= HardwareControlRegisters.START + HardwareControlRegisters.MAPPED_SIZE) {
            throw new BusError(address, Unmapped); // TODO ? unused
        }
        if (address >= HardwareControlRegisters.START) {
            return controlRegisters;
        }
        if (address >= VirtualSoundUnit.START) {
            return vsu;
        }
        if (address >= 0) {
            return vip;
        }
        throw new BusError(address, Unmapped);
    }

    @Override
    protected int handlePermissionException(int address) {
        logger.warning(Logger.Component.Memory, "trying to write at read-only address %#08x", address);
        return 0xdeadbeef;
    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getSize() {
        return BUS_SIZE;
    }

    @Override
    public int getByte(int address) {
        onRead(address, AccessWidth.Byte);
        return super.getByte(address);
    }

    @Override
    public int getHalfWord(int address) {
        onRead(address, AccessWidth.HalfWord);
        return super.getHalfWord(address);
    }

    @Override
    public int getWord(int address) {
        onRead(address, AccessWidth.Word);
        return super.getWord(address);
    }

    private void onRead(int address, AccessWidth width) {
        if (debugger != null) {
            debugger.onRead(address, width);
        }
    }

    @Override
    public void setByte(int address, byte value) {
        onWrite(address, value, AccessWidth.Byte);
        super.setByte(address, value);
    }

    @Override
    public void setHalfWord(int address, short value) {
        onWrite(address, value, AccessWidth.HalfWord);
        super.setHalfWord(address, value);
    }

    @Override
    public void setWord(int address, int value) {
        onWrite(address, value, AccessWidth.Word);
        super.setWord(address, value);
    }

    private void onWrite(int address, int value, AccessWidth width) {
        if (debugger != null) {
            debugger.onWrite(address, value, width);
        }
    }

    public void attach(Debugger debugger) {
        this.debugger = debugger;
    }

    CartridgeROM getRom() {
        return rom;
    }
}
