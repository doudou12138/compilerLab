import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import static org.bytedeco.llvm.global.LLVM.*;
import org.bytedeco.javacpp.BytePointer;

public class Main {
    SymbolTable symbolTable=new SymbolTable();
    public static final BytePointer error = new BytePointer();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        LexerErrorListener myLexerErrorListener = new LexerErrorListener();
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(myLexerErrorListener);


        CommonTokenStream tokenStream = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokenStream);

        ParseErrorListener myParseErrorListener = new ParseErrorListener();
        sysYParser.removeErrorListeners();
        sysYParser.addErrorListener(myParseErrorListener);

        if (false) {
            //if(myLexerErrorListener.hasError())
            myLexerErrorListener.changeStatu(false);
        } else {
            for (Token i : tokenStream.getTokens()) {
                int type_n = i.getType();
                if (type_n == 34) {
                    String tokenText = i.getText();
                    tokenText = String.valueOf(tokenText);
                    //System.err.println(SysYLexer.ruleNames[type_n - 1] + " " + tokenText + " at Line " + i.getLine() + ".");
                } else {
                    //System.err.println(SysYLexer.ruleNames[type_n - 1] + " " + i.getText() + " at Line " + i.getLine() + ".");
                }
            }

            // 如果有语法错误，输出错误信息
            ParseTree tree = sysYParser.program();
            if (false) {
                //if(myParseErrorListener.hasError())
                myParseErrorListener.changeStatu(false);
            } else {
                myParseErrorListener.setMean();
                SymbolTable symbolTable=new SymbolTable();
                ErrorVisitor errorVisitor = new ErrorVisitor(symbolTable,myParseErrorListener);
                //errorVisitor.visit(tree);

                if(false){
                    //if(myParseErrorListener.hasError()){
                    myParseErrorListener.changeStatu(false);
                }else{
                    //Visitor visitor = new Visitor();
                    //visitor.visit(tree);

                    SysYLlvmVisitor llVisitor = new SysYLlvmVisitor();
                    llVisitor.visit(tree);

                    if (LLVMPrintModuleToFile(llVisitor.module, args[1], error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
                        LLVMDisposeMessage(error);
                    }

                }
            }
        }
    }

}