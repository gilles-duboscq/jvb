package gd.twohundred.jvb.components.interfaces;

import gd.twohundred.jvb.RenderedFrame;

public interface Screen {
    int WIDTH = 384;
    int HEIGHT = 224;
    long DISPLAY_REFRESH_RATE_HZ = 50;
    void update(RenderedFrame left, RenderedFrame right);
}
