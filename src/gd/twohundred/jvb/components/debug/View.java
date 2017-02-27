package gd.twohundred.jvb.components.debug;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.utils.AttributedString;

import java.util.List;

public interface View {
    Cursor getCursorPosition();

    String getTitle();

    void appendLines(List<AttributedString> lines, int maxLines);

    KeyMap<Runnable> getKeyMap();

    char getAccelerator();
}
