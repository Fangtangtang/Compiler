package ir.entity.var;

import ir.entity.Entity;
import ir.entity.Storage;
import ir.irType.PtrType;

/**
 * @author F
 * alloca变量时得到的指针
 */
public abstract class Ptr extends Storage {
    public String identity;
    public Storage storage;

    public Ptr(Storage storage,
               String identity) {
        super(new PtrType());
        this.storage = storage;
        this.identity = identity;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String renamed(String rename) {
        return null;
    }
}
