package gd.twohundred.jvb.components;

public abstract class PowerOfTwoRam extends AbstractRAM {
    private final int addressMask;

    public PowerOfTwoRam(int size) {
        super(size);
        assert Integer.bitCount(size) == 1;
        addressMask = size - 1;
    }

    public int getEffectiveAddress(int address) {
        return address & addressMask;
    }
}
