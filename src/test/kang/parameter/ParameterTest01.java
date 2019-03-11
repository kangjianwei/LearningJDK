package test.kang.parameter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;

// 常规形参测试
public class ParameterTest01 {
    public static void main(String[] args) throws NoSuchMethodException {
        Method method = Bean.class.getDeclaredMethod("fun", int.class, double[].class, String.class, Object.class, Number.class, List.class);
    
        Parameter[] parameters = method.getParameters();

        /*
         * 注意设置了编译时参数：-parameters（有多个编译参数时中间用空格隔开）
         * 如果没有此参数，则不会显示真实的形参名称
         */
        
        System.out.println("\n输出各形参的修饰符-类型-名称");
        for(Parameter p : parameters){
            System.out.println(Modifier.toString(p.getModifiers())+" "+p.getType().getSimpleName()+" "+p.getName());
        }

        System.out.println("\n将各形参字符串化");
        for(Parameter p : parameters){
            System.out.println(p.toString());
        }

        System.out.println("\n输出各形参类型[支持泛型语义]");
        for(Parameter p : parameters){
            System.out.println(p.getParameterizedType());
        }
    }
}
