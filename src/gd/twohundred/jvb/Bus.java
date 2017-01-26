package gd.twohundred.jvb;

public class Bus implements ReadWriteMemory {
    public static final int BUS_SIZE = 0x0800_0000;
    private final CartridgeROM rom;
    private final CartridgeRAM ram;
    private final VirtualImageProcessor vip;

    public Bus(CartridgeROM rom, CartridgeRAM ram, VirtualImageProcessor vip) {
        this.rom = rom;
        this.ram = ram;
        this.vip = vip;
    }

    @Override
    public int getByte(int address) {
        MappedMemory mappedModule = getMappedModule(address);
        return ((ReadOnlyMemory) mappedModule).getByte(address - mappedModule.getStart());
    }

    @Override
    public int getHalfWord(int address) {
        MappedMemory mappedModule = getMappedModule(address);
        return ((ReadOnlyMemory) mappedModule).getHalfWord(address - mappedModule.getStart());
    }

    @Override
    public int getWord(int address) {
        MappedMemory mappedModule = getMappedModule(address);
        return ((ReadOnlyMemory) mappedModule).getWord(address - mappedModule.getStart());
    }

    @Override
    public void setByte(int address, byte value) {

    }

    private MappedMemory getMappedModule(int address) {
        int effectiveAddress = address & (BUS_SIZE - 1);
        if (effectiveAddress > CartridgeROM.START) {
            return rom;
        }
        if (effectiveAddress > CartridgeRAM.START) {
            return ram;
        }
        if (effectiveAddress < VirtualImageProcessor.MEMORY_MAP_SIZE) {
            return vip;
        }
        throw new RuntimeException(String.format("Unmapped address! %08x", effectiveAddress));
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
