package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.utils.LinearMemoryMirroring;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.RenderedFrame;
import gd.twohundred.jvb.Screen;
import gd.twohundred.jvb.components.utils.WarningMemory;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Screen.HEIGHT;
import static gd.twohundred.jvb.Screen.WIDTH;
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
    private byte ledBrightness1 = 20;
    private byte ledBrightness2 = 60;
    private byte ledBrightness3 = 100;

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

    private RenderedFrame renderFrameBuffer(FrameBuffer fb) {
        RenderedFrame frame = new RenderedFrame();
        int fbAddr = 0;
        int imageAddr = 0;
        byte[] intensities = new byte[]{
                0,
                ledBrightness1,
                ledBrightness2,
                (byte) min(255, (ledBrightness1 & 0xff) + (ledBrightness2 & 0xff) + (ledBrightness3 & 0xff))
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

    @Override
    public void tickExact(int cycles) {
        if (false) {
            RenderedFrame left = renderFrameBuffer(leftFb0);
            RenderedFrame right = renderFrameBuffer(rightFb0);
            screen.update(left, right);
        }
    }

    @Override
    public void reset() {
        leftFb0.reset();
        leftFb1.reset();
        rightFb0.reset();
        rightFb1.reset();
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
        if (address >= 0x00040000) {
            throw new BusError(address, Unimplemented); // TODO control regs and above
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
            throw new BusError(address, Unimplemented); // TODO CHR RAM Pattern Table 3
        }
        if (address >= RIGHT_FRAMEBUFFER_1_START) {
            return rightFb1;
        }
        if (address >= 0x00016000) {
            throw new BusError(address, Unimplemented); // TODO CHR RAM Pattern Table 2
        }
        if (address >= RIGHT_FRAMEBUFFER_0_START) {
            return rightFb0;
        }
        if (address >= 0x0000E000) {
            throw new BusError(address, Unimplemented); // TODO CHR RAM Pattern Table 1
        }
        if (address >= LEFT_FRAMEBUFFER_1_START) {
            return leftFb1;
        }
        if (address >= 0x00006000) {
            throw new BusError(address, Unimplemented); // TODO CHR RAM Pattern Table 0
        }
        if (address >= LEFT_FRAMEBUFFER_0_START) {
            return leftFb0;
        }
        throw new BusError(address, Unmapped);
    }
}
