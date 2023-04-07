import java.util.HashMap;

public class Utils{
    public static String showTitle(String str){
        if(str.length()<=1){
            return str.toUpperCase();
        }else{
            return str.substring(0,1).toUpperCase()+str.substring(1);
        }
    }

    public static int toDecimal(String nums){
        if (nums.charAt(0) == '0') {
            if (nums.length() == 1) {
                return 0;
            } else {
                if ((nums.charAt(1) - 'x' == 0) || (nums.charAt(1) - 'X' == 0)) {
                    return Integer.parseInt(nums.substring(2), 16);
                } else {
                    return Integer.parseInt(nums.substring(1), 8);
                }
            }
        }
        return Integer.parseInt(nums);
    }



    public static void main(){

    }
}