import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class MyErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?,?> recognizer, java.lang.Object offendingSymbol, int line,
                            int charPositionInLine, java.lang.String msg, org.antlr.v4.runtime.RecognitionException e) {
        /* compiled code */
        throw new ParseCancellationException("Error type A at Line "+line+":"+" "+msg);
    }
}
