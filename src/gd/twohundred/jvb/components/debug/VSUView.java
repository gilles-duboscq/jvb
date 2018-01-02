package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.components.Debugger;
import gd.twohundred.jvb.components.debug.boxes.Box;
import gd.twohundred.jvb.components.debug.boxes.SimpleColumn;
import gd.twohundred.jvb.components.debug.boxes.Table;
import gd.twohundred.jvb.components.debug.boxes.VerticalBoxes;
import gd.twohundred.jvb.components.vsu.ModulationTable;
import gd.twohundred.jvb.components.vsu.PCMWaveTable;
import gd.twohundred.jvb.components.vsu.VSUChannel;
import gd.twohundred.jvb.components.vsu.VSUNoiseChannel;
import gd.twohundred.jvb.components.vsu.VSUPCMChannel;
import gd.twohundred.jvb.components.vsu.VSUPCMSweepModChannel;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit.OutputChannel;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gd.twohundred.jvb.components.vsu.VirtualSoundUnit.CHANNEL_1_START;

public class VSUView implements View {
    private final Debugger debugger;
    private final ChannelAttributesTable channelTable;
    private final VerticalBoxes verticalBoxes;

    public VSUView(Debugger debugger) {
        this.debugger = debugger;
        channelTable = new ChannelAttributesTable(debugger.getTerminal());
        List<Box> boxes = new ArrayList<>();
        boxes.add(channelTable);
        for (int i = 0; i < 5; i++) {
            boxes.add(new PCMData(i));
        }
        boxes.add(new ModulationData());
        verticalBoxes = new VerticalBoxes("VIP", boxes);
    }

    @Override
    public Cursor getCursorPosition(Size size) {
        return null;
    }

    @Override
    public String getTitle() {
        return "VSU";
    }

    @Override
    public void appendLines(List<AttributedString> lines, int width, int height) {
        verticalBoxes.appendLines(lines, width, height);
    }

    @Override
    public KeyMap<Runnable> getKeyMap() {
        return null;
    }

    @Override
    public char getAccelerator() {
        return 's';
    }

    private abstract class VSUChannelColumn extends SimpleColumn<VSUChannel> {
        public VSUChannelColumn(String name, int width) {
            super(name, width);
        }

        @Override
        protected VSUChannel[] getObjects() {
            return debugger.getVsu().getChannels();
        }
    }

    private class ChannelIDColumn extends VSUChannelColumn {
        public ChannelIDColumn() {
            super("ID", 2);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            asb.append(Integer.toString(((channel.getStart() - CHANNEL_1_START) / 0x40) + 1));
        }
    }

    private class ChannelTypeColumn extends VSUChannelColumn {
        public ChannelTypeColumn() {
            super("Type", 4);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            if (channel instanceof VSUPCMSweepModChannel) {
                asb.append("S/M");
            } else if (channel instanceof VSUPCMChannel) {
                asb.append("PCM");
            } else if (channel instanceof VSUNoiseChannel) {
                asb.append("Noiz");
            }
        }
    }

    private class ChannelEnabledColumn extends VSUChannelColumn {
        public ChannelEnabledColumn() {
            super("E", 1);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            asb.append(channel.isEnabled() ? '✔' : '✗');
        }
    }

    private class ChannelDurationColumn extends VSUChannelColumn {
        public ChannelDurationColumn() {
            super("D (ms)", 7);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            AttributedStyle style = channel.useDuration() ? AttributedStyle.DEFAULT : AttributedStyle.DEFAULT.faint();
            double durationMS = (double) channel.getDurationCycles() * 1000.0 / CPU.CLOCK_HZ;
            asb.append(String.format("%.1f", durationMS), style);
        }
    }

    private class ChannelWaveIndexColumn extends VSUChannelColumn {
        public ChannelWaveIndexColumn() {
            super("WI", 2);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            if (channel instanceof VSUPCMChannel) {
                VSUPCMChannel pcmChannel = (VSUPCMChannel) channel;
                asb.append(Integer.toString(pcmChannel.getWaveIndex()));
            }
        }
    }

    private class ChannelFrequencyColumn extends VSUChannelColumn {
        public ChannelFrequencyColumn() {
            super("f (Hz)", 7);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            asb.append(Long.toString(CPU.CLOCK_HZ / channel.getCyclesPerSample()));
        }
    }

