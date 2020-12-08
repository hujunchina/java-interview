import java.util.Scanner;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/1 8:49 下午
 * @Version 1.0
 */
public class P03 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int N = in.nextInt();   //商品总数
        int M = in.nextInt();   //背包大小
        int[] Ci = new int[N];
        int[] Vi = new int[N];
        for(int i=0; i<N; i++){
            Ci[i] = in.nextInt();
            Vi[i] = in.nextInt();
        }
        dfs(N, M, Ci, Vi, 0, 0, 0);
    }

    public static void dfs(int N, int M, int[] Ci, int[] Vi, int tmpM, int max, int tmpMax){
        if(tmpM>M){
            return;
        }
        if(tmpM==M){
            if(tmpMax > max){
                max = tmpMax;
            }
        }
        for(int i=0; i<N; i++) {
            //放第i个
            dfs(N, M, Ci, Vi, tmpM+Ci[i], max, tmpMax+Vi[i]);
            //不放第i个
            dfs(N, M, Ci, Vi, tmpM-Ci[i], max, tmpMax-Vi[i]);
        }
    }
}
