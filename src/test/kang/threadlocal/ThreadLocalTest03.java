package test.kang.threadlocal;

// 为ThreadLocal关联初值
public class ThreadLocalTest03 {
    
    public static void main(String[] args) {
        ThreadLocal<User> userThreadLocal = new ThreadLocal<>(){
            @Override
            protected User initialValue() {
                return new User("张三", 20);
            }
        };
        
        // 输出的就是关联的初值
        System.out.println(userThreadLocal.get());
    }
}
