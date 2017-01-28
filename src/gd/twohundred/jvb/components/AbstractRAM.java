package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractRAM implements ReadWriteMemory, Resetable {
    private final ByteBuffer data;

    public AbstractRAM(int size) {
        assert Integer.bitCount(size) == 1;
        data = ByteBuffer.allocateDirect(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int getByte(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        return data.get(effectiveAddress) & 0xff;
    }

    @Override
    public int getHalfWord(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        return data.getShort(effectiveAddress) & 0xffff;
    }

    @Override
    public int getWord(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        return data.getInt(effectiveAddress);
    }

    @Override
    public void setByte(int address, byte value) {
        int effectiveAddress = getEffectiveAddress(address);
        data.put(effectiveAddress, value);
    }

    @Override
    public void setHalfWord(int address, short value) {
        int effectiveAddress = getEffectiveAddress(address);
        data.putShort(effectiveAddress, value);
    }

    @Override
    public void setWord(int address, int value) {
        int effectiveAddress = getEffectiveAddress(address);
        data.putInt(effectiveAddress, value);
    }

    public abstract int getEffectiveAddress(int address);

    @Override
    public void reset() {
        for (int i = 0; i < data.limit(); i+=4) {
            data.putInt(0xdeadbeef);
        }
    }
}
