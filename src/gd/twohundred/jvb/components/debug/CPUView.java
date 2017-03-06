package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.disassembler.Disassembler;
import gd.twohundred.jvb.disassembler.Instruction;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.Formatter;
import java.util.List;

public class CPUView implements View {
    private final Debugger debugger;

    public CPUView(Debugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "CPU";
    }

    private int pc = 0xdeadbeef;

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        pc = debugger.getPc();
        int registersWidth = 16 + 2;
        int instructionsWidth = width - registersWidth - 2 - 2 - 1;
        lines.add(top(instructionsWidth, registersWidth, width));
        for (int i = 0; i < height - 2; i++) {
            lines.add(line(instructionsWidth, registersWidth, width, i));
        }
        lines.add(bottom(instructionsWidth, registersWidth, width));
    }

    private AttributedString line(int instructionsWidth, int registersWidth, int totalWidth, int line) {
        return line(padded(instructionLine(line)), padded(registerLine(line)), instructionsWidth, registersWidth, totalWidth, '│', '│');
    }

    private LineFragmentWidget instructionLine(int line) {
        return (asb, width) -> {
            boolean currentPc = debugger.getPc() == pc;
            if (currentPc) {
                asb.style(AttributedStyle.BOLD);
            }
            String pcHex = Integer.toHexString(pc);
            View.repeat(asb, 8 - pcHex.length(), '0');
            asb.append(pcHex);
            asb.append("  ");
            Instruction instruction = Disassembler.disassemble(debugger.getBus(), pc);
            if (instruction == null) {
                asb.append("ILLEGAL", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                pc += 2;
            } else {
                asb.append(instruction.toString());
                pc += instruction.getFormat().getLength();
            }
            if (currentPc) {
                asb.style(AttributedStyle.BOLD_OFF);
            }
        };
    }

    private LineFragmentWidget registerLine(int line) {
        return (asb, width) -> {
            if (line < CPU.REGISTER_COUNT) {
                new Formatter(asb).format("r%02d: %#010x", line, debugger.getCpu().getRegister(line));
            } else if (line == CPU.REGISTER_COUNT) {
                new Formatter(asb).format("pc:  %#010x", debugger.getPc());
            }
        };
    }

    private static LineFragmentWidget padded(LineFragmentWidget content) {
        return (asb, width) -> {
            int start = asb.length();
            content.append(asb, width);
            pad(asb, width - (asb.length() - start));
        };
    }

    private static void pad(AttributedStringBuilder asb, int width) {
        View.repeat(asb, width, ' ');
    }

    private static AttributedString top(int instructionsWidth, int registersWidth, int totalWidth) {
        return line(title("Disassembly"), title("Registers"), instructionsWidth, registersWidth, totalWidth, '┌', '┐');
    }

    private static AttributedString bottom(int instructionsWidth, int registersWidth, int totalWidth) {
        return line(View::horizontalLine, View::horizontalLine, instructionsWidth, registersWidth, totalWidth, '└', '┘');
    }

    private static LineFragmentWidget title(String title) {
        return (asb, width) -> {
            int pad;
            if (title.length() + 1<= width) {
                asb.append('─');
                asb.append(title);
                pad = width - title.length() - 1;
            } else {
                pad = width;
            }
            View.repeat(asb, pad, '─');
        };
    }

    private static AttributedString line(LineFragmentWidget instructions, LineFragmentWidget registers, int instructionsWidth, int registersWidth, int totalWidth, char left, char right) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(left);
        instructions.append(asb, instructionsWidth);
        asb.append(right);
        for (int i = 0; i < totalWidth - instructionsWidth - registersWidth - 4; i++) {
            asb.append(' ');
        }
        asb.append(left);
        registers.append(asb, registersWidth);
        asb.append(right);
        return asb.toAttributedString();
    }

    private interface LineFragmentWidget {
        void append(AttributedStringBuilder asb, int width);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return null;
    }

    @Override
    public char getAccelerator() {
        return 'p';
    }
}
