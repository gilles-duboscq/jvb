package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.components.AbstractRAM;

import static gd.twohundred.jvb.Utils.insertNth;

public class FrameBuffer extends AbstractRAM {
    public static final int WIDTH = 384;
    public static final int HEIGHT = 256;
    public static final int BITS_PER_PIXEL = 2;
    public static final int PIXEL_PER_BYTE = Byte.SIZE / BITS_PER_PIXEL;
    public static final int SIZE = WIDTH * HEIGHT / PIXEL_PER_BYTE;
    private final int address;

    public FrameBuffer(int address) {
        super(SIZE);
        this.address = address;
    }

    @Override
    public int getEffectiveAddress(int address) {
        assert address < SIZE;
        return address;
    }

    @Override
    public int getStart() {
        return address;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    public void setPixel(int x, int y, int color) {
        int pixelIndex = x * FrameBuffer.HEIGHT + y;
        int pixelAddr = pixelIndex / FrameBuffer.PIXEL_PER_BYTE;
        int pixelPos = pixelIndex % FrameBuffer.PIXEL_PER_BYTE;
        setByte(pixelAddr, (byte) insertNth(color, pixelPos, FrameBuffer.BITS_PER_PIXEL, getByte(pixelAddr)));
    }

    public void clear() {
        for (int i = 0; i < SIZE / Integer.BYTES; i++) {
            setWord(i, 0);
        }
    }
}
