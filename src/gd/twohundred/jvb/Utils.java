package gd.twohundred.jvb;

public class Utils {
    public static String leftPad(String s, char c, int length) {
        if (s.length() < length) {
            StringBuilder sb = new StringBuilder();
            for (int  i = 0 ; i < length - s.length(); i++) {
                sb.append(c);
            }
            sb.append(s);
            return sb.toString();
        }
        return s;
    }

    public static int extractU(int v, int pos, int len) {
        return (v >> pos) & mask(len);
    }

    public static int extractS(int v, int pos, int len) {
        return signExtend(extractU(v, pos, len), len);
    }

    private static int mask(int len) {
        return (1 << len) - 1;
    }

    public static int signExtend(int v, int bits) {
        int shift = 32 - bits;
        return ((v << shift) >> shift);
    }

    public static boolean testBits(int v, int bits, int pos, int len) {
        return (v & (mask(len) << pos)) == bits << pos;
    }
}
