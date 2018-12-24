package test.kang.serializable.test05;

import java.io.Serializable;

// 如果父类没有实现Serializable接口，则继承来的属性不会被序列化
public class Child extends Parent implements Serializable {
    public int thisField;
    
    public Child(int thisField, int superField) {
        super(superField);
        this.thisField = thisField;
    }
    
    @Override
    public String toString() {
        return "thisField=" + thisField + ", superField=" + superField;
    }
}
