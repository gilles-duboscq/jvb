package gd.twohundred.jvb.components.utils;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;

import static gd.twohundred.jvb.Logger.Component.Memory;

public class WarningMemory implements ReadWriteMemory {
    private final String name;
    private final int start;
    private final int size;
    private final Logger logger;

    public WarningMemory(String name, int start, int size, Logger logger) {
        this.name = name;
        this.start = start;
        this.size = size;
        this.logger = logger;
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
        logger.debug(Memory, "reading unimplemented %s memory @ %#08x", name, address);
        return 0xde;
    }

    @Override
    public int getHalfWord(int address) {
        logger.debug(Memory, "reading unimplemented %s memory @ %#08x", name, address);
        return 0xdead;
    }

    @Override
    public int getWord(int address) {
        logger.debug(Memory, "reading unimplemented %s memory @ %#08x", name, address);
        return 0xdeadbeef;
    }

    @Override
    public void setByte(int address, byte value) {
        logger.debug(Memory, "writing unimplemented %s memory @ %#08x", name, address);
    }

    @Override
    public void setHalfWord(int address, short value) {
        logger.debug(Memory, "writing unimplemented %s memory @ %#08x", name, address);
    }

    @Override
    public void setWord(int address, int value) {
        logger.debug(Memory, "writing unimplemented %s memory @ %#08x", name, address);
    }
}
