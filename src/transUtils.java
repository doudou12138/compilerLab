public class transUtils {
    public static int o_t_d(String octonary){
        int decimal=0;
        int base = 8;
        int m = 1;
        for(int i=octonary.length()-1;i>=1;i--){
            decimal+=(octonary.charAt(i)-'0')*m;
            m*=base;
        }
        return decimal;
    }

    public static int h_t_d(String hexadecimal){
        int decimal = 0;
        int base = 16;
        int m=1;
        for(int i=hexadecimal.length()-1;i>=1;i--){
            decimal+=(hexadecimal.charAt(i))*m;
            m*=base;
        }
        return decimal;
    }
}
