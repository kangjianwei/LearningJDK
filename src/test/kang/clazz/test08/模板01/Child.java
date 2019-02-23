package test.kang.clazz.test08.模板01;

// 如果子类设置了跟父类同样的注解，则子类会屏蔽继承自父类的注解
@注解_不可继承_public(value = "子类中不可继承的注解")
@注解_不可继承_default(value = "子类中不可继承的注解，且只允许在当前包内使用")
public class Child extends Parent {
}
