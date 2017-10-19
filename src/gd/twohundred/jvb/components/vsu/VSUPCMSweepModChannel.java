package gd.twohundred.jvb.components.vsu;

import gd.twohundred.jvb.Logger;

import static gd.twohundred.jvb.Utils.extractU;
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

    private boolean sweepModEnabled;
    private boolean repeatModulation;
    private Mode mode;
    private ModificationBaseInterval modificationBaseInterval;
    private byte modificationInterval;
    private SweepDirection sweepDirection;
    private byte sweepAmount;

    public VSUPCMSweepModChannel(int start, PCMWaveTable[] waveTables, Logger logger) {
        super(start, waveTables, logger);
    }

    public enum ModificationBaseInterval {
        I0(10416),
        I1(1302);
        private final int modificationBasewIntervalDeciHz;

        ModificationBaseInterval(int modificationBasewIntervalDeciHz) {
            this.modificationBasewIntervalDeciHz = modificationBasewIntervalDeciHz;
        }

        public int getModificationBasewIntervalDeciHz() {
            return modificationBasewIntervalDeciHz;
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
    public void reset() {
        super.reset();
        sweepModEnabled = true;
        repeatModulation = false;
        mode = Mode.Sweep;
        modificationBaseInterval = ModificationBaseInterval.I1;
        modificationInterval = 3;
        sweepDirection = SweepDirection.Down;
        sweepAmount = 2;

    }
}
