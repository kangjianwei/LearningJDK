package test.kang.serializable.test05;

public class Parent {
    public int superField;
    
    // 此处必须有空的构造方法，否则反序列化时无法构造父类对象
    public Parent() {
    }
    
    public Parent(int superField) {
        this.superField = superField;
    }
}
