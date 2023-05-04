public class BasicType extends Type{
    private String typeName;

    public BasicType(String typeName){
        this.typeName=typeName;
    }

    @Override
    public String toString(){
        return typeName;
    }
}
