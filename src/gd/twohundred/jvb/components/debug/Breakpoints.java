package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.Instructions;
import gd.twohundred.jvb.components.debug.boxes.Table;
import gd.twohundred.jvb.components.debug.boxes.Table.Column;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Breakpoints implements View {
    private static final String ADDRESS_PROMPT = "Address: ";
    private static final int ADDRESS_PROMPT_LEN = ADDRESS_PROMPT.length();
    private static final char DEL = '\177';
    private final KeyMap<Runnable> idleKeyMap;
    private final KeyMap<Runnable> addingKeyMap;
    private final Debugger debugger;
    private final StringBuffer addressBuffer;
    private volatile State state;
    private volatile int addressCursor;

    private final List<Breakpoint> execBreakpoints;
    private final List<MemBreakpoint> memBreakpoints;
    private final VerticalBoxes verticalBoxes;

    private static class Breakpoint {
        private final int address;
        private final String comment;
        private boolean enabled;
        private int hitCount;

        protected Breakpoint(int address, String comment) {
            this.address = address;
            this.comment = comment;
            this.enabled = true;
        }

        public int getAddress() {
            return address;
        }

        public String getComment() {
            return comment;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void incrementHistCount() {
            hitCount++;
        }

        public int getHitCount() {
            return hitCount;
        }
    }

    private static class MemBreakpoint extends Breakpoint {
        private boolean read;
        private boolean write;
        private Integer value;

        protected MemBreakpoint(int address, String comment, boolean read, boolean write) {
            super(address, comment);
        }

        public boolean isRead() {
            return read;
        }

        public boolean isWrite() {
            return write;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        public void setWrite(boolean write) {
            this.write = write;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    private enum State {
        Idle,
        Adding
    }

    private abstract class BreakpointColumn implements Column {
        private final BreakpointType type;

        private BreakpointColumn(BreakpointType type) {
            this.type = type;
        }

        @Override
        public void cell(AttributedStringBuilder asb, int row, int width) {
            List<? extends Breakpoint> breakpoints = type.get(Breakpoints.this);
            if (row >= breakpoints.size()) {
                return;
            }
            cell(asb, breakpoints.get(row));
        }

        protected void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {

        }
    }

    private class EnabledColumn extends BreakpointColumn {

        private EnabledColumn(BreakpointType type) {
            super(type);
        }

        @Override
        public String name() {
            return "E";
        }

        @Override
        public int minWidth() {
            return 1;
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {
            asb.append(breakpoint.isEnabled() ? '✔' : ' ');
        }
    }

    private class AddressColumn extends BreakpointColumn {

        private AddressColumn(BreakpointType type) {
            super(type);
        }

        @Override
        public String name() {
            return "Address";
        }

        @Override
        public int minWidth() {
            return 8;
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {
            String hexString = Integer.toHexString(breakpoint.getAddress());
            int padding = 8 - hexString.length();
            for (int i = 0; i < padding; i++) {
                asb.append('0');
            }
            asb.append(hexString);
        }
    }

    private enum BreakpointType {
        Execute,
        Memory;

        public List<? extends Breakpoint> get(Breakpoints bps) {
            switch (this) {
                case Execute:
                    return bps.execBreakpoints;
                case Memory:
                    return bps.memBreakpoints;
            }
            throw new RuntimeException("should not reach here");
        }
    }

    private class ExecBreakpointsTable extends Table {
        protected ExecBreakpointsTable() {
            super("Execute", Arrays.asList(new EnabledColumn(BreakpointType.Execute), new AddressColumn(BreakpointType.Execute)));
        }
    }

    private class MemBreakpointsTable extends Table {
        protected MemBreakpointsTable() {
            super("Memory", Arrays.asList(new EnabledColumn(BreakpointType.Memory), new AddressColumn(BreakpointType.Memory)));
        }
    }

    public Breakpoints(Debugger debugger) {
        this.debugger = debugger;
        this.state = State.Idle;
        addressBuffer = new StringBuffer();
        idleKeyMap = new KeyMap<>();
        idleKeyMap.bind(this::startAddBreakpoint, "a");
        addingKeyMap = new KeyMap<>();
        addingKeyMap.bind(this::finishAddBreakpoint, "\r");
        for (char i = '0'; i <= '9'; i++) {
            addingKeyMap.bind(insertAddressChar(i), Character.toString(i));
        }
        for (char i = 'A'; i <= 'F'; i++) {
            addingKeyMap.bind(insertAddressChar(Character.toLowerCase(i)), Character.toString(i));
        }
        for (char i = 'a'; i <= 'f'; i++) {
            addingKeyMap.bind(insertAddressChar(i), Character.toString(i));
        }
        addingKeyMap.bind(insertAddressChar(DEL), KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_dc));
        // this should be KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_backspace) but jline does not support it
        addingKeyMap.bind(insertAddressChar('\b'), "\177");
        addingKeyMap.bind(this::moveCursorRight, KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_right));
        addingKeyMap.bind(this::moveCursorLeft, KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_left));
        addingKeyMap.bind(this::abortAddBreakpoint, "q");
        execBreakpoints = new ArrayList<>();
        memBreakpoints = new ArrayList<>();
        verticalBoxes = new VerticalBoxes("Breakpoints", Arrays.asList(new ExecBreakpointsTable(), new MemBreakpointsTable()));
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        switch (state) {
            case Idle:
                return null;
            case Adding:
                return new Cursor(1 + ADDRESS_PROMPT_LEN + addressCursor, 0);
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    @Override
    public String getTitle() {
        return "Breakpoints";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        int startLines = lines.size();
        if (state == State.Adding) {
            AttributedStringBuilder addrPromptLine = new AttributedStringBuilder();
            addrPromptLine.append('│');
            addrPromptLine.append(ADDRESS_PROMPT);
            AttributedStyle style = AttributedStyle.DEFAULT;
            if (!validateAddress()) {
                style = style.foreground(AttributedStyle.RED);
            }
            addrPromptLine.append(addressBuffer.toString(), style);
            View.repeat(addrPromptLine, width - addrPromptLine.length() - 1, ' ');
            addrPromptLine.append('│');
            lines.add(addrPromptLine.toAttributedString());
            AttributedStringBuilder addrPromptBottomLine = new AttributedStringBuilder();
            addrPromptBottomLine.append('└');
            View.horizontalLine(addrPromptBottomLine, width - 2);
            addrPromptBottomLine.append('┘');
            lines.add(addrPromptBottomLine.toAttributedString());
            AttributedStringBuilder actionsLine = new AttributedStringBuilder();
            actionsLine.append(" └Abort(");
            actionsLine.append("q", AttributedStyle.DEFAULT.underline());
            actionsLine.append(")┘ └Accept(");
            actionsLine.append("⏎", validateAddress() ? AttributedStyle.DEFAULT.underline() : AttributedStyle.DEFAULT);
            actionsLine.append(")┘");
            lines.add(actionsLine.toAttributedString());
        } else {
            AttributedStringBuilder addLine = new AttributedStringBuilder();
            addLine.append(" └");
            addLine.append("A", AttributedStyle.DEFAULT.underline());
            addLine.append("dd┘");
            lines.add(addLine.toAttributedString());
        }
        int remainingHeight = height - (lines.size() - startLines);
        for (int i = 0; i < remainingHeight; i++) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            verticalBoxes.line(asb, i, width, remainingHeight);
            lines.add(asb.toAttributedString());
        }
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        switch (state) {
            case Idle:
                return idleKeyMap;
            case Adding:
                return addingKeyMap;
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    @Override
    public char getAccelerator() {
        return 'k';
    }

    private void startAddBreakpoint() {
        addressBuffer.setLength(0);
        addressCursor = 0;
        state = State.Adding;
        debugger.log(Logger.Component.Debugger, Logger.Level.Debug, "Starting bp add..");
        debugger.markRefreshNeeded();
    }

    private boolean validateAddress() {
        int length = addressBuffer.length();
        return length <= 8 && length > 0;
    }

    private void finishAddBreakpoint() {
        if (!validateAddress()) {
            return;
        }
        state = State.Idle;
        int addr = Integer.valueOf(addressBuffer.toString(), 16);
        debugger.log(Logger.Component.Debugger, Logger.Level.Info, "Adding bp at %#010x", addr);
        execBreakpoints.add(new Breakpoint(addr, ""));
        debugger.markRefreshNeeded();
    }

    private void abortAddBreakpoint() {
        state = State.Idle;
        debugger.log(Logger.Component.Debugger, Logger.Level.Debug, "Abort add bp");
        debugger.markRefreshNeeded();
    }

    private Runnable insertAddressChar(char c) {
        return () -> {
            assert state == State.Adding;
            assert addressCursor <= addressBuffer.length();
            if (c == DEL) {
                if (addressCursor < addressBuffer.length()) {
                    addressBuffer.deleteCharAt(addressCursor);
                }
            } else if (c == '\b') {
                if (addressCursor > 0) {
                    addressBuffer.deleteCharAt(addressCursor - 1);
                    addressCursor--;
                }
            } else {
                addressBuffer.insert(addressCursor, c);
                addressCursor++;
            }
        };
    }

    private void moveCursorRight() {
        assert state == State.Adding;
        if (addressCursor < addressBuffer.length()) {
            addressCursor++;
        }
    }

    private void moveCursorLeft() {
        assert state == State.Adding;
        if (addressCursor > 0) {
            addressCursor--;
        }

    }

    public boolean shouldBreakOnExec(int pc) {
        return execBreakpoints.stream().anyMatch(b -> b.getAddress() == pc && b.isEnabled());
    }

    public boolean shouldBreakOnRead(int address, Instructions.AccessWidth width) {
        return memBreakpoints.stream().anyMatch(b -> b.getAddress() == address && b.isEnabled() && b.isRead());
    }

    public boolean shouldBreakOnWrite(int address, int value, Instructions.AccessWidth width) {
        return memBreakpoints.stream().anyMatch(b -> b.getAddress() == address && b.isEnabled() && b.isWrite());
    }
}
