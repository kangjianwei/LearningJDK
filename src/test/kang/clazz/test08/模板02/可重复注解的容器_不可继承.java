package test.kang.clazz.test08.模板02;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 作为可重复注解的容器
@Retention(RetentionPolicy.RUNTIME)
public @interface 可重复注解的容器_不可继承 {
    // 方法名称必须为value
    可重复注解_不可继承[] value();
}
