package ast;

import ast.expr.ConstantExprNode.*;
import ast.stmt.*;
import ast.expr.*;
import ast.other.*;

/**
 * @author F
 * ASTVisitor接口
 * （类似parse tree的Visitor接口）
 * 被ASTNode接受，访问每一个结点，遍历AST
 */
public interface ASTVisitor {

    //root
    void visit(RootNode node);

    //stmt
    void visit(BlockStmtNode node);

    void visit(BreakStmtNode node);

    void visit(ContinueStmtNode node);

    void visit(ExprStmtNode node);

    void visit(ForStmtNode node);

    void visit(FuncDefStmtNode node);

    void visit(IfStmtNode node);

    void visit(ReturnStmtNode node);

    void visit(VarDefStmtNode node);

    void visit(WhileStmtNode node);

    //expr
    void visit(BinaryExprNode node);

    void visit(CmpExprNode node);

    void visit(AssignExprNode node);

    void visit(LogicExprNode node);

    void visit(NestificationExprNode node);

    void visit(PrefixExprNode node);

    void visit(LogicPrefixExprNode node);

    void visit(SuffixExprNode node);

    void visit(TernaryExprNode node);

    void visit(NewExprNode node);

    void visit(MemberVisExprNode node);

    void visit(IntConstantExprNode node);

    void visit(StrConstantExprNode node);

    void visit(BoolConstantExprNode node);

    void visit(NullConstantExprNode node);

    void visit(VarNameExprNode node);

    void visit(FuncCallExprNode node);

    void visit(ArrayVisExprNode node);

    //other
    void visit(TypeNode node);

    void visit(ParameterNode node);

}
