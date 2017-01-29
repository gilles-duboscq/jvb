package gd.twohundred.jvb.components.interfaces;

public interface ReadOnlyMemory extends MappedMemory {
    int getByte(int address);

    default int getHalfWord(int address) {
        return getByte(address) | (getByte(address + 1) << 8);
    }

    default int getWord(int address) {
        return getByte(address)
                | (getByte(address + 1) << 8)
                | (getByte(address + 2) << 16)
                | (getByte(address + 3) << 24);
    }
}
