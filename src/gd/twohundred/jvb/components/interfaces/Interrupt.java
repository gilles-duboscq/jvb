package gd.twohundred.jvb.components.interfaces;

import static gd.twohundred.jvb.Utils.extractU;

public interface Interrupt extends Comparable<Interrupt> {
    enum InterruptType {
        GamePad             (0xfe00, 0xfffffe00, false, true ,  6),
        TimerZero           (0xfe10, 0xfffffe10, false, true ,  5),
        Cartridge           (0xfe20, 0xfffffe20, false, true ,  4),
        Link                (0xfe30, 0xfffffe30, false, true ,  3),
        VIP                 (0xfe40, 0xfffffe40, false, true ,  2),
        FPReservedOperand   (0xff60, 0xffffff60, true,  false, 11),
        FPOverflow          (0xff64, 0xffffff60, true,  false, 14),
        FPZeroDivision      (0xff68, 0xffffff60, true,  false, 13),
        FPInvalidOperation  (0xff70, 0xffffff60, true,  false, 12),
        ZeroDivision        (0xff80, 0xffffff80, true,  false, 10),
        IllegalOpcode       (0xff90, 0xffffff90, true,  false,  9),
        TrapNeg             (0xffa0, 0xffffffa0, false, false,  8),
        TrapPos             (0xffa0, 0xffffffb0, false, false,  8),
        AddressTrap         (0xffc0, 0xffffffc0, true,  false,  7),
        DuplexedException   (0xffd0, 0xffffffd0, true,  false,  1),
        Reset               (0xfff0, 0xfffffff0, false, false,  0);
        private final short baseExceptionCode;
        private final int handlerAddress;
        private final boolean restoreCurrentPC;
        private final boolean maskable;
        private final int priority;

        InterruptType(int baseExceptionCode, int handlerAddress, boolean restoreCurrentPC, boolean maskable, int priority) {
            this.baseExceptionCode = (short) baseExceptionCode;
            this.handlerAddress = handlerAddress;
            this.restoreCurrentPC = restoreCurrentPC;
            this.maskable = maskable;
            this.priority = priority;
        }

        public short getBaseExceptionCode() {
            return baseExceptionCode;
        }

        public int getHandlerAddress() {
            return handlerAddress;
        }

        public boolean isRestoreCurrentPC() {
            return restoreCurrentPC;
        }

        public boolean isMaskable() {
            return maskable;
        }

        public int getInterruptLevel() {
            return extractU(baseExceptionCode, 4 , 4);
        }

        private int getPriority() {
            return priority;
        }
    }
    InterruptType getType();
    short getExceptionCode();

    Interrupt getNext();
    void setNext(Interrupt i);

    default int compareTo(Interrupt o) {
        return Integer.compare(this.getType().getPriority(), o.getType().getPriority());
    }
}
