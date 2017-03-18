package gd.twohundred.jvb.components.debug;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.List;

public interface View {
    Cursor getCursorPosition(Size size);

    String getTitle();

    void appendLines(List<AttributedString> lines, int width, int height);

    KeyMap<Runnable> getKeyMap();

    char getAccelerator();

    static void horizontalLine(AttributedStringBuilder asb, int width) {
        repeat(asb, width, '─');
    }

    static void repeat(AttributedStringBuilder asb, int width, char c) {
        for (int i = 0; i < width; i++) {
            asb.append(c);
        }
    }

    static void leftPadInt(AttributedStringBuilder asb, int length, long value) {
        String s = Long.toString(value);
        if (s.length() < length) {
            View.repeat(asb, length - s.length(), ' ');
        }
        asb.append(s);
    }

    static void leftPadHex(AttributedStringBuilder asb, int length, long value) {
        String s = Long.toHexString(value);
        if (s.length() < length) {
            View.repeat(asb, length - s.length(), ' ');
        }
        asb.append(s);
    }

    static void padToLength(AttributedStringBuilder asb, int length) {
        padToLength(asb, length, ' ');
    }

    static void padToLength(AttributedStringBuilder asb, int length, char c) {
        repeat(asb, length - asb.length(), c);
    }
}
