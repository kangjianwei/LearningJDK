package test.kang.clazz.test02;

// 获取数组类型名称
public class ClassTest04 {
    public static void main(String[] args){
        
        System.out.println(int[][].class.getName());
        System.out.println(int[][].class.getSimpleName());
        System.out.println(int[][].class.getCanonicalName());
        System.out.println(int[][].class.getTypeName());
        
        System.out.println();
        
        System.out.println(Integer[][].class.getName());
        System.out.println(Integer[][].class.getSimpleName());
        System.out.println(Integer[][].class.getCanonicalName());
        System.out.println(Integer[][].class.getTypeName());
        
    }
}
