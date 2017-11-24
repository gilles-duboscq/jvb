package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.WINDOW_ATTRIBUTES_START;

public class WindowAttributes implements ReadWriteMemory, Resetable {
    public static final int SIZE = 16 * Short.BYTES;

    private static final int FLAGS_OFFSET_SHORTS = 0;
    private static final int X_OFFSET_SHORTS = 1;
    private static final int PARALLAX_OFFSET_SHORTS = 2;
    private static final int Y_OFFSET_SHORTS = 3;
    private static final int BACKGROUND_X_OFFSET_SHORTS = 4;
    private static final int BACKGROUND_PARALLAX_OFFSET_SHORTS = 5;
    private static final int BACKGROUND_Y_OFFSET_SHORTS = 6;
    private static final int WIDTH_OFFSET_SHORTS = 7;
    private static final int HEIGHT_OFFSET_SHORTS = 8;
    private static final int PARAMETER_INDEX_OFFSET_SHORTS = 9;
    private static final int OOB_CHARACTER_OFFSET_SHORTS = 10;
    private static final int SCRATCH_OFFSET_SHORTS = 11;

    private static final int FLAGS_START = FLAGS_OFFSET_SHORTS * Short.BYTES;
    private static final int X_START = X_OFFSET_SHORTS * Short.BYTES;
    private static final int PARALLAX_START = PARALLAX_OFFSET_SHORTS * Short.BYTES;
    private static final int Y_START = Y_OFFSET_SHORTS * Short.BYTES;
    private static final int BACKGROUND_X_START = BACKGROUND_X_OFFSET_SHORTS * Short.BYTES;
    private static final int BACKGROUND_PARALLAX_START = BACKGROUND_PARALLAX_OFFSET_SHORTS * Short.BYTES;
    private static final int BACKGROUND_Y_START = BACKGROUND_Y_OFFSET_SHORTS * Short.BYTES;
    private static final int WIDTH_START = WIDTH_OFFSET_SHORTS * Short.BYTES;
    private static final int HEIGHT_START = HEIGHT_OFFSET_SHORTS * Short.BYTES;
    private static final int PARAMETER_INDEX_START = PARAMETER_INDEX_OFFSET_SHORTS * Short.BYTES;
    private static final int OOB_CHARACTER_START = OOB_CHARACTER_OFFSET_SHORTS * Short.BYTES;
    private static final int SCRATCH_START = SCRATCH_OFFSET_SHORTS * Short.BYTES;

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
    private final Logger logger;

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

    public WindowAttributes(int id, Logger logger) {
        this.id = id;
        this.logger = logger;
    }

    @Override
    public int getStart() {
        return WINDOW_ATTRIBUTES_START + SIZE * id;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    public final static boolean IGNORE_SCRATCH_WRITES = true;

    @Override
    public void setHalfWord(int address, short value) {
        if (address >= SCRATCH_START) {
            if (IGNORE_SCRATCH_WRITES) {
                return;
            }
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

    private int getFlags() {
        return (baseSegmentIndex << FLAGS_BASE_SEGMENT_POS)
                | intBit(FLAGS_STOP_POS, isStop)
                | intBit(FLAGS_USE_OOB_CHARACTER_POS, useOutOfBoundsCharacter)
                | (Integer.numberOfTrailingZeros(backgroundHeightSegments) << FLAGS_BACKGROUND_HEIGHT_POS)
                | (Integer.numberOfTrailingZeros(backgroundWidthSegments) << FLAGS_BACKGROUND_WIDTH_POS)
                | mode.getId() << FLAGS_MODE_POS
                | intBit(FLAGS_RIGHT_POS, drawRight)
                | intBit(FLAGS_LEFT_POS, drawLeft)
                ;
    }

    @Override
    public int getHalfWord(int address) {
        if (address >= SCRATCH_START) {
            logger.debug(Logger.Component.VIP, "reading from Window scratch @ %#08x", address);
            return 0xdead;
        }
        switch (address) {
            case FLAGS_START:
                return getFlags();
            case X_START:
                return x;
            case Y_START:
                return y;
            case BACKGROUND_X_START:
                return backgroundX;
            case BACKGROUND_Y_START:
                return backgroundY;
            case PARALLAX_START:
                return parallax;
            case BACKGROUND_PARALLAX_START:
                return backgroundParallax;
            case WIDTH_START:
                return width;
            case HEIGHT_START:
                return height;
            case PARAMETER_INDEX_START:
                return parameterIndex;
            case OOB_CHARACTER_START:
                return outOfBoundsCharacter;
        }
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
        baseSegmentIndex = 5;
        isStop = true;
        useOutOfBoundsCharacter = false;
        backgroundHeightSegments = 1;
        backgroundWidthSegments = 2;
        mode = WindowMode.get(0);
        drawLeft = false;
        drawRight = true;
        x = (short) 0xdead;
        y = (short) 0xbeef;
        backgroundX = (short) 0xbeef;
        backgroundY = (short) 0xdead;
        height = (short) 0xdead;
        width = (short) 0xbeef;
        parameterIndex = (short) 0xdead;
        outOfBoundsCharacter = (short) 0xdead;
        parallax = (short) 0xdead;
        backgroundParallax = (short) 0xbeef;
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

    public int getHeight() {
        return height;
    }

    public int getActualHeight() {
        return getHeight() + 1;
    }

    public int getWidth() {
        return width;
    }

    public int getActualWidth() {
        return getWidth() + 1;
    }

    public int getParameterIndex() {
        return parameterIndex & 0xffff;
    }

    public int getOutOfBoundsCharacter() {
        return outOfBoundsCharacter & 0xffff;
    }

    public short getParallax() {
        return parallax;
    }

    public short getBackgroundParallax() {
        return backgroundParallax;
    }

    public int getId() {
        return id;
    }
}
