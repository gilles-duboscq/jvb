package gd.twohundred.jvb;

public interface WriteOnlyMemory extends MappedMemory {
    void setByte(int address, byte value);

    default void setHalfWord(int address, short value) {
        setByte(address, (byte) value);
        setByte(address + 1, (byte) (value >> 8));
    }

    default void setWord(int address, int value) {
        setByte(address, (byte) value);
        setByte(address + 1, (byte) (value >> 8));
        setByte(address + 2, (byte) (value >> 16));
        setByte(address + 3, (byte) (value >> 24));
    }
}
