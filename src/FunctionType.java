import java.util.List;

public class FunctionType extends Type{
    private List<Type> parameterTypes;
    private Type returnType;

    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.toString()).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            sb.append(parameterTypes.get(i).toString());
            if (i < parameterTypes.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
