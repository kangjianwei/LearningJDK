package test.kang.clazz.test08.模板01;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface 注解_不可继承_public {
    String value();
}
