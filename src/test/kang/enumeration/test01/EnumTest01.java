package test.kang.enumeration.test01;

// 枚举的常规使用
public class EnumTest01 {
    public static void main(String[] args) {
        
        System.out.println("\n直接输出枚举常量（名称）");
        System.out.println(Color.WHITE);
    
        System.out.println("\n输出枚举常量序号");
        System.out.println(Color.WHITE.ordinal());
    
        System.out.println("\n获取该枚举常量的类型");
        System.out.println(Color.WHITE.getDeclaringClass());
    
        System.out.println("\n获取该枚举常量的名称");
        System.out.println(Color.BLACK.name());
    
        System.out.println("\n根据指定的名称获取枚举常量");
        Color color = Color.valueOf("BLACK");
        System.out.println(color);
    
        System.out.println("\n使用隐式方法values()获取所有枚举常量");
        Color[] colors1 = Color.values();
        for(Color c : colors1){
            System.out.println(c + "  " + c.ordinal());
        }
    
        System.out.println("\n获取所有枚举常量");
        Color[] colors2 = Color.class.getEnumConstants();
        for(Color c : colors2){
            System.out.println(c + "  " + c.ordinal());
        }
    }
}
