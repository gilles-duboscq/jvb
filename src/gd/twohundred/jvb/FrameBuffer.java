package gd.twohundred.jvb;

public class FrameBuffer extends AbstractRAM {
    public static final int WIDTH = 384;
    public static final int HEIGHT = 256;
    public static final int BITS_PER_PIXEL = 2;
    public static final int PIXEL_PER_BYTE = Byte.SIZE / BITS_PER_PIXEL;
    public static final int SIZE = 0x6000;
    private final int address;

    public FrameBuffer(int address) {
        super(SIZE);
        this.address = address;
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
