package gd.twohundred.jvb;

public interface WriteOnlyMemory extends MappedMemory {
    void setByte(int address, byte value);
}
