package test.kang.constructor;

import test.kang.constructor.annotation.可重复注解;
import test.kang.constructor.annotation.注解01;
import test.kang.constructor.annotation.注解02_TYPE_USE;
import test.kang.constructor.annotation.注解03;
import test.kang.constructor.annotation.注解04_TYPE_USE;
import test.kang.constructor.annotation.注解_CONSTRUCTOR;
import test.kang.constructor.annotation.注解_PARAMETER;
import test.kang.constructor.annotation.注解_TYPE_USE;

import java.util.List;

public class Bean<B, N extends Number, T extends RuntimeException> {
    private int x, y, z;
    
    // 1. 无参public构造器
    public Bean(){
    }
    
    // 2. 单参数protected构造器
    protected Bean(int x) {
        this.x = x;
    }
    
    // 3. 双参数default构造器
    Bean(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    // 4. 三参数private构造器
    private Bean(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // 5. 带有TypeVariable的构造器
    public <X, Y extends List<String>> Bean(double d){
    }
    
    // 6. 形参数量可变的构造器
    public Bean(int... x){
    }
    
    // 7. 泛型形参构造器
    public Bean(B b, N n, char c){
    }
    
    // 8. 抛异常的构造器
    public Bean(long l) throws @注解02_TYPE_USE T, @注解04_TYPE_USE NullPointerException {
    }
    
    // 9. 带注解的构造器
    @可重复注解(str = "重复次数 1")
    @可重复注解(str = "重复次数 2")
    @注解_CONSTRUCTOR
    public @注解_TYPE_USE Bean (@注解01 @注解02_TYPE_USE short s1, @注解03 @注解04_TYPE_USE short s2, @注解_PARAMETER B b) {
    }
    
    @Override
    public String toString() {
        return "Bean{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
    // 用来测试Receiver Type
    class Test{
        // 拥有一个特殊的this参数
        public Test(@注解_TYPE_USE Bean<B, N, T> Bean.this){
        }
    }
}
