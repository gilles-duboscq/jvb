package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.Emulable;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.Resetable;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;

import static gd.twohundred.jvb.Utils.insert;
import static gd.twohundred.jvb.Utils.signStr;
import static gd.twohundred.jvb.Utils.testBit;
import static gd.twohundred.jvb.Utils.zeroExtend;
import static gd.twohundred.jvb.components.Instructions.BCOND_BE;
import static gd.twohundred.jvb.components.Instructions.BCOND_BGE;
import static gd.twohundred.jvb.components.Instructions.BCOND_BGT;
import static gd.twohundred.jvb.components.Instructions.BCOND_BH;
import static gd.twohundred.jvb.components.Instructions.BCOND_BL;
import static gd.twohundred.jvb.components.Instructions.BCOND_BLE;
import static gd.twohundred.jvb.components.Instructions.BCOND_BN;
import static gd.twohundred.jvb.components.Instructions.BCOND_BNE;
import static gd.twohundred.jvb.components.Instructions.BCOND_BNH;
import static gd.twohundred.jvb.components.Instructions.BCOND_BNL;
import static gd.twohundred.jvb.components.Instructions.BCOND_BP;
import static gd.twohundred.jvb.components.Instructions.BCOND_BR;
import static gd.twohundred.jvb.components.Instructions.BCOND_BLT;
import static gd.twohundred.jvb.components.Instructions.BCOND_BV;
import static gd.twohundred.jvb.components.Instructions.BCOND_NOP;
import static gd.twohundred.jvb.components.Instructions.COND_LEN;
import static gd.twohundred.jvb.components.Instructions.COND_POS;
import static gd.twohundred.jvb.components.Instructions.DISP26_LEN;
import static gd.twohundred.jvb.components.Instructions.DISP9_LEN;
import static gd.twohundred.jvb.components.Instructions.DISP9_POS;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX_LEN;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX_POS;
import static gd.twohundred.jvb.components.Instructions.OP_SETF;
import static gd.twohundred.jvb.components.Instructions.OP_SHR_REG;
import static gd.twohundred.jvb.components.Instructions.SUBOP_ADDF_S;
import static gd.twohundred.jvb.components.Instructions.IMM5_LEN;
import static gd.twohundred.jvb.components.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.components.Instructions.OP_ADDI;
import static gd.twohundred.jvb.components.Instructions.OP_ADD_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_ADD_REG;
import static gd.twohundred.jvb.components.Instructions.OP_AND_REG;
import static gd.twohundred.jvb.components.Instructions.OP_AND_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_CLI;
import static gd.twohundred.jvb.components.Instructions.OP_CMP_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_CMP_REG;
import static gd.twohundred.jvb.components.Instructions.OP_DIV;
import static gd.twohundred.jvb.components.Instructions.OP_DIVU;
import static gd.twohundred.jvb.components.Instructions.OP_SUBOP;
import static gd.twohundred.jvb.components.Instructions.OP_ILL_1;
import static gd.twohundred.jvb.components.Instructions.OP_INB;
import static gd.twohundred.jvb.components.Instructions.OP_INH;
import static gd.twohundred.jvb.components.Instructions.OP_INW;
import static gd.twohundred.jvb.components.Instructions.OP_JAL;
import static gd.twohundred.jvb.components.Instructions.OP_JMP;
import static gd.twohundred.jvb.components.Instructions.OP_JR;
import static gd.twohundred.jvb.components.Instructions.OP_LDB;
import static gd.twohundred.jvb.components.Instructions.OP_LDH;
import static gd.twohundred.jvb.components.Instructions.OP_LDSR;
import static gd.twohundred.jvb.components.Instructions.OP_LDW;
import static gd.twohundred.jvb.components.Instructions.OP_MOVEA;
import static gd.twohundred.jvb.components.Instructions.OP_MOVHI;
import static gd.twohundred.jvb.components.Instructions.OP_MOV_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_MOV_REG;
import static gd.twohundred.jvb.components.Instructions.OP_MUL;
import static gd.twohundred.jvb.components.Instructions.OP_MULU;
import static gd.twohundred.jvb.components.Instructions.OP_NOT;
import static gd.twohundred.jvb.components.Instructions.OP_OR_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_OR_REG;
import static gd.twohundred.jvb.components.Instructions.OP_OUTB;
import static gd.twohundred.jvb.components.Instructions.OP_OUTH;
import static gd.twohundred.jvb.components.Instructions.OP_OUTW;
import static gd.twohundred.jvb.components.Instructions.OP_RETI;
import static gd.twohundred.jvb.components.Instructions.OP_SAR_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_SAR_REG;
import static gd.twohundred.jvb.components.Instructions.OP_SEI;
import static gd.twohundred.jvb.components.Instructions.OP_SHL_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_SHL_REG;
import static gd.twohundred.jvb.components.Instructions.OP_SHR_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_STB;
import static gd.twohundred.jvb.components.Instructions.OP_STH;
import static gd.twohundred.jvb.components.Instructions.OP_STSR;
import static gd.twohundred.jvb.components.Instructions.OP_STW;
import static gd.twohundred.jvb.components.Instructions.OP_SUB;
import static gd.twohundred.jvb.components.Instructions.OP_XOR_IMM;
import static gd.twohundred.jvb.components.Instructions.OP_XOR_REG;
import static gd.twohundred.jvb.components.Instructions.REG1_LEN;
import static gd.twohundred.jvb.components.Instructions.REG1_POS;
import static gd.twohundred.jvb.components.Instructions.REG2_LEN;
import static gd.twohundred.jvb.components.Instructions.REG2_POS;
import static gd.twohundred.jvb.Utils.extractS;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.intBit;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.testBits;
import static gd.twohundred.jvb.Utils.toBinary;
import static gd.twohundred.jvb.components.Instructions.SUBOP_CMPF_S;
import static gd.twohundred.jvb.components.Instructions.SUBOP_CVT_SW;
import static gd.twohundred.jvb.components.Instructions.SUBOP_CVT_WS;
import static gd.twohundred.jvb.components.Instructions.SUBOP_DIVF_S;
import static gd.twohundred.jvb.components.Instructions.SUBOP_MPYHW;
import static gd.twohundred.jvb.components.Instructions.SUBOP_MULF_S;
import static gd.twohundred.jvb.components.Instructions.SUBOP_REV;
import static gd.twohundred.jvb.components.Instructions.SUBOP_SUBF_S;
import static gd.twohundred.jvb.components.Instructions.SUBOP_XB;
import static gd.twohundred.jvb.components.Instructions.SUBOP_XH;
import static gd.twohundred.jvb.components.Instructions.SUB_OPCODE_LEN;
import static gd.twohundred.jvb.components.Instructions.SUB_OPCODE_POS;
import static gd.twohundred.jvb.components.ProgramStatusWord.PSW_MASK;
import static java.lang.Math.abs;

