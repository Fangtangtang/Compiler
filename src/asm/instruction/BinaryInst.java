package asm.instruction;

import asm.ASMVisitor;
import asm.operand.Register;

import java.io.PrintStream;

/**
 * @author F
 * 二元运算指令,rs1,rs2版本
 * - 乘除法没有立即数版本
 */
public class BinaryInst extends ASMInstruction {
    enum Opcode {
        add, sub, mul, div, rem,
        sll, xor, srl, sra, or, and
    }

    public Register rs1, rs2;
    public Register rd;

    public Opcode op;

    @Override
    public void print(PrintStream out) {

    }

    @Override
    public void accept(ASMVisitor visitor) {

    }
}
