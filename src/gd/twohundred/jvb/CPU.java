package gd.twohundred.jvb;

import java.util.Arrays;

import static gd.twohundred.jvb.Instructions.BCOND_BNZ;
import static gd.twohundred.jvb.Instructions.COND_LEN;
import static gd.twohundred.jvb.Instructions.COND_POS;
import static gd.twohundred.jvb.Instructions.DISP9_LEN;
import static gd.twohundred.jvb.Instructions.DISP9_POS;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX_LEN;
import static gd.twohundred.jvb.Instructions.FORMAT_III_PREFIX_POS;
import static gd.twohundred.jvb.Instructions.IMM5_LEN;
import static gd.twohundred.jvb.Instructions.OPCODE_LEN;
import static gd.twohundred.jvb.Instructions.OPCODE_POS;
import static gd.twohundred.jvb.Instructions.OP_ADD;
import static gd.twohundred.jvb.Instructions.OP_JMP;
import static gd.twohundred.jvb.Instructions.OP_MOVEA;
import static gd.twohundred.jvb.Instructions.OP_MOVHI;
import static gd.twohundred.jvb.Instructions.REG1_LEN;
import static gd.twohundred.jvb.Instructions.REG1_POS;
import static gd.twohundred.jvb.Instructions.REG2_LEN;
import static gd.twohundred.jvb.Instructions.REG2_POS;
import static gd.twohundred.jvb.Utils.extractS;
import static gd.twohundred.jvb.Utils.extractU;
import static gd.twohundred.jvb.Utils.signExtend;
import static gd.twohundred.jvb.Utils.testBits;
import static java.lang.Math.abs;

public class CPU implements Emulable, Resetable {
    private static final int REGISTER_COUNT = 32;
    private static final int LINK_REG = 31;
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
    private int tkcw;
    private final int pir = 0x810 << 4;

    private final Bus bus;

    public CPU(Bus bus) {
        this.bus = bus;
    }

    private void setRegister(int r, int value) {
        if (r != 0) {
            registers[r] = value;
        }
    }

    private int getRegister(int r) {
        return registers[r];
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
        tkcw = 0xdeadbeef;
    }

    @Override
    public void tick() {
        int first = bus.getHalfWord(pc);
        int instructionLength = 2;

        if (testBits(first, FORMAT_III_PREFIX, FORMAT_III_PREFIX_POS, FORMAT_III_PREFIX_LEN)) {
            int cond = extractU(first, COND_POS, COND_LEN);
            int disp9 = extractS(first, DISP9_POS, DISP9_LEN);
            boolean branchTaken;
            switch (cond) {
                case BCOND_BNZ: {
                    branchTaken = !psw.getZ();
                    //System.out.println(String.format("%08x BNZ    %s%05X", pc, disp9 > 0 ? '+' : '-', abs(disp9)));
                    break;
                }
                default:
                    throw new RuntimeException("Unknown bcond: 0b" + Utils.leftPad(Integer.toBinaryString(cond), '0', COND_LEN));
            }
            if (branchTaken) {
                instructionLength = 0;
                pc += disp9;
            }
        } else {
            int opcode = extractU(first, OPCODE_POS, OPCODE_LEN);
            int reg1 = extractU(first, REG1_POS, REG1_LEN);
            int reg2 = extractU(first, REG2_POS, REG2_LEN);
            int imm5 = reg1;
            switch (opcode) {
                case OP_MOVEA: {
                    int second = bus.getHalfWord(pc + 2);
                    instructionLength += 2;
                    setRegister(reg2, getRegister(reg1) + signExtend(second, 16));
                    System.out.println(String.format("%08x MOVEA  %X, r%d, r%d", pc, second, reg1, reg2));
                    break;
                }
                case OP_MOVHI: {
                    int second = bus.getHalfWord(pc + 2);
                    instructionLength += 2;
                    setRegister(reg2, getRegister(reg1) + (second << 16));
                    System.out.println(String.format("%08x MOVHI  %X, r%d, r%d", pc, second, reg1, reg2));
                    break;
                }
                case OP_JMP: {
                    instructionLength = 0;
                    pc = getRegister(reg1);
                    System.out.println(String.format("%08x JMP    r%d", pc, reg1));
                    break;
                }
                case OP_ADD: {
                    setRegister(reg2, getRegister(reg2) + signExtend(imm5, IMM5_LEN));
                    //System.out.println(String.format("%08x ADD    %X, r%d, r%d", pc, imm5, reg2, reg2));
                    // TODO: set psw
                    break;
                }
                default:
                    throw new RuntimeException("Unknown opcode: 0b" + Utils.leftPad(Integer.toBinaryString(opcode), '0', OPCODE_LEN));
            }
        }
        pc += instructionLength;
    }
}
