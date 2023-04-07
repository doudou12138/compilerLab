import java.util.HashMap;

public class Utils{
    public static String showTitle(String str){
        if(str.length()<=1){
            return str.toUpperCase();
        }else{
            return str.substring(0,1).toUpperCase()+str.substring(1);
        }
    }



    public static void main(){

    }
}