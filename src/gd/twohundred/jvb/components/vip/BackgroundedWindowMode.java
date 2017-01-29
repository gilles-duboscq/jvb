package gd.twohundred.jvb.components.vip;

import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DRAWING_BLOCK_HEIGHT;
import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class BackgroundedWindowMode extends WindowMode {

    protected void drawBackground(WindowAttributes window, VirtualImageProcessor vip, boolean left) {
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
        int endWindowY = min(maxY - window.getY(), window.getHeight());

        int parallax = left ? -window.getParallax() : window.getParallax();
        int backgroundParallax = left ? -window.getBackgroundParallax() : window.getBackgroundParallax();

        int backgroundWidth = widthSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX;
        int backgroundHeight = heightSegments * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX;

        // TODO: just find the portion of segment/char to draw and use drawChar rather than drawCharPixel
        for (int windowY = startWindowY; windowY < endWindowY; windowY++) {
            int y = windowY + window.getY();
            int backgroundY = windowY + window.getBackgroundY();
            for (int windowX = 0; windowX < window.getWidth(); windowX++) {
                int x = windowX + window.getX() + parallax;
                int backgroundX = windowX + window.getBackgroundX() + backgroundParallax;
                int cellAddr;
                if (window.isUseOutOfBoundsCharacter() && (backgroundX >= backgroundWidth || backgroundY >= backgroundHeight)) {
                    // oob char
                    int segmentIndex = window.getBaseSegmentIndex();
                    int segmentAddr = segmentIndex * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_SIZE;
                    cellAddr = segmentAddr + window.getOutOfBoundsCharacter() * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_CELL_SIZE;
                } else {
                    // tile
                    int xSegment = (backgroundX / BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX) & (widthSegments - 1);
                    int ySegment = (backgroundY / BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX) & (heightSegments - 1);
                    int segmentIndex = window.getBaseSegmentIndex() + xSegment + ySegment * widthSegments;

                    int segmentAddr = segmentIndex * BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_SIZE;

                    int segmentX = backgroundX % BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_WIDTH_PX;
                    int segmentY = backgroundY % BackgroundSegmentsAndParametersRAM.BACKGROUND_SEGMENT_HEIGHT_PX;
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
}
