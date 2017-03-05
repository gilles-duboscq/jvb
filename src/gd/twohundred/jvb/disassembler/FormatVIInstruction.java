package gd.twohundred.jvb.disassembler;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.topU;
import static gd.twohundred.jvb.components.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.components.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.Instructions.REG1_POS;
import static gd.twohundred.jvb.components.Instructions.REG2_LEN;
import static gd.twohundred.jvb.components.Instructions.REG2_POS;

public class FormatVIInstruction implements Instruction {
    private final FormatVIInstructionType type;
    private final int reg2;
    private final int reg1;
    private final int disp;

    public FormatVIInstruction(FormatVIInstructionType type, int reg2, int reg1, int disp) {
        this.type = type;
        this.reg2 = reg2;
        this.reg1 = reg1;
        this.disp = disp;
    }

    public FormatVIInstructionType getType() {
        return type;
    }

    public int getReg2() {
        return reg2;
    }

    public int getReg1() {
        return reg1;
    }

    public int getDisp() {
        return disp;
    }

    public static FormatVIInstruction decode(int firstHalf, int secondHalf) {
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        int reg1 = extractU(firstHalf, REG1_POS, REG1_LEN);
        int reg2 = extractU(firstHalf, REG2_POS, REG2_LEN);
        FormatVIInstructionType type = FormatVIInstructionType.decode(opcode);
        if (type == null) {
            return null;
        }
        return new FormatVIInstruction(type, reg2, reg1, signExtend(secondHalf, 16));
    }

    @Override
    public String toString() {
        String lowerType = type.name().replace('_', '.').toLowerCase();
        switch (type) {
            case LD_B:
            case LD_H:
            case LD_W:
            case IN_B:
            case IN_H:
            case IN_W:
                if (disp == 0) {
                    return String.format("%-8s r%d, [r%d]", lowerType, reg2, reg1);
                } else if (disp > 0) {
                    return String.format("%-8s r%d, [r%d + %d]", lowerType, reg2, reg1, disp);
                } else {
                    assert disp < 0;
                    return String.format("%-8s r%d, [r%d - %d]", lowerType, reg2, reg1, -disp);
                }
            case CAXI:
            case ST_B:
            case ST_H:
            case ST_W:
            case OUT_B:
            case OUT_H:
            case OUT_W:
                if (disp == 0) {
                    return String.format("%-8s [r%d], %d", lowerType, reg1, reg2);
                } else if (disp > 0) {
                    return String.format("%-8s [r%d + %d], %d", lowerType, reg1, disp, reg2);
                } else {
                    assert disp < 0;
                    return String.format("%-8s [r%d - %d], %d", lowerType, reg1, -disp, reg2);
                }
        }
        throw new RuntimeException("shoudl not reach here");
    }

    @Override
    public Format getFormat() {
        return Format.VI;
    }

    public enum FormatVIInstructionType {
        CAXI(0b1010),
        IN_B(0b1000),
        IN_H(0b1001),
        IN_W(0b1011),
        LD_B(0b0000),
        LD_H(0b0001),
        LD_W(0b0011),
        OUT_B(0b1100),
        OUT_H(0b1101),
        OUT_W(0b1111),
        ST_B(0b0100),
        ST_H(0b0101),
        ST_W(0b0111);
        private final int encoding;
        private static final FormatVIInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatVIInstructionType[max + 1];
            for (FormatVIInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatVIInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatVIInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            assert topU(top6, 2, 6) == 0b11 && top6 != 111110;
            return TABLE[top6 & 0b111];
        }
    }
}
