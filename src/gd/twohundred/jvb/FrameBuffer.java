package gd.twohundred.jvb;

public class FrameBuffer implements ReadWriteMemory, Resetable {
    public static final int WIDTH = 384;
    public static final int HEIGHT = 256;
    public static final int BITS_PER_PIXEL = 2;
    public static final int PIXEL_PER_BYTE = Byte.SIZE / BITS_PER_PIXEL;
    public static final int SIZE = 0x6000;
    private final byte[] data = new byte[SIZE];
    private final int address;

    public FrameBuffer(int address) {
        this.address = address;
    }

    @Override
    public int getByte(int address) {
        return data[address] & 0xff;
    }

    @Override
    public int getHalfWord(int address) {
        return (data[address] & 0xff) | (data[address + 1] & 0xff);
    }

    @Override
    public int getWord(int address) {
        return (data[address] & 0xff)
                | (data[address + 1] & 0xff)
                | (data[address + 2] & 0xff)
                | (data[address + 3] & 0xff);
    }

    @Override
    public void setByte(int address, byte value) {
        data[address] = value;
    }

    @Override
    public void reset() {

    }

    @Override
    public int getStart() {
        return address;
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
