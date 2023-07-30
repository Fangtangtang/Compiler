package ast.expr.ConstantExprNode;

import ast.ASTVisitor;
import ast.ExprNode;
import utility.Position;
import utility.type.NullType;

/**
 * @author F
 * null常量
 * 不可赋值
 */
public class NullConstantExprNode extends ExprNode {

    public NullConstantExprNode(Position pos) {
        super(pos);
        this.exprType = new NullType();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
