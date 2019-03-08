package test.kang.field;

public class Bean<B, N extends Number> {
    public    int a;    // a. public字段
    protected int b;    // b. public字段
              int c;    // c. public字段
    private   int d;    // d. public字段
    
    public String e;    // e. 引用类型字段
    
    public B f;         // f. 自由上界字段
    public N g;         // g. 限定上界字段
}
