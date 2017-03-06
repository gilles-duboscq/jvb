package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.Debugger;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

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

    private enum State {
        Idle,
        Adding
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
            View.horizontalLine(addrPromptLine, width - 2);
            addrPromptBottomLine.append('┘');
            lines.add(addrPromptBottomLine.toAttributedString());
        }
        AttributedStringBuilder addLine = new AttributedStringBuilder();
        addLine.append(" └");
        addLine.append("A", AttributedStyle.DEFAULT.underline());
        addLine.append("dd┘");
        lines.add(addLine.toAttributedString());
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
        return addressBuffer.length() <= 8;
    }

    private void finishAddBreakpoint() {
        if (!validateAddress()) {
            return;
        }
        state = State.Idle;
        int addr = Integer.valueOf(addressBuffer.toString(), 16);
        debugger.log(Logger.Component.Debugger, Logger.Level.Info, "Adding bp at %#010x", addr);
        debugger.addExecBreakPoint(addr);
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
}
