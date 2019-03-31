package test.kang.clazz.test03;

// 获取数组组件类型
public class ClassTest05 {
    public static void main(String[] args) {
        System.out.println(int.class.getComponentType());
        System.out.println(int[].class.getComponentType());
        System.out.println(int[][].class.getComponentType());
    
        System.out.println();
        
        System.out.println(Integer.class.getComponentType());
        System.out.println(Integer[].class.getComponentType());
        System.out.println(Integer[][].class.getComponentType());
    }
}
