package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;

public class Bus extends MappedModules {
    public static final int BUS_SIZE = 0x0800_0000;
    private final CartridgeROM rom;
    private final CartridgeRAM ram;
    private final VirtualImageProcessor vip;
    private final HardwareControlRegisters controlRegisters;
    private final VirtualSoundUnit vsu;
    private final SystemWRAM wram;

    public Bus(CartridgeROM rom, CartridgeRAM ram, VirtualImageProcessor vip, HardwareControlRegisters controlRegisters, VirtualSoundUnit vsu) {
        this.rom = rom;
        this.ram = ram;
        this.vip = vip;
        this.controlRegisters = controlRegisters;
        this.vsu = vsu;
        this.wram = new SystemWRAM();
    }

    protected MappedMemory getMappedModule(int address) {
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
            throw new BusError(address, Unimplemented); // TODO ? Cartridge Expansion
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

    public static final boolean DEBUG_MEMORY_PERMISSIONS = false;

    @Override
    protected int handlePermissionException(int address) {
        if (DEBUG_MEMORY_PERMISSIONS) {
            System.out.printf("Warning: trying to write at read-only address: %08X%n", address);
        }
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
}
