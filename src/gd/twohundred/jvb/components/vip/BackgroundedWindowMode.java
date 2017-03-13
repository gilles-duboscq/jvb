package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.components.interfaces.Screen;

import java.awt.*;

import static gd.twohundred.jvb.Utils.ceilDiv;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DRAWING_BLOCK_HEIGHT;
import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class BackgroundedWindowMode extends WindowMode {

    public void draw(WindowAttributes window, VirtualImageProcessor vip, boolean left) {
        if (!validateBackground(window)) {
            return;
        }
        int widthSegments = window.getBackgroundWidthSegments();
        int heightSegments = window.getBackgroundHeightSegments();
        byte[] backgroundPalettes = vip.getControlRegisters().getBackgroundPalettes();
        BackgroundSegmentsAndParametersRAM backgroundSegments = vip.getBackgroundSegmentsAndWindowParameterTable();

        assert Integer.bitCount(widthSegments) == 1;
        assert Integer.bitCount(heightSegments) == 1;

        int currentYBlock = vip.getControlRegisters().getCurrentYBlock();
        int minY = currentYBlock * DRAWING_BLOCK_HEIGHT;
        int maxY = minY + DRAWING_BLOCK_HEIGHT;

        int startWindowY = max(minY - window.getY(), 0);
        int endWindowY = min(maxY - window.getY(), window.getActualHeight());

        int parallax = left ? -window.getParallax() : window.getParallax();

        int backgroundWidth = widthSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX;
        int backgroundHeight = heightSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX;

        // TODO: just find the portion of segment/char to draw and use drawChar rather than drawCharPixel
        for (int windowY = startWindowY; windowY < endWindowY; windowY++) {
            int y = windowY + window.getY();
            for (int windowX = 0; windowX < window.getActualWidth(); windowX++) {
                int x = windowX + window.getX() + parallax;
                if (x >= Screen.WIDTH || x < 0) {
                    continue;
                }
                int backgroundX = getBackgroundX(window, vip, windowX, windowY, left);
                int backgroundY = getBackgroundY(window, vip, windowX, windowY, left);
                int cellAddr;
                if (window.isUseOutOfBoundsCharacter() && (backgroundX < 0 || backgroundX >= backgroundWidth || backgroundY < 0 || backgroundY >= backgroundHeight)) {
                    // oob char
                    int segmentIndex = window.getBaseSegmentIndex();
                    int segmentAddr = segmentIndex * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_SIZE;
                    cellAddr = segmentAddr + window.getOutOfBoundsCharacter() * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_CELL_SIZE;
                } else {
                    // tile
                    backgroundY = backgroundY & (widthSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX - 1);
                    backgroundX = backgroundX & (heightSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX - 1);
                    int xSegment = backgroundX / BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX;
                    int ySegment = backgroundY / BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX;
                    int segmentIndex = window.getBaseSegmentIndex() + xSegment + ySegment * widthSegments;

                    int segmentAddr = segmentIndex * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_SIZE;

                    int segmentX = backgroundX & (BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX - 1);
                    int segmentY = backgroundY & (BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX - 1);
                    int segmentXCell = segmentX / CharacterRAM.CHARACTER_WIDTH_PX;
                    int segmentYCell = segmentY / CharacterRAM.CHARACTER_HEIGHT_PX;
                    int cellIndex = segmentXCell + segmentYCell * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_CELLS;
                    cellAddr = segmentAddr + cellIndex * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_CELL_SIZE;
                }
                int characterX = backgroundX % CharacterRAM.CHARACTER_HEIGHT_PX;
                int characterY = backgroundY % CharacterRAM.CHARACTER_WIDTH_PX;
                int cell = backgroundSegments.getHalfWord(cellAddr);
                drawCharacterPixel(x, y, characterX, characterY, cell, backgroundPalettes, vip, left);
            }
        }
    }

    public int getBackgroundY(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left) {
        return window.getBackgroundY() + y;
    }

    public int getBackgroundX(WindowAttributes window, VirtualImageProcessor vip, int x, int y, boolean left) {
        int backgroundParallax = left ? -window.getBackgroundParallax() : window.getBackgroundParallax();
        return window.getBackgroundX() + backgroundParallax + x;
    }

    private static final int SEGMENT_COUNT = BackgroundSegmentsAndParametersRAM.SIZE / BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_SIZE;

    private boolean validateBackground(WindowAttributes window) {
        int segmentCount = window.getBackgroundHeightSegments() * window.getBackgroundWidthSegments();
        if (segmentCount > 8) {
            return false;
        }
        if (segmentCount + window.getBaseSegmentIndex() >= SEGMENT_COUNT) {
            return false;
        }
        return true;
    }

    @Override
    public void drawDebug(WindowAttributes window, VirtualImageProcessor vip, Graphics g, int scale) {
        super.drawDebug(window, vip, g, scale);
        g.setColor(Color.red);
        g.drawString("BG.x:" + window.getBackgroundX() + " BG.y:" + window.getBackgroundY()
                + " BG.sw:" + window.getBackgroundWidthSegments()+ " BG.sh:" + window.getBackgroundHeightSegments()
                + " BG.oob:" + (window.isUseOutOfBoundsCharacter() ? "char" : "tile")
                , 2 +window.getX() * scale, window.getY() * scale + g.getFontMetrics().getHeight() * 2);
        int widthChars = ceilDiv(window.getActualWidth(), CharacterRAM.CHARACTER_WIDTH_PX);
        int heightChars = ceilDiv(window.getActualHeight(), CharacterRAM.CHARACTER_HEIGHT_PX);
        int bgDx = window.getBackgroundX() % CharacterRAM.CHARACTER_WIDTH_PX;
        int bgDy = window.getBackgroundY() % CharacterRAM.CHARACTER_HEIGHT_PX;
        for (int cx = 0; cx < widthChars; cx++) {
            for (int cy = 0; cy < heightChars; cy++) {
                g.drawRect((window.getX() - bgDx + cx * CharacterRAM.CHARACTER_WIDTH_PX + window.getParallax() + window.getBackgroundParallax()) * scale,
                        (window.getY() - bgDy + cy * CharacterRAM.CHARACTER_HEIGHT_PX) * scale,
                        CharacterRAM.CHARACTER_WIDTH_PX * scale, CharacterRAM.CHARACTER_HEIGHT_PX * scale);
            }
        }
    }
}
