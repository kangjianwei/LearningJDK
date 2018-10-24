package test.kang.sharedsecrets;

import test.kang.sharedsecrets.bean.User;
import test.kang.sharedsecrets.internal.misc.AccessBean;
import test.kang.sharedsecrets.internal.misc.SharedSecrets;

// 模拟SharedSecrets的实现机制
public class SharedSecretsTest01 {
    public static void main(String[] args) {
        User user = new User();
        
        // 访问User中的private方法和default方法
        AccessBean access = SharedSecrets.getAccessBean();
        access.fun1(user);
        access.fun2(user);
    }
}
