package gd.twohundred.jvb.components;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.maskedMerge;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.Utils.testBits;

public class ProgramStatusWord {
    private static final int Z_POS = 0;
    private static final int S_POS = 1;
    private static final int OV_POS = 2;
    private static final int CY_POS = 3;
    private static final int ID_POS = 11;
    private static final int AE_POS = 13;
    private static final int EP_POS = 14;
    private static final int NP_POS = 15;
    private static final int INT_POS = 16;
    private static final int INT_LEN = 2;
    private int psw;

    public void set(int value) {
        this.psw = value;
    }

    public void setZeroSignOveflowCarry(boolean zero, boolean sign, boolean overflow, boolean carry) {
        int affected = intBits(Z_POS, S_POS, OV_POS, CY_POS);
        int set = intBit(Z_POS, zero) | intBit(S_POS, sign) | intBit(OV_POS, overflow) | intBit(CY_POS, carry);
        psw = maskedMerge(set, affected, psw);
    }

    public void setZeroSignOveflow(boolean zero, boolean sign, boolean overflow) {
        int affected = intBits(Z_POS, S_POS, OV_POS);
        int set = intBit(Z_POS, zero) | intBit(S_POS, sign) | intBit(OV_POS, overflow);
        psw = maskedMerge(set, affected, psw);
    }


    public void setInterruptDisable(boolean set) {
        psw = insert(set, ID_POS, psw);
    }

    public boolean getCY() {
        return testBit(psw, CY_POS);
    }

    public boolean getOV() {
        return testBit(psw, OV_POS);
    }

    public boolean getS() {
        return testBit(psw, S_POS);
    }

    public boolean getZ() {
        return testBit(psw, Z_POS);
    }

    public int getValue() {
        return psw;
    }

    public void setNMIPending(boolean set) {
        psw = insert(set, NP_POS, psw);
    }

    public void setAddressTrapEnable(boolean set) {
        psw = insert(set, AE_POS, psw);
    }

    public boolean getID() {
        return testBit(psw, ID_POS);
    }

    public int getInt() {
        return extractU(psw, INT_POS, INT_LEN);
    }

    public void setInterruptLevel(int interruptlevel) {
        this.psw = insert(interruptlevel, INT_POS, INT_LEN, psw);
    }

    public void setExceptionPending(boolean set) {
        psw = insert(set, EP_POS, psw);
    }

    public boolean getNP() {
        return testBit(psw, NP_POS);
    }
}
