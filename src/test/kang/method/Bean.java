package test.kang.method;

import java.util.List;

public class Bean<B, N extends Number> {
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
    
    @Override
    public String toString() {
        return "Bean{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
