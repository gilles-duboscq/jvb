package gd.twohundred.jvb;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class MergedScreen extends Canvas implements Screen {
    private static final int SCALE;
    private final BufferedImage bufferedImage = new BufferedImage(Screen.WIDTH, Screen.HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] imageData = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();

    static {
        int systemDpi = Toolkit.getDefaultToolkit().getScreenResolution();
        SCALE = systemDpi / 96 * 4;
    }

    public MergedScreen() {
        setSize(Screen.WIDTH * SCALE, Screen.HEIGHT * SCALE);
        setPreferredSize(getSize());
    }

    @Override
    public void update(RenderedFrame left, RenderedFrame right) {
        BufferStrategy bufferStrategy = getBufferStrategy();
        if (bufferStrategy == null) {
            createBufferStrategy(2);
            bufferStrategy = getBufferStrategy();
        }
        for (int row = 0; row < Screen.HEIGHT; row++) {
            for (int col = 0; col < Screen.WIDTH; col++) {
                imageData[col + Screen.WIDTH * row] = (left.getPixel(row, col) & 0xff) << 16 | (right.getPixel(row, col) & 0xff);
            }
        }
        Graphics g = bufferStrategy.getDrawGraphics();
        g.drawImage(bufferedImage, 0, 0, Screen.WIDTH * SCALE, Screen.HEIGHT * SCALE, null);
        g.dispose();
        bufferStrategy.show();
    }
}
