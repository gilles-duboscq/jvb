package gd.twohundred.jvb.disassembler;

import static gd.twohundred.jvb.Utils.topU;

public enum Format {
    I,
    II,
    II_BIT_STRING,
    III,
    IV,
    V,
    VI,
    VII;

    public static Format decode(int top6) {
        assert (top6 & 0b111111) == top6;
        int top2 = topU(top6, 2, 6);
        if (top2 == 0b00) {
            return I;
        } else if (top2 == 0b01) {
            if (top6 == 0b011111) {
                return II_BIT_STRING;
            }
            return II;
        }
        int top3 = topU(top6, 3, 6);
        if (top3 == 0b100) {
            return III;
        } else if (top3 == 0b101) {
            int top5 = topU(top6, 5, 6);
            if (top5 == 0b10101) {
                return IV;
            }
            return V;
        }
        if (top6 == 0b111110) {
            return VII;
        }
        return VI;
    }

    public int getLength() {
        switch (this) {
            case I:
            case II:
            case II_BIT_STRING:
            case III:
                return 2;
            case IV:
            case V:
            case VI:
            case VII:
                return 2;
        }
        throw new RuntimeException("should not reach here: " + this);
    }
}
