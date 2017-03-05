package gd.twohundred.jvb.disassembler;

import java.util.Arrays;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.components.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.Instructions.REG1_POS;

public enum FormatIIBitStringnstruction implements Instruction {
    ANDBSU(0b01001),
    ANDNBSU(0b01101),
    MOVBSU(0b01011),
    NOTBSU(0b01111),
    ORBSU(0b01000),
    ORNBSU(0b01100),
    XORBSU(0b01010),
    XORNBSU(0b01110),
    SCH0BSD(0b00001),
    SCH0BSU(0b00000),
    SCH1BSD(0b00011),
    SCH1BSU(0b00010);
    private final int encoding;
    private static final FormatIIBitStringnstruction TABLE[];

    static {
        int max = Arrays.stream(values()).mapToInt(i -> i.encoding).max().getAsInt();
        TABLE = new FormatIIBitStringnstruction[max + 1];
        for (FormatIIBitStringnstruction i : values()) {
            assert TABLE[i.encoding] == null;
            TABLE[i.encoding] = i;
        }
    }

    FormatIIBitStringnstruction(int encoding) {
        this.encoding = encoding;
    }

    public static FormatIIBitStringnstruction decode(int firstHalf) {
        int imm5 = extractU(firstHalf, REG1_POS, REG1_LEN);
        if (imm5 >= TABLE.length) {
            return null;
        }
        return TABLE[imm5];
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    @Override
    public Format getFormat() {
        return Format.II_BIT_STRING;
    }
}
