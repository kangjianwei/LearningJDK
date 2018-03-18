package test.kang.reference;

public class User {
    private String name;
    
    public User(String name) {
        this.name = name;
    }
    
    public static void print(User... args) {
        for(User arg : args) {
            if(arg != null) {
                System.out.print(arg.name + "  ");
            } else {
                System.out.print("null" + "  ");
            }
        }
        System.out.println();
    }
    
    @Override
    public String toString() {
        return "姓名：" + name + " ";
    }
}
