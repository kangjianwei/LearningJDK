package test.kang.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

// ClassLoader加载资源
public class ClassLoaderTest01 {
    public static void main(String[] args) throws IOException {
        // 从当前类加载器加载类文件的根目录开始搜索资源
        String path = "test/kang/classloader/res/welcome/greeting.txt";
        
        URL urlRes = ClassLoader.getSystemResource(path);
        System.out.println(urlRes);

        Enumeration<URL> urlEnumeration = ClassLoader.getSystemResources(path);
        while(urlEnumeration.hasMoreElements()){
            URL url = urlEnumeration.nextElement();
            System.out.println(url);
        }

        InputStream inputStream = ClassLoader.getSystemResourceAsStream(path);
        byte[] bytes = new byte[1024];
        while(inputStream.read(bytes)!=-1){
            System.out.println(new String(bytes));
        }
    }
}
