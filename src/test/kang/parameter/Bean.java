package test.kang.parameter;

import java.util.List;
import test.kang.parameter.annotation.可重复注解;
import test.kang.parameter.annotation.注解;
import test.kang.parameter.annotation.注解_PARAMETER;
import test.kang.parameter.annotation.注解_TYPE_USE;

public class Bean<B, N extends Number> {
    public void fun(final int p1, double[] p2, String p3, B p4, N p5, List<?>p6){
    }
    
    public void fun(
        @可重复注解(str = "重复次数 1")
        @可重复注解(str = "重复次数 2")
        @注解
        @注解_PARAMETER
        @注解_TYPE_USE
            int x
    ){
    }
}
