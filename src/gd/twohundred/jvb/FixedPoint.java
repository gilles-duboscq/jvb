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

    @Override
    public String toString() {
        long positiveValue = value;
        if (positiveValue < 0) {
            positiveValue = -positiveValue;
        }
        double fractional = 0;
        double weight = 1.0 / 2.0;
        long v = positiveValue << Long.SIZE - shift;
        for (int i = 0; i < shift; i++) {
            if (v < 0) {
                fractional += weight;
            }
            weight /= 2.0;
            v <<= 1;
        }
        assert fractional < 1.0;
        return (value < 0 ? "-" : "") + Long.toString(positiveValue >> shift) + Double.toString(fractional).substring(1);
    }
}
