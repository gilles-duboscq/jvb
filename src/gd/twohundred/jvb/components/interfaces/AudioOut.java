package gd.twohundred.jvb.components.interfaces;

public interface AudioOut {
    int OUTPUT_SAMPLING_HZ = 41700;
    int OUTPUT_BITS = 10;

    void update(int left, int right);
}
