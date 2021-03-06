package gd.twohundred.jvb;

import gd.twohundred.jvb.components.interfaces.InputProvider;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.atomic.AtomicBoolean;

import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.testBit;

public class DefaultSwingInputProvider implements InputProvider, KeyListener, KeyEventPostProcessor {
    private volatile int status = intBit(Inputs.One.offset());

    @Override
    public boolean read(Inputs inputs) {
        return testBit(status, inputs.offset());
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        Inputs input = keyCodeToInput(e.getKeyCode(), e.getKeyLocation());
        if (input != null) {
            status |= intBit(input.offset());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Inputs input = keyCodeToInput(e.getKeyCode(), e.getKeyLocation());
        if (input != null) {
            status &= ~intBit(input.offset());
        }

    }

    private Inputs keyCodeToInput(int keyCode, int keyLocation) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
                return Inputs.RightDPadUp;
            case KeyEvent.VK_DOWN:
                return Inputs.RightDPadDown;
            case KeyEvent.VK_LEFT:
                return Inputs.RightDPadLeft;
            case KeyEvent.VK_RIGHT:
                return Inputs.RightDPadRight;
            case KeyEvent.VK_I:
                return Inputs.RightDPadUp;
            case KeyEvent.VK_K:
                return Inputs.RightDPadDown;
            case KeyEvent.VK_J:
                return Inputs.RightDPadLeft;
            case KeyEvent.VK_L:
                return Inputs.RightDPadRight;
            case KeyEvent.VK_W:
                return Inputs.LeftDPadUp;
            case KeyEvent.VK_S:
                return Inputs.LeftDPadDown;
            case KeyEvent.VK_A:
                return Inputs.LeftDPadLeft;
            case KeyEvent.VK_D:
                return Inputs.LeftDPadRight;
            case KeyEvent.VK_SPACE:
                return Inputs.Select;
            case KeyEvent.VK_ENTER:
                return Inputs.Start;
            case KeyEvent.VK_F:
                return Inputs.A;
            case KeyEvent.VK_H:
                return Inputs.B;
            case KeyEvent.VK_E:
                return Inputs.L;
            case KeyEvent.VK_U:
                return Inputs.R;
            case KeyEvent.VK_CONTROL:
                if (keyLocation == KeyEvent.KEY_LOCATION_RIGHT) {
                    return Inputs.A;
                }
                break;
            case KeyEvent.VK_NUMPAD0:
                return Inputs.B;
            case KeyEvent.VK_B:
                return Inputs.LowBattery;
        }
        return null;
    }

    @Override
    public boolean postProcessKeyEvent(KeyEvent e) {
        Inputs input = keyCodeToInput(e.getKeyCode(), e.getKeyLocation());
        if (input == null) {
            return false;
        }
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            status |= intBit(input.offset());
            return true;
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            status &= ~intBit(input.offset());
            return true;
        }
        return false;
    }
}
