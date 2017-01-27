package gd.twohundred.jvb.components.utils;

import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;

public class WarningMemory implements ReadWriteMemory {
    private final String name;
    private final int start;
    private final int size;

    public WarningMemory(String name, int start, int size) {
        this.name = name;
        this.start = start;
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

    public static final boolean DEBUG_WARNING_MEMORY = false;

    @Override
    public int getByte(int address) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: reading unimplemented %s memory @ 0x%08X%n", name, address);
        }
        return 0xde;
    }

    @Override
    public int getHalfWord(int address) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: reading unimplemented %s memory @ 0x%08X%n", name, address);
        }
        return 0xdead;
    }

    @Override
    public int getWord(int address) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: reading unimplemented %s memory @ 0x%08X%n", name, address);
        }
        return 0xdeadbeef;
    }

    @Override
    public void setByte(int address, byte value) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: writing into unimplemented %s memory @ 0x%08X%n", name, address);
        }
    }

    @Override
    public void setHalfWord(int address, short value) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: writing into unimplemented %s memory @ 0x%08X%n", name, address);
        }

    }

    @Override
    public void setWord(int address, int value) {
        if (DEBUG_WARNING_MEMORY) {
            System.out.printf("Warning: writing into unimplemented %s memory @ 0x%08X%n", name, address);
        }

    }
}
