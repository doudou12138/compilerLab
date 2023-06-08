
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeTarget;

public class SysYLlvmVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    LLVMModuleRef module=null;
    LLVMBuilderRef builder=null;
    LLVMTypeRef i32Type=null;
    LLVMBasicBlockRef block=null;

    LLVMSymbolTable llvmSymbolTable = null;

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
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx){
        if(ctx.decl().size()!=0){
            for(int i=0;i<ctx.decl().size();++i){
                if(ctx.decl(i).constDecl()==null){
                    List<SysYParser.VarDefContext> varDefs = ctx.decl(i).varDecl().varDef();
                    for(int j=0;j<varDefs.size();++j){
                        //创建一个常量,这里是常数0
                        LLVMValueRef value = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                        if(varDefs.get(j).initVal()!=null){
                            value = visitInitVal(varDefs.get(j).initVal());
                        }

                        //创建名为globalVar的全局变量
                        LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varDefs.get(j).IDENT().getText());
                        llvmSymbolTable.addEntry(varDefs.get(j).IDENT().getText(),globalVar,0);

                        //为全局变量设置初始化器
                        LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);
                    }
                }else{
                    List<SysYParser.ConstDefContext> constDefs = ctx.decl(i).constDecl().constDef();
                    for(int j=0;j<constDefs.size();++j){
                        //创建一个常量,这里是常数0
                        LLVMValueRef value = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                        if(constDefs.get(j).constInitVal()!=null){
                            value = visitConstInitVal(constDefs.get(j).constInitVal());
                        }

                        //创建名为globalVar的全局变量
                        LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/constDefs.get(j).IDENT().getText());
                        llvmSymbolTable.addEntry(constDefs.get(j).IDENT().getText(),globalVar,0);
                        //为全局变量设置初始化器
                        LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);
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
        LLVMTypeRef returnType = i32Type;

        LLVMTypeRef ft=null;
        if(ctx.funcFParams()==null){
            ft = LLVMFunctionType(returnType, (LLVMTypeRef) null, /* argumentCount */ 0, /* isVariadic */ 0);
        }else if(ctx.funcFParams().funcFParam()==null){

        }else if(ctx.funcFParams().funcFParam().size()==1){
            ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 1, /* isVariadic */ 0);
        }else{
            List<SysYParser.FuncFParamContext> paras = ctx.funcFParams().funcFParam();
            //生成函数参数类型
            PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paras.size());
            for(int m=0;m<paras.size();++m){
                argumentTypes.put(m,i32Type);
            }

            //生成函数类型
            ft = LLVMFunctionType(returnType, argumentTypes , /* argumentCount */ paras.size(), /* isVariadic */ 0);

        }

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/ctx.IDENT().getText(), ft);
        llvmSymbolTable.addEntry(ctx.IDENT().getText(),function,0);

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        block = LLVMAppendBasicBlock(function, /*blockName:String*/ctx.IDENT().getText()+"Entry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block);//后续生成的指令将追加在block1的后面

        llvmSymbolTable.enterScope();
        visit(ctx.block());
        llvmSymbolTable.exitScope();
        return null;
    }

    @Override
    public LLVMValueRef visitParam(SysYParser.ParamContext ctx){
        visitExp(ctx.exp());
        return null;
    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx){
        LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/ctx.IDENT().getText());
        llvmSymbolTable.addEntry(ctx.IDENT().getText(),pointer,0);

        LLVMValueRef value = LLVMConstInt(i32Type,0,0);
        if(ctx.initVal()!=null){
            value = visitInitVal(ctx.initVal());
        }
        LLVMBuildStore(builder,value,pointer);
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
        LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/ctx.IDENT().getText());
        llvmSymbolTable.addEntry(ctx.IDENT().getText(),pointer,0);

        LLVMValueRef value = LLVMConstInt(i32Type,0,0);
        if(ctx.constInitVal()!=null){
            value = visitConstInitVal(ctx.constInitVal());
            LLVMSymbolTable.SymbolTableEntry entry = llvmSymbolTable.lookup(ctx.IDENT().getText(),2);
        }
        LLVMBuildStore(builder,value,pointer);
        return null;
    }

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
            LLVMBuildRet(builder, /*result:LLVMValueRef*/result);
        }else if(ctx.block()!=null){
            llvmSymbolTable.enterScope();
            visitBlock(ctx.block());
            llvmSymbolTable.exitScope();
        }else if(ctx.ASSIGN()!=null){
            LLVMSymbolTable.SymbolTableEntry entry = llvmSymbolTable.lookup(ctx.lVal().IDENT().getText(),2);
            LLVMValueRef value = visitExp(ctx.exp());
            LLVMBuildStore(builder,value,entry.getLLValue());
        }
        return null;
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx){

        if(ctx.exp().size()==0){
            if(ctx.number()!=null){
                return LLVMConstInt(i32Type,Utils.toDecimal(ctx.number().getText()),0);
            }else if(ctx.lVal()!=null){
                return visitLVal(ctx.lVal());
            }else if(ctx.IDENT()!=null){
                LLVMValueRef func = llvmSymbolTable.lookup(ctx.IDENT().getText(),2).getLLValue();
                LLVMValueRef result;
                if(ctx.funcRParams()==null){
                    result = LLVMBuildCall(builder,func,(PointerPointer) null,0,"call"+ctx.IDENT().getText());
                }else {
                    LLVMValueRef[] args = new LLVMValueRef[ctx.funcRParams().param().size()];
                    for(int i=0;i<ctx.funcRParams().param().size();++i){
                        args[i]=visitParam(ctx.funcRParams().param(i));
                    }
                    result = LLVMBuildCall(builder,func,new PointerPointer<>(args),ctx.funcRParams().param().size(),"call"+ctx.IDENT().getText());
                }
                LLVMBuildRet(builder,result);
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
        if(entry!=null){
            result = LLVMBuildLoad(builder,entry.getLLValue(),ctx.IDENT().getText());
        }
        return result;
    }
}
