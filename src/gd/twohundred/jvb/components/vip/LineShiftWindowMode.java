package gd.twohundred.jvb.components.vip;

import static gd.twohundred.jvb.Utils.signExtend;

public class LineShiftWindowMode extends BackgroundedWindowMode {
    public static final int ID = 1;
    public static final LineShiftWindowMode INSTANCE = new LineShiftWindowMode();

    @Override
    public int getBackgroundX(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left) {
        int parameterAddress = (window.getParameterIndex() + 2 * y + (left ? 0 : 1)) * Short.BYTES;
        BackgroundSegmentsAndParametersRAM parameterTable = vip.getBackgroundSegmentsAndWindowParameterTable();
        int shift = signExtend(parameterTable.getHalfWord(parameterAddress), Short.SIZE);
        return super.getBackgroundX(window, vip, x, y, left) + shift;
    }

    @Override
    public int getId() {
        return ID;
    }
}
