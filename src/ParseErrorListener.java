import org.antlr.v4.runtime.BaseErrorListener;

public class ParseErrorListener extends BaseErrorListener {
    private boolean has_Error = false;
    private boolean is_mean = false;
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?,?> recognizer, java.lang.Object offendingSymbol, int line,
                            int charPositionInLine, java.lang.String msg, org.antlr.v4.runtime.RecognitionException e) {
        /* compiled code */
        has_Error = true;
        if (!is_mean) {
            System.err.println("Error type B at Line " + line + ":" + " " + msg + ".");
        }else{
            System.err.println("Error type "+(msg.charAt(1)==' '?msg.charAt(0):msg.substring(0,2))+" at Line "+line+": "+msg.substring(2)+".");
        }
    }

    public boolean hasError(){
        return has_Error;
    }

    public void changeStatu(boolean a){
        has_Error = a;
    }

    public void setMean(){
        is_mean=true;
    }
}
