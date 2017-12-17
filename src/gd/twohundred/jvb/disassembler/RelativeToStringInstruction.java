package gd.twohundred.jvb.disassembler;

public interface RelativeToStringInstruction extends Instruction {
    String toString(int instructionAddress);
}
