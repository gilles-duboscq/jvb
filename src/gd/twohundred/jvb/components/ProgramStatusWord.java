package gd.twohundred.jvb.components;

import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.Utils.testBits;

public class ProgramStatusWord {
    private static final int Z_POS = 0;
    private static final int S_POS = 1;
    private static final int OV_POS = 2;
    private static final int CY_POS = 3;
    private static final int ID_POS = 11;
    private int psw;
    public void set(int value) {
        this.psw = value;
    }

    public boolean getZ() {
        return testBit(psw, Z_POS);
    }

    public void setZeroSignOveflowCarry(boolean zero, boolean sign, boolean overflow, boolean carry) {
        int affected = intBits(Z_POS, S_POS, OV_POS, CY_POS);
        int set = intBit(Z_POS, zero) | intBit(S_POS, sign) | intBit(OV_POS, overflow) | intBit(CY_POS, carry);
        psw = (psw | set) & (~affected | set);
    }


    public void setInterruptDisable(boolean set) {
        if (set) {
            psw |= intBit(ID_POS);
        } else {
            psw &= ~intBit(ID_POS);
        }
    }

    public boolean getCY() {
        return testBit(psw, CY_POS);
    }
}
