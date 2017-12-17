package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Utils;
import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.debug.boxes.Box;
import gd.twohundred.jvb.components.debug.boxes.HorizontalBoxes;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import gd.twohundred.jvb.disassembler.Disassembler;
import gd.twohundred.jvb.disassembler.Instruction;
import gd.twohundred.jvb.disassembler.RelativeToStringInstruction;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

public class CPUView implements View {
    private final Debugger debugger;
    private final KeyMap<Runnable> runningKeyMap;
    private final KeyMap<Runnable> pausedKeyMap;
    private final HorizontalBoxes horizontalBoxes;
    private final DisassemblyBox disassemblyBox;

    public CPUView(Debugger debugger) {
        this.debugger = debugger;
        runningKeyMap = new KeyMap<>();
        pausedKeyMap = new KeyMap<>();
        runningKeyMap.bind(debugger::pause, "p");
        pausedKeyMap.bind(debugger::continueExecution, "c");
        pausedKeyMap.bind(debugger::step, "s");
        disassemblyBox = new DisassemblyBox();
        horizontalBoxes = new HorizontalBoxes("CPU", new VerticalBoxes("Instructions", new TraceBox(), disassemblyBox), new RegistersBox());
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "CPU";
    }

    private abstract class AbstractDisassemblyBox implements Box {
        @Override
        public int minWidth() {
            return 38;
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

        protected int instruction(AttributedStringBuilder asb, int address) {
            String pcHex = Integer.toHexString(address);
            View.repeat(asb, 8 - pcHex.length(), '0');
            asb.append(pcHex);
            asb.append("  ");
            Instruction instruction = Disassembler.disassemble(debugger.getBus(), address);
            if (instruction == null) {
                asb.append("ILLEGAL", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                return  2;
            } else {
                if (instruction instanceof RelativeToStringInstruction) {
                    asb.append(((RelativeToStringInstruction) instruction).toString(address));
                } else {
                    asb.append(instruction.toString());
                }
                return instruction.getFormat().getLength();
            }
        }
    }

    private class DisassemblyBox extends AbstractDisassemblyBox {
        int pc;

        @Override
        public String name() {
            return "Disassembly";
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            boolean currentPc = debugger.getPc() == pc;
            if (currentPc) {
                asb.style(AttributedStyle.BOLD);
            }
            pc += instruction(asb, pc);
            if (currentPc) {
                asb.style(AttributedStyle.BOLD_OFF);
            }
        }

    }

    private class TraceBox extends AbstractDisassemblyBox {
        @Override
        public String name() {
            return "Trace";
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            Debugger.TraceBuffer traceBuffer = debugger.getTraceBuffer();
            if (height - line - 1 >= traceBuffer.size()) {
                return;
            }
            int pc = traceBuffer.get(height - line - 1);
            instruction(asb, pc);
        }
    }

    private class RegistersBox implements Box {
        @Override
        public String name() {
            return "Registers";
        }

        @Override
        public int minWidth() {
            return 15;
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public int minHeight() {
            return CPU.REGISTER_COUNT + 1;
        }

        @Override
        public boolean fixedHeight() {
            return false;
        }

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            if (line < CPU.REGISTER_COUNT) {
                new Formatter(asb).format("r%02d: %#010x", line, debugger.getCpu().getRegister(line));
            } else if (line == CPU.REGISTER_COUNT) {
                new Formatter(asb).format("pc:  %#010x", debugger.getPc());
            }
        }
    }


    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        AttributedStringBuilder actionsLine = new AttributedStringBuilder();
        switch (debugger.getState()) {
            case Running:
                actionsLine.append(" └");
                actionsLine.append("P", AttributedStyle.DEFAULT.underline());
                actionsLine.append("ause┘");
                break;
            case Paused:
                actionsLine.append(" └");
                actionsLine.append("C", AttributedStyle.DEFAULT.underline());
                actionsLine.append("ontinue┘ └");
                actionsLine.append("S", AttributedStyle.DEFAULT.underline());
                actionsLine.append("tep┘");
                break;
        }
        lines.add(actionsLine.toAttributedString());
        disassemblyBox.pc = debugger.getPc();
        horizontalBoxes.appendLines(lines, width, height - 1);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        switch (debugger.getState()) {
            case Running:
                return runningKeyMap;
            case Stepping:
            case Paused:
                return pausedKeyMap;
            default:
                return null;
        }
    }

    @Override
    public char getAccelerator() {
        return 'p';
    }
}
