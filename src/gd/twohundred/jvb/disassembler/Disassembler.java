package gd.twohundred.jvb.disassembler;

import gd.twohundred.jvb.components.interfaces.ReadOnlyMemory;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.cpu.Instructions.OPCODE_POS;

public class Disassembler {
    public static Instruction disassemble(ReadOnlyMemory src, int pc) {
        int firstHalf = src.getHalfWord(pc);
        int opcode = extractU(firstHalf, OPCODE_POS, OPCODE_LEN);
        Format format = Format.decode(opcode);
        switch (format) {
            case I:
                return FormatIInstruction.decode(firstHalf);
            case II:
                return FormatIIInstruction.decode(firstHalf);
            case II_BIT_STRING:
                return FormatIIBitStringnstruction.decode(firstHalf);
            case III:
                return FormatIIIInstruction.decode(firstHalf);
            case IV:
                return FormatIVInstruction.decode(firstHalf, src.getHalfWord(pc + 2));
            case V:
                return FormatVInstruction.decode(firstHalf, src.getHalfWord(pc + 2));
            case VI:
                return FormatVIInstruction.decode(firstHalf, src.getHalfWord(pc + 2));
            case VII:
                return FormatVIIInstruction.decode(firstHalf, src.getHalfWord(pc + 2));
        }
        throw new RuntimeException("should not reach here");
    }
}
