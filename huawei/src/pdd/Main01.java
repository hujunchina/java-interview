package pdd;

import java.util.Arrays;
import java.util.Scanner;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/10/23 6:56 下午
 * @Version 1.0
 */
public class Main01 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int N = in.nextInt();
        int M = in.nextInt();
        int[] arr = new int[N];
        for(int i=0; i<N; i++){
            arr[i] = in.nextInt();
        }
        Arrays.sort(arr);
        int max=-1, min=-1;
        int i=0, j=N-1;
        while(i<j){
            int t = arr[i]+M;
            if(Math.abs(arr[j]-t)<=M){
                max = t;
            } else {
                i++;
            }
            int k = Math.abs(arr[j]-M);
            if(Math.abs(arr[i]-k)<=M){
                min = k;
            } else {
                j--;
            }
            if(max!=-1 && min!=-1){
                break;
            }
        }
        if(max==-1 && min==-1){
            System.out.println("-1");
        } else {
            System.out.printf("%d %d", min, max);
        }
    }
}