public class CPU implements Emulable, Resetable, InterruptSource {
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


    private static final int ECR_EICC_POS = 0;
    private static final int ECR_EICC_LEN = 16;
    private static final int ECR_FECC_POS = 16;
    private static final int ECR_FECC_LEN = 16;


    private static final int PC_MASK = ~0b1;


    public static final int REGISTER_COUNT = 32;
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
    private final Logger logger;
    private Debugger debugger;
    private Interrupt pendingInterrupt;

    public CPU(Bus bus, Logger logger) {
        this.bus = bus;
        this.logger = logger;
    }

    private void setRegister(int r, int value) {
        if (r != 0) {
            registers[r] = value;
        }
    }

    public int getRegister(int r) {
        return registers[r];
    }

    private void setFloatRegister(int r, float value) {
        setRegister(r, Float.floatToRawIntBits(value));
    }

    private float getFloatRegister(int r) {
        return Float.intBitsToFloat(getRegister(r));
    }

    private void setSystemRegister(int r, int value) {
        switch (r) {
            case EIPC_REG:
                eipc = value & PC_MASK;
                break;
            case EIPSW_REG:
                eipsw = value & PSW_MASK;
                break;
            case FEPC_REG:
                fepc = value & PC_MASK;
                break;
            case FEPSW_REG:
                fepsw = value & PSW_MASK;
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
                logger.warning(Logger.Component.CPU, "setting read-only system reg %s (%d)", getSystemRegisterName(r), r);
                break;
            default:
                logger.warning(Logger.Component.CPU, "setting unknown system reg %d", r);
                break;
        }
    }

