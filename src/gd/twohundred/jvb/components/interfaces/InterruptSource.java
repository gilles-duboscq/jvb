package gd.twohundred.jvb.components.interfaces;

public interface InterruptSource {
    boolean raised();
    void clear();
    short exceptionCode();
    int handlerAddress();
}
