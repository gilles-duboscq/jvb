package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.CartridgeROM;
import gd.twohundred.jvb.components.Debugger;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.utils.AttributedString;

import java.util.List;

public class Overview implements View {
    private final Debugger debugger;

    public Overview(Debugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Cursor getCursorPosition() {
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
