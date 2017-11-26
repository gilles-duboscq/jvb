package gd.twohundred.jvb.disassembler;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.topU;
import static gd.twohundred.jvb.components.cpu.Instructions.DISP26_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_POS;

public class FormatIVInstruction implements Instruction {
    private final FormatIVInstructionType type;
    private final int disp;

    public FormatIVInstruction(FormatIVInstructionType type, int disp) {
        this.type = type;
        this.disp = disp;
    }

    public FormatIVInstructionType getType() {
        return type;
    }

    public int getDisp() {
        return disp;
    }

    public static FormatIVInstruction decode(int firstHalf, int secondHlaf) {
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        int disp26 = signExtend(secondHlaf | (firstHalf << 16), DISP26_LEN);
        return new FormatIVInstruction(FormatIVInstructionType.decode(opcode), disp26);
    }

    @Override
    public String toString() {
        return String.format("%-8s %+d", type.name().toLowerCase(), disp);
    }

    @Override
    public Format getFormat() {
        return Format.IV;
    }

    public enum FormatIVInstructionType {
        JAL,
        JR;

        public static FormatIVInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            assert topU(top6, 5, 6) == 0b10101;
            if ((top6 & 0b1) == 0) {
                return JR;
            } else {
                return JAL;
            }
        }
    }
}
