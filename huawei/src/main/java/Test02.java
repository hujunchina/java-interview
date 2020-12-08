package main.java;

import java.util.Arrays;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/15 10:59 上午
 * @Version 1.0
 */
public class Test02 {
    public static void main(String[] args) {

    }

    public static String BigNumberMultiple(String a, String b){
        String res = "";
        //1.把a、b转为int数组，并倒置数组
        int[] x = reverse(a);
        int[] y = reverse(b);

        //2.以短的作为因子，循环长的相乘，并记录状态
        //每次记录的大数要保存
        int[] sumTmp = new int[x.length*y.length];
        for(int i : x){
            //分解问题，每步转为大数乘普通数
            //存储乘结果，最长为y长度+1
            int[] tmp = new int[y.length+1];
            int k = 0;
            int t = 0; //进位记录
            for(int j : y){
                tmp[k++] = (i*j)%10 + t;
                t = (i*j) / 10;  //考虑进位
            }
            //每步求的结果和sumTmp做大数相加
            //进位考虑
            t = 0;
            int n = 0;
            for(int ii=0; ii<k; ii++){
                int tt = sumTmp[n]+tmp[ii];
                sumTmp[n++] = tt%10 + t;
                t = tt/10; //进位
            }
        }

        //10000   100 100  res*100 + 100
        //a b c  (a+b)*(c+d)

        return res;
    }

    public static int[] reverse(String s){
        char[] a = s.toCharArray();
        int[] res = new int[a.length];
        for(int i=a.length-1,j=0; i>=0; i++){
            res[j++] = a[i]+'0';
        }
        return res;
    }
}
