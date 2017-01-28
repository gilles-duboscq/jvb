package gd.twohundred.jvb.components;

public class CartridgeRAM extends PowerOfTwoRam {
    public static final int MIN_SIZE = 0x400;
    public static final int MAX_SIZE = 0x100_0000;
    public static final int START = 0x06000000;

    public CartridgeRAM() {
        super(MAX_SIZE);
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return MAX_SIZE;
    }
}
