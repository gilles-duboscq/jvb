package gd.twohundred.jvb.components.interfaces;

public interface AudioOut {
    int OUTPUT_SAMPLING_DECIHZ = 416667;
    int OUTPUT_BITS = 10;

    void update(int left, int right);
}