    private int getSystemRegister(int r) {
        switch (r) {
            case EIPC_REG:
                return eipc;
            case EIPSW_REG:
                return eipsw;
            case FEPC_REG:
                return fepc;
            case FEPSW_REG:
                return fepsw;
            case PSW_REG:
                return psw.getValue();
            case CHCW_REG:
                return chcw;
            case ADTRE_REG:
                return adtre;
            case PIR_REG:
                return PIR;
            case TKCW_REG:
                return TKCW;
            case ECR_REG:
                return ecr;
            default:
                throw new RuntimeException("Unknown reading unknown system reg " + r);
        }
    }

    public static String getSystemRegisterName(int r) {
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

    private static final int CHCW_ICE_POS = 1;
    private void processCacheControlCommand(int value) {
        chcw = value & intBit(CHCW_ICE_POS);
        if (DEBUG_CC) {
            logger.debug(Logger.Component.CPU, "ignoring cache control 0b%s", toBinary(value, 32));
        }
    }

    @Override
    public void reset() {
        pc = 0xFFFFFFF0;
        psw.set(0x00008000);
        ecr = 0x0000FFF0;

        Arrays.fill(registers, 1, REGISTER_COUNT - 1, 0xdeadbeef);
        eipc = 0xdeadbeef;
        eipsw = 0xdeadbeef & PSW_MASK;
        fepc = 0xdeadbeef;
        fepsw = 0xdeadbeef & PSW_MASK;
        adtre = 0xdeadbeef;
        chcw = 0xdeadbeef;
    }

    public static final boolean DEBUG_CC = false;

    @Override
    public long tick(long targetCycles) {
        if (this.debugger != null) {
            this.debugger.onExec(pc);
        }
        int first = bus.getHalfWord(pc);
        int nextPC = pc + 2;
        int cycles = 1;
        if (testBits(first, FORMAT_III_PREFIX, FORMAT_III_PREFIX_POS, FORMAT_III_PREFIX_LEN)) {
            int cond = extractU(first, COND_POS, COND_LEN);
            int disp9 = extractS(first, DISP9_POS, DISP9_LEN);
            if (testCondition(cond)) {
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
                    break;
                }
                case OP_MOVHI: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, getRegister(reg1) + (second << 16));
                    break;
                }
                case OP_ADDI: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, add(getRegister(reg1), signExtend(second, 16)));
                    break;
                }
                case OP_AND_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, andi(getRegister(reg1), second));
                    break;
                }
                case OP_OR_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, or(getRegister(reg1), second));
                    break;
                }
                case OP_XOR_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, xor(getRegister(reg1), second));
                    break;
                }
                case OP_JMP: {
                    cycles = 3;
                    nextPC = getRegister(reg1);
                    break;
                }
                case OP_ADD_IMM: {
                    int intValue = add(getRegister(reg2), signExtend(imm5, IMM5_LEN));
                    setRegister(reg2, intValue);
                    break;
                }
                case OP_ADD_REG: {
                    setRegister(reg2, add(getRegister(reg2), getRegister(reg1)));
                    break;
                }
                case OP_AND_REG: {
                    setRegister(reg2, and(getRegister(reg2), getRegister(reg1)));
                    break;
                }
                case OP_LDSR: {
                    setSystemRegister(imm5, getRegister(reg2));
                    break;
                }
                case OP_STSR: {
                    setRegister(reg2, getSystemRegister(imm5));
                    break;
                }
                case OP_SEI: {
                    psw.setInterruptDisable(true);
                    break;
                }
                case OP_CLI: {
                    psw.setInterruptDisable(false);
                    break;
                }
                case OP_MOV_IMM: {
                    setRegister(reg2, signExtend(imm5, IMM5_LEN));
                    break;
                }
                case OP_MOV_REG: {
                    setRegister(reg2, getRegister(reg1));
                    break;
                }
                case OP_OUTB:
                case OP_STB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setByte(getRegister(reg1) + disp16, (byte) getRegister(reg2));
                    break;
                }
                case OP_OUTH:
                case OP_STH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setHalfWord(getRegister(reg1) + disp16, (short) getRegister(reg2));
                    break;
                }
                case OP_STW:
                case OP_OUTW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setWord(getRegister(reg1) + disp16, getRegister(reg2));
                    break;
                }
                case OP_LDB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, signExtend(bus.getByte(getRegister(reg1) + disp16), Byte.SIZE));
                    break;
                }
                case OP_LDH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, signExtend(bus.getHalfWord(getRegister(reg1) + disp16), Short.SIZE));
                    break;
                }
                case OP_INW:
                case OP_LDW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, bus.getWord(getRegister(reg1) + disp16));
                    break;
                }
                case OP_INB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, zeroExtend(bus.getByte(getRegister(reg1) + disp16), Byte.SIZE));
                    break;
                }
                case OP_INH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, zeroExtend(bus.getHalfWord(getRegister(reg1) + disp16), Short.SIZE));
                    break;
                }
                case OP_JAL: {
                    int second = bus.getHalfWord(pc + 2);
                    int disp26 = signExtend(second | (reg1 << (16 + REG1_POS)) | (reg2 << (16 + REG2_POS)), DISP26_LEN);
                    nextPC = pc + disp26;
                    cycles = 3;
                    setRegister(LINK_REG, pc + 4);
                    break;
                }
                case OP_JR: {
                    int second = bus.getHalfWord(pc + 2);
                    int disp26 = signExtend(second | (reg1 << (16 + REG1_POS)) | (reg2 << (16 + REG2_POS)), DISP26_LEN);
                    nextPC = pc + disp26;
                    cycles = 3;
                    break;
                }
                case OP_CMP_REG: {
                    sub(getRegister(reg2), getRegister(reg1));
                    break;
                }
                case OP_CMP_IMM: {
                    sub(getRegister(reg2), signExtend(imm5, IMM5_LEN));
                    break;
                }
                case OP_SUB: {
                    setRegister(reg2, sub(getRegister(reg2), getRegister(reg1)));
                    break;
                }
                case OP_XOR_REG: {
                    setRegister(reg2, xor(getRegister(reg2), getRegister(reg1)));
                    break;
                }
                case OP_OR_REG: {
                    setRegister(reg2, or(getRegister(reg2), getRegister(reg1)));
                    break;
                }
                case OP_SHL_IMM: {
                    setRegister(reg2, shl(getRegister(reg2), imm5));
                    break;
                }
                case OP_SHL_REG: {
                    setRegister(reg2, shl(getRegister(reg2), getRegister(reg1) & 0x1f));
                    break;
                }
                case OP_SHR_IMM: {
                    setRegister(reg2, shr(getRegister(reg2), imm5));
                    break;
                }
                case OP_SHR_REG: {
                    setRegister(reg2, shr(getRegister(reg2), getRegister(reg1) & 0x1f));
                    break;
                }
                case OP_SAR_IMM: {
                    setRegister(reg2, sar(getRegister(reg2), imm5));
                    break;
                }
                case OP_SAR_REG: {
                    setRegister(reg2, sar(getRegister(reg2), getRegister(reg1) & 0x1f));
                    break;
                }
                case OP_NOT: {
                    setRegister(reg2, not(getRegister(reg1)));
                    break;
                }
                case OP_MULU: {
                    cycles = 13;
                    long full = mul(getRegister(reg2) & 0xffff_ffffL, getRegister(reg1) & 0xffff_ffffL);
                    setRegister(30, (int) (full >> 32));
                    setRegister(reg2, (int) full);
                    break;
                }
                case OP_MUL: {
                    cycles = 13;
                    long full = mul(getRegister(reg2), getRegister(reg1));
                    setRegister(30, (int) (full >> 32));
                    setRegister(reg2, (int) full);
                    break;
                }
                case OP_DIVU: {
                    long divisor = getRegister(reg1) & 0xffff_ffffL;
                    if (divisor == 0) {
                        cycles = 3;
                        pendingInterrupt = new SimpleInterrupt(Interrupt.InterruptType.ZeroDivision);
                    } else {
                        cycles = 36;
                        long dividend = getRegister(reg2) & 0xffff_ffffL;
                        setRegister(30, (int) (dividend % divisor)); // mod or rem?
                        setRegister(reg2, divu(dividend, divisor));
                    }
                    break;
                }
                case OP_DIV: {
                    int divisor = getRegister(reg1);
                    if (divisor == 0) {
                        cycles = 3;
                        pendingInterrupt = new SimpleInterrupt(Interrupt.InterruptType.ZeroDivision);
                    } else {
                        cycles = 38;
                        int dividend = getRegister(reg2);
                        setRegister(30, dividend % divisor); // mod or rem?
                        setRegister(reg2, div(dividend, divisor));
                    }
                    break;
                }
                case OP_RETI: {
                    cycles = 10;
                    if (psw.getNP()) {
                        nextPC = fepc;
                        psw.set(fepsw);
                        psw.setExecutionMode(ProgramStatusWord.ExecutionMode.Exception);
                        logger.debug(Logger.Component.CPU, "RETI: Duplex -> Exception");
                    } else {
                        nextPC = eipc;
                        psw.set(eipsw);
                        psw.setExecutionMode(ProgramStatusWord.ExecutionMode.Normal);
                        logger.debug(Logger.Component.CPU, "RETI: Exception -> Normal");
                    }
                    break;
                }
                case OP_SUBOP: {
                    int second = bus.getHalfWord(pc + 2);
                    int subOp = extractU(second, SUB_OPCODE_POS - 16, SUB_OPCODE_LEN);
                    nextPC += 2;
                    cycles = subop(reg2, reg1, subOp);
                    break;
                }
                case OP_SETF: {
                    int cond = imm5;
                    setRegister(reg2, testCondition(cond) ? 1 : 0);
                }
                case OP_ILL_1: {
                    logger.warning(Logger.Component.CPU, "Illegal instruction @ %#08x!", pc);
                    pendingInterrupt = new SimpleInterrupt(Interrupt.InterruptType.IllegalOpcode);
                    break;
                }
                default:
                    throw new RuntimeException(String.format("Unknown opcode: 0b%s @ %08X", toBinary(opcode, OPCODE_LEN), pc));
            }
        }
        pc = nextPC;
        return cycles;
    }

    private boolean testCondition(int cond) {
        switch (cond) {
            case BCOND_BR: {
                return true;
            }
            case BCOND_BNE: {
                return !psw.getZ();
            }
            case BCOND_BE: {
                return psw.getZ();
            }
            case BCOND_BNH: {
                return psw.getZ() || psw.getCY();
            }
            case BCOND_BH: {
                return !(psw.getZ() || psw.getCY());
            }
            case BCOND_BL: {
                return psw.getCY();
            }
            case BCOND_BNL: {
                return !psw.getCY();
            }
            case BCOND_BLT: {
                return psw.getS() ^ psw.getOV();
            }
            case BCOND_BLE: {
                return (psw.getS() ^ psw.getOV()) || psw.getZ();
            }
            case BCOND_BGT: {
                return !((psw.getS() ^ psw.getOV()) || psw.getZ());
            }
            case BCOND_BGE: {
                return !(psw.getS() ^ psw.getOV());
            }
            case BCOND_NOP: {
                return false;
            }
            case BCOND_BN: {
                return psw.getS();
            }
            case BCOND_BP: {
                return !psw.getS();
            }
            case BCOND_BV: {
                return psw.getOV();
            }
            default:
                throw new RuntimeException("Unknown bcond: 0b" + toBinary(cond, COND_LEN));
        }
    }

    private int subop(int reg2, int reg1, int subOp) {
        switch (subOp) {
            case SUBOP_ADDF_S: {
                setFloatRegister(reg2, addf(getFloatRegister(reg2), getFloatRegister(reg1)));
                return 28;
            }
            case SUBOP_SUBF_S: {
                setFloatRegister(reg2, subf(getFloatRegister(reg2), getFloatRegister(reg1)));
                return 28;
            }
            case SUBOP_MULF_S: {
                setFloatRegister(reg2, mulf(getFloatRegister(reg2), getFloatRegister(reg1)));
                return 30;
            }
            case SUBOP_DIVF_S: {
                setFloatRegister(reg2, divf(getFloatRegister(reg2), getFloatRegister(reg1)));
                return 44;
            }
            case SUBOP_CMPF_S: {
                subf(getFloatRegister(reg2), getFloatRegister(reg1));
                return 10;
            }
            case SUBOP_CVT_WS: {
                setFloatRegister(reg2, cvt(getRegister(reg1)));
                return 16;
            }
            case SUBOP_CVT_SW: {
                setRegister(reg2, cvt(getFloatRegister(reg1)));
                return 14;
            }
            case SUBOP_REV: {
                setRegister(reg2, Integer.reverse(getRegister(reg1)));
                return 1;
            }
            case SUBOP_XB: {
                setRegister(reg2, xb(getRegister(reg2)));
                return 1;
            }
            case SUBOP_XH: {
                setRegister(reg2, xh(getRegister(reg2)));
                return 1;
            }
            case SUBOP_MPYHW: {
                setRegister(reg2, signExtend(getRegister(reg2), Short.SIZE) * signExtend(getRegister(reg1), Short.SIZE));
                return 9;
            }
            default:
                throw new RuntimeException(String.format("Unknown subop opcode: 0b%s @ %08X", toBinary(subOp, SUB_OPCODE_LEN), pc));
        }
    }

    private boolean isReserved(float v) {
        return !Float.isFinite(v) || isDenormal(v);
    }

    private boolean isDenormal(float v) {
        return v != 0 && Math.abs(v) < Float.MIN_NORMAL;
    }

    private void commonFPUFlagsAndExceptions(float a, float b, float result) {
        boolean reservedArg = isReserved(a) || isReserved(b);
        boolean underflow = isDenormal(result);
        boolean overflow = Float.isInfinite(result);
        boolean precisionDegradation = false; // TODO
        psw.accumulateReservedUnderFlowOverflowPrecisionDegradation(reservedArg, underflow, overflow, precisionDegradation);
        // TODO exceptions
    }

    private float cvt(int a) {
        float value = (float) a;
        // TODO precision
        return value;
    }

    private int cvt(float a) {
        int value = (int) a;
        psw.accumulateReservedUnderFlowOverflowPrecisionDegradation(isReserved(a), false, false, false);
        // TODO precision / invalid
        return value;
    }

    private float addf(float a, float b) {
        float value = a + b;
        commonFPUFlagsAndExceptions(a, b, value);
        boolean zero = value == 0;
        boolean sign = value < 0;
        // TODO OF? CY?
        psw.setZeroSignOveflowCarry(zero, sign, false, false);
        return value;
    }

    private float subf(float a, float b) {
        float value = a - b;
        commonFPUFlagsAndExceptions(a, b, value);
        boolean zero = value == 0;
        boolean sign = value < 0;
        // TODO OF? CY?
        psw.setZeroSignOveflowCarry(zero, sign, false, false);
        return value;
    }

    private float mulf(float a, float b) {
        float value = a * b;
        commonFPUFlagsAndExceptions(a, b, value);
        boolean zero = value == 0;
        boolean sign = value < 0;
        // TODO OF? CY?
        psw.setZeroSignOveflowCarry(zero, sign, false, false);
        return value;
    }

    private float divf(float a, float b) {
        if (b == 0) {
            if (a == 0) {
                // TODO exception
                psw.accumulateFPUInvalidOp();
                return a; // ?
            } else {
                // TODO exception
                psw.accumulateFPUDivByZero();
                return a; // ?
            }
        }
        float value = a / b;
        commonFPUFlagsAndExceptions(a, b, value);
        boolean zero = value == 0;
        boolean sign = value < 0;
        // TODO OF? CY?
        psw.setZeroSignOveflowCarry(zero, sign, false, false);
        return value;
    }

    private int xb(int a) {
        return extractU(a, 16, 16) | (extractU(a, 0, 8) << 8) | (extractU(a, 8, 8) >> 8);
    }

    private int xh(int a) {
        return (extractU(a, 16, 16) >> 16) | (extractU(a, 0, 16) << 16);
    }

    private int add(int a, int b) {
        long value = a + b;
        int intValue = (int) value;
        boolean carry = (value >>> 32) != 0;
        boolean zero = intValue == 0;
        boolean sign = intValue < 0;
        boolean overflow = ((a ^ intValue) & (b ^ intValue)) < 0;
        psw.setZeroSignOveflowCarry(zero, sign, overflow, carry);
        return intValue;
    }

    private int sub(int a, int b) {
        long value = a - b;
        int intValue = (int) value;
        boolean carry = (value >>> 32) != 0;
        boolean zero = intValue == 0;
        boolean sign = intValue < 0;
        boolean overflow = ((a ^ b) & (a ^ intValue)) < 0;
        psw.setZeroSignOveflowCarry(zero, sign, overflow, carry);
        return intValue;
    }

    private int xor(int a, int b) {
        int value = a ^ b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, false);
        return value;
    }

    private int or(int a, int b) {
        int value = a | b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, false);
        return value;
    }

    private int andi(int a, int b) {
        int value = a & b;
        boolean zero = value == 0;
        psw.setZeroSignOveflow(zero, false, false);
        return value;
    }

    private int and(int a, int b) {
        int value = a & b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, false);
        return value;
    }

    private int shl(int a, int b) {
        int value = a << b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        boolean carry = b > 0 && testBit(a, 32 - b);
        psw.setZeroSignOveflowCarry(zero, sign, false, carry);
        return value;
    }

    private int shr(int a, int b) {
        int value = a >>> b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        boolean carry = b > 0 && testBit(a, b - 1);
        psw.setZeroSignOveflowCarry(zero, sign, false, carry);
        return value;
    }

    private int sar(int a, int b) {
        int value = a >> b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        boolean carry = b > 0 && testBit(a, b - 1);
        psw.setZeroSignOveflowCarry(zero, sign, false, carry);
        return value;
    }

    private int not(int a) {
        int value = ~a;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, false);
        return value;
    }

    private long mul(long a, long b) {
        long value = a * b;
        boolean zero = (int) value == 0;
        boolean sign = (int) value < 0;
        psw.setZeroSignOveflow(zero, sign, (int) value != value);
        return value;
    }

    private int div(int a, int b) {
        int value = a / b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, a == Integer.MIN_VALUE && b == -1);
        return value;
    }

    private int divu(long a, long b) {
        int value = (int) (a / b);
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, false);
        return value;
    }

    Bus getBus() {
        return bus;
    }

    int getPc() {
        return pc;
    }

    ProgramStatusWord getPsw() {
        return psw;
    }

    public void setFepc(int fepc) {
        this.fepc = fepc & PC_MASK;
    }

    public void setFepsw(int fepsw) {
        this.fepsw = fepsw & PSW_MASK;
    }

    public void setFecc(short fecc) {
        this.ecr = insert(fecc, ECR_FECC_POS, ECR_FECC_LEN, ecr);
    }

    public void setPc(int pc) {
        this.pc = pc & PC_MASK;
    }

    public void setEipc(int eipc) {
        this.eipc = eipc & PC_MASK;
    }

    public void setEipsw(int eipsw) {
        this.eipsw = eipsw & PSW_MASK;
    }

    public void setEicc(short eicc) {
        this.ecr = insert(eicc, ECR_EICC_POS, ECR_EICC_LEN, ecr);
    }

    @Override
    public Interrupt raised() {
        Interrupt interrupt = pendingInterrupt;
        pendingInterrupt = null;
        return interrupt;
    }

    void attach(Debugger debugger) {
        this.debugger = debugger;
        this.bus.attach(debugger);
    }
}
