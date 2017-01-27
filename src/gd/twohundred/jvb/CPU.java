package gd.twohundred.jvb;

import java.util.Arrays;

import static gd.twohundred.jvb.Instructions.BCOND_BE;
import static gd.twohundred.jvb.Instructions.BCOND_BL;
import static gd.twohundred.jvb.Instructions.BCOND_BNE;
import static gd.twohundred.jvb.Instructions.BCOND_BR;
import static gd.twohundred.jvb.Instructions.COND_LEN;
import static gd.twohundred.jvb.Instructions.COND_POS;
import static gd.twohundred.jvb.Instructions.DISP26_LEN;
import static gd.twohundred.jvb.Instructions.DISP9_LEN;
import static gd.twohundred.jvb.Instructions.DISP9_POS;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX_LEN;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX_POS;
import static gd.twohundred.jvb.Instructions.IMM5_LEN;
import static gd.twohundred.jvb.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.Instructions.OP_ADD_IMM;
import static gd.twohundred.jvb.Instructions.OP_CMP_REG;
import static gd.twohundred.jvb.Instructions.OP_JAL;
import static gd.twohundred.jvb.Instructions.OP_JMP;
import static gd.twohundred.jvb.Instructions.OP_JR;
import static gd.twohundred.jvb.Instructions.OP_LDB;
import static gd.twohundred.jvb.Instructions.OP_LDSR;
import static gd.twohundred.jvb.Instructions.OP_LDW;
import static gd.twohundred.jvb.Instructions.OP_MOVEA;
import static gd.twohundred.jvb.Instructions.OP_MOVHI;
import static gd.twohundred.jvb.Instructions.OP_MOV_IMM;
import static gd.twohundred.jvb.Instructions.OP_MOV_REG;
import static gd.twohundred.jvb.Instructions.OP_OUTW;
import static gd.twohundred.jvb.Instructions.OP_SEI;
import static gd.twohundred.jvb.Instructions.OP_STB;
import static gd.twohundred.jvb.Instructions.OP_STW;
import static gd.twohundred.jvb.Instructions.REG1_LEN;
import static gd.twohundred.jvb.Instructions.REG1_POS;
import static gd.twohundred.jvb.Instructions.REG2_LEN;
import static gd.twohundred.jvb.Instructions.REG2_POS;
import static gd.twohundred.jvb.Utils.extractS;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.testBits;
import static gd.twohundred.jvb.Utils.toBinary;
import static java.lang.Math.abs;

public class CPU implements Emulable, Resetable {
    public static final long CLOCK_HZ = 20_000_000L;

    private static final int EIPC_REG = 0;
    private static final int EIPSW_REG = 1;
    private static final int FEPC_REG = 2;
    private static final int FEPSW_REG = 3;
    private static final int ECR_REG = 4;
    private static final int PSW_REG = 5;
    private static final int PIR_REG = 6;
    private static final int TKCW_REG = 7;
    private static final int CHCW_REG = 24;
    private static final int ADTRE_REG = 25;

    private static final int LINK_REG = 31;


    private static final int REGISTER_COUNT = 32;
    private final int[] registers = new int[REGISTER_COUNT];
    private int pc;
    private final ProgramStatusWord psw = new ProgramStatusWord();
    private int eipc;
    private int eipsw;
    private int fepc;
    private int fepsw;
    private int ecr;
    private int adtre;
    private int chcw;
    private static final int PIR = 0x810 << 4;
    private static final int TKCW = 0x000000E0;

    private final Bus bus;

    public CPU(Bus bus) {
        this.bus = bus;
    }

    private void setRegister(int r, int value) {
        if (r != 0) {
            registers[r] = value;
        }
    }

