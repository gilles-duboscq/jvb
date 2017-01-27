package gd.twohundred.jvb;

public class Utils {
    public static String leftPad(String s, char c, int length) {
        if (s.length() < length) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length - s.length(); i++) {
                sb.append(c);
            }
            sb.append(s);
            return sb.toString();
        }
        return s;
    }

    public static int extractU(int v, int pos, int len) {
        assert pos >= 0 && pos < 32;
        assert len > 0 && len + pos < 32;
        return (v >> pos) & mask(len);
    }

    public static int extractS(int v, int pos, int len) {
        return signExtend(extractU(v, pos, len), len);
    }

    public static int mask(int len) {
        return (1 << len) - 1;
    }

    public static int signExtend(int v, int bits) {
        assert bits <= 32 && bits > 0;
        int shift = 32 - bits;
        return ((v << shift) >> shift);
    }

    public static boolean testBits(int v, int bits, int pos, int len) {
        assert pos < 32 && pos >= 0;
        return (v & (mask(len) << pos)) == bits << pos;
    }

    public static boolean testBit(int v, int pos) {
        return testBits(v, 1, pos, 1);
    }

    public static long nextPowerOfTwo(long v) {
        int leadingZeros = Long.numberOfLeadingZeros(v);
        if (leadingZeros == 0) {
            throw new ArithmeticException();
        }
        return 1L << (64 - leadingZeros);
    }

    public static int intBit(int pos) {
        return 1 << pos;
    }

    public static int intBits(int... positions) {
        int v = 0;
        for (int pos : positions) {
            v |= 1 << pos;
        }
        return v;
    }

    public static int intBit(int pos, boolean set) {
        return (set ? 1 : 0) << pos;
    }

    public static String toBinary(long v, int len) {
        return Utils.leftPad(Long.toBinaryString(v), '0', len);
    }

    public static char signStr(long v) {
        return v > 0 ? '+' : '-';
    }
}
