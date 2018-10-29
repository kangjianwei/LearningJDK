package test.kang.threadlocal;

/**
 * 测试SuppliedThreadLocal。
 * 该内部类位于ThreadLocal中，是ThreadLocal的子类，使用很简单。
 */
public class ThreadLocalTest04 {
    public static void main(String[] args) {
        User user = new User("张三", 20);
        
        ThreadLocal<User> userThreadLocal = ThreadLocal.withInitial(()->user);
        
        /*
         * 输出的是关联的初值
         * 参照例3和函数表达式Supplier就很好理解了
         */
        System.out.println(userThreadLocal.get());
    }
}
