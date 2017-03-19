package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.maskedMerge;
import static gd.twohundred.jvb.Utils.testBit;

public class VIPControlRegisters implements ReadWriteMemory, Resetable {
    public static final int START = 0x0005F800;
    public static final int SIZE = 0x80;
    public static final int VERSION = 2;

    private static final int INTERRUPT_PENDING_START = 0x00;
    private static final int INTERRUPT_ENABLE_START = 0x02;
    private static final int INTERRUPT_CLEAR_START = 0x04;
    private static final int DISPLAY_STATUS_START = 0x20;
    private static final int DISPLAY_CONTROL_START = 0x22;
    private static final int LED_BRIGHTNESS_1_START = 0x24;
    private static final int LED_BRIGHTNESS_2_START = 0x26;
    private static final int LED_BRIGHTNESS_3_START = 0x28;
    private static final int LED_BRIGHTNESS_IDLE_START = 0x2a;
    private static final int FRAME_REPEAT_START = 0x2e;
    private static final int COLUMN_TABLE_ADDRESS_START = 0x30;
    private static final int DRAWING_STATUS_START = 0x40;
    private static final int DRAWING_CONTROL_START = 0x42;
    private static final int VERSION_START = 0x44;
    private static final int OBJECT_GROUP_INDEX_0_START = 0x48;
    private static final int OBJECT_GROUP_INDEX_1_START = 0x4a;
    private static final int OBJECT_GROUP_INDEX_2_START = 0x4c;
    private static final int OBJECT_GROUP_INDEX_3_START = 0x4e;
    private static final int BACKGROUND_PALETTE_0_START = 0x60;
    private static final int BACKGROUND_PALETTE_1_START = 0x62;
    private static final int BACKGROUND_PALETTE_2_START = 0x64;
    private static final int BACKGROUND_PALETTE_3_START = 0x66;
    private static final int OBJECT_PALETTE_0_START = 0x68;
    private static final int OBJECT_PALETTE_1_START = 0x6a;
    private static final int OBJECT_PALETTE_2_START = 0x6c;
    private static final int OBJECT_PALETTE_3_START = 0x6e;
    private static final int CLEAR_COLOR_START = 0x70;

    private static final int DISPLAY_STATUS_DISPLAY_ENABLED_POS = 1;
    private static final int DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS = 2;
    private static final int DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS = 3;
    private static final int DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS = 4;
    private static final int DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS = 5;
    private static final int DISPLAY_STATUS_DISPLAY_READY_POS = 6;
    private static final int DISPLAY_STATUS_DISPLAY_PROC_BEGINING_POS = 7;
    private static final int DISPLAY_STATUS_MEMORY_REFRESHING_POS = 8;
    private static final int DISPLAY_STATUS_DISPLAY_SYNC_ENABLED_POS = 9;
    private static final int DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS = 10;

    private static final int DISPLAY_CONTROL_RESET_DISPLAY_POS = 0;
    private static final int DISPLAY_CONTROL_ENABLE_DISPLAY_POS = 1;
    private static final int DISPLAY_CONTROL_MEMORY_REFRESH_POS = 8;
    private static final int DISPLAY_CONTROL_ENABLE_DISPLAY_SYNC_POS = 9;
    private static final int DISPLAY_CONTROL_LOCK_COLUMN_TABLE_ADDR_POS = 10;

    private static final int DRAWING_STATUS_DRAWING_AT_Y_POSITION_POS = 15;
    private static final int DRAWING_STATUS_CURRENT_Y_POSITION_POS = 8;
    private static final int DRAWING_STATUS_CURRENT_Y_POSITION_LEN = 5;
    private static final int DRAWING_STATUS_DRAWING_EXCEEDS_FRAME_PERIOD_POS = 4;
    private static final int DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_0_POS = 3;
    private static final int DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_1_POS = 2;
    private static final int DRAWING_STATUS_DRAWING_ENABLED_POS = 1;