    private void setSystemRegister(int r, int value) {
        switch (r) {
            case EIPC_REG:
                eipc = value;
                break;
            case EIPSW_REG:
                eipsw = value;
                break;
            case FEPC_REG:
                fepc = value;
                break;
            case FEPSW_REG:
                fepsw = value;
                break;
            case PSW_REG:
                psw.set(value);
                break;
            case CHCW_REG:
                processCacheControlCommand(value);
                break;
            case ADTRE_REG:
                adtre = value;
                break;
            case PIR_REG:
            case TKCW_REG:
            case ECR_REG:
                System.err.printf("warning: setting read-only system reg %d%n", r);
                break;
            default:
                System.err.printf("warning: setting unknown system reg %d%n", r);
                break;
        }
    }

    private String setSystemRegisterName(int r) {
        switch (r) {
            case EIPC_REG:
                return "EIPC";
            case EIPSW_REG:
                return "EIPSW";
            case FEPC_REG:
                return "FEPC";
            case FEPSW_REG:
                return "FEPSW";
            case PSW_REG:
                return "PSW";
            case CHCW_REG:
                return "CHCW";
            case ADTRE_REG:
                return "ADTRE";
            case PIR_REG:
                return "PIR";
            case TKCW_REG:
                return "TKCW";
            case ECR_REG:
                return "ECR";
            default:
                return "???" + r;
        }
    }

    private int getRegister(int r) {
        return registers[r];
    }

    private static final int CHCW_ICE_POS = 1;
    private void processCacheControlCommand(int value) {
        chcw = value & intBit(CHCW_ICE_POS);
        if (DEBUG_CC) {
            System.out.printf("ignoring: cache control 0b%s%n", toBinary(value, 32));
        }
    }

    @Override
    public void reset() {
        pc = 0xFFFFFFF0;
        psw.set(0x00008000);
        ecr = 0x0000FFF0;

        Arrays.fill(registers, 1, REGISTER_COUNT - 1, 0xdeadbeef);
        eipc = 0xdeadbeef;
        eipsw = 0xdeadbeef;
        fepc = 0xdeadbeef;
        fepsw = 0xdeadbeef;
        adtre = 0xdeadbeef;
        chcw = 0xdeadbeef;
    }

    public static final boolean DEBUG_INST = false;
    public static final boolean DEBUG_CC = false;

