package test.kang.clazz.test04;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

// 泛型
public class ClassTest11 {
    public static void main(String[] args) {
        // 获取HashMap类的父类
        Type type = HashMap.class.getGenericSuperclass();
        System.out.println(type.getTypeName()+"    "+type.getClass().getName());
        
        System.out.println();
    
        // 获取HashMap类的父接口
        Type[] types = HashMap.class.getGenericInterfaces();
        for(Type t : types){
            System.out.println(t.getTypeName()+"    "+t.getClass().getName());
        }
        
        System.out.println();
        
        // 获取HashMap类的两个TypeVariable
        TypeVariable[] tvs = HashMap.class.getTypeParameters();
        for (TypeVariable tv : tvs){
            System.out.println(tv.getTypeName()+"    "+tv.getClass().getName());
        }
    }
}
