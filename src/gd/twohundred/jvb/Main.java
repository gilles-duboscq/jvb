package gd.twohundred.jvb;

import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.CartridgeRAM;
import gd.twohundred.jvb.components.CartridgeROM;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.VirtualBoy;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.LockSupport;

import static gd.twohundred.jvb.Logger.Component.Misc;

public class Main {
    private Path cartridgePath;

    public static void main(String... args) throws IOException {
        Main m = new Main();
        m.cartridgePath = Paths.get(args[0]);
        try {
            m.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws IOException {
        MainWindow mainWindow = new MainWindow();
        Debugger debugger = null;
        Logger logger;
        try {
            debugger = new Debugger();
            logger = debugger;
        } catch (RuntimeException re) {
            logger = new StdLogger();
            logger.error(Misc, "Could not create debugger: %s", re);
        }
        mainWindow.setVisible(true);
        CartridgeROM rom = new CartridgeROM(cartridgePath, logger);
        /*System.out.println(" Title: " + rom.getGameTitle());
        System.out.println(" Maker code: " + rom.getMakerCode());
        System.out.println(" Game code: " + rom.getGameCode());
        System.out.println(" Version: 1." + rom.getGameVersion());*/
        DefaultSwingInputProvider inputProvider = new DefaultSwingInputProvider();
        mainWindow.setFocusable(true);
        mainWindow.addKeyListener(inputProvider);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(inputProvider);
        VirtualBoy virtualBoy = new VirtualBoy(mainWindow.getScreen(), inputProvider, rom, new CartridgeRAM(), logger);
        virtualBoy.reset();
        if (debugger != null) {
            debugger.attach(virtualBoy);
            debugger.refresh();
        }
        long t = System.nanoTime();
        long cycles = 0;
        while (!Thread.interrupted() && mainWindow.isOpen()) {
            long newT = System.nanoTime();
            long dt = newT - t;
            long dCycles = dt * CPU.CLOCK_HZ / Utils.NANOS_PER_SECOND;
            long missingCycles = dCycles - cycles;

            int cyclesDone = virtualBoy.tick((int) missingCycles);
            cycles += cyclesDone;
            LockSupport.parkNanos(100000);
        }
        if (debugger != null) {
            debugger.exit();
        }
        System.exit(0);
    }
}
