import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.*;

public class LLVMSymbolTable {
    public static class SymbolTableEntry {
        private String name;        // 符号名称
        private LLVMValueRef llvmValueRef;          //
        private int scope;          // 符号作用域
        private int address;        // 符号地址

        public SymbolTableEntry(String name, LLVMValueRef llvmValueRef, int scope, int address) {
            this.name = name;
            this.llvmValueRef= llvmValueRef;
            this.scope = scope;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public LLVMValueRef getLLValue() {
            return llvmValueRef;
        }

        public int getScope() {
            return scope;
        }

        public int getAddress() {
            return address;
        }

    }

    // 符号表
    private Map<String, List<LLVMSymbolTable.SymbolTableEntry>> llvmSymbolTable;
    private int currentScope;

    public LLVMSymbolTable() {
        this.llvmSymbolTable = new HashMap<>();
        this.currentScope = 0;
    }

    // 进入新的作用域
    public void enterScope() {
        currentScope++;
    }

    // 退出当前作用域
    public void exitScope() {
        // 删除当前作用域中的所有符号
        Iterator<Map.Entry<String, List<LLVMSymbolTable.SymbolTableEntry>>> iterator = llvmSymbolTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<LLVMSymbolTable.SymbolTableEntry>> entry = iterator.next();
            List<LLVMSymbolTable.SymbolTableEntry> entries = entry.getValue();
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i).getScope() == currentScope) {
                    entries.remove(i);
                }
            }
            if (entries.isEmpty()) {
                iterator.remove();
            }
        }

        currentScope--;
    }

    // 查找符号
    public LLVMSymbolTable.SymbolTableEntry lookup(String name, int way) {
        if(way==1) {
            List<LLVMSymbolTable.SymbolTableEntry> entries = llvmSymbolTable.get(name);
            if (entries != null) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    LLVMSymbolTable.SymbolTableEntry entry = entries.get(i);
                    if (entry.getScope() == currentScope) {
                        return entry;
                    }
                }
            }
        }else if(way==2){
            List<LLVMSymbolTable.SymbolTableEntry> entries = llvmSymbolTable.get(name);
            if (entries != null) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    LLVMSymbolTable.SymbolTableEntry entry = entries.get(i);
                    if (entry.getScope() == currentScope) {
                        return entry;
                    }
                }
                for(int i=entries.size()-1;i>=0;i--){
                    LLVMSymbolTable.SymbolTableEntry entry=entries.get(i);
                    if(entry.getScope()<currentScope){
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    // 添加符号
    public void addEntry(String name,LLVMValueRef llvmValueRef, int address) {
        LLVMSymbolTable.SymbolTableEntry entry = new LLVMSymbolTable.SymbolTableEntry(name, llvmValueRef, currentScope, address);
        List<LLVMSymbolTable.SymbolTableEntry> entries = llvmSymbolTable.getOrDefault(name, new ArrayList<>());
        entries.add(entry);
        llvmSymbolTable.put(name, entries);
    }
}
