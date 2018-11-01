package test.kang.system;

import java.util.Map;
import java.util.Map.Entry;

// 环境变量
public class SystemTest04 {
    public static void main(String[] args) {
        System.out.println("\n---------------查看单个指定的环境变量---------------");
        System.out.println("JAVA_HOME =  "+ System.getenv("JAVA_HOME"));
    
        System.out.println("\n---------------所有环境变量---------------");
        Map<String, String> env = System.getenv();
        for(Entry entry : env.entrySet()){
            System.out.println(entry.getKey()+" =  "+entry.getValue());
        }
    }
}

