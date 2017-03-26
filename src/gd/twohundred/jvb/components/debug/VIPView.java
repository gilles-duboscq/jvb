package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.debug.boxes.Box;
import gd.twohundred.jvb.components.debug.boxes.Table;
import gd.twohundred.jvb.components.debug.boxes.Table.Column;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import gd.twohundred.jvb.components.vip.VIPControlRegisters;
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

    private class ModeColumn extends WindowAttributesColumn {
        protected ModeColumn() {
            super("Mode", 4);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            asb.append(attributes.getMode().getShortName());
        }
    }

    private class ParamIndexColumn extends WindowAttributesColumn {
        protected ParamIndexColumn() {
            super("Param", 5);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, WindowAttributes attributes) {
            View.leftPadHex(asb, minWidth(), attributes.getParameterIndex());
        }
    }

    private class WindowAttributesTable extends Table {
        private WindowAttributesTable(Terminal terminal) {
            super("Windows", Arrays.asList(new IDColumn(), new StopColumn(), new ModeColumn(), new XColumn(), new YColumn(), new WidthColumn(), new HeightColumn(), new BaseSegmentColumn(), new ParamIndexColumn()), terminal);
        }
    }

    private static class FrameProgress implements Box {
        private final Debugger debugger;

        private FrameProgress(Debugger debugger) {
            this.debugger = debugger;
        }

        @Override
        public String name() {
            return "Frame progress";
        }

        @Override
        public int minWidth() {
            return 0;
        }

        @Override
        public boolean fixedWidth() {
            return false;
        }

        @Override
        public int minHeight() {
            return 1;
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            int done = (int) (debugger.getVip().getDisplayCycles() * width / VirtualImageProcessor.FRAME_PERIOD);
            View.repeat(asb, done, '█');
        }
    }

    private static class VIPstatus implements Box {
        private final Debugger debugger;

        private VIPstatus(Debugger debugger) {
            this.debugger = debugger;
        }

        @Override
        public String name() {
            return "Status";
        }

        @Override
        public int minWidth() {
            return 0;
        }

        @Override
        public boolean fixedWidth() {
            return false;
        }

        @Override
        public int minHeight() {
            return 2;
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            VirtualImageProcessor vip = debugger.getVip();
            switch (line) {
                case 0:
                    VIPControlRegisters controlRegs = vip.getControlRegs();
                    asb.append("Display: ").append(controlRegs.isDisplayEnabled() ? "enabled" : "disabled");
                    asb.append(" Drawing: ").append(controlRegs.isDrawingEnabled() ? "enabled" : "disabled");
                    break;
                case 1:
                    asb.append("Display state: ").append(vip.getDisplayState().toString());
                    asb.append(" Drawing state: ").append(vip.getDrawingState().toString());
                    if (vip.getDrawingState() == VirtualImageProcessor.DrawingState.Drawing) {
                        asb.append(" Y block: ").append(Integer.toString(vip.getControlRegs().getCurrentYBlock()));
                        asb.append(" Window: ").append(Integer.toString(vip.getCurrentWindow().getId()));
                    }
                    break;
            }
        }
    }

    public VIPView(Debugger debugger) {
        this.debugger = debugger;
        windowAttributesTable = new WindowAttributesTable(debugger.getTerminal());
        verticalBoxes = new VerticalBoxes("VIP", Arrays.asList(new VIPstatus(debugger), new FrameProgress(debugger), windowAttributesTable));
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
