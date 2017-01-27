package gd.twohundred.jvb;

import gd.twohundred.jvb.components.CPU;
import gd.twohundred.jvb.components.CartridgeROM;
import gd.twohundred.jvb.components.VirtualBoy;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.LockSupport;

public class Main {
    public static final long NANOS_PER_SECOND = 1_000_000_000L;
    private Path cartridgePath;

    public static void main(String... args) throws IOException {
        Main m = new Main();
        m.cartridgePath = Paths.get(args[0]);
        m.run();
    }

    private void run() throws IOException {
        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
        CartridgeROM rom = new CartridgeROM(cartridgePath);
        System.out.println(" Title: " + rom.getGameTitle());
        System.out.println(" Maker code: " + rom.getMakerCode());
        System.out.println(" Game code: " + rom.getGameCode());
        System.out.println(" Version: 1." + rom.getGameVersion());
        VirtualBoy virtualBoy = new VirtualBoy(mainWindow.getScreen(), rom);
        virtualBoy.reset();
        long t = System.nanoTime();
        long cycles = 0;
        boolean tooSlow = false;
        long iterations = 0;
        while (!Thread.interrupted() && mainWindow.isOpen()) {
            long newT = System.nanoTime();
            long dt = newT - t;
            long dCycles = dt * CPU.CLOCK_HZ / NANOS_PER_SECOND;
            long missingCycles = dCycles - cycles;

            if (missingCycles > CPU.CLOCK_HZ / Screen.DISPLAY_REFRESH_RATE_HZ) {
                tooSlow = true;
            }
            int cyclesDone = virtualBoy.tick((int) missingCycles);

            if (cycles > CPU.CLOCK_HZ) {
                if (cycles != 0) {
                    System.out.printf("%,d Hz  %,d CPItr%s%n", cycles * NANOS_PER_SECOND / dt, cycles / iterations, tooSlow ? " SLOW!" : "");
                }
                t = System.nanoTime();
                cycles = 0;
                iterations = 0;
                tooSlow = false;
            }
            cycles += cyclesDone;
            iterations++;
            LockSupport.parkNanos(100000);
            /*try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }*/
        }
        System.out.println("Bye!");
        System.exit(0);
    }
}
