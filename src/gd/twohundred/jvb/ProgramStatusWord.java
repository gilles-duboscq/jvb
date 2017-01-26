package gd.twohundred.jvb;

import static gd.twohundred.jvb.Utils.testBits;

public class ProgramStatusWord {
    private static final int Z_POS = 0;
    private int psw;
    public void set(int value) {
        this.psw = value;
    }

    public boolean getZ() {
        return testBits(psw, 1, Z_POS, 1);
    }
}
