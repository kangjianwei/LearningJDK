package test.kang.classloader;

// 自定义类加载器
public class ClassLoaderTest03 {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        CustomClassLoader customClassLoader = new CustomClassLoader();
        
        // 将编译好的User类放在{项目源码的src目录}/test/kang/classloader/other下面，然后就可以使用自定义类加载器加载了
        Class<?> loadClass = customClassLoader.loadClass("test.kang.classloader.other.User");
        loadClass.newInstance();
    }
}
