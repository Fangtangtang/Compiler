package ast.expr;

import ast.ASTVisitor;
import ast.ExprNode;
import utility.Position;
import utility.type.Type;

/**
 * @author F
 * 用括号改变优先级的表达式
 */
public class ParenthesisExprNode extends ExprNode {
    public ExprNode expression;
    public ParenthesisExprNode(Position pos,
                                 Type exprType,
                                 ExprNode expression) {
        super(pos);
        this.exprType=exprType;
        this.expression = expression;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}