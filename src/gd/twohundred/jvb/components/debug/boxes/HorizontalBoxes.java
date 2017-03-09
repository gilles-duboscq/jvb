package gd.twohundred.jvb.components.debug.boxes;

import gd.twohundred.jvb.components.debug.View;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.List;

import static java.lang.Integer.max;

public class HorizontalBoxes implements Box {
    private final String name;
    private final List<Box> boxes;
    private final int minWidth;
    private final int minHeight;

    public HorizontalBoxes(String name, List<Box> boxes) {
        this.name = name;
        this.boxes = boxes;
        this.minWidth = boxes.stream().mapToInt(Box::minWidth).sum() + 2 * boxes.size();
        this.minHeight = boxes.stream().mapToInt(Box::minHeight).max().orElse(0);
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
        int[] widths = getWidths(width, boxes);
        if (line == 0) {
            top(width, boxes, widths, asb);
        } else if (line == height - 1) {
            bottom(widths, width, asb);
        } else {
            line(width, max(0, height - 2), boxes, widths, line - 1, asb);
        }
    }

    public static void horizontalBoxes(List<AttributedString> lines, int width, int height, List<Box> boxes) {
        int[] widths = getWidths(width, boxes);
        AttributedStringBuilder top = new AttributedStringBuilder();
        top(width, boxes, widths, top);
        lines.add(top.toAttributedString());

        for (int i = 0; i < height - 2; i++) {
            AttributedStringBuilder line = new AttributedStringBuilder();
            line(width, max(0, height - 2), boxes, widths, i, line);
            lines.add(line.toAttributedString());
        }

        AttributedStringBuilder bottom = new AttributedStringBuilder();
        bottom(widths, width, bottom);
        lines.add(bottom.toAttributedString());
    }

    private static void line(int width, int height, List<Box> boxes, int[] widths, int line, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int i = 0; i < widths.length; i++) {
            Box box = boxes.get(i);
            asb.append('║');
            currentWidth++;
            box.line(asb, line, widths[i], height);
            currentWidth += widths[i];
            if (asb.length() - startLen > currentWidth) {
                if (widths[i] > 0) {
                    asb.setLength(startLen + currentWidth - 1);
                    asb.append('…');
                } else {
                    asb.setLength(startLen + currentWidth);
                }

            } else {
                View.repeat(asb, currentWidth - (asb.length() - startLen), ' ');
            }
            asb.append('║');
            currentWidth++;
            if (currentWidth > width) {
                asb.setLength(startLen +width);
                break;
            }
        }
    }

    static void bottom(int[] widths, int width, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int w : widths) {
            asb.append('╚');
            View.repeat(asb, w, '═');
            asb.append('╝');
            currentWidth += w + 2;
            if (currentWidth > width) {
                asb.setLength(startLen + width);
                break;
            }
        }
    }

    static void top(int width, List<Box> boxes, int[] widths, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int i = 0; i < widths.length; i++) {
            Box box = boxes.get(i);
            asb.append('╔');
            currentWidth++;
            if (widths[i] > 1) {
                asb.append('═');
            }
            asb.append(box.name());
            currentWidth += widths[i];
            if (asb.length() - startLen > currentWidth) {
                if (widths[i] > 0) {
                    asb.setLength(startLen + currentWidth - 1);
                    asb.append('…');
                } else {
                    asb.setLength(startLen + currentWidth);
                }

            } else {
                View.repeat(asb, currentWidth - (asb.length() - startLen), '═');
            }
            asb.append('╗');
            currentWidth++;
            if (currentWidth > width) {
                asb.setLength(startLen + width);
                break;
            }
        }
    }

    private static int[] getWidths(int width, List<Box> boxes) {
        int[] widths = new int[boxes.size()];
        int nonFixed = 0;
        int remainingWidth = width;
        for (int i = 0; i < widths.length; i++) {
            Box box = boxes.get(i);
            if (box.fixedWidth()) {
                remainingWidth -= box.minWidth() + 2;
                widths[i] = box.minWidth();
            } else {
                remainingWidth -= 2;
                nonFixed++;
            }
        }
        int nonFixedWidth = nonFixed == 0 ? Integer.MIN_VALUE : max(0, remainingWidth / nonFixed);
        for (int i = 0; i < widths.length; i++) {
            Box box = boxes.get(i);
            if (!box.fixedWidth()) {
                widths[i] = nonFixedWidth;
            }
        }
        return widths;
    }
}
