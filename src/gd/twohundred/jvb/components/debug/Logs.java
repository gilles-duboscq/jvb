package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.List;

import static java.lang.Integer.max;

public class Logs implements View {
    private final List<LogMessage> messages;

    public Logs(List<LogMessage> messages) {
        this.messages = messages;
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
        int start = max(0, messages.size() - height);
        for (int i = start; i < messages.size(); i++) {
            LogMessage message = messages.get(i);
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append('[');
            builder.append(message.getLevel().name(), AttributedStyle.DEFAULT.foreground(levelColor(message.getLevel())));
            builder.append("][");
            builder.append(message.getSource().name());
            builder.append("] ");
            builder.append(message.getMessage());
            lines.add(builder.toAttributedString());
        }
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return null;
    }

    @Override
    public char getAccelerator() {
        return 'l';
    }

    private static int levelColor(Logger.Level level) {
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
