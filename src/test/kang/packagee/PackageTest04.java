package test.kang.packagee;

import com.kang.hello.annotation.AnnRe;
import com.kang.hello.annotation.Ann_PACKAGE;
import com.kang.hello.Hello;
import java.lang.annotation.Annotation;

// 返回所有注解，或返回指定类型的注解
public class PackageTest04 {
    public static void main(String[] args) {
        System.out.println("\n====通过类对象获取com.kang.hello包对象");
        Package pack = Hello.class.getPackage();
        System.out.println(pack);
    
        System.out.println("\n====2-1 getDeclaredAnnotations====");
        Annotation[] as1 = pack.getDeclaredAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====2-2 getDeclaredAnnotation====");
        Annotation annotation = pack.getDeclaredAnnotation(Ann_PACKAGE.class);
        System.out.println(annotation);
    
        System.out.println("\n====2-3 getDeclaredAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = pack.getDeclaredAnnotationsByType(AnnRe.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
