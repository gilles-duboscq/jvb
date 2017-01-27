package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.Utils.mask;

public class ModulationTable implements WriteOnlyMemory {
    public static final int SAMPLE_BIT_WIDTH = 8;
    private final int start;
    private final byte[] data = new byte[0x20];

    public ModulationTable(int start) {
        this.start = start;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getSize() {
        return 0x80;
    }

    @Override
    public void setByte(int address, byte value) {
        data[address / 4] = (byte) (value & mask(SAMPLE_BIT_WIDTH));
    }

    @Override
    public void setHalfWord(int address, short value) {
        data[address / 4] = (byte) (value & mask(SAMPLE_BIT_WIDTH));
    }

    @Override
    public void setWord(int address, int value) {
        data[address / 4] = (byte) (value & mask(SAMPLE_BIT_WIDTH));
    }
}
