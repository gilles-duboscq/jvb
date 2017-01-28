package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.RenderedFrame;
import gd.twohundred.jvb.Screen;
import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.LinearMemoryMirroring;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.utils.WarningMemory;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Screen.HEIGHT;
import static gd.twohundred.jvb.Screen.WIDTH;
import static gd.twohundred.jvb.Utils.NANOS_PER_SECOND;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.LeftFrameBuffer;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.RightFrameBuffer;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.Waiting;
import static java.lang.Math.min;

public class VirtualImageProcessor extends MappedModules implements ExactlyEmulable {
    public static final int MAPPED_SIZE = 0x0100_0000;
    public static final int SIZE = 0x0008_0000;
    public static final int RIGHT_FRAMEBUFFER_1_START = 0x0018_0000;
    public static final int RIGHT_FRAMEBUFFER_0_START = 0x0001_0000;
    public static final int LEFT_FRAMEBUFFER_0_START = 0x0000_0000;
    public static final int LEFT_FRAMEBUFFER_1_START = 0x0000_8000;


    private final Screen screen;
    private final FrameBuffer leftFb0 = new FrameBuffer(LEFT_FRAMEBUFFER_0_START);
    private final FrameBuffer leftFb1 = new FrameBuffer(LEFT_FRAMEBUFFER_1_START);
    private final FrameBuffer rightFb0 = new FrameBuffer(RIGHT_FRAMEBUFFER_0_START);
    private final FrameBuffer rightFb1 = new FrameBuffer(RIGHT_FRAMEBUFFER_1_START);
    private final VIPControlRegisters controlRegs = new VIPControlRegisters(this);

    private final WarningMemory backgroundSegmentsAndWindowPataremeterTable =
            new WarningMemory("Background Segments and Window Parameter Table", 0x00020000, 0x1d800);
    private final WarningMemory windowAttr =
            new WarningMemory("Window Attributes", 0x0003D800, 0x400);
    private final WarningMemory columnTable =
            new WarningMemory("Column Table", 0x0003DC00, 0x400);
    private final WarningMemory oam =
            new WarningMemory("Object Attribute Memory", 0x0003E000, 0x2000);


    private final WarningMemory chrTable0 =
            new WarningMemory("CHR RAM Pattern Table 0", 0x00006000, 0x8000);
    private final WarningMemory chrTable1 =
            new WarningMemory("CHR RAM Pattern Table 1", 0x0000E000, 0x8000);
    private final WarningMemory chrTable2 =
            new WarningMemory("CHR RAM Pattern Table 2", 0x00016000, 0x8000);
    private final WarningMemory chrTable3 =
            new WarningMemory("CHR RAM Pattern Table 3", 0x0001E000, 0x8000);

    private final LinearMemoryMirroring chrTable0Mirror = new LinearMemoryMirroring(0x00078000, chrTable0);
    private final LinearMemoryMirroring chrTable1Mirror = new LinearMemoryMirroring(0x0007A000, chrTable1);
    private final LinearMemoryMirroring chrTable2Mirror = new LinearMemoryMirroring(0x0007C000, chrTable2);
    private final LinearMemoryMirroring chrTable3Mirror = new LinearMemoryMirroring(0x0007E000, chrTable3);


    public VirtualImageProcessor(Screen screen) {
        this.screen = screen;
    }

    private RenderedFrame renderFrameBuffer(FrameBuffer fb, RenderedFrame frame) {
        int fbAddr = 0;
        int imageAddr = 0;
        byte[] intensities = new byte[]{
                0,
                (byte) controlRegs.getLedBrightness1(),
                (byte) controlRegs.getLedBrightness2(),
                (byte) min(255, (controlRegs.getLedBrightness1() & 0xff) + (controlRegs.getLedBrightness2() & 0xff) + (controlRegs.getLedBrightness3() & 0xff))
        };
        for (int col = 0; col < WIDTH; col++) {
            for (int row = 0; row < HEIGHT / FrameBuffer.PIXEL_PER_BYTE; row++) {
                int b = fb.getByte(fbAddr++);
                for (int j = 0; j < FrameBuffer.PIXEL_PER_BYTE; j++, b >>= FrameBuffer.BITS_PER_PIXEL) {
                    frame.setPixel(imageAddr++, intensities[b & ((1 << FrameBuffer.BITS_PER_PIXEL) - 1)]);
                }
            }
            fbAddr += (FrameBuffer.HEIGHT - Screen.HEIGHT) / FrameBuffer.PIXEL_PER_BYTE;
        }
        return frame;
    }

