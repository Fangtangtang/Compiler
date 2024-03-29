package ast.other;

import ast.ASTNode;
import ast.ASTVisitor;
import utility.Position;
import ast.type.*;

/**
 * @author F
 * 表示变量、函数返回值等类型
 */
public class TypeNode extends ASTNode {
    public Type type;

    public TypeNode(Position pos) {
        super(pos);
    }

    public TypeNode(Position pos, Type type) {
        super(pos);
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<? extends T> visitor) {
        return visitor.visit(this);
    }
}
