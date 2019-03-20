package test.kang.annotatedtype;

import test.kang.annotatedtype.annotation.注解_TYPE_USE_1;
import test.kang.annotatedtype.annotation.注解_TYPE_USE_2_1;
import test.kang.annotatedtype.annotation.注解_TYPE_USE_2_2;
import test.kang.annotatedtype.annotation.注解_TYPE_USE_3;

// 直接作用在T上的类型注解@注解_TYPE_USE_2_1目前没法获取
public class Child<@注解_TYPE_USE_2_1 T extends @注解_TYPE_USE_2_2 Parent> {
    public @注解_TYPE_USE_1 Parent.Inner field1;
    public T field2;
    public @注解_TYPE_USE_3 Parent<? extends Number, ? super Integer> field3;
    
    public Child() {
    }
}
