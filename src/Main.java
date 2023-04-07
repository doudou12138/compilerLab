import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        LexerErrorListener myLexerErrorListener = new LexerErrorListener();
        ParseErrorListener myParseErrorListener = new ParseErrorListener();
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(myLexerErrorListener);

        List<? extends Token> tokens = sysYLexer.getAllTokens();

        if (myLexerErrorListener.hasError()) {
            myLexerErrorListener.changeStatu(false);
        } else {
            for (Token i : tokens) {
                int type_n = i.getType();
                if (type_n == 34) {
                    String tokenText = i.getText();
                    if (tokenText.charAt(0) == '0') {
                        if (tokenText.length() == 1) {
                            tokenText = "0";
                        } else {
                            if ((tokenText.charAt(1) - 'x' == 0) || (tokenText.charAt(1) - 'X' == 0)) {
                                tokenText = String.valueOf(Integer.parseInt(tokenText.substring(2), 16));
                            } else {
                                tokenText = String.valueOf(Integer.parseInt(tokenText.substring(1), 8));
                            }
                        }
                    }
                    System.err.println(SysYLexer.ruleNames[type_n - 1] + " " + tokenText + " at Line " + i.getLine() + ".");
                } else {
                    System.err.println(SysYLexer.ruleNames[type_n - 1] + " " + i.getText() + " at Line " + i.getLine() + ".");
                }
            }


            CommonTokenStream tokenStream = new CommonTokenStream(sysYLexer);
            SysYParser sysYParser = new SysYParser(tokenStream);
            sysYParser.removeErrorListeners();
            sysYParser.addErrorListener(myParseErrorListener);

            SysYParser.ProgramContext prog = sysYParser.program();
            // 如果有语法错误，输出错误信息
            if (myParseErrorListener.hasError()) {
                myParseErrorListener.changeStatu(false);
            } else {
                System.err.println("yeyeye");
            }

        }
    }

}