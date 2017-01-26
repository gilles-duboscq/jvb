package gd.twohundred.jvb;

public class Instructions {
    public static final int OPCODE_POS = 10;
    public static final int OPCODE_LEN = 6;
    public static final int REG1_POS = 0;
    public static final int REG1_LEN = 5;
    public static final int REG2_POS = 5;
    public static final int REG2_LEN = 5;
    public static final int IMM5_POS = 0;
    public static final int IMM5_LEN = 5;
    public static final int COND_POS = 9;
    public static final int COND_LEN = 4;
    public static final int DISP9_POS = 0;
    public static final int DISP9_LEN = 9;

    public static final int FORMAT_III_PREFIX = 0b100;
    public static final int FORMAT_III_PREFIX_LEN = 3;
    public static final int FORMAT_III_PREFIX_POS = 13;

    public static final int OP_MOVHI = 0b101111;
    public static final int OP_MOVEA = 0b101000;
    public static final int OP_JMP = 0b000110;
    public static final int OP_ADD = 0b010001;

    public static final int BCOND_BNZ = 0b1010;
}
