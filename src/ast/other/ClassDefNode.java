package ast.other;

import ast.ASTNode;
import ast.ASTVisitor;
import ast.stmt.StmtNode;
import utility.Position;

import java.util.ArrayList;

/**
 * @author F
 * Mx类定义
 */
public class ClassDefNode extends ASTNode {
    public String name;
    public ArrayList<StmtNode> members = new ArrayList<>();

    public ClassDefNode(Position pos,
                        String name) {
        super(pos);
        this.name = name;
    }

    @Override
    public <T> T accept(ASTVisitor<? extends T> visitor) {
        return visitor.visit(this);
    }
}
