package test.kang.method;

import test.kang.method.annotation.可重复注解;
import test.kang.method.annotation.注解01;
import test.kang.method.annotation.注解02_TYPE_USE;
import test.kang.method.annotation.注解03;
import test.kang.method.annotation.注解04_TYPE_USE;
import test.kang.method.annotation.注解_METHOD;
import test.kang.method.annotation.注解_PARAMETER;
import test.kang.method.annotation.注解_TYPE_USE;

import java.util.List;

public class Bean<B, N extends Number, T extends RuntimeException> {
    private int x, y, z;
    
    // 1. public方法
    public Bean fun(){
        return this;
    }
    
    // 2. protected方法
    protected Bean fun(int x) {
        this.x = x;
        return this;
    }
    
    // 3. default方法
    Bean fun(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    // 4. private方法
    private Bean fun(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    // 5. 带有TypeVariable的方法
    public <X, Y extends List<String>> void fun(double d){
    }
    
    // 6. 返回void
    public void fun(boolean b1){
    }
    
    // 7. 返回基本类型int
    public int fun(boolean b1, boolean b2){
        return 123;
    }
    
    // 8. 返回引用类型Integer
    public Integer fun(boolean b1, boolean b2, boolean b3){
        return 123;
    }
    
    // 9. 返回上界为Object的泛型
    public B fun(boolean b1, boolean b2, boolean b3, boolean b4){
        return null;
    }
    
    // 10. 返回引用类型Integer
    public N fun(boolean b1, boolean b2, boolean b3, boolean b4, boolean b5){
        return null;
    }
    
    // 11. 形参数量可变的方法
    public void fun(int... x){
    }
    
    // 12. 泛型形参方法
    public void fun(B b, N n, char c){
    }
    
    // 13. 抛异常的构造器
    public void fun(long l) throws @注解02_TYPE_USE T, @注解04_TYPE_USE NullPointerException {
    }
    
    // 14. 带注解的构造器
    @可重复注解(str = "重复次数 1")
    @可重复注解(str = "重复次数 2")
    @注解_METHOD
    public @注解_TYPE_USE int fun(@注解01 @注解02_TYPE_USE short s1, @注解03 @注解04_TYPE_USE short s2, @注解_PARAMETER B b) {
        return 123;
    }
    
    // 15. 拥有一个特殊的this参数
    public void fun(@注解_TYPE_USE Bean<B, N, T> this, byte b){
    }
    
    @Override
    public String toString() {
        return "Bean{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
