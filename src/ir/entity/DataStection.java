package ir.entity;

import ir.irType.IRType;

/**
 * @author F
 * 派生自Entity
 * 内存中的数据区，存放全局变量
 * ------------------------------------------------------------------------
 * 全局变量使用alloca定义，用一个指针（i32）指向DataStection中一段空间
 * 指针仅被赋值一次，指向固定空间
 * 空间的内容可变
 */
public class DataStection extends Entity {
    private static int cnt = -1;
    private final int index;
    private final String identity;


    //由类型能确定占多少空间
    //TODO：type是否必要
    public DataStection(IRType type,
                    String identity) {
        super(type);
        ++cnt;
        this.index = cnt;
        this.identity = identity;
    }

    @Override
    public String toString() {
        return "DataStection " + index + ": " + identity + " " + type.toString();
    }
}