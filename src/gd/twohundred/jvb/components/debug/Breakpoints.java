package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.cpu.Instructions;
import gd.twohundred.jvb.components.debug.boxes.Table;
import gd.twohundred.jvb.components.debug.boxes.Table.Column;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Breakpoints implements View {

    private final Debugger debugger;
    private final List<Breakpoint> execBreakpoints;
    private final List<MemBreakpoint> memBreakpoints;
    private final VerticalBoxes verticalBoxes;
    private final ExecBreakpointsTable execBreakpointsTable;
    private final MemBreakpointsTable memBreakpointsTable;
    private volatile State state;
    private Table selectedTable;

    private final KeyMap<Runnable> addingKeyMap;
    private final AddressField addressField;
    private boolean addingExec = true;
    private boolean addingRead;
    private boolean addingWrite;

    public static class AddressField {
        private static final String ADDRESS_PROMPT = "Address: ";
        private static final int ADDRESS_PROMPT_LEN = ADDRESS_PROMPT.length();
        private static final char DEL = '\177';

        private final StringBuffer addressBuffer;
        private volatile int addressCursor;

        public AddressField(KeyMap<Runnable> keyMap, Terminal terminal) {
            addressBuffer = new StringBuffer();
            for (char i = '0'; i <= '9'; i++) {
                keyMap.bind(insertAddressChar(i), Character.toString(i));
            }
            for (char i = 'A'; i <= 'F'; i++) {
                keyMap.bind(insertAddressChar(Character.toLowerCase(i)), Character.toString(i));
            }
            for (char i = 'a'; i <= 'f'; i++) {
                keyMap.bind(insertAddressChar(i), Character.toString(i));
            }
            keyMap.bind(insertAddressChar(DEL), KeyMap.key(terminal, InfoCmp.Capability.key_dc));
            // this should be KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_backspace) but jline does not support it
            keyMap.bind(insertAddressChar('\b'), "\177");
            keyMap.bind(this::moveCursorRight, KeyMap.key(terminal, InfoCmp.Capability.key_right));
            keyMap.bind(this::moveCursorLeft, KeyMap.key(terminal, InfoCmp.Capability.key_left));
        }

        public int getCursorX() {
            return 1 + ADDRESS_PROMPT_LEN + addressCursor;
        }

        public String getAddressPrompt() {
            return ADDRESS_PROMPT;
        }

        public String getCurrentInput() {
            return addressBuffer.toString();
        }

        public int getCurrentValue() {
            return (int) Long.parseLong(addressBuffer.toString(), 16);
        }

        public boolean isCurrentInputValid() {
            int length = addressBuffer.length();
            return length <= 8 && length > 0;
        }

        private Runnable insertAddressChar(char c) {
            return () -> {
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
            if (addressCursor < addressBuffer.length()) {
                addressCursor++;
            }
        }

        private void moveCursorLeft() {
            if (addressCursor > 0) {
                addressCursor--;
            }
        }

        public void reset() {
            addressBuffer.setLength(0);
            addressCursor = 0;
        }
    }

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
            this.read = read;
            this.write = write;
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
        private final String name;
        private final int width;

        private BreakpointColumn(BreakpointType type, String name, int width) {
            this.type = type;
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
            List<? extends Breakpoint> breakpoints = type.get(Breakpoints.this);
            if (row >= breakpoints.size()) {
                return;
            }
            cell(asb, breakpoints.get(row));
        }

        protected abstract void cell(AttributedStringBuilder asb, Breakpoint breakpoint);
    }

    private class EnabledColumn extends BreakpointColumn {
        private EnabledColumn(BreakpointType type) {
            super(type, "E", 1);
        }

        @Override
        public void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {
            asb.append(breakpoint.isEnabled() ? '✔' : ' ');
        }
    }

    private class AddressColumn extends BreakpointColumn {
        private AddressColumn(BreakpointType type) {
            super(type, "Address", 8);
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

    private class HitsColumn extends BreakpointColumn {
        private HitsColumn(BreakpointType type) {
            super(type, "Hits", 5);
        }

        @Override
        public void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {
            String str = Integer.toString(breakpoint.hitCount);
            View.repeat(asb, minWidth() - str.length(), ' ');
            asb.append(str);
        }
    }

    private class CommentsColumn extends BreakpointColumn {
        private CommentsColumn(BreakpointType type) {
            super(type, "Comments", 10);
        }

        @Override
        public boolean fixedWidth() {
            return false;
        }

        @Override
        protected void cell(AttributedStringBuilder asb, Breakpoint breakpoint) {
            asb.append(breakpoint.getComment());
        }
    }

    private class ModesColumn implements Column {
        @Override
        public String name() {
            return "Mode";
        }

        @Override
        public int minWidth() {
            return 4;
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public void cell(AttributedStringBuilder asb, int row, int width) {
            if (row >= memBreakpoints.size()) {
                return;
            }
            MemBreakpoint memBreakpoint = memBreakpoints.get(row);
            if (memBreakpoint.isRead()) {
                asb.append('R');
            }
            if (memBreakpoint.isWrite()) {
                asb.append('W');
            }
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
        protected ExecBreakpointsTable(Terminal terminal) {
            super("Execute", Arrays.asList(new EnabledColumn(BreakpointType.Execute), new AddressColumn(BreakpointType.Execute), new HitsColumn(BreakpointType.Execute), new CommentsColumn(BreakpointType.Execute)), terminal);
        }
    }

    private class MemBreakpointsTable extends Table {
        protected MemBreakpointsTable(Terminal terminal) {
            super("Memory", Arrays.asList(new EnabledColumn(BreakpointType.Memory), new AddressColumn(BreakpointType.Memory), new ModesColumn(), new HitsColumn(BreakpointType.Memory), new CommentsColumn(BreakpointType.Memory)), terminal);
        }
    }

    public Breakpoints(Debugger debugger) {
        this.debugger = debugger;
        this.state = State.Idle;
        execBreakpoints = new ArrayList<>();
        memBreakpoints = new ArrayList<>();
        execBreakpointsTable = new ExecBreakpointsTable(debugger.getTerminal());
        memBreakpointsTable = new MemBreakpointsTable(debugger.getTerminal());
        selectedTable = execBreakpointsTable;
        selectedTable.setActive(true);
        addIdleKeyBindings(execBreakpointsTable.getKeyMap());
        addIdleKeyBindings(memBreakpointsTable.getKeyMap());
        addTableKeyBindings(execBreakpointsTable, execBreakpoints, debugger.getTerminal());
        addTableKeyBindings(memBreakpointsTable, memBreakpoints, debugger.getTerminal());
        addingKeyMap = new KeyMap<>();
        addingKeyMap.bind(this::finishAddBreakpoint, "\r");
        addingKeyMap.bind(this::abortAddBreakpoint, "q");
        addingKeyMap.bind(() -> this.addingExec = !this.addingExec, "x");
        addingKeyMap.bind(() -> this.addingRead = !this.addingRead, "r");
        addingKeyMap.bind(() -> this.addingWrite = !this.addingWrite, "w");
        addressField = new AddressField(addingKeyMap, debugger.getTerminal());
        verticalBoxes = new VerticalBoxes("Breakpoints", Arrays.asList(execBreakpointsTable, memBreakpointsTable));
    }

    private void addTableKeyBindings(Table table, List<? extends Breakpoint> list, Terminal terminal) {
        table.bind((index) -> {
            if (index < list.size()) {
                list.remove(index);
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_dc));
        table.bind(i -> {
            if (i < list.size()) {
                Breakpoint breakpoint = list.get(i);
                breakpoint.setEnabled(!breakpoint.isEnabled());
            }
        }, "e");
    }

    private void addIdleKeyBindings(KeyMap<Runnable> keyMap) {
        keyMap.bind(this::startAddBreakpoint, "a");
        keyMap.bind(() -> {
            selectedTable = execBreakpointsTable;
            selectedTable.setActive(true);
            memBreakpointsTable.setActive(false);
        }, KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_ppage));
        keyMap.bind(() -> {
            selectedTable = memBreakpointsTable;
            selectedTable.setActive(true);
            execBreakpointsTable.setActive(false);
        }, KeyMap.key(debugger.getTerminal(), InfoCmp.Capability.key_npage));
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        switch (state) {
            case Idle:
                return null;
            case Adding:
                return new Cursor(addressField.getCursorX(), 0);
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
            addrPromptLine.append(addressField.getAddressPrompt());
            AttributedStyle style = AttributedStyle.DEFAULT;
            if (!addressField.isCurrentInputValid()) {
                style = style.foreground(AttributedStyle.RED);
            }
            addrPromptLine.append(addressField.getCurrentInput(), style);
            View.padToLength(addrPromptLine, width - 1);
            addrPromptLine.append('│');
            lines.add(addrPromptLine.toAttributedString());

            AttributedStringBuilder modeLine = new AttributedStringBuilder();
            modeLine.append('│');
            if (addingExec) {
                modeLine.style(AttributedStyle.INVERSE);
            }
            modeLine.append('E');
            modeLine.append("X", AttributedStyle.DEFAULT.underline());
            modeLine.append("EC");
            modeLine.style(AttributedStyle.INVERSE_OFF);
            modeLine.append(' ');
            if (addingRead) {
                modeLine.style(AttributedStyle.INVERSE);
            }
            modeLine.append("R", AttributedStyle.DEFAULT.underline());
            modeLine.append("EAD");
            modeLine.style(AttributedStyle.INVERSE_OFF);
            modeLine.append(' ');
            if (addingWrite) {
                modeLine.style(AttributedStyle.INVERSE);
            }
            modeLine.append("W", AttributedStyle.DEFAULT.underline());
            modeLine.append("RITE");
            modeLine.style(AttributedStyle.INVERSE_OFF);
            View.padToLength(modeLine, width - 1);
            modeLine.append('│');
            lines.add(modeLine.toAttributedString());

            AttributedStringBuilder addrPromptBottomLine = new AttributedStringBuilder();
            addrPromptBottomLine.append('└');
            View.horizontalLine(addrPromptBottomLine, width - 2);
            addrPromptBottomLine.append('┘');
            lines.add(addrPromptBottomLine.toAttributedString());

            AttributedStringBuilder actionsLine = new AttributedStringBuilder();
            actionsLine.append(" └Abort(");
            actionsLine.append("q", AttributedStyle.DEFAULT.underline());
            actionsLine.append(")┘ └Accept(");
            actionsLine.append("⏎", addressField.isCurrentInputValid() ? AttributedStyle.DEFAULT.underline() : AttributedStyle.DEFAULT);
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
        verticalBoxes.appendLines(lines, width, remainingHeight);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        switch (state) {
            case Idle:
                return selectedTable.getKeyMap();
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
        addressField.reset();
        state = State.Adding;
        debugger.log(Logger.Component.Debugger, Logger.Level.Debug, "Starting bp add..");
        debugger.markRefreshNeeded();
    }

    private void finishAddBreakpoint() {
        if (!addressField.isCurrentInputValid()) {
            return;
        }
        state = State.Idle;
        int addr = addressField.getCurrentValue();
        debugger.log(Logger.Component.Debugger, Logger.Level.Info, "Adding bp at %#010x", addr);
        if (addingExec) {
            execBreakpoints.add(new Breakpoint(addr, ""));
        }
        if (addingRead || addingWrite) {
            memBreakpoints.add(new MemBreakpoint(addr, "", addingRead, addingWrite));
        }
        debugger.markRefreshNeeded();
    }

    private void abortAddBreakpoint() {
        state = State.Idle;
        debugger.log(Logger.Component.Debugger, Logger.Level.Debug, "Abort add bp");
        debugger.markRefreshNeeded();
    }

    public boolean shouldBreakOnExec(int pc) {
        for (Breakpoint b : execBreakpoints) {
            if (b.getAddress() == pc && b.isEnabled()) {
                b.incrementHistCount();
                return true;
            }
        }
        return false;
    }

    public boolean shouldBreakOnRead(int address, Instructions.AccessWidth width) {
        for (MemBreakpoint b : memBreakpoints) {
            if (b.getAddress() == address && b.isEnabled() && b.isRead()) {
                b.incrementHistCount();
                return true;
            }
        }
        return false;
    }

    public boolean shouldBreakOnWrite(int address, int value, Instructions.AccessWidth width) {
        for (MemBreakpoint b : memBreakpoints) {
            if (b.getAddress() == address && b.isEnabled() && b.isWrite()) {
                b.incrementHistCount();
                return true;
            }
        }
        return false;
    }
}
