package gd.twohundred.jvb.components.cpu;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.maskedMerge;
import static gd.twohundred.jvb.Utils.testBit;

public class ProgramStatusWord {
    private static final int Z_POS = 0;
    private static final int S_POS = 1;
    private static final int OV_POS = 2;
    private static final int CY_POS = 3;
    private static final int FPR_POS = 4;
    private static final int FUD_POS = 5;
    private static final int FOV_POS = 6;
    private static final int FZD_POS = 7;
    private static final int FIV_POS = 8;
    private static final int FRO_POS = 9;
    private static final int ID_POS = 12;
    private static final int AE_POS = 13;
    private static final int EP_POS = 14;
    private static final int NP_POS = 15;
    private static final int INT_POS = 16;
    private static final int INT_LEN = 2;
    static final int PSW_MASK = intBits(Z_POS, S_POS, OV_POS, CY_POS, FPR_POS, FUD_POS, FOV_POS, FZD_POS, FIV_POS, FRO_POS, ID_POS, AE_POS, EP_POS, NP_POS) | mask(INT_POS, INT_LEN);

    private int psw;

    public enum ExecutionMode {
        Normal,
        Exception,
        DuplexedException,
        Halt
    }

    private ExecutionMode executionMode;


    public void set(int value) {
        this.psw = value & PSW_MASK;
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

    public void accumulateReservedUnderFlowOverflowPrecisionDegradation(boolean reservedArg, boolean underflow, boolean overflow, boolean precisionDegradation) {
        psw |= intBit(FRO_POS, reservedArg) | intBit(FUD_POS, underflow) | intBit(FOV_POS, overflow) | intBit(FPR_POS, precisionDegradation);
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

    public void setInterruptLevel(int interruptLevel) {
        this.psw = insert(interruptLevel, INT_POS, INT_LEN, psw);
    }

    public void setExceptionPending(boolean set) {
        psw = insert(set, EP_POS, psw);
    }

    public boolean getNP() {
        return testBit(psw, NP_POS);
    }

    public void accumulateFPUInvalidOp() {
        psw |= intBit(FIV_POS);
    }

    public void accumulateFPUDivByZero() {
        psw |= intBit(FZD_POS);
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }
}
