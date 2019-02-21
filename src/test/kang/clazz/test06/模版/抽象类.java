package test.kang.clazz.test06.模版;

public abstract class 抽象类 implements 接口 {
    
    // 抽象方法不能修饰为private
    public    abstract void 抽象类抽象方法_public();
    protected abstract void 抽象类抽象方法_protected();
              abstract void 抽象类抽象方法_default();
              
    public    void 抽象类非抽象方法_public() { }
    protected void 抽象类非抽象方法_protected() { }
              void 抽象类非抽象方法_default() { }
    private   void 抽象类非抽象方法_private() { }
}
