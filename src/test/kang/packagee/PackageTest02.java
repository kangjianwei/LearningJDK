package test.kang.packagee;

import com.kang.hello.Hello;

// 从MANIFEST.MF中获取相关的Package信息
public class PackageTest02 {
    public static void main(String[] args) {
        System.out.println("\n====激活引入的jar包====");
        Hello hello = new Hello();
        hello.sayHello();
    
        System.out.println("\n====获取com.kang.hello包对象");
        Package pack = Package.getPackage("com.kang.hello");
        System.out.println(pack);
        
        System.out.println("\n====版本信息");
        System.out.println(pack.getSpecificationTitle());
        System.out.println(pack.getSpecificationVendor());
        System.out.println(pack.getSpecificationVersion());
        System.out.println("----------------------------");
        System.out.println(pack.getImplementationTitle());
        System.out.println(pack.getImplementationVendor());
        System.out.println(pack.getImplementationVersion());
        
        System.out.println("\n====密封性====");
        System.out.println(pack.getName());
        System.out.println(pack.isSealed());
    }
}
