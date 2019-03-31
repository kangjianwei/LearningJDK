package test.kang.clazz.test08.模板01;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface 注解_不可继承_default {
    String value();
}
