package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.RenderedFrame;
import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.SimpleInterrupt;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.interfaces.Screen;
import gd.twohundred.jvb.components.utils.LinearMemoryMirroring;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.utils.WarningMemory;
import gd.twohundred.jvb.components.vip.VIPControlRegisters.VIPInterruptType;

import java.awt.*;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.NANOS_PER_SECOND;
import static gd.twohundred.jvb.Utils.repeat;
import static gd.twohundred.jvb.components.interfaces.Screen.HEIGHT;
import static gd.twohundred.jvb.components.interfaces.Screen.WIDTH;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.LeftFrameBuffer;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.RightFrameBuffer;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DisplayState.Waiting;
import static java.lang.Math.min;

public class VirtualImageProcessor extends MappedModules implements ExactlyEmulable, InterruptSource {
    public static final int MAPPED_SIZE = 0x0100_0000;
    public static final int SIZE = 0x0008_0000;
    public static final int RIGHT_FRAMEBUFFER_1_START = 0x0018_0000;
    public static final int RIGHT_FRAMEBUFFER_0_START = 0x0001_0000;
    public static final int LEFT_FRAMEBUFFER_0_START = 0x0000_0000;
    public static final int LEFT_FRAMEBUFFER_1_START = 0x0000_8000;
    static final int WINDOW_ATTRIBUTES_START = 0x0003_D800;
    public static final int WINDOW_ATTRIBUTE_COUNT = 32;


    private final Screen screen;
    private final FrameBuffer leftFb0 = new FrameBuffer(LEFT_FRAMEBUFFER_0_START);
    private final FrameBuffer leftFb1 = new FrameBuffer(LEFT_FRAMEBUFFER_1_START);
    private final FrameBuffer rightFb0 = new FrameBuffer(RIGHT_FRAMEBUFFER_0_START);
    private final FrameBuffer rightFb1 = new FrameBuffer(RIGHT_FRAMEBUFFER_1_START);
    private final VIPControlRegisters controlRegs = new VIPControlRegisters(this);
    private final LinearMemoryMirroring controlRegsMirror = new LinearMemoryMirroring(controlRegs, 0x00040000, 0, 0x80);
    private final WindowAttributes[] windowAttributes = new WindowAttributes[WINDOW_ATTRIBUTE_COUNT];

    private final BackgroundSegmentsAndParametersRAM backgroundSegmentsAndWindowParameterTable =
            new BackgroundSegmentsAndParametersRAM();
    private final WarningMemory columnTable =
            new WarningMemory("Column Table", 0x0003DC00, 0x400);
    private final ObjectAttributesMemory oam = new ObjectAttributesMemory();

    private final CharacterRAM characterRAM = new CharacterRAM();

    private final LinearMemoryMirroring chrTable0Mirror = new LinearMemoryMirroring(characterRAM, 0x00006000, 0x0000, 0x2000);
    private final LinearMemoryMirroring chrTable1Mirror = new LinearMemoryMirroring(characterRAM, 0x0000E000, 0x2000, 0x2000);
    private final LinearMemoryMirroring chrTable2Mirror = new LinearMemoryMirroring(characterRAM, 0x00016000, 0x4000, 0x2000);
    private final LinearMemoryMirroring chrTable3Mirror = new LinearMemoryMirroring(characterRAM, 0x0001E000, 0x6000, 0x2000);

    public static final boolean DEBUG_GRAPHICS = true;

