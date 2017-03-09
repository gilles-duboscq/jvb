package gd.twohundred.jvb.components.debug.boxes;

import org.jline.utils.AttributedStringBuilder;

public interface Box {
    String name();
    int minWidth();
    boolean fixedWidth();
    int minHeight();
    boolean fixedHeight();
    void line(AttributedStringBuilder asb, int line, int width, int height);
}
