package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.TimeoutBindingReader;
import gd.twohundred.jvb.components.Instructions.AccessWidth;
import gd.twohundred.jvb.components.debug.LogMessage;
import gd.twohundred.jvb.components.debug.Logs;
import gd.twohundred.jvb.components.debug.Overview;
import gd.twohundred.jvb.components.debug.View;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Debugger implements ExactlyEmulable, Logger {
    private static final int REFRESH_RATE_HZ = 2;
    private static final long REFRESH_PERIOD = CPU.CLOCK_HZ / REFRESH_RATE_HZ;
    private static final long KEY_REFRESH_PERIOD = CPU.CLOCK_HZ / REFRESH_RATE_HZ;
    private static final AttributedString EMPTY_LINE = new AttributedString("");
    private static final String APP_NAME = "JVB";

    private final Display display;
    private final int[] memoryBreakpoints = new int[2];
    private final Terminal terminal;
    private final Attributes originalAttributes;
    private final Size size;
    private final List<View> views;
    private final List<LogMessage> log;
    private final Map<Component, Level> levels;
    private final BindingReader bindingReader;
    private final KeyMap<Runnable> keyMap;
    private int currentViewIndex;
    private long cycles;
    private VirtualBoy virtualBoy;
    private boolean cursorVisible = true;
    private boolean supportsCursorVisibility;

    public Debugger() throws IOException {
        this.log = new ArrayList<>();
        this.levels = new EnumMap<>(Component.class);
        for (Component c : Component.values()) {
            levels.put(c, Level.Warning);
        }
        this.views = new ArrayList<>();
        this.views.add(new Overview());
        this.views.add(new Logs(log));
        this.terminal = TerminalBuilder.terminal();
        this.size = new Size();
        size.copy(terminal.getSize());
        if (size.getRows() == 0) {
            throw new RuntimeException("0-row term");
        }
        if (size.getColumns() == 0) {
            Integer columns = terminal.getNumericCapability(InfoCmp.Capability.columns);
            if (columns != null) {
                size.setColumns(columns);
            }
        }
        supportsCursorVisibility = terminal.getBooleanCapability(InfoCmp.Capability.cursor_visible) && terminal.getBooleanCapability(InfoCmp.Capability.cursor_invisible);
        this.display = new Display(terminal, true);
        originalAttributes = terminal.enterRawMode();
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        display.clear();
        display.reset();
        display.resize(size.getRows(), size.getColumns());
        bindingReader = new TimeoutBindingReader(terminal.reader(), 10);
        keyMap = initKeyMap();
        currentViewIndex = 1;
    }

    private KeyMap<Runnable> initKeyMap() {
        KeyMap<Runnable> keyMap = new KeyMap<>();
        for (int i = 0; i < views.size(); i++) {
            int idx = i;
            Runnable r = () -> {
                currentViewIndex = idx;
            };
            keyMap.bind(r, KeyMap.alt((char) ('0' + i)));
        }
        return keyMap;
    }

    public void attach(VirtualBoy virtualBoy) {
        this.virtualBoy = virtualBoy;
        virtualBoy.attach(this);
    }

    public void onExec(int pc) {

    }

    public void onRead(int address, AccessWidth width) {

    }

    public void onWrite(int address, int value, AccessWidth width) {

    }

    public void refresh() {
        View currentView = views.get(currentViewIndex);
        List<AttributedString> lines = new ArrayList<>();
        AttributedStringBuilder header = new AttributedStringBuilder();
        for (View v : views) {
            String title = v.getTitle();
            int len = title.length() + 2;
            if (len + header.length() + 1 > size.getColumns()) {
                header.append('…');
                break;
            }
            if (v == currentView) {
                header.append('[');
                header.append(title, AttributedStyle.BOLD);
                header.append(']');
            } else {
                header.append('-');
                header.append(title);
                header.append('-');
            }
        }
        lines.add(header.toAttributedString());
        currentView.appendLines(lines, size.getRows() - 2);
        while (lines.size() < size.getRows() - 1) {
            lines.add(EMPTY_LINE);
        }
        AttributedStringBuilder footer = new AttributedStringBuilder();
        int leftPad = size.getColumns() - 2 - APP_NAME.length() - VirtualBoy.VERSION.length();
        if (leftPad < 0) {
            for (int i = 0; i < size.getColumns(); i++) {
                footer.append('-');
            }
        } else {
            for (int i = 0; i < leftPad; i++) {
                footer.append('-');
            }
            footer.append(APP_NAME);
            footer.append('-');
            footer.append(VirtualBoy.VERSION);
            footer.append('-');
        }
        lines.add(footer.toAttributedString());
        int targetCursorPos;
        Cursor viewCursor = currentView.getCursorPosition();
        if (viewCursor != null) {
            if (!cursorVisible && supportsCursorVisibility) {
                terminal.puts(InfoCmp.Capability.cursor_normal);
            }
            targetCursorPos = size.cursorPos(viewCursor.getY() + 1, viewCursor.getX());
        } else {
            targetCursorPos = -1;
            if (cursorVisible && supportsCursorVisibility) {
                terminal.puts(InfoCmp.Capability.cursor_invisible);
            }
        }
        display.update(lines, targetCursorPos);
        terminal.flush();
    }

    @Override
    public void reset() {

    }

    @Override
    public void tickExact(int cycles) {
        this.cycles += cycles;
        if (this.cycles >= REFRESH_PERIOD) {
            refresh();
            this.cycles = 0;
        }
    }

    public void exit() {
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        terminal.flush();
        terminal.setAttributes(originalAttributes);
    }

    @Override
    public void log(Component component, Level level, String format, Object... args) {
        if (isLevelEnabled(component, level)) {
            log.add(new LogMessage(level, component, String.format(format, args)));
        }
    }

    @Override
    public boolean isLevelEnabled(Component component, Level level) {
        return levels.get(component).ordinal() >= level.ordinal();
    }
}
