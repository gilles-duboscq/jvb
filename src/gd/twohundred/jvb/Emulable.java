package gd.twohundred.jvb;

public interface Emulable extends Resetable {
    int tick(int targetCycles);
}
