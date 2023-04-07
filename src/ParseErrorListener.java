import org.antlr.v4.runtime.BaseErrorListener;

public class ParseErrorListener extends BaseErrorListener {
    private boolean has_Error = false;
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?,?> recognizer, java.lang.Object offendingSymbol, int line,
                            int charPositionInLine, java.lang.String msg, org.antlr.v4.runtime.RecognitionException e) {
        /* compiled code */
        has_Error=true;
        System.err.println("Error type B at Line "+line+":"+" "+msg+".");
    }

    public boolean hasError(){
        return has_Error;
    }

    public void changeStatu(boolean a){
        has_Error = a;
    }
}
