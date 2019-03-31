package test.kang.classloader;

// ClassLoader加载类
public class ClassLoaderTest02 {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Class<?> loadClass = classLoader.loadClass("test.kang.classloader.Bean");
        Bean bean = (Bean) loadClass.newInstance();
    }
}
