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
import static java.lang.Long.min;

public class Main {
    private Path cartridgePath;

    private static final long NS_PER_CYCLES = Utils.NANOS_PER_SECOND / CPU.CLOCK_HZ;
    private static final long TARGET_MACRO_TICK_PER_SECOND = 100;
    private static final long TARGET_GRANULARITY_NS = Utils.NANOS_PER_SECOND / TARGET_MACRO_TICK_PER_SECOND;
    private static final long MAX_CYCLES_PER_MACRO_TICK = TARGET_GRANULARITY_NS / NS_PER_CYCLES;

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
            //debugger.pause();
            logger = debugger;
        } catch (RuntimeException re) {
            logger = new StdLogger();
            logger.error(Misc, re, "Could not create debugger");
        }
        try {
            mainWindow.setVisible(true);
            CartridgeROM rom = new CartridgeROM(cartridgePath, logger);
            DefaultSwingInputProvider inputProvider = new DefaultSwingInputProvider();
            mainWindow.setFocusable(true);
            mainWindow.addKeyListener(inputProvider);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(inputProvider);
            DefaultAudioOut audioOut = new DefaultAudioOut(logger);
            VirtualBoy virtualBoy = new VirtualBoy(mainWindow.getScreen(), audioOut, inputProvider, rom, new CartridgeRAM(), logger);
            virtualBoy.reset();
            if (debugger != null) {
                debugger.attach(virtualBoy);
                debugger.refresh();
            }
            long startT = System.nanoTime();
            long cycles = 0;
            while (!Thread.interrupted() && mainWindow.isOpen() && !virtualBoy.isHalted()) {
                long t = System.nanoTime();
                long clockT = t - startT;
                long simulationT = cycles * NS_PER_CYCLES;

                long missingTime = clockT - simulationT;
                if (missingTime > 0) {
                    long missingCycles = missingTime / NS_PER_CYCLES;
                    missingCycles = min(missingCycles, MAX_CYCLES_PER_MACRO_TICK);
                    //logger.warning(Misc, "Missing cycles: %d", missingCycles);
                    //System.out.println(missingCycles + " " +  missingTime / NS_PER_CYCLES);
                    long cyclesDone = virtualBoy.tick(missingCycles);
                    cycles += cyclesDone;
                } else if (-missingTime > 10000){
                    LockSupport.parkNanos(-missingTime);
                }
            }
//            while (!Thread.interrupted() && mainWindow.isOpen() && !virtualBoy.isHalted()) {
//                virtualBoy.tick(20000);
//            }
            logger.info(Misc, "Bye :)");
        } finally {
            if (debugger != null) {
                debugger.exit();
            }
        }
        System.exit(0);
    }
}
