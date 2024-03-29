package ast.expr.ConstantExprNode;

import ast.ASTVisitor;
import ast.expr.ExprNode;
import utility.Position;
import ast.type.NullType;

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
    public <T> T accept(ASTVisitor<? extends T> visitor) {
        return visitor.visit(this);
    }
}
