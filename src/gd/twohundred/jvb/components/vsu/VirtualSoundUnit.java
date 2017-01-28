package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.ExactlyEmulable;
import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.BusError.Reason.Unimplemented;
import static gd.twohundred.jvb.BusError.Reason.Unmapped;
import static gd.twohundred.jvb.Utils.testBit;

public class VirtualSoundUnit implements ExactlyEmulable, WriteOnlyMemory {
    public static final int START = 0x01000000;
    public static final int MAPPED_SIZE = 0x01000000;

    private static final int SOUND_DISABLE_ADDR = 0x580;
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
    private final VSUPCMChannel channel5 = new VSUPCMChannel(CHANNEL_5_START);
    private final VSUNoiseChannel noiseChannel = new VSUNoiseChannel(NOISE_CHANNEL_START);

    private final PCMWaveTable waveTable0 = new PCMWaveTable(WAVE_TABLE_0_START);
    private final PCMWaveTable waveTable1 = new PCMWaveTable(WAVE_TABLE_1_START);
    private final PCMWaveTable waveTable2 = new PCMWaveTable(WAVE_TABLE_2_START);
    private final PCMWaveTable waveTable3 = new PCMWaveTable(WAVE_TABLE_3_START);
    private final PCMWaveTable waveTable4 = new PCMWaveTable(WAVE_TABLE_4_START);
    private final ModulationTable modulationTable = new ModulationTable(MODULATION_TABLE_START);

    private boolean soundEnabled;

    @Override
    public void reset() {
        // ??
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
    public void setByte(int address, byte value) {
        if (address > SOUND_DISABLE_ADDR) {
            throw new BusError(address, Unmapped);
        }
        if (address == SOUND_DISABLE_ADDR) {
            soundEnabled = !testBit(value, SOUND_DISABLE_POS);
            return;
        }
        if (address >= NOISE_CHANNEL_START) {
            noiseChannel.setByte(address - NOISE_CHANNEL_START, value);
            return;
        }
        if (address >= CHANNEL_5_START) {
            channel1.setByte(address - CHANNEL_5_START, value);
            return;
        }
        if (address >= CHANNEL_4_START) {
            channel1.setByte(address - CHANNEL_4_START, value);
            return;
        }
        if (address >= CHANNEL_3_START) {
            channel1.setByte(address - CHANNEL_3_START, value);
            return;
        }
        if (address >= CHANNEL_2_START) {
            channel1.setByte(address - CHANNEL_2_START, value);
            return;
        }
        if (address >= CHANNEL_1_START) {
            channel1.setByte(address - CHANNEL_1_START, value);
            return;
        }
        if (address >= CHANNEL_1_START) {
            channel1.setByte(address - CHANNEL_1_START, value);
            return;
        }
        if (address >= 0x300) {
            throw new BusError(address, Unimplemented);
        }
        if (address >= MODULATION_TABLE_START) {
            modulationTable.setByte(address - MODULATION_TABLE_START, value);
            return;
        }
        if (address >= WAVE_TABLE_4_START) {
            waveTable4.setByte(address - WAVE_TABLE_4_START, value);
            return;
        }
        if (address >= WAVE_TABLE_3_START) {
            waveTable3.setByte(address - WAVE_TABLE_3_START, value);
            return;
        }
        if (address >= WAVE_TABLE_2_START) {
            waveTable2.setByte(address - WAVE_TABLE_2_START, value);
            return;
        }
        if (address >= WAVE_TABLE_1_START) {
            waveTable1.setByte(address - WAVE_TABLE_1_START, value);
            return;
        }
        if (address >= WAVE_TABLE_0_START) {
            waveTable0.setByte(address - WAVE_TABLE_0_START, value);
            return;
        }
        throw new BusError(address, Unmapped);
    }

    @Override
    public void tickExact(int cycles) {
        // TODO
    }
}
