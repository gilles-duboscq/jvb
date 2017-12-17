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

    @CommandLine.Option(names = { "--logging" }, description = "Logging configuration: comma-separated list of <Component>:<Level>. See --list-logging.")
    private String logging;

    @CommandLine.Option(names = { "--list-logging" }, help = true, description = "List logging components and levels")
    private boolean listLogging = false;

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

    private void setLoggingLevels(LevelLogger logger) {
        if (logging == null) {
            return;
        }
        for (String spec : logging.split(",")) {
            int idx = spec.indexOf(':');
            if (idx < 0) {
                System.err.println("--logging-level should be a comma-separated list of <Component>:<Level> pairs.");
                System.exit(2);
            }
            String componentString = spec.substring(0, idx);
            String levelString = spec.substring(idx + 1);
            Logger.Component component;
            try {
                component = Logger.Component.valueOf(componentString);
            } catch (IllegalArgumentException e) {
                System.err.printf("Unknown component in --logging-level: '%s' See --list-logging for valid values.%n", componentString);
                System.exit(2);
                throw new RuntimeException("should not reach here");
            }
            Logger.Level level;
            try {
                level = Logger.Level.valueOf(levelString);
            } catch (IllegalArgumentException e) {
                System.err.printf("Unknown level in --logging-level: '%s' See --list-logging for valid values.%n", levelString);
                System.exit(2);
                throw new RuntimeException("should not reach here");
            }
            logger.setLevel(component, level);
        }
    }

    private void run() throws IOException {
        if (listLogging) {
            listLogging();
            return;
        }
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
                setLoggingLevels(debugger);
                logger.setDestination(debugger);
                if (pauseOnStart) {
                    debugger.pause();
                }
                debugger.attach(virtualBoy);
                debugger.refresh();
            } else {
                StdLogger stdLogger = new StdLogger();
                setLoggingLevels(stdLogger);
                logger.setDestination(stdLogger);
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

    private void listLogging() {
        System.out.println("Components:");
        for (Logger.Component component : Logger.Component.values()) {
            System.out.printf(" - %s%n", component);
        }
        System.out.println("Levels:");
        for (Logger.Level level : Logger.Level.values()) {
            System.out.printf(" - %s%n", level);
        }
    }
}
