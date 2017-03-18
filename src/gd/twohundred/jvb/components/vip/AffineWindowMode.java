package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.FixedPoint;

import static gd.twohundred.jvb.Utils.signExtend;

public class AffineWindowMode extends BackgroundedWindowMode {
    public static final int ID = 2;
    public static final AffineWindowMode INSTANCE = new AffineWindowMode();

    private static final int AFFINE_PARAMETER_LEN_SHORTS = 8;
    private static final int BG_X_OFFSET_SHORTS = 0;
    private static final int BG_PARALLAX_OFFSET_SHORTS = 1;
    private static final int BG_Y_OFFSET_SHORTS = 2;
    private static final int BG_X_INC_OFFSET_SHORTS = 3;
    private static final int BG_Y_INC_OFFSET_SHORTS = 4;

    private static final int INC_FRACTIONAL_BITS = 9;
    private static final int BG_FRACTIONAL_BITS = 3;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public String getShortName() {
        return "A";
    }

    @Override
    public int getBackgroundY(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left) {
        return getBackgroundPos(window, vip, x, y, left, BG_Y_INC_OFFSET_SHORTS, BG_Y_OFFSET_SHORTS);
    }

    @Override
    public int getBackgroundX(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left) {
        return getBackgroundPos(window, vip, x, y, left, BG_X_INC_OFFSET_SHORTS, BG_X_OFFSET_SHORTS);
    }

    private int getBackgroundPos(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left, int incOffsetShorts, int bgOffsetShorts) {
        int parameterBaseAddress = (window.getParameterIndex() + AFFINE_PARAMETER_LEN_SHORTS * y) * Short.BYTES;
        BackgroundSegmentsAndParametersRAM parameterTable = vip.getBackgroundSegmentsAndWindowParameterTable();
        int bgParallax = signExtend(parameterTable.getHalfWord(parameterBaseAddress + BG_PARALLAX_OFFSET_SHORTS * Short.BYTES), Short.SIZE);
        FixedPoint bgYIncrement = new FixedPoint(parameterTable.getHalfWord(parameterBaseAddress + incOffsetShorts * Short.BYTES), INC_FRACTIONAL_BITS);
        FixedPoint bgY = new FixedPoint(parameterTable.getHalfWord(parameterBaseAddress + bgOffsetShorts * Short.BYTES), BG_FRACTIONAL_BITS);
        if ((bgParallax < 0) ^ left) {
            return (int) bgYIncrement.mul(x).add(bgY).roundToLong();
        } else {
            return (int) bgYIncrement.mul(x + bgParallax).add(bgY).roundToLong();
        }
    }
}
