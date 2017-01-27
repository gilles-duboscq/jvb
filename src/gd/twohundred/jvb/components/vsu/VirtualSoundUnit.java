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

    private final VSUChannel channel1 = new VSUChannel(0x400);
    private final VSUChannel channel2 = new VSUChannel(0x440);
    private final VSUChannel channel3 = new VSUChannel(0x480);
    private final VSUChannel channel4 = new VSUChannel(0x4c0);
    private final VSUChannel channel5 = new VSUChannel(0x500);

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
        if (address < 0x300) {
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
        if (address == SOUND_DISABLE_ADDR) {
            soundEnabled = !testBit(value, SOUND_DISABLE_POS);
            return;
        }
        throw new BusError(address, Unimplemented);
    }

    @Override
    public void tickExact(int cycles) {
        // TODO
    }
}
