package utility.type;

/**
 * @author F
 * 标识类型的基类
 */
abstract public class Type {
    enum Types {
        BOOL, INT, STRING,
        VOID,
        ARRAY, CLASS, FUNCTION,
        NULL
    }

    public Types typeName;

    @Override
    public String toString(){
        return typeName.name();
    }
}
