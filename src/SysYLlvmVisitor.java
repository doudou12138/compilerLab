
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeTarget;

public class SysYLlvmVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    LLVMModuleRef module=null;
    LLVMBuilderRef builder=null;
    LLVMTypeRef i32Type=null;
    LLVMBasicBlockRef block=null;

    public SysYLlvmVisitor(){
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
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx){
        //生成返回值类型
        LLVMTypeRef returnType = i32Type;

        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, (LLVMTypeRef) null, /* argumentCount */ 0, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/"main", ft);
        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        block = LLVMAppendBasicBlock(function, /*blockName:String*/"mainEntry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block);//后续生成的指令将追加在block1的后面

        visit(ctx.block());
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

        }
        return null;
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx){
            if(ctx.number()!=null){
                return LLVMConstInt(i32Type,Utils.toDecimal(ctx.number().getText()),0);
            }else{
                if(ctx.exp().size()==1){
                    LLVMValueRef exp_0 = visitExp(ctx.exp(0));
                    if(ctx.unaryOp()!=null){
                        if(ctx.unaryOp().MINUS()!=null){
                            return LLVMBuildSub(builder,LLVMConstInt(i32Type,0,0),exp_0,"subtmp");
                        }else if(ctx.unaryOp().PLUS()!=null){
                            return exp_0;
                        }else if(ctx.unaryOp().NOT()!=null){
                            LLVMBuildNot(builder,exp_0,"notExp");
                        }
                    } else{
                        return exp_0;//(exp)
                    }
                }else if(ctx.exp().size()==2){
                    LLVMValueRef exp1=visitExp(ctx.exp(0));
                    LLVMValueRef exp2=visitExp(ctx.exp(1));

                    if(ctx.PLUS()!=null){
                        return LLVMBuildAdd(builder,exp1,exp2,"addtmp");
                    }else if(ctx.MINUS()!=null){
                        return LLVMBuildSub(builder,exp1,exp2,"subtmp");
                    }else if(ctx.DIV()!=null){
                        return  LLVMBuildSDiv(builder,exp1,exp2,"divtmp");
                    }else if(ctx.MUL()!=null){
                        return LLVMBuildMul(builder,exp1,exp2,"multmp");
                    }else if(ctx.MOD()!=null){
                        return LLVMBuildSRem(builder,exp1,exp2,"remtmp");
                    }
                }
            }
            return null;
    }

    public void printLlvm(){
        LLVMDumpModule(module);
    }
}
