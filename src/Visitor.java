import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor{
    static int ws_nums = -2;
    static int ws_num = 2;


    @Override
    public Object visitChildren(RuleNode node){
        // result = this.defaultResult();
        Object result = this.defaultResult();
        int n = node.getChildCount();

        if(n==0){
            visitTerminal((TerminalNode) node);
        }else{
            ws_nums+=ws_num;
            for(int i=0;i<ws_nums;i++){
                System.err.print(" ");
            }
            String text = SysYParser.ruleNames[node.getRuleContext().getRuleIndex()];
            System.err.println(Utils.showTitle(text));

            for(int i = 0; i < n && this.shouldVisitNextChild(node, result); ++i) {
                ParseTree c = node.getChild(i);
                Object childResult = c.accept(this);
                result = this.aggregateResult(result, childResult);
            }
            ws_nums-=ws_num;
        }

        return result;
    }

    @Override
    public Object visitTerminal(TerminalNode node){
        ws_nums+=ws_num;
        Object result = this.defaultResult();

        int a = node.getSymbol().getType();
        String text = node.getSymbol().getText();
        show_terminal(a,text);
        ws_nums-=ws_num;
        return result;
    }

    public void show_terminal(int a,String text){

        if(a>=25&&a<33){return;}
        if(a>=35&&a<39){return;}
        if(a>=39||a<0){return;}
        for(int i=0;i<ws_nums;++i){
            System.err.print(" ");
        }
        if(a==34){
            System.err.println(Utils.toDecimal(text)+" "+SysYLexer.ruleNames[a-1]+"[green]");
        }else {
            System.err.print(text + " " + SysYLexer.ruleNames[a - 1]);
            if (a >= 0 && a < 10) {
                System.err.println("[orange]");
            }
            if (a >= 10 && a < 25) {
                System.err.println("[blue]");
            }

            if (a == 33) {
                System.err.println("[red]");
            }
        }

    }

}
