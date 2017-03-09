package gd.twohundred.jvb.components.debug.boxes;

import gd.twohundred.jvb.components.debug.View;
import org.jline.utils.AttributedStringBuilder;

import java.util.List;

import static java.lang.Integer.max;

public abstract class Table implements Box {
    private final List<Column> columns;
    private final String name;
    private final int minWidth;

    protected Table(String name, List<Column> columns) {
        this.columns = columns;
        this.minWidth = columns.stream().mapToInt(Column::minWidth).sum() + max(0, columns.size() - 1);
        this.name = name;
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
        return 0;
    }

    @Override
    public boolean fixedHeight() {
        return false;
    }

    @Override
    public void line(AttributedStringBuilder asb, int line, int width, int height) {
        int[] widths = getWidths(width, columns);
        if (line == 0) {
            header(width, columns, widths, asb);
        } else if (line == 1) {
            separator(widths, width, asb);
        } else {
            line(width, columns, widths, line - 2 , asb);
        }
    }

    public interface Column {
        String name();
        int minWidth();
        boolean fixedWidth();
        void cell(AttributedStringBuilder asb, int row, int width);
    }

    private static void line(int width, List<Column> columns, int[] widths, int row, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int i = 0; i < widths.length; i++) {
            Column column = columns.get(i);
            column.cell(asb, row, widths[i]);
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
            if (i < widths.length - 1) {
                asb.append('│');
                currentWidth++;
            }
            if (currentWidth > width) {
                asb.setLength(startLen +width);
                break;
            }
        }
    }

    private static void header(int width, List<Column> columns, int[] widths, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int i = 0; i < widths.length; i++) {
            Column column = columns.get(i);
            asb.append(column.name());
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
            if (i < widths.length - 1) {
                asb.append('│');
                currentWidth++;
            }
            if (currentWidth > width) {
                asb.setLength(startLen + width);
                break;
            }
        }
    }

    static void separator(int[] widths, int width, AttributedStringBuilder asb) {
        int currentWidth = 0;
        int startLen = asb.length();
        for (int i = 0; i < widths.length; i++) {
            View.repeat(asb, widths[i], '─');
            currentWidth += widths[i];
            if (i < widths.length - 1) {
                asb.append('┼');
                currentWidth++;
            }
            if (currentWidth > width) {
                asb.setLength(startLen + width);
                break;
            }
        }
    }

    private static int[] getWidths(int width, List<Column> columns) {
        int[] widths = new int[columns.size()];
        int nonFixed = 0;
        int remainingWidth = width;
        for (int i = 0; i < widths.length; i++) {
            Column box = columns.get(i);
            if (box.fixedWidth()) {
                remainingWidth -= box.minWidth() + 1;
                widths[i] = box.minWidth();
            } else {
                remainingWidth -= 1;
                nonFixed++;
            }
        }
        int nonFixedWidth = nonFixed == 0 ? Integer.MIN_VALUE : max(0, remainingWidth / nonFixed);
        for (int i = 0; i < widths.length; i++) {
            Column box = columns.get(i);
            if (!box.fixedWidth()) {
                widths[i] = nonFixedWidth;
            }
        }
        return widths;
    }
}
