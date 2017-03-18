package gd.twohundred.jvb.components.interfaces;

public interface Emulable extends Resetable {
    long tick(long targetCycles);
}
