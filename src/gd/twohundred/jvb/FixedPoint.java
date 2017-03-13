package gd.twohundred.jvb;

public class FixedPoint {
    private final long value;
    private final int shift;

    public FixedPoint(long value, int shift) {
        this.value = value;
        this.shift = shift;
    }

    public FixedPoint mul(int a) {
        return new FixedPoint(value * a, shift);
    }

    public FixedPoint add(FixedPoint a) {
        if (a.shift > this.shift) {
            return a.add(this);
        } else{
            long newValue = (a.value << (this.shift - a.shift)) + this.value;
            return new FixedPoint(newValue, shift);
        }
    }

    public long roundToLong() {
        return value >> shift;
    }
}
