package gd.twohundred.jvb.components.interfaces;

public interface ExactlyEmulable extends Emulable {
    @Override
    default int tick(int cycles) {
        tickExact(cycles);
        return cycles;
    }

    void tickExact(int cycles);
}
