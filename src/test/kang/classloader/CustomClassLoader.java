package test.kang.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

// 自定义类加载器，从指定的目录加载类或其他资源
public class CustomClassLoader extends ClassLoader {
    
    // 设置父级类加载器为app class loader，保持双亲委托模式
    public CustomClassLoader() {
        // 类加载器名称为custom
        super("custom", ClassLoader.getSystemClassLoader());
    }
    
    @Override
    protected Class<?> findClass(String className) {
        // 包名+类名转换为路径
        String cn = className.replace('.', '/') + ".class";
        
        URL url = findResource(cn);
        
        try {
            InputStream inputStream = url.openStream();
            byte[] bytes = new byte[10240]; // 假设一次就把类文件读完了
            int len = inputStream.read(bytes);
            return defineClass(className, bytes, 0, len);
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Override
    protected URL findResource(String resName) {
        // 获取项目根目录
        String basepath = new File("").getAbsolutePath();
        
        URL url = null;
        try {
            // 这里约定从项目源码的src目录开始查找资源（不是类路径）
            url = new URL("file:/" + basepath + "/src/" + resName);
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }
}
