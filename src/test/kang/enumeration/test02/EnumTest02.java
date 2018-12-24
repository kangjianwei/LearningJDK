package test.kang.enumeration.test02;

// 向枚举类添加方法与自定义构造方法
public class EnumTest02 {
    public static void main(String[] args) {
        // 隐式方法values()
        Color[] colors = Color.values();
        for(Color color : colors){
            System.out.println(color.getDesc());
        }
    }
}
