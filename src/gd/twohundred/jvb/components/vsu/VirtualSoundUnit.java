package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.utils.MappedModules;
import gd.twohundred.jvb.components.utils.WarningMemory;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.testBit;

public class VirtualSoundUnit extends MappedModules implements ExactlyEmulable {
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

    private final VSUPCMChannel channel1 = new VSUPCMChannel(CHANNEL_1_START);
    private final VSUPCMChannel channel2 = new VSUPCMChannel(CHANNEL_2_START);
    private final VSUPCMChannel channel3 = new VSUPCMChannel(CHANNEL_3_START);
    private final VSUPCMChannel channel4 = new VSUPCMChannel(CHANNEL_4_START);
    private final VSUPCMSweepModChannel channel5 = new VSUPCMSweepModChannel(CHANNEL_5_START);
    private final VSUNoiseChannel noiseChannel = new VSUNoiseChannel(NOISE_CHANNEL_START);

    private final PCMWaveTable waveTable0 = new PCMWaveTable(WAVE_TABLE_0_START);
    private final PCMWaveTable waveTable1 = new PCMWaveTable(WAVE_TABLE_1_START);
    private final PCMWaveTable waveTable2 = new PCMWaveTable(WAVE_TABLE_2_START);
    private final PCMWaveTable waveTable3 = new PCMWaveTable(WAVE_TABLE_3_START);
    private final PCMWaveTable waveTable4 = new PCMWaveTable(WAVE_TABLE_4_START);
    private final ModulationTable modulationTable = new ModulationTable(MODULATION_TABLE_START);

    private final WarningMemory unmappedWarning;

    private boolean soundEnabled;

    public VirtualSoundUnit(Logger logger) {
        unmappedWarning = new WarningMemory("VSU unmapped", 0, MAPPED_SIZE, logger);
    }

    @Override
    public void reset() {
        // ??
        soundEnabled = false;
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
            soundEnabled = !testBit(value, SOUND_DISABLE_POS);
            return;
        }
        super.setByte(address, value);
    }

    @Override
    public void tickExact(long cycles) {
        // TODO
    }
}
