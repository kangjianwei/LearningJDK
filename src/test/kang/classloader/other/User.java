package test.kang.classloader.other;

public class User {
    public User() {
        ClassLoader classLoader = User.class.getClassLoader();
        System.out.println("我是被 " + classLoader.getName() + " 加载的");
    }
}
