import asm.Program;
import ast.*;
import backend.InstSelector;
import frontend.*;
import midend.IRBuilder;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import parser.*;
import tool.*;
import utility.MxErrorListener;
import utility.SymbolTable;
import utility.error.MxException;
import utility.scope.Scope;

import java.io.*;

/**
 * @author F
 * COMPILER
 */
public class LocalMain {
    //程序的入口点
    //可能会抛出任何类型的异常
    public static void main(String[] args) throws Exception {

//        String fileName = "testcases/optim/binary_tree.mx";
        String fileName = "testcases/codegen/e1.mx";
//        String fileName = "testcases/primary/1.mx";

        InputStream inputStream = new FileInputStream(fileName);

        try {
            compile(inputStream);
        } catch (MxException exception) {
            System.err.println(exception);
            throw new RuntimeException();
        }
    }

    public static void compile(InputStream inputStream) throws Exception {
        //AST root
        RootNode astRoot;

        //char -> lexer
        MxLexer lexer = new MxLexer(CharStreams.fromStream(inputStream));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new MxErrorListener());
        //lexer -> token
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        //token -> parser
        MxParser parser = new MxParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new MxErrorListener());
        //parse tree
        ParseTree parseTreeRoot = parser.program();

        ASTBuilder astBuilder = new ASTBuilder();
        astRoot = (RootNode) astBuilder.visit(parseTreeRoot);

        Scope.symbolTable = new SymbolTable();
        SymbolCollector symbolCollector = new SymbolCollector(Scope.symbolTable);
        symbolCollector.visit(astRoot);

        SemanticChecker semanticChecker = new SemanticChecker();
        semanticChecker.visit(astRoot);

        IRBuilder irBuilder = new IRBuilder(Scope.symbolTable);
        irBuilder.visit(astRoot);

//        PrintStream outputStream = new PrintStream(new FileOutputStream("C:/Users/21672/Desktop/buildin/main.ll"));
        PrintStream outputStream = new PrintStream(new FileOutputStream("output"));
        IRPrinter printer = new IRPrinter(outputStream);
//        printer.visit(irBuilder.irRoot);

        Program program = new Program();
        InstSelector instSelector = new InstSelector(program);
        instSelector.visit(irBuilder.irRoot);

        program.print(outputStream);

    }
}

