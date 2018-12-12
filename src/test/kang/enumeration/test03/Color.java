package test.kang.enumeration.test03;

public enum Color {
    WHITE {
        @Override
        public String getDesc() {
            return "白色";
        }
    },
    
    BLACK {
        @Override
        public String getDesc() {
            return "黑色";
        }
    };   // 这里需要加分号
    
    public abstract String getDesc();
}
