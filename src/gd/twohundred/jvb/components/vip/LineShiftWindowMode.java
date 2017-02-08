package gd.twohundred.jvb.components.vip;

public class LineShiftWindowMode extends BackgroundedWindowMode {
    public static final int ID = 1;
    public static final LineShiftWindowMode INSTANCE = new LineShiftWindowMode();

    @Override
    public void draw(WindowAttributes window, VirtualImageProcessor vip, boolean left) {
        super.draw(window, vip, left);
        throw new RuntimeException("NYI");
    }

    @Override
    public int getId() {
        return ID;
    }
}
