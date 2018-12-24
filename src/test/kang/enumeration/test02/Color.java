package test.kang.enumeration.test02;

public enum Color {
    WHITE("白色"), BLACK("黑色");   // 这里需要加分号
    
    private String desc;//中文描述
    
    Color(String desc) {
        this.desc = desc;
    }
    
    public String getDesc(){
        return desc;
    }
}
