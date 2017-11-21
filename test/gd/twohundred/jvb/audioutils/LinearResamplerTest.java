package gd.twohundred.jvb.audioutils;

import gd.twohundred.jvb.components.interfaces.AudioOut;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class LinearResamplerTest {

    private static class CollectingOut implements AudioOut {
        private final int[] left;
        private final int[] right;
        private int index;

        CollectingOut(int size) {
            left = new int[size];
            right = new int[size];
        }

        @Override
        public void update(int left, int right) {
            this.left[index] = left;
            this.right[index] = right;
            index++;
        }

        int[] getLeft() {
            return Arrays.copyOf(left, index);
        }

        int[] getRight() {
            return Arrays.copyOf(right, index);
        }
    }

    @Test
    public void sameRate() {
        LinearResampler resampler = new LinearResampler(42, 42);
        CollectingOut out = new CollectingOut(10);
        resampler.in(5, 0, out);
        resampler.in(10, 10, out);
        resampler.in(0, 5, out);

        assertArrayEquals(new int[]{5,10,0}, out.getLeft());
        assertArrayEquals(new int[]{0,10,5}, out.getRight());
    }

    @Test
    public void doubleRate() {
        LinearResampler resampler = new LinearResampler(42, 84);
        CollectingOut out = new CollectingOut(10);
        resampler.in(8 , 0, out);
        resampler.in(4 , 4, out);
        resampler.in(0 , 8, out);

        assertArrayEquals(new int[]{8,6,4,2,0}, out.getLeft());
        assertArrayEquals(new int[]{0,2,4,6,8}, out.getRight());
    }

    @Test
    public void halfRate() {
        LinearResampler resampler = new LinearResampler(42, 21);
        CollectingOut out = new CollectingOut(10);
        resampler.in(8 , 0, out);
        resampler.in(4 , 4, out);
        resampler.in(0 , 8, out);
        resampler.in(8 , 0, out);

        assertArrayEquals(new int[]{8,0}, out.getLeft());
        assertArrayEquals(new int[]{0,8}, out.getRight());
    }

    @Test
    public void typical() {
        LinearResampler resampler = new LinearResampler(41700, 44100);
        CollectingOut out = new CollectingOut(50000);
        for (int i = 0; i < 41700; i++) {
            resampler.in(0 , 0, out);
        }

        assertArrayEquals(new int[44100], out.getLeft());
        assertArrayEquals(new int[44100], out.getRight());
    }

    @Test
    public void upSample() {
        LinearResampler resampler = new LinearResampler(2, 3);
        CollectingOut out = new CollectingOut(10);
        resampler.in(8 , 0, out);
        resampler.in(4 , 4, out);
        resampler.in(0 , 8, out);
        resampler.in(0 , 8, out);

        assertArrayEquals(new int[]{8,6,4,2,0}, out.getLeft());
        assertArrayEquals(new int[]{0,2,4,6,8}, out.getRight());
    }
}