    private class ChannelVolumeColumn extends VSUChannelColumn {
        private final OutputChannel leftRight;

        public ChannelVolumeColumn(OutputChannel leftRight) {
            super("V " + leftRight.name().charAt(0), 3);
            this.leftRight = leftRight;
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            int v;
            switch (leftRight) {
                case Right:
                    v = channel.getVolumeRight();
                    break;
                case Left:
                    v = channel.getVolumeLeft();
                    break;
                default:
                    throw new RuntimeException();
            }
            asb.append(Integer.toString(v));
        }
    }

    private class ChannelEnvelopeColumn extends VSUChannelColumn {
        public ChannelEnvelopeColumn() {
            super("env (ms)", 8);
        }

        @Override
        protected void cell(AttributedStringBuilder asb, VSUChannel channel) {
            AttributedStyle style = channel.isEnvelopeEnabled() ? AttributedStyle.DEFAULT : AttributedStyle.DEFAULT.faint();
            double durationMS = (double) channel.getCyclesPerEnvelopeStep() * 1000.0 / CPU.CLOCK_HZ;
            asb.append(String.format("%.1f", durationMS), style);
        }
    }

    private class ChannelAttributesTable extends Table {
        private ChannelAttributesTable(Terminal terminal) {
            super("Channels", Arrays.asList(new ChannelIDColumn(), new ChannelTypeColumn(), new ChannelEnabledColumn(), new ChannelDurationColumn(), new ChannelWaveIndexColumn(), new ChannelFrequencyColumn(), new ChannelVolumeColumn(OutputChannel.Left), new ChannelVolumeColumn(OutputChannel.Right), new ChannelEnvelopeColumn()), terminal);
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        @Override
        public int minHeight() {
            return 6 + 2;
        }
    }

    private abstract class DataBox implements Box {
        private final int BLOCK_STATES = 8;
        private final char BLOCK_STATE_ONE = '\u2581';

        @Override
        public int minWidth() {
            return sampleCount();
        }

        @Override
        public boolean fixedWidth() {
            return true;
        }

        @Override
        public int minHeight() {
            return numBlocks() + 1;
        }

        @Override
        public boolean fixedHeight() {
            return true;
        }

        protected int numBlocks() {
            return maxValue() / BLOCK_STATES;
        }

        protected abstract int maxValue();

        protected abstract int sampleCount();

        protected abstract byte sample(int i);

        @Override
        public void line(AttributedStringBuilder asb, int line, int width, int height) {
            int y = numBlocks() - line - 1;
            if (y < 0) {
                for (int i = 0; i < sampleCount() && i < width; i++) {
                    asb.append('\'');
                }
            } else {
                for (int i = 0; i < sampleCount() && i < width; i++) {
                    byte sample = sample(i);
                    int blockValue = sample - y * BLOCK_STATES;
                    char blockChar;
                    if (blockValue <= 0) {
                        blockChar = ' ';
                    } else if (blockValue < 8) {
                        blockChar = (char) (BLOCK_STATE_ONE + (blockValue - 1));
                    } else {
                        blockChar = (char) (BLOCK_STATE_ONE + 7);
                    }
                    asb.append(blockChar);
                }
            }
        }
    }

    private class PCMData extends DataBox {
        private final int index;

        private PCMData(int index) {
            this.index = index;
        }

        @Override
        public String name() {
            return "PCM Wave Table " + index;
        }

        @Override
        protected int maxValue() {
            return (1 << PCMWaveTable.SAMPLE_BIT_WIDTH) - 1;
        }

        @Override
        protected int sampleCount() {
            return PCMWaveTable.SAMPLE_COUNT;
        }

        @Override
        protected byte sample(int i) {
            return debugger.getVsu().getWaveTables()[index].getSample(i);
        }
    }

    private class ModulationData extends DataBox {

        @Override
        public String name() {
            return "Modulation Table ";
        }

        @Override
        protected int maxValue() {
            return (1 << ModulationTable.SAMPLE_BIT_WIDTH) - 1;
        }

        @Override
        protected int sampleCount() {
            return ModulationTable.SAMPLE_COUNT;
        }

        @Override
        protected byte sample(int i) {
            return debugger.getVsu().getModulationTable().getSample(i);
        }
    }
}
