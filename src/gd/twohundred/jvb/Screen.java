package gd.twohundred.jvb;

public interface Screen {
    int WIDTH = 384;
    int HEIGHT = 224;
    double DISPLAY_REFRESH_RATE_HZ = 50.0;
    void update(RenderedFrame left, RenderedFrame right);
}
