package ir.stmt.instruction;

import ir.IRVisitor;
import ir.entity.Entity;
import ir.entity.SSAEntity;
import ir.entity.constant.*;
import ir.entity.var.*;
import ir.irType.PtrType;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author F
 * 将entity存到指针指向位置
 * store <ty> <value>, ptr <pointer>
 * + -----------------------------------
 * |
 * |    int a = b;
 * |    %0 = load i32, ptr %b
 * |    store i32 %0, ptr %a
 * |
 * |    b = 1;
 * |    store i32 1, ptr %b
 * |
 * + ------------------------------------
 */
public class Store extends Instruction {
    public Entity value;
    public SSAEntity ssaValue;
    public Entity pointer;
    public SSAEntity ssaPtr;

    public Store(Entity value,
                 Entity pointer) {
        this.value = value;
        this.pointer = pointer;
    }

    @Override
    public void print(PrintStream out) {
        String str;
        if (value instanceof LocalTmpVar) {
            str = value.type.toString() + " " + value.toString();
        } else if (value instanceof Ptr ptr) {
            str = ptr.storage.type + " " + ptr;
        } else {
            str = value.toString();
        }
        out.println("\tstore " + str
                + ", ptr " + pointer.toString());
    }

    @Override
    public void printSSA(PrintStream out) {
        String str;
        if (value instanceof LocalTmpVar) {
            str = value.type.toString() + " " + ssaValue.toString();
        } else if (value instanceof Ptr ptr) {
            str = ptr.storage.type + " " + ptr;
        } else {
            str = ssaValue.toString();
        }
        out.println("\tstore " + str
                + ", ptr " + ssaPtr.toString());
    }

    @Override
    public void accept(IRVisitor irVisitor) {
        irVisitor.visit(this);
    }

    @Override
    public ArrayList<Entity> getUse() {
        ArrayList<Entity> ret = new ArrayList<>();
        ret.add(value);
        return ret;
    }

    @Override
    public Entity getDef() {
        return pointer;
    }

    @Override
    public void setUse(ArrayList<SSAEntity> list) {
        ssaValue = list.get(0);
    }

    @Override
    public void setDef(SSAEntity entity) {
        ssaPtr = entity;
    }

    @Override
    public ArrayList<SSAEntity> getSSAUse() {
        ArrayList<SSAEntity> ret = new ArrayList<>();
        ret.add(ssaValue);
        return ret;
    }

    @Override
    public SSAEntity getSSADef() {
        return ssaPtr;
    }
}
