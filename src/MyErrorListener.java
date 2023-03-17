import org.antlr.v4.runtime.BaseErrorListener;


public class MyErrorListener extends BaseErrorListener {
    private boolean hasError = false;
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?,?> recognizer, java.lang.Object offendingSymbol, int line,
                            int charPositionInLine, java.lang.String msg, org.antlr.v4.runtime.RecognitionException e) {
        /* compiled code */
        hasError=true;
        System.err.println("Error type A at Line "+line+":"+" "+msg+".");
    }

    public boolean hasError(){
        return hasError;
    }

}
