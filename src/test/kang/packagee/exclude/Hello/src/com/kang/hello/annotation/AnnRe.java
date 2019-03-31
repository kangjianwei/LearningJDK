package com.kang.hello.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 可重复注解必须关联一个容器
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AnnReContainer.class)
public @interface AnnRe {
    String str();
}
