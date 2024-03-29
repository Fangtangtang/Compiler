package ast.stmt;

import ast.ASTVisitor;
import utility.Position;

/**
 * @author F
 * 类的构造函数
 * Identifier LeftRoundBracket RightRoundBracket suite;
 */
public class ConstructorDefStmtNode extends StmtNode {
    public String name;
    public BlockStmtNode suite;

    public ConstructorDefStmtNode(Position pos,
                                  String name,
                                  BlockStmtNode suite) {
        super(pos);
        this.name=name;
        this.suite=suite;
    }


    @Override
    public <T> T accept(ASTVisitor<? extends T> visitor) {
        return visitor.visit(this);
    }
}
