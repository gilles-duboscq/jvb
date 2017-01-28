package gd.twohundred.jvb.components.utils;

import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;

public class LinearMemoryMirroring implements ReadWriteMemory {
    private final ReadWriteMemory original;
    private final int start;
    private final int offset;
    private final int size;

    public LinearMemoryMirroring(ReadWriteMemory original, int start, int offset, int size) {
        this.start = start;
        this.original = original;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getByte(int address) {
        return original.getByte(address + offset);
    }

    @Override
    public int getHalfWord(int address) {
        return original.getHalfWord(address + offset);
    }

    @Override
    public int getWord(int address) {
        return original.getWord(address + offset);
    }

    @Override
    public void setByte(int address, byte value) {
        original.setByte(address + offset, value);
    }

    @Override
    public void setHalfWord(int address, short value) {
        original.setHalfWord(address + offset, value);
    }

    @Override
    public void setWord(int address, int value) {
        original.setWord(address + offset, value);
    }
}
