package test.kang.clazz.test07;

interface 内部接口{
}

// 获取内部类所处的外部构造方法
public class ClassTest19 {
    class 成员内部类{
    }
    
    内部接口 obj1 = new 内部接口() {
        // 匿名成员内部类对象
    };
    
    public ClassTest19(){
        class 方法内部类{
        }
        
        方法内部类 obj = new 方法内部类();
        
        /*
         * 这里正常运行
         * 因为用来声明obj对象的方法内部类定义在ClassTest19()方法内部
         */
        System.out.println("    方法内部类的外部构造方法：" + obj.getClass().getEnclosingConstructor());
    }
    
    public ClassTest19(int x){
        内部接口 obj2 = new 内部接口() {
            // 匿名方法内部类对象
        };
    
        /*
         * 这里正常运行
         * 因为用来声明obj2对象的匿名类定义在ClassTest19(int)方法内部
         */
        System.out.println("匿名方法内部类的外部构造方法：" + obj2.getClass().getEnclosingConstructor());
    }
    
    public ClassTest19(int x, int y){
        成员内部类 obj = new 成员内部类();
        
        /*
         * 这里返回null，因为用来声明obj对象的成员内部类定义在所有方法外面
         */
        System.out.println("    成员内部类的外部构造方法：" + obj.getClass().getEnclosingConstructor());
    }
    
    public ClassTest19(int x, int y, int z){
        /*
         * 这里返回null，因为用来声明obj1对象的匿名类定义在所有方法外面
         */
        System.out.println("匿名成员内部类的外部构造方法：" + obj1.getClass().getEnclosingConstructor());
    }
    
    public static void main(String[] args) throws NoSuchMethodException {
        ClassTest19 test1 = new ClassTest19();
        ClassTest19 test2 = new ClassTest19(1);
        ClassTest19 test3 = new ClassTest19(1, 2);
        ClassTest19 test4 = new ClassTest19(1, 2, 3);
    }
    
}
