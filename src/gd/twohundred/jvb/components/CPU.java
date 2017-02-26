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
import static gd.twohundred.jvb.components.Instructions.BCOND_NOP;
import static gd.twohundred.jvb.components.Instructions.COND_LEN;
import static gd.twohundred.jvb.components.Instructions.COND_POS;
import static gd.twohundred.jvb.components.Instructions.DISP26_LEN;
import static gd.twohundred.jvb.components.Instructions.DISP9_LEN;
import static gd.twohundred.jvb.components.Instructions.DISP9_POS;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX_LEN;
import static gd.twohundred.jvb.components.Instructions.FORMAT_III_PREFIX_POS;
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
    private final Logger logger;
    private Debugger debugger;
    private Interrupt pendingInterrupt;

    public CPU(Bus bus, Logger logger) {
        this.bus = bus;
        this.logger = logger;
    }

    static {
        PrintStream out = System.out;
        try {
            out = new PrintStream("trace.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        debugInstOut = out;
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

    private String getSystemRegisterName(int r) {
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
        eipsw = 0xdeadbeef;
        fepc = 0xdeadbeef;
        fepsw = 0xdeadbeef;
        adtre = 0xdeadbeef;
        chcw = 0xdeadbeef;
    }

    public static final boolean DEBUG_INST = false;
    public static final PrintStream debugInstOut;
    public static final boolean DEBUG_CC = false;

    @Override
    public int tick(int targetCycles) {
        if (this.debugger != null) {
            this.debugger.onExec(pc);
        }
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
                        debugInstOut.println(String.format("%08x br     %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BNE: {
                    branchTaken = !psw.getZ();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bne    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BE: {
                    branchTaken = psw.getZ();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x be     %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BNH: {
                    branchTaken = psw.getZ() || psw.getCY();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bnh    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BH: {
                    branchTaken = !(psw.getZ() || psw.getCY());
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bh     %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BL: {
                    branchTaken = psw.getCY();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bl     %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BNL: {
                    branchTaken = !psw.getCY();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bnl    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BLT: {
                    branchTaken = psw.getS() ^ psw.getOV();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x blt    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BLE: {
                    branchTaken = (psw.getS() ^ psw.getOV()) || psw.getZ();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x ble    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BGT: {
                    branchTaken = !((psw.getS() ^ psw.getOV()) || psw.getZ());
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bgt    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BGE: {
                    branchTaken = !(psw.getS() ^ psw.getOV());
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bge    %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_NOP: {
                    branchTaken = false;
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x nop", pc));
                    }
                    break;
                }
                case BCOND_BN: {
                    branchTaken = psw.getS();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bn     %s0x%x", pc, signStr(disp9), abs(disp9)));
                    }
                    break;
                }
                case BCOND_BP: {
                    branchTaken = !psw.getS();
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x bp     %s0x%x", pc, signStr(disp9), abs(disp9)));
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
                        debugInstOut.println(String.format("%08x movea  0x%04x, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_MOVHI: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, getRegister(reg1) + (second << 16));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x movhi  0x%04x, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_ADDI: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, add(getRegister(reg1), signExtend(second, 16)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x addi   0x%04x, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_AND_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, andi(getRegister(reg1), second));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x andi   0x%04x, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_OR_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, or(getRegister(reg1), second));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x ori    0x%04x, r%d, r%d", pc, second, reg1, reg2));
                    }
                    break;
                }
                case OP_XOR_IMM: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    setRegister(reg2, xor(getRegister(reg2), second));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x xori   0x%04x, r%d", pc, imm5, reg2));
                    }
                    break;
                }
                case OP_JMP: {
                    cycles = 3;
                    nextPC = getRegister(reg1);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x jmp    r%d", pc, reg1));
                    }
                    break;
                }
                case OP_ADD_IMM: {
                    int intValue = add(getRegister(reg2), signExtend(imm5, IMM5_LEN));
                    setRegister(reg2, intValue);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x add    %d, r%d, r%d", pc, signExtend(imm5, IMM5_LEN), reg2, reg2));
                    }
                    break;
                }
                case OP_ADD_REG: {
                    setRegister(reg2, add(getRegister(reg2), getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x add    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_AND_REG: {
                    setRegister(reg2, and(getRegister(reg2), getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x and    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_LDSR: {
                    setSystemRegister(imm5, getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x ldsr   r%d, %s", pc, reg2, getSystemRegisterName(imm5)));
                    }
                    break;
                }
                case OP_STSR: {
                    setRegister(reg2, getSystemRegister(imm5));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x ldsr   r%d, %s", pc, reg2, getSystemRegisterName(imm5)));
                    }
                    break;
                }
                case OP_SEI: {
                    psw.setInterruptDisable(true);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x sei", pc));
                    }
                    break;
                }
                case OP_CLI: {
                    psw.setInterruptDisable(false);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x sei", pc));
                    }
                    break;
                }
                case OP_MOV_IMM: {
                    setRegister(reg2, signExtend(imm5, IMM5_LEN));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x mov    %d, r%d", pc, signExtend(imm5, IMM5_LEN), reg2));
                    }
                    break;
                }
                case OP_MOV_REG: {
                    setRegister(reg2, getRegister(reg1));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x mov    r%d, r%d", pc, reg1, reg2));
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
                        debugInstOut.println(String.format("%08x st.b   r%d, %d[r%d]", pc, reg2, disp16, reg1));
                    }
                    break;
                }
                case OP_STH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setHalfWord(getRegister(reg1) + disp16, (short) getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x st.h   r%d, %d[r%d]", pc, reg2, disp16, reg1));
                    }
                    break;
                }
                case OP_STW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setWord(getRegister(reg1) + disp16, getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x st.w   r%d, %d[r%d]", pc, reg2, disp16, reg1));
                    }
                    break;
                }
                case OP_OUTB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setByte(getRegister(reg1) + disp16, (byte) getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x out.w  r%d, %d[r%d]", pc, reg2, disp16, reg1));
                    }
                    break;
                }
                case OP_OUTH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setHalfWord(getRegister(reg1) + disp16, (short) getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x out.w  r%d, %d[r%d]", pc, reg2, disp16, reg1));
                    }
                    break;
                }
                case OP_OUTW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    bus.setWord(getRegister(reg1) + disp16, getRegister(reg2));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x out.w  r%d, %d[r%d]", pc, reg2, disp16, reg1));
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
                        debugInstOut.println(String.format("%08x ld.b   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_LDH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, signExtend(bus.getHalfWord(getRegister(reg1) + disp16), Short.SIZE));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x ld.h   %d[r%d], r%d", pc, disp16, reg1, reg2));
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
                        debugInstOut.println(String.format("%08x ld.w   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_INB: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, zeroExtend(bus.getByte(getRegister(reg1) + disp16), Byte.SIZE));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x in.b   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_INH: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, zeroExtend(bus.getHalfWord(getRegister(reg1) + disp16), Short.SIZE));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x in.h   %d[r%d], r%d", pc, disp16, reg1, reg2));
                    }
                    break;
                }
                case OP_INW: {
                    int second = bus.getHalfWord(pc + 2);
                    nextPC += 2;
                    cycles = 4;
                    int disp16 = signExtend(second, 16);
                    setRegister(reg2, bus.getWord(getRegister(reg1) + disp16));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x in.w   %d[r%d], r%d", pc, disp16, reg1, reg2));
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
                        debugInstOut.println(String.format("%08x jal    %s0x%x", pc, signStr(disp26), abs(disp26)));
                    }
                    break;
                }
                case OP_JR: {
                    int second = bus.getHalfWord(pc + 2);
                    int disp26 = signExtend(second | (reg1 << (16 + REG1_POS)) | (reg2 << (16 + REG2_POS)), DISP26_LEN);
                    nextPC = pc + disp26;
                    cycles = 3;
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x jr     %s0x%x", pc, signStr(disp26), abs(disp26)));
                    }
                    break;
                }
                case OP_CMP_REG: {
                    sub(getRegister(reg2), getRegister(reg1));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x cmp    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_CMP_IMM: {
                    sub(getRegister(reg2), signExtend(imm5, IMM5_LEN));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x cmp    %d, r%d", pc, signExtend(imm5, IMM5_LEN), reg2));
                    }
                    break;
                }
                case OP_SUB: {
                    setRegister(reg2, sub(getRegister(reg2), getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x sub    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_XOR_REG: {
                    setRegister(reg2, xor(getRegister(reg2), getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x xor    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_OR_REG: {
                    setRegister(reg2, or(getRegister(reg2), getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x or     r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_SHL_IMM: {
                    setRegister(reg2, shl(getRegister(reg2), imm5));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x shl    %d, r%d", pc, imm5, reg2));
                    }
                    break;
                }
                case OP_SHL_REG: {
                    setRegister(reg2, shl(getRegister(reg2), getRegister(reg1) & 0x1f));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x shl    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_SHR_IMM: {
                    setRegister(reg2, shr(getRegister(reg2), imm5));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x shr    %d, r%d", pc, imm5, reg2));
                    }
                    break;
                }
                case OP_SAR_IMM: {
                    setRegister(reg2, sar(getRegister(reg2), imm5));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x sar    %d, r%d", pc, imm5, reg2));
                    }
                    break;
                }
                case OP_SAR_REG: {
                    setRegister(reg2, sar(getRegister(reg2), getRegister(reg1) & 0x1f));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x sar    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_NOT: {
                    setRegister(reg2, not(getRegister(reg1)));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x not    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_MULU: {
                    cycles = 13;
                    long full = mul(getRegister(reg2) & 0xffff_ffffL, getRegister(reg1) & 0xffff_ffffL);
                    setRegister(30, (int) (full >> 32));
                    setRegister(reg2, (int) full);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x mul    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_MUL: {
                    cycles = 13;
                    long full = mul(getRegister(reg2), getRegister(reg1));
                    setRegister(30, (int) (full >> 32));
                    setRegister(reg2, (int) full);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x mul    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_DIVU: {
                    cycles = 36;
                    long divisor = getRegister(reg1) & 0xffff_ffffL;
                    if (divisor == 0) {
                        throw new RuntimeException("Impl Zero Division exception");
                    }
                    long dividend = getRegister(reg2) & 0xffff_ffffL;
                    setRegister(30, (int) (dividend % divisor)); // mod or rem?
                    setRegister(reg2, divu(dividend, divisor));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x divu   r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_DIV: {
                    cycles = 38;
                    int divisor = getRegister(reg1);
                    if (divisor == 0) {
                        throw new RuntimeException("Impl Zero Division exception");
                    }
                    int dividend = getRegister(reg2);
                    setRegister(30, dividend % divisor); // mod or rem?
                    setRegister(reg2, div(dividend, divisor));
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x div    r%d, r%d", pc, reg1, reg2));
                    }
                    break;
                }
                case OP_RETI: {
                    cycles = 10;
                    if (psw.getNP()) {
                        nextPC = fepc;
                        psw.set(fepsw);
                    } else {
                        nextPC = eipc;
                        psw.set(eipsw);
                    }
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x reti", pc));
                    }
                    break;
                }
                case OP_ILL_1: {
                    logger.warning(Logger.Component.CPU, "Illegal instruction @ %#08x!", pc);
                    if (DEBUG_INST) {
                        debugInstOut.println(String.format("%08x illegal! 0b%s", pc, toBinary(opcode, OPCODE_LEN)));
                    }
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
        int value = a * b;
        boolean zero = value == 0;
        boolean sign = value < 0;
        psw.setZeroSignOveflow(zero, sign, a == Integer.MIN_VALUE && b == -1);
        return value;
    }

    private int divu(long a, long b) {
        int value = (int) (a * b);
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
        this.fepc = fepc;
    }

    public void setFepsw(int fepsw) {
        this.fepsw = fepsw;
    }

    public void setFecc(short fecc) {
        this.ecr = insert(fecc, ECR_FECC_POS, ECR_FECC_LEN, ecr);
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public void setEipc(int eipc) {
        this.eipc = eipc;
    }

    public void setEipsw(int eipsw) {
        this.eipsw = eipsw;
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
