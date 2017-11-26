package gd.twohundred.jvb.disassembler;

import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.disassembler.FormatIIIInstruction.FormatIIIInstructionType;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.topU;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.components.cpu.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.REG1_POS;
import static gd.twohundred.jvb.components.cpu.Instructions.REG2_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.REG2_POS;

public class FormatIIInstruction implements Instruction {
    private final FormatIIInstructionType type;
    private final int reg2;
    private final int uimm;

    public FormatIIInstruction(FormatIIInstructionType type, int reg2, int uimm) {
        this.type = type;
        this.reg2 = reg2;
        this.uimm = uimm;
    }

    public FormatIIInstructionType getType() {
        return type;
    }

    public int getReg2() {
        return reg2;
    }

    public int getUImm() {
        return uimm;
    }

    public static FormatIIInstruction decode(int firstHalf) {
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        int imm5 = extractU(firstHalf, REG1_POS, REG1_LEN);
        int reg2 = extractU(firstHalf, REG2_POS, REG2_LEN);
        FormatIIInstructionType type = FormatIIInstructionType.decode(opcode);
        if (type == null) {
            return null;
        }
        return new FormatIIInstruction(type, reg2, imm5);
    }

    @Override
    public String toString() {
        int imm = uimm;
        String lowerType = type.name().toLowerCase();
        switch (type) {
            case ADD:
            case CMP:
            case MOV:
                imm = signExtend(imm, 5);
            case SAR:
            case SHL:
            case SHR:
                return String.format("%-8s r%d, %d", lowerType, reg2, imm);
            case CLI:
            case SEI:
            case HALT:
            case RETI:
                return lowerType;
            case TRAP:
                return String.format("%-8s %d", lowerType, imm);
            case SETF:
                String conditionName;
                if ((imm & 0b1111) != imm) {
                    conditionName = "ILLEGAL";
                } else {
                    FormatIIIInstructionType bcond = FormatIIIInstructionType.decode(imm);
                    conditionName = bcond.conditionName();
                }
                return String.format("%-8s r%d, %s", lowerType, reg2, conditionName);
            case LDSR:
            case STSR:
                return String.format("%-8s r%d, %s", lowerType, reg2, CPU.getSystemRegisterName(imm));
        }
        throw new RuntimeException("should not reach here");
    }

    @Override
    public Format getFormat() {
        return Format.II;
    }

    public enum FormatIIInstructionType {
        ADD(0b0001),
        CLI(0b0110),
        HALT(0b1010),
        CMP(0b0011),
        LDSR(0b1100),
        MOV(0b0000),
        RETI(0b1001),
        SAR(0b0111),
        SEI(0b1110),
        SETF(0b0010),
        SHL(0b0100),
        SHR(0b0101),
        STSR(0b1101),
        TRAP(0b1000);
        private final int encoding;
        private static final FormatIIInstructionType TABLE[];

        static {
            int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
            TABLE = new FormatIIInstructionType[max + 1];
            for (FormatIIInstructionType i : values()) {
                assert TABLE[i.encoding] == null;
                TABLE[i.encoding] = i;
            }
        }

        FormatIIInstructionType(int encoding) {
            this.encoding = encoding;
        }

        public static FormatIIInstructionType decode(int top6) {
            assert (top6 & 0b111111) == top6;
            assert topU(top6, 2, 6) == 0b01;
            return TABLE[top6 & 0b1111];
        }
    }
}