    private static final int FRAME_PERIOD = (int) (CPU.CLOCK_HZ / Screen.DISPLAY_REFRESH_RATE_HZ);
    private static final int RIGH_DISPLAY_CYCLE = FRAME_PERIOD - 10;
    private static final int LEFT_DISPLAY_CYCLE = RIGH_DISPLAY_CYCLE - 10;

    private static final int MAX_COLUMN_TIME = 0xfe;
    private static final long FOUR_COLUMN_UNIT_TIME_NS = 200;
    private static final long COLUMN_UNIT_TIME_NS = FOUR_COLUMN_UNIT_TIME_NS / 4;
    private static final long MAX_FRAME_BUFFER_DISPLAY_TIME_NS = COLUMN_UNIT_TIME_NS * Screen.WIDTH;
    private static final int MAX_FRAME_BUFFER_DISPLAY_CYCLES = (int) (CPU.CLOCK_HZ * MAX_FRAME_BUFFER_DISPLAY_TIME_NS / NANOS_PER_SECOND);
    private static final int RIGH_DISPLAY_START_CYCLE = FRAME_PERIOD - (10 + MAX_FRAME_BUFFER_DISPLAY_CYCLES);
    private static final int LEFT_DISPLAY_START_CYCLE = RIGH_DISPLAY_START_CYCLE - (10 + MAX_FRAME_BUFFER_DISPLAY_CYCLES);

    private final RenderedFrame leftRendered = new RenderedFrame();
    private final RenderedFrame rightRendered = new RenderedFrame();
    private int displayCycles;
    private long frameCounter;
    private DrawingState drawingState;
    private DisplayState displayState;

    private FrameBuffer currentRight = rightFb1;
    private FrameBuffer currentLeft = leftFb1;

    protected enum DisplayState {
        Waiting,
        LeftFrameBuffer,
        RightFrameBuffer,
        Finished
    }

    protected enum DrawingState {
        Drawing,
        Finished
    }

