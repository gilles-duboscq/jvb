package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.HardwareTimer;
import gd.twohundred.jvb.components.debug.boxes.Box;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.util.Arrays;
import java.util.List;

public class OthersView implements View {
    private final VerticalBoxes verticalBoxes;
    private final Debugger debugger;

    public OthersView(Debugger debugger) {
        this.debugger = debugger;
        verticalBoxes = new VerticalBoxes("Misc.", Arrays.asList(new TimerBox()));
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "Others";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        verticalBoxes.appendLines(lines, width, height);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return null;
    }

    @Override
    public char getAccelerator() {
        return 'o';
    }

    private class TimerBox implements Box {

        @Override
        public String name() {
            return "Timer";
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
            return 4;
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            HardwareTimer timer = debugger.getTimer();
            switch (line) {
                case 0:
                    asb.append("Enabled: ").append(timer.isTimerEnabled() ? '✔' : ' ');
                    break;
                case 1:
                    asb.append("Interrupts Enabled: ").append(timer.isInterruptEnabled() ? '✔' : ' ');
                    break;
                case 2:
                    asb.append("Clock: ").append(Long.toString(CPU.CLOCK_HZ / timer.getPeriod())).append("Hz");
                    break;
                case 3:
                    asb.append("Reload value: ").append(Integer.toString(timer.getReloadValue()));
                    break;
            }
        }
    }
}
