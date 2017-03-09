package gd.twohundred.jvb.disassembler;

import gd.twohundred.jvb.components.CPU;

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

public class FormatVInstruction implements Instruction {
    private final FormatVInstructionType type;
    private final int reg2;
    private final int reg1;
    private final int uimm;

    public FormatVInstruction(FormatVInstructionType type, int reg2, int reg1, int uimm) {
        this.type = type;
        this.reg2 = reg2;
        this.reg1 = reg1;
        this.uimm = uimm;
    }

    public FormatVInstructionType getType() {
        return type;
    }

    public int getReg2() {
        return reg2;
    }

    public int getReg1() {
        return reg1;
    }

    public int getUimm() {
        return uimm;
    }

    public static FormatVInstruction decode(int firstHalf, int secondHalf) {
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        int reg1 = extractU(firstHalf, REG1_POS, REG1_LEN);
        int reg2 = extractU(firstHalf, REG2_POS, REG2_LEN);
        return new FormatVInstruction(FormatVInstructionType.decode(opcode), reg2, reg1, secondHalf);
    }

    @Override
    public String toString() {
        int imm = uimm;
        String lowerType = type.name().toLowerCase();
        switch (type) {
            case ADDI:
            case MOVEA:
                imm = signExtend(imm, 5);
                return String.format("%-8s r%d, r%d, %d", lowerType, reg2, reg1, imm);
            case MOVHI:
                imm <<= 16;
            case ANDI:
            case ORI:
            case XORI:
                return String.format("%-8s r%d, r%d, %#04x", lowerType, reg2, reg1, imm);
        }
        throw new RuntimeException("should not reach here");
    }

    @Override
    public Format getFormat() {
        return Format.V;
    }

    public enum FormatVInstructionType {
        ADDI(0b001),
        ANDI(0b101),
        MOVEA(0b000),
        MOVHI(0b111),
        ORI(0b100),
        XORI(0b110);
        private final int encoding;
        private static final FormatVInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatVInstructionType[max + 1];
            for (FormatVInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatVInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatVInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            assert topU(top6, 3, 6) == 0b101;
            return TABLE[top6 & 0b111];
        }
    }

}
