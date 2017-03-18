package gd.twohundred.jvb.disassembler;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.components.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.components.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.Instructions.REG1_POS;
import static gd.twohundred.jvb.components.Instructions.REG2_LEN;
import static gd.twohundred.jvb.components.Instructions.REG2_POS;
import static gd.twohundred.jvb.components.Instructions.SUB_OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.SUB_OPCODE_POS;

public class FormatVIIInstruction implements Instruction{
    private final FormatVIIInstructionType type;
    private final int reg2;
    private final int reg1;

    public FormatVIIInstruction(FormatVIIInstructionType type, int reg2, int reg1) {
        this.type = type;
        this.reg2 = reg2;
        this.reg1 = reg1;
    }

    public FormatVIIInstructionType getType() {
        return type;
    }

    public int getReg2() {
        return reg2;
    }

    public int getReg1() {
        return reg1;
    }

    public static FormatVIIInstruction decode(int firstHalf, int secondHalf) {
        int subOp = extractU(secondHalf, SUB_OPCODE_POS - 16, SUB_OPCODE_LEN);
        int reg1 = extractU(firstHalf, REG1_POS, REG1_LEN);
        int reg2 = extractU(firstHalf, REG2_POS, REG2_LEN);
        FormatVIIInstructionType type = FormatVIIInstructionType.decode(subOp);
        if (type == null) {
            return null;
        }
        return new FormatVIIInstruction(type, reg2, reg1);
    }

    @Override
    public String toString() {
        String lowerType = type.name().replace('_', '.').toLowerCase();
        switch (type) {
            case XB:
            case XH:
                if (reg1 == 0) {
                    return String.format("%-8s r%d", lowerType, reg2);
                } else {
                    return String.format("%-8s r%d, r%d (?)", lowerType, reg2, reg1);
                }
            default:
                return String.format("%-8s r%d, r%d", lowerType, reg2, reg1);
        }
    }

    @Override
    public Format getFormat() {
        return Format.VII;
    }

    public enum FormatVIIInstructionType {
        ADDF_S(0b000100),
        CMPF_S(0b000000),
        CVT_SW(0b000011),
        CVT_WS(0b000010),
        DIVF_S(0b000111),
        MULF_S(0b000110),
        SUBF_S(0b000101),
        TRNC_SW(0b001011),
        MPYHW(0b001100),
        REV(0b001010),
        XB(0b001000),
        XH(0b001001);
        private final int encoding;
        private static final FormatVIIInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatVIIInstructionType[max + 1];
            for (FormatVIIInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatVIIInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatVIIInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            if (top6 >= TABLE.length) {
                return null;
            }
            return TABLE[top6];
        }
    }
}
