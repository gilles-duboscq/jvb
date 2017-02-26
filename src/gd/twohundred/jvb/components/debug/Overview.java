package gd.twohundred.jvb.components.debug;

import org.jline.terminal.Cursor;
import org.jline.utils.AttributedString;

import java.util.List;

public class Overview implements View {
    @Override
    public Cursor getCursorPosition() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int maxLines) {

    }
}
