package gd.twohundred.jvb;

public class VirtualBoy implements Emulable {
    private final VirtualImageProcessor vip;
    private final CPU cpu;

    public VirtualBoy(Screen screen, CartridgeROM rom) {
        vip = new VirtualImageProcessor(screen);
        Bus bus = new Bus(rom, null, vip);
        cpu = new CPU(bus);
    }

    @Override
    public void tick() {
        cpu.tick();
        vip.tick();
    }

    @Override
    public void reset() {
        cpu.reset();
        vip.reset();
    }
}