    public VirtualImageProcessor(Screen screen) {
        this.screen = screen;
        for (int i = 0; i < WINDOW_ATTRIBUTE_COUNT; i++) {
            windowAttributes[i] = new WindowAttributes(i);
        }
        if (DEBUG_GRAPHICS) {
            debugDrawer = this::drawDebug;
        }
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

    static final int FRAME_PERIOD = (int) (CPU.CLOCK_HZ / Screen.DISPLAY_REFRESH_RATE_HZ);

    // Fake value
    static final int DRAWING_INIT_CYCLES = 300;

    private static final int MAX_COLUMN_TIME = 0xfe;
    private static final long FOUR_COLUMN_UNIT_TIME_NS = 200;
    private static final long COLUMN_UNIT_TIME_NS = FOUR_COLUMN_UNIT_TIME_NS / 4;
    private static final long MAX_FRAME_BUFFER_DISPLAY_TIME_NS = MAX_COLUMN_TIME * COLUMN_UNIT_TIME_NS * Screen.WIDTH;
    private static final int MAX_FRAME_BUFFER_DISPLAY_CYCLES = (int) (CPU.CLOCK_HZ * MAX_FRAME_BUFFER_DISPLAY_TIME_NS / NANOS_PER_SECOND);
    private static final int RIGH_DISPLAY_START_CYCLE = FRAME_PERIOD - (10 + MAX_FRAME_BUFFER_DISPLAY_CYCLES);
    private static final int LEFT_DISPLAY_START_CYCLE = RIGH_DISPLAY_START_CYCLE - (10 + MAX_FRAME_BUFFER_DISPLAY_CYCLES);

    static final int DRAWING_WINDOW_COUNT = 32;
    static final int DRAWING_BLOCK_HEIGHT = 8;
    static final int DRAWING_BLOCK_COUNT = Screen.HEIGHT / DRAWING_BLOCK_HEIGHT;

    private final RenderedFrame leftRendered = new RenderedFrame();
    private final RenderedFrame rightRendered = new RenderedFrame();
    private int displayCycles;
    private int nextWindowCycles;
    private long frameCounter;
    private DrawingState drawingState;
    private DisplayState displayState;
    private Screen.DebugDrawer debugDrawer;

    private int currentWindowId;
    private int latchedClearColor;
    private int currentObjectGroup;

    private FrameBuffer currentRight = rightFb1;
    private FrameBuffer currentLeft = leftFb1;

    private boolean interruptRaised;

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
            displayCycles += cycles;
            if (displayCycles >= FRAME_PERIOD) {
                leftRendered.clear();
                rightRendered.clear();
                screen.update(leftRendered, rightRendered, debugDrawer);
                displayCycles = 0;
            }
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
                screen.update(leftRendered, rightRendered, debugDrawer);
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), true, false);
                interrupt(VIPInterruptType.LeftDisplayFinished);
            } else if (displayCycles == RIGH_DISPLAY_START_CYCLE) {
                displayState = RightFrameBuffer;
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), false, true);
                renderFrameBuffer(currentRight, rightRendered);
            } else if (displayCycles == RIGH_DISPLAY_START_CYCLE + MAX_FRAME_BUFFER_DISPLAY_CYCLES) {
                screen.update(leftRendered, rightRendered, debugDrawer);
                controlRegs.setDisplayingFrameBufferPair(currentFbPair(), false, false);
                interrupt(VIPInterruptType.RightDisplayFinished);
                displayState = DisplayState.Finished;
            }
            if (drawingState != DrawingState.Finished) {
                tickDrawing();
            }
            cyclesToConsume--;
            displayCycles++;
        }
    }

    private void interrupt(VIPInterruptType type) {
        if (controlRegs.isInterruptEnabled(type)) {
            interruptRaised = true;
            controlRegs.addPendingInterrupt(type);
        }
    }

    private int currentFbPair() {
        return rightFb0 == currentRight ? 0 : 1;
    }

    private void startDisplay() {
        if (drawingState != DrawingState.Finished) {
            controlRegs.setDrawingExceedsFramePeriod();
            interrupt(VIPInterruptType.DrawingExceedsFramePeriod);
        }
        controlRegs.setDisplayProcStart();
        displayState = Waiting;
        controlRegs.setDisplayingFrameBufferPair(0, true, false);
        interrupt(VIPInterruptType.StartFrameProcessing);
        if (controlRegs.isDrawingEnabled() && frameCounter % (controlRegs.getFrameRepeat() + 1) == 0) {
            startDrawing();
        }
        frameCounter++;
    }

    private void startDrawing() {
        interrupt(VIPInterruptType.StartDrawing);
        drawingState = DrawingState.Drawing;
        nextWindowCycles = DRAWING_INIT_CYCLES;
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
        setCurrentYBlock(0);
        currentWindowId = DRAWING_WINDOW_COUNT - 1;
    }

    private void setCurrentYBlock(int block) {
        controlRegs.setCurrentYBlock(block);
        if (block == controlRegs.getInterruptYPosition()) {
            interrupt(VIPInterruptType.DrawingYPositionMatch);
        }
    }

    private void tickDrawing() {
        if (displayCycles < nextWindowCycles) {
            return;
        }
        int currentYBlock = controlRegs.getCurrentYBlock();
        if (currentWindowId < 0 || getCurrentWindow().isStop()) {
            int nextBlock = currentYBlock + 1;
            if (nextBlock >= DRAWING_BLOCK_COUNT) {
                endDrawing();
                return;
            } else {
                setCurrentYBlock(nextBlock);
                currentWindowId = DRAWING_WINDOW_COUNT - 1;
            }
        }
        if (currentWindowId == DRAWING_WINDOW_COUNT - 1) {
            clearCurrentBlock();
            latchedClearColor = controlRegs.getClearColor();
            currentObjectGroup = 3;
        }
        WindowAttributes window = getCurrentWindow();
        if (window.isDrawLeft()) {
            window.getMode().draw(window, this, true);
        }
        if (window.isDrawRight()) {
            window.getMode().draw(window, this, false);
        }
        window.getMode().onFinished(window, this);
        currentWindowId--;
        nextWindowCycles += window.getMode().cycles();
    }

    private void endDrawing() {
        controlRegs.setDrawingFrameBufferPair(0, false);
        drawingState = DrawingState.Finished;
        interrupt(VIPInterruptType.DrawingFinished);
    }

    private WindowAttributes getCurrentWindow() {
        return windowAttributes[currentWindowId];
    }

    public FrameBuffer getCurrentFrameBuffer(boolean left) {
        return left ? currentLeft : currentRight;
    }

    VIPControlRegisters getControlRegisters() {
        return controlRegs;
    }

    int getCurrentObjectGroup() {
        return currentObjectGroup;
    }

    void setCurrentObjectGroup(int currentObjectGroup) {
        this.currentObjectGroup = currentObjectGroup;
    }

    ObjectAttributesMemory getObjectAttributesMemory() {
        return oam;
    }

    private void clearCurrentBlock() {
        short clearHalfWord = (short) repeat(latchedClearColor, FrameBuffer.BITS_PER_PIXEL, Short.SIZE);
        int currentYBlock = controlRegs.getCurrentYBlock();
        for (int col = 0; col < FrameBuffer.WIDTH; col++) {
            int addr = col * FrameBuffer.HEIGHT / FrameBuffer.PIXEL_PER_BYTE + currentYBlock * DRAWING_BLOCK_HEIGHT / FrameBuffer.PIXEL_PER_BYTE;
            currentLeft.setHalfWord(addr, clearHalfWord);
            currentRight.setHalfWord(addr, clearHalfWord);
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
        characterRAM.reset();
        backgroundSegmentsAndWindowParameterTable.reset();
        for (WindowAttributes window : windowAttributes) {
            window.reset();
        }
        displayCycles = 0;
        controlRegs.setDrawingFrameBufferPair(0, false);
        frameCounter = 0;
        currentWindowId = -1;
        latchedClearColor = 0;
        leftRendered.clear();
        rightRendered.clear();
    }

    public BackgroundSegmentsAndParametersRAM getBackgroundSegmentsAndWindowParameterTable() {
        return backgroundSegmentsAndWindowParameterTable;
    }

    public CharacterRAM getCharacterRam() {
        return characterRAM;
    }

    @Override
    public Interrupt raised() {
        if (interruptRaised) {
            return new SimpleInterrupt(Interrupt.InterruptType.VIP);
        }
        return null;
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
        if (address >= CharacterRAM.START) {
            return characterRAM;
        }
        if (address >= 0x00060000) {
            throw new BusError(address, Unimplemented); // not used ?
        }
        if (address >= VIPControlRegisters.START) {
            return controlRegs;
        }
        if (address >= 0x00040000) {
            return controlRegsMirror; // mirroring?
        }
        if (address >= ObjectAttributesMemory.START) {
            return oam;
        }
        if (address >= 0x0003DC00) {
            return columnTable; // TODO Column Table
        }
        if (address >= WINDOW_ATTRIBUTES_START) {
            int windowAttributesBlockAddress = address - WINDOW_ATTRIBUTES_START;
            int windowId = windowAttributesBlockAddress / WindowAttributes.SIZE;
            return windowAttributes[windowId];
        }
        if (address >= BackgroundSegmentsAndParametersRAM.START) {
            return backgroundSegmentsAndWindowParameterTable;
        }
        if (address >= 0x0001E000) {
            return chrTable3Mirror;
        }
        if (address >= RIGHT_FRAMEBUFFER_1_START) {
            return rightFb1;
        }
        if (address >= 0x00016000) {
            return chrTable2Mirror;
        }
        if (address >= RIGHT_FRAMEBUFFER_0_START) {
            return rightFb0;
        }
        if (address >= 0x0000E000) {
            return chrTable1Mirror;
        }
        if (address >= LEFT_FRAMEBUFFER_1_START) {
            return leftFb1;
        }
        if (address >= 0x00006000) {
            return chrTable0Mirror;
        }
        if (address >= LEFT_FRAMEBUFFER_0_START) {
            return leftFb0;
        }
        throw new BusError(address, Unmapped);
    }

    private void drawDebug(Graphics g, int scale) {
        for (int id = 31; id >= 0; id--) {
            WindowAttributes window = windowAttributes[id];
            if (window.isStop()) {
                break;
            }
            window.getMode().drawDebug(window, this, g, scale);
        }
    }
}
