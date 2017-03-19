package gd.twohundred.jvb.disassembler;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractS;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.components.Instructions.COND_LEN;
import static gd.twohundred.jvb.components.Instructions.COND_POS;
import static gd.twohundred.jvb.components.Instructions.DISP9_LEN;
import static gd.twohundred.jvb.components.Instructions.DISP9_POS;

public class FormatIIIInstruction implements Instruction {
    private final FormatIIIInstructionType type;
    private final int disp;

    public FormatIIIInstruction(FormatIIIInstructionType type, int disp) {
        this.type = type;
        this.disp = disp;
    }

    public FormatIIIInstructionType getType() {
        return type;
    }

    public int getDisp() {
        return disp;
    }

    public static FormatIIIInstruction decode(int firstHalf) {
        int cond = extractU(firstHalf, COND_POS, COND_LEN);
        int disp9 = extractS(firstHalf, DISP9_POS, DISP9_LEN);
        return new FormatIIIInstruction(FormatIIIInstructionType.decode(cond), disp9);
    }

    @Override
    public String toString() {
        if (type == FormatIIIInstructionType.NOP) {
            return "nop";
        } else {
            return String.format("%-8s %+d", type.name().toLowerCase(), disp);
        }
    }

    @Override
    public Format getFormat() {
        return Format.III;
    }

    public enum FormatIIIInstructionType {
        BV(0b0000),
        BL(0b0001),
        BE(0b0010),
        BNH(0b0011),
        BN(0b0100),
        BR(0b0101),
        BLT(0b0110),
        BLE(0b0111),
        BNV(0b1000),
        BNL(0b1001),
        BNE(0b1010),
        BH(0b1011),
        BP(0b1100),
        NOP(0b1101),
        BGE(0b1110),
        BGT(0b1111);
        private final int encoding;
        private static final FormatIIIInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatIIIInstructionType[max + 1];
            for (FormatIIIInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatIIIInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatIIIInstructionType decode(int cond) {
            assert (cond & 0b1111) == cond;
            return TABLE[cond];
        }

        public String conditionName() {
            switch (this) {
                case BR:
                    return "T";
                case NOP:
                    return "F";
                default:
                    return name().substring(1);
            }
        }
    }
}
