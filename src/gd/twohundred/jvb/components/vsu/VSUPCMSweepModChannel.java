package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.CPU;

import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.log2;
import static gd.twohundred.jvb.Utils.mask;
import static gd.twohundred.jvb.Utils.testBit;

public class VSUPCMSweepModChannel extends VSUPCMChannel {
    private static final int SWEEP_DATA_START = 0x1c;

    private static final int CONTROL_ENABLE_POS = 6;
    private static final int CONTROL_REPEAT_POS = 5;
    private static final int CONTROL_FUNCTION_POS = 4;

    private static final int DATA_AMOUNT_POS = 0;
    private static final int DATA_AMOUNT_LEN = 3;
    private static final int DATA_MODIFICATION_INTERVAL_POS = 6;
    private static final int DATA_MODIFICATION_INTERVAL_LEN = 3;
    private static final int DATA_DIRECTION_POS = 3;
    private static final int DATA_MODIFICATION_BASE_INTERVAL_POS = 7;
    private final ModulationTable modulationTable;

    private boolean sweepModEnabled;
    private boolean repeatModulation;
    private Mode mode;
    private ModificationBaseInterval modificationBaseInterval;
    private byte modificationInterval;
    private SweepDirection sweepDirection;
    private byte sweepAmount;
    private long cyclesSinceLastModification;
    private int currenModulationTableIndex;

    public VSUPCMSweepModChannel(int start, PCMWaveTable[] waveTables, ModulationTable modulationTable, Logger logger) {
        super(start, waveTables, logger);
        this.modulationTable = modulationTable;
    }

    public enum ModificationBaseInterval {
        I0(10416),
        I1(1302);
        private final long modificationBaseIntervalCycles;

        ModificationBaseInterval(int modificationBaseIntervalDeciHz) {
            this.modificationBaseIntervalCycles = (CPU.CLOCK_HZ * 10L) / modificationBaseIntervalDeciHz;
        }

        public long getModificationBaseIntervalCycles() {
            return modificationBaseIntervalCycles;
        }
    }

    public enum Mode {
        Sweep,
        Modulation
    }

    public enum SweepDirection {
        Up,
        Down
    }

    @Override
    public void setByte(int address, byte value) {
        if (address == ENVELOPE_CONTROL_START) {
            sweepModEnabled = testBit(value, CONTROL_ENABLE_POS);
            repeatModulation = testBit(value, CONTROL_REPEAT_POS);
            mode = testBit(value, CONTROL_FUNCTION_POS) ? Mode.Modulation : Mode.Sweep;
        } else if (address == SWEEP_DATA_START) {
            sweepAmount = (byte) extractU(value, DATA_AMOUNT_POS, DATA_AMOUNT_LEN);
            sweepDirection = testBit(value, DATA_DIRECTION_POS) ? SweepDirection.Up : SweepDirection.Down;
            modificationInterval = (byte) extractU(value, DATA_MODIFICATION_INTERVAL_POS, DATA_MODIFICATION_INTERVAL_LEN);
            modificationBaseInterval = testBit(value, DATA_MODIFICATION_BASE_INTERVAL_POS) ? ModificationBaseInterval.I0 : ModificationBaseInterval.I1;
            return;
        }
        super.setByte(address, value);
    }

    public boolean isSweepModEnabled() {
        return sweepModEnabled;
    }

    public boolean repeatModulation() {
        return repeatModulation;
    }

    public Mode getMode() {
        return mode;
    }

    public ModificationBaseInterval getModificationBaseInterval() {
        return modificationBaseInterval;
    }

    public int getModificationInterval() {
        return modificationInterval;
    }

    public SweepDirection getSweepDirection() {
        return sweepDirection;
    }

    public int getSweepAmount() {
        return sweepAmount;
    }

    @Override
    public void tickExact(long cycles) {
        super.tickExact(cycles);
        if (modificationInterval != 0) {
            long cyclesPerModification = modificationInterval * modificationBaseInterval.getModificationBaseIntervalCycles();
            cyclesSinceLastModification += cycles;
            while (cyclesSinceLastModification > cyclesPerModification) {
                short delta;
                switch (mode) {
                    case Sweep:
                        switch (sweepDirection) {
                            case Up:
                                delta = sweepAmount;
                                break;
                            case Down:
                                delta = (short) -sweepAmount;
                                break;
                            default:
                                throw new RuntimeException();
                        }
                        break;
                    case Modulation:
                        delta = modulationTable.getSample(currenModulationTableIndex);
                        currenModulationTableIndex++;
                        currenModulationTableIndex &= mask(log2(ModulationTable.SAMPLE_COUNT));
                        break;
                    default:
                        throw new RuntimeException();
                }
                addFrequencyDelta(delta);
                cyclesSinceLastModification -= cyclesPerModification;
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        sweepModEnabled = true;
        repeatModulation = false;
        mode = Mode.Sweep;
        modificationBaseInterval = ModificationBaseInterval.I1;
        modificationInterval = 3;
        sweepDirection = SweepDirection.Down;
        sweepAmount = 2;
        cyclesSinceLastModification = 0;
        currenModulationTableIndex = 0;
    }
}
