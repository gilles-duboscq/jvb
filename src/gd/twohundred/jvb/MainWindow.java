package gd.twohundred.jvb;

import javax.swing.*;
import java.awt.event.WindowEvent;

import static java.awt.event.WindowEvent.WINDOW_CLOSING;

public class MainWindow extends JFrame {
    private volatile boolean open = true;
    private final Screen screen;

    public MainWindow() {
        this.setTitle("JVirtualBoy");
        MergedScreen scr = new MergedScreen();
        screen = scr;
        add(scr);
        setIgnoreRepaint(true);
        pack();
        setResizable(false);
    }

    public boolean isOpen() {
        return this.isVisible() && open;
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WINDOW_CLOSING) {
            open = false;
        }
    }

    public Screen getScreen() {
        return screen;
    }
}
