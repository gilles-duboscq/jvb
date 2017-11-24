package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.Bus;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.debug.boxes.Box;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.util.Arrays;
import java.util.List;

import static gd.twohundred.jvb.Utils.log2;
import static gd.twohundred.jvb.Utils.mask;
import static java.lang.Integer.compareUnsigned;

public class MemoryView implements View {
    private final Debugger debugger;
    private final VerticalBoxes verticalBoxes;
    private final KeyMap<Runnable> keyMap;
    private final MemoryBox memoryBox;
    private int startAddress = 0x05000000;

    private volatile State state;
    private final KeyMap<Runnable> gotoKeyMap;
    private final Breakpoints.AddressField addressField;

    public MemoryView(Debugger debugger) {
        this.debugger = debugger;
        state = State.Idle;
        memoryBox = new MemoryBox();
        verticalBoxes = new VerticalBoxes("Memory", Arrays.asList(memoryBox));
        Terminal terminal = debugger.getTerminal();
        keyMap = new KeyMap<>();
        keyMap.bind(() -> {
            if (compareUnsigned(startAddress, memoryBox.height * 16) < 0) {
                startAddress = 0;
            } else {
                startAddress -= memoryBox.height * 16;
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_ppage));
        keyMap.bind(() -> {
            if (compareUnsigned(startAddress, -memoryBox.height * 16 * 2) > 0) {
                startAddress = -memoryBox.height * 16;
            } else {
                startAddress += memoryBox.height * 16;
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_npage));
        keyMap.bind(() -> {
            startAddress = -memoryBox.height * 16;
        }, KeyMap.key(terminal, InfoCmp.Capability.key_end));
        keyMap.bind(() -> {
            startAddress = 0;
        }, KeyMap.key(terminal, InfoCmp.Capability.key_home));
        keyMap.bind(this::startGoto, "g");
        gotoKeyMap = new KeyMap<>();
        gotoKeyMap.bind(this::finishGoto, "\r");
        gotoKeyMap.bind(this::abortGoto, "q");
        addressField = new Breakpoints.AddressField(gotoKeyMap, debugger.getTerminal());
    }

    private enum State {
        Idle,
        Goto
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        switch (state) {
            case Idle:
                return null;
            case Goto:
                return new Cursor(addressField.getCursorX(), 0);
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    @Override
    public String getTitle() {
        return "Memory";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        int startLines = lines.size();
        if (state == State.Goto) {
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

            AttributedStringBuilder addrPromptBottomLine = new AttributedStringBuilder();
            addrPromptBottomLine.append('└');
            View.horizontalLine(addrPromptBottomLine, width - 2);
            addrPromptBottomLine.append('┘');
            lines.add(addrPromptBottomLine.toAttributedString());

            AttributedStringBuilder actionsLine = new AttributedStringBuilder();
            actionsLine.append(" └Abort(");
            actionsLine.append("q", AttributedStyle.DEFAULT.underline());
            actionsLine.append(")┘ └Go(");
            actionsLine.append("⏎", addressField.isCurrentInputValid() ? AttributedStyle.DEFAULT.underline() : AttributedStyle.DEFAULT);
            actionsLine.append(")┘");
            lines.add(actionsLine.toAttributedString());
        } else {
            AttributedStringBuilder gotoLine = new AttributedStringBuilder();
            gotoLine.append(" └");
            gotoLine.append("G", AttributedStyle.DEFAULT.underline());
            gotoLine.append("oto┘");
            lines.add(gotoLine.toAttributedString());
        }
        int remainingHeight = height - (lines.size() - startLines);
        verticalBoxes.appendLines(lines, width, remainingHeight);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        switch (state) {
            case Idle:
                return keyMap;
            case Goto:
                return gotoKeyMap;
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    private void startGoto() {
        addressField.reset();
        state = State.Goto;
        debugger.debug(Logger.Component.Debugger, "Starting goto..");
        debugger.markRefreshNeeded();
    }

    private void finishGoto() {
        if (!addressField.isCurrentInputValid()) {
            return;
        }
        state = State.Idle;
        int addr = addressField.getCurrentValue() & ~mask(log2(16));
        if (compareUnsigned(addr, -(memoryBox.height + 2) * 16) > 0) {
            addr = -(memoryBox.height + 2) * 16;
        }
        debugger.debug(Logger.Component.Debugger, "Goto %#010x", addr);
        startAddress = addr;
        debugger.markRefreshNeeded();
    }

    private void abortGoto() {
        state = State.Idle;
        debugger.debug(Logger.Component.Debugger, "Abort goto");
        debugger.markRefreshNeeded();
    }

    @Override
    public char getAccelerator() {
        return 'y';
    }

    private class MemoryBox implements Box {
        int height;

        @Override
        public String name() {
            return "Memory";
        }

        @Override
        public int minWidth() {
            return 62;
        }

        @Override
        public boolean fixedWidth() {
            return true;
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
            this.height = height;
            int lineAddress = startAddress + 16 * line;
            Bus bus = debugger.getBus();
            asb.append(String.format("%08x: ", lineAddress));
            appendWord(asb, lineAddress, bus);
            appendWord(asb, lineAddress + 4, bus);
            appendWord(asb, lineAddress + 8, bus);
            appendWord(asb, lineAddress + 12, bus);
        }

        private void appendWord(AttributedStringBuilder asb, int address, Bus bus) {
            try {
                asb.append(formattedWord(bus.getWord(address)));
            } catch (BusError e) {
                if (e.getReason() == BusError.Reason.Error) {
                    throw new RuntimeException(e);
                }
                asb.append(" ");
                appendByte(asb, address, bus);
                asb.append(" ");
                appendByte(asb, address + 1, bus);
                asb.append(" ");
                appendByte(asb, address + 2, bus);
                asb.append(" ");
                appendByte(asb, address + 3, bus);
                asb.append(" ");
            }
        }

        private void appendByte(AttributedStringBuilder asb, int address, Bus bus) {
            try {
                asb.append(String.format("%02x", bus.getByte(address)));
            } catch (BusError e) {
                String errorValue;
                switch (e.getReason()) {
                    case Unmapped:
                        errorValue = "UU";
                        break;
                    case Unimplemented:
                        errorValue = "II";
                        break;
                    case Permission:
                        errorValue = "XX";
                        break;
                    case Error:
                    default:
                        throw new RuntimeException(e);
                }
                asb.append(errorValue);
            }
        }
    }

    private static String formattedWord(int word) {
        return String.format(" %02x %02x %02x %02x ", word & 0xff, (word >> 8) & 0xff, (word >> 16) & 0xff, (word >> 24) & 0xff);
    }
}
