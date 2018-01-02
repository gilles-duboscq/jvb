package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger.Component;
import gd.twohundred.jvb.Logger.Level;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class Logs implements View {
    private final LogBox logBox;
    private final VerticalBoxes boxes;
    private final Map<Component, Level> levels;
    private final KeyMap<Runnable> settingsKeyMap;
    private State state;
    private Component settingsComponent;

    private enum State {
        Default,
        Settings
    }

    public Logs(List<LogMessage> messages, Terminal terminal, Map<Component, Level> levels) {
        this.levels = levels;
        state = State.Default;
        settingsComponent = Component.values()[0];
        logBox = new LogBox(messages, terminal, levels);
        boxes = new VerticalBoxes("Logs", logBox);
        logBox.getKeyMap().bind(() -> state = State.Settings, "s");
        logBox.getKeyMap().bind(logBox::trimLogs, "t");
        settingsKeyMap = new KeyMap<>();
        settingsKeyMap.bind(() -> state = State.Default, "\r");
        settingsKeyMap.bind(() -> {
            if (settingsComponent.ordinal() > 0) {
                settingsComponent = Component.values()[settingsComponent.ordinal() - 1];
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_up));
        settingsKeyMap.bind(() -> {
            if (settingsComponent.ordinal() + 1 < Component.values().length) {
                settingsComponent = Component.values()[settingsComponent.ordinal() + 1];
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_down));
        settingsKeyMap.bind(() -> {
            Level componentLevel = levels.get(settingsComponent);
            if (componentLevel.ordinal() > 0) {
                levels.put(settingsComponent, Level.values()[componentLevel.ordinal() - 1]);
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_left));
        settingsKeyMap.bind(() -> {
            Level componentLevel = levels.get(settingsComponent);
            if (componentLevel.ordinal() + 1 < Level.values().length) {
                levels.put(settingsComponent, Level.values()[componentLevel.ordinal() + 1]);
            }
        }, KeyMap.key(terminal, InfoCmp.Capability.key_right));
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "Logs";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        int startLines = lines.size();
        if (state == State.Settings) {
            int componentLength = Arrays.stream(Component.values()).map(Component::name).mapToInt(String::length).max().orElse(0);
            for (Component component : Component.values()) {
                AttributedStringBuilder componentLine = new AttributedStringBuilder();
                componentLine.append('│');
                if (component == settingsComponent) {
                    componentLine.style(AttributedStyle.INVERSE);
                }
                View.rightPad(componentLine, componentLength + 1, component.name());
                componentLine.style(AttributedStyle.INVERSE_OFF);

                Level[] values = Level.values();
                Level componentLevel = levels.get(component);
                for (int i = 0; i < values.length; i++) {
                    Level level = values[i];
                    if (level == componentLevel) {
                        componentLine.style(AttributedStyle.INVERSE);
                    }
                    componentLine.append(level.name());
                    componentLine.style(AttributedStyle.INVERSE_OFF);
                    if (i < values.length - 1) {
                        componentLine.append(' ');
                    }
                }

                View.padToLength(componentLine, width - 1);
                componentLine.append('│');
                lines.add(componentLine.toAttributedString());
            }

            AttributedStringBuilder settingsBottomLine = new AttributedStringBuilder();
            settingsBottomLine.append('└');
            View.horizontalLine(settingsBottomLine, width - 2);
            settingsBottomLine.append('┘');
            lines.add(settingsBottomLine.toAttributedString());

            AttributedStringBuilder actionsLine = new AttributedStringBuilder();
            actionsLine.append(" └Done(");
            actionsLine.append("⏎");
            actionsLine.append(")┘");
            lines.add(actionsLine.toAttributedString());
        } else {
            AttributedStringBuilder addLine = new AttributedStringBuilder();
            addLine.append(" └");
            addLine.append("S", AttributedStyle.DEFAULT.underline());
            addLine.append("ettings┘ └");
            addLine.append("T", AttributedStyle.DEFAULT.underline());
            addLine.append("rim┘");
            lines.add(addLine.toAttributedString());
        }
        int remainingHeight = height - (lines.size() - startLines);
        boxes.appendLines(lines, width, remainingHeight);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        switch (state) {
            case Default:
                return logBox.getKeyMap();
            case Settings:
                return settingsKeyMap;
        }
        return null;
    }

    @Override
    public char getAccelerator() {
        return 'l';
    }

    private static class LogBox implements Box {
        private final List<LogMessage> messages;
        private final Map<Component, Level> levels;
        private int firstLine;
        private int selectedLine;
        private boolean scrolling = true;
        private final KeyMap<Runnable> keyMap;
        private int lastHeight;

        public LogBox(List<LogMessage> messages, Terminal terminal, Map<Component, Level> levels) {
            this.messages = messages;
            this.levels = levels;
            keyMap = new KeyMap<>();
            keyMap.bind(() -> {
                if (selectedLine + 1 < messages.size()) {
                    this.selectedLine++;
                }
            }, KeyMap.key(terminal, InfoCmp.Capability.key_down));
            keyMap.bind(() -> {
                if (selectedLine > 0) {
                    this.selectedLine--;
                }
                scrolling = false;
            }, KeyMap.key(terminal, InfoCmp.Capability.key_up));
            keyMap.bind(() -> {
                selectedLine = max(0, selectedLine - lastHeight);
                scrolling = false;
            }, KeyMap.key(terminal, InfoCmp.Capability.key_ppage));
            keyMap.bind(() -> {
                selectedLine = min(max(0, messages.size() - 1), selectedLine + lastHeight);
            }, KeyMap.key(terminal, InfoCmp.Capability.key_npage));
            keyMap.bind(() -> {
                scrolling = true;
            }, KeyMap.key(terminal, InfoCmp.Capability.key_end));
            keyMap.bind(() -> {
                selectedLine = 0;
                scrolling = false;
            }, KeyMap.key(terminal, InfoCmp.Capability.key_home));
        }

        public KeyMap<Runnable> getKeyMap() {
            return keyMap;
        }

        @Override
        public String name() {
            return "Logs";
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
            return 0;
        }

        @Override
        public boolean fixedHeight() {
            return false;
        }

        private void trimLogs() {
            messages.removeIf(message -> levels.get(message.getSource()).ordinal() < message.getLevel().ordinal());
            selectedLine = max(selectedLine, messages.size() - 1);
            firstLine = max(firstLine, max(messages.size() - 1, 0));
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            if (scrolling) {
                firstLine = max(firstLine, messages.size() - height);
                selectedLine = max(0, messages.size() - 1);
            } else {
                if (selectedLine < firstLine) {
                    firstLine = selectedLine;
                }
                if (selectedLine >= firstLine + height) {
                    firstLine = selectedLine - height + 1;
                }
            }
            lastHeight = height;

            int messageIndex = firstLine + line;
            if (messageIndex >= messages.size()) {
                return;
            }
            LogMessage message = messages.get(messageIndex);
            if (messageIndex == selectedLine) {
                asb.style(AttributedStyle.INVERSE);
            }
            asb.append('[');
            asb.append(Integer.toString(messageIndex));
            asb.append("][");
            asb.append(Long.toString(message.getCycle()));
            asb.append("][");
            asb.append(message.getLevel().name(), AttributedStyle.DEFAULT.foreground(levelColor(message.getLevel())));
            asb.append("][");
            asb.append(message.getSource().name());
            asb.append("] ");
            asb.append(message.getMessage());
            asb.style(AttributedStyle.INVERSE_OFF);
        }

        private static int levelColor(Level level) {
            switch (level) {
                case Error:
                    return AttributedStyle.RED;
                case Warning:
                    return AttributedStyle.MAGENTA;
                default:
                    return AttributedStyle.WHITE;
            }
        }
    }
}
