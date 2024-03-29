package asm.instruction;

import asm.ASMVisitor;
import asm.operand.Operand;
import asm.operand.Register;
import ir.stmt.instruction.Icmp;
import utility.error.InternalException;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author F
 * 比较指令
 * 由多个基本指令组合的伪指令
 */
public class CmpInst extends ASMInstruction {

    enum Opcode {
        slt, sgt
    }

    public Operand rs1, rs2;
    public Register rd;
    public Opcode op;

    public CmpInst(Operand rs1, Operand rs2,
                   Register rd,
                   Icmp.Cond operator) {
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.rd = rd;
        this.rd.size = 1;
        switch (operator) {
            case slt -> this.op = Opcode.slt;
            case sgt -> this.op = Opcode.sgt;
            case sle -> {
                this.op = Opcode.sgt;
                this.rs1 = rs2;
                this.rs2 = rs1;
            }
            case sge -> {
                this.op = Opcode.slt;
                this.rs1 = rs2;
                this.rs2 = rs1;
            }
            default -> throw new InternalException("unexpected operator in ir icmp");
        }
    }

    public CmpInst(Operand rs1, Operand rs2,
                   Register rd,
                   Opcode opcode) {
        this.rs1 = rs1;
        this.rs2 = rs2;
        this.rd = rd;
        this.rd.size = 1;
        this.op = opcode;
    }

    @Override
    public void print(PrintStream out) {
        out.println("\t" + op + "\t" + rd + ", " + rs1 + ", " + rs2);
    }

    @Override
    public void printRegColoring(PrintStream out) {
        out.println("\t" + op + "\t" + rd.toRegColoringString()
                + ", " + rs1.toRegColoringString() + ", " + rs2.toRegColoringString());
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ArrayList<Register> getUse() {
        ArrayList<Register> ret = new ArrayList<>();
        if (rs1 instanceof Register register) {
            ret.add(register);
        }
        if (rs2 instanceof Register register) {
            ret.add(register);
        }
        return ret;
    }

    @Override
    public Register getDef() {
        return rd;
    }

    @Override
    public void setUse(ArrayList<Register> use) {
        rs1 = use.get(0);
        rs2 = use.get(1);
    }

    @Override
    public void setDef(Register def) {
        rd = def;
    }
}
