package test.kang.sharedsecrets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import test.kang.sharedsecrets.bean.User;

/*
 * 测试jdk.internal.misc.SharedSecrets
 *
 * JDK>=9
 * java参数和javac参数中均要加入以下命令：
 * --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED
 *
 * 传入方式参见res中的截图
 */
public class SharedSecretsTest02 {
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        
        User user = new User();
        
        JavaLangAccess access = SharedSecrets.getJavaLangAccess();
        
        /*
         * 获取User类中名为fun4的无参方法
         *
         * getDeclaredPublicMethods在CLass类中是一个default方法，本来别的包中是无法直接使用的，除非使用反射
         * 但是此刻，无需使用反射，即可访问getDeclaredPublicMethods这个包访问权限的方法
         * 原因是早在System类中便使用JavaLangAccess对Class类的getDeclaredPublicMethods留下了后门
         */
        List<Method> methods = access.getDeclaredPublicMethods(User.class, "fun4");
        for(Method method : methods){
            System.out.println(method);
            method.invoke(user);
        }
    }
}
