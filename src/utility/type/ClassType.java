package utility.type;

import java.util.*;

/**
 * @author F
 * class类型，类内定义成员、函数
 */
public class ClassType extends Type {

    public String name;

    public ClassType() {
        this.typeName = Types.CLASS;
    }
    public ClassType(String name) {
        this.name=name;
        this.typeName = Types.CLASS;
        this.members=new HashMap<>();
    }

    @Override
    public String toString() {
        return typeName.name()+" "+this.name;
    }
}
