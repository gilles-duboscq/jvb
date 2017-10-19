package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.CartridgeROM;
import gd.twohundred.jvb.components.Debugger;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;

import java.util.List;

public class Overview implements View {
    private final Debugger debugger;

    public Overview(Debugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        CartridgeROM cartridgeRom = debugger.getCartridgeRom();
        lines.add(new AttributedString(" Title:      " + cartridgeRom.getGameTitle()));
        lines.add(new AttributedString(" Game code:  " + cartridgeRom.getGameCode()));
        lines.add(new AttributedString(" Version:    1." + cartridgeRom.getGameVersion()));
        lines.add(new AttributedString(" Maker code: " + cartridgeRom.getMakerCode()));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString(" Emulation Stats:"));
        Debugger.TicksStats ticksStats = debugger.getTicksStats();
        lines.add(new AttributedString("  cycles/s: " + ticksStats.lastCyclesPerSecond));
        lines.add(new AttributedString("  ticks/s: " + ticksStats.lastTickPerSecond));
        long cyclesPerTick = -1;
        if (ticksStats.lastTickPerSecond != 0) {
            cyclesPerTick = ticksStats.lastCyclesPerSecond / ticksStats.lastTickPerSecond;
        }
        lines.add(new AttributedString("  cycles/tick: " + cyclesPerTick));
        lines.add(new AttributedString("  last cycles/tick: " + ticksStats.lastCycles));
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return null;
    }

    @Override
    public char getAccelerator() {
        return 'o';
    }
}
