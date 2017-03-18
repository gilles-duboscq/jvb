package gd.twohundred.jvb.components.interfaces;

public interface ExactlyEmulable extends Emulable {
    @Override
    default long tick(long cycles) {
        tickExact(cycles);
        return cycles;
    }

    void tickExact(long cycles);
}
