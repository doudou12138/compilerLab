
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeTarget;

public class SysYLlvmVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    LLVMModuleRef module=null;
    LLVMBuilderRef builder=null;
    LLVMTypeRef i32Type=null;
    LLVMBasicBlockRef block=null;

    LLVMValueRef func_now = null;
    Stack<LLVMBasicBlockRef> blocks = null;

    LLVMSymbolTable llvmSymbolTable = null;

    Stack<LLVMBasicBlockRef> con = null;
    Stack<LLVMBasicBlockRef> ne_block=null;
    Stack<LLVMBasicBlockRef> con_true_ne = null;
    Stack<LLVMBasicBlockRef> con_f_ne = null;

    Map<LLVMValueRef,Integer> types=null;

    public SysYLlvmVisitor(){
        llvmSymbolTable=new LLVMSymbolTable();
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        //创建module
        module = LLVMModuleCreateWithName("moudle");
        //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        builder = LLVMCreateBuilder();
        //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
        i32Type = LLVMInt32Type();

        blocks = new Stack<>();
        con = new Stack<>();
        ne_block = new Stack<>();
        con_true_ne = new Stack<>();
        con_f_ne = new Stack<>();
        types = new HashMap<>();
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx){
        //全局变量定义，函数定义
        if(ctx.decl().size()!=0){
            for(int i=0;i<ctx.decl().size();++i){
                if(ctx.decl(i).constDecl()==null){
                    List<SysYParser.VarDefContext> varDefs = ctx.decl(i).varDecl().varDef();
                    for(int j=0;j<varDefs.size();++j) {
                        //创建一个常量,这里是常数0
                        LLVMValueRef value;
                        if (varDefs.get(j).L_BRACKT().size() == 0) {
                            //全局变量为i32type型
                            value = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                            if (varDefs.get(j).initVal() != null) {
                                value = visitInitVal(varDefs.get(j).initVal());
                            }

                            //创建名为globalVar的全局变量
                            LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varDefs.get(j).IDENT().getText());
                            llvmSymbolTable.addEntry(varDefs.get(j).IDENT().getText(), globalVar, 0);
                            types.put(globalVar,1);
                            //为全局变量设置初始化器
                            LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);
                        }else{
                            //全局变量为array型
                            int len=0;
                            if(varDefs.get(j).constExp().size()!=0) {
                                len = Utils.toDecimal(varDefs.get(j).constExp(0).exp().number().getText());
                            }
                            LLVMTypeRef arrayType = LLVMArrayType(i32Type, len);

                            LLVMValueRef[] initVa = new LLVMValueRef[len];
                            LLVMValueRef initMeth = null;
                            int m=0;
                            if(varDefs.get(j).initVal()!=null){
                                for(;m<varDefs.get(j).initVal().initVal().size();++m){
                                    if(varDefs.get(j).initVal().initVal(m).exp()!=null) {
                                        initVa[m] = visitExp(varDefs.get(j).initVal().initVal(m).exp());
                                    }
                                }
                            }

                            for(;m<len;++m){
                                initVa[m]=LLVMConstInt(i32Type,0,0);
                            }


                            initMeth = LLVMConstArray(i32Type,new PointerPointer<>(initVa),len);
                            // 创建全局变量并设置类型、名称和初始值
                            LLVMValueRef globalVar = LLVMAddGlobal(module, arrayType, varDefs.get(j).IDENT().getText());
                            LLVMSetInitializer(globalVar,initMeth);
                            llvmSymbolTable.addEntry(varDefs.get(j).IDENT().getText(), globalVar, 0);
                            types.put(globalVar,2);

                        }
                    }
                }else{
                    List<SysYParser.ConstDefContext> constDefs = ctx.decl(i).constDecl().constDef();
                    for(int j=0;j<constDefs.size();++j){

                        LLVMValueRef value;
                        if(constDefs.get(j).L_BRACKT().size()==0){
                            //创建一个常量,这里是常数0
                            value = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                            if(constDefs.get(j).constInitVal()!=null){
                                value = visitConstInitVal(constDefs.get(j).constInitVal());
                            }
                            //创建名为globalVar的全局变量
                            LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/constDefs.get(j).IDENT().getText());
                            llvmSymbolTable.addEntry(constDefs.get(j).IDENT().getText(),globalVar,0);
                            //为全局变量设置初始化器
                            LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);
                            types.put(globalVar,1);
                        }else{
                            // 获取全局变量类型 [5 x i32]
                            int len = 0;
                            if(constDefs.get(j).constExp().size()!=0){
                                len=Utils.toDecimal(constDefs.get(j).constExp(0).exp().getText());
                            }
                            LLVMTypeRef arrayType = LLVMArrayType(i32Type, len);

                            LLVMValueRef initMeth = null;
                            // 创建全局变量并设置类型、名称和初始值
                            LLVMValueRef[] initVa = new LLVMValueRef[len];

                            int m=0;
                            if(constDefs.get(j).constInitVal()!=null){
                                for(;m<constDefs.get(j).constInitVal().constInitVal().size();++m){
                                    initVa[m] = visitConstExp(constDefs.get(j).constInitVal().constInitVal(m).constExp());
                                }
                            }
                            for(;m<len;++m){
                                initVa[m]=LLVMConstInt(i32Type,0,0);
                            }


                            initMeth = LLVMConstArray(i32Type,new PointerPointer<>(initVa),len);
                            LLVMValueRef globalVar = LLVMAddGlobal(module, arrayType, constDefs.get(j).IDENT().getText());
                            LLVMSetInitializer(globalVar, initMeth);
                            llvmSymbolTable.addEntry(constDefs.get(j).IDENT().getText(),globalVar,0);
                            types.put(globalVar,2);

                        }

                    }
                }
            }
        }

        for(int i=0;i<ctx.funcDef().size();++i){
            visitFuncDef(ctx.funcDef(i));
        }

        return null;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx){
        //生成返回值类型
        LLVMTypeRef returnType = LLVMVoidType();
        if(ctx.funcType().INT()!=null){
            returnType = i32Type;
        }

        LLVMTypeRef ft=null;
        if(ctx.funcFParams()==null){
            ft = LLVMFunctionType(returnType, (LLVMTypeRef) null, /* argumentCount */ 0, /* isVariadic */ 0);
        }else if(ctx.funcFParams().funcFParam()==null){
            ft = LLVMFunctionType(returnType, (LLVMTypeRef) null, /* argumentCount */ 0, /* isVariadic */ 0);
        }else if(ctx.funcFParams().funcFParam().size()==1){
            if(ctx.funcFParams().funcFParam(0).L_BRACKT()==null) {
                ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 1, /* isVariadic */ 0);
            }else{
                LLVMTypeRef pointerType = LLVMPointerType(i32Type, 0);
                ft=LLVMFunctionType(returnType,pointerType,1,0);
            }
        }else{
            LLVMTypeRef pointerType = LLVMPointerType(i32Type, 0);

            List<SysYParser.FuncFParamContext> paras = ctx.funcFParams().funcFParam();
            //生成函数参数类型
            PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paras.size());
            for(int m=0;m<paras.size();++m){
                if(paras.get(m).L_BRACKT().size()==0){
                    argumentTypes.put(m,i32Type);
                }else{
                    argumentTypes.put(m,pointerType);
                }
            }

            //生成函数类型
            ft = LLVMFunctionType(returnType, argumentTypes , /* argumentCount */ paras.size(), /* isVariadic */ 0);

        }

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/ctx.IDENT().getText(), ft);
        llvmSymbolTable.addEntry(ctx.IDENT().getText(),function,0);

        func_now = function;

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        block = LLVMAppendBasicBlock(function, /*blockName:String*/ctx.IDENT().getText()+"Entry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block);//后续生成的指令将追加在block1的后面

        blocks.push(block);
        llvmSymbolTable.enterScope();

        if(ctx.funcFParams()!=null){
            for(int m=0;m<ctx.funcFParams().funcFParam().size();++m) {
                LLVMValueRef pointer;
                if (ctx.funcFParams().funcFParam(m).L_BRACKT().size() != 0) {
                    pointer = LLVMBuildAlloca(builder,LLVMPointerType(i32Type,0),ctx.funcFParams().funcFParam(m).IDENT().getText());
                    types.put(pointer,3);
                } else {
                    pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/ctx.funcFParams().funcFParam(m).IDENT().getText());
                    types.put(pointer,1);
                }
                LLVMBuildStore(builder, LLVMGetParam(function, m), pointer);
                llvmSymbolTable.addEntry(ctx.funcFParams().funcFParam(m).IDENT().getText(), pointer, 0);
            }
        }

        visit(ctx.block());
        llvmSymbolTable.exitScope();

        if(ctx.funcType().VOID()!=null){
            LLVMPositionBuilderAtEnd(builder,block);
            LLVMBuildRet(builder,null);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitParam(SysYParser.ParamContext ctx){
        if(ctx.exp().lVal()!=null) {
            return visitLVal(ctx.exp().lVal());
        }else{
            return visitExp(ctx.exp());
        }
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx){
        LLVMValueRef pointer;
        if(ctx.constExp().size()==0){
            pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/ctx.IDENT().getText());
            llvmSymbolTable.addEntry(ctx.IDENT().getText(),pointer,0);

            LLVMValueRef value = LLVMConstInt(i32Type,0,0);
            if(ctx.initVal()!=null){
                value = visitInitVal(ctx.initVal());
            }
            LLVMBuildStore(builder,value,pointer);
        }else{
            int num = Utils.toDecimal(ctx.constExp(0).exp().getText());
            LLVMTypeRef point = LLVMArrayType(i32Type,num);
            pointer = LLVMBuildAlloca(builder,point,ctx.IDENT().getText());
            llvmSymbolTable.addEntry(ctx.IDENT().getText(),pointer,0);

            types.put(pointer,2);
            LLVMValueRef[] initVa = new LLVMValueRef[num];
            for(int m=0;m<num;++m){
                initVa[m] = LLVMConstInt(i32Type,0,0);
            }
            if(ctx.initVal()!=null){
                for(int m=0;m<ctx.initVal().initVal().size();++m){
                    initVa[m]=visitExp(ctx.initVal().initVal(m).exp());
                }
            }

            LLVMValueRef initMe = LLVMConstArray(i32Type,new PointerPointer<>(initVa),num);
            LLVMSetInitializer(pointer,initMe);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitInitVal(SysYParser.InitValContext ctx){
        if(ctx.L_BRACE()==null&&ctx.exp()!=null){
            return visitExp(ctx.exp());
        }
        return null;
    }

    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx){
        LLVMValueRef pointer;
        if(ctx.constExp()==null) {
            pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/ctx.IDENT().getText());
            llvmSymbolTable.addEntry(ctx.IDENT().getText(), pointer, 0);

            LLVMValueRef value = LLVMConstInt(i32Type, 0, 0);
            if (ctx.constInitVal() != null) {
                value = visitConstInitVal(ctx.constInitVal());
            }
            LLVMBuildStore(builder, value, pointer);
        }else{
            LLVMTypeRef point = LLVMPointerType(i32Type,0);
            pointer = LLVMBuildAlloca(builder,point,ctx.IDENT().getText());
            llvmSymbolTable.addEntry(ctx.IDENT().getText(),pointer,0);

            int num = Utils.toDecimal(ctx.constExp(0).exp().getText());
            LLVMValueRef[] val = new LLVMValueRef[num];
            for(int m=0;m<num;++m){
                val[m]=LLVMConstInt(i32Type,0,0);
            }
            if(ctx.constInitVal()!=null){
                for(int m=0;m<ctx.constInitVal().constInitVal().size();++m){
                    val[m]=visitConstExp(ctx.constInitVal().constInitVal(m).constExp());
                }
            }

            LLVMValueRef initMe = LLVMConstArray(i32Type,new PointerPointer<>(val),num);
            LLVMSetInitializer(pointer,initMe);

        }
        return null;
    }

    @Override
    public  LLVMValueRef visitConstExp(SysYParser.ConstExpContext ctx){
        return visitExp(ctx.exp());
    }

    @Override
    public LLVMValueRef visitConstInitVal(SysYParser.ConstInitValContext ctx){
        if(ctx.L_BRACE()==null&&ctx.constExp()!=null){
            return visitConstExp(ctx.constExp());
        }
        return null;
    }

    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx){
        if(ctx.RETURN()!=null){
            LLVMValueRef result = null;
            if(ctx.exp()!=null){
                result = visitExp(ctx.exp());
            }
            //函数返回指令
            if(result!=null) {
                LLVMBuildRet(builder, /*result:LLVMValueRef*/result);
            }
        }else if(ctx.block()!=null){
            llvmSymbolTable.enterScope();
            visitBlock(ctx.block());
            llvmSymbolTable.exitScope();
        }else if(ctx.ASSIGN()!=null){
            LLVMValueRef lva = visitLVal(ctx.lVal());
            LLVMValueRef value = visitExp(ctx.exp());
            LLVMBuildStore(builder,value,lva);
        }else if(ctx.IF()!=null){

            LLVMBasicBlockRef con_block = LLVMAppendBasicBlock(func_now,"condition");

            LLVMPositionBuilderAtEnd(builder,blocks.pop());
            LLVMBuildBr(builder,con_block);
            LLVMPositionBuilderAtEnd(builder,con_block);

            LLVMBasicBlockRef trueBlock = LLVMAppendBasicBlock(func_now,"the_true");
            LLVMBasicBlockRef falseBlock= LLVMAppendBasicBlock(func_now,"the_false");

            blocks.push(con_block);
            con_true_ne.push(trueBlock);
            con_f_ne.push(falseBlock);
            LLVMValueRef cond = visitCond(ctx.cond());
            con_true_ne.pop();
            con_f_ne.pop();
            blocks.pop();


            //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
            LLVMBasicBlockRef next_block = LLVMAppendBasicBlock(func_now, /*blockName:String*/"the_next");

            LLVMPositionBuilderAtEnd(builder,trueBlock);
            blocks.push(trueBlock);
            visitStmt(ctx.stmt(0));

            LLVMPositionBuilderAtEnd(builder,blocks.pop());
            LLVMBuildBr(builder,next_block);

            if(ctx.ELSE()!=null){
                LLVMPositionBuilderAtEnd(builder,falseBlock);
                blocks.push(falseBlock);
                visitStmt(ctx.stmt(1));
                LLVMPositionBuilderAtEnd(builder,blocks.pop());
                LLVMBuildBr(builder,next_block);
            }else{
                LLVMPositionBuilderAtEnd(builder,falseBlock);
                LLVMBuildBr(builder,next_block);
            }


            LLVMPositionBuilderAtEnd(builder,con_block);
            //条件跳转指令，选择跳转到哪个块
            LLVMValueRef cond_end = LLVMBuildICmp(builder,LLVMIntNE,LLVMConstInt(i32Type,0,0),cond,"cond");
            LLVMBuildCondBr(builder, /*condition:LLVMValueRef*/ cond_end,trueBlock/*ifTrue:LLVMBasicBlockRef*/,falseBlock/*ifFalse:LLVMBasicBlockRef*/);

            //选择要在哪个基本块后追加指令
            LLVMPositionBuilderAtEnd(builder, next_block);//后续生成的指令将追加在block1的后面

            blocks.push(next_block);
        }else if(ctx.WHILE()!=null){
            LLVMBasicBlockRef con_block = LLVMAppendBasicBlock(func_now,"condition");
            LLVMBasicBlockRef whi_body = LLVMAppendBasicBlock(func_now,"while_body");
            LLVMBasicBlockRef next_block =LLVMAppendBasicBlock(func_now,"next_block");

            con.push(con_block);
            ne_block.push(next_block);

            LLVMPositionBuilderAtEnd(builder,blocks.pop());
            LLVMBuildBr(builder,con_block);

            LLVMPositionBuilderAtEnd(builder,con_block);

            blocks.push(con_block);
            con_true_ne.push(whi_body);
            con_f_ne.push(next_block);
            LLVMValueRef cond = visitCond(ctx.cond());
            con_f_ne.pop();
            con_true_ne.pop();
            blocks.pop();

            LLVMPositionBuilderAtEnd(builder,con_block);
            //条件跳转指令，选择跳转到哪个块

            LLVMValueRef cond_end = LLVMBuildICmp(builder,LLVMIntNE,LLVMConstInt(i32Type,0,0),cond,"cond");
            LLVMBuildCondBr(builder, /*condition:LLVMValueRef*/ cond_end,whi_body/*ifTrue:LLVMBasicBlockRef*/,next_block/*ifFalse:LLVMBasicBlockRef*/);

            LLVMPositionBuilderAtEnd(builder,whi_body);
            blocks.push(whi_body);
            visitStmt(ctx.stmt(0));

            LLVMBasicBlockRef whi_body_end = blocks.pop();

            LLVMPositionBuilderAtEnd(builder, whi_body_end);
            LLVMBuildBr(builder,con_block);

            LLVMPositionBuilderAtEnd(builder,next_block);
            blocks.push(next_block);

        }else if(ctx.BREAK()!=null){
            LLVMBuildBr(builder,ne_block.pop());
        }else if(ctx.CONTINUE()!=null){
            LLVMBuildBr(builder,con.pop());
        }else if(ctx.exp()!=null){
            //visitExp(ctx.exp());
        }
        return null;
    }

    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return visitExp(ctx.exp());
        } else {
            LLVMValueRef cond = null;
            LLVMValueRef cond1 = null;
            LLVMValueRef cond2 = null;

            cond1 = visitCond(ctx.cond(0));
            if(ctx.AND()!=null){
                LLVMBasicBlockRef conl_true = LLVMAppendBasicBlock(func_now,"conl_true");
                LLVMBasicBlockRef conl_false = LLVMAppendBasicBlock(func_now,"conl_false");
                LLVMValueRef isEqualToZero = LLVMBuildICmp(builder, LLVMIntEQ, cond1, LLVMConstInt(LLVMInt32Type(), 0, 0), "cmp");
                LLVMBuildCondBr(builder,isEqualToZero,conl_false,conl_true);

                LLVMPositionBuilderAtEnd(builder,conl_false);

                LLVMBasicBlockRef con_fa_ne = con_f_ne.pop();
                LLVMBuildBr(builder,con_fa_ne);
                con_f_ne.push(con_fa_ne);

                LLVMPositionBuilderAtEnd(builder,conl_true);
                cond2 = visitCond(ctx.cond(1));
                isEqualToZero = LLVMBuildICmp(builder,LLVMIntEQ,cond2,LLVMConstInt(LLVMInt32Type(),0,0),"cmp_2");

                LLVMBasicBlockRef con_tr_ne = con_true_ne.pop();
                con_fa_ne = con_f_ne.pop();
                LLVMValueRef isZero_32 = LLVMBuildZExt(builder,isEqualToZero,i32Type,"chek_32");
                LLVMBuildCondBr(builder,isEqualToZero,con_tr_ne,con_fa_ne);
                con_f_ne.push(con_fa_ne);
                con_true_ne.push(con_tr_ne);

                return isZero_32;
            }else if(ctx.OR()!=null){
                LLVMBasicBlockRef conl_true = LLVMAppendBasicBlock(func_now,"conl_true");
                LLVMBasicBlockRef conl_false = LLVMAppendBasicBlock(func_now,"conl_false");
                LLVMValueRef isEqualToOne = LLVMBuildICmp(builder, LLVMIntNE, cond1, LLVMConstInt(LLVMInt32Type(), 0, 0), "cmp");
                LLVMBuildCondBr(builder,isEqualToOne,conl_true,conl_false);

                LLVMPositionBuilderAtEnd(builder,conl_true);

                LLVMBasicBlockRef con_tr_ne = con_true_ne.pop();
                LLVMBuildBr(builder,con_tr_ne);
                con_true_ne.push(con_tr_ne);

                LLVMPositionBuilderAtEnd(builder,conl_false);
                cond2 = visitCond(ctx.cond(1));
                isEqualToOne = LLVMBuildICmp(builder,LLVMIntNE,cond2,LLVMConstInt(LLVMInt32Type(),0,0),"cmp_2");

                LLVMValueRef isOne_32 = LLVMBuildZExt(builder,isEqualToOne,i32Type,"isONe_32");
                LLVMBasicBlockRef con_fa_ne = con_f_ne.pop();
                con_tr_ne = con_true_ne.pop();
                LLVMBuildCondBr(builder,isEqualToOne,con_tr_ne,con_fa_ne);
                con_f_ne.push(con_fa_ne);
                con_true_ne.push(con_tr_ne);

                return isOne_32;
            }else{
                cond2 = visitCond(ctx.cond(1));

                if (ctx.LT() != null) {
                    cond = LLVMBuildICmp(builder,LLVMIntSLT,cond1,cond2,"lt");
                }else if(ctx.GT()!=null){
                    cond = LLVMBuildICmp(builder,LLVMIntSGT,cond1,cond2,"gt");
                }else if(ctx.GE()!=null){
                    cond = LLVMBuildICmp(builder,LLVMIntSGE,cond1,cond2,"ge");
                }else if(ctx.LE()!=null){
                    cond = LLVMBuildICmp(builder,LLVMIntSLE,cond1,cond2,"le");
                }else if(ctx.EQ()!=null){
                    cond = LLVMBuildICmp(builder,LLVMIntEQ,cond1,cond2,"eq");
                }else if(ctx.NEQ()!=null){
                    cond = LLVMBuildICmp(builder,LLVMIntNE,cond1,cond2,"ne");
                }else {

                }
                LLVMValueRef cond_i32 = LLVMBuildZExt(builder,cond,i32Type,"cond");
                return cond_i32;
            }
        }
    }


    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx){

        if(ctx.exp().size()==0){
            if(ctx.number()!=null){
                return LLVMConstInt(i32Type,Utils.toDecimal(ctx.number().getText()),0);
            }else if(ctx.lVal()!=null){
                return LLVMBuildLoad(builder,visitLVal(ctx.lVal()),ctx.lVal().IDENT().getText());
            }else if(ctx.IDENT()!=null){
                LLVMValueRef func = llvmSymbolTable.lookup(ctx.IDENT().getText(),2).getLLValue();
                LLVMValueRef result;
                if(ctx.funcRParams()==null){
                    result = LLVMBuildCall(builder,func,(PointerPointer) null,0,"call"+ctx.IDENT().getText());
                }else {
                    LLVMValueRef[] args = new LLVMValueRef[ctx.funcRParams().param().size()];
                    for(int i=0;i<ctx.funcRParams().param().size();++i){
                        args[i]=visitParam(ctx.funcRParams().param(i));
                        Object type = types.get(args[i]);
                        if (type!=null&&(int)type == 2) {
                            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(
                                    LLVMConstInt(i32Type, 0, 0),
                                    LLVMConstInt(i32Type, 0, 0)
                            );
                            args[i] = LLVMBuildInBoundsGEP(builder, args[i], indexPointer, 2, "arrayPtr");
                        }
                    }
                    result = LLVMBuildCall(builder,func,new PointerPointer<>(args),ctx.funcRParams().param().size(),"call"+ctx.IDENT().getText());
                }

                return result;
            }else{
                return null;
            }
        }else if(ctx.exp().size()==1){
            LLVMValueRef exp_0 = visitExp(ctx.exp(0));
            if(ctx.unaryOp()!=null){
                if(ctx.unaryOp().MINUS()!=null){
                    return LLVMBuildSub(builder,LLVMConstInt(i32Type,0,0),exp_0,"subtmp");
                }else if(ctx.unaryOp().PLUS()!=null){
                    return exp_0;
                }else if(ctx.unaryOp().NOT()!=null){
                    LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
                    LLVMValueRef one = LLVMConstInt(i32Type, 1, 0);LLVMValueRef result = LLVMBuildXor(builder, exp_0, zero, "xortmp");
                    return LLVMBuildSelect(builder, LLVMBuildICmp(builder, LLVMIntEQ, result, zero, "isZero"), one, zero, "notExpSelect");
                }
            } else{
                return exp_0;//(exp)
            }
        }else if(ctx.exp().size()==2) {
            LLVMValueRef exp1 = visitExp(ctx.exp(0));
            LLVMValueRef exp2 = visitExp(ctx.exp(1));
            if (ctx.PLUS() != null) {
                return LLVMBuildAdd(builder, exp1, exp2, "addtmp");
            } else if (ctx.MINUS() != null) {
                return LLVMBuildSub(builder, exp1, exp2, "subtmp");
            } else if (ctx.DIV() != null) {
                return LLVMBuildSDiv(builder, exp1, exp2, "divtmp");
            } else if (ctx.MUL() != null) {
                return LLVMBuildMul(builder, exp1, exp2, "multmp");
            } else if (ctx.MOD() != null) {
                return LLVMBuildSRem(builder, exp1, exp2, "remtmp");
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx){
        LLVMSymbolTable.SymbolTableEntry entry = llvmSymbolTable.lookup(ctx.IDENT().getText(),2);
        LLVMValueRef result=null;
        if(entry!=null) {
            if (ctx.exp().size() == 0) {
                result = entry.getLLValue();
            }else {
                LLVMValueRef index = visitExp(ctx.exp(0));
                Object type = types.get(entry.getLLValue());
                if(type!=null) {
                    int type_i = (int)type;
                    if (type_i == 3) {
                        PointerPointer<LLVMValueRef> indexArray = new PointerPointer<>(1);
                        indexArray.put(index);
                        LLVMValueRef elePoint = LLVMBuildGEP(builder, entry.getLLValue(), indexArray, 1, "elementPoint");
                        result = elePoint;
                    } else if (type_i == 2) {
                        LLVMValueRef[] indexArr = new LLVMValueRef[2];
                        indexArr[0]=(LLVMConstInt(i32Type, 0, 0));
                        indexArr[1]=(index);
                        result = LLVMBuildInBoundsGEP(builder, entry.getLLValue(), new PointerPointer<>(indexArr), 2, "elePoint");
                    }
                }
            }
        }
        return result;
    }
}
