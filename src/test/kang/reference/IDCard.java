package test.kang.reference;

// 身份证号
public class IDCard {
    private int num;
    
    public IDCard(int num) {
        this.num = num;
    }
    
    public int getNum() {
        return num;
    }
    
    @Override
    public String toString() {
        return "身份证号：" + num + " ";
    }
}
