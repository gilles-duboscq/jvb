package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.cpu.CPU;
import gd.twohundred.jvb.components.interfaces.AudioOut;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.utils.WarningMemory;

import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public class VirtualSoundUnit extends MappedModules implements ExactlyEmulable {
    public static final long CYCLES_PER_OUTPUT_SAMPLE = CPU.CLOCK_HZ / AudioOut.OUTPUT_SAMPLING_HZ;

    public static final int START = 0x01000000;
    public static final int MAPPED_SIZE = 0x01000000;

    private static final int SOUND_DISABLE_START = 0x580;
    private static final int SOUND_DISABLE_POS = 0;

    public static final int WAVE_TABLE_0_START = 0x000;
    public static final int WAVE_TABLE_1_START = 0x080;
    public static final int WAVE_TABLE_2_START = 0x100;
    public static final int WAVE_TABLE_3_START = 0x180;
    public static final int WAVE_TABLE_4_START = 0x200;
    public static final int MODULATION_TABLE_START = 0x280;

    public static final int CHANNEL_1_START = 0x400;
    public static final int CHANNEL_2_START = 0x440;
    public static final int CHANNEL_3_START = 0x480;
    public static final int CHANNEL_4_START = 0x4C0;
    public static final int CHANNEL_5_START = 0x500;
    public static final int NOISE_CHANNEL_START = 0x540;

    private final PCMWaveTable waveTable0;
    private final PCMWaveTable waveTable1;
    private final PCMWaveTable waveTable2;
    private final PCMWaveTable waveTable3;
    private final PCMWaveTable waveTable4;
    private final ModulationTable modulationTable;
    private final PCMWaveTable[] waveTables;

    private final VSUPCMChannel channel1;
    private final VSUPCMChannel channel2;
    private final VSUPCMChannel channel3;
    private final VSUPCMChannel channel4;
    private final VSUPCMSweepModChannel channel5;
    private final VSUNoiseChannel noiseChannel;
    private final DebugChannel debugChannel;
    private final VSUChannel[] channels;

    private final WarningMemory unmappedWarning;
    private final AudioOut audioOut;
    private final Logger logger;

    public VirtualSoundUnit(AudioOut audioOut, Logger logger) {
        this.audioOut = audioOut;
        this.logger = logger;
        unmappedWarning = new WarningMemory("VSU unmapped", 0, MAPPED_SIZE, logger);
        waveTable0 = new PCMWaveTable(WAVE_TABLE_0_START);
        waveTable1 = new PCMWaveTable(WAVE_TABLE_1_START);
        waveTable2 = new PCMWaveTable(WAVE_TABLE_2_START);
        waveTable3 = new PCMWaveTable(WAVE_TABLE_3_START);
        waveTable4 = new PCMWaveTable(WAVE_TABLE_4_START);
        modulationTable = new ModulationTable(MODULATION_TABLE_START);
        waveTables = new PCMWaveTable[]{waveTable0, waveTable1, waveTable2, waveTable3, waveTable4};
        channel1 = new VSUPCMChannel(CHANNEL_1_START, waveTables, logger);
        channel2 = new VSUPCMChannel(CHANNEL_2_START, waveTables, logger);
        channel3 = new VSUPCMChannel(CHANNEL_3_START, waveTables, logger);
        channel4 = new VSUPCMChannel(CHANNEL_4_START, waveTables, logger);
        channel5 = new VSUPCMSweepModChannel(CHANNEL_5_START, waveTables, modulationTable, logger);
        noiseChannel = new VSUNoiseChannel(NOISE_CHANNEL_START, logger);
        debugChannel = new DebugChannel(NOISE_CHANNEL_START, logger);
        channels = new VSUChannel[]{channel1, channel2, channel3, channel4, channel5, noiseChannel/*, debugChannel*/};
    }

    @Override
    public void reset() {
        // ??
        channel1.reset();
        channel2.reset();
        channel3.reset();
        channel4.reset();
        channel5.reset();
        noiseChannel.reset();
        waveTable0.reset();
        waveTable1.reset();
        waveTable2.reset();
        waveTable3.reset();
        waveTable4.reset();
        modulationTable.reset();
        cyclesSinceLastOutputSample = 0;
    }

    @Override
    public int getStart() {
        return START;
    }

    @Override
    public int getSize() {
        return MAPPED_SIZE;
    }

    @Override
    protected MappedMemory getMappedModule(int address) {
        assert address != SOUND_DISABLE_START;
        if (address >= NOISE_CHANNEL_START) {
            return noiseChannel;
        }
        if (address >= CHANNEL_5_START) {
            return channel5;
        }
        if (address >= CHANNEL_4_START) {
            return channel4;
        }
        if (address >= CHANNEL_3_START) {
            return channel3;
        }
        if (address >= CHANNEL_2_START) {
            return channel2;
        }
        if (address >= CHANNEL_1_START) {
            return channel1;
        }
        if (address >= MODULATION_TABLE_START + modulationTable.getSize()) {
            return unmappedWarning;
        }
        if (address >= MODULATION_TABLE_START) {
            return modulationTable;
        }
        if (address >= WAVE_TABLE_4_START) {
            return waveTable4;
        }
        if (address >= WAVE_TABLE_3_START) {
            return waveTable3;
        }
        if (address >= WAVE_TABLE_2_START) {
            return waveTable2;
        }
        if (address >= WAVE_TABLE_1_START) {
            return waveTable1;
        }
        if (address >= WAVE_TABLE_0_START) {
            return waveTable0;
        }
        return unmappedWarning;
    }

    @Override
    public void setByte(int address, byte value) {
        if (address == SOUND_DISABLE_START) {
            if (testBit(value, SOUND_DISABLE_POS)) {
                logger.debug(Logger.Component.VSU, "Sound disabled");
                for (VSUChannel channel : channels) {
                    channel.setEnabled(false);
                }
            }
            return;
        }
        super.setByte(address, value);
    }

    private long cyclesSinceLastOutputSample;

    @Override
    public void tickExact(long cycles) {
        // TODO
        for (VSUChannel channel : channels) {
            channel.tickExact(cycles);
        }
        cyclesSinceLastOutputSample += cycles;
        while (cyclesSinceLastOutputSample > CYCLES_PER_OUTPUT_SAMPLE) {
            sample();
            cyclesSinceLastOutputSample -= CYCLES_PER_OUTPUT_SAMPLE;
        }
    }

    public enum OutputChannel {
        Left,
        Right
    }

    private void sample() {
        int sampleLeft = 0;
        int sampleRight = 0;
        for (VSUChannel channel : channels) {
            if (!channel.isEnabled()) {
                continue;
            }
            int left = channel.outputSample(OutputChannel.Left);
            int right = channel.outputSample(OutputChannel.Right);
            assert (mask(AudioOut.OUTPUT_BITS) & left) == left;
            assert (mask(AudioOut.OUTPUT_BITS) & right) == right;
            sampleLeft += left;
            sampleRight += right;
        }
        sampleLeft >>= 3;
        sampleRight >>= 3;
        audioOut.update(sampleLeft, sampleRight);
    }

    public VSUChannel[] getChannels() {
        return channels;
    }

    public PCMWaveTable[] getWaveTables() {
        return waveTables;
    }

    public ModulationTable getModulationTable() {
        return modulationTable;
    }
}
