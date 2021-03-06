package gd.twohundred.jvb.components.debug.boxes;

import gd.twohundred.jvb.components.debug.View;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.Integer.max;

public class VerticalBoxes implements Box {
    private final String name;
    private final List<Box> boxes;
    private final int minWidth;
    private final int minHeight;

    public VerticalBoxes(String name, Box... boxes) {
        this(name, Arrays.asList(boxes));
    }

    public VerticalBoxes(String name, List<Box> boxes) {
        this.name = name;
        this.boxes = boxes;
        this.minWidth = boxes.stream().mapToInt(Box::minWidth).max().orElse(0);
        this.minHeight = boxes.stream().mapToInt(Box::minHeight).sum() + 2 * boxes.size();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public boolean fixedWidth() {
        return false;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public boolean fixedHeight() {
        return false;
    }

    @Override
    public void line(AttributedStringBuilder asb, int line, int width, int height) {
        int nonFixedHeight = getNonFixedHeight(height, boxes);
        int currentHeight = 0;
        int i = -1;
        Box box;
        int boxStart;
        int boxHeight;
        do {
            i++;
            boxStart = currentHeight;
            if (i >= boxes.size()) {
                return;
            }
            box = boxes.get(i);
            boxHeight = box.fixedHeight() ? box.minHeight() : nonFixedHeight;
            currentHeight += boxHeight + 2;
        } while (currentHeight <= line);
        if (line == boxStart + boxHeight + 1) {
            HorizontalBoxes.bottom(new int[]{max(0, width - 2)}, width, asb);
        }  else if (line == boxStart) {
            HorizontalBoxes.top(width, Collections.singletonList(box), new int[]{max(0, width - 2)}, asb);
        } else {
            boxLine(asb, box, line - (boxStart + 1), boxHeight, width);
        }
    }

    private static int getNonFixedHeight(int height, List<Box> boxes) {
        int nonFixed = 0;
        int remainingHeight = height;
        for (Box box : boxes) {
            if (box.fixedHeight()) {
                remainingHeight -= box.minHeight() + 2;
            } else {
                remainingHeight -= 2;
                nonFixed++;
            }
        }
        return nonFixed == 0 ? Integer.MIN_VALUE : max(0, remainingHeight / nonFixed);
    }

    public void appendLines(List<AttributedString> lines, int width, int height) {
        int nonFixedHeight = getNonFixedHeight(height, boxes);
        Iterator<Box> iterator = boxes.iterator();
        int currentBoxStart = -1;
        Box currentBox = null;
        for (int line = 0; line < height; line++) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            if (currentBox == null) {
                if (iterator.hasNext()) {
                    currentBox = iterator.next();
                    currentBoxStart = line + 1;
                    HorizontalBoxes.top(width, Collections.singletonList(currentBox), new int[]{max(0, width - 2)}, asb);
                }
            } else {
                int boxHeight = currentBox.fixedHeight() ? currentBox.minHeight() : nonFixedHeight;
                if (line == currentBoxStart + boxHeight) {
                    HorizontalBoxes.bottom(new int[]{max(0, width - 2)}, width, asb);
                    currentBox = null;
                } else {
                    boxLine(asb, currentBox, line - currentBoxStart, boxHeight, width);
                }
            }
            lines.add(asb.toAttributedString());
        }
    }

    private void boxLine(AttributedStringBuilder asb, Box box, int line, int height, int width) {
        int startLen = asb.length();
        if (width > 0) {
            asb.append('║');
        }
        box.line(asb, line, max(0, width - 2), height);
        if (asb.length() - startLen > width - 1) {
            asb.setLength(startLen + width - 1);
        } else {
            View.repeat(asb, width - 1 - (asb.length() - startLen), ' ');
        }
        if (width > 1) {
            asb.append('║');
        }
    }
}
