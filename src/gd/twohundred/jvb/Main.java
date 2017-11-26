package gd.twohundred.jvb;

import gd.twohundred.jvb.components.CartridgeRAM;
import gd.twohundred.jvb.components.CartridgeROM;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.VirtualBoy;
import gd.twohundred.jvb.components.cpu.CPU;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.LockSupport;

import static gd.twohundred.jvb.Logger.Component.Misc;
import static java.lang.Long.min;

public class Main {
    @CommandLine.Parameters(description = "ROM file to execute", paramLabel = "ROM")
    private Path cartridgePath;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = { "-d", "--debugger" }, description = "enable text-mode debugger")
    private boolean useDebugger = false;

    @CommandLine.Option(names = { "-p", "--paused" }, description = "Start the debugger in paused mode")
    private boolean pauseOnStart = false;

    private static final long NS_PER_CYCLES = Utils.NANOS_PER_SECOND / CPU.CLOCK_HZ;
    private static final long TARGET_MACRO_TICK_PER_SECOND = 100;
    private static final long TARGET_GRANULARITY_NS = Utils.NANOS_PER_SECOND / TARGET_MACRO_TICK_PER_SECOND;
    private static final long MAX_CYCLES_PER_MACRO_TICK = TARGET_GRANULARITY_NS / NS_PER_CYCLES;

    public static void main(String... args) throws IOException {
        Main m = new Main();
        try {
            CommandLine commandLine = new CommandLine(m);
            commandLine.registerConverter(java.nio.file.Path.class, s -> java.nio.file.Paths.get(s));
            commandLine.parse(args);
            m.checkParameters(commandLine);
            if (commandLine.isUsageHelpRequested()) {
                commandLine.usage(System.out);
                return;
            } else if (commandLine.isVersionHelpRequested()) {
                commandLine.printVersionHelp(System.out);
                return;
            }
        } catch (CommandLine.ParameterException ex) {
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
            System.exit(2);
        }

        try {
            m.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void checkParameters(CommandLine commandLine) {
        if (pauseOnStart && !useDebugger) {
            throw new CommandLine.ParameterException(commandLine, "--paused can not be used without --debugger");
        }
    }

    private void run() throws IOException {
        MainWindow mainWindow = new MainWindow();
        Debugger debugger = null;
        ForwardingLogger logger = new ForwardingLogger();
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
            if (useDebugger) {
                debugger = new Debugger();
                logger.setDestination(debugger);
                if (pauseOnStart) {
                    debugger.pause();
                }
                debugger.attach(virtualBoy);
                debugger.refresh();
            } else {
                logger.setDestination(new StdLogger());
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
                } else if (-missingTime > 10000) {
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
