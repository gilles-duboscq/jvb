package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.GamePad;
import gd.twohundred.jvb.components.cpu.CPU;
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
        verticalBoxes = new VerticalBoxes("Misc.", new TimerBox(), new GamePadBox());
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
        return 't';
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
            return 5;
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
                case 4:
                    asb.append("Zero Status: ").append(timer.hasZeroStatus() ? "1" : "0");
                    break;
            }
        }
    }

    private class GamePadBox implements Box {

        @Override
        public String name() {
            return "GamePad";
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
            return 3;
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            GamePad gamePad = debugger.getGamePad();
            switch (line) {
                case 0:
                    asb.append("Software read in progress: ").append(gamePad.isSoftwareReadInProgress() ? '✔' : '✗');
                    break;
                case 1:
                    asb.append("Hardware read in progress: ").append(gamePad.isHardwareReadInProgress() ? '✔' : '✗');
                    break;
                case 2:
                    asb.append("Interrupts: ").append(gamePad.isInterruptEnabled() ? '✔' : '✗');
                    break;
            }
        }
    }
}
