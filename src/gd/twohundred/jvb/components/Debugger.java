package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.Utils;
import gd.twohundred.jvb.Utils.IntArray;
import gd.twohundred.jvb.components.Instructions.AccessWidth;
import gd.twohundred.jvb.components.debug.Breakpoints;
import gd.twohundred.jvb.components.debug.CPUView;
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
    private static final int DISPLAY_REFRESH_RATE_HZ = 2;
    private static final long DISPLAY_REFRESH_PERIOD = CPU.CLOCK_HZ / DISPLAY_REFRESH_RATE_HZ;
    private static final String APP_NAME = "JVB";

    private final Display display;
    private final IntArray memoryBreakpoints;
    private final IntArray execBreakpoints;
    private final Terminal terminal;
    private final Attributes originalAttributes;
    private final Size size;
    private final List<View> views;
    private final List<LogMessage> log;
    private final Map<Component, Level> levels;
    private final BindingReader bindingReader;
    private final KeyMap<Runnable> keyMap;
    private final Terminal.SignalHandler prevWinchHandler;
    private int currentViewIndex;
    private long cyclesDisplay;
    private boolean forceRefresh;
    private VirtualBoy virtualBoy;
    private boolean cursorVisible = true;
    private volatile Runnable inputAction;
    private volatile ControlCommand controlCommand;
    private InputThread inputThread;
    private State state;

    public enum State {
        Running,
        Paused
    }

    public enum ControlCommand {
        Run,
        Step,
        Halt
    }

    public Debugger() throws IOException {
        this.log = new ArrayList<>();
        this.levels = new EnumMap<>(Component.class);
        for (Component c : Component.values()) {
            levels.put(c, Level.Info);
        }
        levels.put(Component.Memory, Level.Warning);
        memoryBreakpoints = new IntArray();
        execBreakpoints = new IntArray();
        state = State.Running;
        this.terminal = TerminalBuilder.terminal();
        this.views = new ArrayList<>();
        this.views.add(new Overview(this));
        this.views.add(new CPUView(this));
        this.views.add(new Breakpoints(this));
        this.views.add(new Logs(log));
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
        this.display = new Display(terminal, true);
        originalAttributes = terminal.enterRawMode();
        prevWinchHandler = terminal.handle(Terminal.Signal.WINCH, this::resize);
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        terminal.puts(InfoCmp.Capability.keypad_xmit);
        display.clear();
        display.reset();
        display.resize(size.getRows(), size.getColumns());
        bindingReader = new BindingReader(terminal.reader());
        keyMap = initKeyMap();
        inputThread = new InputThread();
        inputThread.start();
    }

    private KeyMap<Runnable> initKeyMap() {
        KeyMap<Runnable> keyMap = new KeyMap<>();
        for (int i = 0; i < views.size(); i++) {
            int idx = i;
            Runnable r = () -> {
                currentViewIndex = idx;
            };
            keyMap.bind(r, KeyMap.ctrl(views.get(i).getAccelerator()));
        }
        keyMap.bind(() -> {
            this.virtualBoy.halt();
            if (this.state != State.Running) {
                this.controlCommand = ControlCommand.Halt;
            }
        }, "q");
        keyMap.setAmbiguousTimeout(10);
        return keyMap;
    }

    public void attach(VirtualBoy virtualBoy) {
        this.virtualBoy = virtualBoy;
        virtualBoy.attach(this);
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void onExec(int pc) {
        if (execBreakpoints.contains(pc)) {
            log(Component.Debugger, Level.Info, "Break on PC %#010x", pc);
            onBreak();
        }
    }

    public void onRead(int address, AccessWidth width) {

    }

    public void onWrite(int address, int value, AccessWidth width) {

    }

    private void onBreak() {
        state = State.Paused;
        refresh();
        while (controlCommand == null) {
            inputTick();
            if (forceRefresh) {
                refresh();
                forceRefresh = false;
            }
        }
    }

    private void resize(Terminal.Signal signal) {
        size.copy(terminal.getSize());
        display.clear();
        display.resize(size.getRows(), size.getColumns());
        markRefreshNeeded();
    }

    public void refresh() {
        View currentView = getCurrentView();
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
                header.append(' ');
                int idx = title.indexOf(v.getAccelerator());
                if (idx < 0) {
                    idx = title.indexOf(Character.toUpperCase(v.getAccelerator()));
                }
                if (idx < 0) {
                    header.append(title);
                } else {
                    header.append(title.substring(0, idx));
                    header.append(title.substring(idx, idx + 1), AttributedStyle.DEFAULT.underline());
                    header.append(title.substring(idx + 1));
                }
                header.append(' ');
            }
        }
        int rightPad = size.getColumns() - header.length();
        for (int i = 0; i < rightPad; i++) {
            header.append('─');
        }
        lines.add(header.toAttributedString());
        currentView.appendLines(lines, size.getColumns(), size.getRows() - 2);
        while (lines.size() < size.getRows() - 1) {
            lines.add(AttributedString.EMPTY);
        }
        AttributedStringBuilder footer = new AttributedStringBuilder();
        String stateString = state.name().toUpperCase();
        int leftPad = size.getColumns() - 3 - APP_NAME.length() - VirtualBoy.VERSION.length() - stateString.length() - 1;
        if (leftPad < 0) {
            for (int i = 0; i < size.getColumns(); i++) {
                footer.append('─');
            }
        } else {
            footer.append('─');
            footer.append(stateString);
            for (int i = 0; i < leftPad; i++) {
                footer.append('─');
            }
            footer.append(' ');
            footer.append(APP_NAME);
            footer.append(' ');
            footer.append(VirtualBoy.VERSION);
            footer.append('─');
        }
        lines.add(footer.toAttributedString());
        int targetCursorPos;
        Cursor viewCursor = currentView.getCursorPosition(size);
        if (viewCursor != null) {
            if (!cursorVisible) {
                cursorVisible = true;
                terminal.puts(InfoCmp.Capability.cursor_normal);
            }
            targetCursorPos = size.cursorPos(viewCursor.getY() + 1, viewCursor.getX());
        } else {
            targetCursorPos = 0;
            if (cursorVisible) {
                cursorVisible = false;
                terminal.puts(InfoCmp.Capability.cursor_invisible);
            }
        }
        display.update(lines, targetCursorPos);
        terminal.flush();
    }

    private View getCurrentView() {
        return views.get(currentViewIndex);
    }

    @Override
    public void reset() {

    }

    @Override
    public void tickExact(int cycles) {
        inputTick();
        cyclesDisplay += cycles;
        if (forceRefresh || cyclesDisplay >= DISPLAY_REFRESH_PERIOD) {
            refresh();
            forceRefresh = false;
            cyclesDisplay = 0;
        }
    }

    private void inputTick() {
        Runnable runnable = inputAction;
        if (runnable != null) {
            runnable.run();
            inputAction = null;
            synchronized (inputThread) {
                inputThread.notify();
            }
            markRefreshNeeded();
        }
    }

    public void markRefreshNeeded() {
        forceRefresh = true;
    }

    public VirtualBoy getVirtualBoy() {
        return virtualBoy;
    }

    public CartridgeROM getCartridgeRom() {
        return getVirtualBoy().getCpu().getBus().getRom();
    }

    public CPU getCpu() {
        return getVirtualBoy().getCpu();
    }

    public int getPc() {
        return getCpu().getPc();
    }

    public Bus getBus() {
        return getCpu().getBus();
    }

    public void addExecBreakPoint(int pc) {
        execBreakpoints.add(pc);
    }

    private class InputThread extends Thread {
        InputThread() {
            super("InputThread");
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                while (inputAction != null) {
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                inputAction = bindingReader.readBinding(keyMap, getCurrentView().getKeyMap());
            }
        }
    }

    public void exit() {
        terminal.puts(InfoCmp.Capability.cursor_normal);
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        terminal.puts(InfoCmp.Capability.keypad_local);
        terminal.flush();
        terminal.setAttributes(originalAttributes);
        terminal.handle(Terminal.Signal.WINCH, prevWinchHandler);
    }

    @Override
    public void log(Component component, Level level, String format, Object... args) {
        if (isLevelEnabled(component, level)) {
            log.add(new LogMessage(level, component, String.format(format, args)));
            markRefreshNeeded();
        }
    }

    @Override
    public boolean isLevelEnabled(Component component, Level level) {
        return levels.get(component).ordinal() >= level.ordinal();
    }
}
