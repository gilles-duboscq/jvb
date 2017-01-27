package gd.twohundred.jvb.components.interfaces;

public interface ReadOnlyMemory extends MappedMemory {
    int getByte(int address);

    default int getHalfWord(int address) {
        return getByte(address) | getByte(address + 1);
    }

    default int getWord(int address) {
        return getByte(address)
                | getByte(address + 1)
                | getByte(address + 2)
                | getByte(address + 3);
    }
}
