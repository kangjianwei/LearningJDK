package test.kang.enumeration.test04;

// 接口和枚举配合使用，将多个枚举组合在一起
public class EnumTest04 {
    public static void main(String[] args) {
        Meal.BREAKFAST.menu();
        Meal.LUNCH.menu();
        Meal.DINNER.menu();
    }
}
