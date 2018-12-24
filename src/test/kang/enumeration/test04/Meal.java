package test.kang.enumeration.test04;

public enum Meal {
    BREAKFAST(Food.Breakfast.class),
    LUNCH(Food.Lunch.class),
    DINNER(Food.Dinner.class);
    
    private Food[] foods;
    
    private Meal(Class<? extends Food> kind) {
        // 通过class对象获取枚举实例
        foods = kind.getEnumConstants();
    }
    
    public void menu() {
        String s = "";
        for(Food food : foods) {
            s += food + " ";
        }
        
        switch(this) {
            case BREAKFAST:
                System.out.println("早餐是：" + s);
                break;
            case LUNCH:
                System.out.println("午餐是：" + s);
                break;
            case DINNER:
                System.out.println("晚餐是：" + s);
                break;
        }
    }
    
    interface Food {
        enum Breakfast implements Food {
            鸡蛋, 面包, 牛奶
        }
        
        enum Lunch implements Food {
            米饭, 土豆, 牛肉
        }
        
        enum Dinner implements Food {
            小米粥, 馒头, 咸菜
        }
    }
}
