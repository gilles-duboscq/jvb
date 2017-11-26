package gd.twohundred.jvb.components.cpu;

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
    public static final int SUB_OPCODE_POS = 26;
    public static final int SUB_OPCODE_LEN = 6;

    public static final int FORMAT_III_PREFIX = 0b100;
    public static final int FORMAT_III_PREFIX_LEN = 3;
    public static final int FORMAT_III_PREFIX_POS = 13;

    public static final int OP_MOVHI = 0b101111;
    public static final int OP_MOVEA = 0b101000;
    public static final int OP_JMP = 0b000110;
    public static final int OP_ADD_IMM = 0b010001;
    public static final int OP_LDSR = 0b011100;
    public static final int OP_STSR = 0b011101;
    public static final int OP_SEI = 0b011110;
    public static final int OP_CLI = 0b010110;
    public static final int OP_MOV_IMM = 0b010000;
    public static final int OP_MOV_REG = 0b000000;
    public static final int OP_STB = 0b110100;
    public static final int OP_STH = 0b110101;
    public static final int OP_STW = 0b110111;
    public static final int OP_OUTB = 0b111100;
    public static final int OP_OUTH = 0b111101;
    public static final int OP_OUTW = 0b111111;
    public static final int OP_INB = 0b111000;
    public static final int OP_INH = 0b111001;
    public static final int OP_INW = 0b111011;
    public static final int OP_LDB = 0b110000;
    public static final int OP_LDH = 0b110001;
    public static final int OP_LDW = 0b110011;
    public static final int OP_JAL = 0b101011;
    public static final int OP_JR = 0b101010;
    public static final int OP_CMP_REG = 0b000011;
    public static final int OP_CMP_IMM = 0b010011;
    public static final int OP_XOR_REG = 0b001110;
    public static final int OP_XOR_IMM = 0b101110;
    public static final int OP_SHL_IMM = 0b010100;
    public static final int OP_SHL_REG = 0b000100;
    public static final int OP_SHR_IMM = 0b010101;
    public static final int OP_SHR_REG = 0b000101;
    public static final int OP_SAR_IMM = 0b010111;
    public static final int OP_SAR_REG = 0b000111;
    public static final int OP_ADD_REG = 0b000001;
    public static final int OP_ADDI = 0b101001;
    public static final int OP_AND_IMM = 0b101101;
    public static final int OP_AND_REG = 0b001101;
    public static final int OP_OR_IMM = 0b101100;
    public static final int OP_OR_REG = 0b001100;
    public static final int OP_MUL = 0b001000;
    public static final int OP_MULU = 0b001010;
    public static final int OP_DIV = 0b001001;
    public static final int OP_DIVU = 0b001011;
    public static final int OP_SUB = 0b000010;
    public static final int OP_RETI = 0b011001;
    public static final int OP_NOT = 0b001111;
    public static final int OP_ILL_1 = 0b110010;
    public static final int OP_SUBOP = 0b111110;
    public static final int OP_SETF = 0b010010;
    public static final int OP_CAXI = 0b111010;

    public static final int OP_BITSTRING = 0b011111;

    public static final int BITSTRING_ANDBSU = 0b001;
    public static final int BITSTRING_ANDNBSU = 0b101;
    public static final int BITSTRING_MOVBSU = 0b011;
    public static final int BITSTRING_NOTBSU = 0b111;
    public static final int BITSTRING_ORBSU = 0b000;
    public static final int BITSTRING_ORNBSU = 0b100;
    public static final int BITSTRING_XORBSU = 0b010;
    public static final int BITSTRING_XORNBSU = 0b110;

    public static final int BCOND_BNE = 0b1010;
    public static final int BCOND_BL = 0b0001;
    public static final int BCOND_BNL = 0b1001;
    public static final int BCOND_BE = 0b0010;
    public static final int BCOND_BR = 0b0101;
    public static final int BCOND_BLT = 0b0110;
    public static final int BCOND_BGE = 0b1110;
    public static final int BCOND_BLE = 0b0111;
    public static final int BCOND_BGT = 0b1111;
    public static final int BCOND_BNH = 0b0011;
    public static final int BCOND_NOP = 0b1101;
    public static final int BCOND_BN = 0b0100;
    public static final int BCOND_BP = 0b1100;
    public static final int BCOND_BH = 0b1011;
    public static final int BCOND_BV = 0b0000;

    public static final int SUBOP_ADDF_S = 0b000100;
    public static final int SUBOP_SUBF_S = 0b000101;
    public static final int SUBOP_MULF_S = 0b000110;
    public static final int SUBOP_DIVF_S = 0b000111;
    public static final int SUBOP_CMPF_S = 0b000000;
    public static final int SUBOP_CVT_WS = 0b000010;
    public static final int SUBOP_CVT_SW = 0b000011;
    public static final int SUBOP_REV = 0b001010;
    public static final int SUBOP_XB = 0b001000;
    public static final int SUBOP_XH = 0b001001;
    public static final int SUBOP_MPYHW = 0b001100;
    public static final int SUBOP_TRNC_SW = 0b001011;

    public enum AccessWidth {
        Byte(java.lang.Byte.BYTES),
        HalfWord(Short.BYTES),
        Word(Integer.BYTES);
        private final int bytes;

        AccessWidth(int bytes) {
            this.bytes = bytes;
        }

        public int getBytes() {
            return bytes;
        }
    }
}
