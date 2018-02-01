package test.kang.uoid;

// Void在反射中的使用
public class VoidTest01 {
    public static void main(String[] args) throws NoSuchMethodException {
        String methodName = "fun";
        
        if(VoidTest01.class.getMethod("foo").getReturnType() == Void.TYPE) {
            System.out.println(methodName + "方法的返回值为void");
        } else {
            System.out.println(methodName + "方法的返回值不为void");
        }
    }
    
    public void foo() {
    }
}
