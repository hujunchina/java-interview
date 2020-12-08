/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/1 7:08 下午
 * @Version 1.0
 */
public class P01 {
    public static void main(String[] args) {
        printMatrix(12);
    }
    public static void printMatrix(int n){
        int[][] arr = new int[n][n];
        int m = n/2;
        for(int i=0; i<n; i++){
            for(int j=0; j<n; j++){
                arr[i][j] = 0;
                if(i<m && j>=m && i+j<n-1 && i+j>=m){
                    arr[i][j] = 1;
                }
                if(i<m && j<m && i+j<n-1 && i!=j && i<j){
                    arr[i][j] = 2;
                }
                if(i<=m && j<m && i+j<n-1 && i>j){
                    arr[i][j] = 3;
                }
                if(i>=m && j<m && i+j<n-1){
                    arr[i][j] = 4;
                }
                if(i>m && j<m && i+j>n-1){
                    arr[i][j] = 5;
                }
                if(i>m && j>=m && i+j>n-1 && i>j){
                    arr[i][j] = 6;
                }
                if(i>=m && j>m && i+j>n-1 && j>i){
                    arr[i][j] = 7;
                }
                if(i<m && j>m && i+j>n-1 ){
                    arr[i][j] = 8;
                }
                if(n%2!=0){
                    arr[i][m]=0;
                    arr[m][j]=0;
                }
            }
        }
        for(int i=0;i<n; i++){
            for(int j=0;j<n; j++){
                System.out.print(arr[i][j]);
                if(j!=n-1){
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

}