    @Override
    public int tick(int targetCycles) {
        int first = bus.getHalfWord(pc);
        int nextPC = pc + 2;
        int cycles = 1;
        if (testBits(first, FORMAT_III_PREFIX, FORMAT_III_PREFIX_POS, FORMAT_III_PREFIX_LEN)) {
            int cond = extractU(first, COND_POS, COND_LEN);
            int disp9 = extractS(first, DISP9_POS, DISP9_LEN);
            boolean branchTaken;
            switch (cond) {
                case BCOND_BR: {
                    branchTaken = true;
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x BR     %s%05X", pc, disp9 > 0 ? '+' : '-', abs(disp9)));
                    }
                    break;
                }
                case BCOND_BNE: {
                    branchTaken = !psw.getZ();
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x BNE    %s%05X", pc, disp9 > 0 ? '+' : '-', abs(disp9)));
                    }
                    break;
                }
                case BCOND_BE: {
                    branchTaken = psw.getZ();
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x BE     %s%05X", pc, disp9 > 0 ? '+' : '-', abs(disp9)));
                    }
                    break;
                }
                case BCOND_BL: {
                    branchTaken = psw.getCY();
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x BL     %s%05X", pc, disp9 > 0 ? '+' : '-', abs(disp9)));
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Unknown bcond: 0b" + toBinary(cond, COND_LEN));
            }
            if (branchTaken) {
                cycles = 3;
                nextPC = pc + disp9;
            }
        } else {
            int opcode = extractU(first, OPCODE_POS, OPCODE_LEN);
            int reg1 = extractU(first, REG1_POS, REG1_LEN);
            int reg2 = extractU(first, REG2_POS, REG2_LEN);
            int imm5 = reg1;
            switch (opcode) {
                case OP_MOVEA: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, getRegister(reg1) + signExtend(second, 16));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x MOVEA  %X, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_MOVHI: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, getRegister(reg1) + (second << 16));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x MOVHI  %X, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_JMP: {
                    cycles = 3;
                    nextPC = getRegister(reg1);
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x JMP    r%d", pc, reg1));
                    }
                    break;
                }
                case OP_ADD_IMM: {
                    int intValue = add(getRegister(reg2), signExtend(imm5, IMM5_LEN));
                    setRegister(reg2, intValue);
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x ADD    %X, r%d, r%d", pc, imm5, reg2, reg2));
                    }
                    break;
                }
                case OP_LDSR: {
                    setSystemRegister(imm5, getRegister(reg2));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x LDSR   r%d, %s", pc, reg2, setSystemRegisterName(imm5)));
                    }
                    break;
                }
                case OP_SEI: {
                    psw.setInterruptDisable(true);
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x SEI", pc));
                    }
                    break;
                }
                case OP_MOV_IMM: {
                    setRegister(reg2, signExtend(imm5, IMM5_LEN));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x MOV    %X, r%d", pc, imm5, reg2));
                    }
                    break;
                }
                case OP_MOV_REG: {
                    setRegister(reg2, getRegister(reg1));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x MOV    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_STB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setByte(getRegister(reg1) + disp16, (byte) getRegister(reg2));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x ST.B   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_STW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setWord(getRegister(reg1) + disp16, (byte) getRegister(reg2));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x ST.W   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_OUTW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setWord(getRegister(reg1) + disp16, (byte) getRegister(reg2));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x OUT.W  %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_LDB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, signExtend(bus.getByte(getRegister(reg1) + disp16), Byte.SIZE));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x LD.B   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_LDW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, bus.getWord(getRegister(reg1) + disp16));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x LD.W   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_JAL: {
                    int second = bus.getHalfWord(pc + 2);
                    int disp26 = signExtend(second | (reg1 << (16 + REG1_POS)) | (reg2 << (16 + REG2_POS)), DISP26_LEN);
                    nextPC = pc + disp26;
                    cycles = 3;
                    setRegister(LINK_REG, pc + 4);
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x JAL    %s%07X", pc, disp26 > 0 ? '+' : '-', abs(disp26)));
                    }
                    break;
                }
                case OP_JR: {
                    int second = bus.getHalfWord(pc + 2);
                    int disp26 = signExtend(second | (reg1 << (16 + REG1_POS)) | (reg2 << (16 + REG2_POS)), DISP26_LEN);
                    nextPC = pc + disp26;
                    cycles = 3;
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x JR     %s%07X", pc, disp26 > 0 ? '+' : '-', abs(disp26)));
                    }
                    break;
                }
                case OP_CMP_REG: {
                    sub(getRegister(reg2), getRegister(reg1));
                    if (DEBUG_INST) {
                        System.out.println(String.format("%08x CMP    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                default:
                    throw new RuntimeException(String.format("Unknown opcode: 0b%s @ %08X", toBinary(opcode, OPCODE_LEN), pc));
            }
        }
        pc = nextPC;
        return cycles;
    }

    private int add(int a, int b) {
        long value = a + b;
        int intValue = (int) value;
        boolean carry = (value & 0xffff_ffffL) != 0;
        boolean zero = intValue == 0;
        boolean sign = intValue < 0;
        boolean overflow = ((a ^ intValue) & (b ^ intValue)) < 0;
        psw.setZeroSignOveflowCarry(zero, sign, overflow, carry);
        return intValue;
    }

    private int sub(int a, int b) {
        long value = a - b;
        int intValue = (int) value;
        boolean carry = (value & 0xffff_ffffL) != 0;
        boolean zero = intValue == 0;
        boolean sign = intValue < 0;
        boolean overflow = ((a ^ b) & (a ^ intValue)) < 0;
        psw.setZeroSignOveflowCarry(zero, sign, overflow, carry);
        return intValue;
    }
}