    private static final int DRAWING_CONTROL_Y_POSITION_POS = 8;
    private static final int DRAWING_CONTROL_Y_POSITION_LEN = 5;
    private static final int DRAWING_CONTROL_ENABLE_DRAWING_POS = 1;
    private static final int DRAWING_CONTROL_RESET_DRAWING_POS = 0;

    private static final int INT_DRAWING_EXCEEDS_FRAME_PERIOD_POS = 15;
    private static final int INT_DRAWING_FINISHED_POS = 14;
    private static final int INT_DRAWING_Y_POSITION_MATCH_POS = 13;
    private static final int INT_START_OF_FRAME_PROCESSING_POS = 4;
    private static final int INT_START_OF_DRAWING_POS = 3;
    private static final int INT_RIGHT_DISPLAY_FINISHED_POS = 2;
    private static final int INT_LEFT_DISPLAY_FINISHED_POS = 1;
    private static final int INT_DISPLAY_NOT_READY_POS = 0;

    private static final int PALETTE_MASK = 0b11_11_11_00;
    private static final int CLEAR_COLOR_MASK = 0b11;
    private static final int OBJECT_GROUP_LEN = 10;

    public enum VIPInterruptType {
        DrawingExceedsFramePeriod(INT_DRAWING_EXCEEDS_FRAME_PERIOD_POS),
        DrawingFinished(INT_DRAWING_FINISHED_POS),
        DrawingYPositionMatch(INT_DRAWING_Y_POSITION_MATCH_POS),
        StartFrameProcessing(INT_START_OF_FRAME_PROCESSING_POS),
        StartDrawing(INT_START_OF_DRAWING_POS),
        RightDisplayFinished(INT_RIGHT_DISPLAY_FINISHED_POS),
        LeftDisplayFinished(INT_LEFT_DISPLAY_FINISHED_POS);
        private final int bit;

        VIPInterruptType(int bit) {
            this.bit = bit;
        }

        private int getBit() {
            return bit;
        }
    }

    private final VirtualImageProcessor vip;
    private final Logger logger;

    private byte ledBrightness1;
    private byte ledBrightness2;
    private byte ledBrightness3;
    private byte ledBrightnessIdle;

    private final byte[] backgroundPalette = new byte[4];
    private final byte[] objectPalette = new byte[4];
    private byte clearColor;

    private final short[] objectGroupIndexes = new short[4];

    private short displayStatus;
    private short drawingStatus;
    private byte interruptYPosition;
    private short enabledInterrupts;
    private short pendingInterrupts;
    private byte frameRepeat;

