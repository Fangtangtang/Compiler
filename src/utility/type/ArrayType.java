package utility.type;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author F
 * 数组类型
 * eleType：数组基本元素的类型
 * TODO:dimensionList：数组的维度列表
 */
public class ArrayType extends Type {
    public Type eleType;
    public Integer dimensions;
    public ArrayList<Integer> dimensionList = new ArrayList<>();

    public ArrayType(Type eleType,
                     int dimensions) {
        this.typeName = Types.ARRAY;
        this.eleType = eleType;
        this.dimensions = dimensions;
    }

    public static void addBuildInFunc() {
        //数组内建方法
        members = new HashMap<>();
        members.put(
                "size",
                new FunctionType(new IntType())
        );
    }

    public void clarifyEleType(Type eleType) {
        this.eleType = eleType;
    }

    //    @Override
    public String print() {
        String str = typeName.name() + ' '
                + eleType.toString();
        return String.format("%s dim:%d", str, dimensions);
    }

    @Override
    public String toString() {
        return eleType.toString();
    }

    //同为数组，基本类型相同，维度相同
    @Override
    public boolean equals(Type other) {
        return (other instanceof ArrayType
                && this.eleType.equals(((ArrayType) other).eleType)
                && this.dimensions.equals(((ArrayType) other).dimensions));
    }

}

