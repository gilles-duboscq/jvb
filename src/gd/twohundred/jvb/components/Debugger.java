package gd.twohundred.jvb.components;

import gd.twohundred.jvb.LevelLogger;
import gd.twohundred.jvb.Utils;
import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.components.cpu.Instructions.AccessWidth;
import gd.twohundred.jvb.components.debug.Breakpoints;
import gd.twohundred.jvb.components.debug.CPUView;
import gd.twohundred.jvb.components.debug.LogMessage;
import gd.twohundred.jvb.components.debug.Logs;
import gd.twohundred.jvb.components.debug.MemoryView;
import gd.twohundred.jvb.components.debug.OthersView;
import gd.twohundred.jvb.components.debug.Overview;
import gd.twohundred.jvb.components.debug.VIPView;
import gd.twohundred.jvb.components.debug.VSUView;
import gd.twohundred.jvb.components.debug.View;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vip.WindowAttributes;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;
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
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Debugger implements ExactlyEmulable, LevelLogger {
    private static final int DISPLAY_REFRESH_RATE_HZ = 4;
    private static final long DISPLAY_REFRESH_PERIOD = CPU.CLOCK_HZ / DISPLAY_REFRESH_RATE_HZ;
    private static final String APP_NAME = "JVB";

    private final Display display;
    private final Terminal terminal;
    private final Attributes originalAttributes;
    private final Size size;
    private final List<View> views;
    private final List<LogMessage> log;
    private final Map<Component, Level> levels;
    private final BindingReader bindingReader;
    private final KeyMap<Runnable> keyMap;
    private final Terminal.SignalHandler prevWinchHandler;
    private final Breakpoints breakpoints;
    private int currentViewIndex;
    private long cyclesDisplay;
    private boolean forceRefresh;
    private VirtualBoy virtualBoy;
    private boolean cursorVisible = true;
    private volatile Runnable inputAction;
    private volatile State state;
    private final InputThread inputThread;
    private boolean displaying;
    private final TicksStats ticksStats;
    private final TraceBuffer traceBuffer;
    private long totalCycles;

    public enum State {
        Running,
        Stepping,
        Paused,
        Halted
    }

    public static class TicksStats {
        private long lastUpdate;
        private long ticksSinceLastUpdate;
        private long cyclesSinceLastUpdate;
        public long lastTickPerSecond;
        public long lastCyclesPerSecond;
        public long lastCycles;

        void update(long cycles) {
            final long UPDATE_RATE = Utils.NANOS_PER_SECOND / DISPLAY_REFRESH_RATE_HZ;
            long currentTime = System.nanoTime();
            ticksSinceLastUpdate += 1;
            cyclesSinceLastUpdate += cycles;
            lastCycles = cycles;
            if (currentTime - lastUpdate > UPDATE_RATE) {
                lastTickPerSecond = ticksSinceLastUpdate * Utils.NANOS_PER_SECOND / (currentTime - lastUpdate);
                lastCyclesPerSecond = cyclesSinceLastUpdate * Utils.NANOS_PER_SECOND / (currentTime - lastUpdate);
                ticksSinceLastUpdate = 0;
                cyclesSinceLastUpdate = 0;
                lastUpdate = currentTime;
            }
        }
    }

    public Debugger() throws IOException {
        this.ticksStats = new TicksStats();
        this.traceBuffer = new TraceBuffer(64);
        this.log = new ArrayList<>();
        this.levels = new EnumMap<>(Component.class);
        for (Component c : Component.values()) {
            levels.put(c, Level.Info);
        }
        levels.put(Component.Memory, Level.Warning);
        state = State.Running;
        this.terminal = TerminalBuilder.terminal();
        this.views = new ArrayList<>();
        this.views.add(new Overview(this));
        this.views.add(new CPUView(this));
        breakpoints = new Breakpoints(this);
        this.views.add(breakpoints);
        this.views.add(new VIPView(this));
        this.views.add(new VSUView(this));
        this.views.add(new MemoryView(this));
        this.views.add(new OthersView(this));
        this.views.add(new Logs(log, terminal, levels));
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

    @Override
    public void setLevel(Component component, Level level) {
        levels.put(component, level);
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
                this.state = State.Halted;
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
        if (breakpoints.shouldBreakOnExec(pc)) {
            log(Component.Debugger, Level.Info, "Break on PC %#010x", pc);
            onBreak();
        } else if (state == State.Paused || state == State.Stepping) {
            onBreak();
        }
    }

    public void postExec(int pc) {
        this.traceBuffer.add(pc);
    }

    public void onRead(int address, AccessWidth width) {
        if (displaying) {
            return;
        }
        if (breakpoints.shouldBreakOnRead(address, width)) {
            log(Component.Debugger, Level.Info, "Break on read %s @ %#010x", width, address);
            onBreak();
        }
    }

    public void onWrite(int address, int value, AccessWidth width) {
        if (displaying) {
            return;
        }
        if (breakpoints.shouldBreakOnWrite(address, value, width)) {
            log(Component.Debugger, Level.Info, "Break on write %s @ %#010x", width, address);
            onBreak();
        }
    }

    private void onBreak() {
        state = State.Paused;
        refresh();
        while (true) {
            inputTick();
            if (forceRefresh) {
                refresh();
                forceRefresh = false;
            }
            switch (state) {
                case Running:
                case Halted:
                case Stepping:
                    return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
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
        displaying = true;
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
        header.style(AttributedStyle.INVERSE);
        View.repeat(header, rightPad, ' ');
        header.style(AttributedStyle.INVERSE_OFF);
        lines.add(header.toAttributedString());
        currentView.appendLines(lines, size.getColumns(), size.getRows() - 2);
        while (lines.size() < size.getRows() - 1) {
            lines.add(AttributedString.EMPTY);
        }
        AttributedStringBuilder footer = new AttributedStringBuilder();
        String stateString = state.name().toUpperCase();
        int leftPad = size.getColumns() - 3 - APP_NAME.length() - VirtualBoy.VERSION.length() - stateString.length() - 4;
        if (leftPad < 0) {
            footer.style(AttributedStyle.INVERSE);
            View.repeat(footer, size.getColumns(), ' ');
            footer.style(AttributedStyle.INVERSE_OFF);
        } else {
            footer.append(" ", AttributedStyle.INVERSE);
            footer.append(' ');
            footer.append(stateString);
            footer.append(' ');
            footer.style(AttributedStyle.INVERSE);
            View.repeat(footer, leftPad, ' ');
            footer.style(AttributedStyle.INVERSE_OFF);
            footer.append(' ');
            footer.append(APP_NAME);
            footer.append(' ');
            footer.append(VirtualBoy.VERSION);
            footer.append(' ');
            footer.append(" ", AttributedStyle.INVERSE);
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
        displaying = false;
    }

    private View getCurrentView() {
        return views.get(currentViewIndex);
    }

    @Override
    public void reset() {
        this.traceBuffer.clear();
    }

    public void macroTick(long cycles) {
        inputTick();
        this.ticksStats.update(cycles);
        cyclesDisplay += cycles;
        if (forceRefresh || cyclesDisplay >= DISPLAY_REFRESH_PERIOD) {
            refresh();
            forceRefresh = false;
            cyclesDisplay = 0;
        }
    }

    @Override
    public void tickExact(long cycles) {
        totalCycles += cycles;
    }

    private void inputTick() {
        Runnable runnable = inputAction;
        if (runnable != null) {
            runnable.run();
            synchronized (inputThread) {
                inputAction = null;
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

    public WindowAttributes[] getWindowAttributes() {
        return getVip().getWindowAttributes();
    }

    public VirtualImageProcessor getVip() {
        return getBus().getVIP();
    }

    public VirtualSoundUnit getVsu() {
        return getBus().getVSU();
    }

    public HardwareTimer getTimer() {
        return getBus().getTimer();
    }

    public GamePad getGamePad() {
        return getBus().getGamePad();
    }

    public State getState() {
        return state;
    }

    public void pause() {
        this.state = State.Paused;
    }

    public void step() {
        this.state = State.Stepping;
    }

    public void continueExecution() {
        this.state = State.Running;
    }

    public TicksStats getTicksStats() {
        return ticksStats;
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
                            if (inputAction != null) {
                                this.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (bindingReader.peekCharacter(100) != NonBlockingReader.READ_EXPIRED) {
                    inputAction = bindingReader.readBinding(keyMap, getCurrentView().getKeyMap(), false);
                }
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
            log.add(new LogMessage(totalCycles, level, component, String.format(format, args)));
            markRefreshNeeded();
        }
    }

    @Override
    public boolean isLevelEnabled(Component component, Level level) {
        return levels.get(component).ordinal() >= level.ordinal();
    }

    public TraceBuffer getTraceBuffer() {
        return traceBuffer;
    }

    public static class TraceBuffer {
        private final int[] trace;
        private final int mask;
        private int size;
        private int pos;

        TraceBuffer(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Expecting power-of-two sizes");
            }
            trace = new int[size];
            mask = size - 1;
        }

        void add(int pc) {
            trace[pos] = pc;
            pos = (pos + 1) & mask;
            if (size != mask) {
                size++;
            }
        }

        public int size() {
            return size;
        }

        public int get(int i) {
            if (i >= size) {
                throw new IndexOutOfBoundsException(i + " (" + size + ")");
            }
            return trace[(pos - i - 1) & mask];
        }

        void clear() {
            size = 0;
            pos = 0;
        }
    }
}
