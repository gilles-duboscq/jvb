package gd.twohundred.jvb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CartridgeRAM implements ReadWriteMemory {
    public static final int MIN_SIZE = 0x400;
    public static final int MAX_SIZE = 0x100_0000;
    public static final int START = 0x06000000;
    private final ByteBuffer data;
    private final int addressMask;

    public CartridgeRAM() {
        data = ByteBuffer.allocateDirect(MAX_SIZE);
        data.order(ByteOrder.LITTLE_ENDIAN);
        addressMask = MAX_SIZE - 1;
    }

    @Override
    public int getByte(int address) {
        int effectiveAddress = address & addressMask;
        return data.get(effectiveAddress) & 0xff;
    }

    @Override
    public int getHalfWord(int address) {
        int effectiveAddress = address & addressMask;
        return data.getShort(effectiveAddress) & 0xffff;
    }

    @Override
    public int getWord(int address) {
        int effectiveAddress = address & addressMask;
        return data.getInt(effectiveAddress);
    }

    @Override
    public void setByte(int address, byte value) {
        int effectiveAddress = address & addressMask;
        data.put(effectiveAddress, value);
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return MAX_SIZE;
    }
}
