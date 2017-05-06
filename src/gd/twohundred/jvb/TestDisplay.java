package gd.twohundred.jvb;

import org.jline.terminal.Terminal;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;

public class TestDisplay extends Display {
    private int columns;

    public TestDisplay(Terminal terminal, boolean fullscreen) {
        super(terminal, fullscreen);
    }

    public void resize(int rows, int columns) {
        super.resize(rows, columns);
        if (this.columns != columns) {
            this.columns = columns;
        }
    }

    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) {
            return i1;
        }
        int width = columns + 1;
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (c0 == columns) { // at right margin
            if (c1 == 0) {
                terminal.puts(InfoCmp.Capability.carriage_return);
                c0 = 0;
            } else {
                // it's hard to move out of right margin in a portable way
                terminal.puts(InfoCmp.Capability.cursor_address, l1, c1);
                cursorPos = i1;
                return i1;
            }
        }
        if (l0 > l1) {
            perform(InfoCmp.Capability.cursor_up, InfoCmp.Capability.parm_up_cursor, l0 - l1);
        } else if (l0 < l1) {
            // TODO: clean the following
            if (fullScreen) {
                if (!terminal.puts(InfoCmp.Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        terminal.puts(InfoCmp.Capability.cursor_down);
                    }
                    if (cursorDownIsNewLine) {
                        c0 = 0;
                    }
                }
            } else {
                rawPrint('\n', l1 - l0);
                c0 = 0;
            }
        }
        if (c0 != 0 && c1 == 0) {
            terminal.puts(InfoCmp.Capability.carriage_return);
        } else if (c0 < c1) {
            perform(InfoCmp.Capability.cursor_right, InfoCmp.Capability.parm_right_cursor, c1 - c0);
        } else if (c0 > c1) {
            perform(InfoCmp.Capability.cursor_left, InfoCmp.Capability.parm_left_cursor, c0 - c1);
        }
        cursorPos = i1;
        return i1;
    }

    void rawPrint(char c, int num) {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    void rawPrint(int c) {
        terminal.writer().write(c);
    }
}
