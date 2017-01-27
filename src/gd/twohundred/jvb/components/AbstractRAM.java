package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractRAM implements ReadWriteMemory, Resetable {
    private final ByteBuffer data;
    private final int addressMask;

    public AbstractRAM(int size) {
        assert Integer.bitCount(size) == 1;
        data = ByteBuffer.allocateDirect(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
        addressMask = size - 1;
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
    public void setHalfWord(int address, short value) {
        int effectiveAddress = address & addressMask;
        data.putShort(effectiveAddress, value);
    }

    @Override
    public void setWord(int address, int value) {
        int effectiveAddress = address & addressMask;
        data.putInt(effectiveAddress, value);
    }

    @Override
    public void reset() {
        for (int i = 0; i < data.limit(); i+=4) {
            data.putInt(0xdeadbeef);
        }
    }
}
