package gd.twohundred.jvb;

public class LinearMemoryMirroring implements ReadWriteMemory {
    private final int start;
    private final ReadWriteMemory original;

    public LinearMemoryMirroring(int start, ReadWriteMemory original) {
        this.start = start;
        this.original = original;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getSize() {
        return original.getSize();
    }

    @Override
    public int getByte(int address) {
        return original.getByte(address);
    }

    @Override
    public int getHalfWord(int address) {
        return original.getHalfWord(address);
    }

    @Override
    public int getWord(int address) {
        return original.getWord(address);
    }

    @Override
    public void setByte(int address, byte value) {
        original.setByte(address, value);
    }

    @Override
    public void setHalfWord(int address, short value) {
        original.setHalfWord(address, value);
    }

    @Override
    public void setWord(int address, int value) {
        original.setWord(address, value);
    }
}
