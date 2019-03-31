package com.kang.hello.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// 默认作用范围的注解
@Retention(RetentionPolicy.RUNTIME)
public @interface Ann {
}
