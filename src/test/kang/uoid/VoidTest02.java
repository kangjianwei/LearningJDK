package test.kang.uoid;

// Void在泛型中的使用
public class VoidTest02 {
    public static void main(String[] args) {
        Bar bar = new Bar();
        bar.bar();
    }
}

abstract class Foo<T> {
    abstract T bar();
}

// 需要一个返回值为void的泛型
class Bar extends Foo<Void> {
    Void bar() {
        System.out.println("Hello");
        return null;
    }
}
