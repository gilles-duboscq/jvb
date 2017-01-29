package gd.twohundred.jvb.components;

import gd.twohundred.jvb.components.interfaces.Interrupt;

public class SimpleInterrupt implements Interrupt {
    private final InterruptType type;
    private Interrupt next;

    public SimpleInterrupt(InterruptType type) {
        this.type = type;
    }

    @Override
    public InterruptType getType() {
        return type;
    }

    @Override
    public short getExceptionCode() {
        return type.getBaseExceptionCode();
    }

    @Override
    public Interrupt getNext() {
        return next;
    }

    @Override
    public void setNext(Interrupt next) {
        this.next = next;
    }
}
