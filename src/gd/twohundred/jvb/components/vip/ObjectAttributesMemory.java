package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.components.PowerOfTwoRam;

public class ObjectAttributesMemory extends PowerOfTwoRam {
    public static final int START = 0x0003E000;
    public static final int SIZE = 0x2000;

    public static final int ATTRIBUTES_SIZE = 8;

    public static final int ATTRIBUTES_X_START = 0;
    public static final int ATTRIBUTES_PARALLAX_LR_START = 2;
    public static final int ATTRIBUTES_Y_START = 4;
    public static final int ATTRIBUTES_CELL_START = 6;

    public static final int PARALLAX_LR_PARALLAX_POS = 0;
    public static final int PARALLAX_LR_PARALLAX_LEN = 14;
    public static final int PARALLAX_LR_LEFT_POS = 14;
    public static final int PARALLAX_LR_RIGHT_POS = 15;

    public ObjectAttributesMemory() {
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
