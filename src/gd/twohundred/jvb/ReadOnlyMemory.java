package gd.twohundred.jvb;

public interface ReadOnlyMemory extends MappedMemory {
    int getByte(int address);

    int getHalfWord(int address);

    int getWord(int address);
}
