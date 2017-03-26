package gd.twohundred.jvb.components.vip;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.Screen;

import java.awt.*;

import static gd.twohundred.jvb.Utils.extractS;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.components.vip.VirtualImageProcessor.DRAWING_BLOCK_HEIGHT;

public class ObjectWindowMode extends WindowMode {
    public static final ObjectWindowMode INSTANCE = new ObjectWindowMode();
    public static final int ID = 3;
    @Override
    public void draw(WindowAttributes window, VirtualImageProcessor vip, boolean left) {
        int objGroup = vip.getCurrentObjectGroup();
        if (objGroup < 0) {
            return;
        }
        int objectIndex = vip.getControlRegisters().getObjectGroupIndexes()[objGroup];
        int lastObjectIndex = getLastObjectIndex(vip, objGroup, objectIndex);

        ObjectAttributesMemory oam = vip.getObjectAttributesMemory();
        byte[] objectPalettes = vip.getControlRegisters().getObjectPalettes();
        int currentYBlock = vip.getControlRegisters().getCurrentYBlock();
        int minY = currentYBlock * DRAWING_BLOCK_HEIGHT;
        int maxY = minY + DRAWING_BLOCK_HEIGHT;

        while (objectIndex >= lastObjectIndex) {
            int objectAddr = objectIndex * ObjectAttributesMemory.ATTRIBUTES_SIZE;
            int parallaxLR = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_PARALLAX_LR_START);
            int parallax = extractS(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_POS, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_LEN);
            boolean drawLeft = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_LEFT_POS);
            boolean drawRight = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_RIGHT_POS);

            if ((left && !drawLeft) || (!left && !drawRight)) {
                objectIndex--;
                continue;
            }

            int parallaxX = left ? -parallax : parallax;

            int x = signExtend(oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_X_START), Short.SIZE);
            int y = signExtend(oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_Y_START), Short.SIZE);
            int cell = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_CELL_START);

            // TODO: just find the portion of character to draw and use drawChar rather than drawCharPixel
            for (int characterY = 0; characterY < CharacterRAM.CHARACTER_HEIGHT_PX; characterY++) {
                int screenY = y + characterY + window.getY();
                if (screenY >= maxY || screenY < minY) {
                    continue;
                }
                for (int characterX = 0; characterX < CharacterRAM.CHARACTER_WIDTH_PX; characterX++) {
                    int screenX = x + characterX + parallaxX + window.getX();
                    if (screenX >= Screen.WIDTH || screenX < 0) {
                        continue;
                    }
                    drawCharacterPixel(screenX, screenY, characterX, characterY, cell, objectPalettes, vip, left);
                }
            }
            objectIndex--;
        }
    }

    private int getLastObjectIndex(VirtualImageProcessor vip, int objGroup, int objectIndex) {
        int lastObjectIndex;
        if (objGroup == 0) {
            lastObjectIndex = 0;
        } else {
            short nextStart = vip.getControlRegisters().getObjectGroupIndexes()[objGroup - 1];
            if (nextStart >= objectIndex) {
                lastObjectIndex = 0;
            } else {
                lastObjectIndex = nextStart + 1;
            }
        }
        return lastObjectIndex;
    }

    @Override
    public void onFinished(WindowAttributes window, VirtualImageProcessor vip) {
        super.onFinished(window, vip);
        vip.setCurrentObjectGroup(vip.getCurrentObjectGroup() - 1);
    }

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public String getShortName() {
        return "O";
    }

    @Override
    public void drawDebug(WindowAttributes window, VirtualImageProcessor vip, Graphics g, int scale) {
        super.drawDebug(window, vip, g, scale);
        int objGroup = 3;
        for (WindowAttributes w : vip.getWindowAttributes()) {
            if (w == window) {
                break;
            }
            if (w.getMode() instanceof ObjectWindowMode) {
                objGroup--;
            }
        }
        if (objGroup < 0) {
            return;
        }
        int objectIndex = vip.getControlRegisters().getObjectGroupIndexes()[objGroup];
        int lastObjectIndex = getLastObjectIndex(vip, objGroup, objectIndex);

        ObjectAttributesMemory oam = vip.getObjectAttributesMemory();
        byte[] objectPalettes = vip.getControlRegisters().getObjectPalettes();
        while (objectIndex >= lastObjectIndex) {
            int objectAddr = objectIndex * ObjectAttributesMemory.ATTRIBUTES_SIZE;
            int parallaxLR = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_PARALLAX_LR_START);
            int parallax = extractS(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_POS, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_LEN);
            boolean drawLeft = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_LEFT_POS);
            boolean drawRight = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_RIGHT_POS);

            int x = signExtend(oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_X_START), Short.SIZE);
            int y = signExtend(oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_Y_START), Short.SIZE);
            int cell = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_CELL_START);
            int characterIndex = extractU(cell, CELL_CHARACTER_POS, CELL_CHARACTER_LEN);
            int paletteId = extractU(cell, CELL_PALETTE_INDEX_POS, CELL_PALETTE_INDEX_LEN);
            if (drawLeft) {
                g.setColor(Color.MAGENTA);
                g.drawRect((x - parallax) * scale, y * scale, CharacterRAM.CHARACTER_WIDTH_PX * scale, CharacterRAM.CHARACTER_HEIGHT_PX * scale);
            }
            if (drawRight) {
                g.setColor(Color.YELLOW);
                g.drawRect((x + parallax) * scale, y * scale, CharacterRAM.CHARACTER_WIDTH_PX * scale, CharacterRAM.CHARACTER_HEIGHT_PX * scale);
            }
            if (drawLeft || drawRight) {
                g.setColor(Color.CYAN);
                g.drawString(String.format("%x", characterIndex), x * scale, y * scale + g.getFontMetrics().getHeight());
                //g.drawString(String.format("%x", objectPalettes[paletteId]), x * scale, y * scale + 2 * g.getFontMetrics().getHeight());
                g.drawString(String.format("%x", objectIndex), x * scale, y * scale + 2 * g.getFontMetrics().getHeight());
            } else if (x >= 0 && x < Screen.WIDTH && y >= 0 && y < Screen.HEIGHT) {
                g.setColor(Color.ORANGE);
                g.fillRect(x * scale, y * scale, CharacterRAM.CHARACTER_WIDTH_PX * scale, CharacterRAM.CHARACTER_HEIGHT_PX * scale);
            }
            objectIndex--;
        }
    }
}
