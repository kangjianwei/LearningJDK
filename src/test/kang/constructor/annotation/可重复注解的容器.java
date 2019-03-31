package test.kang.constructor.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 作为可重复注解的容器
@Retention(RetentionPolicy.RUNTIME)
public @interface 可重复注解的容器 {
    // 方法名称必须为value
    可重复注解[] value();
}
