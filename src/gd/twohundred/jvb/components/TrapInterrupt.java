package gd.twohundred.jvb.components;

public class TrapInterrupt extends SimpleInterrupt {
    private final int vector;

    public TrapInterrupt(int vector) {
        super(vector >= 0 ? InterruptType.TrapPos : InterruptType.TrapNeg);
        this.vector = vector;
    }

    @Override
    public short getExceptionCode() {
        return (short) (InterruptType.TrapPos.getBaseExceptionCode() + vector);
    }
}
