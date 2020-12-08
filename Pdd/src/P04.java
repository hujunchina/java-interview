import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/1 8:00 下午
 * @Version 1.0
 */
public class P04 {
    public static void main(String[] args) {
        special2();
    }

    public static void special2(){
        Scanner in = new Scanner(System.in);
        HashSet<Integer> set = new HashSet<>();
        Long N = in.nextLong();
        int M = in.nextInt();
        int count = 0;
        int[] y = new int[M];
        for(int i=0; i<M; i++){
            int T = in.nextInt();
            y[i] = T;
        }
        Arrays.sort(y);
        for(int i=0; i<M; i++) {
            for (int j = i + 1; j < M; j++) {
                if (y[i] != 0 && y[j] != 0 && y[j] % y[i] == 0) {
                    y[j] = 0;
                }
            }
        }
        for(int i=0; i<M; i++){
            if(y[i]!=0){
                count += N/y[i];
            }
        }
        if(M>1){
            for(int i=0; i<M; i++){
                for(int j=i+1; j<M; j++){
                    if(y[i]!=0 && y[j]!=0) {
                        long t =  N / (y[i] * y[j]);
                        if(t==0){
                            count -= 1;
                        }else {
                            count -=t;
                        }
                    }
                }
            }
        }
        System.out.println(count);
    }

    public static void special(){
        Scanner in = new Scanner(System.in);
        HashSet<Integer> set = new HashSet<>();
        Long N = in.nextLong();
        int M = in.nextInt();
        int count = 0;
        for(int i=0; i<M; i++){
            int T = in.nextInt();
            for(int j=1; j<=N; j++){
                if(!set.contains(j) && j%T==0){
                    count++;
                    set.add(j);
                }
            }
        }
        System.out.println(count);
    }
}
