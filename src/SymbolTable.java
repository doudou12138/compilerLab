import java.util.*;

public class SymbolTable {
    // 符号表项
    public static class SymbolTableEntry {
        private String name;        // 符号名称
        private Type type;          // 符号类型
        private int scope;          // 符号作用域
        private int address;        // 符号地址

        public SymbolTableEntry(String name, Type type, int scope, int address) {
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public int getScope() {
            return scope;
        }

        public int getAddress() {
            return address;
        }
    }

    // 符号表
    private Map<String, List<SymbolTableEntry>> symbolTable;
    private int currentScope;

    public SymbolTable() {
        this.symbolTable = new HashMap<>();
        this.currentScope = 0;
    }

    // 进入新的作用域
    public void enterScope() {
        currentScope++;
    }

    // 退出当前作用域
    public void exitScope() {
        // 删除当前作用域中的所有符号
        Iterator<Map.Entry<String, List<SymbolTableEntry>>> iterator = symbolTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<SymbolTableEntry>> entry = iterator.next();
            List<SymbolTableEntry> entries = entry.getValue();
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
    public SymbolTableEntry lookup(String name,int way) {
        if(way==1) {
            List<SymbolTableEntry> entries = symbolTable.get(name);
            if (entries != null) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    SymbolTableEntry entry = entries.get(i);
                    if (entry.getScope() == currentScope) {
                        return entry;
                    }
                }
            }
        }else if(way==2){
            List<SymbolTableEntry> entries = symbolTable.get(name);
            if (entries != null) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    SymbolTableEntry entry = entries.get(i);
                    if (entry.getScope() == currentScope) {
                        return entry;
                    }
                }
                for(int i=entries.size()-1;i>=0;i--){
                    SymbolTableEntry entry=entries.get(i);
                    if(entry.getScope()<currentScope){
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    // 添加符号
    public void addEntry(String name, Type type, int address) {
        SymbolTableEntry entry = new SymbolTableEntry(name, type, currentScope, address);
        List<SymbolTableEntry> entries = symbolTable.getOrDefault(name, new ArrayList<>());
        entries.add(entry);
        symbolTable.put(name, entries);
    }
}
