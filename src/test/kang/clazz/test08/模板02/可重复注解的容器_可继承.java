package test.kang.clazz.test08.模板02;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 作为可重复注解的容器
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface 可重复注解的容器_可继承 {
    // 方法名称必须为value
    可重复注解_可继承[] value();
}
