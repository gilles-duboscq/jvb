package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.Resetable;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.intBits;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public class VIPControlRegisters implements ReadWriteMemory, Resetable {
    public static final int START = 0x0005F800;
    public static final int SIZE = 0x74;
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

    private final VirtualImageProcessor vip;

    private byte ledBrightness1;
    private byte ledBrightness2;
    private byte ledBrightness3;
    private byte ledBrightnessIdle;

    private byte backgroundPalette0;
    private byte backgroundPalette1;
    private byte backgroundPalette2;
    private byte backgroundPalette3;
    private byte objectPalette0;
    private byte objectPalette1;
    private byte objectPalette2;
    private byte objectPalette3;
    private byte clearColor;

    private short objectGroupIndex0;
    private short objectGroupIndex1;
    private short objectGroupIndex2;
    private short objectGroupIndex3;

    private short displayStatus;
    private short drawingStatus;
    private short enabledInterrupts;
    private short pendingInterrupts;
    private byte frameRepeat;

    public VIPControlRegisters(VirtualImageProcessor vip) {
        this.vip = vip;
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
                return backgroundPalette0 & 0xff;
            case BACKGROUND_PALETTE_1_START:
                return backgroundPalette1 & 0xff;
            case BACKGROUND_PALETTE_2_START:
                return backgroundPalette2 & 0xff;
            case BACKGROUND_PALETTE_3_START:
                return backgroundPalette3 & 0xff;
            case OBJECT_PALETTE_0_START:
                return objectPalette0 & 0xff;
            case OBJECT_PALETTE_1_START:
                return objectPalette1 & 0xff;
            case OBJECT_PALETTE_2_START:
                return objectPalette2 & 0xff;
            case OBJECT_PALETTE_3_START:
                return objectPalette3 & 0xff;
            case CLEAR_COLOR_START:
                return clearColor & 0xff;
            case OBJECT_GROUP_INDEX_0_START:
                return objectGroupIndex0 & 0xffff;
            case OBJECT_GROUP_INDEX_1_START:
                return objectGroupIndex1 & 0xffff;
            case OBJECT_GROUP_INDEX_2_START:
                return objectGroupIndex2 & 0xffff;
            case OBJECT_GROUP_INDEX_3_START:
                return objectGroupIndex3 & 0xffff;
        }
        throw new BusError(address, Unimplemented);
    }

    @Override
    public void setHalfWord(int address, short value) {
        switch (address) {
            case DISPLAY_CONTROL_START:
                processDisplayControl(value);
                return;
            case INTERRUPT_ENABLE_START:
                enabledInterrupts = value;
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
                backgroundPalette0 = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_1_START:
                backgroundPalette1 = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_2_START:
                backgroundPalette2 = (byte) (value & PALETTE_MASK);
                return;
            case BACKGROUND_PALETTE_3_START:
                backgroundPalette3 = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_0_START:
                objectPalette0 = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_1_START:
                objectPalette1 = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_2_START:
                objectPalette2 = (byte) (value & PALETTE_MASK);
                return;
            case OBJECT_PALETTE_3_START:
                objectPalette3 = (byte) (value & PALETTE_MASK);
                return;
            case CLEAR_COLOR_START:
                clearColor = (byte) (value & CLEAR_COLOR_MASK);
                return;
            case OBJECT_GROUP_INDEX_0_START:
                objectGroupIndex0 = (byte) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_1_START:
                objectGroupIndex1 = (byte) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_2_START:
                objectGroupIndex2 = (byte) (value & mask(OBJECT_GROUP_LEN));
                return;
            case OBJECT_GROUP_INDEX_3_START:
                objectGroupIndex3 = (byte) (value & mask(OBJECT_GROUP_LEN));
                return;
        }
        throw new BusError(address, Unimplemented);
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
            System.out.println("Setting refresh forever...");
        }

        int affected = intBits(DISPLAY_STATUS_DISPLAY_ENABLED_POS, DISPLAY_STATUS_MEMORY_REFRESHING_POS, DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS);
        int set = intBit(DISPLAY_STATUS_DISPLAY_ENABLED_POS, enable)
                | intBit(DISPLAY_STATUS_MEMORY_REFRESHING_POS, refresh)
                | intBit(DISPLAY_STATUS_COLUMN_TABLE_ADDR_LOCKED_POS, lockColumnTable)
                | intBit(DISPLAY_STATUS_DISPLAY_SYNC_ENABLED_POS, displaySync);
        displayStatus = (short) ((displayStatus | set) & (~affected | set));
    }

    private void processDrawingControl(short value) {
        boolean enable = testBit(value, DRAWING_CONTROL_ENABLE_DRAWING_POS);
        boolean reset = testBit(value, DRAWING_CONTROL_RESET_DRAWING_POS);
        int yPos = extractU(value, DRAWING_CONTROL_Y_POSITION_POS, DRAWING_CONTROL_Y_POSITION_LEN);

        if (reset) {
            pendingInterrupts &= (short) (~intBits(
                    INT_DRAWING_EXCEEDS_FRAME_PERIOD_POS,
                    INT_DRAWING_FINISHED_POS,
                    INT_DISPLAY_NOT_READY_POS));
            vip.softReset();
            setDrawingFrameBufferPair(0, true);
        }

        int affected = intBit(DRAWING_STATUS_DRAWING_ENABLED_POS) | mask(DRAWING_CONTROL_Y_POSITION_POS, DRAWING_CONTROL_Y_POSITION_LEN);
        int set = intBit(DRAWING_STATUS_DRAWING_ENABLED_POS, enable) | (yPos << DRAWING_CONTROL_Y_POSITION_POS);
        drawingStatus = (short) ((drawingStatus | set) & (~affected | set));
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
        backgroundPalette0 = (byte) (0xde & PALETTE_MASK);
        backgroundPalette1 = (byte) (0xad & PALETTE_MASK);
        backgroundPalette2 = (byte) (0xbe & PALETTE_MASK);
        backgroundPalette3 = (byte) (0xef & PALETTE_MASK);
        objectPalette0 = (byte) (0xde & PALETTE_MASK);
        objectPalette1 = (byte) (0xad & PALETTE_MASK);
        objectPalette2 = (byte) (0xbe & PALETTE_MASK);
        objectPalette3 = (byte) (0xef & PALETTE_MASK);
        clearColor = (byte) (0xde & CLEAR_COLOR_MASK);
        objectGroupIndex0 = (short) (0xdead & mask(OBJECT_GROUP_LEN));
        objectGroupIndex1 = (short) (0xbeef & mask(OBJECT_GROUP_LEN));
        objectGroupIndex2 = (short) (0xdead & mask(OBJECT_GROUP_LEN));
        objectGroupIndex3 = (short) (0xbeef & mask(OBJECT_GROUP_LEN));

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

    public byte getLedBrightnessIdle() {
        return ledBrightnessIdle;
    }

    public byte getFrameRepeat() {
        return frameRepeat;
    }

    public static final boolean DEBUG_VIP_CTRL_AS_BYTE = false;

    @Override
    public int getByte(int address) {
        if (DEBUG_VIP_CTRL_AS_BYTE) {
            System.out.printf("Warning: accessing VIP Control register as byte @ 0x%08x%n", address);
        }
        return 0xde;
    }

    @Override
    public void setByte(int address, byte value) {
        if (DEBUG_VIP_CTRL_AS_BYTE) {
            System.out.printf("Warning: accessing VIP Control register as byte @ 0x%08x%n", address);
        }
    }

    public boolean isDisplayEnabled() {
        return testBit(displayStatus, DISPLAY_STATUS_DISPLAY_ENABLED_POS);
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
        drawingStatus = (short) ((drawingStatus | set) & (~affected | set));
    }

    public void setDisplayingFrameBufferPair(int pair, boolean left, boolean disaplaying) {
        int affected = intBits(DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS,
                DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS);
        int set = intBit(DISPLAY_STATUS_LEFT_FB_0_DISPLAYED_POS, pair == 0 && left && disaplaying)
                | intBit(DISPLAY_STATUS_LEFT_FB_1_DISPLAYED_POS, pair == 1 && left && disaplaying)
                | intBit(DISPLAY_STATUS_RIGHT_FB_0_DISPLAYED_POS, pair == 0 && !left && disaplaying)
                | intBit(DISPLAY_STATUS_RIGHT_FB_1_DISPLAYED_POS, pair == 1 && !left && disaplaying);
        displayStatus = (short) ((displayStatus | set) & (~affected | set));
    }
}
