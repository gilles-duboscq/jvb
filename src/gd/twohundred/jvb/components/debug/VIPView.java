package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.debug.boxes.Table;
import gd.twohundred.jvb.components.debug.boxes.Table.Column;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vip.WindowAttributes;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VIPView implements View {
    private final Debugger debugger;
    private final WindowAttributesTable windowAttributesTable;
    private final VerticalBoxes verticalBoxes;

    private abstract class WindowAttributesColumn implements Column {
        private final String name;
        private final int width;

        protected WindowAttributesColumn(String name, int width) {
            this.name = name;
            this.width = width;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int minWidth() {
            return width;
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public void cell(AttributedStringBuilder asb, int row, int width) {
            WindowAttributes[] windowAttributes = debugger.getWindowAttributes();
            if (row < VirtualImageProcessor.WINDOW_ATTRIBUTE_COUNT) {
                cell(asb, windowAttributes[VirtualImageProcessor.WINDOW_ATTRIBUTE_COUNT - row - 1]);
            }
        }

        protected abstract void cell(AttributedStringBuilder asb, WindowAttributes attributes);
    }

    private class IDColumn extends WindowAttributesColumn {
        protected IDColumn() {
            super("ID", 2);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getId());
        }
    }

    private class BaseSegmentColumn extends WindowAttributesColumn {
        protected BaseSegmentColumn() {
            super("BS", 2);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getBaseSegmentIndex());
        }
    }

    private class StopColumn extends WindowAttributesColumn {
        protected StopColumn() {
            super("S", 1);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            asb.append(attributes.isStop() ? 'S' : ' ');
        }
    }

    private class XColumn extends WindowAttributesColumn {
        protected XColumn() {
            super("X", 5);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getX());
        }
    }

    private class YColumn extends WindowAttributesColumn {
        protected YColumn() {
            super("Y", 5);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getY());
        }
    }

    private class HeightColumn extends WindowAttributesColumn {
        protected HeightColumn() {
            super("Height", 5);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getHeight());
        }
    }

    private class WidthColumn extends WindowAttributesColumn {
        protected WidthColumn() {
            super("Width", 5);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadInt(asb, minWidth(), attributes.getWidth());
        }
    }

    private class WindowAttributesTable extends Table {
        private WindowAttributesTable(Terminal terminal) {
            super("Windows", Arrays.asList(new IDColumn(), new StopColumn(), new XColumn(), new YColumn(), new WidthColumn(), new HeightColumn(), new BaseSegmentColumn()), terminal);
        }
    }

    public VIPView(Debugger debugger) {
        this.debugger = debugger;
        windowAttributesTable = new WindowAttributesTable(debugger.getTerminal());
        verticalBoxes = new VerticalBoxes("VIP", Collections.singletonList(windowAttributesTable));
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "VIP";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        for (int i = 0; i < height; i++) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            verticalBoxes.line(asb, i, width, height);
            lines.add(asb.toAttributedString());
        }
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return windowAttributesTable.getKeyMap();
    }

    @Override
    public char getAccelerator() {
        return 'v';
    }
}
