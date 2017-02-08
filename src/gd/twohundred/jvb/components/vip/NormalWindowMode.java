package gd.twohundred.jvb.components.vip;

public class NormalWindowMode extends BackgroundedWindowMode {
    public static final int ID = 0;
    public static final NormalWindowMode INSTANCE = new NormalWindowMode();

    @Override
    public int getId() {
        return ID;
    }
}
