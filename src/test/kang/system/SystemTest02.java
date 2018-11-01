package test.kang.system;

import java.util.Map.Entry;
import java.util.Properties;

// 属性
public class SystemTest02 {
    public static void main(String[] args) {
        System.out.println("\n---------------设置/获取属性---------------");
        System.setProperty("name", "张三");
        System.setProperty("age", "20");
        System.out.println(System.getProperty("name")+"  "+ System.getProperty("age"));
        System.out.println(System.getProperty("username", "铁蛋"));   // 有默认值
    
    
        System.out.println("\n---------------设置/获取系统属性集---------------");
        Properties sp = System.getProperties();  // 获取系统属性集【其中包含通过setProperty设置的属性】
        for(Entry entry : sp.entrySet()){        // 遍历系统属性集
            System.out.println(entry.getKey()+" :  "+entry.getValue());
        }
        
        
        System.out.println("\n---------------设置/获取自定义属性集---------------");
        Properties properties = new Properties();   // 构造自定义属性集
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        properties.setProperty("key3", "value3");
        
        System.setProperties(properties);       // 设置自定义属性集【该操作会替换掉原有的系统属性集】
        
        Properties pp = System.getProperties(); // 获取自定义属性集
        for(Entry entry : pp.entrySet()){       // 遍历自定义属性集
            System.out.println(entry.getKey()+" :  "+entry.getValue());
        }
    }
}
