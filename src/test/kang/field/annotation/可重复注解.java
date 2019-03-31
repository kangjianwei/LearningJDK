package test.kang.field.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 可重复注解必须关联一个容器
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(可重复注解的容器.class)
public @interface 可重复注解 {
    String str();
}
