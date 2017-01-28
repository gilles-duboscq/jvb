package gd.twohundred.jvb.components;

public class SystemWRAM extends PowerOfTwoRam {
    public static final int START = 0x05000000;
    public static final int MAPPED_SIZE = 0x01000000;
    public static final int SIZE = 0x10000;
    public SystemWRAM() {
        super(SIZE);
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return MAPPED_SIZE;
    }
}
