package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.WINDOW_ATTRIBUTES_START;

public class WindowAttributes implements ReadWriteMemory, Resetable {
    public static final int SIZE = 32;

    private static final int FLAGS_START = 0x00;
    private static final int X_START = 0x02;
    private static final int PARALLAX_START = 0x04;
    private static final int Y_START = 0x06;
    private static final int BACKGROUND_X_START = 0x08;
    private static final int BACKGROUND_PARALLAX_START = 0x0a;
    private static final int BACKGROUND_Y_START = 0x0c;
    private static final int WIDTH_START = 0x0e;
    private static final int HEIGHT_START = 0x10;
    private static final int PARAMETER_INDEX_START = 0x12;
    private static final int OOB_CHARACTER_START = 0x14;
    private static final int SCRATCH_START = 0x16;

    private static final int FLAGS_BASE_SEGMENT_POS = 0;
    private static final int FLAGS_BASE_SEGMENT_LEN = 4;
    private static final int FLAGS_STOP_POS = 6;
    private static final int FLAGS_USE_OOB_CHARACTER_POS = 7;
    private static final int FLAGS_BACKGROUND_HEIGHT_POS = 8;
    private static final int FLAGS_BACKGROUND_HEIGHT_LEN = 2;
    private static final int FLAGS_BACKGROUND_WIDTH_POS = 10;
    private static final int FLAGS_BACKGROUND_WIDTH_LEN = 2;
    private static final int FLAGS_MODE_POS = 12;
    private static final int FLAGS_MODE_LEN = 2;
    private static final int FLAGS_RIGHT_POS = 14;
    private static final int FLAGS_LEFT_POS = 15;

    private final int id;

    private byte baseSegmentIndex;
    private boolean isStop;
    private boolean useOutOfBoundsCharacter;
    private int backgroundHeightSegments;
    private int backgroundWidthSegments;
    private WindowMode mode;
    private boolean drawLeft;
    private boolean drawRight;
    private short x;
    private short y;
    private short backgroundX;
    private short backgroundY;
    private short height;
    private short width;
    private short parameterIndex;
    private short outOfBoundsCharacter;
    private short parallax;
    private short backgroundParallax;

    public WindowAttributes(int id) {
        this.id = id;
    }

    @Override
    public int getStart() {
        return WINDOW_ATTRIBUTES_START + SIZE * id;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public void setHalfWord(int address, short value) {
        if (address >= SCRATCH_START) {
            throw new BusError(address, BusError.Reason.Permission);
        }
        switch (address) {
            case FLAGS_START:
                setFlags(value);
                return;
            case X_START:
                x = value;
                return;
            case Y_START:
                y = value;
                return;
            case BACKGROUND_X_START:
                backgroundX = value;
                return;
            case BACKGROUND_Y_START:
                backgroundY = value;
                return;
            case PARALLAX_START:
                parallax = value;
                return;
            case BACKGROUND_PARALLAX_START:
                backgroundParallax = value;
                return;
            case WIDTH_START:
                width = value;
                return;
            case HEIGHT_START:
                height = value;
                return;
            case PARAMETER_INDEX_START:
                parameterIndex = value;
                return;
            case OOB_CHARACTER_START:
                outOfBoundsCharacter = value;
                return;
        }
        throw new BusError(address, BusError.Reason.Unimplemented);
    }

    private void setFlags(short value) {
        baseSegmentIndex = (byte) extractU(value, FLAGS_BASE_SEGMENT_POS, FLAGS_BASE_SEGMENT_LEN);
        isStop = testBit(value, FLAGS_STOP_POS);
        useOutOfBoundsCharacter = testBit(value, FLAGS_USE_OOB_CHARACTER_POS);
        backgroundHeightSegments = 1 << extractU(value, FLAGS_BACKGROUND_HEIGHT_POS, FLAGS_BACKGROUND_HEIGHT_LEN);
        backgroundWidthSegments = 1 << extractU(value, FLAGS_BACKGROUND_WIDTH_POS, FLAGS_BACKGROUND_WIDTH_LEN);
        mode = WindowMode.get(extractU(value, FLAGS_MODE_POS, FLAGS_MODE_LEN));
        drawRight = testBit(value, FLAGS_RIGHT_POS);
        drawLeft = testBit(value, FLAGS_LEFT_POS);
    }

    @Override
    public int getHalfWord(int address) {
        throw new BusError(address, BusError.Reason.Unimplemented);
    }

    @Override
    public void setByte(int address, byte value) {
        int halfWord = getHalfWord(address & ~1);
        if ((address & 1) == 0) {
            halfWord = insert(value, 0, Byte.SIZE, halfWord);
        } else {
            halfWord = insert(value, Byte.SIZE, Byte.SIZE, halfWord);
        }
        setHalfWord(address & ~1, (short) halfWord);
    }

    @Override
    public int getByte(int address) {
        int halfWord = getHalfWord(address & ~1);
        return (address & 1) == 0 ? halfWord & 0xff : (halfWord >> 8) & 0xff;
    }

    @Override
    public void reset() {

    }

    public boolean isStop() {
        return isStop;
    }

    public WindowMode getMode() {
        return mode;
    }

    public byte getBaseSegmentIndex() {
        return baseSegmentIndex;
    }

    public boolean isUseOutOfBoundsCharacter() {
        return useOutOfBoundsCharacter;
    }

    public int getBackgroundHeightSegments() {
        return backgroundHeightSegments;
    }

    public int getBackgroundWidthSegments() {
        return backgroundWidthSegments;
    }

    public boolean isDrawLeft() {
        return drawLeft;
    }

    public boolean isDrawRight() {
        return drawRight;
    }

    public short getX() {
        return x;
    }

    public short getY() {
        return y;
    }

    public short getBackgroundX() {
        return backgroundX;
    }

    public short getBackgroundY() {
        return backgroundY;
    }

    public short getHeight() {
        return height;
    }

    public short getWidth() {
        return width;
    }

    public short getParameterIndex() {
        return parameterIndex;
    }

    public short getOutOfBoundsCharacter() {
        return outOfBoundsCharacter;
    }

    public short getParallax() {
        return parallax;
    }

    public short getBackgroundParallax() {
        return backgroundParallax;
    }
}
