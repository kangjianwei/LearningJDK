package test.kang.sharedsecrets.internal.misc;

// 模拟SharedSecrets的实现机制
public class SharedSecrets {
    private static AccessBean sAccessBean;
    
    public static AccessBean getAccessBean() {
        return sAccessBean;
    }
    
    public static void setAccessBean(AccessBean access) {
        sAccessBean = access;
    }
}
