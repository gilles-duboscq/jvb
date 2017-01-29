package gd.twohundred.jvb.components.vip;

import static gd.twohundred.jvb.Utils.extractU;
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

        ObjectAttributesMemory oam = vip.getObjectAttributesMemory();
        byte[] objectPalettes = vip.getControlRegisters().getObjectPalettes();
        int currentYBlock = vip.getControlRegisters().getCurrentYBlock();
        int minY = currentYBlock * DRAWING_BLOCK_HEIGHT;
        int maxY = minY + DRAWING_BLOCK_HEIGHT;

        while (objectIndex >= lastObjectIndex) {
            int objectAddr = objectIndex * ObjectAttributesMemory.ATTRIBUTES_SIZE;
            int parallaxLR = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_PARALLAX_LR_START);
            int parallax = extractU(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_POS, ObjectAttributesMemory.PARALLAX_LR_PARALLAX_LEN);
            boolean drawLeft = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_LEFT_POS);
            boolean drawRight = testBit(parallaxLR, ObjectAttributesMemory.PARALLAX_LR_RIGHT_POS);

            if ((left & !drawLeft) || (!left && !drawRight)) {
                continue;
            }

            int x = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_X_START);
            int y = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_Y_START);
            int cell = oam.getHalfWord(objectAddr + ObjectAttributesMemory.ATTRIBUTES_CELL_START);

            // TODO: just find the portion of character to draw and use drawChar rather than drawCharPixel
            for (int characterY = 0; characterY < CharacterRAM.CHARACTER_HEIGHT_PX; characterY++) {
                if (y + characterY >= maxY || y + characterY < minY) {
                    continue;
                }
                for (int characterX = 0; characterX < CharacterRAM.CHARACTER_WIDTH_PX; characterX++) {
                    drawCharacterPixel(x + characterX, y + characterY, characterX, characterY, cell, objectPalettes, vip, left);
                }
            }

            objectIndex--;
        }

        vip.setCurrentObjectGroup(objGroup - 1);
    }

    @Override
    public int getId() {
        return ID;
    }
}
