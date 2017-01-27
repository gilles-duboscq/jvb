package gd.twohundred.jvb.components;

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
    public static final int DISP26_LEN = 26;

    public static final int FORMAT_III_PREFIX = 0b100;
    public static final int FORMAT_III_PREFIX_LEN = 3;
    public static final int FORMAT_III_PREFIX_POS = 13;

    public static final int OP_MOVHI = 0b101111;
    public static final int OP_MOVEA = 0b101000;
    public static final int OP_JMP = 0b000110;
    public static final int OP_ADD_IMM = 0b010001;
    public static final int OP_LDSR = 0b011100;
    public static final int OP_SEI = 0b011110;
    public static final int OP_CLI = 0b010110;
    public static final int OP_MOV_IMM = 0b010000;
    public static final int OP_MOV_REG = 0b000000;
    public static final int OP_STB = 0b110100;
    public static final int OP_STH = 0b110101;
    public static final int OP_STW = 0b110111;
    public static final int OP_OUTW = 0b111111;
    public static final int OP_LDB = 0b110000;
    public static final int OP_LDH = 0b110001;
    public static final int OP_LDW = 0b110011;
    public static final int OP_JAL = 0b101011;
    public static final int OP_JR = 0b101010;
    public static final int OP_CMP_REG = 0b000011;
    public static final int OP_XOR_REG = 0b001110;
    public static final int OP_SHL_IMM = 0b010100;
    public static final int OP_ADD_REG = 0b000001;
    public static final int OP_ADDI = 0b101001;

    public static final int BCOND_BNE = 0b1010;
    public static final int BCOND_BL = 0b0001;
    public static final int BCOND_BE = 0b0010;
    public static final int BCOND_BR = 0b0101;
    public static final int BCOND_LT = 0b0110;
}
