package gd.twohundred.jvb;

import static gd.twohundred.jvb.OpcodeFormat.V;

public enum Opcode {
    MOVHI(0b101111, V);
    private final int encoding;
    private final OpcodeFormat format;

    Opcode(int encoding, OpcodeFormat format) {
        this.encoding = encoding;
        this.format = format;
    }

    public int getEncoding() {
        return encoding;
    }

    public OpcodeFormat getFormat() {
        return format;
    }
}
