package gd.twohundred.jvb;

public class Utils {
    public static final long NANOS_PER_SECOND = 1_000_000_000L;

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
        assert pos >= 0 && pos < 32 : pos;
        assert len > 0 && len + pos < 32 : pos + ", " + len;
        return (v >> pos) & mask(len);
    }

    public static int topU(int v, int count, int len) {
        return extractU(v, len - count, count);
    }

    public static int extractNthU(int v, int n, int len) {
        return extractU(v, n * len, len);
    }

    public static int extractS(int v, int pos, int len) {
        return signExtend(extractU(v, pos, len), len);
    }

    public static int maskedMerge(int v, int mask, int into) {
        return ((into | v) & (~mask | v));
    }

    public static int insertNth(int v, int n, int len, int into) {
        return insert(v, n * len, len, into);
    }

    public static int insert(boolean v, int pos, int into) {
        return insert(v ? 1 : 0, pos, 1, into);
    }

    public static int insert(int v, int pos, int len, int into) {
        assert pos >= 0 && pos < 32;
        int set = v << pos;
        int affected = mask(pos, len);
        return maskedMerge(set, affected, into);
    }

    public static int mask(int len) {
        assert len <= 32 && len > 0;
        return (1 << len) - 1;
    }

    public static int mask(int pos, int len) {
        return mask(len) << pos;
    }

    public static int signExtend(int v, int bits) {
        assert bits <= 32 && bits > 0;
        int shift = 32 - bits;
        return ((v << shift) >> shift);
    }

    public static int zeroExtend(int v, int bits) {
        assert bits <= 32 && bits > 0;
        return v & mask(bits);
    }

    public static boolean testBits(int v, int bits, int pos, int len) {
        assert pos < 32 && pos >= 0;
        return (v & mask(pos, len)) == bits << pos;
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

    public static int repeat(int v, int patternLength, int targetLength) {
        assert (v & ~mask(patternLength)) == 0 : toBinary(v, Integer.SIZE) + " l=" + patternLength;
        int shift = 0;
        int result = 0;
        for (int i = 0; i < targetLength / patternLength; i++) {
            result |= v << shift;
        }
        return result & mask(targetLength);
    }

    public static int ceilDiv(int x, int y){
        if (x < 0 != y < 0) {
            return x / y;
        }
        return 1 + (x - 1) / y;
    }

    public static int log2(int x) {
        assert Integer.bitCount(x) == 1;
        return Integer.numberOfTrailingZeros(x);
    }

    public static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }
}
