package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.components.AbstractRAM;

public class BackgroundSegmentsAndParametersRAM extends AbstractRAM {
    public static final int START = 0x00020000;
    public static final int SIZE = 0x0001D800;

    public static final int BACKGROUND_SEGMENT_WIDTH_CELLS = 64;
    public static final int BACKGROUND_SEGMENT_HEIGHT_CELLS = 64;
    public static final int BACKGROUND_SEGMENT_CELL_SIZE = 2;
    public static final int BACKGROUND_SEGMENT_SIZE = BACKGROUND_SEGMENT_WIDTH_CELLS * BACKGROUND_SEGMENT_HEIGHT_CELLS * BACKGROUND_SEGMENT_CELL_SIZE;

    public static final int BACKGROUND_SEGMENT_WIDTH_PX = BACKGROUND_SEGMENT_WIDTH_CELLS * CharacterRAM.CHARACTER_WIDTH_PX;
    public static final int BACKGROUND_SEGMENT_HEIGHT_PX = BACKGROUND_SEGMENT_HEIGHT_CELLS * CharacterRAM.CHARACTER_HEIGHT_PX;

    public BackgroundSegmentsAndParametersRAM() {
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

    @Override
    public int getEffectiveAddress(int address) {
        assert address < SIZE;
        return address;
    }
}
