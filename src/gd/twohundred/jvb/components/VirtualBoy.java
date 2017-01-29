package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.Emulable;
import gd.twohundred.jvb.components.interfaces.InputProvider;
import gd.twohundred.jvb.components.interfaces.Screen;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;

public class VirtualBoy implements Emulable {
    private final CPU cpu;
    private final HardwareTimer timer;
    private final VirtualImageProcessor vip;
    private final VirtualSoundUnit vsu;
    private final GamePad gamePad;

    public VirtualBoy(Screen screen, InputProvider inputProvider, CartridgeROM rom, CartridgeRAM ram) {
        timer = new HardwareTimer();
        vip = new VirtualImageProcessor(screen);
        vsu = new VirtualSoundUnit();
        gamePad = new GamePad(inputProvider);
        HardwareControlRegisters controlRegisters = new HardwareControlRegisters(timer, gamePad);
        Bus bus = new Bus(rom, ram, vip, controlRegisters, vsu);
        cpu = new CPU(bus);
    }

    @Override
    public int tick(int targetCycles) {
        int cycles = 0;
        while (cycles < targetCycles) {
            int actualCycles = cpu.tick(targetCycles);
            timer.tickExact(actualCycles);
            vip.tickExact(actualCycles);
            vsu.tickExact(actualCycles);
            gamePad.tickExact(actualCycles);
            cycles += actualCycles;
        }
        return cycles;
    }

    @Override
    public void reset() {
        cpu.reset();
        timer.reset();
        vip.reset();
        vsu.reset();
        gamePad.reset();
    }
}
