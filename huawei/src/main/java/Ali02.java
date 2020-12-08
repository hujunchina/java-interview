package main.java;

import javax.swing.plaf.IconUIResource;
import java.util.*;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/29 7:13 下午
 * @Version 1.0
 */
public class Ali02 {

    public static void main(String[] args) {
        test();
    }

    public static void test(){
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        Set<String> dict = new HashSet<>();
        Map<Character, Integer> headChar = new HashMap<>();
        String t = in.nextLine();
        for(int i=0; i<n; i++){
            String str = in.nextLine();
            dict.add(str);
            char c = str.charAt(0);
            if(headChar.containsKey(c)){
                headChar.put(c, headChar.get(c)+1);
            }else{
                headChar.put(c, 1);
            }
        }
        System.out.println();
        for(int i=0; i<m; i++){
            String target = in.nextLine();
            if(!headChar.containsKey(target.charAt(0))){
                System.out.println(target.length());
                continue;
            }
            char[] chars = target.toCharArray();
            int times = 0;
            if(headChar.get(chars[0])==1 && dict.contains(target)){
                System.out.println(2);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            boolean flat = false;
            for(int j=0; j<chars.length; j++){
                sb.append(chars[j]);
                if(dict.contains(sb.toString())){
                    times++;
                    flat=true;
                }else{
                    if(flat){
                        times++;
                    }
                }
            }
            System.out.println(times);
        }
    }
}

/*
5 5
a
an
at
and
nowcoder
a
and
nowcoder
nowcoderovo
what
 */