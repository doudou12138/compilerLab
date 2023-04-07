import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;

public class Visitor extends SysYParserBaseVisitor{
    static int ws_nums = 0;
    static int ws_num = 2;
    public Object visitChild(RuleNode node){
        // result = this.defaultResult();

        ws_nums+=ws_num;

        Object result = this.defaultResult();
        int n = node.getChildCount();

        for(int i = 0; i < n && this.shouldVisitNextChild(node, result); ++i) {
            ParseTree c = node.getChild(i);
            Object childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }

        for(int i=0;i<ws_nums;i++){
            System.err.print(" ");
        }
        System.err.println(SysYParser.ruleNames[node.getRuleContext().getRuleIndex()]);

        ws_nums-=ws_num;

        return result;
    }

    public void visitTerminal(RuleNode node){

    }
}
