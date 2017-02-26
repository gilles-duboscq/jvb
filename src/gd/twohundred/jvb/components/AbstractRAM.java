package gd.twohundred.jvb.components;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.BusError.Reason;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractRAM implements ReadWriteMemory, Resetable {
    private final ByteBuffer data;

    public AbstractRAM(int size) {
        data = ByteBuffer.allocateDirect(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int getByte(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            return data.get(effectiveAddress) & 0xff;
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    @Override
    public int getHalfWord(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            return data.getShort(effectiveAddress) & 0xffff;
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    @Override
    public int getWord(int address) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            return data.getInt(effectiveAddress);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    @Override
    public void setByte(int address, byte value) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            data.put(effectiveAddress, value);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    @Override
    public void setHalfWord(int address, short value) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            data.putShort(effectiveAddress, value);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    @Override
    public void setWord(int address, int value) {
        int effectiveAddress = getEffectiveAddress(address);
        try {
            data.putInt(effectiveAddress, value);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new BusError(address, Reason.Error, this.getClass().getName(), ioobe);
        }
    }

    public abstract int getEffectiveAddress(int address);

    @Override
    public void reset() {
        for (int i = 0; i < data.limit(); i+=4) {
            data.putInt(0xdeadbeef);
        }
    }
}
