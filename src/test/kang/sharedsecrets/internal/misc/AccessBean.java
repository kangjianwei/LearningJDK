package test.kang.sharedsecrets.internal.misc;

import test.kang.sharedsecrets.bean.User;

// 为访问bean包留下的后门，模拟jdk.internal.misc包下的后门接口
public interface AccessBean {
    void fun1(User user);
    
    void fun2(User user);
}
