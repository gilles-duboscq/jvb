package gd.twohundred.jvb.components.interfaces;

public interface InputProvider {
    boolean read(Inputs inputs);

    enum Inputs {
        LowBattery,
        One,
        A,
        B,
        R,
        L,
        RightDPadUp,
        RightDPadRight,
        LeftDPadRight,
        LeftDPadLeft,
        LeftDPadDown,
        LeftDPadUp,
        Start,
        Select,
        RightDPadLeft,
        RightDPadDown;

        public int offset() {
            return this.ordinal();
        }

        public static Inputs get(int offset) {
            return values()[offset];
        }
    }
}
