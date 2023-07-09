public class ArrayType extends Type{
    private Type elementType;
    private int elementNum[];
    private int dimension;

    public ArrayType(Type elementType,int elementNum[],int dimension) {
        this.elementType = elementType;
        this.elementNum=elementNum;
        this.dimension=dimension;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getElementNum(int i){
        return elementNum[i];
    }

    public int getDimension(){return dimension;}

    @Override
    public String toString() {
        StringBuilder array_str = new StringBuilder(elementType.toString());
        for(int i=0;i<dimension;++i){
            array_str.append("[]");
        }
        return array_str.toString();
    }

}
