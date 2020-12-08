package main.java;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 管仲（胡军 hujun@tuya.com）
 * @Date 2020/9/15 9:55 上午
 * @Version 1.0
 */
public class Test01 {

    public static void main(String[] args) {
        System.out.println(getTokenList("I am \"hua\"\"\"\"wei\n\"").toString());
    }

    public static List<String> getTokenList(String sentenceStr){
        List<String> res = new ArrayList<>();
        //字符串以空格分离
        String[] tokens = sentenceStr.split("\\ ");
        //循环处理
        for(String token : tokens){
            //1.是否以引号开头
            if(token.startsWith("\"")){
                token = token.substring(1, token.length()-1);
                token.replaceAll("", "\"");
                System.out.println(token);
//                StringBuilder tmp = new StringBuilder();
//                char[] cArr = token.toCharArray();
//                boolean flag = false;
//                for (int i = 0; i < cArr.length-1; i++) {
//                    if(cArr[i]=='"' && cArr[i+1]=='"' && !flag){
//                        flag = true;
//                        continue;
//                    }else{
//                        tmp.append(cArr[i]);
//                        flag = false;
//                    }
//                }
//                token = tmp.toString();
            //2.是\n结尾
            }else if(token.endsWith("\\n")){
                token = token.substring(0, token.length()-1);
            }
            //3.正常的
            res.add(token);
        }
        return res;
    }
}
