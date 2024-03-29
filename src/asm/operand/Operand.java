package asm.operand;

/**
 * @author F
 * ASM中的操作数
 * 包括寄存器、立即数
 */
public abstract class Operand {
    public int size;

    @Override
    public abstract String toString();

    public abstract String toRegColoringString();
}
