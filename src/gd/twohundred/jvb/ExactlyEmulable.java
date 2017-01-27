package gd.twohundred.jvb;

public interface ExactlyEmulable extends Emulable {
    @Override
    default int tick(int cycles) {
        tickExact(cycles);
        return cycles;
    }

    void tickExact(int cycles);
}