    @Override
    public void tickExact(int cycles) {
        int cyclesToConsume = cycles;
        if (displayState == DisplayState.Finished) {
            int idleCycles = min(cyclesToConsume, FRAME_PERIOD - displayCycles);
            cyclesToConsume -= idleCycles;
            displayCycles += idleCycles;
        }
        if (!controlRegs.isDisplayEnabled()) {
            return;
        }
        while (cyclesToConsume > 0) {
            if (displayCycles >= FRAME_PERIOD) {
                displayCycles = 0;
                startDisplay();
            } else if (displayCycles == LEFT_DISPLAY_START_CYCLE) {
                displayState = LeftFrameBuffer;
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), true, true);
                renderFrameBuffer(currentLeft, leftRendered);
            } else if (displayCycles == LEFT_DISPLAY_START_CYCLE + MAX_FRAME_BUFFER_DISPLAY_CYCLES) {
                screen.update(leftRendered, rightRendered);
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), true, false);
                // TODO: interrupt Left Display Finished
            } else if (displayCycles == RIGH_DISPLAY_START_CYCLE) {
                displayState = RightFrameBuffer;
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), false, true);
                renderFrameBuffer(currentLeft, leftRendered);
            } else if (displayCycles == RIGH_DISPLAY_START_CYCLE + MAX_FRAME_BUFFER_DISPLAY_CYCLES) {
                screen.update(leftRendered, rightRendered);
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), false, false);
                // TODO: interrupt Right Display Finished
                displayState = DisplayState.Finished;
            }
            if (drawingState == DrawingState.Drawing) {
                tickDrawing();
            }
            cyclesToConsume--;
            displayCycles++;
        }
    }

    private int currentFbPair() {
        return rightFb0 == currentRight ? 0 : 1;
    }

    private void tickDrawing() {
        if (displayCycles >= 100) {
            controlRegs.setDrawingFrameBufferPair(0, false);
            drawingState = DrawingState.Finished;
        }
    }

    private void startDisplay() {
        if (drawingState != DrawingState.Finished) {
            controlRegs.setDrawingExceedsFramePeriod();
            // TODO: interrupt?
        }
        controlRegs.setDisplayProcStart();
        displayState = Waiting;
        controlRegs.setDisplayingFrameBufferPair(0, true, false);
        // TODO: interrupt: Start of Frame Processing and/or Start of Drawing
        if (frameCounter % (controlRegs.getFrameRepeat() + 1) == 0) {
            startDrawing();
        }
        frameCounter++;
    }

    private void startDrawing() {
        drawingState = DrawingState.Drawing;
        // swap buffers
        if (currentRight == rightFb0) {
            currentRight = rightFb1;
            currentLeft = leftFb1;
            controlRegs.setDrawingFrameBufferPair(1, true);
        } else {
            currentRight = rightFb0;
            currentLeft = leftFb0;
            controlRegs.setDrawingFrameBufferPair(0, true);
        }
    }

    public void softReset() {
        drawingState = DrawingState.Finished;
        controlRegs.setDisplayingFrameBufferPair(0, true, false); // ?
    }

    @Override
    public void reset() {
        drawingState = DrawingState.Finished;
        displayState = DisplayState.Finished;
        leftFb0.reset();
        leftFb1.reset();
        rightFb0.reset();
        rightFb1.reset();
        controlRegs.reset();
        displayCycles = 0;
        controlRegs.setDrawingFrameBufferPair(0, false);
        frameCounter = 0;
    }

    @Override
    public int getStart() {
        return 0x0000_0000;
    }

    @Override
    public int getSize() {
        return MAPPED_SIZE;
    }

    @Override
    protected MappedMemory getMappedModule(int address) {
        if (address >= 0x0007E000) {
            return chrTable3Mirror;
        }
        if (address >= 0x0007C000) {
            return chrTable2Mirror;
        }
        if (address >= 0x0007A000) {
            return chrTable1Mirror;
        }
        if (address >= 0x00078000) {
            return chrTable0Mirror;
        }
        if (address >= 0x00060000) {
            throw new BusError(address, Unimplemented); // not used ?
        }
        if (address >= 0x00040000) {
            return controlRegs;
        }
        if (address >= 0x0003E000) {
            return oam; // TODO
        }
        if (address >= 0x0003DC00) {
            return columnTable; // TODO Column Table
        }
        if (address >= 0x0003D800) {
            return windowAttr; // TODO Window Attributes
        }
        if (address >= 0x00020000) {
            return backgroundSegmentsAndWindowPataremeterTable; // TODO ?
        }
        if (address >= 0x0001E000) {
            return chrTable3; // TODO CHR RAM Pattern Table 3
        }
        if (address >= RIGHT_FRAMEBUFFER_1_START) {
            return rightFb1;
        }
        if (address >= 0x00016000) {
            return chrTable2; // TODO CHR RAM Pattern Table 2
        }
        if (address >= RIGHT_FRAMEBUFFER_0_START) {
            return rightFb0;
        }
        if (address >= 0x0000E000) {
            return chrTable1; // TODO CHR RAM Pattern Table 1
        }
        if (address >= LEFT_FRAMEBUFFER_1_START) {
            return leftFb1;
        }
        if (address >= 0x00006000) {
            return chrTable0; // TODO CHR RAM Pattern Table 0
        }
        if (address >= LEFT_FRAMEBUFFER_0_START) {
            return leftFb0;
        }
        throw new BusError(address, Unmapped);
    }
}
