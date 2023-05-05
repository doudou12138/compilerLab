import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ErrorVisitor extends SysYParserBaseVisitor{
    public SymbolTable symbolTable;
    private ParseErrorListener parseErrorListener;
    boolean is_const=false;
    Type retype=null;
    Type type = null;

    public ErrorVisitor(SymbolTable symbolTable,ParseErrorListener parseErrorListener){
        this.symbolTable = symbolTable;
        this.parseErrorListener = parseErrorListener;
    }

    @Override
    public Object visitConstDecl(SysYParser.ConstDeclContext ctx){
        String type_str = ctx.bType().getText();
        type = new BasicType(type_str);
        //if(type_str=="INT")

        Object result = this.defaultResult();
        int n = ctx.getChildCount();
        for(int i = 0; i < n && this.shouldVisitNextChild(ctx, result); ++i) {
            ParseTree c = ctx.getChild(i);
            Object childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }
        return result;
    }

    @Override
    public Object visitVarDecl(SysYParser.VarDeclContext ctx){
        String type_str = ctx.bType().getText();
        type = new BasicType(type_str);
        //if(type_str=="INT")

        Object result = this.defaultResult();
        int n = ctx.getChildCount();
        for(int i = 0; i < n && this.shouldVisitNextChild(ctx, result); ++i) {
            ParseTree c = ctx.getChild(i);
            Object childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }
        return result;
    }

    @Override
    public Object visitConstDef(SysYParser.ConstDefContext ctx){
        Object result = this.defaultResult();
        SymbolTable.SymbolTableEntry entry = symbolTable.lookup(ctx.IDENT().getText(),1);
        if(entry!=null){
            //repeat define
            Token token = ctx.IDENT().getSymbol();
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            Object offendingSymbol = token.getText();
            String msg = "3 redefined";
            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
            return null;
        }else {
            Type initType =null;
            if(ctx.constInitVal()!=null){
                initType = (Type) visitConstInitVal(ctx.constInitVal());
            }

            if (ctx.constExp().size() == 0) {
                //basic type
                symbolTable.addEntry(ctx.IDENT().getText(),type,0);
                if(initType!=null&&(!(initType.toString().equals("int")))){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "5 type mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return result;
                }
            } else{
                //array type
                int eleNum[] = new int[ctx.constExp().size()];
                for(int i=0;i<ctx.constExp().size();++i){
                    eleNum[i]=Utils.toDecimal(ctx.constExp(i).getText());
                }

                symbolTable.addEntry(ctx.IDENT().getText(),new ArrayType(type,eleNum,ctx.constExp().size()),0);

                if(initType!=null&&((initType instanceof BasicType)||(initType instanceof ArrayType&&((ArrayType) initType).getDimension()!=ctx.constExp().size()))){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "5 type mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return result;
                }
            }

            return result;
        }
    }

    @Override
    public Object visitConstInitVal(SysYParser.ConstInitValContext ctx){
        if(ctx.constInitVal().size()==0){
            return visitConstExp(ctx.constExp());
        }else{
            Object type=visitConstInitVal(ctx.constInitVal(0));
            if(type==null){
                return null;
            }
            for(int i=1;i<ctx.constInitVal().size();++i){
                if(!(type.toString().equals(visitConstInitVal(ctx.constInitVal(i)).toString()))){
                    Token token = ctx.constInitVal(i).getStart();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "5 type mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }
            }
            if(type instanceof BasicType){
                int eleNum[] = new int[1];
                eleNum[0]=ctx.constInitVal().size();
                return new ArrayType((Type) type,eleNum,1);
            }else if(type instanceof ArrayType){
                int eleNum[] = new int[((ArrayType) type).getDimension()+1];
                eleNum[0]=ctx.constInitVal().size();
                for(int i=0;i<((ArrayType) type).getDimension();++i){
                    eleNum[i+1]=((ArrayType) type).getElementNum(i);
                }
                return new ArrayType(((ArrayType) type).getElementType(),eleNum,((ArrayType) type).getDimension()+1);
            }
        }
        return new BasicType("else");
    }

    @Override
    public Object visitVarDef(SysYParser.VarDefContext ctx){
        Object result = this.defaultResult();
        SymbolTable.SymbolTableEntry entry = symbolTable.lookup(ctx.IDENT().getText(),1);
        if(entry!=null){
            //repeat define
            Token token = ctx.IDENT().getSymbol();
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            Object offendingSymbol = token.getText();
            String msg = "3 redefined";
            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
            return null;
        }else {
            Type initType =null;
            if(ctx.initVal()!=null){
                initType = (Type) visitInitVal(ctx.initVal());
            }

            if (ctx.constExp().size() == 0) {
                //basic type
                symbolTable.addEntry(ctx.IDENT().getText(),type,0);
                if(initType!=null&&(!(initType.toString().equals("int")))){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "5 type mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return result;
                }
            } else {
                //array type
                int eleNum[] = new int[ctx.constExp().size()];
                for(int i=0;i<ctx.constExp().size();++i){
                    eleNum[i]=Utils.toDecimal(ctx.constExp(i).getText());
                }

                symbolTable.addEntry(ctx.IDENT().getText(),new ArrayType(type,eleNum,ctx.constExp().size()),0);

                if(initType!=null&&((initType instanceof BasicType)||(initType instanceof ArrayType&&((ArrayType) initType).getDimension()!=ctx.constExp().size()))){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "5 type mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return result;
                }
            }

            return result;
        }
    }

    @Override
    public Object visitInitVal(SysYParser.InitValContext ctx){
            if(ctx.initVal().size()==0){
                if(ctx.exp()!=null) {
                    return visitExp(ctx.exp());
                }
            }else{
                Object type=visitInitVal(ctx.initVal(0));
                if(type==null){
                    return null;
                }
                for(int i=1;i<ctx.initVal().size();++i){
                    if(!(type.toString().equals(visitInitVal(ctx.initVal(i)).toString()))){
                        Token token = ctx.initVal(i).getStart();
                        int line = token.getLine();
                        int charPositionInLine = token.getCharPositionInLine();
                        Object offendingSymbol = token.getText();
                        String msg = "5 type mismatched";
                        parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                        return null;
                    }
                }
                if(type instanceof BasicType){
                    int eleNum[] = new int[1];
                    eleNum[0]=ctx.initVal().size();
                    return new ArrayType((Type) type,eleNum,1);
                }else if(type instanceof ArrayType){
                    int eleNum[] = new int[((ArrayType) type).getDimension()+1];
                    eleNum[0]=ctx.initVal().size();
                    for(int i=0;i<((ArrayType) type).getDimension();++i){
                        eleNum[i+1]=((ArrayType) type).getElementNum(i);
                    }
                    return new ArrayType(((ArrayType) type).getElementType(),eleNum,((ArrayType) type).getDimension()+1);
                }
            }
            return new BasicType("else");
    }

    @Override
    public Object visitExp(SysYParser.ExpContext ctx){
        if(ctx.exp().size()==0){
            if(ctx.lVal()!=null){
                Type lvalType = (Type) visitLVal(ctx.lVal());
                 if(lvalType instanceof FunctionType){
                     Token token = ctx.lVal().getStart();
                     int line = token.getLine();
                     int charPositionInLine = token.getCharPositionInLine();
                     Object offendingSymbol = token.getText();
                     String msg = "6 "+ctx.lVal().getText()+" is a function name";
                     parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                     return null;
                 }else{
                     return lvalType;
                 }
            }else if(ctx.IDENT()!=null){
                SymbolTable.SymbolTableEntry entry=symbolTable.lookup(ctx.IDENT().getText(),2);
                if(entry==null){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "2 no defined function type";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }else if(!(entry.getType() instanceof FunctionType)){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "10"+ctx.IDENT().getText()+" is not a function";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }
                //check paras
                ArrayList<Type> parasType =null;
                if(ctx.funcRParams()!=null) {
                   parasType = (ArrayList<Type>) visitFuncRParams(ctx.funcRParams());
                }

                if(parasType!=null&&((FunctionType) entry.getType()).getParameterTypes()!=null){
                    for(int i=0;i<parasType.size();++i){
                        if(parasType.get(i)==null){
                            return null;
                        }
                    }
                }
                boolean parasMis=true;
                if(parasType==null&&((FunctionType) entry.getType()).getParameterTypes()==null){
                    parasMis = false;
                }else if((parasType==null&&((FunctionType) entry.getType()).getParameterTypes()!=null)||(parasType!=null&&((FunctionType) entry.getType()).getParameterTypes()==null)){

                }else if(parasType!=null&&((FunctionType) entry.getType()).getParameterTypes().size()==parasType.size()){
                    {
                        boolean same=true;
                        for (int i = 0; i < parasType.size(); ++i) {
                            if (!((FunctionType) entry.getType()).getParameterTypes().get(i).toString().equals(parasType.get(i).toString())) {
                                same=false;
                                break;
                            }
                        }
                        if(same){
                            parasMis=false;
                        }
                    }
                }
                if(parasMis){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "8 "+ctx.IDENT().getText()+"'s paras mismatched";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }

                Type type = ((FunctionType)entry.getType()).getReturnType();
                if(type.toString().equals("void")){
                    return type;
                }else{
                    return type;
                }
            }else{
                return new BasicType("int");
            }
        }else {
            if(ctx.L_PAREN()!=null){
                return visitExp(ctx.exp(0));
            }else {
                if (ctx.exp() != null) {
                    Type type=null;
                    for (int i = 0; i < ctx.exp().size(); ++i) {
                        type = (Type) visitExp(ctx.exp(i));
                        if (type == null) {
                            return null;
                        } else if (!(type instanceof BasicType && type.toString().equals("int"))) {
                            Token token = ctx.exp(0).getStart();
                            int line = token.getLine();
                            int charPositionInLine = token.getCharPositionInLine();
                            Object offendingSymbol = token.getText();
                            String msg = "6 the calculation needs type int";
                            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                            return null;
                        }
                    }
                    return type;
                }
                return type;
            }

        }
    }

    @Override
    public Object visitLVal(SysYParser.LValContext ctx){
        SymbolTable.SymbolTableEntry entry = symbolTable.lookup(ctx.IDENT().getText(),2);
        if(entry==null){
            Token token = ctx.IDENT().getSymbol();
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            Object offendingSymbol = token.getText();
            String msg = "1 no defined value";
            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
            return null;
        }else{
            Type entryType = entry.getType();
            if(entryType instanceof BasicType){
                if(ctx.exp().size()!=0){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "9 "+ctx.IDENT().getText()+" is not an array";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }
                return entryType;
            }else if(entryType instanceof FunctionType){
                if(ctx.exp().size()!=0){
                    Token token = ctx.IDENT().getSymbol();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "9 "+ctx.IDENT().getText()+"is not an array";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    return null;
                }else{
                    return entryType;
                }
            }else if(entryType instanceof ArrayType){
                if(ctx.exp().size()==0){
                    return entryType;
                }else {
                    if (ctx.exp().size() > ((ArrayType) entry.getType()).getDimension()) {
                        Token token = ctx.IDENT().getSymbol();
                        int line = token.getLine();
                        int charPositionInLine = token.getCharPositionInLine();
                        Object offendingSymbol = token.getText();
                        String msg = "9 " + ctx.IDENT().getText() + "is not an array";
                        parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                        return null;
                    } else if (ctx.exp().size() == ((ArrayType) entry.getType()).getDimension()) {
                        return new BasicType("int");
                    } else {
                        int dimension = ((ArrayType) entry.getType()).getDimension()-ctx.exp().size();
                        int eleNum[] = new int[dimension];
                        for (int i = 0; i < dimension; ++i) {
                            eleNum[i] = ((ArrayType) entry.getType()).getElementNum(ctx.exp().size() + i);
                        }
                        return new ArrayType(new BasicType("int"), eleNum, dimension);
                    }
                }
            }else{
                //not so far
                return null;
            }
        }
    }

    @Override
    public Object visitFuncDef(SysYParser.FuncDefContext ctx){
        Object result = this.defaultResult();
        SymbolTable.SymbolTableEntry entry = symbolTable.lookup(ctx.IDENT().getText(),2);
        if(entry!=null){
            Token token = ctx.IDENT().getSymbol();
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            Object offendingSymbol = token.getText();
            String msg = "4 redefined function name";
            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
            return result;
        }else{
            symbolTable.enterScope();
            retype=new BasicType(ctx.funcType().getText());
            Object paras=null;
            if(ctx.funcFParams()!=null){
                paras = this.visitFuncFParams(ctx.funcFParams(),1);
            }
            FunctionType func=null;
            if (paras instanceof List) {
                List<Type> paraList = (List<Type>) paras;
                func = new FunctionType(paraList, retype);
            }else{
                func = new FunctionType(null,retype);
            }
            symbolTable.exitScope();
            symbolTable.addEntry(ctx.IDENT().getText(),func,0);
        }

        symbolTable.enterScope();
        if(ctx.funcFParams()!=null){
            this.visitFuncFParams(ctx.funcFParams(),2);
        }
        this.visitBlock(ctx.block());
        return result;
    }

    public Object visitFuncFParams(SysYParser.FuncFParamsContext ctx,int way){
        ArrayList<Type> parasType = new ArrayList<>();
        for(int i=0;i<ctx.funcFParam().size();++i){
            Object paraType = visitFuncFParam(ctx.funcFParam(i),way);
            //if(paraType==null){
            //    break;
            //}
            parasType.add((Type) paraType);
        }

        return parasType;
    }

    @Override
    public Object visitFuncRParams(SysYParser.FuncRParamsContext ctx){
        ArrayList<Type> parasType = new ArrayList<>();
        for(int i=0;i<ctx.param().size();++i){
            parasType.add((Type) visitParam(ctx.param(i)));
        }
        return parasType;
    }

    public Object visitParam(SysYParser.ParamContext ctx){
        Type type = (Type) visitExp(ctx.exp());
        return type;
    }


    @Override
    public Object visitStmt(SysYParser.StmtContext ctx){
        Object result = this.defaultResult();
        if(ctx.lVal()!=null){
            Type type_l= (Type) visitLVal(ctx.lVal());
            Type type_r=(Type) visitExp(ctx.exp());
            if(type_l==null||type_r==null){
                return null;
            }else if(type_l instanceof BasicType&&type_r instanceof BasicType){
                return null;
            }else if(type_l instanceof ArrayType && type_r instanceof ArrayType &&((ArrayType) type_l).getDimension()==((ArrayType) type_r).getDimension()){

            }else if(type_l instanceof FunctionType) {
                Token token = ctx.lVal().getStart();
                int line = token.getLine();
                int charPositionInLine = token.getCharPositionInLine();
                Object offendingSymbol = token.getText();
                String msg = "11"+"assign for the function";
                parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                return null;
            }else{
                Token token = ctx.lVal().getStart();
                int line = token.getLine();
                int charPositionInLine = token.getCharPositionInLine();
                Object offendingSymbol = token.getText();
                String msg = "5 type mismatched between the left and the right";
                parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
            }
        }else if(ctx.RETURN()!=null){
            if(ctx.exp()==null){
                if(Objects.equals(retype.toString(), "void")) {
                    return result;
                }else{
                    Token token = ctx.lVal().getStart();
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    Object offendingSymbol = token.getText();
                    String msg = "7 return type mismatched ";
                    parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                }
            }else{
                Type retType = (Type) visitExp(ctx.exp());
                if(retType instanceof BasicType){
                    if(visitExp(ctx.exp()).toString().equals(retype.toString())){

                    }else{
                        Token token = ctx.RETURN().getSymbol();
                        int line = token.getLine();
                        int charPositionInLine = token.getCharPositionInLine();
                        Object offendingSymbol = token.getText();
                        String msg = "7 return type mismatched ";
                        parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    }
                }else if(retType instanceof ArrayType){
                    if(!(retype instanceof ArrayType)){
                        Token token = ctx.exp().getStart();
                        int line = token.getLine();
                        int charPositionInLine = token.getCharPositionInLine();
                        Object offendingSymbol = token.getText();
                        String msg = "7 return type mismatched ";
                        parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                    }else{
                        if(((ArrayType) retType).getDimension()==((ArrayType)retype).getDimension()){

                        }else{
                            Token token = ctx.lVal().getStart();
                            int line = token.getLine();
                            int charPositionInLine = token.getCharPositionInLine();
                            Object offendingSymbol = token.getText();
                            String msg = "7 return type mismatched ";
                            parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                        }
                    }
                }
                return result;
            }
        }else if(ctx.block()!=null){
            symbolTable.enterScope();
            visitBlock(ctx.block());
        }else if(ctx.exp()!=null){
            visitExp(ctx.exp());
        }else {
            if(ctx.cond()!=null){
                boolean conError = (Boolean)visitCond(ctx.cond());
                if(!conError) {
                    if (ctx.stmt().size() != 0) {
                        for (int i = 0; i < ctx.stmt().size(); ++i) {
                            visitStmt(ctx.stmt(i));
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Object visitCond(SysYParser.CondContext ctx){
        if(ctx.cond()==null||ctx.cond().size()==0){
            Object type= visitExp(ctx.exp());
            if(type==null){
                return true;
            }
            return false;
        }else{
            Object conError=null;
            for(int i=0;i<ctx.cond().size();++i){
                conError=visitCond(ctx.cond(i));
                if((boolean) conError){
                    return true;
                }
            }
            return false;
        }
    }

    public Object visitFuncFParam(SysYParser.FuncFParamContext ctx,int way){
        Type type=null;
        if(ctx.L_BRACKT().size()==0){
            type = new BasicType("int");
        }else{
            type = new ArrayType(new BasicType("int"),new int[0],ctx.L_BRACKT().size());
        }

        SymbolTable.SymbolTableEntry entry = symbolTable.lookup(ctx.IDENT().getText(),1);
        if(entry!=null) {
            if (way == 1) {
                Token token = ctx.IDENT().getSymbol();
                int line = token.getLine();
                int charPositionInLine = token.getCharPositionInLine();
                Object offendingSymbol = token.getText();
                String msg = "3 redefined variable";
                parseErrorListener.syntaxError(null, offendingSymbol, line, charPositionInLine, msg, null);
                //return null;
            }
        }else{
            //Type type=null;
            //if(ctx.L_BRACKT().size()==0){
            //    type = new BasicType("int");
            //}else{
            //    type = new ArrayType(new BasicType("int"),new int[0],ctx.L_BRACKT().size());
            //}
            symbolTable.addEntry(ctx.IDENT().getText(),type,0);
        }

        return type;
    }

    @Override
    public Object visitBlock(SysYParser.BlockContext ctx){
        Object result = this.defaultResult();
        int n = ctx.getChildCount();
        for(int i = 0; i < n && this.shouldVisitNextChild(ctx, result); ++i) {
            ParseTree c = ctx.getChild(i);
            Object childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }

        symbolTable.exitScope();
        return result;
    }

    @Override
    public Object visitCompUnit(SysYParser.CompUnitContext ctx) {
        Object result = this.defaultResult();
        int n = ctx.getChildCount();
        for(int i = 0; i < n && this.shouldVisitNextChild(ctx, result); ++i) {
            ParseTree c = ctx.getChild(i);
            Object childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }
        return result;
    }
}
