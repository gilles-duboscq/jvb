package gd.twohundred.jvb;

public interface InterruptSource {
    boolean raised();
    void clear();
    short exceptionCode();
    int handlerAddress();
}
