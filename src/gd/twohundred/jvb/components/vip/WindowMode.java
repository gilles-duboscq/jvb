package gd.twohundred.jvb.components.vip;

import java.awt.*;

import static gd.twohundred.jvb.Utils.extractNthU;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.testBit;

public abstract class WindowMode {
    static final int CELL_CHARACTER_POS = 0;
    static final int CELL_CHARACTER_LEN = 11;
    static final int CELL_VERTICAL_FLIP_POS = 12;
    static final int CELL_HORIZONTAL_FLIP_POS = 13;
    static final int CELL_PALETTE_INDEX_POS = 14;
    static final int CELL_PALETTE_INDEX_LEN = 2;

    public static WindowMode get(int id) {
        switch (id) {
            case NormalWindowMode.ID:
                return NormalWindowMode.INSTANCE;
            case ObjectWindowMode.ID:
                return ObjectWindowMode.INSTANCE;
            case LineShiftWindowMode.ID:
                return LineShiftWindowMode.INSTANCE;
            case AffineWindowMode.ID:
                return AffineWindowMode.INSTANCE;
        }
        throw new RuntimeException("NYI: " + id);
    }

    public abstract String getShortName();

    public abstract void draw(WindowAttributes window, VirtualImageProcessor vip, boolean left);

    public void drawDebug(WindowAttributes window, VirtualImageProcessor vip, Graphics g, int scale) {
        g.setColor(Color.green);
        g.drawRect(window.getX() * scale, window.getY() * scale, window.getActualWidth() * scale, window.getActualHeight() * scale);
        g.drawString("W" + window.getId() + " W.w:" + window.getActualWidth() + " W.h:" + window.getActualHeight(),
                2 +window.getX() * scale, window.getY() * scale + g.getFontMetrics().getHeight());
    }

    public void onFinished(WindowAttributes window, VirtualImageProcessor vip) {

    }

    public void drawCharacterPixel(int x, int y, int characterX, int characterY, int cell, byte[] palettes, VirtualImageProcessor vip, boolean left) {
        int characterIndex = extractU(cell, CELL_CHARACTER_POS, CELL_CHARACTER_LEN);
        int paletteId = extractU(cell, CELL_PALETTE_INDEX_POS, CELL_PALETTE_INDEX_LEN);
        int palette = palettes[paletteId] & 0b11_11_11_00;
        int effectiveCharacterX = characterX;
        int effectiveCharacterY = characterY;
        if (testBit(cell, CELL_HORIZONTAL_FLIP_POS)) {
            effectiveCharacterX = CharacterRAM.CHARACTER_WIDTH_PX - effectiveCharacterX - 1;
        }
        if (testBit(cell, CELL_VERTICAL_FLIP_POS)) {
            effectiveCharacterY = CharacterRAM.CHARACTER_HEIGHT_PX - effectiveCharacterY - 1;
        }
        int charPixelIndex = effectiveCharacterX + effectiveCharacterY * CharacterRAM.CHARACTER_WIDTH_PX;
        if (charPixelIndex < 0) {
            trap();
        }
        int charPixelByteAddr = characterIndex * CharacterRAM.CHARACTER_SIZE + charPixelIndex / FrameBuffer.PIXEL_PER_BYTE;
        int pixel = extractNthU(vip.getCharacterRam().getByte(charPixelByteAddr), charPixelIndex % FrameBuffer.PIXEL_PER_BYTE, FrameBuffer.BITS_PER_PIXEL);
        if (pixel != 0) {
            int color = extractNthU(palette, pixel, FrameBuffer.BITS_PER_PIXEL);
            FrameBuffer frameBuffer = vip.getCurrentFrameBuffer(left);
            frameBuffer.setPixel(x, y, color);
        }
    }

    private void trap() {
        int i = 0;
    }

    public abstract int getId();

    public long cycles() {
        // Fake values
        long drawingPeriod = VirtualImageProcessor.FRAME_PERIOD / 4;
        long windowCycles = drawingPeriod - VirtualImageProcessor.DRAWING_INIT_CYCLES - VirtualImageProcessor.DRAWING_WINDOW_COUNT * VirtualImageProcessor.DRAWING_BLOCK_CLEAR_CYCLES;
        return windowCycles / (VirtualImageProcessor.DRAWING_WINDOW_COUNT * VirtualImageProcessor.DRAWING_BLOCK_COUNT);
    }
}