    public VIPControlRegisters(VirtualImageProcessor vip, Logger logger) {
        this.vip = vip;
        this.logger = logger;
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public int getHalfWord(int address) {
        switch (address) {
            case DISPLAY_STATUS_START:
                return displayStatus & 0xffff;
            case DRAWING_STATUS_START:
                return drawingStatus & 0xffff;
            case INTERRUPT_ENABLE_START:
                return enabledInterrupts & 0xffff;
            case INTERRUPT_PENDING_START:
                return pendingInterrupts & 0xffff;
            case VERSION_START:
                return VERSION;
            case BACKGROUND_PALETTE_0_START:
                return backgroundPalette[0] & 0xff;
            case BACKGROUND_PALETTE_1_START:
                return backgroundPalette[1] & 0xff;
            case BACKGROUND_PALETTE_2_START:
                return backgroundPalette[2] & 0xff;
            case BACKGROUND_PALETTE_3_START:
                return backgroundPalette[3] & 0xff;
            case OBJECT_PALETTE_0_START:
                return objectPalette[0] & 0xff;
            case OBJECT_PALETTE_1_START:
                return objectPalette[1] & 0xff;
            case OBJECT_PALETTE_2_START:
                return objectPalette[2] & 0xff;
            case OBJECT_PALETTE_3_START:
                return objectPalette[3] & 0xff;
            case CLEAR_COLOR_START:
                return clearColor & 0xff;
            case OBJECT_GROUP_INDEX_0_START:
                return objectGroupIndexes[0] & 0xffff;
            case OBJECT_GROUP_INDEX_1_START:
                return objectGroupIndexes[1] & 0xffff;
            case OBJECT_GROUP_INDEX_2_START:
                return objectGroupIndexes[2] & 0xffff;
            case OBJECT_GROUP_INDEX_3_START:
                return objectGroupIndexes[3] & 0xffff;
            case LED_BRIGHTNESS_1_START:
                return ledBrightness1 & 0xff;
            case LED_BRIGHTNESS_2_START:
                return ledBrightness2 & 0xff;
            case LED_BRIGHTNESS_3_START:
                return ledBrightness3 & 0xff;
            case LED_BRIGHTNESS_IDLE_START:
                return ledBrightnessIdle & 0xff;
            case FRAME_REPEAT_START:
                return frameRepeat & 0xf;
            case COLUMN_TABLE_ADDRESS_START:
                logger.warning(Logger.Component.VIP, "returning dummy value for Column Table Address");
                return 0x00_00;
            case INTERRUPT_CLEAR_START:
            case DISPLAY_CONTROL_START:
            case DRAWING_CONTROL_START:
                return 0xdead; // write=only
            case 0x06:
            case 0x08:
            case 0x0a:
            case 0x0c:
            case 0x0e:
            case 0x10:
            case 0x12:
            case 0x14:
            case 0x16:
            case 0x18:
            case 0x1a:
            case 0x1c:
            case 0x1e:
            case 0x2c:
            case 0x32:
            case 0x34:
            case 0x36:
            case 0x38:
            case 0x3a:
            case 0x3c:
            case 0x3e:
            case 0x46:
            case 0x50:
            case 0x52:
            case 0x54:
            case 0x56:
            case 0x58:
            case 0x5a:
            case 0x5c:
            case 0x5e:
            case 0x72:
            case 0x74:
            case 0x76:
            case 0x78:
            case 0x7a:
            case 0x7c:
            case 0x7e:
                return 0xbeef; // not mapped to anything
        }
        // ignore other read? mirroring?
        logger.warning(Logger.Component.VIP, "reading out of range into VIP control regs @ %#02x", address);
        return 0xcafe;
    }

    @Override
    public void setHalfWord(int address, short value) {
        switch (address) {
            case DISPLAY_CONTROL_START:
                processDisplayControl(value);
                return;
            case INTERRUPT_ENABLE_START:
                enabledInterrupts = value;
                if (logger.isLevelEnabled(Logger.Component.VIP, Logger.Level.Debug)) {
                    String msg = "";
                    for (VIPInterruptType t : VIPInterruptType.values()) {
                        if (isInterruptEnabled(t)) {
                            msg += " " + t;
                        }
                    }
                    logger.debug(Logger.Component.VIP, "Enabling VIP interrupts: %s", address);
                }
                return;
            case INTERRUPT_CLEAR_START:
                pendingInterrupts &= ~value;
                return;
            case DRAWING_CONTROL_START:
                processDrawingControl(value);
                return;
            case FRAME_REPEAT_START:
                frameRepeat = (byte) (value & 0xf);
                return;
            case LED_BRIGHTNESS_1_START:
                ledBrightness1 = (byte) value;
                return;
            case LED_BRIGHTNESS_2_START:
                ledBrightness2 = (byte) value;
                return;
            case LED_BRIGHTNESS_3_START:
                ledBrightness3 = (byte) value;
                return;
            case LED_BRIGHTNESS_IDLE_START:
                ledBrightnessIdle = (byte) value;
                return;
            case BACKGROUND_PALETTE_0_START:
                backgroundPalette[0] = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_1_START:
                backgroundPalette[1] = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_2_START:
                backgroundPalette[2] = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_3_START:
                backgroundPalette[3] = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_0_START:
                objectPalette[0] = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_1_START:
                objectPalette[1] = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_2_START:
                objectPalette[2] = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_3_START:
                objectPalette[3] = (byte) (value & PALETTE_MASK);
                return;
            case CLEAR_COLOR_START:
                clearColor = (byte) (value & CLEAR_COLOR_MASK);
                return;
            case OBJECT_GROUP_INDEX_0_START:
                objectGroupIndexes[0] = (short) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_1_START:
                objectGroupIndexes[1] = (short) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_2_START:
                objectGroupIndexes[2] = (short) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_3_START:
                objectGroupIndexes[3] = (short) (value & mask(OBJECT_GROUP_LEN));
                return;
            case 0x06:
            case 0x08:
            case 0x0a:
            case 0x0c:
            case 0x0e:
            case 0x10:
            case 0x12:
            case 0x14:
            case 0x16:
            case 0x18:
            case 0x1a:
            case 0x1c:
            case 0x1e:
            case 0x2c:
            case 0x32:
            case 0x34:
            case 0x36:
            case 0x38:
            case 0x3a:
            case 0x3c:
            case 0x3e:
            case 0x46:
            case 0x50:
            case 0x52:
            case 0x54:
            case 0x56:
            case 0x58:
            case 0x5a:
            case 0x5c:
            case 0x5e:
            case 0x72:
            case 0x74:
            case 0x76:
            case 0x78:
            case 0x7a:
            case 0x7c:
            case 0x7e:
                return; // not mapped to anything
            case VERSION_START:
            case COLUMN_TABLE_ADDRESS_START:
            case INTERRUPT_PENDING_START:
            case DISPLAY_STATUS_START:
            case DRAWING_STATUS_START:
                return; // read-only
        }
        // ignore other writes? mirroring?
        logger.warning(Logger.Component.VIP, "writing out of range into VIP control regs @ %#02x", address);
    }

    private void processDisplayControl(short value) {
        boolean enable = testBit(value, DISPLAY_CONTROL_ENABLE_DISPLAY_POS);
        boolean refresh = testBit(value, DISPLAY_CONTROL_MEMORY_REFRESH_POS);
        boolean lockColumnTable = testBit(value, DISPLAY_CONTROL_LOCK_COLUMN_TABLE_ADDR_POS);
        boolean displaySync = testBit(value, DISPLAY_CONTROL_ENABLE_DISPLAY_SYNC_POS);
        boolean reset = testBit(value, DISPLAY_CONTROL_RESET_DISPLAY_POS);

        if (reset) {
            pendingInterrupts &= (short) (~intBits(
                    INT_DRAWING_EXCEEDS_FRAME_PERIOD_POS,
                    INT_START_OF_FRAME_PROCESSING_POS,
                    INT_START_OF_DRAWING_POS,
                    INT_RIGHT_DISPLAY_FINISHED_POS,
                    INT_LEFT_DISPLAY_FINISHED_POS,
                    INT_DISPLAY_NOT_READY_POS));
            vip.softReset();
        }
        if (refresh) {
            logger.info(Logger.Component.VIP, "Ignoring VIP refresh");
        }

        int affected = intBits(DISPLAY_STATUS_DISPLAY_ENABLED_POS, DISPLAY_STATUS_MEMORY_REFRESHING_POS, DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS);
        int set = intBit(DISPLAY_STATUS_DISPLAY_ENABLED_POS, enable)
                | intBit(DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS, lockColumnTable)
                | intBit(DISPLAY_STATUS_DISPLAY_SYNC_ENABLED_POS, displaySync);
        displayStatus = (short) maskedMerge(set, affected, displayStatus);
    }

    private void processDrawingControl(short value) {
        boolean enable = testBit(value, DRAWING_CONTROL_ENABLE_DRAWING_POS);
        boolean reset = testBit(value, DRAWING_CONTROL_RESET_DRAWING_POS);
        interruptYPosition = (byte) extractU(value, DRAWING_CONTROL_Y_POSITION_POS, DRAWING_CONTROL_Y_POSITION_LEN);
        if (reset) {
            pendingInterrupts &= (short) (~intBits(
                    INT_DRAWING_EXCEEDS_FRAME_PERIOD_POS,
                    INT_DRAWING_FINISHED_POS,
                    INT_DISPLAY_NOT_READY_POS));
            vip.softReset();
            setDrawingFrameBufferPair(0, true);
        }

        int affected = intBit(DRAWING_STATUS_DRAWING_ENABLED_POS);
        int set = intBit(DRAWING_STATUS_DRAWING_ENABLED_POS, enable);
        drawingStatus = (short) maskedMerge(set, affected, drawingStatus);
    }

    @Override
    public void reset() {
        enabledInterrupts = 0;
        int usedDisplayStatus = intBits(
                DISPLAY_STATUS_DISPLAY_ENABLED_POS,
                DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS,
                DISPLAY_STATUS_DISPLAY_READY_POS,
                DISPLAY_STATUS_DISPLAY_PROC_BEGINING_POS,
                DISPLAY_STATUS_MEMORY_REFRESHING_POS,
                DISPLAY_STATUS_DISPLAY_SYNC_ENABLED_POS,
                DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS);
        displayStatus = (short) (0xdead & ~intBits(DISPLAY_STATUS_DISPLAY_SYNC_ENABLED_POS,
                DISPLAY_STATUS_MEMORY_REFRESHING_POS,
                DISPLAY_STATUS_DISPLAY_PROC_BEGINING_POS) & usedDisplayStatus);

        int usedDrawingStatus = intBits(DRAWING_STATUS_DRAWING_ENABLED_POS,
                DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_1_POS,
                DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_0_POS,
                DRAWING_STATUS_DRAWING_EXCEEDS_FRAME_PERIOD_POS,
                DRAWING_STATUS_DRAWING_AT_Y_POSITION_POS) | mask(DRAWING_STATUS_CURRENT_Y_POSITION_POS, DRAWING_STATUS_CURRENT_Y_POSITION_LEN);
        drawingStatus = (short) (0xdead & ~intBits(DRAWING_STATUS_DRAWING_ENABLED_POS) & usedDrawingStatus);

        ledBrightness1 = (byte) 0xde;
        ledBrightness2 = (byte) 0xad;
        ledBrightness3 = (byte) 0xbe;
        ledBrightnessIdle = (byte) 0xef;
        pendingInterrupts = (short) 0xdead;
        frameRepeat = 0xde & 0xf;
        backgroundPalette[0] = (byte) (0xde & PALETTE_MASK);
        backgroundPalette[1] = (byte) (0xad & PALETTE_MASK);
        backgroundPalette[2] = (byte) (0xbe & PALETTE_MASK);
        backgroundPalette[3] = (byte) (0xef & PALETTE_MASK);
        objectPalette[0] = (byte) (0xde & PALETTE_MASK);
        objectPalette[1] = (byte) (0xad & PALETTE_MASK);
        objectPalette[2] = (byte) (0xbe & PALETTE_MASK);
        objectPalette[3] = (byte) (0xef & PALETTE_MASK);
        clearColor = (byte) (0xde & CLEAR_COLOR_MASK);
        objectGroupIndexes[0] = (short) (0xdead & mask(OBJECT_GROUP_LEN));
        objectGroupIndexes[1] = (short) (0xbeef & mask(OBJECT_GROUP_LEN));
        objectGroupIndexes[2] = (short) (0xdead & mask(OBJECT_GROUP_LEN));
        objectGroupIndexes[3] = (short) (0xbeef & mask(OBJECT_GROUP_LEN));
        interruptYPosition = 0; // ??

        // set display ready
        displayStatus |= intBit(DISPLAY_STATUS_DISPLAY_READY_POS);
    }

    public int getLedBrightness1() {
        return ledBrightness1 & 0xff;
    }

    public int getLedBrightness2() {
        return ledBrightness2 & 0xff;
    }

    public int getLedBrightness3() {
        return ledBrightness3 & 0xff;
    }

    public int getLedBrightnessIdle() {
        return ledBrightnessIdle & 0xff;
    }

    public byte[] getObjectPalettes() {
        return objectPalette;
    }

    public byte[] getBackgroundPalettes() {
        return backgroundPalette;
    }

    public short[] getObjectGroupIndexes() {
        return objectGroupIndexes;
    }

    public int getClearColor() {
        return clearColor & 0xff;
    }

    public int getFrameRepeat() {
        return frameRepeat;
    }

    public int getEnabledInterrupts() {
        return enabledInterrupts & 0xffff;
    }

    public int getInterruptYPosition() {
        return interruptYPosition & 0xff;
    }

    @Override
    public int getByte(int address) {
        logger.warning(Logger.Component.VIP, "reading VIP Control register as byte @ %#08x", address);
        return 0xde;
    }

    @Override
    public void setByte(int address, byte value) {
        logger.warning(Logger.Component.VIP, "writing VIP Control register as byte @ %#08x", address);
    }

    @Override
    public int getWord(int address) {
        logger.warning(Logger.Component.VIP, "reading VIP Control register as word @ %#08x", address);
        return getHalfWord(address);
    }

    @Override
    public void setWord(int address, int value) {
        logger.warning(Logger.Component.VIP, "writing VIP Control register as word @ %#08x", address);
        setHalfWord(address, (short) value);
    }

    public boolean isDisplayEnabled() {
        return testBit(displayStatus, DISPLAY_STATUS_DISPLAY_ENABLED_POS);
    }

    public boolean isDrawingEnabled() {
        return testBit(drawingStatus, DRAWING_STATUS_DRAWING_ENABLED_POS);
    }

    public void setDrawingExceedsFramePeriod() {
        drawingStatus |= intBit(DRAWING_STATUS_DRAWING_EXCEEDS_FRAME_PERIOD_POS);
    }

    public void setDisplayProcStart() {
        displayStatus |= intBit(DISPLAY_STATUS_DISPLAY_PROC_BEGINING_POS);
    }

    public void setDrawingFrameBufferPair(int pair, boolean drawing) {
        int affected = intBits(DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_0_POS, DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_1_POS);
        int set = intBit(DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_0_POS, drawing && pair == 0)
                | intBit(DRAWING_STATUS_WRITING_TO_FRAME_BUFFER_1_POS, drawing && pair == 1) ;
        drawingStatus = (short) maskedMerge(set, affected, drawingStatus);
    }

    public void setDisplayingFrameBufferPair(int pair, boolean left, boolean displaying) {
        int affected = intBits(DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS);
        int set = intBit(DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS, pair == 0 && left && displaying)
                | intBit(DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS, pair == 1 && left && displaying)
                | intBit(DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS, pair == 0 && !left && displaying)
                | intBit(DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS, pair == 1 && !left && displaying);
        displayStatus = (short) maskedMerge(set, affected, displayStatus);
    }

    public void setCurrentYBlock(int i) {
        drawingStatus = (short) insert(i, DRAWING_STATUS_CURRENT_Y_POSITION_POS, DRAWING_STATUS_CURRENT_Y_POSITION_LEN, drawingStatus);
    }

    public int getCurrentYBlock() {
        return extractU(drawingStatus, DRAWING_STATUS_CURRENT_Y_POSITION_POS, DRAWING_STATUS_CURRENT_Y_POSITION_LEN);
    }

    public boolean isInterruptEnabled(VIPInterruptType type) {
        return testBit(enabledInterrupts, type.getBit());
    }

    public void addPendingInterrupt(VIPInterruptType type) {
        pendingInterrupts |= intBit(type.getBit());
    }
}
