package test.kang.packagee;

import com.kang.hello.Hello;

// 查看活跃的包
public class PackageTest01 {
    public static void main(String[] args) {
        /*
         * 如果没有这一步，则看不到引入的com.kang.hello包
         */
        System.out.println("\n====激活引入的jar包====");
        Hello hello = new Hello();
        hello.sayHello();
        
        System.out.println("\n====获取当前项目中活跃的Package对象");
        Package[] packages = Package.getPackages();
        for(Package p : packages){
            System.out.println(p);
        }
    }
}
