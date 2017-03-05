package gd.twohundred.jvb.disassembler;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.topU;
import static gd.twohundred.jvb.components.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.components.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.Instructions.REG1_POS;
import static gd.twohundred.jvb.components.Instructions.REG2_LEN;
import static gd.twohundred.jvb.components.Instructions.REG2_POS;

public class FormatIInstruction implements Instruction {
    private final FormatIInstructionType type;
    private final int reg2;
    private final int reg1;

    public FormatIInstruction(FormatIInstructionType type, int reg2, int reg1) {
        this.type = type;
        this.reg2 = reg2;
        this.reg1 = reg1;
    }

    public FormatIInstructionType getType() {
        return type;
    }

    public int getReg2() {
        return reg2;
    }

    public int getReg1() {
        return reg1;
    }

    public static FormatIInstruction decode(int firstHalf) {
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        int reg1 = extractU(firstHalf, REG1_POS, REG1_LEN);
        int reg2 = extractU(firstHalf, REG2_POS, REG2_LEN);
        return new FormatIInstruction(FormatIInstructionType.decode(opcode), reg2, reg1);
    }

    @Override
    public String toString() {
        return String.format("%-8s r%d, r%d", type.name().toLowerCase(), reg2, reg1);
    }

    @Override
    public Format getFormat() {
        return Format.I;
    }

    public enum FormatIInstructionType {
        ADD(0b0001),
        AND(0b1101),
        CMP(0b0011),
        DIV(0b1001),
        DIVU(0b1011),
        JMP(0b0110),
        MOV(0b0000),
        MUL(0b1000),
        MULU(0b1010),
        NOT(0b1111),
        OR(0b1100),
        SAR(0b0111),
        SHL(0b0100),
        SHR(0b0101),
        SUB(0b0010),
        XOR(0b1110);
        private final int encoding;
        private static final FormatIInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatIInstructionType[max + 1];
            for (FormatIInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatIInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatIInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            assert topU(top6, 2, 6) == 0b00;
            return TABLE[top6];
        }
    }
}
