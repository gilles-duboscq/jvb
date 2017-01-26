package gd.twohundred.jvb;

import java.util.Random;

import static gd.twohundred.jvb.Screen.HEIGHT;
import static gd.twohundred.jvb.Screen.WIDTH;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class VirtualImageProcessor implements Emulable, ReadWriteMemory {
    public static final int MEMORY_MAP_SIZE = 0x0100_0000;
    private final Screen screen;

    private FrameBuffer leftFb0 = new FrameBuffer(0x0000_0000);
    private FrameBuffer leftFb1 = new FrameBuffer(0x0000_8000);
    private FrameBuffer rightFb0 = new FrameBuffer(0x0001_0000);
    private FrameBuffer rightFb1 = new FrameBuffer(0x0018_0000);
    private byte ledBrightness1 = 20;
    private byte ledBrightness2 = 60;
    private byte ledBrightness3 = 100;

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
    public void tick() {
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
        return MEMORY_MAP_SIZE;
    }

    @Override
    public int getByte(int address) {
        System.err.println(String.format("WARNING: NYI: VIP@%08x", address));
        return 0;
    }

    @Override
    public int getHalfWord(int address) {
        System.err.println(String.format("WARNING: NYI: VIP@%08x", address));
        return 0;
    }

    @Override
    public int getWord(int address) {
        System.err.println(String.format("WARNING: NYI: VIP@%08x", address));
        return 0;
    }

    @Override
    public void setByte(int address, byte value) {

    }
}
