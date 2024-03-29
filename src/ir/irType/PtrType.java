package ir.irType;

/**
 * @author F
 * IR指针ptr
 * type指针指向对象的类型
 */
public class PtrType extends IRType {
    public IRType type = null;

    public PtrType() {
        this.size = 32;
    }

    public PtrType(IRType type) {
        this.size = 32;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ptr";
    }
}
