package com.kang.hello.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 作为可重复注解的容器
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnReContainer {
    // 方法名称必须为value
    AnnRe[] value();
}
