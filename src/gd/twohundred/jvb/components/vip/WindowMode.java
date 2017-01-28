package gd.twohundred.jvb.components.vip;

import static gd.twohundred.jvb.Utils.extractNthU;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.testBit;

public abstract class WindowMode {
    private static final int CELL_CHARACTER_POS = 0;
    private static final int CELL_CHARACTER_LEN = 11;
    private static final int CELL_VERTICAL_FLIP_POS = 12;
    private static final int CELL_HORIZONTAL_FLIP_POS = 13;
    private static final int CELL_PALETTE_INDEX_POS = 14;
    private static final int CELL_PALETTE_INDEX_LEN = 2;

    public static WindowMode get(int i) {
        switch (i) {
            case NormalWindowMode.ID:
                return NormalWindowMode.INSTANCE;
        }
        throw new RuntimeException("NYI: " + i);
    }

    public abstract void draw(WindowAttributes window, VirtualImageProcessor vip, boolean left);

    public void drawCharacter(int x, int y, int characterX, int characterY, int cellAddr, VirtualImageProcessor vip, boolean left) {
        BackgroundSegmentsAndParametersRAM backgroundSegments = vip.getBackgroundSegmentsAndWindowParameterTable();
        int cell = backgroundSegments.getHalfWord(cellAddr);
        int characterIndex = extractU(cell, CELL_CHARACTER_POS, CELL_CHARACTER_LEN);
        int paletteId = extractU(cell, CELL_PALETTE_INDEX_POS, CELL_PALETTE_INDEX_LEN);
        int palette = vip.getControlRegisters().getBackgroundPalette()[paletteId] & 0b11_11_11_00;
        int effectiveCharacterX = characterX;
        int effectiveCharacterY = characterY;
        if (testBit(cell, CELL_HORIZONTAL_FLIP_POS)) {
            effectiveCharacterX = CharacterRAM.CHARACTER_WIDTH_PX - effectiveCharacterX - 1;
        }
        if (testBit(cell, CELL_VERTICAL_FLIP_POS)) {
            effectiveCharacterY = CharacterRAM.CHARACTER_HEIGHT_PX - effectiveCharacterY - 1;
        }
        int charPixelIndex = effectiveCharacterX + effectiveCharacterY * CharacterRAM.CHARACTER_WIDTH_PX;
        int charPixelByteAddr = characterIndex * CharacterRAM.CHARACTER_SIZE + charPixelIndex / FrameBuffer.PIXEL_PER_BYTE;
        int pixel = extractNthU(vip.getCharacterRam().getByte(charPixelByteAddr), charPixelIndex % FrameBuffer.PIXEL_PER_BYTE, FrameBuffer.BITS_PER_PIXEL);
        int color = extractNthU(palette, pixel, FrameBuffer.BITS_PER_PIXEL);
        FrameBuffer frameBuffer = vip.getCurrentFrameBuffer(left);
        frameBuffer.setPixel(x, y, color);
    }
}
