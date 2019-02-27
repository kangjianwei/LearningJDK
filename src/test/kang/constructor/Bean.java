package test.kang.constructor;

import java.util.List;

public class Bean {
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
    
    @Override
    public String toString() {
        return "Bean{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
    
}
