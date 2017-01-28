package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.components.PowerOfTwoRam;

public class CharacterRAM extends PowerOfTwoRam {
    public static final int START = 0x0007_8000;
    public static final int SIZE = 0x0000_8000;

    public static final int CHARACTER_WIDTH_PX = 8;
    public static final int CHARACTER_HEIGHT_PX = 8;
    public static final int CHARACTER_SIZE = (CHARACTER_WIDTH_PX * CHARACTER_HEIGHT_PX) / FrameBuffer.PIXEL_PER_BYTE;

    public CharacterRAM() {
        super(SIZE);
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
