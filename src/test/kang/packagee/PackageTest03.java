package test.kang.packagee;

import com.kang.hello.annotation.AnnRe;
import com.kang.hello.annotation.Ann_PACKAGE;
import com.kang.hello.Hello;
import java.lang.annotation.Annotation;

// 返回所有注解，或返回指定类型的注解
public class PackageTest03 {
    public static void main(String[] args) {
        System.out.println("\n====通过类对象获取com.kang.hello包对象");
        Package pack = Hello.class.getPackage();
        System.out.println(pack);
    
        System.out.println("\n====1-1 getAnnotations====");
        Annotation[] as1 = pack.getAnnotations();
        for(Annotation a : as1){
            System.out.println(a);
        }
    
        System.out.println("\n====1-2 getAnnotation====");
        Annotation annotation = pack.getAnnotation(Ann_PACKAGE.class);
        System.out.println(annotation);
    
        System.out.println("\n====1-3 getAnnotationsByType[支持获取@Repeatable类型的注解]====");
        Annotation[] as2 = pack.getAnnotationsByType(AnnRe.class);
        for(Annotation a : as2){
            System.out.println(a);
        }
    }
}
