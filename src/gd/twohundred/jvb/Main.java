package gd.twohundred.jvb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Main {
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
        long ticks = 0;
        long tt = ticks;
        long t = System.nanoTime();
        while (!Thread.interrupted() && mainWindow.isOpen()) {
            virtualBoy.tick();
            ticks++;
            if ((ticks & 0xfffffffL) == 0) {
                long nt = System.nanoTime();
                long dt = nt - t;
                long dtt = ticks - tt;
                System.out.printf("%,d ticks in %,dms: %,dtps%n", dtt, NANOSECONDS.toMillis(dt), 1_000_000_000L * dtt / dt);
                tt = ticks;
                t = nt;
            }
            /*try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                break;
            }*/
        }
        System.out.println("Bye!");
        System.exit(0);
    }
}
