package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

public class VSUChannel implements WriteOnlyMemory {
    private final int start;

    public VSUChannel(int start) {
        this.start = start;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getSize() {
        return 0x18;
    }

    @Override
    public void setByte(int address, byte value) {
        throw new RuntimeException(String.format("Unmapped address! 0x%08x", address));
    }
}
